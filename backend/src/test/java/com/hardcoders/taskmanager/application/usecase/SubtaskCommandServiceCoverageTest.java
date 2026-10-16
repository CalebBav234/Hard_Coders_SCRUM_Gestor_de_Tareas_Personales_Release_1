package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.CreateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.command.UpdateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.exception.VersionConflictException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Subtask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * US2.3-UC-01: cobertura de rutas de SubtaskCommandService con el arnés en memoria de puertos:
 * alta, edición, transiciones (Jefe y Personal asignado), cancelación y los caminos 403/404/412.
 */
class SubtaskCommandServiceCoverageTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);
    private final Integer personaNoAsignada = 77;

    private InMemoryPorts ports;
    private SubtaskCommandService subtasks;
    private Integer taskId;

    @BeforeEach
    void setUp() {
        ports = new InMemoryPorts(clock);
        ports.seedProject("Proyecto A", InMemoryPorts.ACTOR, new BigDecimal("1000.00"));
        ports.seedStage(InMemoryPorts.PROJECT, "Etapa 1");
        ports.seedActivity(InMemoryPorts.STAGE, "Analisis");
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        taskId = ports.seedTask(InMemoryPorts.ACTIVITY, "Informe", InMemoryPorts.ACTOR).id();
        subtasks = new SubtaskCommandService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor, clock);
    }

    @Test
    @DisplayName("US2.3-UC-01: el Jefe da de alta una subtarea PENDIENTE en su tarea")
    void chiefCreatesSubtask() {
        SubtaskResult created = subtasks.create(new CreateSubtaskCommand(taskId, "Pedir permisos", "Municipalidad"));

        assertThat(created.subtaskId()).isNotNull();
        assertThat(created.taskId()).isEqualTo(taskId);
        assertThat(created.title()).isEqualTo("Pedir permisos");
        assertThat(created.status()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("US2.3-UC-02: crear sobre una tarea inexistente devuelve 404")
    void createOnMissingTaskFails() {
        assertThatThrownBy(() -> subtasks.create(new CreateSubtaskCommand(999_999, "x", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US2.3-UC-03: el Personal ajeno al proyecto no puede dar de alta subtareas (403)")
    void createByUnrelatedPersonIsForbidden() {
        ports.currentActor = new Actor(personaNoAsignada, false, true);

        assertThatThrownBy(() -> subtasks.create(new CreateSubtaskCommand(taskId, "x", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("US2.3-UC-04: el Jefe edita el título de una subtarea existente")
    void chiefUpdatesSubtask() {
        Integer subtaskId = ports.seedSubtask(taskId, "Original").id();

        SubtaskResult updated = subtasks.update(new UpdateSubtaskCommand(subtaskId, "Nuevo título", "Detalle",
                currentVersion(subtaskId)));

        assertThat(updated.title()).isEqualTo("Nuevo título");
        assertThat(updated.description()).isEqualTo("Detalle");
    }

    @Test
    @DisplayName("US2.3-UC-05: editar una subtarea inexistente devuelve 404")
    void updateMissingSubtaskFails() {
        assertThatThrownBy(() -> subtasks.update(new UpdateSubtaskCommand(999_999, "x", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US2.3-UC-06: editar con una versión desactualizada lanza conflicto de versión (412)")
    void updateWithStaleVersionConflicts() {
        Integer subtaskId = ports.seedSubtask(taskId, "Original").id();

        assertThatThrownBy(() -> subtasks.update(new UpdateSubtaskCommand(subtaskId, "x", null, 1L)))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    @DisplayName("US2.3-UC-07: el Jefe recorre el ciclo iniciar→completar→reabrir→pausar")
    void chiefRunsFullTransitionCycle() {
        Integer subtaskId = ports.seedSubtask(taskId, "Subtarea").id();

        assertThat(subtasks.start(new TransitionCommand(subtaskId, null, currentVersion(subtaskId))).status())
                .isEqualTo("EN_PROGRESO");
        assertThat(subtasks.complete(new TransitionCommand(subtaskId, null, currentVersion(subtaskId))).status())
                .isEqualTo("COMPLETADA");
        assertThat(subtasks.reopen(new TransitionCommand(subtaskId, null, currentVersion(subtaskId))).status())
                .isEqualTo("EN_PROGRESO");
        assertThat(subtasks.pause(new TransitionCommand(subtaskId, null, currentVersion(subtaskId))).status())
                .isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("US2.3-UC-08: el Personal asignado inicia la subtarea con su propio actor")
    void assignedWorkerExecutesSubtask() {
        ports.assign(taskId, InMemoryPorts.WORKER, InMemoryPorts.ACTOR);
        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        Integer subtaskId = ports.seedSubtask(taskId, "Subtarea").id();

        SubtaskResult started = subtasks.start(new TransitionCommand(subtaskId, null, currentVersion(subtaskId)));

        assertThat(started.status()).isEqualTo("EN_PROGRESO");
        assertThat(subtasks.complete(new TransitionCommand(subtaskId, null, currentVersion(subtaskId))).status())
                .isEqualTo("COMPLETADA");
        assertThat(ports.subtasks.get(subtaskId).completedByUserId()).isEqualTo(InMemoryPorts.WORKER);
    }

    @Test
    @DisplayName("US2.3-UC-09: el Personal no asignado recibe 403, no 404 (AC-US3.2-01)")
    void unassignedWorkerIsForbidden() {
        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        Integer subtaskId = ports.seedSubtask(taskId, "Subtarea").id();

        assertThatThrownBy(() -> subtasks.start(new TransitionCommand(subtaskId, null, currentVersion(subtaskId))))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("US2.3-UC-10: el Jefe cancela la subtarea con su versión vigente")
    void chiefCancelsSubtask() {
        Integer subtaskId = ports.seedSubtask(taskId, "Subtarea").id();

        subtasks.cancel(new VersionedCommand(subtaskId, currentVersion(subtaskId)));

        assertThat(ports.subtasks.get(subtaskId).status()).isEqualTo(com.hardcoders.taskmanager.domain.model.WorkStatus.CANCELADA);
    }

    @Test
    @DisplayName("US2.3-UC-11: cancelar con una versión desactualizada lanza conflicto de versión (412)")
    void cancelWithStaleVersionConflicts() {
        Integer subtaskId = ports.seedSubtask(taskId, "Subtarea").id();

        assertThatThrownBy(() -> subtasks.cancel(new VersionedCommand(subtaskId, 1L)))
                .isInstanceOf(VersionConflictException.class);
    }

    private long currentVersion(Integer subtaskId) {
        Subtask subtask = ports.subtasks.get(subtaskId);
        return subtask.version();
    }
}
