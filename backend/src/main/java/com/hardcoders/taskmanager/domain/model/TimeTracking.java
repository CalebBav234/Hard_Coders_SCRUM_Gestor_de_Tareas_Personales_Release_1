package com.hardcoders.taskmanager.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Tiempo de trabajo de una tarea derivado de su historial de estados: cada intervalo en
 * {@link WorkStatus#EN_PROGRESO} es un segmento. Así no se necesita una columna extra en V4.
 *
 * @param trackedSeconds   segundos de los segmentos ya cerrados
 * @param currentSegmentStart inicio del segmento abierto, o nulo si la tarea no está en progreso
 */
public record TimeTracking(long trackedSeconds, Instant currentSegmentStart) {

    public static final TimeTracking NONE = new TimeTracking(0, null);

    public static TimeTracking from(List<StatusChange> changes, WorkStatus currentStatus) {
        List<StatusChange> ordered = changes.stream()
                .sorted(StatusChange.CHRONOLOGICAL)
                .toList();
        long closed = 0;
        Instant openSince = null;
        for (StatusChange change : ordered) {
            if (change.newStatus() == WorkStatus.EN_PROGRESO) {
                if (openSince == null) {
                    openSince = change.changedAt();
                }
            } else if (openSince != null) {
                closed += Math.max(0, Duration.between(openSince, change.changedAt()).getSeconds());
                openSince = null;
            }
        }
        Instant current = currentStatus == WorkStatus.EN_PROGRESO ? openSince : null;
        return new TimeTracking(closed, current);
    }

    /** Segundos totales a la fecha indicada, incluido el segmento abierto. */
    public long totalSecondsAt(Instant now) {
        if (currentSegmentStart == null) {
            return trackedSeconds;
        }
        return trackedSeconds + Math.max(0, Duration.between(currentSegmentStart, now).getSeconds());
    }

    /** Minutos para {@code actual_duration_minutes}: redondeo hacia arriba, nulo si no hubo trabajo. */
    public static Integer toMinutes(long seconds) {
        if (seconds <= 0) {
            return null;
        }
        return Math.toIntExact((seconds + 59) / 60);
    }
}
