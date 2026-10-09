package com.hardcoders.taskmanager.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo de prioridades de tarea. Tabla {@code task_manager.task_priority}. */
@Entity
@Table(name = "task_priority", schema = "task_manager")
public class TaskPriorityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_priority_id")
    private Integer taskPriorityId;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    public Integer getTaskPriorityId() {
        return taskPriorityId;
    }

    public void setTaskPriorityId(Integer taskPriorityId) {
        this.taskPriorityId = taskPriorityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
