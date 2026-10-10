package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;

import java.time.Instant;
import java.util.Objects;

/**
 * Etapa de un proyecto (V004 {@code stage}). Nivel 2 de la jerarquía
 * Proyecto → Etapa → Actividad → Tarea → Subtarea.
 */
public final class Stage {

    public static final String ENTITY = "Etapa";

    private final Integer id;
    private final Integer projectId;
    private String name;
    private String description;
    private WorkStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Stage(Integer id, Integer projectId, String name, String description, WorkStatus status,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.projectId = Objects.requireNonNull(projectId, "projectId");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Nueva etapa en PENDIENTE. */
    public static Stage create(Integer projectId, String name, String description, Instant now) {
        return new Stage(null, projectId, WorkText.title(name), WorkText.description(description),
                WorkStatus.PENDIENTE, now, now);
    }

    public static Stage restore(Integer id, Integer projectId, String name, String description, WorkStatus status,
                                Instant createdAt, Instant updatedAt) {
        return new Stage(id, projectId, name, description, status, createdAt, updatedAt);
    }

    public void edit(String newName, String newDescription) {
        this.name = WorkText.title(newName);
        this.description = WorkText.description(newDescription);
    }

    public void verifyVersion(Long expectedVersion) {
        VersionToken.verify(ENTITY, id, expectedVersion, updatedAt);
    }

    public void start() {
        transition(WorkStatus.PENDIENTE, WorkStatus.EN_PROGRESO);
    }

    public void pause() {
        transition(WorkStatus.EN_PROGRESO, WorkStatus.PENDIENTE);
    }

    public void complete() {
        transition(WorkStatus.EN_PROGRESO, WorkStatus.COMPLETADA);
    }

    public void reopen() {
        transition(WorkStatus.COMPLETADA, WorkStatus.EN_PROGRESO);
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

    public Integer projectId() {
        return projectId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public WorkStatus status() {
        return status;
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
