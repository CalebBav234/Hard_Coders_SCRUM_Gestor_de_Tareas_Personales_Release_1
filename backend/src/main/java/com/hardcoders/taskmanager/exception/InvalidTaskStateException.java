package com.hardcoders.taskmanager.exception;

public class InvalidTaskStateException extends RuntimeException {

    public InvalidTaskStateException(String currentStatus, String targetStatus) {
        super("Transición de estado no permitida: " + currentStatus + " -> " + targetStatus);
    }
}
