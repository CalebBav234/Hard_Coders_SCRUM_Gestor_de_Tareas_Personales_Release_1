package com.hardcoders.taskmanager.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo de estados de proyecto. Tabla {@code task_manager.project_status}. */
@Entity
@Table(name = "project_status", schema = "task_manager")
public class ProjectStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_status_id")
    private Integer projectStatusId;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = Boolean.FALSE;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    public Integer getProjectStatusId() {
        return projectStatusId;
    }

    public void setProjectStatusId(Integer projectStatusId) {
        this.projectStatusId = projectStatusId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsClosed() {
        return isClosed;
    }

    public void setIsClosed(Boolean isClosed) {
        this.isClosed = isClosed;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
