package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.command.CreateUserCommand;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;

/** Puerto de entrada para gestión de usuarios (Épica complementaria EC3). */
public interface ManageUsersUseCase {

    /** Crea un usuario no-Jefe. Solo un Jefe activo puede ejecutar la operación. */
    UserSummaryResult create(CreateUserCommand command);
}
