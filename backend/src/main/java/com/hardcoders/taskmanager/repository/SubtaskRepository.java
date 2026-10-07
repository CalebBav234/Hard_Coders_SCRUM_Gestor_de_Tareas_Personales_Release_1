package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubtaskRepository extends JpaRepository<Subtask, Integer> {

    List<Subtask> findByTaskTaskId(Integer taskId);
}
