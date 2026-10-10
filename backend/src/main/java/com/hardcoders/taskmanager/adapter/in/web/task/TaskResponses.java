package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.AssignedTaskResult;
import com.hardcoders.taskmanager.application.result.StageResult;
import com.hardcoders.taskmanager.application.result.StatusEventResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskAssignmentResult;
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
                    result.status(), result.completedAt(), result.createdAt(), result.updatedAt(),
                    result.version());
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
            List<Integer> assignedWorkerIds,
            List<SubtaskResponse> subtasks
    ) {
        public static TaskResponse from(TaskResult result, List<Integer> assignedWorkerIds) {
            return new TaskResponse(result.taskId(), result.activityId(), result.activityName(), result.title(),
                    result.description(), result.status(), result.priority(), result.allocatedBudgetBob(),
                    result.estimatedDurationMinutes(), result.actualDurationMinutes(), result.trackedSeconds(),
                    result.currentSegmentStartedAt(), result.completedAt(), result.createdAt(),
                    result.updatedAt(), result.version(), assignedWorkerIds,
                    result.subtasks().stream().map(SubtaskResponse::from).toList());
        }
    }

    /**
     * US3.1: proyección del Personal. No incluye presupuesto ni duración estimada
     * (AC-US3.1-02) y solo llega si la tarea está asignada al usuario del token.
     */
    public record AssignedTaskResponse(
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
            List<SubtaskResponse> subtasks
    ) {
        public static AssignedTaskResponse from(AssignedTaskResult result) {
            return new AssignedTaskResponse(result.taskId(), result.projectId(), result.activityId(),
                    result.activityName(), result.title(), result.description(), result.status(), result.priority(),
                    result.actualDurationMinutes(), result.trackedSeconds(), result.currentSegmentStartedAt(),
                    result.completedAt(), result.createdAt(), result.updatedAt(), result.version(),
                    result.subtasks().stream().map(SubtaskResponse::from).toList());
        }
    }

    public record StageResponse(
            Integer stageId,
            Integer projectId,
            String name,
            String description,
            String status,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        public static StageResponse from(StageResult result) {
            return new StageResponse(result.stageId(), result.projectId(), result.name(), result.description(),
                    result.status(), result.createdAt(), result.updatedAt(), result.version());
        }
    }

    public record ActivityResponse(
            Integer activityId,
            Integer stageId,
            String name,
            String description,
            String status,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        public static ActivityResponse from(ActivityResult result) {
            return new ActivityResponse(result.activityId(), result.stageId(), result.name(), result.description(),
                    result.status(), result.createdAt(), result.updatedAt(), result.version());
        }
    }

    public record TaskAssignmentResponse(
            Integer taskId,
            String taskTitle,
            Integer workerUserId,
            String workerName,
            Instant assignedAt,
            Integer assignedByUserId,
            String assignedByName,
            String reason
    ) {
        public static TaskAssignmentResponse from(TaskAssignmentResult result) {
            return new TaskAssignmentResponse(result.taskId(), result.taskTitle(), result.workerUserId(),
                    result.workerName(), result.assignedAt(), result.assignedByUserId(), result.assignedByName(),
                    result.reason());
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
        public static TaskHistoryResponse from(TaskHistoryResult result, List<Integer> assignedWorkerIds) {
            return new TaskHistoryResponse(TaskResponse.from(result.task(), assignedWorkerIds), result.cancelledAt(),
                    result.events().stream().map(StatusEventResponse::from).toList());
        }
    }
}
