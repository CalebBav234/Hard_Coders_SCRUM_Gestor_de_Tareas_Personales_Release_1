package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityJpaRepository extends JpaRepository<ActivityEntity, Integer> {

    List<ActivityEntity> findByStageStageId(Integer stageId);
}
