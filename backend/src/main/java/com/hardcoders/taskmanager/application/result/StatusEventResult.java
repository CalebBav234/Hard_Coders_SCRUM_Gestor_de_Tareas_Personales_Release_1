package com.hardcoders.taskmanager.application.result;

import java.time.Instant;

public record StatusEventResult(
        Integer eventId,
        String previousStatus,
        String newStatus,
        String reason,
        Instant changedAt
) {
}
