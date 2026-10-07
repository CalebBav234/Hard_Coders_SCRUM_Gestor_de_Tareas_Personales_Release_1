package com.hardcoders.taskmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProjectPlanHistoryId implements Serializable {

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    public ProjectPlanHistoryId() {
    }

    public ProjectPlanHistoryId(Integer projectId, Integer revisionNumber) {
        this.projectId = projectId;
        this.revisionNumber = revisionNumber;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
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
        if (!(o instanceof ProjectPlanHistoryId other)) {
            return false;
        }
        return Objects.equals(projectId, other.projectId) && Objects.equals(revisionNumber, other.revisionNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, revisionNumber);
    }
}
