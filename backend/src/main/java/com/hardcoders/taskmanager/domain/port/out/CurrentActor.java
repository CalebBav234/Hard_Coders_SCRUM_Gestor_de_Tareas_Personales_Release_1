package com.hardcoders.taskmanager.domain.port.out;

import com.hardcoders.taskmanager.domain.model.Actor;

/**
 * Identifica al usuario que ejecuta la operación. Lo resuelve el adaptador de seguridad a partir
 * del token JWT; nunca proviene del cuerpo de la petición (INV-010).
 */
public interface CurrentActor {

    Actor current();

    default Integer userId() {
        return current().userId();
    }
}
