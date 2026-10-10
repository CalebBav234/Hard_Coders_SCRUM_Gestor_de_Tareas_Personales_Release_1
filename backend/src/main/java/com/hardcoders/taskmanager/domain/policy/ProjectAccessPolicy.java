package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;

/**
 * Matriz de permisos del plan (sección 7): el Jefe gestiona todos los proyectos y su presupuesto;
 * el Encargado ve y edita nombre/descripción de los proyectos que dirige; el resto no los ve.
 */
public final class ProjectAccessPolicy {

    private ProjectAccessPolicy() {
    }

    public static boolean canView(Actor actor, Project project) {
        if (actor == null || !actor.active()) {
            return false;
        }
        return actor.chief() || project.managerUserId().equals(actor.userId());
    }

    /** Crear proyectos, cambiar presupuesto/plan y estado: solo el Jefe (AC-US1.1-02). */
    public static void requireChief(Actor actor, String operation) {
        if (actor == null || !actor.isActiveChief()) {
            throw new ForbiddenException("Solo el Jefe puede " + operation);
        }
    }

    /** Nombre y descripción: el Jefe o el Encargado del proyecto. */
    public static void requireCanEditDetails(Actor actor, Project project) {
        if (!canView(actor, project)) {
            throw new ForbiddenException("No puedes editar este proyecto");
        }
    }
}
