package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.port.in.QueryTasksUseCase;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.AssignedTaskResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskHistoryResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.SearchText;
import com.hardcoders.taskmanager.domain.model.Stage;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.TimeTracking;
import com.hardcoders.taskmanager.domain.policy.ProjectAccessPolicy;
import com.hardcoders.taskmanager.domain.port.out.AssignmentRepository;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.HierarchyRepository;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Consultas de tareas (US2.1, US3.1). Nunca se deduce el proyecto: se indica explícitamente y el
 * servidor comprueba que el actor pueda verlo. El Personal recibe una proyección sin datos
 * financieros (AC-US3.1-02) y solo de las tareas que tiene asignadas (AC-US3.1-01).
 */
public class TaskQueryService implements QueryTasksUseCase {

    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final HierarchyRepository hierarchy;
    private final ProjectRepository projects;
    private final AssignmentRepository assignments;
    private final CurrentActor currentActor;

    public TaskQueryService(TaskRepository tasks, SubtaskRepository subtasks, HierarchyRepository hierarchy,
                            ProjectRepository projects, AssignmentRepository assignments, CurrentActor currentActor) {
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.hierarchy = hierarchy;
        this.projects = projects;
        this.assignments = assignments;
        this.currentActor = currentActor;
    }

    @Override
    public List<TaskResult> listTasks(Integer projectId, String query) {
        Actor actor = currentActor.current();
        requireVisibleProject(actor, projectId);
        return toResults(tasks.findVisible(projectId, pattern(query)));
    }

    @Override
    public TaskResult getTask(Integer taskId) {
        Actor actor = currentActor.current();
        Task task = loadVisible(actor, taskId);
        List<Subtask> subtasksOfTask = subtasks.findVisibleByTaskIds(List.of(taskId));
        return ResultMapper.toResult(task, subtasksOfTask,
                assignments.findForTask(taskId).stream()
                        .map(assignment -> assignment.workerUserId()).toList());
    }

    @Override
    public List<TaskHistoryResult> taskHistory(Integer projectId, String query) {
        requireVisibleProject(currentActor.current(), projectId);
        return tasks.findArchived(projectId, pattern(query)).stream().map(ResultMapper::toHistory).toList();
    }

    @Override
    public List<SubtaskResult> subtaskHistory(Integer projectId, String query) {
        requireVisibleProject(currentActor.current(), projectId);
        return subtasks.findArchived(projectId, pattern(query)).stream().map(ResultMapper::toResult).toList();
    }

    @Override
    public List<ActivityResult> activities(Integer stageId) {
        Stage stage = hierarchy.findStage(stageId)
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, stageId));
        requireVisibleProject(currentActor.current(), stage.projectId());
        return hierarchy.activitiesOf(stageId).stream().map(ResultMapper::toResult).toList();
    }

    @Override
    public List<AssignedTaskResult> listAssignedTasks(String query) {
        Actor actor = currentActor.current();
        return assignedResults(tasks.findAssignedTo(actor.userId(), pattern(query)));
    }

    @Override
    public AssignedTaskResult getAssignedTask(Integer taskId) {
        Actor actor = currentActor.current();
        Integer projectId = tasks.projectIdOf(taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        Task task = tasks.findVisible(projectId, taskId)
                .filter(candidate -> isAssigned(taskId, actor.userId()))
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        return assignedResults(List.of(task)).getFirst();
    }

    @Override
    public List<SubtaskResult> subtasksOfTask(Integer taskId) {
        loadVisible(currentActor.current(), taskId);
        return subtasks.findVisibleByTaskIds(List.of(taskId)).stream().map(ResultMapper::toResult).toList();
    }

    private List<TaskResult> toResults(List<Task> found) {
        Map<Integer, List<Subtask>> byTask = subtasksOf(found);
        Map<Integer, List<Integer>> workers = workersOf(found);
        return found.stream()
                .map(task -> ResultMapper.toResult(task, byTask.getOrDefault(task.id(), List.of()),
                        workers.getOrDefault(task.id(), List.of())))
                .toList();
    }

    private Map<Integer, List<Integer>> workersOf(List<Task> found) {
        return assignments.findForTasks(found.stream().map(Task::id).toList()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(assignment -> assignment.workerUserId()).toList()));
    }

    private List<AssignedTaskResult> assignedResults(List<Task> found) {
        Map<Integer, List<Subtask>> byTask = subtasksOf(found);
        return found.stream().map(task -> toAssigned(task, byTask.getOrDefault(task.id(), List.of()))).toList();
    }

    private Map<Integer, List<Subtask>> subtasksOf(List<Task> found) {
        return subtasks.findVisibleByTaskIds(found.stream().map(Task::id).toList())
                .stream()
                .collect(Collectors.groupingBy(Subtask::taskId));
    }

    private static AssignedTaskResult toAssigned(Task task, List<Subtask> subtasksOfTask) {
        TimeTracking tracking = task.timeTracking();
        return new AssignedTaskResult(
                task.id(),
                null,
                task.activity().id(),
                task.activity().categoryName(),
                task.title(),
                task.description(),
                task.status().name(),
                task.priority().name(),
                task.actualDurationMinutes(),
                tracking.trackedSeconds(),
                tracking.currentSegmentStart(),
                task.completedAt(),
                task.createdAt(),
                task.updatedAt(),
                task.version(),
                subtasksOfTask.stream().map(ResultMapper::toResult).toList());
    }

    private Task loadVisible(Actor actor, Integer taskId) {
        Integer projectId = tasks.projectIdOf(taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        Optional<Task> visible = tasks.findVisible(projectId, taskId)
                .filter(task -> ProjectAccessPolicy.canView(actor, projectOf(projectId)));
        if (visible.isPresent() || actor.chief()) {
            return visible.orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        }
        return tasks.findVisible(projectId, taskId)
                .filter(task -> isAssigned(taskId, actor.userId()))
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
    }

    private boolean isAssigned(Integer taskId, Integer userId) {
        return assignments.find(taskId, userId).isPresent();
    }

    private Project projectOf(Integer projectId) {
        return projects.findById(projectId).orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
    }

    private void requireVisibleProject(Actor actor, Integer projectId) {
        if (actor == null || !actor.active()) {
            throw new NotFoundException(Project.ENTITY, projectId);
        }
        Project project = projectOf(projectId);
        if (!ProjectAccessPolicy.canView(actor, project)) {
            throw new NotFoundException(Project.ENTITY, projectId);
        }
    }

    private static String pattern(String query) {
        return SearchText.likePattern(query).orElse(null);
    }
}
