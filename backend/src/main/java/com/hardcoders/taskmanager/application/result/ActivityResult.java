package com.hardcoders.taskmanager.application.result;

import java.time.Instant;

/** Actividad (nivel 3 de la jerarquía). En la interfaz se presenta como "categoría". */
public record ActivityResult(
        Integer activityId,
        Integer stageId,
        String name,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    /** Nombre visible como categoría: nulo para "Sin categoría". */
    public String categoryName() {
        return "Sin categoría".equalsIgnoreCase(name) ? null : name;
    }
}
