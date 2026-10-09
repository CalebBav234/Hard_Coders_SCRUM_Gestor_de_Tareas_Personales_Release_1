package com.hardcoders.taskmanager.application.result;

import java.time.Instant;
import java.util.List;

/**
 * Tarea archivada (COMPLETADA o CANCELADA) con su historial de estados.
 *
 * @param cancelledAt fecha de la cancelación ("eliminada" en la interfaz), o nulo
 */
public record TaskHistoryResult(TaskResult task, Instant cancelledAt, List<StatusEventResult> events) {
}
