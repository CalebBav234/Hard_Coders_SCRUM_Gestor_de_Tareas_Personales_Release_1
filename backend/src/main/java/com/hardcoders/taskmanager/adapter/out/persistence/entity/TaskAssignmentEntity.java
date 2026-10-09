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
import java.time.Instant;

/** Asignación de un trabajador a una tarea (clave compuesta). Tabla {@code task_manager.task_assignment}. */
@Entity
@Table(name = "task_assignment", schema = "task_manager")
public class TaskAssignmentEntity {

    @EmbeddedId
    private TaskAssignmentId id = new TaskAssignmentId();

    @MapsId("taskId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task;

    @MapsId("workerUserId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_user_id", nullable = false)
    private AppUserEntity worker;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private AppUserEntity assignedBy;

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }

    public TaskAssignmentId getId() {
        return id;
    }

    public void setId(TaskAssignmentId id) {
        this.id = id;
    }

    public TaskEntity getTask() {
        return task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
    }

    public AppUserEntity getWorker() {
        return worker;
    }

    public void setWorker(AppUserEntity worker) {
        this.worker = worker;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public AppUserEntity getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(AppUserEntity assignedBy) {
        this.assignedBy = assignedBy;
    }
}
