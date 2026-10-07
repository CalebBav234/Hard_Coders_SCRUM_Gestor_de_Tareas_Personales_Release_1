package com.hardcoders.taskmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaskPlanHistoryId implements Serializable {

    @Column(name = "task_id", nullable = false)
    private Integer taskId;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    public TaskPlanHistoryId() {
    }

    public TaskPlanHistoryId(Integer taskId, Integer revisionNumber) {
        this.taskId = taskId;
        this.revisionNumber = revisionNumber;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(Integer revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaskPlanHistoryId other)) {
            return false;
        }
        return Objects.equals(taskId, other.taskId) && Objects.equals(revisionNumber, other.revisionNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, revisionNumber);
    }
}
