package com.hardcoders.taskmanager.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Proyecto visible para el actor. Los indicadores {@code can*} y {@code allowedStatuses} reflejan los
 * permisos del actor para que la interfaz muestre solo acciones válidas; el backend vuelve a validarlos.
 */
public record ProjectResult(
        Integer projectId,
        String name,
        String description,
        String status,
        boolean closed,
        BigDecimal budgetBob,
        BigDecimal allocatedBudgetBob,
        int estimatedDurationMinutes,
        UserSummaryResult manager,
        UserSummaryResult createdBy,
        int currentRevision,
        Instant createdAt,
        Instant updatedAt,
        long version,
        boolean canEditDetails,
        boolean canEditPlan,
        boolean canDelegate,
        List<String> allowedStatuses
) {
}
