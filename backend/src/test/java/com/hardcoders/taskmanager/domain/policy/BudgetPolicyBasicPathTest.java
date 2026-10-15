package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.BudgetBelowAllocationsException;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Caminos básicos de las dos unidades de presupuesto de US-1.1. */
class BudgetPolicyBasicPathTest {

    @Test
    @DisplayName("US11-BP-01: importe nulo recorre B1 y se rechaza")
    void validatePositiveRejectsNull() {
        assertThatThrownBy(() -> BudgetPolicy.validatePositive(null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("US11-BP-02: importe negativo recorre B2 y se rechaza")
    void validatePositiveRejectsNegativeAmount() {
        assertThatThrownBy(() -> BudgetPolicy.validatePositive(new BigDecimal("-0.01")))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("US11-BP-03: más de dos decimales recorre B3 y se rechaza")
    void validatePositiveRejectsMoreThanTwoDecimals() {
        assertThatThrownBy(() -> BudgetPolicy.validatePositive(new BigDecimal("10.001")))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("US11-BP-04: más de trece enteros recorre B4 y se rechaza")
    void validatePositiveRejectsMoreThanThirteenIntegerDigits() {
        assertThatThrownBy(() -> BudgetPolicy.validatePositive(new BigDecimal("10000000000000.00")))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("US11-BP-05: importe representable recorre B5 y se acepta")
    void validatePositiveAcceptsRepresentableAmount() {
        assertThatCode(() -> BudgetPolicy.validatePositive(new BigDecimal("1500.50")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("US11-CAP-01: allocated nulo se interpreta como cero")
    void validateAllocationCapTreatsNullAsZero() {
        assertThatCode(() -> BudgetPolicy.validateAllocationCap(new BigDecimal("1.00"), null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("US11-CAP-02: presupuesto igual a lo asignado se acepta")
    void validateAllocationCapAcceptsEquality() {
        assertThatCode(() -> BudgetPolicy.validateAllocationCap(
                new BigDecimal("100.00"), new BigDecimal("100.00")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("US11-CAP-03: presupuesto menor a lo asignado se rechaza")
    void validateAllocationCapRejectsBudgetBelowAllocated() {
        assertThatThrownBy(() -> BudgetPolicy.validateAllocationCap(
                new BigDecimal("99.99"), new BigDecimal("100.00")))
                .isInstanceOf(BudgetBelowAllocationsException.class);
    }
}
