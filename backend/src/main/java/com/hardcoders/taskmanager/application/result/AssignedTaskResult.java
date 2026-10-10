package com.hardcoders.taskmanager.application.result;

import java.time.Instant;
import java.util.List;

/**
 * Proyección de tarea para el Personal (US3.1). No incluye presupuesto ni duración estimada:
 * AC-US3.1-02 exige que los campos financieros no lleguen al trabajador.
 */
public record AssignedTaskResult(
        Integer taskId,
        Integer projectId,
        Integer activityId,
        String activityName,
        String title,
        String description,
        String status,
        String priority,
        Integer actualDurationMinutes,
        long trackedSeconds,
        Instant currentSegmentStartedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<SubtaskResult> subtasks
) {
}
