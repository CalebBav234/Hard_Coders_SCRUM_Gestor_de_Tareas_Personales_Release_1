package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ActivityEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.StageEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskPriorityEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.WorkStatusEntity;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.TaskPriority;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US02-ADP-01: adaptador JPA de tareas con EntityManager y CatalogLookup simulados. */
class TaskPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-15T13:00:00Z");

    private EntityManager entityManager;
    private CatalogLookup catalogs;
    private TaskPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        catalogs = mock(CatalogLookup.class);
        adapter = new TaskPersistenceAdapter(catalogs);
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
        when(catalogs.workStatus(any())).thenAnswer(invocation -> status(invocation.getArgument(0, WorkStatus.class).name()));
        when(catalogs.priority(any())).thenAnswer(invocation -> priority(invocation.getArgument(0, TaskPriority.class).name()));
        whenEmptyHistory();
    }

    @Test
    @DisplayName("US02-ADP-01: findOpenForUpdate devuelve la tarea del proyecto y vacío si falta, cancela u otro proyecto")
    void findOpenForUpdateFiltersByExistenceProjectAndStatus() {
        TaskEntity entity = task(31, 7, "PENDIENTE");
        when(entityManager.find(eq(TaskEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(entity, null, cancelledTask(31, 7), task(31, 8, "PENDIENTE"));

        assertThat(adapter.findOpenForUpdate(7, 31))
                .get()
                .satisfies(found -> {
                    assertThat(found.id()).isEqualTo(31);
                    assertThat(found.title()).isEqualTo("Informe");
                    assertThat(found.status()).isEqualTo(WorkStatus.PENDIENTE);
                    assertThat(found.activity().name()).isEqualTo("Analisis");
                });
        assertThat(adapter.findOpenForUpdate(7, 999)).isEmpty();
        assertThat(adapter.findOpenForUpdate(7, 31)).isEmpty();
        assertThat(adapter.findOpenForUpdate(7, 31)).isEmpty();
    }

    @Test
    @DisplayName("US02-ADP-02: projectIdOf resuelve el proyecto a través de actividad-etapa-proyecto")
    void projectIdOfWalksHierarchy() {
        when(entityManager.find(TaskEntity.class, 31)).thenReturn(task(31, 7, "PENDIENTE"));
        assertThat(adapter.projectIdOf(31)).contains(7);
        when(entityManager.find(TaskEntity.class, 999)).thenReturn(null);
        assertThat(adapter.projectIdOf(999)).isEmpty();
    }

    @Test
    @DisplayName("US02-ADP-03: findVisible por id mapea el identificador devuelto por la consulta nativa")
    void findVisibleByIdMapsNativeIds() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(List.of(31));
        stubEntityQuery(List.of(task(31, 7, "PENDIENTE")));

        assertThat(adapter.findVisible(7, 31)).get().extracting(Task::id).isEqualTo(31);
    }

    @Test
    @DisplayName("US02-ADP-04: findVisible/findArchived sin resultados devuelven lista vacía sin consultar entidades")
    void visibleAndArchivedWithEmptyIdsSkipEntityLoad() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(List.of());

        assertThat(adapter.findVisible(7, "patron")).isEmpty();
        assertThat(adapter.findArchived(7, "patron")).isEmpty();
        verify(entityManager, never()).createQuery(anyString(), eq(TaskEntity.class));
    }

    @Test
    @DisplayName("US02-ADP-05: findVisible por patrón carga historial y última revisión de cada tarea")
    void findVisibleByPatternLoadsHistoryAndRevisions() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(List.of(31, 99));
        stubEntityQuery(List.of(task(31, 7, "PENDIENTE")));
        stubHistoryAndRevisions();

        List<Task> result = adapter.findVisible(7, "analisis");

        assertThat(result).extracting(Task::id).containsExactly(31);
        assertThat(result.getFirst().history()).hasSize(2);
        assertThat(result.getFirst().lastRevisionNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("US02-ADP-06: findArchived filtra por estados archivados usando la consulta nativa")
    void findArchivedUsesNativeQueryForArchivedStatuses() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(List.of(31));
        stubEntityQuery(List.of(task(31, 7, "COMPLETADA")));
        stubHistoryAndRevisions();

        assertThat(adapter.findArchived(7, null)).extracting(Task::id).containsExactly(31);
    }

    @Test
    @DisplayName("US02-ADP-07: findAssignedTo resuelve los ids asignados y los carga; el patrón es opcional")
    void findAssignedToLoadsAssignedIds() {
        stubAssignedIds();
        stubEntityQuery(List.of(task(31, 7, "PENDIENTE")));

        assertThat(adapter.findAssignedTo(2, null)).extracting(Task::id).containsExactly(31);
        assertThat(adapter.findAssignedTo(2, "patron")).extracting(Task::id).containsExactly(31);
    }

    @Test
    @DisplayName("US02-ADP-08: findAssignedForUpdate bloquea solo si está asignada; si no, devuelve vacío")
    void findAssignedForUpdateChecksAssignmentFirst() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(List.of(31));
        when(entityManager.find(TaskEntity.class, 31)).thenReturn(task(31, 7, "PENDIENTE"));
        when(entityManager.find(eq(TaskEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(task(31, 7, "PENDIENTE"));

        assertThat(adapter.findAssignedForUpdate(2, 31)).get().extracting(Task::id).isEqualTo(31);

        when(nativeQuery.getResultList()).thenReturn(List.of());
        assertThat(adapter.findAssignedForUpdate(2, 31)).isEmpty();
    }

    @Test
    @DisplayName("US02-ADP-09: save inserta una tarea nueva con sus revisiones y cambios de estado pendientes")
    void savePersistsNewTaskWithPendingHistory() {
        Activity activity = Activity.reference(21, "Analisis");
        Task task = Task.create("Informe", TaskPriority.MEDIA, activity, 1, NOW, new BigDecimal("150.00"), 90);
        task.start(1, NOW);

        when(entityManager.getReference(eq(ActivityEntity.class), anyInt()))
                .thenAnswer(invocation -> referenceActivity(invocation.getArgument(1)));
        when(entityManager.getReference(eq(AppUserEntity.class), anyInt()))
                .thenAnswer(invocation -> referenceUser(invocation.getArgument(1)));
        when(entityManager.find(TaskEntity.class, 31)).thenReturn(task(31, 7, "PENDIENTE"));
        doAnswer(invocation -> {
            invocation.getArgument(0, TaskEntity.class).setTaskId(31);
            return null;
        }).when(entityManager).persist(any(TaskEntity.class));

        Task saved = adapter.save(task);

        assertThat(saved.id()).isEqualTo(31);
        assertThat(saved.title()).isEqualTo("Informe");
        assertThat(saved.status()).isEqualTo(WorkStatus.EN_PROGRESO);
        // Entidad + revisión de plan + cambio de creación + cambio de inicio, todos persistidos.
        verify(entityManager, times(4)).persist(org.mockito.ArgumentMatchers.<Object>any());
        verify(entityManager).refresh(org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    @DisplayName("US02-ADP-10: save actualiza una tarea existente sin volver a insertarla")
    void saveUpdatesExistingTask() {
        TaskEntity existing = task(31, 7, "EN_PROGRESO");
        existing.setCompletedBy(referenceUser(1));
        when(entityManager.find(TaskEntity.class, 31)).thenReturn(existing);
        when(entityManager.getReference(eq(ActivityEntity.class), anyInt()))
                .thenAnswer(invocation -> referenceActivity(invocation.getArgument(1)));
        when(entityManager.getReference(eq(AppUserEntity.class), anyInt()))
                .thenAnswer(invocation -> referenceUser(invocation.getArgument(1)));

        Task updated = adapter.save(Task.restore(31, Activity.reference(21, "Analisis"), "Renombrada", "Desc",
                WorkStatus.EN_PROGRESO, TaskPriority.MEDIA, new BigDecimal("150.00"), 90, 45, LATER, 1,
                NOW, LATER, List.of(), 1));

        assertThat(updated.id()).isEqualTo(31);
        assertThat(updated.completedByUserId()).isEqualTo(1);
        verify(entityManager, never()).persist(org.mockito.ArgumentMatchers.<Object>any());
        verify(entityManager).refresh(existing);
    }

    private void stubEntityQuery(List<TaskEntity> entities) {
        @SuppressWarnings("unchecked")
        TypedQuery<TaskEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(TaskEntity.class))).thenReturn(query);
        when(query.setParameter(eq("ids"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(entities);
    }

    private void stubHistoryAndRevisions() {
        @SuppressWarnings("unchecked")
        TypedQuery<Object[]> rows = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(rows);
        when(rows.setParameter(anyString(), any())).thenReturn(rows);
        List<Object[]> history = new java.util.ArrayList<>();
        history.add(new Object[]{31, 500, null, "PENDIENTE", NOW, 1, "Tarea creada"});
        history.add(new Object[]{31, 501, "PENDIENTE", "EN_PROGRESO", LATER, 1, "iniciada"});
        List<Object[]> revisions = new java.util.ArrayList<>();
        revisions.add(new Object[]{31, 3});
        when(rows.getResultList()).thenReturn(history, revisions);
    }

    private void stubAssignedIds() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(List.of(31));
    }

    private void whenEmptyHistory() {
        @SuppressWarnings("unchecked")
        TypedQuery<Object[]> rows = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(rows);
        when(rows.setParameter(anyString(), any())).thenReturn(rows);
        when(rows.getResultList()).thenReturn(List.of());
    }

    private TaskEntity task(Integer id, Integer projectId, String statusName) {
        ProjectEntity project = new ProjectEntity();
        project.setProjectId(projectId);
        StageEntity stage = new StageEntity();
        stage.setStageId(11);
        stage.setProject(project);
        ActivityEntity activity = new ActivityEntity();
        activity.setActivityId(21);
        activity.setName("Analisis");
        activity.setStage(stage);
        TaskEntity entity = new TaskEntity();
        entity.setTaskId(id);
        entity.setActivity(activity);
        entity.setTitle("Informe");
        entity.setDescription("Detalle");
        entity.setStatus(status(statusName));
        entity.setPriority(priority("MEDIA"));
        entity.setAllocatedBudgetBob(new BigDecimal("150.00"));
        entity.setEstimatedDurationMinutes(90);
        entity.setActualDurationMinutes(45);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(LATER);
        return entity;
    }

    private TaskEntity cancelledTask(Integer id, Integer projectId) {
        return task(id, projectId, "CANCELADA");
    }

    private ActivityEntity referenceActivity(Integer id) {
        ActivityEntity activity = new ActivityEntity();
        activity.setActivityId(id);
        activity.setName("Analisis");
        return activity;
    }

    private AppUserEntity referenceUser(Integer id) {
        AppUserEntity user = new AppUserEntity();
        user.setUserId(id);
        return user;
    }

    private static WorkStatusEntity status(String name) {
        WorkStatusEntity entity = new WorkStatusEntity();
        entity.setName(name);
        return entity;
    }

    private static TaskPriorityEntity priority(String name) {
        TaskPriorityEntity entity = new TaskPriorityEntity();
        entity.setName(name);
        return entity;
    }
}
