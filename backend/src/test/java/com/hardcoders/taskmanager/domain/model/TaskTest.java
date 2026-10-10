package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;
import com.hardcoders.taskmanager.domain.exception.OpenSubtasksException;
import com.hardcoders.taskmanager.domain.exception.VersionConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskTest {

    private static final Integer ACTOR = 7;
    private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");
    private static final Activity WORK =
            Activity.restore(3, 20, "Trabajo", null, WorkStatus.PENDIENTE, T0, T0);

    @Test
    void createsPendingTaskWithCreationRecord() {
        Task task = Task.create("  Preparar informe  ", null, WORK, ACTOR, T0, BigDecimal.ZERO, 60);

        assertThat(task.title()).isEqualTo("Preparar informe");
        assertThat(task.status()).isEqualTo(WorkStatus.PENDIENTE);
        assertThat(task.priority()).isEqualTo(TaskPriority.MEDIA);
        assertThat(task.isNew()).isTrue();
        assertThat(task.pendingChanges()).singleElement().satisfies(change -> {
            assertThat(change.previousStatus()).isNull();
            assertThat(change.newStatus()).isEqualTo(WorkStatus.PENDIENTE);
            assertThat(change.changedByUserId()).isEqualTo(ACTOR);
        });
    }

    @Test
    void rejectsBlankOrTooLongTitles() {
        assertThatThrownBy(() -> Task.create("   ", TaskPriority.ALTA, WORK, ACTOR, T0, BigDecimal.ZERO, 60))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Task.create("x".repeat(161), TaskPriority.ALTA, WORK, ACTOR, T0, BigDecimal.ZERO, 60))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void followsTheLifecycleAndRecordsEveryTransition() {
        Task task = persisted(WorkStatus.PENDIENTE, List.of());

        task.start(ACTOR, T0.plusSeconds(10));
        task.complete(ACTOR, T0.plusSeconds(130));

        assertThat(task.status()).isEqualTo(WorkStatus.COMPLETADA);
        assertThat(task.completedAt()).isEqualTo(T0.plusSeconds(130));
        assertThat(task.completedByUserId()).isEqualTo(ACTOR);
        assertThat(task.actualDurationMinutes()).isEqualTo(2);
        assertThat(task.pendingChanges()).extracting(StatusChange::newStatus)
                .containsExactly(WorkStatus.EN_PROGRESO, WorkStatus.COMPLETADA);

        task.reopen(ACTOR, T0.plusSeconds(200));
        assertThat(task.status()).isEqualTo(WorkStatus.EN_PROGRESO);
        assertThat(task.completedAt()).isNull();
        assertThat(task.completedByUserId()).isNull();
    }

    @Test
    void rejectsInvalidTransitions() {
        Task pending = persisted(WorkStatus.PENDIENTE, List.of());
        assertThatThrownBy(() -> pending.complete(ACTOR, T0)).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> pending.pause(ACTOR, T0)).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> pending.reopen(ACTOR, T0)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void accumulatesTimeAcrossSegmentsFromTheHistory() {
        Task task = persisted(WorkStatus.PENDIENTE, List.of());
        task.start(ACTOR, T0);
        task.pause(ACTOR, T0.plusSeconds(90));
        task.start(ACTOR, T0.plusSeconds(300));

        TimeTracking tracking = task.timeTracking();
        assertThat(tracking.trackedSeconds()).isEqualTo(90);
        assertThat(tracking.currentSegmentStart()).isEqualTo(T0.plusSeconds(300));
        assertThat(tracking.totalSecondsAt(T0.plusSeconds(330))).isEqualTo(120);
        assertThat(task.actualDurationMinutes()).isEqualTo(2);
    }

    @Test
    void cancellationIsBlockedByOpenSubtasks() {
        Task task = persisted(WorkStatus.PENDIENTE, List.of());

        assertThatThrownBy(() -> task.cancel(true, ACTOR, T0)).isInstanceOf(OpenSubtasksException.class);

        task.cancel(false, ACTOR, T0);
        assertThat(task.status()).isEqualTo(WorkStatus.CANCELADA);
        assertThatThrownBy(() -> task.cancel(false, ACTOR, T0))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void detectsStaleVersions() {
        Task task = persisted(WorkStatus.PENDIENTE, List.of());

        task.verifyVersion(null);
        task.verifyVersion(task.version());
        assertThatThrownBy(() -> task.verifyVersion(task.version() - 1))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void migratedTasksKeepTheirRecordedMinutes() {
        Task migrated = Task.restore(5, WORK, "Legado", null, WorkStatus.COMPLETADA, TaskPriority.BAJA,
                null, 0, 3, T0, ACTOR, T0, T0, List.of());

        assertThat(migrated.timeTracking().trackedSeconds()).isEqualTo(180);
    }

    private static Task persisted(WorkStatus status, List<StatusChange> history) {
        List<StatusChange> all = new ArrayList<>();
        all.add(new StatusChange(1, null, WorkStatus.PENDIENTE, T0.minusSeconds(60), ACTOR, "Tarea creada"));
        all.addAll(history);
        return Task.restore(1, WORK, "Tarea", null, status, TaskPriority.ALTA, null, 0, null, null, null,
                T0.minusSeconds(60), T0.minusSeconds(60), all);
    }
}
