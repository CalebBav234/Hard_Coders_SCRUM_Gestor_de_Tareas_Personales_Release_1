package com.hardcoders.taskmanager.application.command;

import java.math.BigDecimal;

/** @param reason motivo de la revisión 1; si es vacío se usa "Plan inicial" */
public record CreateProjectCommand(String name, String description, Integer managerUserId, BigDecimal budgetBob,
                                   Integer estimatedDurationMinutes, String reason) {
}
