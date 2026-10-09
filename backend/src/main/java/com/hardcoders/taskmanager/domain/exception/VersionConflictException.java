package com.hardcoders.taskmanager.domain.exception;

/** El cliente envió un token de versión desactualizado (otro proceso modificó el registro). */
public class VersionConflictException extends DomainException {

    public VersionConflictException(String entity, Integer id) {
        super(entity + " " + id + " fue modificada por otro proceso. Actualiza y vuelve a intentarlo.");
    }
}
