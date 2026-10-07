package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.TaskPriority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskPriorityRepository extends JpaRepository<TaskPriority, Integer> {

    Optional<TaskPriority> findByName(String name);
    List<TaskPriority> findByIsEnabledTrueOrderBySortOrderAsc();
}
