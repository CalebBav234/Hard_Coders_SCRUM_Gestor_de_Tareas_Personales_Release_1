package com.hardcoders.taskmanager.domain.exception;

/** Datos que violan una regla del dominio (título vacío, texto demasiado largo, etc.). */
public class DomainValidationException extends DomainException {

    public DomainValidationException(String message) {
        super(message);
    }
}
