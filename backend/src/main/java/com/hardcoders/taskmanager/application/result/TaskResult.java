package com.hardcoders.taskmanager.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @param activityName           nombre de la actividad; nulo cuando es "Sin categoría"
 * @param trackedSeconds         segundos trabajados en segmentos ya cerrados
 * @param currentSegmentStartedAt inicio del segmento en curso (solo EN_PROGRESO)
 */
public record TaskResult(
        Integer taskId,
        Integer activityId,
        String activityName,
        String title,
        String description,
        String status,
        String priority,
        BigDecimal allocatedBudgetBob,
        int estimatedDurationMinutes,
        Integer actualDurationMinutes,
        long trackedSeconds,
        Instant currentSegmentStartedAt,
        Instant completedAt,
        Integer completedByUserId,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<Integer> assignedWorkerIds,
        List<SubtaskResult> subtasks
) {
}
