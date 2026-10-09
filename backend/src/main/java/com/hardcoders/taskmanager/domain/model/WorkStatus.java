package com.hardcoders.taskmanager.domain.model;

/** Estados del catálogo {@code work_status} (V004). El nombre coincide con la fila del catálogo. */
public enum WorkStatus {
    PENDIENTE,
    EN_PROGRESO,
    COMPLETADA,
    CANCELADA;

    public boolean isOpen() {
        return this == PENDIENTE || this == EN_PROGRESO;
    }
}
