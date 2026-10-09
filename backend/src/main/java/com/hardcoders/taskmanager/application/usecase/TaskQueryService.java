package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.port.in.QueryTasksUseCase;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskHistoryResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.SearchText;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.Workspace;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;
import com.hardcoders.taskmanager.domain.port.out.WorkspaceRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Consultas de solo lectura. Nunca crean el espacio de trabajo: si no existe, todo está vacío. */
public class TaskQueryService implements QueryTasksUseCase {

    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final WorkspaceRepository workspaces;

    public TaskQueryService(TaskRepository tasks, SubtaskRepository subtasks, WorkspaceRepository workspaces) {
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.workspaces = workspaces;
    }

    @Override
    public List<TaskResult> listTasks(String query) {
        Optional<Workspace> workspace = workspaces.findWorkspace();
        if (workspace.isEmpty()) {
            return List.of();
        }
        List<Task> found = tasks.findVisible(workspace.get().projectId(), pattern(query));
        Map<Integer, List<Subtask>> byTask = subtasks
                .findVisibleByTaskIds(found.stream().map(Task::id).toList())
                .stream()
                .collect(Collectors.groupingBy(Subtask::taskId));
        return found.stream()
                .map(task -> ResultMapper.toResult(task, byTask.getOrDefault(task.id(), List.of())))
                .toList();
    }

    @Override
    public TaskResult getTask(Integer taskId) {
        Task task = workspaces.findWorkspace()
                .flatMap(workspace -> tasks.findVisible(workspace.projectId(), taskId))
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        return ResultMapper.toResult(task, subtasks.findVisibleByTaskIds(List.of(taskId)));
    }

    @Override
    public List<TaskHistoryResult> taskHistory(String query) {
        return workspaces.findWorkspace()
                .map(workspace -> tasks.findArchived(workspace.projectId(), pattern(query)).stream()
                        .map(ResultMapper::toHistory)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public List<SubtaskResult> subtaskHistory(String query) {
        return workspaces.findWorkspace()
                .map(workspace -> subtasks.findArchived(workspace.projectId(), pattern(query)).stream()
                        .map(ResultMapper::toResult)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public List<ActivityResult> activities() {
        return workspaces.findWorkspace()
                .map(workspace -> workspaces.activities(workspace.stageId()).stream()
                        .filter(activity -> !activity.isUncategorized())
                        .map(ResultMapper::toResult)
                        .toList())
                .orElse(List.of());
    }

    private static String pattern(String query) {
        return SearchText.likePattern(query).orElse(null);
    }
}
