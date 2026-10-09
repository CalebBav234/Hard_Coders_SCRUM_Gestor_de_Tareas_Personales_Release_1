package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ActivityEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectPlanHistoryEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.StageEntity;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.model.Workspace;
import com.hardcoders.taskmanager.domain.port.out.WorkspaceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Espacio de trabajo = primer proyecto y su primera etapa. Con datos migrados es el
 * "Proyecto legado"; en una base nueva se crea "Mis tareas" / "General".
 */
@Component
class WorkspacePersistenceAdapter implements WorkspaceRepository {

    static final String DEFAULT_PROJECT = "Mis tareas";
    static final String DEFAULT_STAGE = "General";

    @PersistenceContext
    private EntityManager entityManager;

    private final CatalogLookup catalogs;

    WorkspacePersistenceAdapter(CatalogLookup catalogs) {
        this.catalogs = catalogs;
    }

    @Override
    public Optional<Workspace> findWorkspace() {
        return firstProject().flatMap(project -> firstStage(project.getProjectId())
                .map(stage -> new Workspace(project.getProjectId(), stage.getStageId())));
    }

    @Override
    public Workspace currentWorkspace(Integer actorId) {
        ProjectEntity project = firstProject().orElseGet(() -> createProject(actorId));
        StageEntity stage = firstStage(project.getProjectId()).orElseGet(() -> createStage(project));
        return new Workspace(project.getProjectId(), stage.getStageId());
    }

    @Override
    public Activity findOrCreateActivity(Integer stageId, String name) {
        return entityManager.createQuery("""
                        select a from ActivityEntity a
                        where a.stage.stageId = :stageId and lower(a.name) = lower(:name)
                        order by a.activityId
                        """, ActivityEntity.class)
                .setParameter("stageId", stageId)
                .setParameter("name", name)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(WorkspacePersistenceAdapter::toDomain)
                .orElseGet(() -> toDomain(createActivity(stageId, name)));
    }

    @Override
    public List<Activity> activities(Integer stageId) {
        return entityManager.createQuery("""
                        select a from ActivityEntity a
                        where a.stage.stageId = :stageId
                        order by lower(a.name), a.activityId
                        """, ActivityEntity.class)
                .setParameter("stageId", stageId)
                .getResultStream()
                .map(WorkspacePersistenceAdapter::toDomain)
                .toList();
    }

    static Activity toDomain(ActivityEntity entity) {
        return new Activity(entity.getActivityId(), entity.getName());
    }

    private Optional<ProjectEntity> firstProject() {
        return entityManager.createQuery(
                        "select p from ProjectEntity p order by p.projectId", ProjectEntity.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    private Optional<StageEntity> firstStage(Integer projectId) {
        return entityManager.createQuery(
                        "select s from StageEntity s where s.project.projectId = :projectId order by s.stageId",
                        StageEntity.class)
                .setParameter("projectId", projectId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    private ProjectEntity createProject(Integer actorId) {
        AppUserEntity actor = entityManager.getReference(AppUserEntity.class, actorId);
        ProjectEntity project = new ProjectEntity();
        project.setName(DEFAULT_PROJECT);
        project.setDescription("Espacio de trabajo del gestor de tareas personales.");
        project.setManager(actor);
        project.setCreatedBy(actor);
        project.setStatus(catalogs.projectStatus("EN_EJECUCION"));
        entityManager.persist(project);
        entityManager.flush();

        // V004 exige la revisión 1 del plan en la misma transacción (trigger diferido).
        ProjectPlanHistoryEntity plan = new ProjectPlanHistoryEntity();
        plan.setProject(project);
        plan.getId().setRevisionNumber(1);
        plan.setBudgetBob(BigDecimal.ZERO.setScale(2));
        plan.setEstimatedDurationMinutes(0);
        plan.setChangedAt(Instant.now());
        plan.setChangedBy(actor);
        plan.setReason("Plan inicial del espacio de trabajo");
        entityManager.persist(plan);
        return project;
    }

    private StageEntity createStage(ProjectEntity project) {
        StageEntity stage = new StageEntity();
        stage.setProject(project);
        stage.setName(DEFAULT_STAGE);
        stage.setStatus(catalogs.workStatus(WorkStatus.EN_PROGRESO));
        entityManager.persist(stage);
        entityManager.flush();
        return stage;
    }

    private ActivityEntity createActivity(Integer stageId, String name) {
        ActivityEntity activity = new ActivityEntity();
        activity.setStage(entityManager.getReference(StageEntity.class, stageId));
        activity.setName(name);
        activity.setStatus(catalogs.workStatus(WorkStatus.EN_PROGRESO));
        entityManager.persist(activity);
        entityManager.flush();
        return activity;
    }
}
