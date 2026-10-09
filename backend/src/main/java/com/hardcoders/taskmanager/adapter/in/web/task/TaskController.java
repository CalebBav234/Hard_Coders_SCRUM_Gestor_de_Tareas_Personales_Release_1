package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.adapter.in.web.task.TaskRequests.ChangeActivityRequest;
import com.hardcoders.taskmanager.adapter.in.web.task.TaskRequests.CreateSubtaskRequest;
import com.hardcoders.taskmanager.adapter.in.web.task.TaskRequests.CreateTaskRequest;
import com.hardcoders.taskmanager.adapter.in.web.task.TaskRequests.UpdateTaskRequest;
import com.hardcoders.taskmanager.adapter.in.web.task.TaskRequests.VersionRequest;
import com.hardcoders.taskmanager.adapter.in.web.task.TaskResponses.SubtaskResponse;
import com.hardcoders.taskmanager.adapter.in.web.task.TaskResponses.TaskHistoryResponse;
import com.hardcoders.taskmanager.adapter.in.web.task.TaskResponses.TaskResponse;
import com.hardcoders.taskmanager.application.command.ChangeActivityCommand;
import com.hardcoders.taskmanager.application.command.CreateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.command.UpdateTaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.port.in.ManageSubtasksUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryTasksUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Adaptador HTTP de tareas. Solo traduce HTTP ⇄ puertos de entrada. */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final ManageTasksUseCase manageTasks;
    private final ManageSubtasksUseCase manageSubtasks;
    private final QueryTasksUseCase queries;

    public TaskController(ManageTasksUseCase manageTasks, ManageSubtasksUseCase manageSubtasks,
                          QueryTasksUseCase queries) {
        this.manageTasks = manageTasks;
        this.manageSubtasks = manageSubtasks;
        this.queries = queries;
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam(defaultValue = "") @Size(max = 160) String q) {
        return queries.listTasks(q).stream().map(TaskResponse::from).toList();
    }

    @GetMapping("/history")
    public List<TaskHistoryResponse> history(@RequestParam(defaultValue = "") @Size(max = 160) String q) {
        return queries.taskHistory(q).stream().map(TaskHistoryResponse::from).toList();
    }

    @GetMapping("/{taskId}")
    public TaskResponse get(@PathVariable Integer taskId) {
        return TaskResponse.from(queries.getTask(taskId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
        return TaskResponse.from(manageTasks.create(
                new CreateTaskCommand(request.title(), request.priority(), request.activityName())));
    }

    @PutMapping("/{taskId}")
    public TaskResponse update(@PathVariable Integer taskId, @Valid @RequestBody UpdateTaskRequest request) {
        return TaskResponse.from(manageTasks.update(new UpdateTaskCommand(
                taskId, request.title(), request.description(), request.priority(), request.version())));
    }

    @PutMapping("/{taskId}/activity")
    public TaskResponse changeActivity(@PathVariable Integer taskId,
                                       @Valid @RequestBody ChangeActivityRequest request) {
        return TaskResponse.from(manageTasks.changeActivity(
                new ChangeActivityCommand(taskId, request.activityName(), request.version())));
    }

    @PostMapping("/{taskId}/start")
    public TaskResponse start(@PathVariable Integer taskId, @Valid @RequestBody VersionRequest request) {
        return TaskResponse.from(manageTasks.start(new VersionedCommand(taskId, request.version())));
    }

    @PostMapping("/{taskId}/pause")
    public TaskResponse pause(@PathVariable Integer taskId, @Valid @RequestBody VersionRequest request) {
        return TaskResponse.from(manageTasks.pause(new VersionedCommand(taskId, request.version())));
    }

    @PostMapping("/{taskId}/complete")
    public TaskResponse complete(@PathVariable Integer taskId, @Valid @RequestBody VersionRequest request) {
        return TaskResponse.from(manageTasks.complete(new VersionedCommand(taskId, request.version())));
    }

    @PostMapping("/{taskId}/reopen")
    public TaskResponse reopen(@PathVariable Integer taskId, @Valid @RequestBody VersionRequest request) {
        return TaskResponse.from(manageTasks.reopen(new VersionedCommand(taskId, request.version())));
    }

    /** Equivale al borrado lógico anterior: la tarea pasa a CANCELADA y queda en el historial. */
    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Integer taskId, @RequestParam @PositiveOrZero Long version) {
        manageTasks.cancel(new VersionedCommand(taskId, version));
    }

    @PostMapping("/{taskId}/subtasks")
    @ResponseStatus(HttpStatus.CREATED)
    public SubtaskResponse createSubtask(@PathVariable Integer taskId,
                                         @Valid @RequestBody CreateSubtaskRequest request) {
        return SubtaskResponse.from(manageSubtasks.create(
                new CreateSubtaskCommand(taskId, request.title(), request.description())));
    }
}
