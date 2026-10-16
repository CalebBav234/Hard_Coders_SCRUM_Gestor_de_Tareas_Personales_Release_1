package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionTimeTrackingTest {

    private static final Integer WORKER = 17;
    private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");
    private static final Activity ACTIVITY = Activity.reference(8, "Implementación");

    @Test
    void taskTransitionsAccumulateClosedAndOpenSegmentsAndCanBeReopened() {
        Task task = pendingTask();

        task.start(WORKER, T0);
        task.pause(WORKER, T0.plusSeconds(90));
        assertThat(task.actualDurationMinutes()).isEqualTo(2);

        task.start(WORKER, T0.plusSeconds(300));
        task.complete(WORKER, T0.plusSeconds(330));
        assertThat(task.status()).isEqualTo(WorkStatus.COMPLETADA);
        assertThat(task.actualDurationMinutes()).isEqualTo(2);
        assertThat(task.timeTracking().trackedSeconds()).isEqualTo(120);
        assertThat(task.completedByUserId()).isEqualTo(WORKER);
        assertThat(task.pendingChanges()).extracting(StatusChange::newStatus)
                .containsExactly(WorkStatus.EN_PROGRESO, WorkStatus.PENDIENTE,
                        WorkStatus.EN_PROGRESO, WorkStatus.COMPLETADA);

        task.reopen(WORKER, T0.plusSeconds(400));
        assertThat(task.status()).isEqualTo(WorkStatus.EN_PROGRESO);
        assertThat(task.completedAt()).isNull();
        assertThat(task.completedByUserId()).isNull();
        assertThat(task.timeTracking().currentSegmentStart()).isEqualTo(T0.plusSeconds(400));
    }

    @Test
    void rejectsTaskTransitionsThatDoNotMatchCurrentState() {
        Task pending = pendingTask();
        assertThatThrownBy(() -> pending.pause(WORKER, T0))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> pending.complete(WORKER, T0))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> pending.reopen(WORKER, T0))
                .isInstanceOf(InvalidStatusTransitionException.class);

        Task completed = Task.restore(5, ACTIVITY, "Tarea", null, WorkStatus.COMPLETADA,
                TaskPriority.MEDIA, BigDecimal.ZERO, 60, 1, T0, WORKER, T0, T0, List.of());
        assertThatThrownBy(() -> completed.start(WORKER, T0))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void subtaskSupportsStartPauseCompleteAndReopenWithInvalidTransitionsRejected() {
        Subtask subtask = Subtask.restore(11, 5, "Revisar", null, WorkStatus.PENDIENTE,
                null, null, T0, T0);

        subtask.start();
        subtask.pause();
        subtask.start();
        subtask.complete(WORKER, T0.plusSeconds(120));
        assertThat(subtask.status()).isEqualTo(WorkStatus.COMPLETADA);
        assertThat(subtask.completedAt()).isEqualTo(T0.plusSeconds(120));
        assertThat(subtask.completedByUserId()).isEqualTo(WORKER);

        subtask.reopen();
        assertThat(subtask.status()).isEqualTo(WorkStatus.EN_PROGRESO);
        assertThat(subtask.completedAt()).isNull();
        assertThat(subtask.completedByUserId()).isNull();
        subtask.pause();

        assertThatThrownBy(() -> subtask.complete(WORKER, T0))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(subtask::reopen).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void derivesTimeFromChronologicalHistoryAndRoundsPositiveSecondsUpToMinutes() {
        StatusChange pause = new StatusChange(3, WorkStatus.EN_PROGRESO, WorkStatus.PENDIENTE,
                T0.plusSeconds(90), WORKER, "Pausa");
        StatusChange start = new StatusChange(2, WorkStatus.PENDIENTE, WorkStatus.EN_PROGRESO,
                T0, WORKER, "Inicio");

        TimeTracking tracking = TimeTracking.from(List.of(pause, start), WorkStatus.PENDIENTE);
        assertThat(tracking.trackedSeconds()).isEqualTo(90);
        assertThat(tracking.currentSegmentStart()).isNull();
        assertThat(tracking.totalSecondsAt(T0.plusSeconds(300))).isEqualTo(90);
        assertThat(TimeTracking.toMinutes(1)).isEqualTo(1);
        assertThat(TimeTracking.toMinutes(60)).isEqualTo(1);
        assertThat(TimeTracking.toMinutes(61)).isEqualTo(2);
        assertThat(TimeTracking.toMinutes(0)).isNull();
        assertThat(TimeTracking.toMinutes(-1)).isNull();
    }

    @Test
    void openSegmentsDoNotAddNegativeTimeAndOnlyCountOnce() {
        StatusChange firstStart = new StatusChange(2, WorkStatus.PENDIENTE, WorkStatus.EN_PROGRESO,
                T0, WORKER, "Inicio");
        StatusChange duplicateStart = new StatusChange(3, WorkStatus.PENDIENTE, WorkStatus.EN_PROGRESO,
                T0.plusSeconds(20), WORKER, "Inicio duplicado");
        TimeTracking tracking = TimeTracking.from(List.of(firstStart, duplicateStart), WorkStatus.EN_PROGRESO);

        assertThat(tracking.currentSegmentStart()).isEqualTo(T0);
        assertThat(tracking.totalSecondsAt(T0.minusSeconds(5))).isZero();
        assertThat(tracking.totalSecondsAt(T0.plusSeconds(60))).isEqualTo(60);
        assertThat(TimeTracking.NONE.totalSecondsAt(T0)).isZero();
    }

    @Test
    void preservesRecordedMinutesForMigratedTasksWithoutStatusSegments() {
        Task migrated = Task.restore(9, ACTIVITY, "Migrada", null, WorkStatus.COMPLETADA,
                TaskPriority.MEDIA, BigDecimal.ZERO, 0, 3, T0, WORKER, T0, T0, List.of());

        assertThat(migrated.timeTracking().trackedSeconds()).isEqualTo(180);
        assertThat(migrated.actualDurationMinutes()).isEqualTo(3);

        Task migratedWithoutTrackedTime = Task.restore(10, ACTIVITY, "Sin tiempo", null,
                WorkStatus.COMPLETADA, TaskPriority.MEDIA, BigDecimal.ZERO, 0, 0, T0, WORKER, T0, T0, List.of());
        assertThat(migratedWithoutTrackedTime.timeTracking().trackedSeconds()).isZero();
    }

    private static Task pendingTask() {
        return Task.restore(5, ACTIVITY, "Tarea", null, WorkStatus.PENDIENTE, TaskPriority.MEDIA,
                BigDecimal.ZERO, 60, null, null, null, T0.minusSeconds(60), T0.minusSeconds(60),
                List.of(new StatusChange(1, null, WorkStatus.PENDIENTE, T0.minusSeconds(60),
                        WORKER, "Creada")));
    }
}
