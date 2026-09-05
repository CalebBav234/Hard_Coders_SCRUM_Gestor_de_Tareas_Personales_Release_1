package com.hardcoders.taskmanager.controller;

import com.hardcoders.taskmanager.dto.ChangeCategoryRequest;
import com.hardcoders.taskmanager.dto.CreateTaskRequest;
import com.hardcoders.taskmanager.dto.TaskResponse;
import com.hardcoders.taskmanager.dto.TaskHistoryResponse;
import com.hardcoders.taskmanager.dto.UpdateTaskRequest;
import com.hardcoders.taskmanager.dto.VersionRequest;
import com.hardcoders.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.hardcoders.taskmanager.dto.CreateTaskRelationRequest;
import com.hardcoders.taskmanager.dto.TaskRelationResponse;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(request.title(), request.priority(), request.categoryName(), request.parentTaskId());
    }

    @GetMapping
    public List<TaskResponse> list(
            @RequestParam(defaultValue = "") @Size(max = 160) String q) {
        return taskService.listTasks(q);
    }

    @GetMapping("/history")
    public List<TaskHistoryResponse> history(
            @RequestParam(defaultValue = "") @Size(max = 160) String q) {
        return taskService.listHistory(q);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam @PositiveOrZero Long version) {
        taskService.delete(id, version);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<TaskResponse> activate(
            @PathVariable Long id,
            @Valid @RequestBody VersionRequest request) {
        return ResponseEntity.ok(taskService.activate(id, request.version()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<TaskResponse> complete(
            @PathVariable Long id,
            @Valid @RequestBody VersionRequest request) {
        return ResponseEntity.ok(taskService.complete(id, request.version()));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<TaskResponse> reopen(
            @PathVariable Long id,
            @Valid @RequestBody VersionRequest request) {
        return ResponseEntity.ok(taskService.reopen(id, request.version()));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<TaskResponse> pause(
            @PathVariable Long id,
            @Valid @RequestBody VersionRequest request) {
        return ResponseEntity.ok(taskService.pause(id, request.version()));
    }

    @PutMapping("/{id}/category")
    public ResponseEntity<TaskResponse> changeCategory(
            @PathVariable Long id,
            @Valid @RequestBody ChangeCategoryRequest request) {
        return ResponseEntity.ok(taskService.changeCategory(id, request.categoryName(), request.version()));
    }
    
    @PostMapping("/{id}/relations")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskRelationResponse addRelation(
            @PathVariable Long id,
            @Valid @RequestBody CreateTaskRelationRequest request) {
        return taskService.addRelation(id, request);
    }

    @GetMapping("/{id}/relations")
    public List<TaskRelationResponse> getRelations(@PathVariable Long id) {
        return taskService.getRelations(id);
    }

    @DeleteMapping("/{id}/relations/{relationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRelation(
            @PathVariable Long id,
            @PathVariable Long relationId) {
        taskService.removeRelation(id, relationId);
    }

}
