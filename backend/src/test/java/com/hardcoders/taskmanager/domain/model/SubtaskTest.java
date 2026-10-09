package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubtaskTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    @Test
    void followsTheSameLifecycleAsTasks() {
        Subtask subtask = Subtask.restore(4, 1, "Revisar", null, WorkStatus.PENDIENTE, null, null, NOW, NOW);

        subtask.start();
        subtask.complete(9, NOW);
        assertThat(subtask.status()).isEqualTo(WorkStatus.COMPLETADA);
        assertThat(subtask.completedByUserId()).isEqualTo(9);

        subtask.reopen();
        assertThat(subtask.status()).isEqualTo(WorkStatus.EN_PROGRESO);
        assertThat(subtask.completedAt()).isNull();

        subtask.pause();
        subtask.cancel();
        assertThat(subtask.status()).isEqualTo(WorkStatus.CANCELADA);
        assertThatThrownBy(subtask::start).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void normalizesTexts() {
        Subtask subtask = Subtask.create(1, "  Título ", "   ", NOW);

        assertThat(subtask.title()).isEqualTo("Título");
        assertThat(subtask.description()).isNull();
        assertThat(subtask.status()).isEqualTo(WorkStatus.PENDIENTE);
    }
}
