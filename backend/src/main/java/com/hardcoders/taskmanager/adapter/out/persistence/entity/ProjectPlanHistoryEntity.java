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

/** Revisión del plan (presupuesto/duración) de un proyecto. Tabla {@code task_manager.project_plan_history}. */
@Entity
@Table(name = "project_plan_history", schema = "task_manager")
public class ProjectPlanHistoryEntity {

    @EmbeddedId
    private ProjectPlanHistoryId id = new ProjectPlanHistoryId();

    @MapsId("projectId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "budget_bob", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetBob;

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

    public ProjectPlanHistoryId getId() {
        return id;
    }

    public void setId(ProjectPlanHistoryId id) {
        this.id = id;
    }

    public ProjectEntity getProject() {
        return project;
    }

    public void setProject(ProjectEntity project) {
        this.project = project;
    }

    public BigDecimal getBudgetBob() {
        return budgetBob;
    }

    public void setBudgetBob(BigDecimal budgetBob) {
        this.budgetBob = budgetBob;
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
