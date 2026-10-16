package com.hardcoders.taskmanager.adapter.in.web.project;

import com.hardcoders.taskmanager.application.command.ReplanProjectCommand;
import com.hardcoders.taskmanager.application.command.UpdateProjectDetailsCommand;
import com.hardcoders.taskmanager.application.port.in.ManageProjectsUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryProjectsUseCase;
import com.hardcoders.taskmanager.application.result.PlanRevisionResult;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US11-PQ-01: endpoints de consulta y mutación de proyectos no cubiertos por la creación/delegación. */
class ProjectControllerQueriesTest {

    private ManageProjectsUseCase manageProjects;
    private QueryProjectsUseCase queries;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        manageProjects = mock(ManageProjectsUseCase.class);
        queries = mock(QueryProjectsUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(manageProjects, queries)).build();
    }

    @Test
    @DisplayName("US11-PQ-01: GET /api/projects lista los proyectos devolviendo 200")
    void listReturnsEveryProject() throws Exception {
        when(queries.listProjects()).thenReturn(List.of(result()));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(42))
                .andExpect(jsonPath("$[0].name").value("Edificio A"))
                .andExpect(jsonPath("$[0].manager.userId").value(2));
    }

    @Test
    @DisplayName("US11-PQ-02: GET /eligible-managers devuelve los candidatos a delegar")
    void eligibleManagersAreExposed() throws Exception {
        when(queries.eligibleManagers()).thenReturn(List.of(
                new UserSummaryResult(3, "Ana Ríos", "ana@x.com", false, true)));

        mockMvc.perform(get("/api/projects/eligible-managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(3))
                .andExpect(jsonPath("$[0].fullName").value("Ana Ríos"));
    }

    @Test
    @DisplayName("US11-PQ-03: GET /{projectId} traduce el id de ruta y devuelve el proyecto")
    void getByIdTranslatesPathVariable() throws Exception {
        when(queries.getProject(42)).thenReturn(result());

        mockMvc.perform(get("/api/projects/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(42))
                .andExpect(jsonPath("$.status").value("PLANIFICADO"));

        verify(queries).getProject(42);
    }

    @Test
    @DisplayName("US11-PQ-04: GET /{projectId}/plan-history devuelve las revisiones del plan")
    void planHistoryIsExposed() throws Exception {
        Instant now = Instant.parse("2026-10-14T12:00:00Z");
        when(queries.planHistory(42)).thenReturn(List.of(new PlanRevisionResult(2, new BigDecimal("15000.50"),
                600, now, new UserSummaryResult(1, "Jefe", "jefe@x.com", true, true), "Replan")));

        mockMvc.perform(get("/api/projects/42/plan-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revisionNumber").value(2))
                .andExpect(jsonPath("$[0].budgetBob").value(15000.50))
                .andExpect(jsonPath("$[0].changedBy.userId").value(1));

        verify(queries).planHistory(42);
    }

    @Test
    @DisplayName("US11-PQ-05: PUT /{projectId} actualiza nombre y descripción con el id y versión de ruta")
    void updateDetailsTranslatesRequest() throws Exception {
        when(manageProjects.updateDetails(any())).thenReturn(result());

        mockMvc.perform(put("/api/projects/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Edificio B\",\"description\":\"Remodelada\",\"version\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(42));

        ArgumentCaptor<UpdateProjectDetailsCommand> command = ArgumentCaptor.forClass(UpdateProjectDetailsCommand.class);
        verify(manageProjects).updateDetails(command.capture());
        assertThat(command.getValue().projectId()).isEqualTo(42);
        assertThat(command.getValue().name()).isEqualTo("Edificio B");
        assertThat(command.getValue().version()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("US11-PQ-06: PUT /{projectId}/plan replanifica presupuesto y duración")
    void replanTranslatesRequest() throws Exception {
        when(manageProjects.replan(any())).thenReturn(result());

        mockMvc.perform(put("/api/projects/42/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budgetBob\":20000.00,\"estimatedDurationMinutes\":720,"
                                + "\"reason\":\"Ajuste\",\"version\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(42));

        ArgumentCaptor<ReplanProjectCommand> command = ArgumentCaptor.forClass(ReplanProjectCommand.class);
        verify(manageProjects).replan(command.capture());
        assertThat(command.getValue().projectId()).isEqualTo(42);
        assertThat(command.getValue().budgetBob()).isEqualByComparingTo("20000.00");
        assertThat(command.getValue().estimatedDurationMinutes()).isEqualTo(720);
    }

    @Test
    @DisplayName("US11-PQ-07: PUT /{projectId}/status cambia el estado del proyecto")
    void changeStatusTranslatesRequest() throws Exception {
        when(manageProjects.changeStatus(any())).thenReturn(result());

        mockMvc.perform(put("/api/projects/42/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EN_EJECUCION\",\"version\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(42));

        ArgumentCaptor<com.hardcoders.taskmanager.application.command.ChangeProjectStatusCommand> command =
                ArgumentCaptor.forClass(com.hardcoders.taskmanager.application.command.ChangeProjectStatusCommand.class);
        verify(manageProjects).changeStatus(command.capture());
        assertThat(command.getValue().projectId()).isEqualTo(42);
        assertThat(command.getValue().status()).isEqualTo("EN_EJECUCION");
    }

    private static ProjectResult result() {
        Instant now = Instant.parse("2026-10-14T12:00:00Z");
        UserSummaryResult manager = new UserSummaryResult(2, "Encargado", "enc@x.com", false, true);
        UserSummaryResult chief = new UserSummaryResult(1, "Jefe", "jefe@x.com", true, true);
        return new ProjectResult(42, "Edificio A", "Obra", "PLANIFICADO", false,
                new BigDecimal("15000.50"), BigDecimal.ZERO.setScale(2), 600, manager, chief, 1,
                now, now, 1000L, true, true, true, true, List.of("EN_EJECUCION", "CANCELADO"));
    }
}
