package com.hardcoders.taskmanager.dto;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        Long categoryId,
        String categoryName,
        Long parentTaskId,
        Instant activatedAt,
        Instant completedAt,
        Long totalActiveSeconds,
        Long effectiveActiveSeconds,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
