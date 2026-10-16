package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.result.PlanRevisionResult;
import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.PlanRevision;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * US2.1 - Isolation scoping: caminos basicos de {@code ProjectQueryService.listProjects},
 * {@code loadVisible} y {@code eligibleManagers}. Todo el filtrado ocurre en el servidor.
 */
@ExtendWith(MockitoExtension.class)
class ProjectQueryServiceScopingTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    @Mock
    private ProjectRepository projects;
    @Mock
    private UserDirectory users;

    private ProjectQueryService serviceWith(Actor actor) {
        CurrentActor currentActor = () -> actor;
        return new ProjectQueryService(projects, users, currentActor);
    }

    private static Project project(Integer id, Integer managerUserId) {
        return Project.restore(id, "Proyecto " + id, "desc", managerUserId, ProjectStatus.EN_EJECUCION,
                new BigDecimal("100.00"), 60, managerUserId, NOW, NOW, 1);
    }

    @Test
    @DisplayName("US21-QS-01: actor inactivo recorre B1 y no ve nada sin consultar el repositorio")
    void inactiveActorGetsEmptyList() {
        ProjectQueryService service = serviceWith(new Actor(4, false, false));

        assertThat(service.listProjects()).isEmpty();
        verifyNoInteractions(projects);
    }

    @Test
    @DisplayName("US21-QS-02: el Encargado solo recibe sus proyectos delegados (AC-US2.1-01)")
    void managerOnlyGetsDelegatedProjects() {
        ProjectQueryService service = serviceWith(new Actor(2, false, true));
        when(projects.findManagedBy(2)).thenReturn(List.of(project(10, 2)));
        when(projects.allocatedBudget(any())).thenReturn(BigDecimal.ZERO);
        when(users.findByIds(anyCollection())).thenReturn(Map.of(
                2, new UserAccount(2, "ana@gestor.local", "Ana", "Paz", false, true)));

        List<ProjectResult> visible = service.listProjects();

        assertThat(visible).extracting(ProjectResult::projectId).containsExactly(10);
        verify(projects).findManagedBy(2);
        verify(projects, never()).findAll();
    }

    @Test
    @DisplayName("US21-QS-03: el Jefe recibe todos los proyectos (B2)")
    void chiefGetsEveryProject() {
        ProjectQueryService service = serviceWith(new Actor(1, true, true));
        when(projects.findAll()).thenReturn(List.of(project(10, 2), project(11, 3)));
        when(projects.allocatedBudget(any())).thenReturn(BigDecimal.ZERO);
        when(users.findByIds(anyCollection())).thenReturn(Map.of(
                2, new UserAccount(2, "ana@gestor.local", "Ana", "Paz", false, true),
                3, new UserAccount(3, "luis@gestor.local", "Luis", "Luna", false, true)));

        assertThat(service.listProjects()).extracting(ProjectResult::projectId).containsExactly(10, 11);
    }

    @Test
    @DisplayName("US21-QS-04: el Personal no es Encargado de nadie y la lista queda vacia")
    void personalGetsNoProjects() {
        ProjectQueryService service = serviceWith(new Actor(3, false, true));
        when(projects.findManagedBy(3)).thenReturn(List.of());

        assertThat(service.listProjects()).isEmpty();
    }

    @Test
    @DisplayName("US21-QS-05: getProject con id ajeno devuelve 404, no 403 (AC-US2.1-02)")
    void getProjectOnForeignIdIsNotFound() {
        ProjectQueryService service = serviceWith(new Actor(2, false, true));
        when(projects.findById(10)).thenReturn(Optional.of(project(10, 3)));

        assertThatThrownBy(() -> service.getProject(10))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US21-QS-06: getProject inexistente tambien devuelve 404")
    void getProjectOnMissingIdIsNotFound() {
        ProjectQueryService service = serviceWith(new Actor(1, true, true));
        when(projects.findById(10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProject(10))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US21-QS-07: el Encargado abre su proyecto y su historial de plan")
    void managerReadsOwnProjectAndPlanHistory() {
        ProjectQueryService service = serviceWith(new Actor(2, false, true));
        when(projects.findById(10)).thenReturn(Optional.of(project(10, 2)));
        when(projects.allocatedBudget(any())).thenReturn(new BigDecimal("40.00"));
        when(users.findByIds(anyCollection())).thenReturn(Map.of(
                2, new UserAccount(2, "ana@gestor.local", "Ana", "Paz", false, true)));

        ProjectResult result = service.getProject(10);
        assertThat(result.projectId()).isEqualTo(10);
        assertThat(result.canEditDetails()).isTrue();

        when(projects.planHistory(10)).thenReturn(List.of(new PlanRevision(
                1, new BigDecimal("100.00"), 60, NOW, 2, "Plan inicial")));
        List<PlanRevisionResult> revisions = service.planHistory(10);
        assertThat(revisions).singleElement().satisfies(revision -> {
            assertThat(revision.revisionNumber()).isEqualTo(1);
            assertThat(revision.reason()).isEqualTo("Plan inicial");
        });
    }

    @Test
    @DisplayName("US21-QS-08: planHistory de proyecto ajeno devuelve 404")
    void planHistoryOnForeignProjectIsNotFound() {
        ProjectQueryService service = serviceWith(new Actor(2, false, true));
        when(projects.findById(10)).thenReturn(Optional.of(project(10, 3)));

        assertThatThrownBy(() -> service.planHistory(10))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US21-QS-09: solo el Jefe consulta los encargados delegables (AC-US2.1-04)")
    void eligibleManagersRequiresChief() {
        UserAccount worker = new UserAccount(5, "erin@gestor.local", "Erin", "Rosa", false, true);
        when(users.findActive()).thenReturn(List.of(worker));

        assertThatThrownBy(() -> serviceWith(new Actor(2, false, true)).eligibleManagers())
                .isInstanceOf(ForbiddenException.class);
        verify(users, never()).findActive();

        List<com.hardcoders.taskmanager.application.result.UserSummaryResult> eligible =
                serviceWith(new Actor(1, true, true)).eligibleManagers();
        assertThat(eligible).hasSize(1);
        assertThat(eligible.getFirst().fullName()).isEqualTo("Erin Rosa");
        assertThat(eligible.getFirst().chief()).isFalse();
    }
}
