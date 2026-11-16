package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Caminos basicos de DelegationPolicy.canDelegate para US-1.2. */
class DelegationPolicyBasicPathTest {

    @Test
    @DisplayName("US12-DP-01: actor nulo corta el AND y devuelve false")
    void nullActorCannotDelegate() {
        assertThat(DelegationPolicy.canDelegate(null)).isFalse();
    }

    @Test
    @DisplayName("US12-DP-02: actor presente sin condicion de Jefe activo devuelve false")
    void inactiveChiefCannotDelegate() {
        assertThat(DelegationPolicy.canDelegate(new Actor(1, true, false))).isFalse();
    }

    @Test
    @DisplayName("US12-DP-03: Jefe activo recorre el camino valido y devuelve true")
    void activeChiefCanDelegate() {
        assertThat(DelegationPolicy.canDelegate(new Actor(1, true, true))).isTrue();
    }

    @Test
    @DisplayName("US12-DP-04: Encargado activo pertenece a la clase invalida")
    void activeNonChiefCannotDelegate() {
        assertThat(DelegationPolicy.canDelegate(new Actor(2, false, true))).isFalse();
    }

    @Test
    @DisplayName("US12-DP-05: requireCanDelegate y requireEligibleManager cubren rechazos y caso valido")
    void requireMethodsCoverBranches() {
        assertThatCode(() -> DelegationPolicy.requireCanDelegate(new Actor(1, true, true))).doesNotThrowAnyException();
        assertThatThrownBy(() -> DelegationPolicy.requireCanDelegate(new Actor(2, false, true)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> DelegationPolicy.requireCanDelegate(null))
                .isInstanceOf(ForbiddenException.class);

        UserAccount active = new UserAccount(9, "n@x.com", "Nora", "Vaca", false, true);
        UserAccount inactive = new UserAccount(8, "i@x.com", "Ina", "Vega", false, false);
        assertThatCode(() -> DelegationPolicy.requireEligibleManager(active)).doesNotThrowAnyException();
        assertThatThrownBy(() -> DelegationPolicy.requireEligibleManager(null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> DelegationPolicy.requireEligibleManager(inactive))
                .isInstanceOf(DomainValidationException.class);
    }
}