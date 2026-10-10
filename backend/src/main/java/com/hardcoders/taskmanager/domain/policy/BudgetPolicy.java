package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.BudgetBelowAllocationsException;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;

import java.math.BigDecimal;

/**
 * Invariantes financieras de V4 (moneda fija BOB, {@code DECIMAL(15,2)}).
 * INV-FIN-01: presupuesto finito y no negativo. INV-FIN-03: presupuesto ≥ suma asignada a tareas.
 */
public final class BudgetPolicy {

    /** {@code DECIMAL(15,2)}: hasta 13 dígitos enteros y 2 decimales. */
    static final int MAX_INTEGER_DIGITS = 13;
    static final int MAX_SCALE = 2;

    private BudgetPolicy() {
    }

    /** AC-US1.1-03: rechaza importes nulos, negativos o que no caben en {@code DECIMAL(15,2)}. */
    public static void validatePositive(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new DomainValidationException("El presupuesto debe ser un importe en BOB mayor o igual a 0");
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        int scale = Math.max(normalized.scale(), 0);
        int integerDigits = normalized.precision() - normalized.scale();
        if (scale > MAX_SCALE || integerDigits > MAX_INTEGER_DIGITS) {
            throw new DomainValidationException(
                    "El presupuesto admite como máximo 13 dígitos enteros y 2 decimales");
        }
    }

    /** AC-US1.1-04: el presupuesto no puede quedar por debajo de lo asignado a las tareas del proyecto. */
    public static void validateAllocationCap(BigDecimal budget, BigDecimal allocated) {
        BigDecimal assigned = allocated == null ? BigDecimal.ZERO : allocated;
        if (budget.compareTo(assigned) < 0) {
            throw new BudgetBelowAllocationsException(budget, assigned);
        }
    }

    /** Duraciones en minutos ({@code integer >= 0}). */
    public static void validateDuration(Integer minutes) {
        if (minutes == null || minutes < 0) {
            throw new DomainValidationException("La duración estimada debe ser un número de minutos mayor o igual a 0");
        }
    }

    public static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(MAX_SCALE, java.math.RoundingMode.UNNECESSARY);
    }
}
