package com.hardcoders.taskmanager.adapter.in.security;

import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final JwtProperties PROPERTIES =
            new JwtProperties("test-secret-0123456789-0123456789-abcdef", 60, "gestor-tareas");
    private static final UserAccount CHIEF = new UserAccount(7, "jefe@x.com", "Julia", "Mendoza", true, true);

    @Test
    void issuesATokenThatTheDecoderAccepts() {
        SecretKey key = key(PROPERTIES.secret());
        JwtTokenService service = new JwtTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), PROPERTIES,
                Clock.systemUTC());

        JwtTokenService.IssuedToken token = service.issue(CHIEF);
        Jwt decoded = decoder(key).decode(token.value());

        assertThat(decoded.getSubject()).isEqualTo("7");
        assertThat(decoded.getClaimAsString("email")).isEqualTo("jefe@x.com");
        assertThat(decoded.getClaimAsBoolean("chief")).isTrue();
        assertThat(Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt())).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    void rejectsTokensSignedWithAnotherKeyOrExpired() {
        SecretKey key = key(PROPERTIES.secret());
        String foreign = new JwtTokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(key("otro-secreto-0123456789-0123456789-xyz"))),
                PROPERTIES, Clock.systemUTC()).issue(CHIEF).value();
        assertThatThrownBy(() -> decoder(key).decode(foreign)).isInstanceOf(JwtException.class);

        Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(3)), ZoneOffset.UTC);
        String expired = new JwtTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), PROPERTIES, past)
                .issue(CHIEF).value();
        assertThatThrownBy(() -> decoder(key).decode(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void refusesShortSecrets() {
        assertThatThrownBy(() -> new JwtProperties("corto", 60, "x")).isInstanceOf(IllegalStateException.class);
    }

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static NimbusJwtDecoder decoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("gestor-tareas"));
        return decoder;
    }
}
