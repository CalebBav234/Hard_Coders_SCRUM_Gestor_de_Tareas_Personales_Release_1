package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskAssignmentEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskAssignmentId;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskEntity;
import com.hardcoders.taskmanager.domain.model.TaskAssignment;
import com.hardcoders.taskmanager.domain.port.out.AssignmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Adaptador de salida JPA de {@code task_assignment} (clave compuesta tarea + trabajador). */
@Component
class AssignmentPersistenceAdapter implements AssignmentRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<TaskAssignment> find(Integer taskId, Integer workerUserId) {
        return Optional.ofNullable(entityManager.find(TaskAssignmentEntity.class,
                        new TaskAssignmentId(taskId, workerUserId)))
                .map(AssignmentPersistenceAdapter::toDomain);
    }

    @Override
    public List<TaskAssignment> findForTask(Integer taskId) {
        return entityManager.createQuery("""
                        select a from TaskAssignmentEntity a
                        where a.task.taskId = :taskId
                        order by a.assignedAt desc, a.worker.userId
                        """, TaskAssignmentEntity.class)
                .setParameter("taskId", taskId)
                .getResultList().stream()
                .map(AssignmentPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public Map<Integer, List<TaskAssignment>> findForTasks(Collection<Integer> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        List<TaskAssignment> found = entityManager.createQuery("""
                        select a from TaskAssignmentEntity a
                        where a.task.taskId in :taskIds
                        order by a.assignedAt desc, a.worker.userId
                        """, TaskAssignmentEntity.class)
                .setParameter("taskIds", taskIds)
                .getResultList().stream()
                .map(AssignmentPersistenceAdapter::toDomain)
                .toList();
        return found.stream().collect(Collectors.groupingBy(TaskAssignment::taskId));
    }

    @Override
    public List<TaskAssignment> findForWorker(Integer workerUserId) {
        return entityManager.createQuery("""
                        select a from TaskAssignmentEntity a
                        where a.worker.userId = :workerId
                        order by a.assignedAt desc, a.task.taskId desc
                        """, TaskAssignmentEntity.class)
                .setParameter("workerId", workerUserId)
                .getResultList().stream()
                .map(AssignmentPersistenceAdapter::toDomain)
                .sorted(Comparator.comparing(TaskAssignment::assignedAt).reversed())
                .toList();
    }

    @Override
    public TaskAssignment save(TaskAssignment assignment) {
        TaskAssignmentEntity entity = new TaskAssignmentEntity();
        entity.setId(new TaskAssignmentId(assignment.taskId(), assignment.workerUserId()));
        entity.setTask(entityManager.getReference(TaskEntity.class, assignment.taskId()));
        entity.setWorker(entityManager.getReference(AppUserEntity.class, assignment.workerUserId()));
        entity.setAssignedAt(assignment.assignedAt());
        entity.setAssignedBy(entityManager.getReference(AppUserEntity.class, assignment.assignedByUserId()));
        try {
            entityManager.persist(entity);
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            // AC-US2.3-02: la clave compuesta ya existe; la reasigna quien corresponde.
            throw ex;
        }
        return toDomain(entity);
    }

    @Override
    public void delete(Integer taskId, Integer workerUserId) {
        entityManager.createQuery("""
                        delete from TaskAssignmentEntity a
                        where a.task.taskId = :taskId and a.worker.userId = :workerId
                        """)
                .setParameter("taskId", taskId)
                .setParameter("workerId", workerUserId)
                .executeUpdate();
    }

    private static TaskAssignment toDomain(TaskAssignmentEntity entity) {
        return new TaskAssignment(entity.getTask().getTaskId(), entity.getWorker().getUserId(),
                entity.getAssignedAt(), entity.getAssignedBy().getUserId());
    }
}
