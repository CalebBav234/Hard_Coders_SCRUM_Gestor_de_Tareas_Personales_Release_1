package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ActivityEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskPlanHistoryEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskStatusHistoryEntity;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.PlanRevision;
import com.hardcoders.taskmanager.domain.model.StatusChange;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.TaskPriority;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Adaptador de salida JPA para {@link Task}: tabla {@code task} + {@code task_status_history} + {@code task_plan_history}. */
@Component
class TaskPersistenceAdapter implements TaskRepository {

    /** Misma normalización que {@code SearchText}: minúsculas y sin acentos. */
    static final String NORMALIZE = "translate(lower(coalesce(%s, '')), 'áéíóúüñ', 'aeiouun')";

    private static final String VISIBLE_STATUSES = "('PENDIENTE', 'EN_PROGRESO', 'COMPLETADA')";
    private static final String ARCHIVED_STATUSES = "('COMPLETADA', 'CANCELADA')";

    @PersistenceContext
    private EntityManager entityManager;

    private final CatalogLookup catalogs;

    TaskPersistenceAdapter(CatalogLookup catalogs) {
        this.catalogs = catalogs;
    }

    @Override
    public Optional<Task> findOpenForUpdate(Integer projectId, Integer taskId) {
        // Bloquea solo la fila de task (SELECT ... FOR UPDATE), sin arrastrar los catálogos.
        TaskEntity entity = entityManager.find(TaskEntity.class, taskId, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null || !belongsTo(entity, projectId)
                || WorkStatus.CANCELADA.name().equals(entity.getStatus().getName())) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity, history(List.of(taskId)).getOrDefault(taskId, List.of()),
                lastRevisions(List.of(taskId)).getOrDefault(taskId, 0)));
    }

    @Override
    public Optional<Task> findVisible(Integer projectId, Integer taskId) {
        return load(idsQuery(projectId, VISIBLE_STATUSES, null, "t.task_id = :taskId", taskId)).stream().findFirst();
    }

    @Override
    public List<Task> findVisible(Integer projectId, String likePattern) {
        return load(idsQuery(projectId, VISIBLE_STATUSES, likePattern, null, null));
    }

    @Override
    public List<Task> findArchived(Integer projectId, String likePattern) {
        return load(idsQuery(projectId, ARCHIVED_STATUSES, likePattern, null, null));
    }

    @Override
    public List<Task> findAssignedTo(Integer workerUserId, String likePattern) {
        List<Integer> ids = assignedTaskIds(workerUserId, likePattern);
        return load(ids);
    }

    @Override
    public Optional<Task> findAssignedForUpdate(Integer workerUserId, Integer taskId) {
        if (assignedTaskIds(workerUserId, null).contains(taskId)) {
            return findOpenForUpdate(projectIdOf(taskId).orElse(null), taskId);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Integer> projectIdOf(Integer taskId) {
        return Optional.ofNullable(entityManager.find(TaskEntity.class, taskId))
                .map(entity -> entity.getActivity().getStage().getProject().getProjectId());
    }

    @Override
    public Task save(Task task) {
        TaskEntity entity = task.isNew() ? new TaskEntity() : entityManager.find(TaskEntity.class, task.id());
        entity.setActivity(entityManager.getReference(ActivityEntity.class, task.activity().id()));
        entity.setTitle(task.title());
        entity.setDescription(task.description());
        entity.setStatus(catalogs.workStatus(task.status()));
        entity.setPriority(catalogs.priority(task.priority()));
        entity.setActualDurationMinutes(task.actualDurationMinutes());
        entity.setCompletedAt(task.completedAt());
        entity.setCompletedBy(task.completedByUserId() == null
                ? null : entityManager.getReference(AppUserEntity.class, task.completedByUserId()));

        if (task.isNew()) {
            entity.setAllocatedBudgetBob(task.allocatedBudgetBob());
            entity.setEstimatedDurationMinutes(task.estimatedDurationMinutes());
            entity.setCreatedAt(task.createdAt());
            entity.setUpdatedAt(task.updatedAt());
            entityManager.persist(entity);
            entityManager.flush();
        }
        for (PlanRevision revision : task.pendingRevisions()) {
            persistPlanRevision(entity, revision);
        }
        for (StatusChange change : task.pendingChanges()) {
            persistStatusChange(entity, change);
        }

        entityManager.flush();
        // updated_at lo fija el trigger: se relee para devolver el token de versión real.
        entityManager.refresh(entity);
        Integer id = entity.getTaskId();
        return toDomain(entity, history(List.of(id)).getOrDefault(id, List.of()),
                lastRevisions(List.of(id)).getOrDefault(id, 0));
    }

    private void persistPlanRevision(TaskEntity entity, PlanRevision revision) {
        // TASK ||--|{ TASK_PLAN_HISTORY: la revisión 1 se crea junto con la tarea.
        TaskPlanHistoryEntity row = new TaskPlanHistoryEntity();
        row.setTask(entity);
        row.getId().setRevisionNumber(revision.revisionNumber());
        row.setAllocatedBudgetBob(revision.budgetBob());
        row.setEstimatedDurationMinutes(revision.estimatedDurationMinutes());
        row.setChangedAt(revision.changedAt());
        row.setChangedBy(entityManager.getReference(AppUserEntity.class, revision.changedByUserId()));
        row.setReason(revision.reason());
        entityManager.persist(row);
    }

    private void persistStatusChange(TaskEntity entity, StatusChange change) {
        TaskStatusHistoryEntity row = new TaskStatusHistoryEntity();
        row.setTask(entity);
        row.setPreviousStatus(change.previousStatus() == null ? null : catalogs.workStatus(change.previousStatus()));
        row.setNewStatus(catalogs.workStatus(change.newStatus()));
        row.setChangedAt(change.changedAt());
        row.setChangedBy(entityManager.getReference(AppUserEntity.class, change.changedByUserId()));
        row.setReason(change.reason());
        entityManager.persist(row);
    }

    private boolean belongsTo(TaskEntity entity, Integer projectId) {
        return projectId != null
                && entity.getActivity().getStage().getProject().getProjectId().equals(projectId);
    }

    private List<Integer> assignedTaskIds(Integer workerUserId, String likePattern) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.task_id
                FROM task_manager.task t
                JOIN task_manager.activity a ON a.activity_id = t.activity_id
                JOIN task_manager.stage s ON s.stage_id = a.stage_id
                JOIN task_manager.work_status ws ON ws.work_status_id = t.work_status_id
                JOIN task_manager.task_assignment ta ON ta.task_id = t.task_id
                WHERE ta.worker_user_id = :workerId
                  AND ws.name IN\s""").append(VISIBLE_STATUSES);
        if (likePattern != null) {
            sql.append(" AND (").append(NORMALIZE.formatted("t.title"))
                    .append(" LIKE :pattern ESCAPE '\\' OR ")
                    .append(NORMALIZE.formatted("t.description")).append(" LIKE :pattern ESCAPE '\\')");
        }
        sql.append(" ORDER BY t.created_at DESC, t.task_id DESC");
        var query = entityManager.createNativeQuery(sql.toString()).setParameter("workerId", workerUserId);
        if (likePattern != null) {
            query.setParameter("pattern", likePattern);
        }
        return ((List<?>) query.getResultList()).stream()
                .map(row -> ((Number) row).intValue()).toList();
    }

    private List<Integer> idsQuery(Integer projectId, String statuses, String likePattern,
                                   String extraCondition, Integer taskId) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.task_id
                FROM task_manager.task t
                JOIN task_manager.activity a ON a.activity_id = t.activity_id
                JOIN task_manager.stage s ON s.stage_id = a.stage_id
                JOIN task_manager.work_status ws ON ws.work_status_id = t.work_status_id
                WHERE s.project_id = :projectId
                  AND ws.name IN\s""").append(statuses);
        if (extraCondition != null) {
            sql.append(" AND ").append(extraCondition);
        }
        if (likePattern != null) {
            sql.append(" AND (")
                    .append(NORMALIZE.formatted("t.title")).append(" LIKE :pattern ESCAPE '\\' OR ")
                    .append(NORMALIZE.formatted("t.description")).append(" LIKE :pattern ESCAPE '\\' OR EXISTS (")
                    .append("SELECT 1 FROM task_manager.subtask st ")
                    .append("JOIN task_manager.work_status sws ON sws.work_status_id = st.work_status_id ")
                    .append("WHERE st.task_id = t.task_id AND sws.name <> 'CANCELADA' AND (")
                    .append(NORMALIZE.formatted("st.title")).append(" LIKE :pattern ESCAPE '\\' OR ")
                    .append(NORMALIZE.formatted("st.description")).append(" LIKE :pattern ESCAPE '\\')))");
        }
        sql.append(" ORDER BY CASE WHEN ws.name IN ('COMPLETADA', 'CANCELADA')")
                .append(" THEN COALESCE(CASE WHEN ws.name = 'COMPLETADA' THEN t.completed_at END, t.updated_at)")
                .append(" ELSE t.created_at END DESC, t.task_id DESC");

        var query = entityManager.createNativeQuery(sql.toString()).setParameter("projectId", projectId);
        if (likePattern != null) {
            query.setParameter("pattern", likePattern);
        }
        if (taskId != null) {
            query.setParameter("taskId", taskId);
        }
        return ((List<?>) query.getResultList()).stream()
                .map(row -> ((Number) row).intValue()).toList();
    }

    private List<Task> load(List<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Integer, TaskEntity> entities = entityManager.createQuery("""
                        select t from TaskEntity t
                        join fetch t.activity
                        join fetch t.status
                        join fetch t.priority
                        where t.taskId in :ids
                        """, TaskEntity.class)
                .setParameter("ids", ids)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(TaskEntity::getTaskId, Function.identity()));
        Map<Integer, List<StatusChange>> histories = history(ids);
        Map<Integer, Integer> revisions = lastRevisions(ids);
        List<Task> tasks = new ArrayList<>(ids.size());
        for (Integer id : ids) {
            TaskEntity entity = entities.get(id);
            if (entity != null) {
                tasks.add(toDomain(entity, histories.getOrDefault(id, List.of()), revisions.getOrDefault(id, 0)));
            }
        }
        return tasks;
    }

    private Map<Integer, List<StatusChange>> history(Collection<Integer> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = entityManager.createQuery("""
                        select h.task.taskId, h.taskStatusHistoryId, p.name, n.name,
                               h.changedAt, h.changedBy.userId, h.reason
                        from TaskStatusHistoryEntity h
                        join h.newStatus n
                        left join h.previousStatus p
                        where h.task.taskId in :ids
                        order by h.changedAt, h.taskStatusHistoryId
                        """, Object[].class)
                .setParameter("ids", taskIds)
                .getResultList();
        Map<Integer, List<StatusChange>> byTask = new HashMap<>();
        for (Object[] row : rows) {
            byTask.computeIfAbsent((Integer) row[0], ignored -> new ArrayList<>())
                    .add(new StatusChange(
                            (Integer) row[1],
                            row[2] == null ? null : WorkStatus.valueOf((String) row[2]),
                            WorkStatus.valueOf((String) row[3]),
                            (java.time.Instant) row[4],
                            (Integer) row[5],
                            (String) row[6]));
        }
        return byTask;
    }

    private Map<Integer, Integer> lastRevisions(Collection<Integer> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = entityManager.createQuery("""
                        select h.task.taskId, max(h.id.revisionNumber)
                        from TaskPlanHistoryEntity h
                        where h.task.taskId in :ids
                        group by h.task.taskId
                        """, Object[].class)
                .setParameter("ids", taskIds)
                .getResultList();
        Map<Integer, Integer> byTask = new HashMap<>();
        for (Object[] row : rows) {
            byTask.put((Integer) row[0], ((Number) row[1]).intValue());
        }
        return byTask;
    }

    private static Task toDomain(TaskEntity entity, List<StatusChange> history, int lastRevisionNumber) {
        ActivityEntity activity = entity.getActivity();
        BigDecimal budget = entity.getAllocatedBudgetBob();
        return Task.restore(
                entity.getTaskId(),
                Activity.reference(activity.getActivityId(), activity.getName()),
                entity.getTitle(),
                entity.getDescription(),
                WorkStatus.valueOf(entity.getStatus().getName()),
                TaskPriority.valueOf(entity.getPriority().getName()),
                budget,
                entity.getEstimatedDurationMinutes(),
                entity.getActualDurationMinutes(),
                entity.getCompletedAt(),
                entity.getCompletedBy() == null ? null : entity.getCompletedBy().getUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                history,
                lastRevisionNumber);
    }
}
