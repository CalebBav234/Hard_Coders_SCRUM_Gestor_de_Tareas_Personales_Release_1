package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.TaskPlanHistory;
import com.hardcoders.taskmanager.entity.TaskPlanHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskPlanHistoryRepository extends JpaRepository<TaskPlanHistory, TaskPlanHistoryId> {

    List<TaskPlanHistory> findByTaskTaskIdOrderByIdRevisionNumberDesc(Integer taskId);
}
