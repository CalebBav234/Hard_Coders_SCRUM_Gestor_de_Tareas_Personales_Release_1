package com.hardcoders.taskmanager.adapter.in.security;

import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** ACTOR-01: SecurityCurrentActorAdapter resuelve el Actor desde el JWT validado y app_user. */
class SecurityCurrentActorAdapterTest {

    private UserDirectory users;
    private SecurityCurrentActorAdapter adapter;

    @BeforeEach
    void setUp() {
        users = mock(UserDirectory.class);
        adapter = new SecurityCurrentActorAdapter(users);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ACTOR-01: resuelve el Actor a partir del subject del JWT y los datos de app_user")
    void resolvesActorFromJwtSubject() {
        SecurityContextHolder.getContext().setAuthentication(authentication(jwtWithSubject("7")));
        when(users.findById(7)).thenReturn(Optional.of(
                new UserAccount(7, "jefe@gestor.local", "Julia", "Mendoza", true, true)));

        Actor actor = adapter.current();

        assertThat(actor.userId()).isEqualTo(7);
        assertThat(actor.chief()).isTrue();
        assertThat(actor.active()).isTrue();
    }

    @Test
    @DisplayName("ACTOR-02: sin autenticación o con un principal que no es JWT se rechaza (403)")
    void missingAuthenticationIsForbidden() {
        assertThatThrownBy(adapter::current)
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.ForbiddenException.class)
                .hasMessageContaining("usuario autenticado");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan", "n/a"));
        assertThatThrownBy(adapter::current)
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.ForbiddenException.class)
                .hasMessageContaining("usuario autenticado");
    }

    @Test
    @DisplayName("ACTOR-03: un subject no numérico se rechaza (403)")
    void nonNumericSubjectIsForbidden() {
        SecurityContextHolder.getContext().setAuthentication(authentication(jwtWithSubject("abc")));

        assertThatThrownBy(adapter::current)
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.ForbiddenException.class)
                .hasMessageContaining("Token sin usuario válido");
    }

    @Test
    @DisplayName("ACTOR-04: si el usuario del token ya no existe se rechaza (403)")
    void missingUserIsForbidden() {
        SecurityContextHolder.getContext().setAuthentication(authentication(jwtWithSubject("7")));
        when(users.findById(7)).thenReturn(Optional.empty());

        assertThatThrownBy(adapter::current)
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.ForbiddenException.class)
                .hasMessageContaining("ya no existe");
    }

    private static Jwt jwtWithSubject(String subject) {
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.parse("2026-10-15T12:00:00Z"))
                .expiresAt(Instant.parse("2026-10-15T13:00:00Z"))
                .build();
    }

    private static JwtAuthenticationToken authentication(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, List.of());
    }
}
