package com.hardcoders.taskmanager.application.result;

import java.util.List;

/** Nodo de progreso en cascada (US3.3). */
public record ProgressNodeResult(
        Integer id,
        String name,
        long completed,
        long total,
        boolean leaf,
        double ratio,
        List<ProgressNodeResult> children
) {
}
