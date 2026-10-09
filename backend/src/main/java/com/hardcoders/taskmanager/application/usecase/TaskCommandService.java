package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.ChangeActivityCommand;
import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.command.UpdateTaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.TaskPriority;
import com.hardcoders.taskmanager.domain.model.WorkText;
import com.hardcoders.taskmanager.domain.model.Workspace;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;
import com.hardcoders.taskmanager.domain.port.out.WorkspaceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/** Casos de uso de comandos sobre tareas. Sin dependencias de framework: la transacción la aplica la configuración. */
public class TaskCommandService implements ManageTasksUseCase {

    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final WorkspaceRepository workspaces;
    private final CurrentActor actor;
    private final Clock clock;

    public TaskCommandService(TaskRepository tasks, SubtaskRepository subtasks, WorkspaceRepository workspaces,
                              CurrentActor actor, Clock clock) {
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.workspaces = workspaces;
        this.actor = actor;
        this.clock = clock;
    }

    @Override
    public TaskResult create(CreateTaskCommand command) {
        Integer actorId = actor.userId();
        Workspace workspace = workspaces.currentWorkspace(actorId);
        Activity activity = activityFor(workspace, command.activityName());
        Task task = Task.create(command.title(), TaskPriority.parseOrDefault(command.priority()),
                activity, actorId, Instant.now(clock));
        return result(tasks.save(task));
    }

    @Override
    public TaskResult update(UpdateTaskCommand command) {
        TaskPriority priority = TaskPriority.parseOrDefault(command.priority());
        return modify(command.taskId(), command.version(),
                task -> task.edit(command.title(), command.description(), priority));
    }

    @Override
    public TaskResult changeActivity(ChangeActivityCommand command) {
        Workspace workspace = workspaces.currentWorkspace(actor.userId());
        Activity activity = activityFor(workspace, command.activityName());
        return modify(command.taskId(), command.version(), task -> task.moveTo(activity));
    }

    @Override
    public TaskResult start(VersionedCommand command) {
        Integer actorId = actor.userId();
        return modify(command.id(), command.version(), task -> task.start(actorId, Instant.now(clock)));
    }

    @Override
    public TaskResult pause(VersionedCommand command) {
        Integer actorId = actor.userId();
        return modify(command.id(), command.version(), task -> task.pause(actorId, Instant.now(clock)));
    }

    @Override
    public TaskResult complete(VersionedCommand command) {
        Integer actorId = actor.userId();
        return modify(command.id(), command.version(), task -> task.complete(actorId, Instant.now(clock)));
    }

    @Override
    public TaskResult reopen(VersionedCommand command) {
        Integer actorId = actor.userId();
        return modify(command.id(), command.version(), task -> task.reopen(actorId, Instant.now(clock)));
    }

    @Override
    public void cancel(VersionedCommand command) {
        Integer actorId = actor.userId();
        Task task = load(actorId, command.id());
        task.verifyVersion(command.version());
        task.cancel(subtasks.hasOpenSubtasks(task.id()), actorId, Instant.now(clock));
        tasks.save(task);
    }

    private TaskResult modify(Integer taskId, Long version, Consumer<Task> change) {
        Task task = load(actor.userId(), taskId);
        task.verifyVersion(version);
        change.accept(task);
        return result(tasks.save(task));
    }

    private Task load(Integer actorId, Integer taskId) {
        Workspace workspace = workspaces.currentWorkspace(actorId);
        return tasks.findOpenForUpdate(workspace.projectId(), taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
    }

    private Activity activityFor(Workspace workspace, String requestedName) {
        String name = WorkText.activityName(requestedName);
        return workspaces.findOrCreateActivity(workspace.stageId(), name == null ? Activity.UNCATEGORIZED : name);
    }

    private TaskResult result(Task saved) {
        return ResultMapper.toResult(saved, subtasks.findVisibleByTaskIds(List.of(saved.id())));
    }
}
