package com.hardcoders.taskmanager.domain.policy;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.model.WorkText;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de WorkText: títulos, descripciones y nombres de actividad. */
class WorkTextTest {

    @Test
    void titleValidatesNonBlankAndMaxLength() {
        assertThat(WorkText.title("  Hola  ")).isEqualTo("Hola");
        assertThatThrownBy(() -> WorkText.title("   ")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> WorkText.title(null)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> WorkText.title("x".repeat(161)))
                .isInstanceOf(DomainValidationException.class);
        assertThatCode(() -> WorkText.title("x".repeat(160))).doesNotThrowAnyException();
    }

    @Test
    void descriptionAllowsNullAndValidatesMaxLength() {
        assertThat(WorkText.description(null)).isNull();
        assertThat(WorkText.description("   ")).isNull();
        assertThat(WorkText.description("  desc  ")).isEqualTo("desc");
        assertThatThrownBy(() -> WorkText.description("x".repeat(2001)))
                .isInstanceOf(DomainValidationException.class);
        assertThatCode(() -> WorkText.description("x".repeat(2000))).doesNotThrowAnyException();
    }

    @Test
    void activityNameAllowsNullAndValidatesMaxLength() {
        assertThat(WorkText.activityName(null)).isNull();
        assertThat(WorkText.activityName("   ")).isNull();
        assertThat(WorkText.activityName("  Cat  ")).isEqualTo("Cat");
        assertThatThrownBy(() -> WorkText.activityName("x".repeat(161)))
                .isInstanceOf(DomainValidationException.class);
        assertThatCode(() -> WorkText.activityName("x".repeat(160))).doesNotThrowAnyException();
    }
}
