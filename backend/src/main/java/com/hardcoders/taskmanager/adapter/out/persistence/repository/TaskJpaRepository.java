package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, Integer> {

    List<TaskEntity> findByActivityActivityId(Integer activityId);
}
