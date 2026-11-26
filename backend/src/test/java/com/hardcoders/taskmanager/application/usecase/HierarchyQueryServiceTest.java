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
import java.util.Map;

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
        assertThat(stages).extracting(StageResult::activityCount).containsExactly(1);
        assertThat(activities).extracting(ActivityResult::name).containsExactly("Análisis");
        assertThat(activities).extracting(ActivityResult::taskCount).containsExactly(0);
    }

    @Test
    void reportsZeroActivityCountWhenStageHasNoActivities() {
        Integer emptyStage = ports.seedStage(InMemoryPorts.PROJECT, "Vacía").id();

        List<StageResult> stages = queries.stagesOf(InMemoryPorts.PROJECT);
        assertThat(stages).extracting(StageResult::stageId).contains(emptyStage);
        assertThat(stages).filteredOn(stage -> stage.stageId().equals(emptyStage))
                .extracting(StageResult::activityCount).containsExactly(0);
    }

    @Test
    void reportsTaskCountPerActivity() {
        ports.seedTask(activityId, "Tarea 1", InMemoryPorts.ACTOR);
        ports.seedTask(activityId, "Tarea 2", InMemoryPorts.ACTOR);

        List<ActivityResult> activities = queries.activitiesOf(stageId);
        assertThat(activities).filteredOn(activity -> activity.activityId().equals(activityId))
                .extracting(ActivityResult::taskCount).containsExactly(2);
    }

    @Test
    void countMethodsReturnEmptyMapForEmptyInput() {
        assertThat(ports.hierarchy.countActivitiesByStage(List.of())).isEmpty();
        assertThat(ports.hierarchy.countTasksByActivity(List.of())).isEmpty();
    }

    @Test
    void countsActivitiesByStageIgnoringActivitiesOfOtherStages() {
        Integer stageOne = ports.seedStage(InMemoryPorts.PROJECT, "Etapa uno").id();
        Integer stageTwo = ports.seedStage(InMemoryPorts.PROJECT, "Etapa dos").id();
        ports.seedActivity(stageOne, "Actividad 1.1");
        ports.seedActivity(stageOne, "Actividad 1.2");
        ports.seedActivity(stageTwo, "Actividad 2.1");

        Map<Integer, Integer> byBoth = ports.hierarchy.countActivitiesByStage(List.of(stageOne, stageTwo));
        assertThat(byBoth).containsExactly(Map.entry(stageOne, 2), Map.entry(stageTwo, 1));

        Map<Integer, Integer> byOne = ports.hierarchy.countActivitiesByStage(List.of(stageOne));
        assertThat(byOne).containsExactly(Map.entry(stageOne, 2));
    }

    @Test
    void countsTasksByActivity() {
        ports.seedTask(activityId, "Tarea 1", InMemoryPorts.ACTOR);
        ports.seedTask(activityId, "Tarea 2", InMemoryPorts.ACTOR);
        Integer otherActivity = ports.seedActivity(stageId, "Otra actividad").id();
        ports.seedTask(otherActivity, "Tarea 3", InMemoryPorts.ACTOR);

        Map<Integer, Integer> byActivity = ports.hierarchy.countTasksByActivity(List.of(activityId, otherActivity));
        assertThat(byActivity).containsExactly(Map.entry(activityId, 2), Map.entry(otherActivity, 1));
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
