package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskAssignmentEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskAssignmentId;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskEntity;
import com.hardcoders.taskmanager.domain.model.TaskAssignment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private EntityManager entityManager;
    private AssignmentPersistenceAdapter adapter;
    private TaskAssignmentEntity row;
    private TaskEntity task;
    private AppUserEntity worker;
    private AppUserEntity manager;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        adapter = new AssignmentPersistenceAdapter();
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
        task = new TaskEntity();
        task.setTaskId(31);
        worker = new AppUserEntity();
        worker.setUserId(4);
        manager = new AppUserEntity();
        manager.setUserId(1);
        row = new TaskAssignmentEntity();
        row.setId(new TaskAssignmentId(31, 4));
        row.setTask(task);
        row.setWorker(worker);
        row.setAssignedAt(NOW);
        row.setAssignedBy(manager);
    }

    @Test
    void findsAssignmentByCompositeKeyAndReturnsEmptyForMissingKey() {
        when(entityManager.find(eq(TaskAssignmentEntity.class), any(TaskAssignmentId.class)))
                .thenReturn(row, null);

        assertThat(adapter.find(31, 4)).get().extracting(TaskAssignment::workerUserId).isEqualTo(4);
        assertThat(adapter.find(31, 9)).isEmpty();
    }

    @Test
    void listsAssignmentsByTaskWorkerAndGroupedTaskIds() {
        @SuppressWarnings("unchecked")
        TypedQuery<TaskAssignmentEntity> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(TaskAssignmentEntity.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(row));

        assertThat(adapter.findForTask(31)).extracting(TaskAssignment::workerUserId).containsExactly(4);
        assertThat(adapter.findForWorker(4)).extracting(TaskAssignment::taskId).containsExactly(31);
        assertThat(adapter.findForTasks(List.of(31))).containsOnlyKeys(31);
        assertThat(adapter.findForTasks(List.of(31)).get(31))
                .extracting(TaskAssignment::workerUserId).containsExactly(4);
        assertThat(adapter.findForTasks(List.of())).isEmpty();
    }

    @Test
    void persistsAndDeletesAssignmentRows() {
        when(entityManager.getReference(TaskEntity.class, 31)).thenReturn(task);
        when(entityManager.getReference(AppUserEntity.class, 4)).thenReturn(worker);
        when(entityManager.getReference(AppUserEntity.class, 1)).thenReturn(manager);

        TaskAssignment saved = adapter.save(new TaskAssignment(31, 4, NOW, 1));
        assertThat(saved.taskId()).isEqualTo(31);
        assertThat(saved.workerUserId()).isEqualTo(4);
        verify(entityManager).persist(any(TaskAssignmentEntity.class));
        verify(entityManager).flush();

        Query delete = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(delete);
        when(delete.setParameter(anyString(), any())).thenReturn(delete);
        adapter.delete(31, 4);
        verify(delete).executeUpdate();
    }
}
