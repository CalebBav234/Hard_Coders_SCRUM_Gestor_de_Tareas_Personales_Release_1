package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.dto.CategorySummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<CategorySummary> findVisibleSummaries() {
        return entityManager.createNativeQuery(
                "SELECT id, name FROM task_manager.categories WHERE deleted_at IS NULL ORDER BY lower(name)")
                .getResultList()
                .stream()
                .map(row -> {
                    Object[] cols = (Object[]) row;
                    return new CategorySummary(((Number) cols[0]).longValue(), (String) cols[1]);
                })
                .toList();
    }

    public Long findVisibleIdByName(String name) {
        if (name == null) {
            return null;
        }
        List<?> rows = entityManager.createNativeQuery(
                "SELECT id FROM task_manager.categories WHERE deleted_at IS NULL AND lower(btrim(name)) = lower(btrim(:name))")
                .setParameter("name", name)
                .getResultList();
        return rows.isEmpty() ? null : ((Number) rows.get(0)).longValue();
    }

    public Long insertVisibleAndReturnId(String name) {
        entityManager.createNativeQuery(
                "INSERT INTO task_manager.categories (name) VALUES (:name)")
                .setParameter("name", name)
                .executeUpdate();
        return findVisibleIdByName(name);
    }
}
