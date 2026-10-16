package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectPlanHistoryEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectPlanHistoryId;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectStatusEntity;
import com.hardcoders.taskmanager.domain.model.PlanRevision;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US01-ADP-01: adaptador JPA de proyectos con EntityManager y CatalogLookup simulados. */
class ProjectPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-15T13:00:00Z");

    private EntityManager entityManager;
    private CatalogLookup catalogs;
    private ProjectPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        catalogs = mock(CatalogLookup.class);
        adapter = new ProjectPersistenceAdapter(catalogs);
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
        when(catalogs.projectStatus(any())).thenAnswer(
                invocation -> status(invocation.getArgument(0, String.class)));
        stubNativeQueries();
    }

    private void stubNativeQueries() {
        Query lastRevision = mock(Query.class);
        when(lastRevision.setParameter(anyString(), any())).thenReturn(lastRevision);
        when(lastRevision.getSingleResult()).thenReturn(2);

        Query budget = mock(Query.class);
        when(budget.setParameter(anyString(), any())).thenReturn(budget);
        when(budget.getSingleResult()).thenReturn(new BigDecimal("1500.5"), new BigInteger("3000"));

        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            return sql.contains("SUM(") ? budget : lastRevision;
        });
    }

    @Test
    @DisplayName("US01-ADP-01: findById y findForUpdate reconstruyen el proyecto con su última revisión")
    void findByIdAndForUpdateReconstructTheProject() {
        ProjectEntity entity = project(7, "PLANIFICADO");
        when(entityManager.find(ProjectEntity.class, 7)).thenReturn(entity);
        when(entityManager.find(eq(ProjectEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(entity, null);

        assertThat(adapter.findById(7)).get().satisfies(found -> {
            assertThat(found.id()).isEqualTo(7);
            assertThat(found.name()).isEqualTo("Edificio A");
            assertThat(found.status()).isEqualTo(ProjectStatus.PLANIFICADO);
            assertThat(found.managerUserId()).isEqualTo(2);
            assertThat(found.lastRevisionNumber()).isEqualTo(2);
        });
        assertThat(adapter.findForUpdate(7)).get().extracting(Project::id).isEqualTo(7);
        assertThat(adapter.findForUpdate(999)).isEmpty();
        when(entityManager.find(ProjectEntity.class, 999)).thenReturn(null);
        assertThat(adapter.findById(999)).isEmpty();
    }

    @Test
    @DisplayName("US01-ADP-02: findAll y findManagedBy traducen cada entidad mapeando el encargado")
    void findAllAndFindManagedByReconstructEveryEntity() {
        ProjectEntity entity = project(7, "EN_EJECUCION");
        @SuppressWarnings("unchecked")
        TypedQuery<ProjectEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(ProjectEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(entity));
        when(query.setParameter(anyString(), any())).thenReturn(query);

        assertThat(adapter.findAll()).extracting(Project::id).containsExactly(7);
        assertThat(adapter.findManagedBy(2)).extracting(Project::status).containsExactly(ProjectStatus.EN_EJECUCION);
    }

    @Test
    @DisplayName("US01-ADP-03: allocatedBudget normaliza BigDecimal y otros Number a escala 2")
    void allocatedBudgetScalesAnyNumberToTwo() {
        assertThat(adapter.allocatedBudget(7)).isEqualByComparingTo("1500.50");
        assertThat(adapter.allocatedBudget(7)).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("US01-ADP-04: planHistory ordena de la más reciente a la más antigua")
    void planHistoryMapsEveryRevision() {
        @SuppressWarnings("unchecked")
        TypedQuery<ProjectPlanHistoryEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(ProjectPlanHistoryEntity.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(history(2, "9000.00"), history(1, "0.00")));

        List<PlanRevision> revisions = adapter.planHistory(7);

        assertThat(revisions).extracting(PlanRevision::revisionNumber).containsExactly(2, 1);
        assertThat(revisions.getFirst().budgetBob()).isEqualByComparingTo("9000.00");
        assertThat(revisions.getFirst().changedByUserId()).isEqualTo(1);
    }

    @Test
    @DisplayName("US01-ADP-05: save inserta un proyecto nuevo con su revisión de plan pendiente")
    void savePersistsNewProjectWithRevision() {
        when(entityManager.getReference(eq(AppUserEntity.class), anyInt()))
                .thenAnswer(invocation -> referenceUser(invocation.getArgument(1)));
        doAnswer(invocation -> {
            invocation.getArgument(0, ProjectEntity.class).setProjectId(7);
            return null;
        }).when(entityManager).persist(any(ProjectEntity.class));

        Project saved = adapter.save(Project.create("Edificio A", "Obra", 2, new BigDecimal("9000.00"),
                600, "Plan inicial", 1, NOW));

        assertThat(saved.id()).isEqualTo(7);
        assertThat(saved.name()).isEqualTo("Edificio A");
        // Proyecto + revisión de plan inicial.
        verify(entityManager, times(2)).persist(org.mockito.ArgumentMatchers.<Object>any());
        verify(entityManager).refresh(org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    @DisplayName("US01-ADP-06: save actualiza un proyecto existente sin reinsertarlo")
    void saveUpdatesExistingProject() {
        ProjectEntity existing = project(7, "PLANIFICADO");
        when(entityManager.find(ProjectEntity.class, 7)).thenReturn(existing);
        when(entityManager.getReference(eq(AppUserEntity.class), anyInt()))
                .thenAnswer(invocation -> referenceUser(invocation.getArgument(1)));

        Project saved = adapter.save(Project.restore(7, "Edificio A", "Obra", 2, ProjectStatus.PLANIFICADO,
                new BigDecimal("9000.00"), 600, 1, NOW, LATER, 2));

        assertThat(saved.id()).isEqualTo(7);
        assertThat(saved.budgetBob()).isEqualByComparingTo("9000.00");
        verify(entityManager, org.mockito.Mockito.never()).persist(org.mockito.ArgumentMatchers.<Object>any());
        verify(entityManager).refresh(existing);
    }

    private ProjectPlanHistoryEntity history(int revision, String budget) {
        ProjectPlanHistoryEntity entity = new ProjectPlanHistoryEntity();
        entity.setId(new ProjectPlanHistoryId(7, revision));
        entity.setBudgetBob(new BigDecimal(budget));
        entity.setEstimatedDurationMinutes(600);
        entity.setChangedAt(LATER);
        entity.setChangedBy(referenceUser(1));
        entity.setReason("Replan");
        return entity;
    }

    private ProjectEntity project(Integer id, String statusName) {
        AppUserEntity manager = referenceUser(2);
        AppUserEntity chief = referenceUser(1);
        ProjectEntity entity = new ProjectEntity();
        entity.setProjectId(id);
        entity.setName("Edificio A");
        entity.setDescription("Obra");
        entity.setManager(manager);
        entity.setStatus(status(statusName));
        entity.setBudgetBob(new BigDecimal("9000.00"));
        entity.setEstimatedDurationMinutes(600);
        entity.setCreatedBy(chief);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(LATER);
        return entity;
    }

    private AppUserEntity referenceUser(Integer id) {
        AppUserEntity user = new AppUserEntity();
        user.setUserId(id);
        return user;
    }

    private static ProjectStatusEntity status(String name) {
        ProjectStatusEntity entity = new ProjectStatusEntity();
        entity.setName(name);
        return entity;
    }
}
