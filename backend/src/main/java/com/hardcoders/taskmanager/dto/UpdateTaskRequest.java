package com.hardcoders.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Only descriptive fields can be edited; lifecycle data is managed by the service. */
public record UpdateTaskRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 160, message = "El título no puede superar 160 caracteres")
        String title,
        @Size(max = 4000, message = "La descripción no puede superar 4000 caracteres")
        String description,
        @NotBlank(message = "La prioridad es obligatoria")
        @Pattern(regexp = "ALTA|MEDIA|BAJA", message = "La prioridad debe ser ALTA, MEDIA o BAJA")
        String priority,
        @NotNull(message = "La versión es obligatoria")
        @PositiveOrZero(message = "La versión no puede ser negativa")
        Long version
) {
}
