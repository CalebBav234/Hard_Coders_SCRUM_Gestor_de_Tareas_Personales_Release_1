package com.hardcoders.taskmanager.dto;

import java.time.Instant;

public record TaskStatusHistoryResponse(
        Long id,
        String fromStatus,
        String toStatus,
        String changeReason,
        Instant changedAt
) {
}
