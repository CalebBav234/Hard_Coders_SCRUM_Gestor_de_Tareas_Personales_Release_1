package com.hardcoders.taskmanager.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Proyecto: raíz de la jerarquía de trabajo. Tabla {@code task_manager.project}. */
@Entity
@Table(name = "project", schema = "task_manager")
public class ProjectEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_user_id", nullable = false)
    private AppUserEntity manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_status_id", nullable = false)
    private ProjectStatusEntity status;

    @Column(name = "budget_bob", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetBob = BigDecimal.ZERO.setScale(2);

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUserEntity createdBy;

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AppUserEntity getManager() {
        return manager;
    }

    public void setManager(AppUserEntity manager) {
        this.manager = manager;
    }

    public ProjectStatusEntity getStatus() {
        return status;
    }

    public void setStatus(ProjectStatusEntity status) {
        this.status = status;
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

    public AppUserEntity getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(AppUserEntity createdBy) {
        this.createdBy = createdBy;
    }
}
