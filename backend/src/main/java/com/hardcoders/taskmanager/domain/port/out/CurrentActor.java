package com.hardcoders.taskmanager.domain.port.out;

/**
 * Identifica al usuario que ejecuta la operación, para la atribución de los historiales.
 * Hoy lo resuelve un usuario local; con JWT lo resolverá el adaptador de seguridad.
 */
public interface CurrentActor {

    Integer userId();
}
