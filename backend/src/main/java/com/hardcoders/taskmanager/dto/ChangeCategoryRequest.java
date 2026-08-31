package com.hardcoders.taskmanager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ChangeCategoryRequest(
        @Size(max = 80, message = "La categoría debe tener como máximo 80 caracteres")
        String categoryName,
        @NotNull(message = "La versión es obligatoria")
        @PositiveOrZero(message = "La versión no puede ser negativa")
        Long version
) {
}
