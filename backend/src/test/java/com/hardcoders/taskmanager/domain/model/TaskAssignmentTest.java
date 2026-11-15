package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** EC3-02 / EC3-03: motivo de la asignación persistido y limitado a 500 caracteres en el dominio. */
class TaskAssignmentTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("TA-EP-01: motivo nulo o vacío usa el motivo por defecto")
    void blankReasonFallsBackToDefault(String reason) {
        assertThat(TaskAssignment.normalizeReason(reason)).isEqualTo(TaskAssignment.DEFAULT_REASON);
    }

    @Test
    @DisplayName("TA-EP-02: motivo válido se recorta")
    void explicitReasonIsStripped() {
        assertThat(TaskAssignment.normalizeReason("  Refuerzo de cuadrilla  ")).isEqualTo("Refuerzo de cuadrilla");
    }

    @Test
    @DisplayName("TA-BVA-01: 499 y 500 caracteres se aceptan; 501 se rechaza")
    void reasonLengthBoundaries() {
        assertThat(TaskAssignment.normalizeReason("a".repeat(499))).hasSize(499);
        assertThat(TaskAssignment.normalizeReason("a".repeat(500))).hasSize(500);
        assertThatThrownBy(() -> TaskAssignment.normalizeReason("a".repeat(501)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("El motivo no puede superar 500 caracteres");
    }

    @Test
    @DisplayName("TA-BVA-02: los espacios exteriores no cuentan para el límite")
    void surroundingSpacesDoNotCountTowardsTheLimit() {
        assertThat(TaskAssignment.normalizeReason("  " + "a".repeat(500) + "  ")).hasSize(500);
    }

    @Test
    @DisplayName("TA-01: el constructor conserva el motivo; el de 4 argumentos (filas previas a V006) lo deja nulo")
    void constructorsKeepReason() {
        TaskAssignment withReason = new TaskAssignment(31, 4, NOW, 1, "Refuerzo");
        assertThat(withReason.reason()).isEqualTo("Refuerzo");
        assertThat(withReason.taskId()).isEqualTo(31);
        assertThat(withReason.workerUserId()).isEqualTo(4);
        assertThat(withReason.assignedAt()).isEqualTo(NOW);
        assertThat(withReason.assignedByUserId()).isEqualTo(1);

        TaskAssignment legacy = new TaskAssignment(31, 4, null, 1);
        assertThat(legacy.reason()).isNull();
        assertThat(legacy.assignedAt()).isEqualTo(Instant.EPOCH);
    }
}
