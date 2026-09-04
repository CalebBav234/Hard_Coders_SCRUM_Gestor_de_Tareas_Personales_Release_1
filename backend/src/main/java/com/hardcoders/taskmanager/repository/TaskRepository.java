package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByParentTaskIdAndDeletedAtIsNull(Long parentTaskId);

    String SUMMARY_COLUMNS = """
        id, title, description, status, priority,
        category_id, category_name, parent_task_id,
        activated_at, completed_at,
        total_active_seconds, effective_active_seconds,
        created_at, updated_at, version
        """;

    String ARCHIVE_COLUMNS = """
        id, title, description, status, priority,
        category_id, category_name, parent_task_id,
        activated_at, completed_at,
        total_active_seconds, effective_active_seconds,
        created_at, updated_at, deleted_at, version
        """;

    // Read the existing tables so upgrading the app does not require V003 first.
    // V003 remains an optional convenience view for SQL clients, not an API dependency.
    String ARCHIVE_SOURCE = """
        FROM (
            SELECT task.id, task.title, task.description, task.status, task.priority,
                task.category_id, category.name AS category_name, task.parent_task_id,
                task.activated_at, task.completed_at, task.total_active_seconds,
                task.total_active_seconds + CASE
                    WHEN task.status = 'ACTIVA' AND task.activated_at IS NOT NULL
                        THEN floor(EXTRACT(epoch FROM (
                            COALESCE(task.deleted_at, CURRENT_TIMESTAMP) - task.activated_at
                        )))::bigint
                    ELSE 0
                END AS effective_active_seconds,
                task.created_at, task.updated_at, task.deleted_at, task.version
            FROM task_manager.tasks AS task
            LEFT JOIN task_manager.categories AS category ON category.id = task.category_id
            WHERE task.status = 'TERMINADA' OR task.deleted_at IS NOT NULL
        ) AS archive
        """;

    String SEARCH_PREDICATE = """
        (translate(lower(coalesce(title, '')), 'áéíóúüñ', 'aeiouun') LIKE :searchPattern ESCAPE '\\'
         OR translate(lower(coalesce(description, '')), 'áéíóúüñ', 'aeiouun') LIKE :searchPattern ESCAPE '\\')
        """;

    @Query(value = "SELECT " + SUMMARY_COLUMNS + " FROM task_manager.v_tasks ORDER BY created_at DESC",
            nativeQuery = true)
    List<Object[]> findSummaries();

    @Query(value = "SELECT " + SUMMARY_COLUMNS + " FROM task_manager.v_tasks WHERE "
            + SEARCH_PREDICATE + " ORDER BY created_at DESC", nativeQuery = true)
    List<Object[]> findSummariesBySearchPattern(@Param("searchPattern") String searchPattern);

    @Query(value = "SELECT " + ARCHIVE_COLUMNS + ARCHIVE_SOURCE
            + "ORDER BY COALESCE(deleted_at, completed_at, updated_at) DESC, id DESC", nativeQuery = true)
    List<Object[]> findArchivedSummaries();

    @Query(value = "SELECT " + ARCHIVE_COLUMNS + ARCHIVE_SOURCE + " WHERE "
            + SEARCH_PREDICATE
            + " ORDER BY COALESCE(deleted_at, completed_at, updated_at) DESC, id DESC", nativeQuery = true)
    List<Object[]> findArchivedSummariesBySearchPattern(
            @Param("searchPattern") String searchPattern);

    @Query(value = """
            SELECT id, task_id, from_status, to_status, change_reason, changed_at
            FROM task_manager.v_task_history
            WHERE task_id IN (:taskIds)
            ORDER BY task_id, changed_at DESC, id DESC
            """, nativeQuery = true)
    List<Object[]> findHistoryEventsByTaskIds(@Param("taskIds") List<Long> taskIds);

    @Query(value = "SELECT " + SUMMARY_COLUMNS + " FROM task_manager.v_tasks WHERE id = :id",
            nativeQuery = true)
    Object[] findSummaryById(@Param("id") Long id);
}
