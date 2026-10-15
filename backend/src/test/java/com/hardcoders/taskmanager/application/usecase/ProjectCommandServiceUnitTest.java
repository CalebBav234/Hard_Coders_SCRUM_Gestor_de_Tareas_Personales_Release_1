package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.CreateProjectCommand;
import com.hardcoders.taskmanager.application.command.DelegateProjectCommand;
import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias aisladas de los casos de uso de US-1.1 y US-1.2.
 * Los puertos hexagonales son mocks: no se usa PostgreSQL ni Spring.
 */
class ProjectCommandServiceUnitTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final UserAccount CHIEF =
            new UserAccount(1, "jefe@gestor.local", "Julia", "Mendoza", true, true);
    private static final UserAccount MANAGER =
            new UserAccount(2, "enc1@gestor.local", "Ana", "Paz", false, true);
    private static final UserAccount OTHER_MANAGER =
            new UserAccount(3, "enc2@gestor.local", "Luis", "Vaca", false, true);

    private ProjectRepository projects;
    private UserDirectory users;
    private CurrentActor currentActor;
    private ProjectCommandService service;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        users = mock(UserDirectory.class);
        currentActor = mock(CurrentActor.class);
        service = new ProjectCommandService(projects, users, currentActor,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(currentActor.current()).thenReturn(new Actor(CHIEF.userId(), true, true));
        when(projects.allocatedBudget(anyInt())).thenReturn(new BigDecimal("0.00"));
        when(users.findByIds(anyCollection())).thenReturn(Map.of(
                CHIEF.userId(), CHIEF,
                MANAGER.userId(), MANAGER,
                OTHER_MANAGER.userId(), OTHER_MANAGER));
        when(projects.save(any(Project.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("US11-CS-01: Jefe crea proyecto con Encargado encontrado")
    void createPersistsProjectAndFirstRevision() {
        when(users.findById(MANAGER.userId())).thenReturn(Optional.of(MANAGER));

        ProjectResult result = service.create(new CreateProjectCommand(
                "Edificio A", "Obra", MANAGER.userId(), new BigDecimal("15000.50"), 600, null));

        assertThat(result.projectId()).isEqualTo(42);
        assertThat(result.manager().userId()).isEqualTo(MANAGER.userId());
        assertThat(result.currentRevision()).isEqualTo(1);
        ArgumentCaptor<Project> saved = ArgumentCaptor.forClass(Project.class);
        verify(projects).save(saved.capture());
        assertThat(saved.getValue().pendingRevisions()).singleElement()
                .extracting(revision -> revision.reason()).isEqualTo("Plan inicial");
    }

    @Test
    @DisplayName("US11-CS-02: managerUserId nulo recorre la rama nula y se rechaza")
    void createRejectsNullManagerWithoutCallingDirectory() {
        assertThatThrownBy(() -> service.create(new CreateProjectCommand(
                "Edificio A", null, null, new BigDecimal("10.00"), 60, null)))
                .isInstanceOf(DomainValidationException.class);

        verify(users, never()).findById(any());
        verify(projects, never()).save(any());
    }

    @Test
    @DisplayName("US11-CS-03: Encargado no puede crear proyectos")
    void createRejectsNonChiefBeforeUsingOutputPorts() {
        when(currentActor.current()).thenReturn(new Actor(MANAGER.userId(), false, true));

        assertThatThrownBy(() -> service.create(new CreateProjectCommand(
                "Edificio A", null, MANAGER.userId(), new BigDecimal("10.00"), 60, null)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(users);
        verify(projects, never()).save(any());
    }

    @Test
    @DisplayName("US11-CS-04: identificador de Encargado inexistente se rechaza")
    void createRejectsMissingManager() {
        when(users.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateProjectCommand(
                "Edificio A", null, 999, new BigDecimal("10.00"), 60, null)))
                .isInstanceOf(DomainValidationException.class);

        verify(projects, never()).save(any());
    }

    @Test
    @DisplayName("US12-DS-01: Jefe delega proyecto a Encargado activo")
    void delegateChangesManagerAndCreatesAuditRevision() {
        Project existing = existingProject();
        when(projects.findForUpdate(42)).thenReturn(Optional.of(existing));
        when(users.findById(OTHER_MANAGER.userId())).thenReturn(Optional.of(OTHER_MANAGER));
        when(users.findById(MANAGER.userId())).thenReturn(Optional.of(MANAGER));

        ProjectResult result = service.delegate(new DelegateProjectCommand(
                42, OTHER_MANAGER.userId(), "Reasignación por carga", existing.version()));

        assertThat(result.manager().userId()).isEqualTo(OTHER_MANAGER.userId());
        ArgumentCaptor<Project> saved = ArgumentCaptor.forClass(Project.class);
        verify(projects).save(saved.capture());
        assertThat(saved.getValue().pendingRevisions()).singleElement()
                .extracting(revision -> revision.reason())
                .asString().contains("Delegación de Ana Paz a Luis Vaca");
    }

    @Test
    @DisplayName("US12-DS-02: managerUserId nulo recorre la rama nula y se rechaza")
    void delegateRejectsNullManagerWithoutSaving() {
        Project existing = existingProject();
        when(projects.findForUpdate(42)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.delegate(new DelegateProjectCommand(
                42, null, "Reasignación", existing.version())))
                .isInstanceOf(DomainValidationException.class);

        verify(projects, never()).save(any());
    }

    @Test
    @DisplayName("US12-DS-03: Encargado no puede delegar")
    void delegateRejectsNonChief() {
        Project existing = existingProject();
        when(currentActor.current()).thenReturn(new Actor(MANAGER.userId(), false, true));
        when(projects.findForUpdate(42)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.delegate(new DelegateProjectCommand(
                42, OTHER_MANAGER.userId(), "Reasignación", existing.version())))
                .isInstanceOf(ForbiddenException.class);

        verify(projects, never()).save(any());
    }

    @Test
    @DisplayName("US12-DS-04: proyecto invisible o inexistente se responde como no encontrado")
    void delegateRejectsMissingProject() {
        when(projects.findForUpdate(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delegate(new DelegateProjectCommand(
                404, OTHER_MANAGER.userId(), "Reasignación", 1L)))
                .isInstanceOf(NotFoundException.class);

        verify(projects, never()).save(any());
    }

    private static Project existingProject() {
        return Project.restore(42, "Edificio A", "Obra", MANAGER.userId(), ProjectStatus.PLANIFICADO,
                new BigDecimal("15000.50"), 600, CHIEF.userId(), NOW.minusSeconds(3600), NOW, 1);
    }

    private static Project persisted(Project project) {
        return Project.restore(42, project.name(), project.description(), project.managerUserId(), project.status(),
                project.budgetBob(), project.estimatedDurationMinutes(), project.createdByUserId(),
                project.createdAt(), NOW.plusSeconds(1),
                project.lastRevisionNumber() + project.pendingRevisions().size());
    }
}
