package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;

import java.time.Instant;
import java.util.Objects;

/**
 * Actividad de una etapa (V004 {@code activity}). Nivel 3 de la jerarquía.
 * En la interfaz se muestra como "categoría" de la tarea.
 */
public final class Activity {

    /** Nombre de la actividad que representa las tareas "sin categoría". */
    public static final String UNCATEGORIZED = "Sin categoría";

    public static final String ENTITY = "Actividad";

    private final Integer id;
    /** Etapa contenedora; nulo solo en la referencia mínima devuelta por {@link #reference}. */
    private final Integer stageId;
    private String name;
    private String description;
    private WorkStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Activity(Integer id, Integer stageId, String name, String description, WorkStatus status,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.stageId = stageId;
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Activity create(Integer stageId, String name, String description, Instant now) {
        return new Activity(null, stageId, WorkText.title(name), WorkText.description(description),
                WorkStatus.PENDIENTE, now, now);
    }

    public static Activity restore(Integer id, Integer stageId, String name, String description, WorkStatus status,
                                   Instant createdAt, Instant updatedAt) {
        return new Activity(id, stageId, name, description, status, createdAt, updatedAt);
    }

    public void edit(String newName, String newDescription) {
        this.name = WorkText.title(newName);
        this.description = WorkText.description(newDescription);
    }

    public void verifyVersion(Long expectedVersion) {
        VersionToken.verify(ENTITY, id, expectedVersion, updatedAt);
    }

    /**
     * Referencia mínima (solo identificador y nombre) usada al reconstruir tareas desde JPA,
     * donde no hace falta recorrer toda la jerarquía: {@code stageId} queda nulo.
     */
    public static Activity reference(Integer id, String name) {
        return new Activity(id, null, name, null, WorkStatus.PENDIENTE, null, null);
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

    public Integer stageId() {
        return stageId;
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

    public boolean isUncategorized() {
        return UNCATEGORIZED.equalsIgnoreCase(name);
    }

    /** Nombre visible como categoría: nulo para "Sin categoría". */
    public String categoryName() {
        return isUncategorized() ? null : name;
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
