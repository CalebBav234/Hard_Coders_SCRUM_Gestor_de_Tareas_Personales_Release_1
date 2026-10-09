package com.hardcoders.taskmanager.adapter.out.persistence.repository;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.ExchangeRateQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateQuoteJpaRepository extends JpaRepository<ExchangeRateQuoteEntity, Integer> {

    Optional<ExchangeRateQuoteEntity> findFirstBySourceExchangeRateSourceIdAndRateKindOrderByQuotedAtDesc(Integer sourceId, String rateKind);
}
