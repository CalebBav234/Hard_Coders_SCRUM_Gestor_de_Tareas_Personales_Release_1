package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;

/** Reglas de texto de V004: títulos/nombres {@code varchar(160)} no vacíos y descripciones {@code varchar(2000)}. */
public final class WorkText {

    public static final int TITLE_MAX = 160;
    public static final int DESCRIPTION_MAX = 2000;

    private WorkText() {
    }

    public static String title(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("El título es obligatorio");
        }
        String stripped = value.strip();
        if (stripped.length() > TITLE_MAX) {
            throw new DomainValidationException("El título no puede superar " + TITLE_MAX + " caracteres");
        }
        return stripped;
    }

    public static String description(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String stripped = value.strip();
        if (stripped.length() > DESCRIPTION_MAX) {
            throw new DomainValidationException(
                    "La descripción no puede superar " + DESCRIPTION_MAX + " caracteres");
        }
        return stripped;
    }

    /** Nombre de actividad (la "categoría" de la interfaz); nulo o vacío significa sin categoría. */
    public static String activityName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String stripped = value.strip();
        if (stripped.length() > TITLE_MAX) {
            throw new DomainValidationException("La categoría no puede superar " + TITLE_MAX + " caracteres");
        }
        return stripped;
    }
}
