package com.hardcoders.taskmanager.adapter.in.web.project;

import com.hardcoders.taskmanager.application.command.CreateProjectCommand;
import com.hardcoders.taskmanager.application.command.DelegateProjectCommand;
import com.hardcoders.taskmanager.application.port.in.ManageProjectsUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryProjectsUseCase;
import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Adaptador HTTP aislado con el puerto de entrada simulado. */
class ProjectControllerTest {

    private ManageProjectsUseCase manageProjects;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        manageProjects = mock(ManageProjectsUseCase.class);
        QueryProjectsUseCase queries = mock(QueryProjectsUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(manageProjects, queries)).build();
    }

    @Test
    @DisplayName("US11-PC-01: POST /api/projects traduce JSON, devuelve 201 y resultado")
    void createTranslatesRequestAndReturnsCreated() throws Exception {
        when(manageProjects.create(any())).thenReturn(result(2));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Edificio A",
                                  "description": "Obra",
                                  "managerUserId": 2,
                                  "budgetBob": 15000.50,
                                  "estimatedDurationMinutes": 600,
                                  "reason": "Plan inicial"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(42))
                .andExpect(jsonPath("$.manager.userId").value(2));

        ArgumentCaptor<CreateProjectCommand> command = ArgumentCaptor.forClass(CreateProjectCommand.class);
        verify(manageProjects).create(command.capture());
        assertThat(command.getValue().budgetBob()).isEqualByComparingTo("15000.50");
    }

    @Test
    @DisplayName("US11-PC-02: JSON inválido se rechaza antes del caso de uso")
    void createRejectsInvalidRequestBeforeCallingUseCase() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"budgetBob\":-1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(manageProjects);
    }

    @Test
    @DisplayName("US12-PC-01: POST /delegate traduce el id de ruta y devuelve 200")
    void delegateTranslatesRequestAndReturnsOk() throws Exception {
        when(manageProjects.delegate(any())).thenReturn(result(3));

        mockMvc.perform(post("/api/projects/42/delegate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerUserId\":3,\"reason\":\"Reasignación\",\"version\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manager.userId").value(3));

        ArgumentCaptor<DelegateProjectCommand> command = ArgumentCaptor.forClass(DelegateProjectCommand.class);
        verify(manageProjects).delegate(command.capture());
        assertThat(command.getValue().projectId()).isEqualTo(42);
        assertThat(command.getValue().managerUserId()).isEqualTo(3);
    }

    private static ProjectResult result(int managerId) {
        Instant now = Instant.parse("2026-10-14T12:00:00Z");
        UserSummaryResult manager = new UserSummaryResult(managerId, "Encargado", "enc@x.com", false, true);
        UserSummaryResult chief = new UserSummaryResult(1, "Jefe", "jefe@x.com", true, true);
        return new ProjectResult(42, "Edificio A", "Obra", "PLANIFICADO", false,
                new BigDecimal("15000.50"), BigDecimal.ZERO.setScale(2), 600, manager, chief, 1,
                now, now, 1000L, true, true, true, true, List.of("EN_EJECUCION", "CANCELADO"));
    }
}
