package com.hardcoders.taskmanager.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Revisión de {@code project_plan_history}: presupuesto y duración vigentes desde {@code changedAt}. */
public record PlanRevision(
        int revisionNumber,
        BigDecimal budgetBob,
        int estimatedDurationMinutes,
        Instant changedAt,
        Integer changedByUserId,
        String reason
) {
    public static final int REASON_MAX = 500;

    public PlanRevision {
        if (revisionNumber < 1) {
            throw new IllegalArgumentException("La revisión empieza en 1");
        }
        Objects.requireNonNull(budgetBob, "budgetBob");
        Objects.requireNonNull(changedAt, "changedAt");
        Objects.requireNonNull(changedByUserId, "changedByUserId");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("El motivo de la revisión es obligatorio");
        }
    }
}
