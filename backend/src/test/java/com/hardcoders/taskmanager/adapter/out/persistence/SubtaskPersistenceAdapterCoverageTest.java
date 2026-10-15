package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ActivityEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.StageEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.SubtaskEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.WorkStatusEntity;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubtaskPersistenceAdapterCoverageTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private EntityManager entityManager;
    private CatalogLookup catalogs;
    private SubtaskPersistenceAdapter adapter;
    private TaskEntity task;
    private SubtaskEntity entity;
    private WorkStatusEntity pending;
    private WorkStatusEntity cancelled;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        catalogs = mock(CatalogLookup.class);
        adapter = new SubtaskPersistenceAdapter(catalogs);
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);

        ProjectEntity project = new ProjectEntity();
        project.setProjectId(7);
        StageEntity stage = new StageEntity();
        stage.setStageId(11);
        stage.setProject(project);
        ActivityEntity activity = new ActivityEntity();
        activity.setActivityId(21);
        activity.setStage(stage);
        pending = new WorkStatusEntity();
        pending.setName("PENDIENTE");
        cancelled = new WorkStatusEntity();
        cancelled.setName("CANCELADA");
        task = new TaskEntity();
        task.setTaskId(31);
        task.setActivity(activity);
        task.setStatus(pending);
        entity = subtask(41, task, pending);
        when(catalogs.workStatus(WorkStatus.PENDIENTE)).thenReturn(pending);
        when(catalogs.workStatus(WorkStatus.COMPLETADA)).thenReturn(status("COMPLETADA"));
    }

    @Test
    void filtersLockedSubtaskLookupsByStatusProjectAndParentTask() {
        SubtaskEntity cancelledSubtask = subtask(41, task, cancelled);
        TaskEntity cancelledTask = new TaskEntity();
        cancelledTask.setTaskId(31);
        cancelledTask.setActivity(task.getActivity());
        cancelledTask.setStatus(cancelled);
        SubtaskEntity cancelledParent = subtask(41, cancelledTask, pending);
        ProjectEntity otherProject = new ProjectEntity();
        otherProject.setProjectId(8);
        StageEntity otherStage = new StageEntity();
        otherStage.setProject(otherProject);
        ActivityEntity otherActivity = new ActivityEntity();
        otherActivity.setStage(otherStage);
        TaskEntity otherTask = new TaskEntity();
        otherTask.setTaskId(31);
        otherTask.setActivity(otherActivity);
        otherTask.setStatus(pending);
        SubtaskEntity foreignProject = subtask(41, otherTask, pending);

        when(entityManager.find(eq(SubtaskEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(null, cancelledSubtask, cancelledParent, foreignProject, entity,
                        cancelledSubtask, cancelledParent, entity, entity);

        assertThat(adapter.findOpenForUpdate(7, 41)).isEmpty();
        assertThat(adapter.findOpenForUpdate(7, 41)).isEmpty();
        assertThat(adapter.findOpenForUpdate(7, 41)).isEmpty();
        assertThat(adapter.findOpenForUpdate(7, 41)).isEmpty();
        assertThat(adapter.findOpenForUpdate(7, 41)).get().extracting(Subtask::id).isEqualTo(41);

        assertThat(adapter.findForUpdate(31, 41)).isEmpty();
        assertThat(adapter.findForUpdate(31, 41)).isEmpty();
        assertThat(adapter.findForUpdate(99, 41)).isEmpty();
        assertThat(adapter.findForUpdate(31, 41)).get().extracting(Subtask::taskId).isEqualTo(31);
    }

    @Test
    void findsTaskIdsAndVisibleSubtasksIncludingEmptyInput() {
        when(entityManager.find(SubtaskEntity.class, 41)).thenReturn(entity, null);
        assertThat(adapter.taskIdOf(41)).contains(31);
        assertThat(adapter.taskIdOf(999)).isEmpty();
        assertThat(adapter.findVisibleByTaskIds(List.of())).isEmpty();

        @SuppressWarnings("unchecked")
        TypedQuery<SubtaskEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(SubtaskEntity.class))).thenReturn(query);
        when(query.setParameter(eq("ids"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(entity));
        assertThat(adapter.findVisibleByTaskIds(List.of(31))).extracting(Subtask::id).containsExactly(41);
    }

    @Test
    void searchesArchivedSubtasksAndChecksOpenChildren() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(List.of(41L));
        @SuppressWarnings("unchecked")
        TypedQuery<SubtaskEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(SubtaskEntity.class))).thenReturn(query);
        when(query.setParameter(eq("ids"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(entity));

        assertThat(adapter.findArchived(7, "permiso")).extracting(Subtask::id).containsExactly(41);

        @SuppressWarnings("unchecked")
        TypedQuery<Long> count = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(count);
        when(count.setParameter(eq("taskId"), any())).thenReturn(count);
        when(count.getSingleResult()).thenReturn(2L, 0L);
        assertThat(adapter.hasOpenSubtasks(31)).isTrue();
        assertThat(adapter.hasOpenSubtasks(99)).isFalse();
    }

    @Test
    void persistsNewAndCompletedSubtasks() {
        AppUserEntity completer = new AppUserEntity();
        completer.setUserId(1);
        when(entityManager.getReference(TaskEntity.class, 31)).thenReturn(task);
        when(entityManager.getReference(AppUserEntity.class, 1)).thenReturn(completer);
        when(entityManager.find(SubtaskEntity.class, 41)).thenReturn(entity);
        doAnswer(invocation -> {
            SubtaskEntity saved = invocation.getArgument(0);
            saved.setSubtaskId(42);
            return null;
        }).when(entityManager).persist(any(SubtaskEntity.class));

        Subtask created = adapter.save(Subtask.create(31, "Nueva", null, NOW));
        assertThat(created.id()).isEqualTo(42);
        Subtask completed = adapter.save(Subtask.restore(41, 31, "Terminada", null,
                WorkStatus.COMPLETADA, NOW, 1, NOW, NOW));
        assertThat(completed.completedByUserId()).isEqualTo(1);
    }

    private static SubtaskEntity subtask(Integer id, TaskEntity parent, WorkStatusEntity status) {
        SubtaskEntity row = new SubtaskEntity();
        row.setSubtaskId(id);
        row.setTask(parent);
        row.setTitle("Subtarea");
        row.setStatus(status);
        row.setCreatedAt(NOW);
        row.setUpdatedAt(NOW);
        return row;
    }

    private static WorkStatusEntity status(String name) {
        WorkStatusEntity result = new WorkStatusEntity();
        result.setName(name);
        return result;
    }

    private static ProjectEntity project(Integer id) {
        ProjectEntity result = new ProjectEntity();
        result.setProjectId(id);
        return result;
    }
}
