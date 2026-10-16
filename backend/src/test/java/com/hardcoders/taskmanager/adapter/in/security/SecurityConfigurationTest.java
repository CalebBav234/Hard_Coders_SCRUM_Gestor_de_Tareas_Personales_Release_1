package com.hardcoders.taskmanager.adapter.in.security;

import com.hardcoders.taskmanager.domain.model.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-BEAN-01: los beans de {@link SecurityConfiguration} (clave de firma, encoder/decoder JWT con
 * ida y vuelta, token service, codificador de contraseñas y configuración CORS) se verifican de forma
 * aislada. El {@code securityFilterChain} necesita un contexto web y se cubre aparte.
 */
class SecurityConfigurationTest {

    private final SecurityConfiguration config = new SecurityConfiguration();
    private final JwtProperties properties =
            new JwtProperties("test-secret-0123456789-0123456789-abcdef", 60, "gestor-tareas");

    @Test
    @DisplayName("SEC-BEAN-01: jwtSigningKey deriva una clave HmacSHA256 a partir del secreto")
    void signingKeyUsesHmacSha256() {
        SecretKey key = config.jwtSigningKey(properties);

        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(key.getEncoded()).isEqualTo(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("SEC-BEAN-02: encoder y decoder JWT realizan una ida y vuelta válida del token")
    void jwtEncoderAndDecoderRoundTrip() {
        SecretKey key = config.jwtSigningKey(properties);
        JwtEncoder encoder = config.jwtEncoder(key);
        JwtDecoder decoder = config.jwtDecoder(key, properties);
        JwtTokenService tokens = config.jwtTokenService(encoder, properties, Clock.systemUTC());

        String token = tokens.issue(new UserAccount(7, "jefe@x.com", "Julia", "Mendoza", true, true)).value();
        // decode lanza si la firma o el emisor no coinciden: que no falle ya prueba la ida y vuelta.
        Jwt decoded = decoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo("7");
        assertThat(decoded.getClaimAsString("email")).isEqualTo("jefe@x.com");
        assertThat(decoded.getClaimAsBoolean("chief")).isTrue();
        assertThat(decoded.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("SEC-BEAN-03: passwordEncoder codifica y verifica de forma coherente")
    void passwordEncoderEncodesAndMatches() {
        PasswordEncoder encoder = config.passwordEncoder();

        String hash = encoder.encode("Gestor2026!");
        assertThat(hash).isNotEqualTo("Gestor2026!");
        assertThat(encoder.matches("Gestor2026!", hash)).isTrue();
        assertThat(encoder.matches("otraClave", hash)).isFalse();
    }

    @Test
    @DisplayName("SEC-BEAN-04: corsConfigurationSource permite los orígenes del frontend solo bajo /api/**")
    void corsConfigurationAllowsFrontendOrigins() {
        CorsConfigurationSource source = config.corsConfigurationSource();

        CorsConfiguration cors = source.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/projects"));
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins())
                .contains("http://localhost:4200", "http://localhost:4873");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).contains("*");

        assertThat(source.getCorsConfiguration(new MockHttpServletRequest("GET", "/recurso")))
                .isNull();
    }
}
