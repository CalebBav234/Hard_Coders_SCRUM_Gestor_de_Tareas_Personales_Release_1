package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.ChangeProjectStatusCommand;
import com.hardcoders.taskmanager.application.command.CreateProjectCommand;
import com.hardcoders.taskmanager.application.command.DelegateProjectCommand;
import com.hardcoders.taskmanager.application.command.ReplanProjectCommand;
import com.hardcoders.taskmanager.application.command.UpdateProjectDetailsCommand;
import com.hardcoders.taskmanager.application.result.PlanRevisionResult;
import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.domain.exception.BudgetBelowAllocationsException;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.exception.VersionConflictException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.PlanRevision;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Épica 1 con dobles en memoria: criterios de aceptación de US1.1 y US1.2 sin base de datos. */
class ProjectUseCasesTest {

    private static final UserAccount CHIEF = new UserAccount(1, "jefe@x.com", "Julia", "Mendoza", true, true);
    private static final UserAccount MANAGER = new UserAccount(2, "enc1@x.com", "Ana", "Paz", false, true);
    private static final UserAccount OTHER_MANAGER = new UserAccount(3, "enc2@x.com", "Luis", "Vaca", false, true);
    private static final UserAccount INACTIVE = new UserAccount(4, "baja@x.com", "Iván", "Soto", false, false);

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC);
    private final Map<Integer, UserAccount> accounts = new LinkedHashMap<>();
    private final Map<Integer, Project> stored = new HashMap<>();
    private final Map<Integer, List<PlanRevision>> revisions = new HashMap<>();
    private final Map<Integer, BigDecimal> allocated = new HashMap<>();
    private Actor actor;
    private ProjectCommandService commands;
    private ProjectQueryService queries;
    private int nextId = 100;
    private int tick = 0;

    @BeforeEach
    void setUp() {
        List.of(CHIEF, MANAGER, OTHER_MANAGER, INACTIVE).forEach(user -> accounts.put(user.userId(), user));
        actAs(CHIEF);
        commands = new ProjectCommandService(projectRepository, userDirectory, () -> actor, clock);
        queries = new ProjectQueryService(projectRepository, userDirectory, () -> actor);
    }

    @Test
    void chiefCreatesAProjectWithItsFirstPlanRevision() {
        ProjectResult created = create("Edificio A", MANAGER.userId(), "15000.50");

        assertThat(created.status()).isEqualTo("PLANIFICADO");
        assertThat(created.budgetBob()).isEqualByComparingTo("15000.50");
        assertThat(created.manager().fullName()).isEqualTo("Ana Paz");
        assertThat(created.createdBy().userId()).isEqualTo(CHIEF.userId());
        assertThat(created.currentRevision()).isEqualTo(1);
        assertThat(created.canEditPlan()).isTrue();
        assertThat(created.allowedStatuses()).containsExactly("CANCELADO", "EN_EJECUCION");
        assertThat(queries.planHistory(created.projectId())).singleElement()
                .extracting(PlanRevisionResult::reason).isEqualTo("Plan inicial");
    }

    @Test
    void onlyTheChiefCreatesProjectsAndManagersMustBeActive() {
        actAs(MANAGER);
        assertThatThrownBy(() -> create("Otro", MANAGER.userId(), "10")).isInstanceOf(ForbiddenException.class);

        actAs(CHIEF);
        assertThatThrownBy(() -> create("Otro", INACTIVE.userId(), "10")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> create("Otro", 999, "10")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> create("Otro", MANAGER.userId(), "-1"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void managerCanRenameButCannotTouchTheBudget() {
        ProjectResult project = create("Edificio A", MANAGER.userId(), "1000");

        actAs(MANAGER);
        ProjectResult renamed = commands.updateDetails(new UpdateProjectDetailsCommand(project.projectId(),
                "Edificio A - Fase 1", "Obra gruesa", project.version()));
        assertThat(renamed.name()).isEqualTo("Edificio A - Fase 1");
        assertThat(renamed.canEditPlan()).isFalse();
        assertThat(renamed.canDelegate()).isFalse();

        assertThatThrownBy(() -> commands.replan(new ReplanProjectCommand(project.projectId(),
                new BigDecimal("5000"), 100, "Subir", renamed.version()))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> commands.changeStatus(new ChangeProjectStatusCommand(project.projectId(),
                "EN_EJECUCION", renamed.version()))).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void replanAddsARevisionAndCannotGoBelowAllocations() {
        ProjectResult project = create("Edificio A", MANAGER.userId(), "1000");
        allocated.put(project.projectId(), new BigDecimal("800.00"));

        ProjectResult replanned = commands.replan(new ReplanProjectCommand(project.projectId(),
                new BigDecimal("1200"), 900, "Ampliación del alcance", project.version()));
        assertThat(replanned.budgetBob()).isEqualByComparingTo("1200.00");
        assertThat(replanned.currentRevision()).isEqualTo(2);
        assertThat(replanned.allocatedBudgetBob()).isEqualByComparingTo("800.00");

        assertThatThrownBy(() -> commands.replan(new ReplanProjectCommand(project.projectId(),
                new BigDecimal("799.99"), 900, "Recorte", replanned.version())))
                .isInstanceOf(BudgetBelowAllocationsException.class);
        assertThatThrownBy(() -> commands.replan(new ReplanProjectCommand(project.projectId(),
                new BigDecimal("2000"), 900, "Con versión vieja", project.version())))
                .isInstanceOf(VersionConflictException.class);
        assertThat(queries.planHistory(project.projectId())).extracting(PlanRevisionResult::revisionNumber)
                .containsExactly(2, 1);
    }

    @Test
    void chiefDelegatesToAnActiveUserAndTheChangeIsAudited() {
        ProjectResult project = create("Edificio A", MANAGER.userId(), "1000");

        ProjectResult delegated = commands.delegate(new DelegateProjectCommand(project.projectId(),
                OTHER_MANAGER.userId(), "Reasignación por carga de trabajo", project.version()));

        assertThat(delegated.manager().userId()).isEqualTo(OTHER_MANAGER.userId());
        assertThat(delegated.currentRevision()).isEqualTo(2);
        PlanRevisionResult audit = queries.planHistory(project.projectId()).getFirst();
        assertThat(audit.changedBy().userId()).isEqualTo(CHIEF.userId());
        assertThat(audit.reason()).isEqualTo("Delegación de Ana Paz a Luis Vaca: Reasignación por carga de trabajo");

        assertThatThrownBy(() -> commands.delegate(new DelegateProjectCommand(project.projectId(),
                INACTIVE.userId(), "Baja", delegated.version()))).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void managersCannotDelegateAndOnlySeeTheirProjects() {
        ProjectResult mine = create("Edificio A", MANAGER.userId(), "1000");
        ProjectResult notMine = create("Edificio B", OTHER_MANAGER.userId(), "1000");

        actAs(MANAGER);
        assertThatThrownBy(() -> commands.delegate(new DelegateProjectCommand(mine.projectId(),
                MANAGER.userId(), "Me lo quedo", mine.version()))).isInstanceOf(ForbiddenException.class);
        assertThat(queries.listProjects()).extracting(ProjectResult::projectId).containsExactly(mine.projectId());
        assertThatThrownBy(() -> queries.getProject(notMine.projectId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> commands.updateDetails(new UpdateProjectDetailsCommand(notMine.projectId(),
                "x", null, notMine.version()))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> queries.eligibleManagers()).isInstanceOf(ForbiddenException.class);

        actAs(CHIEF);
        assertThat(queries.listProjects()).hasSize(2);
        assertThat(queries.eligibleManagers()).extracting(user -> user.userId())
                .containsExactly(CHIEF.userId(), MANAGER.userId(), OTHER_MANAGER.userId());
    }

    @Test
    void lifecycleThroughUseCases() {
        ProjectResult project = create("Edificio A", MANAGER.userId(), "1000");

        ProjectResult running = commands.changeStatus(new ChangeProjectStatusCommand(project.projectId(),
                "en_ejecucion", project.version()));
        assertThat(running.status()).isEqualTo("EN_EJECUCION");
        ProjectResult finished = commands.changeStatus(new ChangeProjectStatusCommand(project.projectId(),
                "FINALIZADO", running.version()));
        assertThat(finished.closed()).isTrue();
        assertThat(finished.canEditPlan()).isFalse();
        assertThat(finished.allowedStatuses()).isEmpty();
        assertThatThrownBy(() -> commands.changeStatus(new ChangeProjectStatusCommand(project.projectId(),
                "INVENTADO", finished.version()))).isInstanceOf(DomainValidationException.class);
    }

    private ProjectResult create(String name, Integer managerId, String budget) {
        return commands.create(new CreateProjectCommand(name, null, managerId, new BigDecimal(budget), 600, null));
    }

    private void actAs(UserAccount user) {
        actor = new Actor(user.userId(), user.chief(), user.active());
    }

    private final UserDirectory userDirectory = new UserDirectory() {
        @Override
        public Optional<UserAccount> findById(Integer userId) {
            return Optional.ofNullable(accounts.get(userId));
        }

        @Override
        public Map<Integer, UserAccount> findByIds(Collection<Integer> userIds) {
            return userIds.stream().filter(accounts::containsKey).distinct()
                    .collect(Collectors.toMap(Function.identity(), accounts::get));
        }

        @Override
        public List<UserAccount> findActive() {
            return accounts.values().stream().filter(UserAccount::active).toList();
        }
    };

    private final ProjectRepository projectRepository = new ProjectRepository() {
        @Override
        public Optional<Project> findById(Integer projectId) {
            return Optional.ofNullable(stored.get(projectId));
        }

        @Override
        public Optional<Project> findForUpdate(Integer projectId) {
            return findById(projectId);
        }

        @Override
        public List<Project> findAll() {
            return stored.values().stream().sorted(Comparator.comparing(Project::id)).toList();
        }

        @Override
        public List<Project> findManagedBy(Integer managerUserId) {
            return findAll().stream().filter(p -> p.managerUserId().equals(managerUserId)).toList();
        }

        @Override
        public BigDecimal allocatedBudget(Integer projectId) {
            return allocated.getOrDefault(projectId, BigDecimal.ZERO.setScale(2));
        }

        @Override
        public List<PlanRevision> planHistory(Integer projectId) {
            List<PlanRevision> list = new ArrayList<>(revisions.getOrDefault(projectId, List.of()));
            list.sort(Comparator.comparingInt(PlanRevision::revisionNumber).reversed());
            return list;
        }

        @Override
        public Project save(Project project) {
            Integer id = project.isNew() ? nextId++ : project.id();
            List<PlanRevision> list = revisions.computeIfAbsent(id, key -> new ArrayList<>());
            list.addAll(project.pendingRevisions());
            Project saved = Project.restore(id, project.name(), project.description(), project.managerUserId(),
                    project.status(), project.budgetBob(), project.estimatedDurationMinutes(),
                    project.createdByUserId(), project.createdAt(), Instant.now(clock).plusMillis(++tick),
                    list.size());
            stored.put(id, saved);
            return saved;
        }
    };
}
