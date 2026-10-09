package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskAssignmentEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.TaskAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAssignmentJpaRepository extends JpaRepository<TaskAssignmentEntity, TaskAssignmentId> {

    List<TaskAssignmentEntity> findByTaskTaskId(Integer taskId);
    List<TaskAssignmentEntity> findByWorkerUserId(Integer workerUserId);
}
