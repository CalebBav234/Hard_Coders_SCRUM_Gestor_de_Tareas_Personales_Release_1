package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, Integer> {

    List<ProjectEntity> findByManagerUserId(Integer managerUserId);
}
