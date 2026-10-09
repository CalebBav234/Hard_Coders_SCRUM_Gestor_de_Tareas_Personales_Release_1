package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.StatusEventResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskHistoryResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.StatusChange;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.TimeTracking;
import com.hardcoders.taskmanager.domain.model.WorkStatus;

import java.time.Instant;
import java.util.List;

/** Traduce el modelo de dominio a resultados de caso de uso. */
final class ResultMapper {

    private ResultMapper() {
    }

    static TaskResult toResult(Task task, List<Subtask> subtasks) {
        TimeTracking tracking = task.timeTracking();
        return new TaskResult(
                task.id(),
                task.activity().id(),
                task.activity().categoryName(),
                task.title(),
                task.description(),
                task.status().name(),
                task.priority().name(),
                task.allocatedBudgetBob(),
                task.estimatedDurationMinutes(),
                task.actualDurationMinutes(),
                tracking.trackedSeconds(),
                tracking.currentSegmentStart(),
                task.completedAt(),
                task.createdAt(),
                task.updatedAt(),
                task.version(),
                subtasks.stream().map(ResultMapper::toResult).toList());
    }

    static SubtaskResult toResult(Subtask subtask) {
        return new SubtaskResult(
                subtask.id(),
                subtask.taskId(),
                subtask.title(),
                subtask.description(),
                subtask.status().name(),
                subtask.completedAt(),
                subtask.createdAt(),
                subtask.updatedAt(),
                subtask.version());
    }

    static TaskHistoryResult toHistory(Task task) {
        List<StatusChange> ordered = task.history().stream()
                .sorted(StatusChange.CHRONOLOGICAL.reversed())
                .toList();
        Instant cancelledAt = task.status() == WorkStatus.CANCELADA
                ? ordered.stream()
                        .filter(change -> change.newStatus() == WorkStatus.CANCELADA)
                        .map(StatusChange::changedAt)
                        .findFirst()
                        .orElse(task.updatedAt())
                : null;
        List<StatusEventResult> events = ordered.stream()
                .map(change -> new StatusEventResult(
                        change.id(),
                        change.previousStatus() == null ? null : change.previousStatus().name(),
                        change.newStatus().name(),
                        change.reason(),
                        change.changedAt()))
                .toList();
        return new TaskHistoryResult(toResult(task, List.of()), cancelledAt, events);
    }

    static ActivityResult toResult(Activity activity) {
        return new ActivityResult(activity.id(), activity.name());
    }
}
