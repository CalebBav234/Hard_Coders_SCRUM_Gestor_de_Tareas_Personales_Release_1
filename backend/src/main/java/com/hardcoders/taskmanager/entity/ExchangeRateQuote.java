package com.hardcoders.taskmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Cotización de bolivianos por dólar. Tabla {@code task_manager.exchange_rate_quote}. */
@Entity
@Table(name = "exchange_rate_quote", schema = "task_manager")
public class ExchangeRateQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_rate_quote_id")
    private Integer exchangeRateQuoteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exchange_rate_source_id", nullable = false)
    private ExchangeRateSource source;

    @Column(name = "rate_kind", nullable = false, length = 12)
    private String rateKind;

    @Column(name = "bob_per_usd", nullable = false, precision = 18, scale = 6)
    private BigDecimal bobPerUsd;

    @Column(name = "quoted_at", nullable = false)
    private Instant quotedAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }

    public Integer getExchangeRateQuoteId() {
        return exchangeRateQuoteId;
    }

    public void setExchangeRateQuoteId(Integer exchangeRateQuoteId) {
        this.exchangeRateQuoteId = exchangeRateQuoteId;
    }

    public ExchangeRateSource getSource() {
        return source;
    }

    public void setSource(ExchangeRateSource source) {
        this.source = source;
    }

    public String getRateKind() {
        return rateKind;
    }

    public void setRateKind(String rateKind) {
        this.rateKind = rateKind;
    }

    public BigDecimal getBobPerUsd() {
        return bobPerUsd;
    }

    public void setBobPerUsd(BigDecimal bobPerUsd) {
        this.bobPerUsd = bobPerUsd;
    }

    public Instant getQuotedAt() {
        return quotedAt;
    }

    public void setQuotedAt(Instant quotedAt) {
        this.quotedAt = quotedAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
