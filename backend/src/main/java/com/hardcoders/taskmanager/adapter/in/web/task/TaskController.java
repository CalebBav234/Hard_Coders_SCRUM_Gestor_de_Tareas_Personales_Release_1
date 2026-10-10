package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.application.command.ChangeActivityCommand;
import com.hardcoders.taskmanager.application.command.CreateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.UpdateTaskPlanCommand;
import com.hardcoders.taskmanager.application.command.UpdateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.UpdateTaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.port.in.ManageSubtasksUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryTasksUseCase;
import com.hardcoders.taskmanager.application.result.AssignedTaskResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskHistoryResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Contrato HTTP de tareas y subtareas. Ninguna ruta deduce el proyecto: se indica
 * {@code projectId} (o la actividad/tarea concreta) y el servidor comprueba la visibilidad.
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final QueryTasksUseCase queryTasks;
    private final ManageTasksUseCase manageTasks;
    private final ManageSubtasksUseCase manageSubtasks;

    public TaskController(QueryTasksUseCase queryTasks, ManageTasksUseCase manageTasks,
                          ManageSubtasksUseCase manageSubtasks) {
        this.queryTasks = queryTasks;
        this.manageTasks = manageTasks;
        this.manageSubtasks = manageSubtasks;
    }

    // ------------------------------------------------------------------ tareas

    @GetMapping("/tasks")
    public List<TaskResponses.TaskResponse> listTasks(@RequestParam Integer projectId,
                                                      @RequestParam(required = false) String q) {
        return HierarchyController.toResponses(queryTasks.listTasks(projectId, q));
    }

    /** US3.1: "Mis tareas", tareas asignadas al usuario del token y sin datos financieros. */
    @GetMapping("/tasks/assigned")
    public List<TaskResponses.AssignedTaskResponse> assignedTasks(@RequestParam(required = false) String q) {
        return queryTasks.listAssignedTasks(q).stream()
                .map(TaskResponses.AssignedTaskResponse::from).toList();
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponses.TaskResponse getTask(@PathVariable Integer taskId) {
        return HierarchyController.toResponses(List.of(queryTasks.getTask(taskId))).getFirst();
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponses.TaskResponse> createTask(
            @Valid @RequestBody TaskRequests.CreateTaskRequest request) {
        TaskResult created = manageTasks.create(new CreateTaskCommand(
                request.activityId(), request.title(), request.priority(),
                request.allocatedBudgetBob(), request.estimatedDurationMinutes()));
        return ResponseEntity.created(URI.create("/api/tasks/" + created.taskId()))
                .body(TaskResponses.TaskResponse.from(created, created.assignedWorkerIds()));
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponses.TaskResponse updateTask(@PathVariable Integer taskId,
                                                  @Valid @RequestBody TaskRequests.UpdateTaskRequest request) {
        return TaskResponses.TaskResponse.from(manageTasks.update(
                new UpdateTaskCommand(taskId, request.title(), request.description(), request.priority(),
                        request.version())), List.of());
    }

    @PutMapping("/tasks/{taskId}/activity")
    public TaskResponses.TaskResponse changeActivity(
            @PathVariable Integer taskId,
            @Valid @RequestBody TaskRequests.ChangeActivityRequest request) {
        return TaskResponses.TaskResponse.from(manageTasks.changeActivity(
                new ChangeActivityCommand(taskId, request.activityId(), request.version())), List.of());
    }

    @PutMapping("/tasks/{taskId}/plan")
    public TaskResponses.TaskResponse updatePlan(
            @PathVariable Integer taskId,
            @Valid @RequestBody TaskRequests.UpdateTaskPlanRequest request) {
        return TaskResponses.TaskResponse.from(manageTasks.updatePlan(
                new UpdateTaskPlanCommand(taskId, request.allocatedBudgetBob(),
                        request.estimatedDurationMinutes(), request.reason(), request.version())), List.of());
    }

    @PostMapping("/tasks/{taskId}/start")
    public TaskResponses.TaskResponse start(@PathVariable Integer taskId,
                                            @Valid @RequestBody TaskRequests.TransitionRequest request) {
        return TaskResponses.TaskResponse.from(
                manageTasks.start(new TransitionCommand(taskId, request.reason(), request.version())), List.of());
    }

    @PostMapping("/tasks/{taskId}/pause")
    public TaskResponses.TaskResponse pause(@PathVariable Integer taskId,
                                            @Valid @RequestBody TaskRequests.TransitionRequest request) {
        return TaskResponses.TaskResponse.from(
                manageTasks.pause(new TransitionCommand(taskId, request.reason(), request.version())), List.of());
    }

    @PostMapping("/tasks/{taskId}/complete")
    public TaskResponses.TaskResponse complete(@PathVariable Integer taskId,
                                                @Valid @RequestBody TaskRequests.TransitionRequest request) {
        return TaskResponses.TaskResponse.from(
                manageTasks.complete(new TransitionCommand(taskId, request.reason(), request.version())), List.of());
    }

    @PostMapping("/tasks/{taskId}/reopen")
    public TaskResponses.TaskResponse reopen(@PathVariable Integer taskId,
                                              @Valid @RequestBody TaskRequests.TransitionRequest request) {
        return TaskResponses.TaskResponse.from(
                manageTasks.reopen(new TransitionCommand(taskId, request.reason(), request.version())), List.of());
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Integer taskId,
                                           @RequestParam Long version) {
        manageTasks.cancel(new VersionedCommand(taskId, version));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks/{taskId}/subtasks")
    public List<TaskResponses.SubtaskResponse> subtasks(@PathVariable Integer taskId) {
        return queryTasks.subtasksOfTask(taskId).stream()
                .map(TaskResponses.SubtaskResponse::from).toList();
    }

    // ---------------------------------------------------------------- subtareas

    @PostMapping("/tasks/{taskId}/subtasks")
    public ResponseEntity<TaskResponses.SubtaskResponse> createSubtask(
            @PathVariable Integer taskId,
            @Valid @RequestBody TaskRequests.CreateSubtaskRequest request) {
        SubtaskResult created = manageSubtasks.create(
                new CreateSubtaskCommand(taskId, request.title(), request.description()));
        return ResponseEntity.created(URI.create("/api/subtasks/" + created.subtaskId()))
                .body(TaskResponses.SubtaskResponse.from(created));
    }

    @PutMapping("/subtasks/{subtaskId}")
    public TaskResponses.SubtaskResponse updateSubtask(
            @PathVariable Integer subtaskId,
            @Valid @RequestBody TaskRequests.UpdateSubtaskRequest request) {
        return TaskResponses.SubtaskResponse.from(manageSubtasks.update(
                new UpdateSubtaskCommand(subtaskId, request.title(), request.description(), request.version())));
    }

    @PostMapping("/subtasks/{subtaskId}/start")
    public TaskResponses.SubtaskResponse startSubtask(
            @PathVariable Integer subtaskId,
            @Valid @RequestBody TaskRequests.TransitionRequest request) {
        return TaskResponses.SubtaskResponse.from(
                manageSubtasks.start(new TransitionCommand(subtaskId, request.reason(), request.version())));
    }

    @PostMapping("/subtasks/{subtaskId}/pause")
    public TaskResponses.SubtaskResponse pauseSubtask(
            @PathVariable Integer subtaskId,
            @Valid @RequestBody TaskRequests.TransitionRequest request) {
        return TaskResponses.SubtaskResponse.from(
                manageSubtasks.pause(new TransitionCommand(subtaskId, request.reason(), request.version())));
    }

    @PostMapping("/subtasks/{subtaskId}/complete")
    public TaskResponses.SubtaskResponse completeSubtask(
            @PathVariable Integer subtaskId,
            @Valid @RequestBody TaskRequests.TransitionRequest request) {
        return TaskResponses.SubtaskResponse.from(
                manageSubtasks.complete(new TransitionCommand(subtaskId, request.reason(), request.version())));
    }

    @PostMapping("/subtasks/{subtaskId}/reopen")
    public TaskResponses.SubtaskResponse reopenSubtask(
            @PathVariable Integer subtaskId,
            @Valid @RequestBody TaskRequests.TransitionRequest request) {
        return TaskResponses.SubtaskResponse.from(
                manageSubtasks.reopen(new TransitionCommand(subtaskId, request.reason(), request.version())));
    }

    @DeleteMapping("/subtasks/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(@PathVariable Integer subtaskId,
                                              @RequestParam Long version) {
        manageSubtasks.cancel(new VersionedCommand(subtaskId, version));
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ histórico

    @GetMapping("/tasks/history")
    public List<TaskResponses.TaskHistoryResponse> taskHistory(@RequestParam Integer projectId,
                                                               @RequestParam(required = false) String q) {
        return queryTasks.taskHistory(projectId, q).stream()
                .map(result -> TaskResponses.TaskHistoryResponse.from(result, result.task().assignedWorkerIds()))
                .toList();
    }

    @GetMapping("/subtasks/history")
    public List<TaskResponses.SubtaskResponse> subtaskHistory(@RequestParam Integer projectId,
                                                              @RequestParam(required = false) String q) {
        return queryTasks.subtaskHistory(projectId, q).stream()
                .map(TaskResponses.SubtaskResponse::from).toList();
    }

    @GetMapping("/activities")
    public List<TaskResponses.ActivityResponse> activities(@RequestParam Integer stageId) {
        return queryTasks.activities(stageId).stream()
                .map(TaskResponses.ActivityResponse::from).toList();
    }

    /** Tarea asignada concreta (US3.1). */
    @GetMapping("/assignments/tasks/{taskId}")
    public TaskResponses.AssignedTaskResponse assignedTask(@PathVariable Integer taskId) {
        return TaskResponses.AssignedTaskResponse.from(queryTasks.getAssignedTask(taskId));
    }
}
