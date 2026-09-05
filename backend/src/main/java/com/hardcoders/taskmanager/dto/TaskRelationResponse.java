package com.hardcoders.taskmanager.dto;

import java.time.Instant;

public record TaskRelationResponse(
        Long id,
        Long targetTaskId,
        String targetTaskTitle,
        String relationType,
        Instant createdAt
) {}
