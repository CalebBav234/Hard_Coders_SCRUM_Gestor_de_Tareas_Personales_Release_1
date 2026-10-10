package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.CreateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.command.UpdateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.port.in.ManageSubtasksUseCase;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.policy.CompletionPolicy;
import com.hardcoders.taskmanager.domain.policy.WbsHierarchyPolicy;
import com.hardcoders.taskmanager.domain.port.out.AssignmentRepository;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.HierarchyRepository;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Comandos sobre subtareas (US2.2 alta, US3.2 ejecución, US1.3 excepción del Jefe).
 * La ejecución se autoriza contra la asignación de la tarea contenedora: V4 no guarda quién
 * ejecuta cada subtarea, solo {@code completed_by_user_id}, que se atribuye al usuario autenticado.
 */
public class SubtaskCommandService implements ManageSubtasksUseCase {

    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final HierarchyRepository hierarchy;
    private final ProjectRepository projects;
    private final AssignmentRepository assignments;
    private final CurrentActor actor;
    private final Clock clock;

    public SubtaskCommandService(TaskRepository tasks, SubtaskRepository subtasks, HierarchyRepository hierarchy,
                                 ProjectRepository projects, AssignmentRepository assignments, CurrentActor actor,
                                 Clock clock) {
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.hierarchy = hierarchy;
        this.projects = projects;
        this.assignments = assignments;
        this.actor = actor;
        this.clock = clock;
    }

    @Override
    public SubtaskResult create(CreateSubtaskCommand command) {
        Actor current = actor.current();
        Task task = loadManageableTask(current, command.taskId());
        Subtask subtask = Subtask.create(task.id(), command.title(), command.description(), Instant.now(clock));
        return ResultMapper.toResult(subtasks.save(subtask));
    }

    @Override
    public SubtaskResult update(UpdateSubtaskCommand command) {
        Actor current = actor.current();
        Subtask subtask = loadManageable(current, command.subtaskId());
        subtask.verifyVersion(command.version());
        subtask.edit(command.title(), command.description());
        return ResultMapper.toResult(subtasks.save(subtask));
    }

    @Override
    public SubtaskResult start(TransitionCommand command) {
        return execute(command, (subtask, reason) -> subtask.start(), "Subtarea iniciada");
    }

    @Override
    public SubtaskResult pause(TransitionCommand command) {
        return execute(command, (subtask, reason) -> subtask.pause(), "Subtarea pausada");
    }

    @Override
    public SubtaskResult complete(TransitionCommand command) {
        Integer actorId = actor.userId();
        Instant now = Instant.now(clock);
        return execute(command, (subtask, reason) -> subtask.complete(actorId, now), "Subtarea completada");
    }

    @Override
    public SubtaskResult reopen(TransitionCommand command) {
        return execute(command, (subtask, reason) -> subtask.reopen(), "Subtarea reabierta");
    }

    @Override
    public void cancel(VersionedCommand command) {
        Actor current = actor.current();
        Subtask subtask = load(current, command.id());
        subtask.verifyVersion(command.version());
        subtask.cancel();
        subtasks.save(subtask);
    }

    private SubtaskResult execute(TransitionCommand command, SubtaskAction change, String defaultReason) {
        Actor current = actor.current();
        Subtask subtask = load(current, command.id());
        subtask.verifyVersion(command.version());
        boolean assigned = isAssignedToTask(subtask.taskId(), current.userId());
        CompletionPolicy.requireCanComplete(current, assigned);
        change.accept(subtask, defaultReason);
        return ResultMapper.toResult(subtasks.save(subtask));
    }

    /** El Personal asignado a la tarea contenedora; el Jefe, en cualquier proyecto (US1.3). */
    private Subtask load(Actor current, Integer subtaskId) {
        Integer taskId = subtasks.taskIdOf(subtaskId)
                .orElseThrow(() -> new NotFoundException(Subtask.ENTITY, subtaskId));
        if (current.chief()) {
            return subtasks.findOpenForUpdate(tasks.projectIdOf(taskId).orElse(null), subtaskId)
                    .orElseThrow(() -> new NotFoundException(Subtask.ENTITY, subtaskId));
        }
        Optional<Subtask> assigned = subtasks.findForUpdate(taskId, subtaskId)
                .filter(subtask -> isAssignedToTask(taskId, current.userId()));
        if (assigned.isPresent()) {
            return assigned.get();
        }
        // La subtarea existe pero el actor no está asignado: 403 y no 404 (AC-US3.2-01).
        if (subtasks.taskIdOf(subtaskId).isPresent()) {
            throw new ForbiddenException("Solo el Personal asignado puede ejecutar esta tarea");
        }
        throw new NotFoundException(Subtask.ENTITY, subtaskId);
    }

    private Subtask loadManageable(Actor current, Integer subtaskId) {
        Subtask subtask = subtasks.findForUpdate(taskOf(subtaskId), subtaskId)
                .orElseThrow(() -> new NotFoundException(Subtask.ENTITY, subtaskId));
        requireManageableProject(current, subtask.taskId());
        return subtask;
    }

    private Task loadManageableTask(Actor current, Integer taskId) {
        Integer projectId = tasks.projectIdOf(taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        Task task = tasks.findVisible(projectId, taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        requireManageableProject(current, taskId);
        return task;
    }

    private void requireManageableProject(Actor current, Integer taskId) {
        Integer projectId = tasks.projectIdOf(taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
        WbsHierarchyPolicy.requireCanManage(current, project, "gestionar las subtareas de este proyecto");
    }

    private Integer taskOf(Integer subtaskId) {
        return subtasks.taskIdOf(subtaskId)
                .orElseThrow(() -> new NotFoundException(Subtask.ENTITY, subtaskId));
    }

    private boolean isAssignedToTask(Integer taskId, Integer userId) {
        return assignments.find(taskId, userId).isPresent();
    }

    /** Acción de transición sobre una subtarea. */
    private interface SubtaskAction {
        void accept(Subtask subtask, String reason);
    }
}
