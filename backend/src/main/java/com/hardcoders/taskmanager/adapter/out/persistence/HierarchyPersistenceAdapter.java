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

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida JPA de la jerarquía Proyecto → Etapa → Actividad.
 * Sustituye al antiguo "espacio de trabajo": ya no se selecciona el primer proyecto.
 */
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
        return entityManager.createQuery("""
                        select s from StageEntity s join fetch s.status
                        where s.project.projectId = :projectId
                        order by s.createdAt, s.stageId
                        """, StageEntity.class)
                .setParameter("projectId", projectId)
                .getResultList().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Stage save(Stage stage) {
        StageEntity entity = stage.isNew() ? new StageEntity() : entityManager.find(StageEntity.class, stage.id());
        entity.setProject(entityManager.getReference(
                com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity.class, stage.projectId()));
        entity.setName(stage.name());
        entity.setDescription(stage.description());
        entity.setStatus(catalogs.workStatus(stage.status()));
        if (stage.isNew()) {
            entity.setCreatedAt(stage.createdAt());
            entity.setUpdatedAt(stage.updatedAt());
            entityManager.persist(entity);
        }
        entityManager.flush();
        // updated_at lo fija el trigger: se relee para devolver el token de versión real.
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
        return entityManager.createQuery("""
                        select a from ActivityEntity a join fetch a.status
                        where a.stage.stageId = :stageId
                        order by lower(a.name), a.activityId
                        """, ActivityEntity.class)
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
        // updated_at lo fija el trigger: se relee para devolver el token de versión real.
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
}
