package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.StageResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HierarchyQueryServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
    private InMemoryPorts ports;
    private HierarchyQueryService queries;
    private Integer stageId;
    private Integer activityId;

    @BeforeEach
    void setUp() {
        ports = new InMemoryPorts(clock);
        ports.seedProject("Proyecto A", InMemoryPorts.ACTOR, new BigDecimal("1000.00"));
        stageId = ports.seedStage(InMemoryPorts.PROJECT, "Etapa A").id();
        activityId = ports.seedActivity(stageId, "Análisis").id();
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, false, true);
        queries = new HierarchyQueryService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor);
    }

    @Test
    void returnsStagesAndActivitiesForTheProjectManager() {
        List<StageResult> stages = queries.stagesOf(InMemoryPorts.PROJECT);
        List<ActivityResult> activities = queries.activitiesOf(stageId);

        assertThat(stages).extracting(StageResult::name).containsExactly("Etapa A");
        assertThat(activities).extracting(ActivityResult::name).containsExactly("Análisis");
    }

    @Test
    void chiefCanReadAnyProjectAndEmptyHierarchyCollections() {
        ports.currentActor = new Actor(99, true, true);
        Integer anotherProject = ports.seedProject("Proyecto B", 88, new BigDecimal("500.00")).id();

        assertThat(queries.stagesOf(anotherProject)).isEmpty();
        assertThat(queries.activitiesOf(stageId)).extracting(ActivityResult::stageId).containsExactly(stageId);
        assertThat(queries.tasksOf(activityId)).isEmpty();
    }

    @Test
    void returnsOnlyTasksForTheRequestedActivityWithSubtasksAndAssignedWorkers() {
        Task selected = ports.seedTask(activityId, "Tarea seleccionada", InMemoryPorts.ACTOR);
        Subtask child = ports.seedSubtask(selected.id(), "Subtarea hija");
        ports.assign(selected.id(), InMemoryPorts.WORKER, InMemoryPorts.ACTOR);
        Integer otherActivity = ports.seedActivity(stageId, "Otra actividad").id();
        ports.seedTask(otherActivity, "Tarea distinta", InMemoryPorts.ACTOR);

        List<TaskResult> results = queries.tasksOf(activityId);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().title()).isEqualTo("Tarea seleccionada");
        assertThat(results.getFirst().assignedWorkerIds()).containsExactly(InMemoryPorts.WORKER);
        assertThat(results.getFirst().subtasks()).extracting(subtask -> subtask.subtaskId())
                .containsExactly(child.id());
    }

    @Test
    void hidesMissingForeignAndInactiveProjectAccessAsNotFound() {
        assertThatThrownBy(() -> queries.stagesOf(999)).isInstanceOf(NotFoundException.class);

        ports.currentActor = new Actor(77, false, true);
        assertThatThrownBy(() -> queries.stagesOf(InMemoryPorts.PROJECT)).isInstanceOf(NotFoundException.class);

        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, false);
        assertThatThrownBy(() -> queries.stagesOf(InMemoryPorts.PROJECT)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void missingStageOrActivityIsNotFound() {
        assertThatThrownBy(() -> queries.activitiesOf(999)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> queries.tasksOf(999)).isInstanceOf(NotFoundException.class);

        ports.hierarchy.save(Activity.restore(999, 888, "Sin etapa", null,
                com.hardcoders.taskmanager.domain.model.WorkStatus.PENDIENTE, clock.instant(), clock.instant()));
        assertThatThrownBy(() -> queries.tasksOf(999)).isInstanceOf(NotFoundException.class);
    }
}
