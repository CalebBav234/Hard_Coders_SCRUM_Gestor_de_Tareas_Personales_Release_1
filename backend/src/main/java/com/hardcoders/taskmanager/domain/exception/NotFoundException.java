package com.hardcoders.taskmanager.domain.exception;

/** El recurso no existe o ya no es visible (por ejemplo, fue cancelado). */
public class NotFoundException extends DomainException {

    private final String entity;

    public NotFoundException(String entity, Integer id) {
        super(entity + " " + id + " no encontrada");
        this.entity = entity;
    }

    public String entity() {
        return entity;
    }
}
