package com.hardcoders.taskmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo de estados de etapas, actividades, tareas y subtareas. Tabla {@code task_manager.work_status}. */
@Entity
@Table(name = "work_status", schema = "task_manager")
public class WorkStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_status_id")
    private Integer workStatusId;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = Boolean.FALSE;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    public Integer getWorkStatusId() {
        return workStatusId;
    }

    public void setWorkStatusId(Integer workStatusId) {
        this.workStatusId = workStatusId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
