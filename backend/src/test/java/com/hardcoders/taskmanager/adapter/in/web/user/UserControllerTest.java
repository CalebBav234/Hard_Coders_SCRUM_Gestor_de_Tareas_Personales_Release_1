package com.hardcoders.taskmanager.adapter.in.web.user;

import com.hardcoders.taskmanager.application.command.CreateUserCommand;
import com.hardcoders.taskmanager.application.port.in.ManageUsersUseCase;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.adapter.in.web.error.GlobalExceptionHandler;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private ManageUsersUseCase manageUsers;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        manageUsers = mock(ManageUsersUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(manageUsers))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201WithoutSensitiveFieldsAndStripsNames() throws Exception {
        when(manageUsers.create(any())).thenReturn(
                new UserSummaryResult(55, "Ana Pérez", "ana@gestor.local", false, true));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ana@gestor.local",
                                  "givenNames": "  Ana  ",
                                  "familyNames": "  Pérez  ",
                                  "password": "Gestor2026!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(55))
                .andExpect(jsonPath("$.email").value("ana@gestor.local"))
                .andExpect(jsonPath("$.givenNames").value("Ana"))
                .andExpect(jsonPath("$.familyNames").value("Pérez"))
                .andExpect(jsonPath("$.chief").value(false))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        ArgumentCaptor<CreateUserCommand> captor = ArgumentCaptor.forClass(CreateUserCommand.class);
        verify(manageUsers).create(captor.capture());
        assertThat(captor.getValue().givenNames()).isEqualTo("Ana");
        assertThat(captor.getValue().familyNames()).isEqualTo("Pérez");
        assertThat(captor.getValue().rawPassword()).isEqualTo("Gestor2026!");
    }

    @Test
    void invalidRequestIsRejectedBeforeUseCase() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"givenNames\":\"\",\"familyNames\":\"\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(manageUsers);
    }

    @Test
    void forbiddenFromUseCaseIsPropagated() throws Exception {
        when(manageUsers.create(any())).thenThrow(new ForbiddenException("Solo el Jefe puede crear usuarios"));
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"givenNames\":\"Ana\",\"familyNames\":\"Paz\",\"password\":\"Gestor2026!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationFromUseCaseIsPropagated() throws Exception {
        when(manageUsers.create(any())).thenThrow(new DomainValidationException("El correo ya está registrado"));
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"givenNames\":\"Ana\",\"familyNames\":\"Paz\",\"password\":\"Gestor2026!\"}"))
                .andExpect(status().isBadRequest());
    }
}
