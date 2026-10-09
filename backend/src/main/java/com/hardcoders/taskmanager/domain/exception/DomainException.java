package com.hardcoders.taskmanager.domain.exception;

/** Base de los errores de negocio. No depende de ningún framework. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
