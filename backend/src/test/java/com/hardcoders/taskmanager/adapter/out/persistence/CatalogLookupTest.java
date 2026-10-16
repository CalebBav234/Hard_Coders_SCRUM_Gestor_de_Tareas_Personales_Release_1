package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectStatusEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskPriorityEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.WorkStatusEntity;
import com.hardcoders.taskmanager.domain.model.TaskPriority;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** CAT-01: CatalogLookup resuelve las filas de catálogo por nombre y falla si faltan. */
class CatalogLookupTest {

    private EntityManager entityManager;
    private CatalogLookup catalogs;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        catalogs = new CatalogLookup();
        ReflectionTestUtils.setField(catalogs, "entityManager", entityManager);
    }

    @Test
    @DisplayName("CAT-01: workStatus devuelve la fila del catálogo por nombre")
    void workStatusResolvesByName() {
        TypedQuery<WorkStatusEntity> query = typedQuery(List.of(status("PENDIENTE")));
        when(entityManager.createQuery(anyString(), eq(WorkStatusEntity.class))).thenReturn(query);

        assertThat(catalogs.workStatus(WorkStatus.PENDIENTE).getName()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("CAT-02: workStatus falla si el estado no está en el catálogo")
    void workStatusFailsWhenMissing() {
        TypedQuery<WorkStatusEntity> query = typedQuery(List.of());
        when(entityManager.createQuery(anyString(), eq(WorkStatusEntity.class))).thenReturn(query);

        assertThatThrownBy(() -> catalogs.workStatus(WorkStatus.PENDIENTE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Falta el estado");
    }

    @Test
    @DisplayName("CAT-03: priority devuelve la fila del catálogo o falla si falta")
    void priorityResolvesByNameOrFails() {
        TypedQuery<TaskPriorityEntity> found = typedQuery(List.of(priority("MEDIA")));
        TypedQuery<TaskPriorityEntity> missing = typedQuery(List.of());
        when(entityManager.createQuery(anyString(), eq(TaskPriorityEntity.class))).thenReturn(found, missing);

        assertThat(catalogs.priority(TaskPriority.MEDIA).getName()).isEqualTo("MEDIA");
        assertThatThrownBy(() -> catalogs.priority(TaskPriority.MEDIA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Falta la prioridad");
    }

    @Test
    @DisplayName("CAT-04: projectStatus devuelve la fila del catálogo o falla si falta")
    void projectStatusResolvesByNameOrFails() {
        TypedQuery<ProjectStatusEntity> found = typedQuery(List.of(projectStatus("PLANIFICADO")));
        TypedQuery<ProjectStatusEntity> missing = typedQuery(List.of());
        when(entityManager.createQuery(anyString(), eq(ProjectStatusEntity.class))).thenReturn(found, missing);

        assertThat(catalogs.projectStatus("PLANIFICADO").getName()).isEqualTo("PLANIFICADO");
        assertThatThrownBy(() -> catalogs.projectStatus("INEXISTENTE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Falta el estado");
    }

    @SuppressWarnings("unchecked")
    private <T> TypedQuery<T> typedQuery(List<?> rows) {
        TypedQuery<T> query = mock(TypedQuery.class);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn((List<T>) rows);
        return query;
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

    private static ProjectStatusEntity projectStatus(String name) {
        ProjectStatusEntity entity = new ProjectStatusEntity();
        entity.setName(name);
        return entity;
    }
}
