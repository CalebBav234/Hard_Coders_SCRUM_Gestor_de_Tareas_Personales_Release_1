package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Integer> {

    List<Activity> findByStageStageId(Integer stageId);
}
