package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.ChangeActivityCommand;
import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.UpdateTaskPlanCommand;
import com.hardcoders.taskmanager.application.command.UpdateTaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.BudgetBelowAllocationsException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.Stage;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.TaskPriority;
import com.hardcoders.taskmanager.domain.policy.CompletionPolicy;
import com.hardcoders.taskmanager.domain.policy.WbsHierarchyPolicy;
import com.hardcoders.taskmanager.domain.port.out.AssignmentRepository;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.HierarchyRepository;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Casos de uso de comandos sobre tareas (US2.2, US2.3, US1.3, US3.2).
 *
 * <p>Permisos, comprobados en el servidor en cada operación:</p>
 * <ul>
 *   <li>crear, editar, mover y fijar presupuesto/duración: Encargado del proyecto o Jefe;</li>
 *   <li>iniciar, pausar, completar y reabrir: el Personal asignado, o el Jefe como excepción
 *       autorizada (US1.3) con el motivo registrado en el historial;</li>
 *   <li>cancelar: el Encargado del proyecto, el Jefe o el Personal asignado.</li>
 * </ul>
 */
public class TaskCommandService implements ManageTasksUseCase {

    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final HierarchyRepository hierarchy;
    private final ProjectRepository projects;
    private final AssignmentRepository assignments;
    private final CurrentActor actor;
    private final Clock clock;

