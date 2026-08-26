package com.hardcoders.taskmanager.exception;

public class OptimisticLockConflictException extends RuntimeException {

    public OptimisticLockConflictException(Long id, Long expectedVersion, Long actualVersion) {
        super("Conflicto de concurrencia en la tarea " + id
                + ": versión esperada " + expectedVersion
                + ", versión actual " + actualVersion);
    }
}
