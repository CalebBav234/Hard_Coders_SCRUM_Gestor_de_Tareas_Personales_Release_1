package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.WorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkStatusRepository extends JpaRepository<WorkStatus, Integer> {

    Optional<WorkStatus> findByName(String name);
}
