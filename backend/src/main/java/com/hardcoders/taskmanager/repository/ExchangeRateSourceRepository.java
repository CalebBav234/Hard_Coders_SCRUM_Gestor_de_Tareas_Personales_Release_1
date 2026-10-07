package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.ExchangeRateSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateSourceRepository extends JpaRepository<ExchangeRateSource, Integer> {

    Optional<ExchangeRateSource> findByName(String name);
}
