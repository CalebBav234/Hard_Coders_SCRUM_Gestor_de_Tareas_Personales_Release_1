package com.hardcoders.taskmanager.application.result;

import java.time.Instant;

/** Asignación Personal ↔ Tarea (US2.3). */
public record TaskAssignmentResult(
        Integer taskId,
        String taskTitle,
        Integer workerUserId,
        String workerName,
        Instant assignedAt,
        Integer assignedByUserId,
        String assignedByName,
        String reason
) {
}
