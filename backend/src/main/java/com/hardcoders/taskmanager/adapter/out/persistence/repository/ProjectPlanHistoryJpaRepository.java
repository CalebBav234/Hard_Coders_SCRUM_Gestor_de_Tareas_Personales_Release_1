package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectPlanHistoryEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectPlanHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectPlanHistoryJpaRepository extends JpaRepository<ProjectPlanHistoryEntity, ProjectPlanHistoryId> {

    List<ProjectPlanHistoryEntity> findByProjectProjectIdOrderByIdRevisionNumberDesc(Integer projectId);
}
