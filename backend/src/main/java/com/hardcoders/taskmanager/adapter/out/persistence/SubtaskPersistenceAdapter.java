package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.SubtaskEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskEntity;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Adaptador de salida JPA para {@link Subtask} (tabla {@code subtask}). */
@Component
class SubtaskPersistenceAdapter implements SubtaskRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final CatalogLookup catalogs;

    SubtaskPersistenceAdapter(CatalogLookup catalogs) {
        this.catalogs = catalogs;
    }

    @Override
    public Optional<Subtask> findOpenForUpdate(Integer projectId, Integer subtaskId) {
        SubtaskEntity entity = entityManager.find(SubtaskEntity.class, subtaskId, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null
                || WorkStatus.CANCELADA.name().equals(entity.getStatus().getName())
                || WorkStatus.CANCELADA.name().equals(entity.getTask().getStatus().getName())
                || !entity.getTask().getActivity().getStage().getProject().getProjectId().equals(projectId)) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public Optional<Subtask> findForUpdate(Integer taskId, Integer subtaskId) {
        SubtaskEntity entity = entityManager.find(SubtaskEntity.class, subtaskId, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null
                || WorkStatus.CANCELADA.name().equals(entity.getStatus().getName())
                || WorkStatus.CANCELADA.name().equals(entity.getTask().getStatus().getName())
                || !entity.getTask().getTaskId().equals(taskId)) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public Optional<Integer> taskIdOf(Integer subtaskId) {
        return Optional.ofNullable(entityManager.find(SubtaskEntity.class, subtaskId))
                .map(entity -> entity.getTask().getTaskId());
    }

    @Override
    public List<Subtask> findVisibleByTaskIds(Collection<Integer> taskIds) {
        if (taskIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("""
                        select s from SubtaskEntity s
                        join fetch s.status st
                        where s.task.taskId in :ids and st.name <> 'CANCELADA'
                        order by s.createdAt, s.subtaskId
                        """, SubtaskEntity.class)
                .setParameter("ids", taskIds)
                .getResultList().stream()
                .map(SubtaskPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Subtask> findArchived(Integer projectId, String likePattern) {
        StringBuilder sql = new StringBuilder("""
                SELECT st.subtask_id
                FROM task_manager.subtask st
                JOIN task_manager.work_status ws ON ws.work_status_id = st.work_status_id
                JOIN task_manager.task t ON t.task_id = st.task_id
                JOIN task_manager.activity a ON a.activity_id = t.activity_id
                JOIN task_manager.stage s ON s.stage_id = a.stage_id
                WHERE s.project_id = :projectId
                  AND ws.name IN ('COMPLETADA', 'CANCELADA')
                """);
        if (likePattern != null) {
            sql.append(" AND (")
                    .append(TaskPersistenceAdapter.NORMALIZE.formatted("st.title"))
                    .append(" LIKE :pattern ESCAPE '\\' OR ")
                    .append(TaskPersistenceAdapter.NORMALIZE.formatted("st.description"))
                    .append(" LIKE :pattern ESCAPE '\\')");
        }
        sql.append(" ORDER BY COALESCE(CASE WHEN ws.name = 'COMPLETADA' THEN st.completed_at END, st.updated_at) DESC,")
                .append(" st.subtask_id DESC");
        var query = entityManager.createNativeQuery(sql.toString()).setParameter("projectId", projectId);
        if (likePattern != null) {
            query.setParameter("pattern", likePattern);
        }
        List<Integer> ids = ((List<?>) query.getResultList()).stream()
                .map(row -> ((Number) row).intValue())
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        var byId = entityManager.createQuery(
                        "select s from SubtaskEntity s join fetch s.status where s.subtaskId in :ids",
                        SubtaskEntity.class)
                .setParameter("ids", ids)
                .getResultList().stream()
                .collect(java.util.stream.Collectors.toMap(SubtaskEntity::getSubtaskId, s -> s));
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull)
                .map(SubtaskPersistenceAdapter::toDomain).toList();
    }

    @Override
    public boolean hasOpenSubtasks(Integer taskId) {
        Long open = entityManager.createQuery("""
                        select count(s) from SubtaskEntity s
                        where s.task.taskId = :taskId and s.status.name in ('PENDIENTE', 'EN_PROGRESO')
                        """, Long.class)
                .setParameter("taskId", taskId)
                .getSingleResult();
        return open > 0;
    }

    @Override
    public Subtask save(Subtask subtask) {
        SubtaskEntity entity = subtask.isNew()
                ? new SubtaskEntity()
                : entityManager.find(SubtaskEntity.class, subtask.id());
        entity.setTask(entityManager.getReference(TaskEntity.class, subtask.taskId()));
        entity.setTitle(subtask.title());
        entity.setDescription(subtask.description());
        entity.setStatus(catalogs.workStatus(subtask.status()));
        entity.setCompletedAt(subtask.completedAt());
        entity.setCompletedBy(subtask.completedByUserId() == null
                ? null
                : entityManager.getReference(AppUserEntity.class, subtask.completedByUserId()));
        if (subtask.isNew()) {
            entity.setCreatedAt(subtask.createdAt());
            entity.setUpdatedAt(subtask.updatedAt());
            entityManager.persist(entity);
        }
        entityManager.flush();
        // updated_at lo fija el trigger: se relee para devolver el token de versión real.
        entityManager.refresh(entity);
        return toDomain(entity);
    }

    private static Subtask toDomain(SubtaskEntity entity) {
        return Subtask.restore(
                entity.getSubtaskId(),
                entity.getTask().getTaskId(),
                entity.getTitle(),
                entity.getDescription(),
                WorkStatus.valueOf(entity.getStatus().getName()),
                entity.getCompletedAt(),
                entity.getCompletedBy() == null ? null : entity.getCompletedBy().getUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
