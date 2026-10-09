package com.hardcoders.taskmanager.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Revisión del plan (presupuesto/duración) de una tarea. Tabla {@code task_manager.task_plan_history}. */
@Entity
@Table(name = "task_plan_history", schema = "task_manager")
public class TaskPlanHistoryEntity {

    @EmbeddedId
    private TaskPlanHistoryId id = new TaskPlanHistoryId();

    @MapsId("taskId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task;

    @Column(name = "allocated_budget_bob", nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedBudgetBob;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_rate_quote_id")
    private ExchangeRateQuoteEntity exchangeRateQuote;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private AppUserEntity changedBy;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }

    public TaskPlanHistoryId getId() {
        return id;
    }

    public void setId(TaskPlanHistoryId id) {
        this.id = id;
    }

    public TaskEntity getTask() {
        return task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
    }

    public BigDecimal getAllocatedBudgetBob() {
        return allocatedBudgetBob;
    }

    public void setAllocatedBudgetBob(BigDecimal allocatedBudgetBob) {
        this.allocatedBudgetBob = allocatedBudgetBob;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public ExchangeRateQuoteEntity getExchangeRateQuote() {
        return exchangeRateQuote;
    }

    public void setExchangeRateQuote(ExchangeRateQuoteEntity exchangeRateQuote) {
        this.exchangeRateQuote = exchangeRateQuote;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public AppUserEntity getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(AppUserEntity changedBy) {
        this.changedBy = changedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
