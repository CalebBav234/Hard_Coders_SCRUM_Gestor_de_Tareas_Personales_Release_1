package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskPriorityTest {

    @Test
    void parsesValidValuesAndDefaults() {
        assertThat(TaskPriority.parseOrDefault(null)).isEqualTo(TaskPriority.MEDIA);
        assertThat(TaskPriority.parseOrDefault("  ")).isEqualTo(TaskPriority.MEDIA);
        assertThat(TaskPriority.parseOrDefault("alta")).isEqualTo(TaskPriority.ALTA);
        assertThat(TaskPriority.parseOrDefault(" BAJA ")).isEqualTo(TaskPriority.BAJA);
    }

    @Test
    void rejectsUnknownValues() {
        assertThatThrownBy(() -> TaskPriority.parseOrDefault("URGENTE"))
                .isInstanceOf(DomainValidationException.class);
    }
}
