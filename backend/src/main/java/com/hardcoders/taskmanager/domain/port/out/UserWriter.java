package com.hardcoders.taskmanager.domain.port.out;

import com.hardcoders.taskmanager.domain.model.UserAccount;

import java.util.Optional;

/** Puerto de salida para crear/actualizar/desactivar usuarios y consultar unicidad por email. */
public interface UserWriter {

    boolean existsByEmail(String normalizedEmail);

    /** Persiste un usuario nuevo y devuelve su representación de dominio (sin hash). */
    UserAccount save(String normalizedEmail, String passwordHash, String givenNames, String familyNames);

    /** Actualiza solo campos de perfil de un usuario existente. Devuelve vacío si no existe. */
    Optional<UserAccount> update(Integer userId, String email, String givenNames, String familyNames);

    /** Desactiva lógicamente un usuario (is_active = false) preservando identidad, FK e historial. */
    Optional<UserAccount> deactivate(Integer userId);
}
