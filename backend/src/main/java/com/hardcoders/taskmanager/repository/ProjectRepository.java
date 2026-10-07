package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    List<Project> findByManagerUserId(Integer managerUserId);
}
