package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.BudgetBelowAllocationsException;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetPolicyTest {

    @Test
    void acceptsZeroAndRegularAmounts() {
        assertThatCode(() -> BudgetPolicy.validatePositive(new BigDecimal("0.00"))).doesNotThrowAnyException();
        assertThatCode(() -> BudgetPolicy.validatePositive(new BigDecimal("1500.50"))).doesNotThrowAnyException();
        assertThatCode(() -> BudgetPolicy.validatePositive(new BigDecimal("9999999999999.99")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullNegativeAndOutOfRangeAmounts() {
        assertThatThrownBy(() -> BudgetPolicy.validatePositive(null)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> BudgetPolicy.validatePositive(new BigDecimal("-5.00")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> BudgetPolicy.validatePositive(new BigDecimal("10.001")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> BudgetPolicy.validatePositive(new BigDecimal("10000000000000.00")))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void budgetCannotDropBelowWhatTasksAlreadyHave() {
        assertThatCode(() -> BudgetPolicy.validateAllocationCap(new BigDecimal("100.00"), new BigDecimal("100.00")))
                .doesNotThrowAnyException();
        assertThatCode(() -> BudgetPolicy.validateAllocationCap(new BigDecimal("1.00"), null))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> BudgetPolicy.validateAllocationCap(new BigDecimal("99.99"), new BigDecimal("100.00")))
                .isInstanceOf(BudgetBelowAllocationsException.class);
    }

    @Test
    void durationsAreNonNegativeMinutes() {
        assertThatCode(() -> BudgetPolicy.validateDuration(0)).doesNotThrowAnyException();
        assertThatThrownBy(() -> BudgetPolicy.validateDuration(-1)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> BudgetPolicy.validateDuration(null)).isInstanceOf(DomainValidationException.class);
    }
}
