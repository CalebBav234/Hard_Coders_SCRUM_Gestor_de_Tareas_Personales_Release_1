package com.hardcoders.taskmanager.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Asignación de un trabajador a una tarea (V004 {@code task_assignment}, clave compuesta
 * {@code task_id + worker_user_id}). Es la única fuente de autoridad para la ejecución (Épica 3):
 * el Personal solo actúa sobre tareas y subtareas de tareas que tiene asignadas.
 */
public final class TaskAssignment {

    private final Integer taskId;
    private final Integer workerUserId;
    private final Instant assignedAt;
    private final Integer assignedByUserId;

    public TaskAssignment(Integer taskId, Integer workerUserId, Instant assignedAt, Integer assignedByUserId) {
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.workerUserId = Objects.requireNonNull(workerUserId, "workerUserId");
        this.assignedAt = assignedAt == null ? Instant.EPOCH : assignedAt;
        this.assignedByUserId = assignedByUserId;
    }

    public Integer taskId() {
        return taskId;
    }

    public Integer workerUserId() {
        return workerUserId;
    }

    public Instant assignedAt() {
        return assignedAt;
    }

    public Integer assignedByUserId() {
        return assignedByUserId;
    }
}
