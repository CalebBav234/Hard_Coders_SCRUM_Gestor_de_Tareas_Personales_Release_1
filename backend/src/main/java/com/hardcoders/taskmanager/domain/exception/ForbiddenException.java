package com.hardcoders.taskmanager.domain.exception;

/** El actor no tiene permiso para la operación (403). */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message);
    }
}
