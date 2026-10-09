package com.hardcoders.taskmanager.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fuente que publica cotizaciones BOB/USD. Tabla {@code task_manager.exchange_rate_source}. */
@Entity
@Table(name = "exchange_rate_source", schema = "task_manager")
public class ExchangeRateSourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_rate_source_id")
    private Integer exchangeRateSourceId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "website_url", nullable = false, length = 2048)
    private String websiteUrl;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    public Integer getExchangeRateSourceId() {
        return exchangeRateSourceId;
    }

    public void setExchangeRateSourceId(Integer exchangeRateSourceId) {
        this.exchangeRateSourceId = exchangeRateSourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
