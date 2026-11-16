package com.hardcoders.taskmanager.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkStatusTest {

    @Test
    void openOnlyForPendingAndInProgress() {
        assertThat(WorkStatus.PENDIENTE.isOpen()).isTrue();
        assertThat(WorkStatus.EN_PROGRESO.isOpen()).isTrue();
        assertThat(WorkStatus.COMPLETADA.isOpen()).isFalse();
        assertThat(WorkStatus.CANCELADA.isOpen()).isFalse();
    }
}
