package com.hardcoders.taskmanager.domain.exception;

import com.hardcoders.taskmanager.domain.model.ProjectStatus;

/** Cambio de estado o de plan no permitido por el ciclo de vida del proyecto (409). */
public class ProjectTransitionException extends DomainException {

    public ProjectTransitionException(ProjectStatus from, ProjectStatus to) {
        super("El proyecto no puede pasar de " + from + " a " + to);
    }

    public ProjectTransitionException(String message) {
        super(message);
    }
}
