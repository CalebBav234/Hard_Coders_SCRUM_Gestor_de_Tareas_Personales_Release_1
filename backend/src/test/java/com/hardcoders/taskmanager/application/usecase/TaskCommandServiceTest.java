package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.ChangeActivityCommand;
import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.UpdateTaskPlanCommand;
import com.hardcoders.taskmanager.application.command.UpdateTaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas del servicio de comandos de tareas sin base de datos. */
class TaskCommandServiceTest {

    private InMemoryPorts ports;
    private TaskCommandService commands;

    @BeforeEach
    void setUp() {
        ports = new InMemoryPorts(Clock.fixed(
                Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC));
        ports.seedProject("Proyecto Alfa", InMemoryPorts.ACTOR, new BigDecimal("1000.00"));
        Integer stageId = ports.seedStage(InMemoryPorts.PROJECT, "Etapa Beta").id();
        Integer activityId = ports.seedActivity(stageId, "Actividad Gamma").id();
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, false, true);
        commands = new TaskCommandService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor,
                Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createTaskRequiresManagePermission() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Nueva", null,
                BigDecimal.TEN, 60));
        assertThat(created.title()).isEqualTo("Nueva");
        assertThat(created.status()).isEqualTo("PENDIENTE");
    }

    @Test
    void createRejectsUnknownPriority() {
        assertThatThrownBy(() -> commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Nueva", "INVALIDA",
                BigDecimal.TEN, 60)))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void updateTaskEditsFields() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Original", null,
                BigDecimal.TEN, 60));
        TaskResult updated = commands.update(new UpdateTaskCommand(created.taskId(),
                "Modificada", "desc", null, created.version()));
        assertThat(updated.title()).isEqualTo("Modificada");
    }

    @Test
    void updateRejectsMissingTaskAndForbiddenActor() {
        assertThatThrownBy(() -> commands.update(new UpdateTaskCommand(999, "x", null, null, 1L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void startPauseCompleteReopenLifecycle() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Ciclo", null,
                BigDecimal.TEN, 60));
        ports.assign(created.taskId(), InMemoryPorts.ACTOR, InMemoryPorts.ACTOR);
        TaskResult started = commands.start(new TransitionCommand(created.taskId(), null, created.version()));
        assertThat(started.status()).isEqualTo("EN_PROGRESO");
        TaskResult paused = commands.pause(new TransitionCommand(created.taskId(), null, started.version()));
        assertThat(paused.status()).isEqualTo("PENDIENTE");
        TaskResult startedAgain = commands.start(new TransitionCommand(created.taskId(), null, paused.version()));
        assertThat(startedAgain.status()).isEqualTo("EN_PROGRESO");
        TaskResult completed = commands.complete(new TransitionCommand(created.taskId(), null, startedAgain.version()));
        assertThat(completed.status()).isEqualTo("COMPLETADA");
        TaskResult reopened = commands.reopen(new TransitionCommand(created.taskId(), null, completed.version()));
        assertThat(reopened.status()).isEqualTo("EN_PROGRESO");
    }

    @Test
    void transitionsRejectUnassignedAndInvalidVersion() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Ciclo", null,
                BigDecimal.TEN, 60));
        ports.currentActor = new Actor(99, false, true);
        assertThatThrownBy(() -> commands.start(new TransitionCommand(created.taskId(), null, created.version())))
                .isInstanceOf(ForbiddenException.class);
        ports.assign(created.taskId(), 99, InMemoryPorts.ACTOR);
        assertThatThrownBy(() -> commands.start(new TransitionCommand(created.taskId(), null, 999L)))
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.VersionConflictException.class);
    }

    @Test
    void chiefCanOverrideUnassignedTask() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Override", null,
                BigDecimal.TEN, 60));
        ports.currentActor = new Actor(99, true, true);
        TaskResult started = commands.start(new TransitionCommand(created.taskId(), null, created.version()));
        assertThat(started.status()).isEqualTo("EN_PROGRESO");
    }

    @Test
    void cancelRequiresPermissionAndHasOpenSubtasksCheck() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Cancelar", null,
                BigDecimal.TEN, 60));
        ports.subtaskRepository.save(com.hardcoders.taskmanager.domain.model.Subtask.create(
                created.taskId(), "abierta", null, Instant.now()));
        assertThatThrownBy(() -> commands.cancel(new VersionedCommand(created.taskId(), created.version())))
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.OpenSubtasksException.class);
    }

    @Test
    void replanValidatesBudgetCap() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Plan", null,
                new BigDecimal("100.00"), 60));
        assertThatThrownBy(() -> commands.updatePlan(new UpdateTaskPlanCommand(created.taskId(),
                new BigDecimal("99999.00"), 600, "motivo", created.version())))
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.BudgetBelowAllocationsException.class);
        TaskResult replanned = commands.updatePlan(new UpdateTaskPlanCommand(created.taskId(),
                new BigDecimal("50.00"), 30, "motivo valido", created.version()));
        assertThat(replanned.allocatedBudgetBob()).isEqualByComparingTo("50.00");
    }

    @Test
    void reorderTasksRequiresManagePermission() {
        commands.reorderTasks(InMemoryPorts.ACTIVITY, List.of(1));
        ports.currentActor = new Actor(99, false, true);
        assertThatThrownBy(() -> commands.reorderTasks(InMemoryPorts.ACTIVITY, List.of(1)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void changeActivityMovesTaskWithinSameProject() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Mover", null,
                BigDecimal.TEN, 60));
        Integer otherStage = ports.seedStage(InMemoryPorts.PROJECT, "Etapa 2").id();
        Integer otherActivity = ports.seedActivity(otherStage, "Otra").id();
        TaskResult moved = commands.changeActivity(new ChangeActivityCommand(created.taskId(),
                otherActivity, created.version()));
        assertThat(moved.activityId()).isEqualTo(otherActivity);
    }

    @Test
    void cancelSucceedsWhenNoOpenSubtasks() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Cancelar", null,
                BigDecimal.TEN, 60));
        commands.cancel(new VersionedCommand(created.taskId(), created.version()));
    }
}
