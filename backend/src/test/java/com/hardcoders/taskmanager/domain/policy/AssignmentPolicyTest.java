package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de AssignmentPolicy: asignación, trabajador elegible y no autoasignación. */
class AssignmentPolicyTest {

    private static Project project(Integer managerUserId) {
        return Project.restore(1, "Proyecto", "desc", managerUserId, ProjectStatus.EN_EJECUCION,
                new BigDecimal("100.00"), 60, 1, Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T10:00:00Z"), 1);
    }

    @Test
    void canAssignFollowsCanManage() {
        Project p = project(2);
        assertThat(AssignmentPolicy.canAssign(new Actor(1, true, true), p)).isTrue();
        assertThat(AssignmentPolicy.canAssign(new Actor(2, false, true), p)).isTrue();
        assertThat(AssignmentPolicy.canAssign(new Actor(9, false, true), p)).isFalse();
        assertThat(AssignmentPolicy.canAssign(null, p)).isFalse();
    }

    @Test
    void requireCanAssignThrowsForNonManager() {
        Project p = project(2);
        assertThatThrownBy(() -> AssignmentPolicy.requireCanAssign(new Actor(9, false, true), p))
                .isInstanceOf(ForbiddenException.class);
        assertThatCode(() -> AssignmentPolicy.requireCanAssign(new Actor(1, true, true), p))
                .doesNotThrowAnyException();
    }

    @Test
    void requireEligibleWorkerValidates() {
        UserAccount active = new UserAccount(2, "a@x.com", "Ana", "Paz", false, true);
        UserAccount inactive = new UserAccount(3, "i@x.com", "Iván", "Soto", false, false);
        assertThatCode(() -> AssignmentPolicy.requireEligibleWorker(active)).doesNotThrowAnyException();
        assertThatThrownBy(() -> AssignmentPolicy.requireEligibleWorker(null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> AssignmentPolicy.requireEligibleWorker(inactive))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void requireNotSelfPreventsSelfAssignment() {
        Actor actor = new Actor(2, false, true);
        UserAccount same = new UserAccount(2, "a@x.com", "Ana", "Paz", false, true);
        UserAccount other = new UserAccount(3, "o@x.com", "Otro", "User", false, true);
        assertThatThrownBy(() -> AssignmentPolicy.requireNotSelf(actor, same))
                .isInstanceOf(ForbiddenException.class);
        assertThatCode(() -> AssignmentPolicy.requireNotSelf(actor, other)).doesNotThrowAnyException();
        assertThatCode(() -> AssignmentPolicy.requireNotSelf(null, same)).doesNotThrowAnyException();
        assertThatCode(() -> AssignmentPolicy.requireNotSelf(actor, null)).doesNotThrowAnyException();
    }
}
