package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Caminos básicos de las unidades de ejecución autorizada de US-1.3 (override del Jefe). */
class CompletionPolicyBasicPathTest {

    @Test
    @DisplayName("US13-BP-01: actor nulo recorre B1 y se rechaza")
    void canCompleteRejectsNullActor() {
        assertThat(CompletionPolicy.canComplete(null, true)).isFalse();
    }

    @Test
    @DisplayName("US13-BP-02: actor inactivo recorre B2 y se rechaza")
    void canCompleteRejectsInactiveActor() {
        assertThat(CompletionPolicy.canComplete(new Actor(7, false, false), true)).isFalse();
    }

    @Test
    @DisplayName("US13-BP-03: activo sin jefatura ni asignación recorre B3 y se rechaza")
    void canCompleteRejectsUnassignedNonChief() {
        assertThat(CompletionPolicy.canComplete(new Actor(7, false, true), false)).isFalse();
    }

    @Test
    @DisplayName("US13-BP-04a: jefe activo recorre B4 y ejecuta sin estar asignado (AC-US1.3-01)")
    void canCompleteAllowsChiefEvenWithoutAssignment() {
        assertThat(CompletionPolicy.canComplete(new Actor(1, true, true), false)).isTrue();
    }

    @Test
    @DisplayName("US13-BP-04b: personal asignado recorre B4 y ejecuta (AC-US3.2-01)")
    void canCompleteAllowsAssignedWorker() {
        assertThat(CompletionPolicy.canComplete(new Actor(7, false, true), true)).isTrue();
    }

    @Test
    @DisplayName("US13-BP-05: requireCanComplete lanza 403 cuando el actor no puede ejecutar (AC-US1.3-02)")
    void requireCanCompleteThrowsForUnassignedNonChief() {
        assertThatThrownBy(() -> CompletionPolicy.requireCanComplete(new Actor(7, false, true), false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Solo el Personal asignado");
    }

    @Test
    @DisplayName("US13-BP-06: requireCanComplete deja pasar al Jefe sin lanzar")
    void requireCanCompleteAllowsChief() {
        assertThatCode(() -> CompletionPolicy.requireCanComplete(new Actor(1, true, true), false))
                .doesNotThrowAnyException();
    }
}
