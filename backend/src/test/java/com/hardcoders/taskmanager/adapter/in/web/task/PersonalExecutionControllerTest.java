package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.adapter.in.web.error.GlobalExceptionHandler;
import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.port.in.ManageSubtasksUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryTasksUseCase;
import com.hardcoders.taskmanager.application.result.AssignedTaskResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PersonalExecutionControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    private QueryTasksUseCase queries;
    private ManageTasksUseCase tasks;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queries = mock(QueryTasksUseCase.class);
        tasks = mock(ManageTasksUseCase.class);
        ManageSubtasksUseCase subtasks = mock(ManageSubtasksUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(queries, tasks, subtasks))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void assignedTaskEndpointReturnsExecutionViewWithoutFinancialFields() throws Exception {
        when(queries.listAssignedTasks(null)).thenReturn(List.of(assignedTask()));

        mockMvc.perform(get("/api/tasks/assigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(31))
                .andExpect(jsonPath("$[0].trackedSeconds").value(45))
                .andExpect(jsonPath("$[0].allocatedBudgetBob").doesNotExist())
                .andExpect(jsonPath("$[0].estimatedDurationMinutes").doesNotExist());
    }

    @Test
    void startEndpointReturnsOkAndMapsTransitionCommand() throws Exception {
        when(tasks.start(any())).thenReturn(taskResult());

        mockMvc.perform(post("/api/tasks/31/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1000,\"reason\":\"Inicio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(31))
                .andExpect(jsonPath("$.status").value("EN_PROGRESO"));

        verify(tasks).start(new TransitionCommand(31, "Inicio", 1000L));
    }

    @Test
    void invalidJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tasks/31/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void forbiddenAndUnknownExecutionReturn403And404() throws Exception {
        doThrow(new ForbiddenException("No asignada")).when(tasks).start(any());
        mockMvc.perform(post("/api/tasks/31/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1000}"))
                .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        doThrow(new NotFoundException("Tarea", 31)).when(tasks).start(any());
        mockMvc.perform(post("/api/tasks/31/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1000}"))
                .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.error").value("TASK_NOT_FOUND"));
    }

    @Test
    void invalidStateTransitionReturnsConflict() throws Exception {
        when(tasks.start(any())).thenThrow(new InvalidStatusTransitionException(
                com.hardcoders.taskmanager.domain.model.WorkStatus.COMPLETADA,
                com.hardcoders.taskmanager.domain.model.WorkStatus.EN_PROGRESO));

        mockMvc.perform(post("/api/tasks/31/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1000}"))
                .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));
    }

    private static AssignedTaskResult assignedTask() {
        return new AssignedTaskResult(31, null, 21, "Actividad", "Tarea asignada", null,
                "EN_PROGRESO", "MEDIA", 1, 45, NOW.minusSeconds(45), null, NOW, NOW, 1000L, List.of());
    }

    private static TaskResult taskResult() {
        return new TaskResult(31, 21, "Actividad", "Tarea asignada", null, "EN_PROGRESO", "MEDIA",
                BigDecimal.ZERO, 60, null, 0, NOW, null, null, NOW, NOW, 1000L, List.of(), List.of());
    }
}
