package com.hardcoders.taskmanager.domain.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressPolicyTest {

    @Test
    void leafWorkRatioHandlesEmptyAndBounds() {
        assertThat(ProgressPolicy.leafWorkRatio(0, 0)).isEqualTo(0.0);
        assertThat(ProgressPolicy.leafWorkRatio(5, 0)).isEqualTo(0.0);
        assertThat(ProgressPolicy.leafWorkRatio(-1, 10)).isEqualTo(0.0);
        assertThat(ProgressPolicy.leafWorkRatio(3, 4)).isEqualTo(0.75);
        assertThat(ProgressPolicy.leafWorkRatio(10, 4)).isEqualTo(1.0);
    }

    @Test
    void addIsSaturating() {
        assertThat(ProgressPolicy.add(2, 3)).isEqualTo(5);
        assertThat(ProgressPolicy.add(Long.MAX_VALUE, 1)).isEqualTo(Long.MAX_VALUE);
    }
}
