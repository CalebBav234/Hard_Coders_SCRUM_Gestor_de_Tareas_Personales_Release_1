package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Caminos básicos del aislamiento por actor de US2.1. */
class ProjectAccessPolicyTest {

    private static Project projectManagedBy(Integer managerUserId) {
        return Project.restore(1, "Proyecto", "desc", managerUserId, ProjectStatus.EN_EJECUCION,
                new BigDecimal("100.00"), 60, 1, Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T10:00:00Z"), 1);
    }

    @Test
    @DisplayName("US21-BP-01: actor nulo recorre B1 y no ve el proyecto")
    void canViewRejectsNullActor() {
        assertThat(ProjectAccessPolicy.canView(null, projectManagedBy(2))).isFalse();
    }

    @Test
    @DisplayName("US21-BP-02: actor inactivo recorre B2 y no ve el proyecto (AC-US2.1-04)")
    void canViewRejectsInactiveActor() {
        assertThat(ProjectAccessPolicy.canView(new Actor(2, false, false), projectManagedBy(2))).isFalse();
    }

    @Test
    @DisplayName("US21-BP-03: activo ajeno al proyecto recorre B3 y no lo ve (AC-US2.1-01)")
    void canViewRejectsForeignActiveActor() {
        assertThat(ProjectAccessPolicy.canView(new Actor(9, false, true), projectManagedBy(2))).isFalse();
    }

    @Test
    @DisplayName("US21-BP-04a: jefe activo recorre B4 y ve cualquier proyecto")
    void canViewAllowsChief() {
        assertThat(ProjectAccessPolicy.canView(new Actor(1, true, true), projectManagedBy(2))).isTrue();
    }

    @Test
    @DisplayName("US21-BP-04b: encargado del proyecto recorre B4 y lo ve")
    void canViewAllowsManager() {
        assertThat(ProjectAccessPolicy.canView(new Actor(2, false, true), projectManagedBy(2))).isTrue();
    }

    @Test
    @DisplayName("US21-BP-05: requireChief con actor nulo recorre B1 y lanza 403")
    void requireChiefRejectsNullActor() {
        assertThatThrownBy(() -> ProjectAccessPolicy.requireChief(null, "cambiar el estado"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("US21-BP-06: requireChief con encargado recorre B2 y lanza 403")
    void requireChiefRejectsManager() {
        assertThatThrownBy(() -> ProjectAccessPolicy.requireChief(new Actor(2, false, true),
                "cambiar el estado")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("US21-BP-07: requireChief con jefe activo recorre B3 y continua")
    void requireChiefAllowsActiveChief() {
        assertThatCode(() -> ProjectAccessPolicy.requireChief(new Actor(1, true, true), "cambiar el estado"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("US21-BP-08: requireCanEditDetails rechaza con 403 a quien no puede ver el proyecto")
    void requireCanEditDetailsThrowsForForbiddenActor() {
        assertThatThrownBy(() -> ProjectAccessPolicy.requireCanEditDetails(new Actor(9, false, true),
                projectManagedBy(2))).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("US21-BP-09: requireCanEditDetails deja pasar al encargado del proyecto")
    void requireCanEditDetailsAllowsManager() {
        assertThatCode(() -> ProjectAccessPolicy.requireCanEditDetails(new Actor(2, false, true),
                projectManagedBy(2))).doesNotThrowAnyException();
    }
}
