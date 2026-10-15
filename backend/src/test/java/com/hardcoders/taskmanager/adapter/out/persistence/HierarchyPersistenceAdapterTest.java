package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ActivityEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.StageEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.WorkStatusEntity;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Stage;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HierarchyPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private EntityManager entityManager;
    private CatalogLookup catalogs;
    private HierarchyPersistenceAdapter adapter;
    private ProjectEntity project;
    private StageEntity stageEntity;
    private ActivityEntity activityEntity;
    private WorkStatusEntity pending;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        catalogs = mock(CatalogLookup.class);
        adapter = new HierarchyPersistenceAdapter(catalogs);
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);

        project = new ProjectEntity();
        project.setProjectId(7);
        pending = new WorkStatusEntity();
        pending.setName("PENDIENTE");
        stageEntity = new StageEntity();
        stageEntity.setStageId(11);
        stageEntity.setProject(project);
        stageEntity.setName("Etapa");
        stageEntity.setStatus(pending);
        stageEntity.setCreatedAt(NOW);
        stageEntity.setUpdatedAt(NOW);
        activityEntity = new ActivityEntity();
        activityEntity.setActivityId(21);
        activityEntity.setStage(stageEntity);
        activityEntity.setName("Actividad");
        activityEntity.setStatus(pending);
        activityEntity.setCreatedAt(NOW);
        activityEntity.setUpdatedAt(NOW);
        when(catalogs.workStatus(WorkStatus.PENDIENTE)).thenReturn(pending);
    }

    @Test
    void findsStagesAndActivitiesIncludingLockedLookupsAndProjectLists() {
        when(entityManager.find(StageEntity.class, 11)).thenReturn(stageEntity);
        when(entityManager.find(StageEntity.class, 404)).thenReturn(null);
        when(entityManager.find(StageEntity.class, 11, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(stageEntity);
        when(entityManager.find(ActivityEntity.class, 21)).thenReturn(activityEntity);
        when(entityManager.find(ActivityEntity.class, 404)).thenReturn(null);
        when(entityManager.find(ActivityEntity.class, 21, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(activityEntity);

        assertThat(adapter.findStage(11)).get().extracting(Stage::id).isEqualTo(11);
        assertThat(adapter.findStage(404)).isEmpty();
        assertThat(adapter.findStageForUpdate(11)).get().extracting(Stage::projectId).isEqualTo(7);
        assertThat(adapter.findActivity(21)).get().extracting(Activity::stageId).isEqualTo(11);
        assertThat(adapter.findActivity(404)).isEmpty();
        assertThat(adapter.findActivityForUpdate(21)).get().extracting(Activity::id).isEqualTo(21);

        @SuppressWarnings("unchecked")
        TypedQuery<StageEntity> stagesQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(StageEntity.class))).thenReturn(stagesQuery);
        when(stagesQuery.setParameter(eq("projectId"), any())).thenReturn(stagesQuery);
        when(stagesQuery.getResultList()).thenReturn(List.of(stageEntity));
        assertThat(adapter.stagesOf(7)).extracting(Stage::name).containsExactly("Etapa");

        @SuppressWarnings("unchecked")
        TypedQuery<ActivityEntity> activitiesQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(ActivityEntity.class))).thenReturn(activitiesQuery);
        when(activitiesQuery.setParameter(eq("stageId"), any())).thenReturn(activitiesQuery);
        when(activitiesQuery.getResultList()).thenReturn(List.of(activityEntity));
        assertThat(adapter.activitiesOf(11)).extracting(Activity::name).containsExactly("Actividad");
    }

    @Test
    void savesNewAndExistingStagesAndActivities() {
        when(entityManager.getReference(ProjectEntity.class, 7)).thenReturn(project);
        when(entityManager.getReference(StageEntity.class, 11)).thenReturn(stageEntity);
        when(entityManager.find(StageEntity.class, 11)).thenReturn(stageEntity);
        when(entityManager.getReference(StageEntity.class, 11)).thenReturn(stageEntity);
        when(entityManager.find(ActivityEntity.class, 21)).thenReturn(activityEntity);
        doAnswer(invocation -> {
            StageEntity entity = invocation.getArgument(0);
            entity.setStageId(12);
            return null;
        }).when(entityManager).persist(any(StageEntity.class));
        doAnswer(invocation -> {
            ActivityEntity entity = invocation.getArgument(0);
            entity.setActivityId(22);
            return null;
        }).when(entityManager).persist(any(ActivityEntity.class));

        Stage createdStage = adapter.save(Stage.create(7, "Nueva", null, NOW));
        assertThat(createdStage.id()).isEqualTo(12);
        Stage editedStage = adapter.save(Stage.restore(11, 7, "Editada", null, WorkStatus.PENDIENTE, NOW, NOW));
        assertThat(editedStage.name()).isEqualTo("Editada");

        Activity createdActivity = adapter.save(Activity.create(11, "Nueva actividad", null, NOW));
        assertThat(createdActivity.id()).isEqualTo(22);
        Activity editedActivity = adapter.save(
                Activity.restore(21, 11, "Editada", null, WorkStatus.PENDIENTE, NOW, NOW));
        assertThat(editedActivity.name()).isEqualTo("Editada");

        verify(entityManager, org.mockito.Mockito.times(4)).flush();
        verify(entityManager).refresh(stageEntity);
        verify(entityManager).refresh(activityEntity);
    }
}
