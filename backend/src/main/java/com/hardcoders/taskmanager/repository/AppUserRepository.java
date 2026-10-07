package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
