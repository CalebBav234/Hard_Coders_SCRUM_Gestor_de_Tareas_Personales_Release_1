package com.hardcoders.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(min = 1, max = 160)
        String title,
        @Pattern(regexp = "ALTA|MEDIA|BAJA", message = "La prioridad debe ser ALTA, MEDIA o BAJA")
        String priority,
        @Size(max = 80, message = "La categoría debe tener como máximo 80 caracteres")
        String categoryName,
        Long parentTaskId
) {
}
