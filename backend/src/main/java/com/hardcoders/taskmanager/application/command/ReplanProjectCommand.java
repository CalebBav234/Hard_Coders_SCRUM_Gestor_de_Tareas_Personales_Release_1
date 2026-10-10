package com.hardcoders.taskmanager.application.command;

import java.math.BigDecimal;

public record ReplanProjectCommand(Integer projectId, BigDecimal budgetBob, Integer estimatedDurationMinutes,
                                   String reason, Long version) {
}
