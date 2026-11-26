package com.hardcoders.taskmanager.application.command;

/** Comando para actualizar campos de perfil de un usuario (solo Jefe activo). */
public record UpdateUserCommand(
        Integer userId,
        String email,
        String givenNames,
        String familyNames
) {
}
