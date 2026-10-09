package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.VersionConflictException;

import java.time.Instant;

/**
 * Control de concurrencia optimista sin columna {@code version}: el token es {@code updated_at}
 * en microsegundos (la precisión de PostgreSQL), que el trigger renueva en cada UPDATE.
 */
public final class VersionToken {

    private VersionToken() {
    }

    public static long of(Instant updatedAt) {
        return updatedAt.getEpochSecond() * 1_000_000L + updatedAt.getNano() / 1_000L;
    }

    /** Un token esperado nulo omite la verificación (compatibilidad con clientes antiguos). */
    public static void verify(String entity, Integer id, Long expected, Instant updatedAt) {
        if (expected != null && expected != of(updatedAt)) {
            throw new VersionConflictException(entity, id);
        }
    }
}
