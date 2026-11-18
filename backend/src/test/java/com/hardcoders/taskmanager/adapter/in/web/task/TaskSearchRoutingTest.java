package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.adapter.in.web.error.GlobalExceptionHandler;
import com.hardcoders.taskmanager.application.port.in.ManageSubtasksUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryTasksUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/tasks: con projectId filtra por proyecto (aunque haya q); solo q es búsqueda global (R1). */
class TaskSearchRoutingTest {

    private QueryTasksUseCase queries;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queries = mock(QueryTasksUseCase.class);
        when(queries.listTasks(7, "informe")).thenReturn(List.of());
        when(queries.search("informe")).thenReturn(List.of());
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(queries,
                        mock(ManageTasksUseCase.class), mock(ManageSubtasksUseCase.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void projectIdWithQueryUsesProjectScopedList() throws Exception {
        mockMvc.perform(get("/api/tasks").param("projectId", "7").param("q", "informe"))
                .andExpect(status().isOk());
        verify(queries).listTasks(7, "informe");
        verify(queries, never()).search(anyString());
    }

    @Test
    void queryWithoutProjectIdUsesGlobalSearch() throws Exception {
        mockMvc.perform(get("/api/tasks").param("q", "informe"))
                .andExpect(status().isOk());
        verify(queries).search("informe");
    }
}
