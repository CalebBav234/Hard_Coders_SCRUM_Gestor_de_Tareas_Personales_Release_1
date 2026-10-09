package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.SubtaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubtaskJpaRepository extends JpaRepository<SubtaskEntity, Integer> {

    List<SubtaskEntity> findByTaskTaskId(Integer taskId);
}
