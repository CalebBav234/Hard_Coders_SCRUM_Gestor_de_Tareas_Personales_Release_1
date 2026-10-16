package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.application.port.in.QueryProgressUseCase;
import com.hardcoders.taskmanager.application.result.ProgressNodeResult;
import com.hardcoders.taskmanager.application.result.ProjectProgressResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US33-PC-01: el controlador de progreso en cascada traduce el árbol de nodos a su respuesta HTTP. */
class ProgressControllerTest {

    private QueryProgressUseCase progress;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        progress = mock(QueryProgressUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProgressController(progress)).build();
    }

    @Test
    @DisplayName("US33-PC-01: GET /api/projects/{id}/progress devuelve la cascada completa anidada")
    void progressMapsTheNestedTree() throws Exception {
        ProgressNodeResult grandchild = new ProgressNodeResult(31, "Subtarea", 1, 1, true, 1.0, List.of());
        ProgressNodeResult activity = new ProgressNodeResult(21, "Analisis", 1, 2, false, 0.5, List.of(grandchild));
        ProgressNodeResult stage = new ProgressNodeResult(11, "Etapa 1", 2, 2, true, 1.0, List.of());
        ProgressNodeResult root = new ProgressNodeResult(42, "Edificio A", 3, 5, false, 0.6,
                List.of(activity, stage));
        ProjectProgressResult result = new ProjectProgressResult(42, "Edificio A", "EN_EJECUCION",
                new BigDecimal("15000.00"), new BigDecimal("9000.00"), 600, root);
        when(progress.progress(42)).thenReturn(result);

        mockMvc.perform(get("/api/projects/42/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(42))
                .andExpect(jsonPath("$.projectStatus").value("EN_EJECUCION"))
                .andExpect(jsonPath("$.allocatedBudgetBob").value(9000.00))
                .andExpect(jsonPath("$.root.id").value(42))
                .andExpect(jsonPath("$.root.leaf").value(false))
                .andExpect(jsonPath("$.root.children[0].id").value(21))
                .andExpect(jsonPath("$.root.children[0].children[0].name").value("Subtarea"))
                .andExpect(jsonPath("$.root.children[1].leaf").value(true));

        verify(progress).progress(42);
    }
}
