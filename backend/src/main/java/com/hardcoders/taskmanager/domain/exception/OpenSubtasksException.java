package com.hardcoders.taskmanager.domain.exception;

/** Una tarea con subtareas abiertas no puede eliminarse. */
public class OpenSubtasksException extends DomainException {

    public OpenSubtasksException(Integer taskId) {
        super("La tarea " + taskId + " tiene subtareas abiertas; elimínalas o complétalas primero.");
    }
}
