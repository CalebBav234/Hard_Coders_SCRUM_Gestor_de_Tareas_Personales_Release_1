package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.ChangeActivityCommand;
import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.UpdateTaskPlanCommand;
import com.hardcoders.taskmanager.application.command.UpdateTaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.Stage;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.TaskPriority;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.policy.AssignmentPolicy;
import com.hardcoders.taskmanager.domain.policy.BudgetPolicy;
import com.hardcoders.taskmanager.domain.policy.ProjectAccessPolicy;
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
import java.util.Locale;
import java.util.function.BiFunction;

public class TaskCommandService implements ManageTasksUseCase {

    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final HierarchyRepository hierarchy;
    private final ProjectRepository projects;
    private final AssignmentRepository assignments;
    private final CurrentActor currentActor;
    private final Clock clock;

    public TaskCommandService(TaskRepository tasks, SubtaskRepository subtasks, HierarchyRepository hierarchy,
                              ProjectRepository projects, AssignmentRepository assignments, CurrentActor currentActor,
                              Clock clock) {
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.hierarchy = hierarchy;
        this.projects = projects;
        this.assignments = assignments;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Override
    public TaskResult create(CreateTaskCommand command) {
        Actor actor = currentActor.current();
        Activity activity = hierarchy.findActivityForUpdate(command.activityId())
                .orElseThrow(() -> new NotFoundException(Activity.ENTITY, command.activityId()));
        Stage stage = hierarchy.findStage(activity.stageId())
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, activity.stageId()));
        Project project = projects.findForUpdate(stage.projectId())
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, stage.projectId()));
        WbsHierarchyPolicy.requireCanManage(actor, project, "crear tareas");
        Task task = Task.create(command.title(), priority(command.priority()), activity, actor.userId(), Instant.now(clock),
                command.allocatedBudgetBob(), command.estimatedDurationMinutes());
        return result(tasks.save(task));
    }

    @Override
    public TaskResult update(UpdateTaskCommand command) {
        Actor actor = currentActor.current();
        Integer projectId = tasks.projectIdOf(command.taskId()).orElse(null);
        Task task = tasks.findVisible(projectId, command.taskId())
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, command.taskId()));
        Project project = projectOfTask(task);
        if (!WbsHierarchyPolicy.canManage(actor, project)) {
            throw new ForbiddenException("No puedes gestionar este proyecto");
        }
        task.verifyVersion(command.version());
        task.edit(command.title(), command.description(), priority(command.priority()));
        return result(tasks.save(task));
    }

    @Override
    public TaskResult changeActivity(ChangeActivityCommand command) {
        Actor actor = currentActor.current();
        Integer projectId = tasks.projectIdOf(command.taskId()).orElse(null);
        Task task = tasks.findVisible(projectId, command.taskId())
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, command.taskId()));
        Project project = projectOfTask(task);
        if (!ProjectAccessPolicy.canManageHierarchy(actor, project)) {
            throw new ForbiddenException("No puedes gestionar este proyecto");
        }
        Activity target = hierarchy.findActivity(command.activityId())
                .orElseThrow(() -> new NotFoundException(Activity.ENTITY, command.activityId()));
        Stage targetStage = hierarchy.findStage(target.stageId())
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, target.stageId()));
        if (!targetStage.projectId().equals(project.id())) {
            throw new NotFoundException(Activity.ENTITY, command.activityId());
        }
        task.verifyVersion(command.version());
        task.moveTo(target);
        return result(tasks.save(task));
    }

    @Override
    public TaskResult updatePlan(UpdateTaskPlanCommand command) {
        Actor actor = currentActor.current();
        Integer projectId = tasks.projectIdOf(command.taskId()).orElse(null);
        Task task = tasks.findVisible(projectId, command.taskId())
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, command.taskId()));
        Project project = projectOfTask(task);
        if (!WbsHierarchyPolicy.canManage(actor, project)) {
            throw new ForbiddenException("No puedes gestionar este proyecto");
        }
        task.verifyVersion(command.version());
        BigDecimal alreadyAllocated = tasks.findVisible(project.id(), (String) null).stream()
                .filter(existing -> !existing.id().equals(task.id()))
                .map(t -> t.allocatedBudgetBob())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAllocated = alreadyAllocated.add(command.allocatedBudgetBob());
        BudgetPolicy.validateAllocationCap(project.budgetBob(), totalAllocated);
        task.replan(command.allocatedBudgetBob(), command.estimatedDurationMinutes(), command.reason(),
                actor.userId(), Instant.now(clock));
        return result(tasks.save(task));
    }

    @Override
    public TaskResult start(TransitionCommand command) {
        return transition(command, "Ejecutado por el Personal asignado",
                (task, r) -> { task.start(actorUserId(), Instant.now(clock), r); return result(tasks.save(task)); });
    }

    @Override
    public TaskResult pause(TransitionCommand command) {
        return transition(command, "Tarea pausada",
                (task, r) -> { task.pause(actorUserId(), Instant.now(clock), r); return result(tasks.save(task)); });
    }

    @Override
    public TaskResult complete(TransitionCommand command) {
        return transition(command, "Tarea completada",
                (task, r) -> { task.complete(actorUserId(), Instant.now(clock), r); return result(tasks.save(task)); });
    }

    @Override
    public TaskResult reopen(TransitionCommand command) {
        return transition(command, "Tarea reabierta",
                (task, r) -> { task.reopen(actorUserId(), Instant.now(clock), r); return result(tasks.save(task)); });
    }

    @Override
    public void cancel(VersionedCommand command) {
        Actor actor = currentActor.current();
        Integer projectId = tasks.projectIdOf(command.id()).orElse(null);
        Task task = tasks.findVisible(projectId, command.id())
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, command.id()));
        Project project = projectOfTask(task);
        if (!WbsHierarchyPolicy.canManage(actor, project)) {
            throw new ForbiddenException("No puedes gestionar este proyecto");
        }
        boolean hasOpenSubtasks = subtasks.findVisibleByTaskIds(List.of(task.id())).stream()
                .anyMatch(s -> s.status() != WorkStatus.CANCELADA && s.status() != WorkStatus.COMPLETADA);
        task.cancel(hasOpenSubtasks, actor.userId(), Instant.now(clock));
        tasks.save(task);
    }

    @Override
    public void reorderTasks(Integer activityId, List<Integer> taskIds) {
        Actor actor = currentActor.current();
        Activity activity = hierarchy.findActivity(activityId)
                .orElseThrow(() -> new NotFoundException(Activity.ENTITY, activityId));
        Stage stage = hierarchy.findStage(activity.stageId())
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, activity.stageId()));
        Project project = projects.findById(stage.projectId())
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, stage.projectId()));
        if (!ProjectAccessPolicy.canManageHierarchy(actor, project)) {
            throw new ForbiddenException("No puedes gestionar este proyecto");
        }
        tasks.reorderTasks(activityId, taskIds);
    }

    private TaskResult transition(TransitionCommand command, String defaultReason,
                                  BiFunction<Task, String, TaskResult> action) {
        Actor actor = currentActor.current();
        Integer projectId = tasks.projectIdOf(command.id()).orElse(null);
        Task task = tasks.findVisible(projectId, command.id())
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, command.id()));
        boolean assigned = assignments.find(task.id(), actor.userId()).isPresent();
        boolean canExecute = actor.chief() || assigned;
        if (!canExecute) {
            throw new ForbiddenException("No puedes ejecutar esta tarea");
        }
        task.verifyVersion(command.version());
        String reason = command.reason() == null || command.reason().isBlank()
                ? (actor.chief() && !assigned ? "sin motivo indicado" : defaultReason)
                : command.reason().strip();
        if (actor.chief() && !assigned) {
            reason = "Override del Jefe (US1.3): " + reason;
        }
        return action.apply(task, reason);
    }

    private Integer actorUserId() {
        return currentActor.current().userId();
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

    private static TaskPriority priority(String value) {
        try {
            return value == null ? TaskPriority.MEDIA : TaskPriority.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Prioridad desconocida: " + value);
        }
    }
}
