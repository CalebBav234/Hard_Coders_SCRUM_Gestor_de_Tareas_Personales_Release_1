package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    String SUMMARY_COLUMNS = """
        id, title, description, status, priority,
        category_id, parent_task_id,
        activated_at, completed_at,
        total_active_seconds, effective_active_seconds,
        created_at, updated_at, version
        """;

    @Query(value = "SELECT " + SUMMARY_COLUMNS + " FROM task_manager.v_tasks ORDER BY created_at DESC",
            nativeQuery = true)
    List<Object[]> findSummaries();

    @Query(value = "SELECT " + SUMMARY_COLUMNS + " FROM task_manager.v_tasks WHERE id = :id",
            nativeQuery = true)
    Object[] findSummaryById(@Param("id") Long id);
}
