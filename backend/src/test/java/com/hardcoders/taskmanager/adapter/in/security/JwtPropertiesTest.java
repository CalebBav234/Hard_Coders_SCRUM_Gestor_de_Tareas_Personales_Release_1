package com.hardcoders.taskmanager.adapter.in.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JWTPROP-01: normalización de las propiedades del token (secreto, expiración y emisor por defecto). */
class JwtPropertiesTest {

    private static final String VALID_SECRET = "test-secret-0123456789-0123456789-abcdef";

    @Test
    @DisplayName("JWTPROP-01: conserva los valores válidos provistos")
    void keepsValidValues() {
        JwtProperties properties = new JwtProperties(VALID_SECRET, 30, "mi-emisor");

        assertThat(properties.secret()).isEqualTo(VALID_SECRET);
        assertThat(properties.expirationMinutes()).isEqualTo(30);
        assertThat(properties.issuer()).isEqualTo("mi-emisor");
    }

    @Test
    @DisplayName("JWTPROP-02: un secreto corto o nulo rompe el arranque")
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> new JwtProperties("corto", 30, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new JwtProperties(null, 30, "x"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("JWTPROP-03: una expiración no positiva y un emisor vacío toman valores por defecto")
    void defaultsAreApplied() {
        JwtProperties properties = new JwtProperties(VALID_SECRET, 0, "   ");

        assertThat(properties.expirationMinutes()).isEqualTo(480);
        assertThat(properties.issuer()).isEqualTo("gestor-tareas");

        assertThat(new JwtProperties(VALID_SECRET, 10, null).issuer()).isEqualTo("gestor-tareas");
    }
}
