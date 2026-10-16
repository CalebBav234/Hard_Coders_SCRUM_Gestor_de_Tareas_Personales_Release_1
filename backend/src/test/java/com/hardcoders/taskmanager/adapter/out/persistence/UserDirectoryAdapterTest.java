package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DIR-01: UserDirectoryAdapter expone las lecturas de app_user y traduce banderas booleanas. */
class UserDirectoryAdapterTest {

    private EntityManager entityManager;
    private UserDirectoryAdapter adapter;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        adapter = new UserDirectoryAdapter();
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);
    }

    @Test
    @DisplayName("DIR-01: findById devuelve la cuenta de dominio o vacío si no existe")
    void findByIdMapsOrEmpty() {
        AppUserEntity entity = user(1, true, true);
        when(entityManager.find(AppUserEntity.class, 1)).thenReturn(entity);
        when(entityManager.find(AppUserEntity.class, 999)).thenReturn(null);

        assertThat(adapter.findById(1)).get()
                .isEqualTo(new UserAccount(1, "jefe@gestor.local", "Julia", "Mendoza", true, true));
        assertThat(adapter.findById(999)).isEmpty();
    }

    @Test
    @DisplayName("DIR-02: findByIds indexa por id y devuelve vacío sin consultar cuando no hay ids")
    void findByIdsIndexesByUserId() {
        TypedQuery<AppUserEntity> query = typedQuery(List.of(user(1, true, true), user(2, false, true)));
        when(entityManager.createQuery(anyString(), eq(AppUserEntity.class))).thenReturn(query);

        Map<Integer, UserAccount> found = adapter.findByIds(List.of(1, 2));
        assertThat(found).containsOnlyKeys(1, 2);
        assertThat(found.get(1).chief()).isTrue();
        assertThat(found.get(2).chief()).isFalse();

        Collection<Integer> empty = List.of();
        assertThat(adapter.findByIds(empty)).isEmpty();
    }

    @Test
    @DisplayName("DIR-03: findActive mapea todos los usuarios activos del catálogo")
    void findActiveMapsEveryUser() {
        TypedQuery<AppUserEntity> query = typedQuery(List.of(user(1, false, true)));
        when(entityManager.createQuery(anyString(), eq(AppUserEntity.class))).thenReturn(query);

        List<UserAccount> active = adapter.findActive();
        assertThat(active).extracting(UserAccount::userId).containsExactly(1);
        assertThat(active.getFirst().fullName()).isEqualTo("Julia Mendoza");
    }

    @SuppressWarnings("unchecked")
    private <T> TypedQuery<T> typedQuery(List<?> rows) {
        TypedQuery<T> query = mock(TypedQuery.class);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn((List<T>) rows);
        return query;
    }

    private static AppUserEntity user(Integer id, boolean chief, boolean active) {
        AppUserEntity entity = new AppUserEntity();
        entity.setUserId(id);
        entity.setEmail("jefe@gestor.local");
        entity.setPasswordHash("hash");
        entity.setGivenNames("Julia");
        entity.setFamilyNames("Mendoza");
        entity.setIsChief(chief);
        entity.setIsActive(active);
        return entity;
    }
}
