package com.hardcoders.taskmanager.adapter.in.web.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Cuerpos de petición de la jerarquía y de las asignaciones (Épica 2). */
public final class HierarchyRequests {

    private HierarchyRequests() {
    }

    public record CreateStageRequest(
            @NotBlank(message = "El nombre de la etapa es obligatorio")
            @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
            String name,
            @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
            String description
    ) {
    }

    public record CreateActivityRequest(
            @NotBlank(message = "El nombre de la actividad es obligatorio")
            @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
            String name,
            @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
            String description
    ) {
    }

    public record UpdateStageRequest(
            @NotBlank(message = "El nombre de la etapa es obligatorio")
            @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
            String name,
            @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
            String description,
            @NotNull(message = "La versión es obligatoria")
            @PositiveOrZero(message = "La versión no puede ser negativa")
            Long version
    ) {
    }

    public record UpdateActivityRequest(
            @NotBlank(message = "El nombre de la actividad es obligatorio")
            @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
            String name,
            @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
            String description,
            @NotNull(message = "La versión es obligatoria")
            @PositiveOrZero(message = "La versión no puede ser negativa")
            Long version
    ) {
    }

    public record StatusRequest(
            @NotBlank(message = "El estado es obligatorio")
            @Pattern(regexp = "PENDIENTE|EN_PROGRESO|COMPLETADA|CANCELADA",
                    message = "El estado debe ser PENDIENTE, EN_PROGRESO, COMPLETADA o CANCELADA")
            String status,
            @NotNull(message = "La versión es obligatoria")
            @PositiveOrZero(message = "La versión no puede ser negativa")
            Long version
    ) {
    }

    public record AssignWorkerRequest(
            @NotNull(message = "El trabajador es obligatorio")
            Integer workerUserId,
            @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
            String reason
    ) {
    }
}
