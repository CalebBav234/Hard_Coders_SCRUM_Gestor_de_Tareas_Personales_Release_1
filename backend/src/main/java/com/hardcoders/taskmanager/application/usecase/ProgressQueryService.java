package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.port.in.QueryProgressUseCase;
import com.hardcoders.taskmanager.application.result.ProgressNodeResult;
import com.hardcoders.taskmanager.application.result.ProjectProgressResult;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.ProgressNode;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.Stage;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.policy.ProgressPolicy;
import com.hardcoders.taskmanager.domain.policy.ProjectAccessPolicy;
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
 * US3.3: progreso en cascada. Solo cuenta trabajo hoja, así que una tarea con subtareas no se
 * duplica con ellas (AC-US3.3-01); un proyecto sin etapas ni tareas queda en 0 % (AC-US3.3-02);
 * el 100 % nunca cierra el proyecto por sí solo (AC-US3.3-04). Los contadores son {@code long}
 * y la suma va acotada para no desbordar (AC-US3.3-03).
 */
public class ProgressQueryService implements QueryProgressUseCase {

    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final HierarchyRepository hierarchy;
    private final ProjectRepository projects;
    private final CurrentActor currentActor;

    public ProgressQueryService(TaskRepository tasks, SubtaskRepository subtasks, HierarchyRepository hierarchy,
                                ProjectRepository projects, CurrentActor currentActor) {
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.hierarchy = hierarchy;
        this.projects = projects;
        this.currentActor = currentActor;
    }

    @Override
    public ProjectProgressResult progress(Integer projectId) {
        Actor actor = currentActor.current();
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
        if (!ProjectAccessPolicy.canView(actor, project)) {
            throw new NotFoundException(Project.ENTITY, projectId);
        }
        List<Task> projectTasks = tasks.findVisible(projectId, (String) null);
        Map<Integer, List<Subtask>> byTask = subtasks
                .findVisibleByTaskIds(projectTasks.stream().map(Task::id).toList())
                .stream()
                .collect(Collectors.groupingBy(Subtask::taskId));
        Map<Integer, List<Task>> tasksByActivity = projectTasks.stream()
                .collect(Collectors.groupingBy(task -> task.activity().id()));

        List<ProgressNode> stageNodes = new ArrayList<>();
        for (Stage stage : hierarchy.stagesOf(projectId)) {
            List<ProgressNode> activityNodes = new ArrayList<>();
            for (Activity activity : hierarchy.activitiesOf(stage.id())) {
                activityNodes.add(activityNode(activity, tasksByActivity.getOrDefault(activity.id(), List.of()),
                        byTask));
            }
            stageNodes.add(node(stage.id(), stage.name(), activityNodes));
        }
        ProgressNode root = node(null, project.name(), stageNodes);
        return new ProjectProgressResult(project.id(), project.name(), project.status().name(), project.budgetBob(),
                projects.allocatedBudget(project.id()), project.estimatedDurationMinutes(), toResult(root));
    }

    private ProgressNode activityNode(Activity activity, List<Task> tasksOfActivity,
                                      Map<Integer, List<Subtask>> byTask) {
        List<ProgressNode> taskNodes = new ArrayList<>();
        for (Task task : tasksOfActivity) {
            List<Subtask> ofTask = byTask.getOrDefault(task.id(), List.of());
            if (ofTask.isEmpty()) {
                // Sin subtareas la tarea es el trabajo hoja.
                taskNodes.add(new ProgressNode(task.id(), task.title(), leaf(task).completed(),
                        leaf(task).total(), true, List.of()));
            } else {
                List<ProgressNode> subtaskNodes = ofTask.stream()
                        .map(subtask -> new ProgressNode(subtask.id(), subtask.title(), leaf(subtask).completed(),
                                leaf(subtask).total(), true, List.of()))
                        .toList();
                taskNodes.add(node(task.id(), task.title(), subtaskNodes));
            }
        }
        return node(activity.id(), activity.name(), taskNodes);
    }

    /** Unidad de trabajo hoja: una subtarea, o una tarea sin subtareas. */
    private static Count leaf(Task task) {
        return WorkStatus.COMPLETADA == task.status() ? new Count(1, 1) : new Count(0, 1);
    }

    private static Count leaf(Subtask subtask) {
        return WorkStatus.COMPLETADA == subtask.status() ? new Count(1, 1) : new Count(0, 1);
    }

    private static ProgressNode node(Integer id, String name, List<ProgressNode> children) {
        long completed = 0;
        long total = 0;
        for (ProgressNode child : children) {
            completed = ProgressPolicy.add(completed, child.completed());
            total = ProgressPolicy.add(total, child.total());
        }
        return new ProgressNode(id, name, completed, total, children.isEmpty(), children);
    }

    private static ProgressNodeResult toResult(ProgressNode node) {
        List<ProgressNodeResult> childResults = node.children().stream()
                .map(ProgressQueryService::toResult)
                .toList();
        return new ProgressNodeResult(node.id(), node.name(), node.completed(), node.total(), node.leaf(),
                node.ratio(), childResults);
    }

    /** Contador completado/total de una unidad de trabajo hoja. */
    private record Count(long completed, long total) {
    }
}
