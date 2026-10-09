package com.hardcoders.taskmanager.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VersionTokenTest {

    @Test
    void usesMicrosecondsLikePostgresql() {
        Instant updatedAt = Instant.parse("2026-10-09T15:30:00.123456Z");

        assertThat(VersionToken.of(updatedAt)).isEqualTo(1_791_559_800_123_456L);
        // Cabe en un número de JavaScript sin perder precisión.
        assertThat(VersionToken.of(updatedAt)).isLessThan(9_007_199_254_740_991L);
    }
}
