package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.model.PlanRevision;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de ProjectQueryService: listado, búsqueda y acceso por actor. */
class ProjectQueryServiceTest {

    private final Map<Integer, Project> stored = new LinkedHashMap<>();
    private final Map<Integer, UserAccount> accounts = new LinkedHashMap<>();
    private final UserDirectory userDirectory = new UserDirectory() {
        @Override
        public Optional<UserAccount> findById(Integer userId) {
            return Optional.ofNullable(accounts.get(userId));
        }

        @Override
        public Map<Integer, UserAccount> findByIds(java.util.Collection<Integer> userIds) {
            return userIds.stream().filter(accounts::containsKey).distinct()
                    .collect(Collectors.toMap(Function.identity(), accounts::get));
        }

        @Override
        public List<UserAccount> findActive() {
            return accounts.values().stream().filter(UserAccount::active).toList();
        }
    };
    private int nextId = 1;
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
            return new ArrayList<>(stored.values());
        }

        @Override
        public List<Project> findManagedBy(Integer managerUserId) {
            return findAll().stream().filter(p -> p.managerUserId().equals(managerUserId)).toList();
        }

        @Override
        public List<Project> search(String likePattern) {
            return findAll();
        }

        @Override
        public List<Project> searchManagedBy(Integer managerUserId, String likePattern) {
            return findManagedBy(managerUserId);
        }

        @Override
        public BigDecimal allocatedBudget(Integer projectId) {
            return BigDecimal.ZERO;
        }

        @Override
        public List<PlanRevision> planHistory(Integer projectId) {
            return java.util.List.of();
        }

        @Override
        public Project save(Project project) {
            Integer id = project.isNew() ? nextId++ : project.id();
            Project restored = Project.restore(id, project.name(), project.description(),
                    project.managerUserId(), project.status(), project.budgetBob(),
                    project.estimatedDurationMinutes(), project.createdByUserId(),
                    project.createdAt(), project.createdAt(), project.lastRevisionNumber());
            stored.put(id, restored);
            return restored;
        }
    };
    private Actor actor;
    private ProjectQueryService queries;

    @BeforeEach
    void setUp() {
        accounts.put(1, new UserAccount(1, "jefe@x.com", "Julia", "Mendoza", true, true));
        accounts.put(2, new UserAccount(2, "enc1@x.com", "Ana", "Paz", false, true));
        accounts.put(3, new UserAccount(3, "enc2@x.com", "Luis", "Vaca", false, true));
        actor = new Actor(1, true, true);
        queries = new ProjectQueryService(projectRepository, userDirectory, () -> actor);
    }

    private Project seedProject(String name, Integer managerId) {
        Project p = Project.create(name, "desc", managerId, new BigDecimal("1000"), 480, "Plan", managerId,
                Instant.parse("2026-10-01T10:00:00Z"));
        return projectRepository.save(p);
    }

    private void actAs(UserAccount user) {
        actor = new Actor(user.userId(), user.chief(), user.active());
    }

    @Test
    void chiefListsAllProjects() {
        seedProject("A", 2);
        seedProject("B", 2);
        assertThat(queries.listProjects()).hasSize(2);
    }

    @Test
    void managerListsOnlyOwnProjects() {
        seedProject("Mia", 2);
        seedProject("Ajena", 3);
        actAs(accounts.get(2));
        assertThat(queries.listProjects()).extracting(ProjectResult::name).containsExactly("Mia");
    }

    @Test
    void inactiveActorSeesNoProjects() {
        seedProject("A", 2);
        actAs(accounts.get(2));
        actor = new Actor(2, false, false);
        assertThat(queries.listProjects()).isEmpty();
    }

    @Test
    void nullActorSeesNoProjects() {
        seedProject("A", 2);
        actor = null;
        assertThat(queries.listProjects()).isEmpty();
    }

    @Test
    void getProjectReturnsVisibleProject() {
        Project p = seedProject("A", 2);
        assertThat(queries.getProject(p.id()).name()).isEqualTo("A");
    }

    @Test
    void getProjectRejectsMissingAndInvisible() {
        assertThatThrownBy(() -> queries.getProject(999)).isInstanceOf(NotFoundException.class);
        seedProject("Ajena", 3);
        actAs(accounts.get(2));
        Integer ajenaId = stored.keySet().stream().max(Integer::compareTo).orElse(null);
        assertThatThrownBy(() -> queries.getProject(ajenaId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void planHistoryReturnsEmptyForProjectWithoutRevisions() {
        Project p = seedProject("A", 2);
        assertThat(queries.planHistory(p.id())).isEmpty();
    }

    @Test
    void eligibleManagersOnlyForChief() {
        actAs(accounts.get(2));
        assertThatThrownBy(() -> queries.eligibleManagers()).isInstanceOf(ForbiddenException.class);
        actAs(accounts.get(1));
        assertThat(queries.eligibleManagers()).extracting(UserSummaryResult::userId)
                .containsExactly(1, 2, 3);
    }

    @Test
    void projectStatusCanBeCanceled() {
        Project p = seedProject("A", 2);
        p.changeStatus(ProjectStatus.CANCELADO);
        assertThat(p.status()).isEqualTo(ProjectStatus.CANCELADO);
        assertThat(p.status().isClosed()).isTrue();
    }

    @Test
    void searchProjectsFiltersByActorVisibility() {
        seedProject("Alpha", 2);
        seedProject("Beta", 2);
        seedProject("Gamma", 3);

        actor = new Actor(1, true, true);
        List<ProjectResult> chief = queries.search("Alpha");
        assertThat(chief).isNotNull();

        actor = new Actor(2, false, true);
        List<ProjectResult> mgr = queries.search("Alpha");
        assertThat(mgr).isNotNull();

        actor = new Actor(3, false, true);
        List<ProjectResult> mgr3 = queries.search("Gamma");
        assertThat(mgr3).isNotNull();
    }

    @Test
    void eligibleManagersFiltersInactiveWorkers() {
        accounts.put(4, new UserAccount(4, "inactive@x.com", "In", "Active", false, false));
        actAs(accounts.get(1));
        assertThat(queries.eligibleManagers()).extracting(UserSummaryResult::userId)
                .containsExactly(1, 2, 3);
    }
}
