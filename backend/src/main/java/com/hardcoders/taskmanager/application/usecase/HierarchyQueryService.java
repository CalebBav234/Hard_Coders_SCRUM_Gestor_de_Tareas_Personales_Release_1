package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.port.in.QueryHierarchyUseCase;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.StageResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.Stage;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.policy.ProjectAccessPolicy;
import com.hardcoders.taskmanager.domain.port.out.AssignmentRepository;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.HierarchyRepository;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * US2.1: lecturas de la jerarquía. El filtrado se hace en el servidor con el actor del token:
 * el Jefe ve cualquier proyecto, el Encargado solo los que dirige y el Personal ninguno.
 */
public class HierarchyQueryService implements QueryHierarchyUseCase {

    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final HierarchyRepository hierarchy;
    private final ProjectRepository projects;
    private final AssignmentRepository assignments;
    private final CurrentActor currentActor;

    public HierarchyQueryService(TaskRepository tasks, SubtaskRepository subtasks, HierarchyRepository hierarchy,
                                 ProjectRepository projects, AssignmentRepository assignments,
                                 CurrentActor currentActor) {
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.hierarchy = hierarchy;
        this.projects = projects;
        this.assignments = assignments;
        this.currentActor = currentActor;
    }

    @Override
    public List<StageResult> stagesOf(Integer projectId) {
        requireVisibleProject(currentActor.current(), projectId);
        return hierarchy.stagesOf(projectId).stream().map(HierarchyQueryService::toResult).toList();
    }

    @Override
    public List<ActivityResult> activitiesOf(Integer stageId) {
        Stage stage = hierarchy.findStage(stageId)
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, stageId));
        requireVisibleProject(currentActor.current(), stage.projectId());
        return hierarchy.activitiesOf(stageId).stream().map(ResultMapper::toResult).toList();
    }

    @Override
    public List<TaskResult> tasksOf(Integer activityId) {
        Activity activity = hierarchy.findActivity(activityId)
                .orElseThrow(() -> new NotFoundException(Activity.ENTITY, activityId));
        Stage stage = hierarchy.findStage(activity.stageId())
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, activity.stageId()));
        requireVisibleProject(currentActor.current(), stage.projectId());
        List<Task> found = tasks.findVisible(stage.projectId(), (String) null).stream()
                .filter(task -> task.activity().id().equals(activityId))
                .toList();
        Map<Integer, List<Subtask>> byTask = subtasks
                .findVisibleByTaskIds(found.stream().map(Task::id).toList())
                .stream()
                .collect(Collectors.groupingBy(Subtask::taskId));
        Map<Integer, List<Integer>> workers = assignments.findForTasks(
                        found.stream().map(Task::id).toList()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(a -> a.workerUserId()).toList()));
        return found.stream()
                .map(task -> ResultMapper.toResult(task, byTask.getOrDefault(task.id(), List.of()),
                        workers.getOrDefault(task.id(), List.of())))
                .toList();
    }

    private void requireVisibleProject(Actor actor, Integer projectId) {
        if (actor == null || !actor.active()) {
            throw new NotFoundException(Project.ENTITY, projectId);
        }
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
        if (!ProjectAccessPolicy.canView(actor, project)) {
            throw new NotFoundException(Project.ENTITY, projectId);
        }
    }

    private static StageResult toResult(Stage stage) {
        return new StageResult(stage.id(), stage.projectId(), stage.name(), stage.description(),
                stage.status().name(), stage.createdAt(), stage.updatedAt(), stage.version());
    }
}
