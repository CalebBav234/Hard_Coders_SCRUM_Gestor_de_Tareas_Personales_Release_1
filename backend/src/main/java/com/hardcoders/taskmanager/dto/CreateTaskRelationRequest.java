package com.hardcoders.taskmanager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateTaskRelationRequest(
        @NotNull(message = "El ID de la tarea destino es obligatorio")
        Long targetTaskId,
        
        @Pattern(regexp = "RELACIONADA|BLOQUEA|DEPENDE_DE", message = "Tipo de relación inválido")
        String relationType
) {}