    public TaskCommandService(TaskRepository tasks, SubtaskRepository subtasks, HierarchyRepository hierarchy,
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
    public TaskResult create(CreateTaskCommand command) {
        Actor current = actor.current();
        Activity activity = hierarchy.findActivity(command.activityId())
                .orElseThrow(() -> new NotFoundException(Activity.ENTITY, command.activityId()));
        Project project = requireManageableProject(current, activity.stageId());
        WbsHierarchyPolicy.requireOpen(project, "crear tareas");
        Task task = Task.create(command.title(), TaskPriority.parseOrDefault(command.priority()), activity,
                current.userId(), Instant.now(clock), command.allocatedBudgetBob(),
                command.estimatedDurationMinutes());
        return result(tasks.save(task));
    }

    @Override
    public TaskResult update(UpdateTaskCommand command) {
        Actor current = actor.current();
        Task task = loadManageable(current, command.taskId());
        task.verifyVersion(command.version());
        task.edit(command.title(), command.description(), TaskPriority.parseOrDefault(command.priority()));
        return result(tasks.save(task));
    }

    @Override
    public TaskResult changeActivity(ChangeActivityCommand command) {
        Actor current = actor.current();
        Task task = loadManageable(current, command.taskId());
        task.verifyVersion(command.version());
        Activity target = hierarchy.findActivity(command.activityId())
                .orElseThrow(() -> new NotFoundException(Activity.ENTITY, command.activityId()));
        Stage stage = hierarchy.findStage(target.stageId())
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, target.stageId()));
        WbsHierarchyPolicy.requireSameProject(projectOfTask(task).id(), stage.projectId(), Activity.ENTITY);
        WbsHierarchyPolicy.requireOpen(projectOfTask(task), "mover tareas");
        task.moveTo(target);
        return result(tasks.save(task));
    }

    @Override
    public TaskResult updatePlan(UpdateTaskPlanCommand command) {
        Actor current = actor.current();
        Task task = loadManageable(current, command.taskId());
        task.verifyVersion(command.version());
        Project project = projectOfTask(task);
        WbsHierarchyPolicy.requireOpen(project, "cambiar el plan de la tarea");
        BigDecimal newBudget = command.allocatedBudgetBob() == null
                ? task.allocatedBudgetBob()
                : command.allocatedBudgetBob();
        Integer newMinutes = command.estimatedDurationMinutes() == null
                ? task.estimatedDurationMinutes()
                : command.estimatedDurationMinutes();
        // AC-US1.1-04: la suma de lo asignado a las tareas no puede superar el presupuesto del proyecto.
        BigDecimal totalAfterChange = projects.allocatedBudget(project.id())
                .subtract(task.allocatedBudgetBob())
                .add(newBudget == null ? BigDecimal.ZERO : newBudget);
        if (totalAfterChange.compareTo(project.budgetBob()) > 0) {
            throw new BudgetBelowAllocationsException(project.budgetBob(), totalAfterChange);
        }
        task.replan(newBudget, newMinutes, command.reason(), current.userId(), Instant.now(clock));
        return result(tasks.save(task));
    }

    @Override
    public TaskResult start(TransitionCommand command) {
        return transition(command, (task, reason) -> task.start(actor.userId(), Instant.now(clock), reason));
    }

    @Override
    public TaskResult pause(TransitionCommand command) {
        return transition(command, (task, reason) -> task.pause(actor.userId(), Instant.now(clock), reason));
    }

    @Override
    public TaskResult complete(TransitionCommand command) {
        return transition(command, (task, reason) -> task.complete(actor.userId(), Instant.now(clock), reason));
    }

    @Override
    public TaskResult reopen(TransitionCommand command) {
        return transition(command, (task, reason) -> task.reopen(actor.userId(), Instant.now(clock), reason));
    }

    @Override
    public void cancel(VersionedCommand command) {
        Actor current = actor.current();
        Task task = load(current, command.id());
        task.verifyVersion(command.version());
        boolean allowed = current.chief() || isAssigned(task.id(), current.userId());
        if (!allowed) {
            throw new ForbiddenException("Solo el Personal asignado puede ejecutar esta tarea");
        }
        task.cancel(subtasks.hasOpenSubtasks(task.id()), current.userId(), Instant.now(clock));
        tasks.save(task);
    }

    /**
     * Ejecuta una transición de estado comprobando antes que el actor pueda ejecutar la tarea:
     * Personal asignado o Jefe (excepción US1.3). El motivo del Jefe queda en el historial.
     */
    private TaskResult transition(TransitionCommand command, Transition change) {
        Actor current = actor.current();
        Task task = load(current, command.id());
        task.verifyVersion(command.version());
        boolean assigned = isAssigned(task.id(), current.userId());
        CompletionPolicy.requireCanComplete(current, assigned);
        String reason = current.chief() && !assigned
                ? overrideReason(command.reason())
                : "Ejecutado por el Personal asignado";
        change.accept(task, reason);
        return result(tasks.save(task));
    }

    private static String overrideReason(String reason) {
        String motive = reason == null || reason.isBlank() ? "sin motivo indicado" : reason.strip();
        return "Override del Jefe (US1.3): " + motive;
    }

    private boolean isAssigned(Integer taskId, Integer userId) {
        return assignments.find(taskId, userId).isPresent();
    }

    private Task load(Actor current, Integer taskId) {
        Integer projectId = tasks.projectIdOf(taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        if (current.chief()) {
            // US1.3: el Jefe ejecuta en cualquier proyecto, con el motivo registrado.
            return tasks.findVisible(projectId, taskId)
                    .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        }
        // US3.2: el Personal solo actúa sobre el trabajo que tiene asignado.
        Optional<Task> assigned = tasks.findAssignedForUpdate(current.userId(), taskId);
        if (assigned.isPresent()) {
            return assigned.get();
        }
        // Si la tarea existe pero no es suya, la respuesta es 403 y no 404 (AC-US3.2-01): el actor
        // está autenticado y se le dice que no tiene permiso, sin filtrar datos del proyecto.
        if (tasks.findVisible(projectId, taskId).isPresent()) {
            throw new ForbiddenException("Solo el Personal asignado puede ejecutar esta tarea");
        }
        throw new NotFoundException(Task.ENTITY, taskId);
    }

    private Task loadManageable(Actor current, Integer taskId) {
        Integer projectId = tasks.projectIdOf(taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        Task task = tasks.findVisible(projectId, taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
        WbsHierarchyPolicy.requireCanManage(current, project, "editar esta tarea");
        return task;
    }

    private Project requireManageableProject(Actor current, Integer stageId) {
        Stage stage = hierarchy.findStage(stageId)
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, stageId));
        Project project = projects.findById(stage.projectId())
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, stage.projectId()));
        WbsHierarchyPolicy.requireCanManage(current, project, "gestionar este proyecto");
        return project;
    }

    private Project projectOfTask(Task task) {
        Integer projectId = tasks.projectIdOf(task.id())
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, task.id()));
        return projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
    }

    private TaskResult result(Task saved) {
        return ResultMapper.toResult(saved, subtasks.findVisibleByTaskIds(List.of(saved.id())),
                assignments.findForTask(saved.id()).stream()
                        .map(assignment -> assignment.workerUserId()).toList());
    }

    /** Acción de transición parametrizada con el motivo que se registra en el historial. */
    private interface Transition {
        void accept(Task task, String reason);
    }
}
