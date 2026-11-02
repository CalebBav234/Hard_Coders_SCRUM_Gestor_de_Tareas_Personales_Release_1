package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.exception.ProjectTransitionException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de WbsHierarchyPolicy: gestión, proyectos abiertos, mismo proyecto, nombres y descripciones. */
class WbsHierarchyPolicyTest {

    private static Project project(Integer managerUserId, ProjectStatus status) {
        return Project.restore(1, "Proyecto", "desc", managerUserId, status,
                new BigDecimal("100.00"), 60, 1, Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T10:00:00Z"), 1);
    }

    @Test
    void canManageChiefAndManager() {
        Project p = project(2, ProjectStatus.EN_EJECUCION);
        assertThat(WbsHierarchyPolicy.canManage(new Actor(1, true, true), p)).isTrue();
        assertThat(WbsHierarchyPolicy.canManage(new Actor(2, false, true), p)).isTrue();
        assertThat(WbsHierarchyPolicy.canManage(new Actor(9, false, true), p)).isFalse();
        assertThat(WbsHierarchyPolicy.canManage(null, p)).isFalse();
        assertThat(WbsHierarchyPolicy.canManage(new Actor(2, false, false), p)).isFalse();
    }

    @Test
    void requireCanManageThrowsForNonManager() {
        Project p = project(2, ProjectStatus.EN_EJECUCION);
        assertThatThrownBy(() -> WbsHierarchyPolicy.requireCanManage(new Actor(9, false, true), p, "crear"))
                .isInstanceOf(ForbiddenException.class);
        assertThatCode(() -> WbsHierarchyPolicy.requireCanManage(new Actor(1, true, true), p, "crear"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireOpenThrowsForClosedProject() {
        Project closed = project(2, ProjectStatus.CANCELADO);
        assertThatThrownBy(() -> WbsHierarchyPolicy.requireOpen(closed, "editar"))
                .isInstanceOf(ProjectTransitionException.class);
        assertThatCode(() -> WbsHierarchyPolicy.requireOpen(project(2, ProjectStatus.EN_EJECUCION), "editar"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireSameProjectThrowsOnMismatch() {
        assertThatThrownBy(() -> WbsHierarchyPolicy.requireSameProject(1, 2, "Etapa"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> WbsHierarchyPolicy.requireSameProject(1, null, "Etapa"))
                .isInstanceOf(NotFoundException.class);
        assertThatCode(() -> WbsHierarchyPolicy.requireSameProject(1, 1, "Etapa"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireNameValidates() {
        assertThat(WbsHierarchyPolicy.requireName("  Hola  ", 160, "título")).isEqualTo("Hola");
        assertThatThrownBy(() -> WbsHierarchyPolicy.requireName("   ", 160, "título"))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> WbsHierarchyPolicy.requireName(null, 160, "título"))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> WbsHierarchyPolicy.requireName("x".repeat(161), 160, "título"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void requireDescriptionValidatesAndAllowsNull() {
        assertThat(WbsHierarchyPolicy.requireDescription("  desc  ", 2000, "descripción")).isEqualTo("desc");
        assertThat(WbsHierarchyPolicy.requireDescription(null, 2000, "descripción")).isNull();
        assertThat(WbsHierarchyPolicy.requireDescription("   ", 2000, "descripción")).isNull();
        assertThatThrownBy(() -> WbsHierarchyPolicy.requireDescription("x".repeat(2001), 2000, "descripción"))
                .isInstanceOf(DomainValidationException.class);
    }
}
