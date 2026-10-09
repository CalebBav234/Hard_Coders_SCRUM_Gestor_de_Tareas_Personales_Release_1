package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.CreateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.UpdateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.port.in.ManageSubtasksUseCase;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.Workspace;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;
import com.hardcoders.taskmanager.domain.port.out.WorkspaceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Consumer;

/** Casos de uso de comandos sobre subtareas. */
public class SubtaskCommandService implements ManageSubtasksUseCase {

    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final WorkspaceRepository workspaces;
    private final CurrentActor actor;
    private final Clock clock;

    public SubtaskCommandService(TaskRepository tasks, SubtaskRepository subtasks, WorkspaceRepository workspaces,
                                 CurrentActor actor, Clock clock) {
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.workspaces = workspaces;
        this.actor = actor;
        this.clock = clock;
    }

    @Override
    public SubtaskResult create(CreateSubtaskCommand command) {
        Workspace workspace = workspaces.currentWorkspace(actor.userId());
        // Bloquear la tarea evita agregar subtareas mientras otra petición la elimina.
        Task task = tasks.findOpenForUpdate(workspace.projectId(), command.taskId())
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, command.taskId()));
        Subtask subtask = Subtask.create(task.id(), command.title(), command.description(), Instant.now(clock));
        return ResultMapper.toResult(subtasks.save(subtask));
    }

    @Override
    public SubtaskResult update(UpdateSubtaskCommand command) {
        return modify(command.subtaskId(), command.version(),
                subtask -> subtask.edit(command.title(), command.description()));
    }

    @Override
    public SubtaskResult start(VersionedCommand command) {
        return modify(command.id(), command.version(), Subtask::start);
    }

    @Override
    public SubtaskResult pause(VersionedCommand command) {
        return modify(command.id(), command.version(), Subtask::pause);
    }

    @Override
    public SubtaskResult complete(VersionedCommand command) {
        Integer actorId = actor.userId();
        return modify(command.id(), command.version(), subtask -> subtask.complete(actorId, Instant.now(clock)));
    }

    @Override
    public SubtaskResult reopen(VersionedCommand command) {
        return modify(command.id(), command.version(), Subtask::reopen);
    }

    @Override
    public void cancel(VersionedCommand command) {
        Subtask subtask = load(command.id());
        subtask.verifyVersion(command.version());
        subtask.cancel();
        subtasks.save(subtask);
    }

    private SubtaskResult modify(Integer subtaskId, Long version, Consumer<Subtask> change) {
        Subtask subtask = load(subtaskId);
        subtask.verifyVersion(version);
        change.accept(subtask);
        return ResultMapper.toResult(subtasks.save(subtask));
    }

    private Subtask load(Integer subtaskId) {
        Workspace workspace = workspaces.currentWorkspace(actor.userId());
        return subtasks.findOpenForUpdate(workspace.projectId(), subtaskId)
                .orElseThrow(() -> new NotFoundException(Subtask.ENTITY, subtaskId));
    }
}
