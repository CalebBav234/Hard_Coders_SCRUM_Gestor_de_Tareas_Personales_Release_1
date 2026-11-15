package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;

import java.time.Instant;
import java.util.Objects;

/**
 * Asignación de un trabajador a una tarea (V004 {@code task_assignment}, clave compuesta
 * {@code task_id + worker_user_id}). Es la única fuente de autoridad para la ejecución (Épica 3):
 * el Personal solo actúa sobre tareas y subtareas de tareas que tiene asignadas.
 *
 * <p>EC3-02: el motivo se persiste en {@code task_assignment.reason} ({@code varchar(500)}, V006).
 * Las asignaciones anteriores a V006 no tienen motivo ({@code null}).
 */
public final class TaskAssignment {

    /** Límite de {@code task_assignment.reason}; coincide con el input HTML y con el contrato web (EC3-03). */
    public static final int REASON_MAX = 500;
    public static final String DEFAULT_REASON = "Asignación de Personal";

    private final Integer taskId;
    private final Integer workerUserId;
    private final Instant assignedAt;
    private final Integer assignedByUserId;
    private final String reason;

    public TaskAssignment(Integer taskId, Integer workerUserId, Instant assignedAt, Integer assignedByUserId) {
        this(taskId, workerUserId, assignedAt, assignedByUserId, null);
    }

    public TaskAssignment(Integer taskId, Integer workerUserId, Instant assignedAt, Integer assignedByUserId,
                          String reason) {
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.workerUserId = Objects.requireNonNull(workerUserId, "workerUserId");
        this.assignedAt = assignedAt == null ? Instant.EPOCH : assignedAt;
        this.assignedByUserId = assignedByUserId;
        this.reason = reason;
    }

    /**
     * Regla de dominio del motivo de asignación: vacío o nulo usa {@link #DEFAULT_REASON}; se recortan
     * los espacios y no puede superar {@link #REASON_MAX} caracteres.
     */
    public static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return DEFAULT_REASON;
        }
        String stripped = reason.strip();
        if (stripped.length() > REASON_MAX) {
            throw new DomainValidationException("El motivo no puede superar " + REASON_MAX + " caracteres");
        }
        return stripped;
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

    public String reason() {
        return reason;
    }
}
