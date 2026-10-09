package com.hardcoders.taskmanager.adapter.in.web.subtask;

import com.hardcoders.taskmanager.adapter.in.web.task.TaskRequests.UpdateSubtaskRequest;
import com.hardcoders.taskmanager.adapter.in.web.task.TaskRequests.VersionRequest;
import com.hardcoders.taskmanager.adapter.in.web.task.TaskResponses.SubtaskResponse;
import com.hardcoders.taskmanager.application.command.UpdateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.port.in.ManageSubtasksUseCase;
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

/** Adaptador HTTP de subtareas (las subtareas se crean desde {@code POST /api/tasks/{id}/subtasks}). */
@RestController
@RequestMapping("/api/subtasks")
public class SubtaskController {

    private final ManageSubtasksUseCase manageSubtasks;
    private final QueryTasksUseCase queries;

    public SubtaskController(ManageSubtasksUseCase manageSubtasks, QueryTasksUseCase queries) {
        this.manageSubtasks = manageSubtasks;
        this.queries = queries;
    }

    @GetMapping("/history")
    public List<SubtaskResponse> history(@RequestParam(defaultValue = "") @Size(max = 160) String q) {
        return queries.subtaskHistory(q).stream().map(SubtaskResponse::from).toList();
    }

    @PutMapping("/{subtaskId}")
    public SubtaskResponse update(@PathVariable Integer subtaskId,
                                  @Valid @RequestBody UpdateSubtaskRequest request) {
        return SubtaskResponse.from(manageSubtasks.update(new UpdateSubtaskCommand(
                subtaskId, request.title(), request.description(), request.version())));
    }

    @PostMapping("/{subtaskId}/start")
    public SubtaskResponse start(@PathVariable Integer subtaskId, @Valid @RequestBody VersionRequest request) {
        return SubtaskResponse.from(manageSubtasks.start(new VersionedCommand(subtaskId, request.version())));
    }

    @PostMapping("/{subtaskId}/pause")
    public SubtaskResponse pause(@PathVariable Integer subtaskId, @Valid @RequestBody VersionRequest request) {
        return SubtaskResponse.from(manageSubtasks.pause(new VersionedCommand(subtaskId, request.version())));
    }

    @PostMapping("/{subtaskId}/complete")
    public SubtaskResponse complete(@PathVariable Integer subtaskId, @Valid @RequestBody VersionRequest request) {
        return SubtaskResponse.from(manageSubtasks.complete(new VersionedCommand(subtaskId, request.version())));
    }

    @PostMapping("/{subtaskId}/reopen")
    public SubtaskResponse reopen(@PathVariable Integer subtaskId, @Valid @RequestBody VersionRequest request) {
        return SubtaskResponse.from(manageSubtasks.reopen(new VersionedCommand(subtaskId, request.version())));
    }

    @DeleteMapping("/{subtaskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Integer subtaskId, @RequestParam @PositiveOrZero Long version) {
        manageSubtasks.cancel(new VersionedCommand(subtaskId, version));
    }
}
