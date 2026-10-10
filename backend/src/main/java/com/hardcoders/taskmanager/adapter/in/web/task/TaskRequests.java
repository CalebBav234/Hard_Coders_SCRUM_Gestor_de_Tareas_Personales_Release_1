package com.hardcoders.taskmanager.adapter.in.web.task;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Cuerpos de petición HTTP para tareas. La validación técnica vive en el adaptador. */
public final class TaskRequests {

    private TaskRequests() {
    }

    /**
     * US2.2: la actividad es obligatoria, así que la tarea siempre cuelga de un proyecto.
     * El presupuesto asignado y la duración los fija el Encargado del proyecto (o el Jefe).
     */
    public record CreateTaskRequest(
            @NotNull(message = "La actividad es obligatoria")
            Integer activityId,
            @NotBlank(message = "El título es obligatorio")
            @Size(max = 160, message = "El título no puede superar 160 caracteres")
            String title,
            @Pattern(regexp = "ALTA|MEDIA|BAJA", message = "La prioridad debe ser ALTA, MEDIA o BAJA")
            String priority,
            @DecimalMin(value = "0.00", message = "El presupuesto asignado no puede ser negativo")
            @Digits(integer = 13, fraction = 2, message = "El presupuesto asignado admite 13 enteros y 2 decimales")
            BigDecimal allocatedBudgetBob,
            @PositiveOrZero(message = "La duración no puede ser negativa")
            @Max(value = Integer.MAX_VALUE, message = "Duración demasiado grande")
            Integer estimatedDurationMinutes
    ) {
    }

    public record UpdateTaskRequest(
            @NotBlank(message = "El título es obligatorio")
            @Size(max = 160, message = "El título no puede superar 160 caracteres")
            String title,
            @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
            String description,
            @NotBlank(message = "La prioridad es obligatoria")
            @Pattern(regexp = "ALTA|MEDIA|BAJA", message = "La prioridad debe ser ALTA, MEDIA o BAJA")
            String priority,
            @NotNull(message = "La versión es obligatoria")
            @PositiveOrZero(message = "La versión no puede ser negativa")
            Long version
    ) {
    }

    /** US2.2: mover la tarea a otra actividad del mismo proyecto. */
    public record ChangeActivityRequest(
            @NotNull(message = "La actividad es obligatoria")
            Integer activityId,
            @NotNull(message = "La versión es obligatoria")
            @PositiveOrZero(message = "La versión no puede ser negativa")
            Long version
    ) {
    }

    /** US2.2: presupuesto y duración de la tarea, con motivo obligatorio. */
    public record UpdateTaskPlanRequest(
            @NotNull(message = "El presupuesto asignado es obligatorio")
            @DecimalMin(value = "0.00", message = "El presupuesto asignado no puede ser negativo")
            @Digits(integer = 13, fraction = 2, message = "El presupuesto asignado admite 13 enteros y 2 decimales")
            BigDecimal allocatedBudgetBob,
            @NotNull(message = "La duración estimada es obligatoria")
            @PositiveOrZero(message = "La duración no puede ser negativa")
            Integer estimatedDurationMinutes,
            @NotBlank(message = "El motivo del cambio es obligatorio")
            @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
            String reason,
            @NotNull(message = "La versión es obligatoria")
            @PositiveOrZero(message = "La versión no puede ser negativa")
            Long version
    ) {
    }

    /** Transición de estado. El motivo es obligatorio cuando la ejecuta el Jefe (US1.3). */
    public record TransitionRequest(
            @PositiveOrZero(message = "La versión no puede ser negativa")
            Long version,
            @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
            String reason
    ) {
    }

    public record CreateSubtaskRequest(
            @NotBlank(message = "El título es obligatorio")
            @Size(max = 160, message = "El título no puede superar 160 caracteres")
            String title,
            @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
            String description
    ) {
    }

    public record UpdateSubtaskRequest(
            @NotBlank(message = "El título es obligatorio")
            @Size(max = 160, message = "El título no puede superar 160 caracteres")
            String title,
            @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
            String description,
            @NotNull(message = "La versión es obligatoria")
            @PositiveOrZero(message = "La versión no puede ser negativa")
            Long version
    ) {
    }
}
