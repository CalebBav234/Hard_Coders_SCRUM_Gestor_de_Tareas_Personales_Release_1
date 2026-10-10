package com.hardcoders.taskmanager.application.result;

import java.time.Instant;

/** Etapa (nivel 2 de la jerarquía). */
public record StageResult(
        Integer stageId,
        Integer projectId,
        String name,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
