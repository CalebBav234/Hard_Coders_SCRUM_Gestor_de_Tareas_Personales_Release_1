package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.AssignmentCommands.AssignWorkerCommand;
import com.hardcoders.taskmanager.application.command.AssignmentCommands.UnassignWorkerCommand;
import com.hardcoders.taskmanager.application.result.TaskAssignmentResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssignmentCommandServiceCoverageTest {

    private static final UserAccount MANAGER =
            new UserAccount(1, "encargado@gestor.local", "Ana", "Paz", false, true);
    private static final UserAccount WORKER =
            new UserAccount(2, "personal@gestor.local", "Pedro", "Rojas", false, true);
    private static final UserAccount INACTIVE =
            new UserAccount(5, "inactivo@gestor.local", "Luis", "Luna", false, false);

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
    private InMemoryPorts ports;
    private UserDirectory users;
    private AssignmentCommandService service;
    private Task task;

    @BeforeEach
    void setUp() {
        ports = new InMemoryPorts(clock);
        ports.seedProject("Proyecto A", MANAGER.userId(), new BigDecimal("1000.00"));
        Integer stage = ports.seedStage(InMemoryPorts.PROJECT, "Etapa").id();
        Integer activity = ports.seedActivity(stage, "Actividad").id();
        task = ports.seedTask(activity, "Tarea", MANAGER.userId());
        ports.currentActor = new Actor(MANAGER.userId(), false, true);
        users = mock(UserDirectory.class);
        when(users.findById(MANAGER.userId())).thenReturn(Optional.of(MANAGER));
        when(users.findById(WORKER.userId())).thenReturn(Optional.of(WORKER));
        when(users.findById(INACTIVE.userId())).thenReturn(Optional.of(INACTIVE));
        when(users.findByIds(anyCollection())).thenReturn(Map.of(
                MANAGER.userId(), MANAGER, WORKER.userId(), WORKER));
        when(users.findActive()).thenReturn(List.of(MANAGER, WORKER));
        service = new AssignmentCommandService(ports.taskRepository, ports.projectRepository,
                ports.assignmentRepository, users, ports.actor, clock);
    }

    @Test
    void assignsWithDefaultReasonAndListsWorkerAndManagerNames() {
        TaskAssignmentResult assigned = service.assign(
                new AssignWorkerCommand(task.id(), WORKER.userId(), "  "));

        assertThat(assigned.reason()).isEqualTo("Asignación de Personal");
        assertThat(assigned.workerName()).isEqualTo("Pedro Rojas");
        assertThat(assigned.assignedByName()).isEqualTo("Ana Paz");
        assertThat(service.assignmentsForTask(task.id()))
                .extracting(TaskAssignmentResult::workerName).containsExactly("Pedro Rojas");
    }

    @Test
    void trimsAnExplicitReasonAndRejectsDuplicateAssignment() {
        TaskAssignmentResult assigned = service.assign(
                new AssignWorkerCommand(task.id(), WORKER.userId(), "  Refuerzo  "));
        assertThat(assigned.reason()).isEqualTo("Refuerzo");

        assertThatThrownBy(() -> service.assign(
                new AssignWorkerCommand(task.id(), WORKER.userId(), "Duplicada")))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void listsOnlyActiveAssignableAccountsAndCanRemoveAnAssignment() {
        assertThat(service.assignableWorkers(InMemoryPorts.PROJECT))
                .extracting(worker -> worker.userId())
                .containsExactly(MANAGER.userId(), WORKER.userId());

        service.assign(new AssignWorkerCommand(task.id(), WORKER.userId(), null));
        service.unassign(new UnassignWorkerCommand(task.id(), WORKER.userId(), null));
        assertThat(service.assignmentsForTask(task.id())).isEmpty();
    }

    @Test
    void reportsMissingTaskAssignmentAndProject() {
        assertThatThrownBy(() -> service.assign(new AssignWorkerCommand(999, WORKER.userId(), null)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.unassign(new UnassignWorkerCommand(task.id(), WORKER.userId(), null)))
                .isInstanceOf(NotFoundException.class);

        ports.projects.remove(InMemoryPorts.PROJECT);
        assertThatThrownBy(() -> service.assignableWorkers(InMemoryPorts.PROJECT))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsAssignmentOperationsForNonManagersAndInactiveWorkers() {
        assertThatThrownBy(() -> service.assign(
                new AssignWorkerCommand(task.id(), INACTIVE.userId(), null)))
                .isInstanceOf(DomainValidationException.class);

        ports.currentActor = new Actor(WORKER.userId(), false, true);
        assertThatThrownBy(() -> service.assignableWorkers(InMemoryPorts.PROJECT))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.assignmentsForTask(task.id()))
                .isInstanceOf(ForbiddenException.class);
    }
}
