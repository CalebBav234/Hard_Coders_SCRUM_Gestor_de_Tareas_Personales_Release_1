package com.hardcoders.taskmanager.exception;

public class InvalidParentTaskException extends RuntimeException {

    public InvalidParentTaskException(Long parentTaskId) {
        super("La tarea " + parentTaskId + " ya es una subtarea; no se pueden crear subtareas de otra subtarea.");
    }
}
