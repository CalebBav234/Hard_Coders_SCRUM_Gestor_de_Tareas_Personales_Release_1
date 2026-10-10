package com.hardcoders.taskmanager.domain.exception;

import java.math.BigDecimal;

/** INV-FIN-03: el presupuesto del proyecto no puede quedar por debajo de lo asignado a sus tareas (409). */
public class BudgetBelowAllocationsException extends DomainException {

    public BudgetBelowAllocationsException(BigDecimal budget, BigDecimal allocated) {
        super("El presupuesto " + budget.toPlainString() + " BOB es menor que lo ya asignado a las tareas ("
                + allocated.toPlainString() + " BOB)");
    }
}
