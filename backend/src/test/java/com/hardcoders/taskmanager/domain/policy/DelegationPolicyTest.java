package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DelegationPolicyTest {

    @Test
    void onlyAnActiveChiefCanDelegate() {
        assertThat(DelegationPolicy.canDelegate(new Actor(1, true, true))).isTrue();
        assertThat(DelegationPolicy.canDelegate(new Actor(1, true, false))).isFalse();
        assertThat(DelegationPolicy.canDelegate(new Actor(2, false, true))).isFalse();
        assertThat(DelegationPolicy.canDelegate(null)).isFalse();
        assertThatThrownBy(() -> DelegationPolicy.requireCanDelegate(new Actor(2, false, true)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void theNewManagerMustExistAndBeActive() {
        UserAccount active = new UserAccount(5, "e@x.com", "Eva", "Rojas", false, true);
        UserAccount inactive = new UserAccount(6, "i@x.com", "Ivo", "Paz", false, false);

        assertThatCode(() -> DelegationPolicy.requireEligibleManager(active)).doesNotThrowAnyException();
        assertThatThrownBy(() -> DelegationPolicy.requireEligibleManager(inactive))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> DelegationPolicy.requireEligibleManager(null))
                .isInstanceOf(DomainValidationException.class);
    }
}
