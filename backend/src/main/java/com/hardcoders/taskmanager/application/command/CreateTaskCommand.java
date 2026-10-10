package com.hardcoders.taskmanager.application.command;

import java.math.BigDecimal;

/**
 * US2.2: la tarea se crea dentro de una actividad concreta y, por tanto, de un proyecto.
 * {@code activityId} es obligatorio: ya no se deduce del primer proyecto (fallback eliminado).
 */
public record CreateTaskCommand(Integer activityId, String title, String priority,
                                BigDecimal allocatedBudgetBob, Integer estimatedDurationMinutes) {
}
