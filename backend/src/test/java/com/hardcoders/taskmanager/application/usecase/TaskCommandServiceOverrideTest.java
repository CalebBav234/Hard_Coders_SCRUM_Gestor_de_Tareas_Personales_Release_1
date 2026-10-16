package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.exception.VersionConflictException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
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
 * US1.3 — Override rules: caminos básicos de {@code TaskCommandService.transition},
 * {@code overrideReason} y {@code load} cuando el Jefe ejecuta trabajo ajeno.
 */
class TaskCommandServiceOverrideTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
    private InMemoryPorts ports;
    private TaskCommandService tasks;
    private TaskQueryService queries;

    @BeforeEach
    void setUp() {
        ports = new InMemoryPorts(clock);
        ports.seedProject("Proyecto A", InMemoryPorts.ACTOR, new BigDecimal("1000.00"));
        ports.seedStage(InMemoryPorts.PROJECT, "Etapa 1");
        ports.seedActivity(InMemoryPorts.STAGE, "Analisis");
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        tasks = new TaskCommandService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor, clock);
        queries = new TaskQueryService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor);
    }

    private TaskResult seedTask(String title) {
        return tasks.create(new com.hardcoders.taskmanager.application.command.CreateTaskCommand(
                InMemoryPorts.ACTIVITY, title, "MEDIA", BigDecimal.ZERO, 60));
    }

    @Test
    @DisplayName("US13-TS-01: el Jefe completa tarea ajena y audita el override (AC-US1.3-01)")
    void chiefCompletesForeignTaskAndRecordsOverrideReason() {
        TaskResult task = seedTask("Tarea ajena");
        ports.assign(task.taskId(), InMemoryPorts.WORKER, InMemoryPorts.ACTOR);

        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        TaskResult started = tasks.start(new TransitionCommand(task.taskId(), null, task.version()));
        TaskResult completed = tasks.complete(
                new TransitionCommand(task.taskId(), "cierre de release", started.version()));

        assertThat(completed.status()).isEqualTo("COMPLETADA");
        assertThat(completed.completedByUserId()).isEqualTo(InMemoryPorts.ACTOR);
        assertThat(queries.taskHistory(InMemoryPorts.PROJECT, "").getFirst().events())
                .extracting(event -> event.reason())
                .anyMatch(reason -> reason != null && reason.contains("Override del Jefe (US1.3)"));
    }

    @Test
    @DisplayName("US13-TS-02: sin motivo se registra 'sin motivo indicado' (AC-US1.3-04)")
    void chiefOverrideWithoutReasonRecordsDefaultMotive() {
        TaskResult task = seedTask("Tarea sin motivo");

        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        TaskResult started = tasks.start(new TransitionCommand(task.taskId(), null, task.version()));
        TaskResult completed = tasks.complete(
                new TransitionCommand(task.taskId(), "   ", started.version()));

        assertThat(completed.status()).isEqualTo("COMPLETADA");
        assertThat(queries.taskHistory(InMemoryPorts.PROJECT, "").getFirst().events())
                .extracting(event -> event.reason())
                .contains("Override del Jefe (US1.3): sin motivo indicado");
    }

    @Test
    @DisplayName("US13-TS-03: el personal asignado ejecuta con el motivo estandar, sin override")
    void assignedPersonalRunsWithStandardReason() {
        TaskResult task = seedTask("Tarea propia");
        ports.assign(task.taskId(), InMemoryPorts.WORKER, InMemoryPorts.ACTOR);

        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        TaskResult started = tasks.start(new TransitionCommand(task.taskId(), null, task.version()));
        TaskResult completed = tasks.complete(
                new TransitionCommand(task.taskId(), null, started.version()));

        assertThat(completed.completedByUserId()).isEqualTo(InMemoryPorts.WORKER);
        // El historial se consulta como Encargado del proyecto: el Personal no tiene visión de él.
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        assertThat(queries.taskHistory(InMemoryPorts.PROJECT, "").getFirst().events())
                .extracting(event -> event.reason())
                .contains("Ejecutado por el Personal asignado");
    }

    @Test
    @DisplayName("US13-TS-04: usuario sin asignación recibe 403 y la tarea no cambia (AC-US1.3-02)")
    void nonChiefWithoutAssignmentIsForbidden() {
        TaskResult task = seedTask("Tarea protegida");

        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        assertThatThrownBy(() -> tasks.complete(
                new TransitionCommand(task.taskId(), null, task.version())))
                .isInstanceOf(ForbiddenException.class);

        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        assertThat(ports.tasks.get(task.taskId()).status()).isEqualTo(WorkStatus.PENDIENTE);
    }

    @Test
    @DisplayName("US13-TS-05: el override del Jefe también cruza proyectos (B1 de load)")
    void chiefOverrideCrossesProjects() {
        Integer otherProject = ports.seedProject("Proyecto B", InMemoryPorts.WORKER,
                new BigDecimal("10.00")).id();
        Integer otherStage = ports.seedStage(otherProject, "Etapa B").id();
        Integer otherActivity = ports.seedActivity(otherStage, "Analisis B").id();
        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        TaskResult foreign = tasks.create(new com.hardcoders.taskmanager.application.command.CreateTaskCommand(
                otherActivity, "Tarea de B", "MEDIA", BigDecimal.ZERO, 60));

        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        TaskResult started = tasks.start(
                new TransitionCommand(foreign.taskId(), null, foreign.version()));
        TaskResult completed = tasks.complete(
                new TransitionCommand(foreign.taskId(), "override externo", started.version()));

        assertThat(completed.status()).isEqualTo("COMPLETADA");
    }

    @Test
    @DisplayName("US13-TS-06: versión obsoleta detiene el override sin tocar la tarea (AC-US1.3-03)")
    void staleVersionBlocksChiefOverride() {
        TaskResult task = seedTask("Tarea con versión");
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        TaskResult started = tasks.start(new TransitionCommand(task.taskId(), null, task.version()));

        assertThatThrownBy(() -> tasks.complete(
                new TransitionCommand(task.taskId(), "reintento", task.version())))
                .isInstanceOf(VersionConflictException.class);
        assertThat(ports.tasks.get(task.taskId()).version()).isEqualTo(started.version());
    }

    @Test
    @DisplayName("US13-TS-07: tarea cancelada es inexistente para el Jefe (B4 de load)")
    void canceledTaskIsNotFoundForChief() {
        TaskResult task = seedTask("Tarea cancelada");
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        tasks.cancel(new VersionedCommand(task.taskId(), task.version()));

        assertThatThrownBy(() -> tasks.start(
                new TransitionCommand(task.taskId(), null, ports.tasks.get(task.taskId()).version())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US13-TS-08: tarea inexistente devuelve 404 también al Jefe")
    void unknownTaskIsNotFound() {
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        assertThatThrownBy(() -> tasks.complete(new TransitionCommand(999, null, 1L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("US13-TS-09: el Jefe cancela la tarea de otro trabajador; quien no puede, recibe 403")
    void chiefCancelsTaskAndUnassignedWorkerIsRejected() {
        TaskResult task = seedTask("Tarea a cancelar");
        ports.assign(task.taskId(), InMemoryPorts.WORKER, InMemoryPorts.ACTOR);

        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        tasks.cancel(new VersionedCommand(task.taskId(), task.version()));
        assertThat(ports.tasks.get(task.taskId()).status()).isEqualTo(WorkStatus.CANCELADA);

        TaskResult second = seedTask("Segunda tarea");
        ports.currentActor = new Actor(9, false, true);
        assertThatThrownBy(() -> tasks.cancel(new VersionedCommand(second.taskId(), second.version())))
                .isInstanceOf(ForbiddenException.class);
    }
}
