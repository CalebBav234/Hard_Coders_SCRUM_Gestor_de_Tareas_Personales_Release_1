package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ProjectStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectStatusJpaRepository extends JpaRepository<ProjectStatusEntity, Integer> {

    Optional<ProjectStatusEntity> findByName(String name);
}
