package com.hardcoders.taskmanager.controller;

import com.hardcoders.taskmanager.dto.ChangeCategoryRequest;
import com.hardcoders.taskmanager.dto.CreateTaskRequest;
import com.hardcoders.taskmanager.dto.TaskResponse;
import com.hardcoders.taskmanager.dto.UpdateTaskRequest;
import com.hardcoders.taskmanager.dto.VersionRequest;
import com.hardcoders.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
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
        return taskService.create(request.title(), request.priority(), request.categoryName());
    }

    @GetMapping
    public List<TaskResponse> list() {
        return taskService.listTasks();
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
}
