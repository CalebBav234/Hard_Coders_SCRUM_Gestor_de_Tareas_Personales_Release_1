package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.UserAccount;

/**
 * US2.3: asignación de Personal a tareas. Solo asigna el Encargado del proyecto (o el Jefe,
 * por la excepción autorizada de US1.3); el trabajador debe estar activo y no puede asignarse a
 * sí mismo. La duplicidad la protege la clave compuesta de V004.
 */
public final class AssignmentPolicy {

    private AssignmentPolicy() {
    }

    public static boolean canAssign(Actor actor, Project project) {
        return WbsHierarchyPolicy.canManage(actor, project);
    }

    public static void requireCanAssign(Actor actor, Project project) {
        if (!canAssign(actor, project)) {
            throw new ForbiddenException("Solo el Encargado del proyecto puede asignar Personal");
        }
    }

    /** AC-US2.3-01: el trabajador debe existir y estar activo. */
    public static void requireEligibleWorker(UserAccount worker) {
        if (worker == null) {
            throw new DomainValidationException("El usuario indicado no existe");
        }
        if (!worker.active()) {
            throw new DomainValidationException("No se puede asignar a un usuario inactivo");
        }
    }

    /** AC-US2.3-03: nadie se asigna trabajo a sí mismo. */
    public static void requireNotSelf(Actor actor, UserAccount worker) {
        if (actor != null && worker != null && worker.userId().equals(actor.userId())) {
            throw new ForbiddenException("No puedes asignarte trabajo a ti mismo");
        }
    }
}
