package com.hardcoders.taskmanager.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** Cambio de estado de una tarea. Tabla {@code task_manager.task_status_history}. */
@Entity
@Table(name = "task_status_history", schema = "task_manager")
public class TaskStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_status_history_id")
    private Integer taskStatusHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_status_id")
    private WorkStatusEntity previousStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "new_status_id", nullable = false)
    private WorkStatusEntity newStatus;

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

    public Integer getTaskStatusHistoryId() {
        return taskStatusHistoryId;
    }

    public void setTaskStatusHistoryId(Integer taskStatusHistoryId) {
        this.taskStatusHistoryId = taskStatusHistoryId;
    }

    public TaskEntity getTask() {
        return task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
    }

    public WorkStatusEntity getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(WorkStatusEntity previousStatus) {
        this.previousStatus = previousStatus;
    }

    public WorkStatusEntity getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(WorkStatusEntity newStatus) {
        this.newStatus = newStatus;
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
