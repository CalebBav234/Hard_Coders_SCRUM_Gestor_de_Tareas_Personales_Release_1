package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;

import java.time.Instant;
import java.util.Objects;

/**
 * Subtarea (V004 {@code subtask}). Mismo ciclo de vida que la tarea, pero V4 no guarda
 * historial, prioridad ni tiempo de las subtareas.
 */
public final class Subtask {

    public static final String ENTITY = "Subtarea";

    private final Integer id;
    private final Integer taskId;
    private String title;
    private String description;
    private WorkStatus status;
    private Instant completedAt;
    private Integer completedByUserId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Subtask(Integer id, Integer taskId, String title, String description, WorkStatus status,
                    Instant completedAt, Integer completedByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.title = title;
        this.description = description;
        this.status = Objects.requireNonNull(status, "status");
        this.completedAt = completedAt;
        this.completedByUserId = completedByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Subtask create(Integer taskId, String title, String description, Instant now) {
        return new Subtask(null, taskId, WorkText.title(title), WorkText.description(description),
                WorkStatus.PENDIENTE, null, null, now, now);
    }

    public static Subtask restore(Integer id, Integer taskId, String title, String description, WorkStatus status,
                                  Instant completedAt, Integer completedByUserId, Instant createdAt, Instant updatedAt) {
        return new Subtask(id, taskId, title, description, status, completedAt, completedByUserId, createdAt, updatedAt);
    }

    public void verifyVersion(Long expectedVersion) {
        VersionToken.verify(ENTITY, id, expectedVersion, updatedAt);
    }

    public void edit(String newTitle, String newDescription) {
        this.title = WorkText.title(newTitle);
        this.description = WorkText.description(newDescription);
    }

    public void start() {
        transition(WorkStatus.PENDIENTE, WorkStatus.EN_PROGRESO);
    }

    public void pause() {
        transition(WorkStatus.EN_PROGRESO, WorkStatus.PENDIENTE);
    }

    public void complete(Integer actorId, Instant now) {
        transition(WorkStatus.EN_PROGRESO, WorkStatus.COMPLETADA);
        this.completedAt = now;
        this.completedByUserId = actorId;
    }

    public void reopen() {
        transition(WorkStatus.COMPLETADA, WorkStatus.EN_PROGRESO);
        this.completedAt = null;
        this.completedByUserId = null;
    }
    public void cancel() {
        if (status == WorkStatus.CANCELADA) {
            throw new InvalidStatusTransitionException(status, WorkStatus.CANCELADA);
        }
        this.status = WorkStatus.CANCELADA;
    }

    private void transition(WorkStatus expected, WorkStatus target) {
        if (status != expected) {
            throw new InvalidStatusTransitionException(status, target);
        }
        this.status = target;
    }

    public boolean isNew() {
        return id == null;
    }

    public Integer id() {
        return id;
    }

    public Integer taskId() {
        return taskId;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public WorkStatus status() {
        return status;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public Integer completedByUserId() {
        return completedByUserId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return VersionToken.of(updatedAt);
    }
}
