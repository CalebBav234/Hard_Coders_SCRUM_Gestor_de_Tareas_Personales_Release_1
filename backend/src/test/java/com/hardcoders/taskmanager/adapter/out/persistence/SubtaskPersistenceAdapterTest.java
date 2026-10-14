package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.SubtaskEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.WorkStatusEntity;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regresión de la pantalla de Historial: el adaptador casteaba el flujo de la consulta nativa
 * a {@code List} (ClassCastException en cada llamada a {@code /api/subtasks/history}). Aquí se
 * comprueba que los identificadores que devuelve PostgreSQL se mapean a subtareas de dominio.
 */
class SubtaskPersistenceAdapterTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final SubtaskPersistenceAdapter adapter =
            new SubtaskPersistenceAdapter(mock(CatalogLookup.class));

    SubtaskPersistenceAdapterTest() {
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
    }

    @Test
    void mapsTheArchivedIdsReturnedByTheNativeQuery() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(eq("projectId"), any())).thenReturn(nativeQuery);
        // PostgreSQL devuelve una fila con una sola columna: el sponsor solo envuelve el id.
        when(nativeQuery.getResultList()).thenReturn(List.of(9L));

        @SuppressWarnings("unchecked")
        TypedQuery<SubtaskEntity> jpaQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(SubtaskEntity.class))).thenReturn(jpaQuery);
        when(jpaQuery.setParameter(eq("ids"), any())).thenReturn(jpaQuery);
        when(jpaQuery.getResultList()).thenReturn(List.of(archivedSubtask()));

        List<Subtask> archived = adapter.findArchived(1, null);

        assertThat(archived).hasSize(1);
        assertThat(archived.getFirst().id()).isEqualTo(9);
        assertThat(archived.getFirst().taskId()).isEqualTo(3);
        assertThat(archived.getFirst().title()).isEqualTo("Pedir permisos");
        assertThat(archived.getFirst().status()).isEqualTo(WorkStatus.COMPLETADA);
    }

    @Test
    void returnsNothingWhenTheProjectHasNoArchivedSubtasks() {
        Query nativeQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(eq("projectId"), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(List.of());

        assertThat(adapter.findArchived(1, "patron")).isEmpty();
    }

    private SubtaskEntity archivedSubtask() {
        TaskEntity task = new TaskEntity();
        task.setTaskId(3);
        WorkStatusEntity status = new WorkStatusEntity();
        status.setName("COMPLETADA");
        SubtaskEntity entity = new SubtaskEntity();
        entity.setSubtaskId(9);
        entity.setTask(task);
        entity.setTitle("Pedir permisos");
        entity.setDescription("Municipalidad");
        entity.setStatus(status);
        entity.setCompletedAt(Instant.parse("2026-10-14T10:00:00Z"));
        entity.setCreatedAt(Instant.parse("2026-10-14T09:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-10-14T10:00:00Z"));
        return entity;
    }
}
