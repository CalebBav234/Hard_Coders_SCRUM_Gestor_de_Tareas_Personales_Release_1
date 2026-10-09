package com.hardcoders.taskmanager.adapter.in.web.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Cuerpos de petición HTTP para tareas. La validación técnica vive en el adaptador. */
public final class TaskRequests {

    private TaskRequests() {
    }

    public record CreateTaskRequest(
            @NotBlank(message = "El título es obligatorio")
            @Size(max = 160, message = "El título no puede superar 160 caracteres")
            String title,
            @Pattern(regexp = "ALTA|MEDIA|BAJA", message = "La prioridad debe ser ALTA, MEDIA o BAJA")
            String priority,
            @Size(max = 160, message = "La categoría no puede superar 160 caracteres")
            String activityName
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

    public record ChangeActivityRequest(
            @Size(max = 160, message = "La categoría no puede superar 160 caracteres")
            String activityName,
            @NotNull(message = "La versión es obligatoria")
            @PositiveOrZero(message = "La versión no puede ser negativa")
            Long version
    ) {
    }

    public record VersionRequest(
            @PositiveOrZero(message = "La versión no puede ser negativa")
            Long version
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
