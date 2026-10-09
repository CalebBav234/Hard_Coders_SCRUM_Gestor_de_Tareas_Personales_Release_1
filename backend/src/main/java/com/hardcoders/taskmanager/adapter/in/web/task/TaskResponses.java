package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.StatusEventResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskHistoryResult;
import com.hardcoders.taskmanager.application.result.TaskResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contrato HTTP de lectura (V4). {@code version} es el token de concurrencia que el cliente devuelve. */
public final class TaskResponses {

    private TaskResponses() {
    }

    public record SubtaskResponse(
            Integer subtaskId,
            Integer taskId,
            String title,
            String description,
            String status,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        public static SubtaskResponse from(SubtaskResult result) {
            return new SubtaskResponse(result.subtaskId(), result.taskId(), result.title(), result.description(),
                    result.status(), result.completedAt(), result.createdAt(), result.updatedAt(), result.version());
        }
    }

    public record TaskResponse(
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
            Instant createdAt,
            Instant updatedAt,
            long version,
            List<SubtaskResponse> subtasks
    ) {
        public static TaskResponse from(TaskResult result) {
            return new TaskResponse(result.taskId(), result.activityId(), result.activityName(), result.title(),
                    result.description(), result.status(), result.priority(), result.allocatedBudgetBob(),
                    result.estimatedDurationMinutes(), result.actualDurationMinutes(), result.trackedSeconds(),
                    result.currentSegmentStartedAt(), result.completedAt(), result.createdAt(), result.updatedAt(),
                    result.version(), result.subtasks().stream().map(SubtaskResponse::from).toList());
        }
    }

    public record StatusEventResponse(
            Integer eventId,
            String previousStatus,
            String newStatus,
            String reason,
            Instant changedAt
    ) {
        public static StatusEventResponse from(StatusEventResult result) {
            return new StatusEventResponse(result.eventId(), result.previousStatus(), result.newStatus(),
                    result.reason(), result.changedAt());
        }
    }

    public record TaskHistoryResponse(TaskResponse task, Instant cancelledAt, List<StatusEventResponse> events) {
        public static TaskHistoryResponse from(TaskHistoryResult result) {
            return new TaskHistoryResponse(TaskResponse.from(result.task()), result.cancelledAt(),
                    result.events().stream().map(StatusEventResponse::from).toList());
        }
    }

    public record ActivityResponse(Integer activityId, String name) {
        public static ActivityResponse from(ActivityResult result) {
            return new ActivityResponse(result.activityId(), result.name());
        }
    }
}
