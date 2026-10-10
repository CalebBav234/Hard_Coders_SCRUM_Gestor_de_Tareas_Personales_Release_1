package com.hardcoders.taskmanager.application.result;

import java.math.BigDecimal;

/** Progreso del proyecto completo (raíz de la cascada). */
public record ProjectProgressResult(
        Integer projectId,
        String projectName,
        String projectStatus,
        BigDecimal budgetBob,
        BigDecimal allocatedBudgetBob,
        Integer estimatedDurationMinutes,
        ProgressNodeResult root
) {
}
