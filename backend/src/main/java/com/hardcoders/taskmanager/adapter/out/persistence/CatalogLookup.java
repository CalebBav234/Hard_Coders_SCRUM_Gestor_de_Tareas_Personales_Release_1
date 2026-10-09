package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectStatusEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskPriorityEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.WorkStatusEntity;
import com.hardcoders.taskmanager.domain.model.TaskPriority;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/** Resuelve las filas de los catálogos de V004 por nombre. */
@Component
class CatalogLookup {

    @PersistenceContext
    private EntityManager entityManager;

    WorkStatusEntity workStatus(WorkStatus status) {
        return entityManager.createQuery(
                        "select s from WorkStatusEntity s where s.name = :name", WorkStatusEntity.class)
                .setParameter("name", status.name())
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Falta el estado " + status + " en work_status"));
    }

    TaskPriorityEntity priority(TaskPriority priority) {
        return entityManager.createQuery(
                        "select p from TaskPriorityEntity p where p.name = :name", TaskPriorityEntity.class)
                .setParameter("name", priority.name())
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Falta la prioridad " + priority + " en task_priority"));
    }

    ProjectStatusEntity projectStatus(String name) {
        return entityManager.createQuery(
                        "select p from ProjectStatusEntity p where p.name = :name", ProjectStatusEntity.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Falta el estado " + name + " en project_status"));
    }
}
