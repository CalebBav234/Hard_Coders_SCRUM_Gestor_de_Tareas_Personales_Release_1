package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.TaskAssignment;
import com.hardcoders.taskmanager.entity.TaskAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, TaskAssignmentId> {

    List<TaskAssignment> findByTaskTaskId(Integer taskId);
    List<TaskAssignment> findByWorkerUserId(Integer workerUserId);
}
