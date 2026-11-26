package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ActivityEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.StageEntity;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Stage;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.port.out.HierarchyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
class HierarchyPersistenceAdapter implements HierarchyRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final CatalogLookup catalogs;

    HierarchyPersistenceAdapter(CatalogLookup catalogs) {
        this.catalogs = catalogs;
    }

    @Override
    public Optional<Stage> findStage(Integer stageId) {
        return Optional.ofNullable(entityManager.find(StageEntity.class, stageId)).map(this::toDomain);
    }

    @Override
    public Optional<Stage> findStageForUpdate(Integer stageId) {
        return Optional.ofNullable(entityManager.find(StageEntity.class, stageId, LockModeType.PESSIMISTIC_WRITE))
                .map(this::toDomain);
    }

    @Override
    public List<Stage> stagesOf(Integer projectId) {
        return entityManager.createQuery("select s from StageEntity s join fetch s.status where s.project.projectId = :projectId order by s.sortOrder, s.createdAt, s.stageId", StageEntity.class)
                .setParameter("projectId", projectId)
                .getResultList().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Stage save(Stage stage) {
        StageEntity entity = stage.isNew() ? new StageEntity() : entityManager.find(StageEntity.class, stage.id());
        entity.setProject(entityManager.getReference(com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity.class, stage.projectId()));
        entity.setName(stage.name());
        entity.setDescription(stage.description());
        entity.setStatus(catalogs.workStatus(stage.status()));
        if (stage.isNew()) {
            entity.setCreatedAt(stage.createdAt());
            entity.setUpdatedAt(stage.updatedAt());
            entityManager.persist(entity);
        }
        entityManager.flush();
        entityManager.refresh(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Activity> findActivity(Integer activityId) {
        return Optional.ofNullable(entityManager.find(ActivityEntity.class, activityId)).map(this::toDomain);
    }

    @Override
    public Optional<Activity> findActivityForUpdate(Integer activityId) {
        return Optional.ofNullable(entityManager.find(ActivityEntity.class, activityId, LockModeType.PESSIMISTIC_WRITE))
                .map(this::toDomain);
    }

    @Override
    public List<Activity> activitiesOf(Integer stageId) {
        return entityManager.createQuery("select a from ActivityEntity a join fetch a.status where a.stage.stageId = :stageId order by a.sortOrder, lower(a.name), a.activityId", ActivityEntity.class)
                .setParameter("stageId", stageId)
                .getResultList().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Activity save(Activity activity) {
        ActivityEntity entity = activity.isNew()
                ? new ActivityEntity()
                : entityManager.find(ActivityEntity.class, activity.id());
        entity.setStage(entityManager.getReference(StageEntity.class, activity.stageId()));
        entity.setName(activity.name());
        entity.setDescription(activity.description());
        entity.setStatus(catalogs.workStatus(activity.status()));
        if (activity.isNew()) {
            entity.setCreatedAt(activity.createdAt());
            entity.setUpdatedAt(activity.updatedAt());
            entityManager.persist(entity);
        }
        entityManager.flush();
        entityManager.refresh(entity);
        return toDomain(entity);
    }

    private Stage toDomain(StageEntity entity) {
        return Stage.restore(entity.getStageId(), entity.getProject().getProjectId(), entity.getName(),
                entity.getDescription(), WorkStatus.valueOf(entity.getStatus().getName()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Activity toDomain(ActivityEntity entity) {
        return Activity.restore(entity.getActivityId(), entity.getStage().getStageId(), entity.getName(),
                entity.getDescription(), WorkStatus.valueOf(entity.getStatus().getName()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Override
    public List<Stage> searchStages(String likePattern) {
        if (likePattern == null) {
            return List.of();
        }
        String normalize = "translate(lower(coalesce(%s, '')), 'áéíóúüñ', 'aeiouun')";
        return entityManager.createQuery("select s from StageEntity s join fetch s.status where " + normalize.formatted("s.name") + " LIKE :pattern ESCAPE '" + "\\" + "' or " + normalize.formatted("s.description") + " LIKE :pattern ESCAPE '" + "\\" + "' order by s.stageId desc", StageEntity.class)
                .setParameter("pattern", likePattern)
                .getResultList().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Activity> searchActivities(String likePattern) {
        if (likePattern == null) {
            return List.of();
        }
        String normalize = "translate(lower(coalesce(%s, '')), 'áéíóúüñ', 'aeiouun')";
        return entityManager.createQuery("select a from ActivityEntity a join fetch a.status where " + normalize.formatted("a.name") + " LIKE :pattern ESCAPE '" + "\\" + "' or " + normalize.formatted("a.description") + " LIKE :pattern ESCAPE '" + "\\" + "' order by a.activityId desc", ActivityEntity.class)
                .setParameter("pattern", likePattern)
                .getResultList().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Stage> searchStagesVisibleFor(Integer userId, String likePattern) {
        if (likePattern == null) {
            return List.of();
        }
        String normalize = "translate(lower(coalesce(%s, '')), 'áéíóúüñ', 'aeiouun')";
        return entityManager.createQuery(
                "select distinct s from StageEntity s join fetch s.status join com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity p on p.projectId = s.project.projectId where p.manager.userId = :userId and (" + normalize.formatted("s.name") + " LIKE :pattern ESCAPE '" + "\\" + "' or " + normalize.formatted("s.description") + " LIKE :pattern ESCAPE '" + "\\" + "') order by s.stageId desc", StageEntity.class)
                .setParameter("userId", userId)
                .setParameter("pattern", likePattern)
                .getResultList().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Activity> searchActivitiesVisibleFor(Integer userId, String likePattern) {
        if (likePattern == null) {
            return List.of();
        }
        String normalize = "translate(lower(coalesce(%s, '')), 'áéíóúüñ', 'aeiouun')";
        return entityManager.createQuery("select distinct a from ActivityEntity a join fetch a.status join StageEntity s on s.stageId = a.stage.stageId join com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity p on p.projectId = s.project.projectId where p.manager.userId = :userId and (" + normalize.formatted("a.name") + " LIKE :pattern ESCAPE '" + "\\" + "' or " + normalize.formatted("a.description") + " LIKE :pattern ESCAPE '" + "\\" + "') order by a.activityId desc", ActivityEntity.class)
                .setParameter("userId", userId)
                .setParameter("pattern", likePattern)
                .getResultList().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void reorderStages(Integer projectId, List<Integer> stageIds) {
        if (stageIds == null || stageIds.isEmpty()) {
            return;
        }
        int idx = 0;
        for (Integer id : stageIds) {
            entityManager.createQuery("update StageEntity s set s.sortOrder = :order where s.stageId = :id and s.project.projectId = :projectId")
                    .setParameter("order", idx++)
                    .setParameter("id", id)
                    .setParameter("projectId", projectId)
                    .executeUpdate();
        }
    }

    @Override
    @Transactional
    public void reorderActivities(Integer stageId, List<Integer> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return;
        }
        int idx = 0;
        for (Integer id : activityIds) {
            entityManager.createQuery("update ActivityEntity a set a.sortOrder = :order where a.activityId = :id and a.stage.stageId = :stageId")
                    .setParameter("order", idx++)
                    .setParameter("id", id)
                    .setParameter("stageId", stageId)
                    .executeUpdate();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Integer> countActivitiesByStage(List<Integer> stageIds) {
        if (stageIds == null || stageIds.isEmpty()) {
            return Map.of();
        }
        // Consulta nativa para evitar caché de entidades de Hibernate y leer el conteo real de la BD.
        jakarta.persistence.Query q = entityManager.createNativeQuery(
                "select stage_id, count(*) from task_manager.activity where stage_id in :ids group by stage_id");
        q.setParameter("ids", stageIds);
        List<Object[]> rows = q.getResultList();
        Map<Integer, Integer> counts = new HashMap<>();
        for (Integer id : stageIds) {
            counts.put(id, 0);
        }
        for (Object[] row : rows) {
            counts.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Integer> countTasksByActivity(List<Integer> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }
        jakarta.persistence.Query q = entityManager.createNativeQuery(
                "select activity_id, count(*) from task_manager.task where activity_id in :ids group by activity_id");
        q.setParameter("ids", activityIds);
        List<Object[]> rows = q.getResultList();
        Map<Integer, Integer> counts = new HashMap<>();
        for (Integer id : activityIds) {
            counts.put(id, 0);
        }
        for (Object[] row : rows) {
            counts.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
        }
        return counts;
    }
}
