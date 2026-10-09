package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.WorkStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkStatusJpaRepository extends JpaRepository<WorkStatusEntity, Integer> {

    Optional<WorkStatusEntity> findByName(String name);
}
