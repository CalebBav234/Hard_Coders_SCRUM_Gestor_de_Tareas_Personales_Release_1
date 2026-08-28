package com.hardcoders.taskmanager.exception;

public class TaskHasSubtasksException extends RuntimeException {
    public TaskHasSubtasksException() {
        super("Elimina primero las subtareas de esta tarea.");
    }
}
