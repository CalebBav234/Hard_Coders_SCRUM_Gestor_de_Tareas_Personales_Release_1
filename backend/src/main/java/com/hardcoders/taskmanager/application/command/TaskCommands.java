package com.hardcoders.taskmanager.application.command;

import java.math.BigDecimal;

/** Comandos de tareas: creación dentro de una actividad y cambio de plan. */
public final class TaskCommands {

    private TaskCommands() {
    }

    /**
     * US2.2: la tarea se crea dentro de una actividad concreta (y por tanto de un proyecto).
     * {@code activityId} es obligatorio: ya no se deduce del primer proyecto.
     */
    public record CreateTaskCommand(Integer activityId, String title, String priority,
                                    BigDecimal allocatedBudgetBob, Integer estimatedDurationMinutes) {
    }

    /** Presupuesto y duración de la tarea: los fija el Encargado del proyecto (US2.2). */
    public record UpdateTaskPlanCommand(Integer taskId, BigDecimal allocatedBudgetBob,
                                        Integer estimatedDurationMinutes, String reason, Long version) {
    }

    /** Transición de estado con motivo obligatorio cuando la ejecuta el Jefe (US1.3). */
    public record TransitionCommand(Integer id, String reason, Long version) {
    }
}
