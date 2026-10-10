package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.UserAccount;

/** US1.2: quién puede delegar un proyecto y a quién. */
public final class DelegationPolicy {

    private DelegationPolicy() {
    }

    /** Solo un Jefe activo delega o reasigna proyectos (AC-US1.2-02). */
    public static boolean canDelegate(Actor delegator) {
        return delegator != null && delegator.isActiveChief();
    }

    public static void requireCanDelegate(Actor delegator) {
        if (!canDelegate(delegator)) {
            throw new ForbiddenException("Solo un Jefe puede delegar proyectos");
        }
    }

    /** El nuevo Encargado debe existir y estar activo (AC-US1.2-03). */
    public static void requireEligibleManager(UserAccount candidate) {
        if (candidate == null) {
            throw new DomainValidationException("El usuario indicado no existe");
        }
        if (!candidate.active()) {
            throw new DomainValidationException("No se puede delegar a un usuario inactivo");
        }
    }
}
