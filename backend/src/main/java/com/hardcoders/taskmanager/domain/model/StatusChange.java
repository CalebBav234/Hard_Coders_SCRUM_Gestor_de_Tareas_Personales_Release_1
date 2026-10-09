package com.hardcoders.taskmanager.domain.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

/**
 * Registro inmutable de {@code task_status_history}. {@code id} es nulo mientras el cambio no se
 * ha persistido; {@code previousStatus} es nulo en el registro de creación.
 */
public record StatusChange(
        Integer id,
        WorkStatus previousStatus,
        WorkStatus newStatus,
        Instant changedAt,
        Integer changedByUserId,
        String reason
) {
    /** Orden cronológico; a igual fecha, por id (los cambios aún no persistidos van al final). */
    public static final Comparator<StatusChange> CHRONOLOGICAL = Comparator
            .comparing(StatusChange::changedAt)
            .thenComparing(StatusChange::id, Comparator.nullsLast(Comparator.naturalOrder()));

    public StatusChange {
        Objects.requireNonNull(newStatus, "newStatus");
        Objects.requireNonNull(changedAt, "changedAt");
        Objects.requireNonNull(changedByUserId, "changedByUserId");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("El motivo del cambio es obligatorio");
        }
        if (previousStatus == newStatus) {
            throw new IllegalArgumentException("Un cambio de estado debe modificar el estado");
        }
    }
}
