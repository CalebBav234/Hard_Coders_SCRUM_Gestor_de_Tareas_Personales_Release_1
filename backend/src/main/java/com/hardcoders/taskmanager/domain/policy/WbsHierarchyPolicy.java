package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.exception.ProjectTransitionException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;

/**
 * US2.1 / US2.2: aislamiento y construcción de la jerarquía.
 * El Encargado solo opera en los proyectos que le fueron delegados; el Jefe puede hacerlo en
 * cualquiera (excepción autorizada de US1.3). Ningún cruce de proyecto es posible: la etapa, la
 * actividad y la tarea se validan siempre contra el proyecto del contexto.
 */
public final class WbsHierarchyPolicy {

    private WbsHierarchyPolicy() {
    }

    /** Jefe (cualquier proyecto) o Encargado del proyecto (AC-US2.1-01, AC-US2.1-04). */
    public static boolean canManage(Actor actor, Project project) {
        return actor != null && actor.active()
                && (actor.chief() || project.managerUserId().equals(actor.userId()));
    }

    public static void requireCanManage(Actor actor, Project project, String operation) {
        if (!canManage(actor, project)) {
            throw new ForbiddenException("Solo el Encargado del proyecto puede " + operation);
        }
    }

    /** Los proyectos cerrados no admiten cambios de plan ni de estructura. */
    public static void requireOpen(Project project, String operation) {
        if (project.status().isClosed()) {
            throw new ProjectTransitionException("No se puede " + operation + " en un proyecto "
                    + project.status());
        }
    }

    /** AC-US2.2-02: un padre de otro proyecto se trata como inexistente (no se filtra su existencia). */
    public static void requireSameProject(Integer expectedProjectId, Integer actualProjectId, String entity) {
        if (actualProjectId == null || !actualProjectId.equals(expectedProjectId)) {
            throw new NotFoundException(entity, actualProjectId);
        }
    }

    /** Nombre obligatorio y con longitud máxima de V004 ({@code varchar(160)}). */
    public static String requireName(String value, int maxLength, String field) {
        if (value == null || value.strip().isEmpty()) {
            throw new DomainValidationException("El " + field + " es obligatorio");
        }
        String stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new DomainValidationException("El " + field + " no puede superar " + maxLength + " caracteres");
        }
        return stripped;
    }

    /** Descripción opcional con longitud máxima de V004 ({@code varchar(2000)}). */
    public static String requireDescription(String value, int maxLength, String field) {
        if (value == null || value.strip().isEmpty()) {
            return null;
        }
        String stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new DomainValidationException("La " + field + " no puede superar " + maxLength + " caracteres");
        }
        return stripped;
    }
}
