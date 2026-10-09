package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskPlanHistoryEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskPlanHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskPlanHistoryJpaRepository extends JpaRepository<TaskPlanHistoryEntity, TaskPlanHistoryId> {

    List<TaskPlanHistoryEntity> findByTaskTaskIdOrderByIdRevisionNumberDesc(Integer taskId);
}
