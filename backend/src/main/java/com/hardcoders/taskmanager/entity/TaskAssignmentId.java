package com.hardcoders.taskmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaskAssignmentId implements Serializable {

    @Column(name = "task_id", nullable = false)
    private Integer taskId;

    @Column(name = "worker_user_id", nullable = false)
    private Integer workerUserId;

    public TaskAssignmentId() {
    }

    public TaskAssignmentId(Integer taskId, Integer workerUserId) {
        this.taskId = taskId;
        this.workerUserId = workerUserId;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getWorkerUserId() {
        return workerUserId;
    }

    public void setWorkerUserId(Integer workerUserId) {
        this.workerUserId = workerUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaskAssignmentId other)) {
            return false;
        }
        return Objects.equals(taskId, other.taskId) && Objects.equals(workerUserId, other.workerUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, workerUserId);
    }
}
