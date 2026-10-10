package com.hardcoders.taskmanager.adapter.in.web.project;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Cuerpos de petición de proyectos. El actor nunca viaja en el cuerpo: sale del token (INV-010). */
public final class ProjectRequests {

    private ProjectRequests() {
    }

    public record CreateProjectRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
            String name,
            @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
            String description,
            @NotNull(message = "El Encargado es obligatorio")
            Integer managerUserId,
            @NotNull(message = "El presupuesto es obligatorio")
            @DecimalMin(value = "0.00", message = "El presupuesto no puede ser negativo")
            @Digits(integer = 13, fraction = 2, message = "El presupuesto admite 13 enteros y 2 decimales")
            BigDecimal budgetBob,
            @NotNull(message = "La duración estimada es obligatoria")
            @PositiveOrZero(message = "La duración no puede ser negativa")
            @Max(value = Integer.MAX_VALUE, message = "Duración demasiado grande")
            Integer estimatedDurationMinutes,
            @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
            String reason
    ) {
    }

    public record UpdateProjectDetailsRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
            String name,
            @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
            String description,
            @NotNull(message = "La versión es obligatoria")
            Long version
    ) {
    }

    public record ReplanProjectRequest(
            @NotNull(message = "El presupuesto es obligatorio")
            @DecimalMin(value = "0.00", message = "El presupuesto no puede ser negativo")
            @Digits(integer = 13, fraction = 2, message = "El presupuesto admite 13 enteros y 2 decimales")
            BigDecimal budgetBob,
            @NotNull(message = "La duración estimada es obligatoria")
            @PositiveOrZero(message = "La duración no puede ser negativa")
            Integer estimatedDurationMinutes,
            @NotBlank(message = "El motivo del cambio es obligatorio")
            @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
            String reason,
            @NotNull(message = "La versión es obligatoria")
            Long version
    ) {
    }

    public record ChangeProjectStatusRequest(
            @NotBlank(message = "El estado es obligatorio")
            String status,
            @NotNull(message = "La versión es obligatoria")
            Long version
    ) {
    }

    public record DelegateProjectRequest(
            @NotNull(message = "El nuevo Encargado es obligatorio")
            Integer managerUserId,
            @NotBlank(message = "El motivo de la delegación es obligatorio")
            @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
            String reason,
            @NotNull(message = "La versión es obligatoria")
            Long version
    ) {
    }
}
