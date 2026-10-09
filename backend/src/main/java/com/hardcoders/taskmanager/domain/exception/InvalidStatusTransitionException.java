package com.hardcoders.taskmanager.domain.exception;

import com.hardcoders.taskmanager.domain.model.WorkStatus;

/** Transición de estado no permitida por el ciclo de vida. */
public class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(WorkStatus from, WorkStatus to) {
        super("No se puede pasar de " + from + " a " + to);
    }
}
