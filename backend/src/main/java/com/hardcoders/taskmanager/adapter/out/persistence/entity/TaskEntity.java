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
import java.time.Instant;

/** Tarea de una actividad. Tabla {@code task_manager.task}. */
@Entity
@Table(name = "task", schema = "task_manager")
public class TaskEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer taskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private ActivityEntity activity;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_status_id", nullable = false)
    private WorkStatusEntity status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_priority_id", nullable = false)
    private TaskPriorityEntity priority;

    @Column(name = "allocated_budget_bob", nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedBudgetBob = BigDecimal.ZERO.setScale(2);

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes = 0;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_user_id")
    private AppUserEntity completedBy;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public ActivityEntity getActivity() {
        return activity;
    }

    public void setActivity(ActivityEntity activity) {
        this.activity = activity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkStatusEntity getStatus() {
        return status;
    }

    public void setStatus(WorkStatusEntity status) {
        this.status = status;
    }

    public TaskPriorityEntity getPriority() {
        return priority;
    }

    public void setPriority(TaskPriorityEntity priority) {
        this.priority = priority;
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

    public Integer getActualDurationMinutes() {
        return actualDurationMinutes;
    }

    public void setActualDurationMinutes(Integer actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public AppUserEntity getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(AppUserEntity completedBy) {
        this.completedBy = completedBy;
    }
}
