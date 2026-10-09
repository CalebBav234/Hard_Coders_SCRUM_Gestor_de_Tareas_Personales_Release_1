package com.hardcoders.taskmanager.application.result;

import java.time.Instant;

public record SubtaskResult(
        Integer subtaskId,
        Integer taskId,
        String title,
        String description,
        String status,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
