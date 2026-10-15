package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.model.Actor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Caminos básicos de DelegationPolicy.canDelegate para US-1.2. */
class DelegationPolicyBasicPathTest {

    @Test
    @DisplayName("US12-DP-01: actor nulo corta el AND y devuelve false")
    void nullActorCannotDelegate() {
        assertThat(DelegationPolicy.canDelegate(null)).isFalse();
    }

    @Test
    @DisplayName("US12-DP-02: actor presente sin condición de Jefe activo devuelve false")
    void inactiveChiefCannotDelegate() {
        assertThat(DelegationPolicy.canDelegate(new Actor(1, true, false))).isFalse();
    }

    @Test
    @DisplayName("US12-DP-03: Jefe activo recorre el camino válido y devuelve true")
    void activeChiefCanDelegate() {
        assertThat(DelegationPolicy.canDelegate(new Actor(1, true, true))).isTrue();
    }

    @Test
    @DisplayName("US12-DP-04: Encargado activo pertenece a la clase inválida")
    void activeNonChiefCannotDelegate() {
        assertThat(DelegationPolicy.canDelegate(new Actor(2, false, true))).isFalse();
    }
}
