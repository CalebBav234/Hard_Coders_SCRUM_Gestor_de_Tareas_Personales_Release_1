package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskStatusHistoryJpaRepository extends JpaRepository<TaskStatusHistoryEntity, Integer> {

    List<TaskStatusHistoryEntity> findByTaskTaskIdOrderByChangedAtDesc(Integer taskId);
}
