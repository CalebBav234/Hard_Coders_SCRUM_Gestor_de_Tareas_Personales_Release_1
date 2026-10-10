package com.hardcoders.taskmanager.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Catálogo {@code project_status} (V004) con el ciclo de vida del proyecto:
 * PLANIFICADO → EN_EJECUCION ⇄ EN_PAUSA → FINALIZADO; cualquier estado abierto → CANCELADO.
 * FINALIZADO y CANCELADO son cerrados ({@code is_closed}) y no admiten más cambios.
 */
public enum ProjectStatus {
    PLANIFICADO,
    EN_EJECUCION,
    EN_PAUSA,
    FINALIZADO,
    CANCELADO;

    public boolean isClosed() {
        return this == FINALIZADO || this == CANCELADO;
    }

    public Set<ProjectStatus> allowedTargets() {
        return switch (this) {
            case PLANIFICADO -> EnumSet.of(EN_EJECUCION, CANCELADO);
            case EN_EJECUCION -> EnumSet.of(EN_PAUSA, FINALIZADO, CANCELADO);
            case EN_PAUSA -> EnumSet.of(EN_EJECUCION, CANCELADO);
            case FINALIZADO, CANCELADO -> EnumSet.noneOf(ProjectStatus.class);
        };
    }

    public boolean canMoveTo(ProjectStatus target) {
        return allowedTargets().contains(target);
    }
}
