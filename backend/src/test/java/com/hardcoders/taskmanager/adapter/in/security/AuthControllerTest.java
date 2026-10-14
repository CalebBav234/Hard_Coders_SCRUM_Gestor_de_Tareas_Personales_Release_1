package com.hardcoders.taskmanager.adapter.in.security;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.repository.AppUserJpaRepository;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-15 / P1.2 y P1.3 del plan de pruebas manuales: el inicio de sesion distingue credenciales
 * invalidas (401) de un usuario desactivado (403), para que la pantalla de login pueda explicar
 * cada caso. Nunca se emite token para un usuario inactivo.
 */
class AuthControllerTest {

    private static final String PASSWORD = "Gestor2026!";

    private AppUserJpaRepository appUsers;
    private UserDirectory users;
    private JwtTokenService tokens;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        appUsers = mock(AppUserJpaRepository.class);
        users = mock(UserDirectory.class);
        tokens = mock(JwtTokenService.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        CurrentActor currentActor = mock(CurrentActor.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(appUsers, encoder, tokens, users, currentActor))
                .build();
    }

    private AppUserEntity user(String email, boolean active) {
        AppUserEntity entity = new AppUserEntity();
        entity.setUserId(1);
        entity.setEmail(email);
        entity.setPasswordHash(new BCryptPasswordEncoder().encode(PASSWORD));
        entity.setGivenNames("Julia");
        entity.setFamilyNames("Mendoza");
        entity.setIsChief(true);
        entity.setIsActive(active);
        return entity;
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void wrongPasswordIsRejectedWithoutRevealingWhetherTheEmailExists() throws Exception {
        when(appUsers.findByEmail("jefe@gestor.local")).thenReturn(Optional.of(user("jefe@gestor.local", true)));

        login("jefe@gestor.local", "otraClave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos."));
    }

    @Test
    void unknownEmailGetsTheSameAnswerAsAWrongPassword() throws Exception {
        when(appUsers.findByEmail("nadie@gestor.local")).thenReturn(Optional.empty());

        login("nadie@gestor.local", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void inactiveUserWithTheRightPasswordGetsItsOwnMessageAndNoToken() throws Exception {
        when(appUsers.findByEmail("inactivo@gestor.local"))
                .thenReturn(Optional.of(user("inactivo@gestor.local", false)));

        login("inactivo@gestor.local", PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("USER_INACTIVE"))
                .andExpect(jsonPath("$.message").value("Tu usuario está inactivo. Contacta al administrador."));

        // Sin token: el usuario inactivo nunca entra.
        verifyNoInteractions(tokens);
    }

    @Test
    void activeUserGetsAToken() throws Exception {
        AppUserEntity entity = user("jefe@gestor.local", true);
        when(appUsers.findByEmail("jefe@gestor.local")).thenReturn(Optional.of(entity));
        when(users.findById(1)).thenReturn(Optional.of(
                new UserAccount(1, "jefe@gestor.local", "Julia", "Mendoza", true, true)));
        when(tokens.issue(any())).thenReturn(new JwtTokenService.IssuedToken("jwt-de-prueba",
                java.time.Instant.parse("2026-12-31T00:00:00Z")));

        login("JEFE@gestor.local", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-de-prueba"))
                .andExpect(jsonPath("$.user.chief").value(true));

        // El correo se normaliza a minusculas antes de buscar.
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(appUsers).findByEmail(emailCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("jefe@gestor.local", emailCaptor.getValue());
    }
}
