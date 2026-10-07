package com.hardcoders.taskmanager.repository;

import com.hardcoders.taskmanager.entity.ExchangeRateQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateQuoteRepository extends JpaRepository<ExchangeRateQuote, Integer> {

    Optional<ExchangeRateQuote> findFirstBySourceExchangeRateSourceIdAndRateKindOrderByQuotedAtDesc(Integer sourceId, String rateKind);
}
