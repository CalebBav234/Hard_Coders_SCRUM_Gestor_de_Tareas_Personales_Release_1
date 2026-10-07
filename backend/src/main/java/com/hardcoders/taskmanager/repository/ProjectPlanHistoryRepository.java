package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.ProjectPlanHistory;
import com.hardcoders.taskmanager.entity.ProjectPlanHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectPlanHistoryRepository extends JpaRepository<ProjectPlanHistory, ProjectPlanHistoryId> {

    List<ProjectPlanHistory> findByProjectProjectIdOrderByIdRevisionNumberDesc(Integer projectId);
}
