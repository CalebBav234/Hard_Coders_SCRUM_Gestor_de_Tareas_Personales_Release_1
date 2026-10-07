package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Integer> {

    List<Task> findByActivityActivityId(Integer activityId);
}
