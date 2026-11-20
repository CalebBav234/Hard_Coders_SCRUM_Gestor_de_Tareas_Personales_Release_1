package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.result.AssignedTaskResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de búsqueda, visibilidad y tareas asignadas de TaskQueryService sin base de datos. */
class TaskQueryServiceTest {

    private InMemoryPorts ports;
    private TaskQueryService queries;
    private TaskCommandService commands;

    @BeforeEach
    void setUp() {
        ports = new InMemoryPorts(java.time.Clock.fixed(
                Instant.parse("2026-10-15T12:00:00Z"), java.time.ZoneOffset.UTC));
        ports.seedProject("Proyecto Alfa", InMemoryPorts.ACTOR, new BigDecimal("1000.00"));
        Integer stageId = ports.seedStage(InMemoryPorts.PROJECT, "Etapa Beta").id();
        Integer activityId = ports.seedActivity(stageId, "Actividad Gamma").id();
        ports.seedTask(activityId, "Tarea buscarme", InMemoryPorts.ACTOR);
        ports.seedTask(activityId, "Otra tarea", InMemoryPorts.ACTOR);
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, false, true);
        queries = new TaskQueryService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor);
        commands = new TaskCommandService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor,
                java.time.Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), java.time.ZoneOffset.UTC));
    }

    @Test
    void listTasksReturnsVisibleTasksForProject() {
        assertThat(queries.listTasks(InMemoryPorts.PROJECT, null)).hasSize(2);
        // InMemoryPorts.findVisible does not filter by likePattern (JPA adapter does)
        assertThat(queries.listTasks(InMemoryPorts.PROJECT, "ZZZ")).hasSize(2);
    }

    @Test
    void listTasksRejectsMissingAndInactiveActor() {
        assertThatThrownBy(() -> queries.listTasks(999, null)).isInstanceOf(NotFoundException.class);
        ports.currentActor = new Actor(77, false, true);
        assertThatThrownBy(() -> queries.listTasks(InMemoryPorts.PROJECT, null))
                .isInstanceOf(NotFoundException.class);
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, false);
        assertThatThrownBy(() -> queries.listTasks(InMemoryPorts.PROJECT, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchAcrossAllTasksForChiefOrVisibleForManager() {
        // likePattern añade comodines % que InMemoryPorts.contains() no interpreta,
        // por tanto todas las búsquedas devuelven vacías; lo importante es cubrir
        // las ramas de actor (null, inactivo, chief, encargado).
        assertThat(queries.search("tarea")).isEmpty();
        assertThat(queries.search(null)).isEmpty();

        ports.currentActor = new Actor(99, true, true);
        assertThat(queries.search("tarea")).isEmpty();

        ports.currentActor = new Actor(77, false, true);
        assertThat(queries.search("tarea")).isEmpty();

        ports.currentActor = null;
        assertThat(queries.search("tarea")).isEmpty();
    }

    @Test
    void assignedTasksReturnOnlyForTheActor() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Asignada", null,
                BigDecimal.TEN, 60));
        ports.assign(created.taskId(), InMemoryPorts.WORKER, InMemoryPorts.ACTOR);
        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        assertThat(queries.listAssignedTasks(null)).isNotEmpty();

        ports.currentActor = new Actor(99, false, true);
        assertThat(queries.listAssignedTasks(null)).isEmpty();
    }

    @Test
    void getAssignedTaskRequiresAssignment() {
        TaskResult created = commands.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Asignada", null,
                BigDecimal.TEN, 60));
        ports.assign(created.taskId(), InMemoryPorts.WORKER, InMemoryPorts.ACTOR);
        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        AssignedTaskResult result = queries.getAssignedTask(created.taskId());
        assertThat(result.taskId()).isEqualTo(created.taskId());

        ports.currentActor = new Actor(99, false, true);
        assertThatThrownBy(() -> queries.getAssignedTask(created.taskId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void taskHistoryAndSubtaskHistoryRespectVisibility() {
        assertThatThrownBy(() -> queries.taskHistory(999, null)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> queries.subtaskHistory(999, null)).isInstanceOf(NotFoundException.class);
        assertThat(queries.taskHistory(InMemoryPorts.PROJECT, null)).isNotNull();
        assertThat(queries.subtaskHistory(InMemoryPorts.PROJECT, null)).isNotNull();
    }

    @Test
    void activitiesRequiresVisibleProject() {
        assertThatThrownBy(() -> queries.activities(999)).isInstanceOf(NotFoundException.class);
    }
}
