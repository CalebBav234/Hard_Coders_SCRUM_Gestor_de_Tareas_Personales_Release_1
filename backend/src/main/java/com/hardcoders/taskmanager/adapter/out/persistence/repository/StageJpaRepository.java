package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.StageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageJpaRepository extends JpaRepository<StageEntity, Integer> {

    List<StageEntity> findByProjectProjectId(Integer projectId);
}
