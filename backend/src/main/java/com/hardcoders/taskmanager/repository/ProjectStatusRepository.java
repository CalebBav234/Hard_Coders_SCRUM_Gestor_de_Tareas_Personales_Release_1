package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectStatusRepository extends JpaRepository<ProjectStatus, Integer> {

    Optional<ProjectStatus> findByName(String name);
}
