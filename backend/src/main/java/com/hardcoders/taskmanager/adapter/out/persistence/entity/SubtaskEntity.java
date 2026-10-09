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
import java.time.Instant;

/** Subtarea de una tarea. Tabla {@code task_manager.subtask}. */
@Entity
@Table(name = "subtask", schema = "task_manager")
public class SubtaskEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subtask_id")
    private Integer subtaskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_status_id", nullable = false)
    private WorkStatusEntity status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_user_id")
    private AppUserEntity completedBy;

    public Integer getSubtaskId() {
        return subtaskId;
    }

    public void setSubtaskId(Integer subtaskId) {
        this.subtaskId = subtaskId;
    }

    public TaskEntity getTask() {
        return task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
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
