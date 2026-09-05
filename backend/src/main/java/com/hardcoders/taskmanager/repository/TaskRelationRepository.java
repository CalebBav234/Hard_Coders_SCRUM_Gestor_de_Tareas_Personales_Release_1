package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.TaskRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRelationRepository extends JpaRepository<TaskRelation, Long> {
    
    List<TaskRelation> findBySourceTaskId(Long sourceTaskId);
    
    boolean existsBySourceTaskIdAndTargetTaskIdAndRelationType(Long sourceTaskId, Long targetTaskId, String relationType);
}
