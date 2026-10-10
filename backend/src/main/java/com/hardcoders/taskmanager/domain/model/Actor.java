package com.hardcoders.taskmanager.domain.model;

/**
 * Usuario autenticado que ejecuta una operación. Se resuelve en cada petición desde la base
 * (no desde el cliente), para que una baja o un cambio de rol tengan efecto inmediato.
 */
public record Actor(Integer userId, boolean chief, boolean active) {

    public boolean isActiveChief() {
        return chief && active;
    }
}
