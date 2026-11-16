package com.hardcoders.taskmanager.application.command;

/** Comando para crear un usuario de la aplicación (solo Jefe). */
public record CreateUserCommand(
        String email,
        String givenNames,
        String familyNames,
        String rawPassword
) {
}
