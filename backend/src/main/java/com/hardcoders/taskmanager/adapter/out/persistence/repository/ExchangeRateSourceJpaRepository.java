package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ExchangeRateSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateSourceJpaRepository extends JpaRepository<ExchangeRateSourceEntity, Integer> {

    Optional<ExchangeRateSourceEntity> findByName(String name);
}
