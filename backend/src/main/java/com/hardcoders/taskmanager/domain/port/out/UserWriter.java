package com.hardcoders.taskmanager.domain.port.out;

import com.hardcoders.taskmanager.domain.model.UserAccount;

import java.util.Optional;

/** Puerto de salida para crear/consultar usuarios por unicidad (escritura limitada). */
public interface UserWriter {

    boolean existsByEmail(String normalizedEmail);

    /** Persiste un usuario nuevo y devuelve su representación de dominio (sin hash). */
    UserAccount save(String normalizedEmail, String passwordHash, String givenNames, String familyNames);
}
