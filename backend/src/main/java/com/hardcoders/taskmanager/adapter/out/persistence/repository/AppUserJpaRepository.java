package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserJpaRepository extends JpaRepository<AppUserEntity, Integer> {

    Optional<AppUserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
