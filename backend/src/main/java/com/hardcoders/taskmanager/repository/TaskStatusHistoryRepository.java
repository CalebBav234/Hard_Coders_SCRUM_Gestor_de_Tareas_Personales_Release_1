package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.TaskStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskStatusHistoryRepository extends JpaRepository<TaskStatusHistory, Integer> {

    List<TaskStatusHistory> findByTaskTaskIdOrderByChangedAtDesc(Integer taskId);
}
