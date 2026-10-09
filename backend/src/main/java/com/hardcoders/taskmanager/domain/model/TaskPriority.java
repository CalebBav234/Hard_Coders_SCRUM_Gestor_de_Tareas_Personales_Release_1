package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;

/** Prioridades del catálogo {@code task_priority} (V004). */
public enum TaskPriority {
    ALTA,
    MEDIA,
    BAJA;

    public static TaskPriority parseOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return MEDIA;
        }
        try {
            return valueOf(value.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("La prioridad debe ser ALTA, MEDIA o BAJA");
        }
    }
}
