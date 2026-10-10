package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;

/**
 * US3.2 / US1.3: quién puede ejecutar (iniciar, pausar, completar, reabrir) una tarea o subtarea.
 * El Personal solo actúa sobre el trabajo que tiene asignado (AC-US3.2-01); el Jefe puede hacerlo
 * en cualquier proyecto como excepción autorizada (AC-US1.3-01). El Encargado planifica y asigna,
 * pero no ejecuta el trabajo de otros.
 */
public final class CompletionPolicy {

    private CompletionPolicy() {
    }

    public static boolean canComplete(Actor actor, boolean assigned) {
        if (actor == null || !actor.active()) {
            return false;
        }
        return actor.chief() || assigned;
    }

    public static void requireCanComplete(Actor actor, boolean assigned) {
        if (!canComplete(actor, assigned)) {
            throw new ForbiddenException("Solo el Personal asignado puede ejecutar esta tarea");
        }
    }
}
