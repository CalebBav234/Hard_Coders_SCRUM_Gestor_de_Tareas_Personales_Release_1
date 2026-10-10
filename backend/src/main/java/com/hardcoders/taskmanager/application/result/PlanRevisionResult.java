package com.hardcoders.taskmanager.application.result;

import java.math.BigDecimal;
import java.time.Instant;

public record PlanRevisionResult(
        int revisionNumber,
        BigDecimal budgetBob,
        int estimatedDurationMinutes,
        Instant changedAt,
        UserSummaryResult changedBy,
        String reason
) {
}
