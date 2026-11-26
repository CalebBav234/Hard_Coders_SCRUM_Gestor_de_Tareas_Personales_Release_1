package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.command.CreateUserCommand;
import com.hardcoders.taskmanager.application.command.UpdateUserCommand;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.domain.model.UserAccount;

import java.util.List;

/** Puerto de entrada para gestión de usuarios (Épica complementaria EC3). */
public interface ManageUsersUseCase {

    /** Crea un usuario no-Jefe. Solo un Jefe activo puede ejecutar la operación. */
    UserSummaryResult create(CreateUserCommand command);

    /** Actualiza solo campos de perfil (nombres, apellidos, email) de un usuario existente. Solo Jefe activo. */
    UserSummaryResult update(UpdateUserCommand command);

    /** Desactiva lógicamente un usuario (is_active = false), preservando identidad, FK e historial. Solo Jefe activo. */
    UserSummaryResult deactivate(Integer userId);

    /** Listado de todos los usuarios para administración, sin secretos. Solo Jefe activo. */
    List<UserAccount> listUsers();

    /** Consulta individual de un usuario. Solo Jefe activo. */
    UserAccount getUserById(Integer userId);
}
