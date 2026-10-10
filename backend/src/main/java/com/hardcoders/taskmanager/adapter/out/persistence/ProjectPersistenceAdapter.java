package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectPlanHistoryEntity;
import com.hardcoders.taskmanager.domain.model.PlanRevision;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Adaptador de salida JPA para {@link Project}: tablas {@code project} y {@code project_plan_history}. */
@Component
class ProjectPersistenceAdapter implements ProjectRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final CatalogLookup catalogs;

    ProjectPersistenceAdapter(CatalogLookup catalogs) {
        this.catalogs = catalogs;
    }

    @Override
    public Optional<Project> findById(Integer projectId) {
        return Optional.ofNullable(entityManager.find(ProjectEntity.class, projectId)).map(this::toDomain);
    }

    @Override
    public Optional<Project> findForUpdate(Integer projectId) {
        // SELECT ... FOR UPDATE solo sobre la fila del proyecto.
        return Optional.ofNullable(entityManager.find(ProjectEntity.class, projectId, LockModeType.PESSIMISTIC_WRITE))
                .map(this::toDomain);
    }

    @Override
    public List<Project> findAll() {
        return entityManager.createQuery("""
                        select p from ProjectEntity p join fetch p.status
                        order by p.createdAt desc, p.projectId desc
                        """, ProjectEntity.class)
                .getResultStream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Project> findManagedBy(Integer managerUserId) {
        return entityManager.createQuery("""
                        select p from ProjectEntity p join fetch p.status
                        where p.manager.userId = :managerId
                        order by p.createdAt desc, p.projectId desc
                        """, ProjectEntity.class)
                .setParameter("managerId", managerUserId)
                .getResultStream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public BigDecimal allocatedBudget(Integer projectId) {
        Object sum = entityManager.createNativeQuery("""
                        SELECT COALESCE(SUM(t.allocated_budget_bob), 0)
                        FROM task_manager.task t
                        JOIN task_manager.activity a ON a.activity_id = t.activity_id
                        JOIN task_manager.stage s ON s.stage_id = a.stage_id
                        JOIN task_manager.work_status ws ON ws.work_status_id = t.work_status_id
                        WHERE s.project_id = :projectId AND ws.name <> 'CANCELADA'
                        """)
                .setParameter("projectId", projectId)
                .getSingleResult();
        return sum instanceof BigDecimal value ? value.setScale(2) : new BigDecimal(sum.toString()).setScale(2);
    }

    @Override
    public List<PlanRevision> planHistory(Integer projectId) {
        return entityManager.createQuery("""
                        select h from ProjectPlanHistoryEntity h
                        where h.project.projectId = :projectId
                        order by h.id.revisionNumber desc
                        """, ProjectPlanHistoryEntity.class)
                .setParameter("projectId", projectId)
                .getResultStream()
                .map(h -> new PlanRevision(h.getId().getRevisionNumber(), h.getBudgetBob(),
                        h.getEstimatedDurationMinutes(), h.getChangedAt(), h.getChangedBy().getUserId(),
                        h.getReason()))
                .toList();
    }

    @Override
    public Project save(Project project) {
        ProjectEntity entity = project.isNew()
                ? new ProjectEntity()
                : entityManager.find(ProjectEntity.class, project.id());
        entity.setName(project.name());
        entity.setDescription(project.description());
        entity.setManager(entityManager.getReference(AppUserEntity.class, project.managerUserId()));
        entity.setStatus(catalogs.projectStatus(project.status().name()));
        entity.setBudgetBob(project.budgetBob());
        entity.setEstimatedDurationMinutes(project.estimatedDurationMinutes());
        if (project.isNew()) {
            entity.setCreatedBy(entityManager.getReference(AppUserEntity.class, project.createdByUserId()));
            entity.setCreatedAt(project.createdAt());
            entity.setUpdatedAt(project.updatedAt());
            entityManager.persist(entity);
            entityManager.flush();
        }
        // PROJECT ||--|{ PROJECT_PLAN_HISTORY: el trigger diferido exige la revisión en esta transacción.
        for (PlanRevision revision : project.pendingRevisions()) {
            ProjectPlanHistoryEntity row = new ProjectPlanHistoryEntity();
            row.setProject(entity);
            row.getId().setRevisionNumber(revision.revisionNumber());
            row.setBudgetBob(revision.budgetBob());
            row.setEstimatedDurationMinutes(revision.estimatedDurationMinutes());
            row.setChangedAt(revision.changedAt());
            row.setChangedBy(entityManager.getReference(AppUserEntity.class, revision.changedByUserId()));
            row.setReason(revision.reason());
            entityManager.persist(row);
        }
        entityManager.flush();
        // updated_at lo fija el trigger: se relee para devolver el token de versión real.
        entityManager.refresh(entity);
        return toDomain(entity);
    }

    private Project toDomain(ProjectEntity entity) {
        Number last = (Number) entityManager.createNativeQuery("""
                        SELECT COALESCE(MAX(revision_number), 0)
                        FROM task_manager.project_plan_history WHERE project_id = :projectId
                        """)
                .setParameter("projectId", entity.getProjectId())
                .getSingleResult();
        Instant updatedAt = entity.getUpdatedAt();
        return Project.restore(
                entity.getProjectId(),
                entity.getName(),
                entity.getDescription(),
                entity.getManager().getUserId(),
                ProjectStatus.valueOf(entity.getStatus().getName()),
                entity.getBudgetBob(),
                entity.getEstimatedDurationMinutes(),
                entity.getCreatedBy().getUserId(),
                entity.getCreatedAt(),
                updatedAt,
                last.intValue());
    }
}
