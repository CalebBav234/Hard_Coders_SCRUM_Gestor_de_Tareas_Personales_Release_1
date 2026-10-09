package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskPriorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskPriorityJpaRepository extends JpaRepository<TaskPriorityEntity, Integer> {

    Optional<TaskPriorityEntity> findByName(String name);
    List<TaskPriorityEntity> findByIsEnabledTrueOrderBySortOrderAsc();
}
