package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.AssignmentCommands.AssignWorkerCommand;
import com.hardcoders.taskmanager.application.command.AssignmentCommands.UnassignWorkerCommand;
import com.hardcoders.taskmanager.application.port.in.ManageAssignmentsUseCase;
import com.hardcoders.taskmanager.application.result.TaskAssignmentResult;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.TaskAssignment;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.policy.AssignmentPolicy;
import com.hardcoders.taskmanager.domain.policy.WbsHierarchyPolicy;
import com.hardcoders.taskmanager.domain.port.out.AssignmentRepository;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * US2.3: asignación y revocación de Personal. Solo asigna el Encargado del proyecto o el Jefe;
 * el trabajador debe existir, estar activo y no puede asignarse a sí mismo. La duplicidad la
 * rechaza la clave compuesta de V004 (AC-US2.3-02) y la revocación surte efecto de inmediato
 * (AC-US2.3-04).
 */
public class AssignmentCommandService implements ManageAssignmentsUseCase {

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final AssignmentRepository assignments;
    private final UserDirectory users;
    private final CurrentActor currentActor;
    private final Clock clock;

    public AssignmentCommandService(TaskRepository tasks, ProjectRepository projects, AssignmentRepository assignments,
                                    UserDirectory users, CurrentActor currentActor, Clock clock) {
        this.tasks = tasks;
        this.projects = projects;
        this.assignments = assignments;
        this.users = users;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Override
    public TaskAssignmentResult assign(AssignWorkerCommand command) {
        Actor actor = currentActor.current();
        Task task = loadTask(command.taskId());
        Project project = projectOf(task);
        WbsHierarchyPolicy.requireOpen(project, "asignar Personal");
        AssignmentPolicy.requireCanAssign(actor, project);
        UserAccount worker = users.findById(command.workerUserId())
                .orElseThrow(() -> new DomainValidationException("El usuario indicado no existe"));
        AssignmentPolicy.requireEligibleWorker(worker);
        AssignmentPolicy.requireNotSelf(actor, worker);
        if (assignments.find(task.id(), worker.userId()).isPresent()) {
            throw new DomainValidationException("Ese Personal ya está asignado a la tarea");
        }
        String reason = command.reason() == null || command.reason().isBlank()
                ? "Asignación de Personal"
                : command.reason().strip();
        TaskAssignment saved = assignments.save(new TaskAssignment(task.id(), worker.userId(), Instant.now(clock),
                actor.userId()));
        return toResult(saved, task, worker, reason);
    }

    @Override
    public void unassign(UnassignWorkerCommand command) {
        Actor actor = currentActor.current();
        Task task = loadTask(command.taskId());
        AssignmentPolicy.requireCanAssign(actor, projectOf(task));
        if (assignments.find(task.id(), command.workerUserId()).isEmpty()) {
            throw new NotFoundException("Asignación", command.workerUserId());
        }
        assignments.delete(task.id(), command.workerUserId());
    }

    @Override
    public List<TaskAssignmentResult> assignmentsForTask(Integer taskId) {
        Actor actor = currentActor.current();
        Task task = loadTask(taskId);
        AssignmentPolicy.requireCanAssign(actor, projectOf(task));
        return toResults(task, assignments.findForTask(taskId));
    }

    @Override
    public List<UserSummaryResult> assignableWorkers(Integer projectId) {
        Actor actor = currentActor.current();
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
        AssignmentPolicy.requireCanAssign(actor, project);
        return users.findActive().stream().map(ProjectResultAssembler::summary).toList();
    }

    private Task loadTask(Integer taskId) {
        Integer projectId = tasks.projectIdOf(taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
        return tasks.findVisible(projectId, taskId)
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, taskId));
    }

    private Project projectOf(Task task) {
        Integer projectId = tasks.projectIdOf(task.id())
                .orElseThrow(() -> new NotFoundException(Task.ENTITY, task.id()));
        return projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
    }

    private List<TaskAssignmentResult> toResults(Task task, List<TaskAssignment> found) {
        Set<Integer> ids = found.stream()
                .flatMap(assignment -> java.util.stream.Stream.of(assignment.workerUserId(),
                        assignment.assignedByUserId()))
                .collect(Collectors.toSet());
        Map<Integer, UserAccount> accounts = users.findByIds(ids);
        return found.stream()
                .map(assignment -> new TaskAssignmentResult(
                        assignment.taskId(),
                        task.title(),
                        assignment.workerUserId(),
                        nameOf(accounts.get(assignment.workerUserId()), assignment.workerUserId()),
                        assignment.assignedAt(),
                        assignment.assignedByUserId(),
                        nameOf(accounts.get(assignment.assignedByUserId()), assignment.assignedByUserId()),
                        null))
                .toList();
    }

    private TaskAssignmentResult toResult(TaskAssignment assignment, Task task, UserAccount worker, String reason) {
        return new TaskAssignmentResult(assignment.taskId(), task.title(), assignment.workerUserId(),
                worker.fullName(), assignment.assignedAt(), assignment.assignedByUserId(),
                nameOf(users.findById(assignment.assignedByUserId()).orElse(null), assignment.assignedByUserId()),
                reason);
    }

    private static String nameOf(UserAccount account, Integer fallbackId) {
        return account == null ? "Usuario " + fallbackId : account.fullName();
    }
}
