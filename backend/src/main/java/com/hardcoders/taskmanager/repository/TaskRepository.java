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

    @Query(value = "SELECT " + ARCHIVE_COLUMNS + " FROM task_manager.v_task_archive "
            + "ORDER BY COALESCE(deleted_at, completed_at, updated_at) DESC", nativeQuery = true)
    List<Object[]> findArchivedSummaries();

    @Query(value = "SELECT " + ARCHIVE_COLUMNS + " FROM task_manager.v_task_archive WHERE "
            + SEARCH_PREDICATE
            + " ORDER BY COALESCE(deleted_at, completed_at, updated_at) DESC", nativeQuery = true)
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
