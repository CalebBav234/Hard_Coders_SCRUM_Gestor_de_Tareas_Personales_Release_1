package com.hardcoders.taskmanager.adapter.in.web.error;

import com.hardcoders.taskmanager.domain.exception.BudgetBelowAllocationsException;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.exception.OpenSubtasksException;
import com.hardcoders.taskmanager.domain.exception.ProjectTransitionException;
import com.hardcoders.taskmanager.domain.exception.VersionConflictException;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US14-ERR-01: el GlobalExceptionHandler traduce cada excepción de dominio y de infraestructura a un
 * código HTTP y un cuerpo JSON estables. Se prueba aislado con un controlador de pruebas que lanza
 * cada excepción y MockMvc en modo standalone.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BoomController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("US14-ERR-01: DomainValidationException mapea a 400 VALIDATION_ERROR")
    void validationExceptionBecomesBadRequest() throws Exception {
        mockMvc.perform(get("/boom/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("El título es obligatorio"));
    }

    @Test
    @DisplayName("US14-ERR-02: ForbiddenException mapea a 403 FORBIDDEN")
    void forbiddenExceptionBecomesForbidden() throws Exception {
        mockMvc.perform(get("/boom/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Sin permiso"));
    }

    @Test
    @DisplayName("US14-ERR-03: NotFoundException mapea a 404 con código por entidad")
    void notFoundExceptionBecomesNotFoundWithEntityCode() throws Exception {
        mockMvc.perform(get("/boom/notfound/task"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TASK_NOT_FOUND"));
        mockMvc.perform(get("/boom/notfound/project"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PROJECT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Proyecto 5 no encontrada"));
        mockMvc.perform(get("/boom/notfound/subtask"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SUBTASK_NOT_FOUND"));
    }

    @Test
    @DisplayName("US14-ERR-04: VersionConflictException y bloqueo optimista mapean a 412 VERSION_CONFLICT")
    void versionConflictBecomesPreconditionFailed() throws Exception {
        mockMvc.perform(get("/boom/version"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.error").value("VERSION_CONFLICT"));
        mockMvc.perform(get("/boom/optimistic"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.error").value("VERSION_CONFLICT"));
    }

    @Test
    @DisplayName("US14-ERR-05: BudgetBelowAllocationsException mapea a 409")
    void budgetBelowAllocationsBecomesConflict() throws Exception {
        mockMvc.perform(get("/boom/budget"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BUDGET_BELOW_ALLOCATIONS"));
    }

    @Test
    @DisplayName("US14-ERR-06: InvalidStatusTransitionException mapea a 409 INVALID_STATE_TRANSITION")
    void invalidStatusTransitionBecomesConflict() throws Exception {
        mockMvc.perform(get("/boom/transition"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    @DisplayName("US14-ERR-07: OpenSubtasksException mapea a 409 TASK_HAS_SUBTASKS")
    void openSubtasksBecomesConflict() throws Exception {
        mockMvc.perform(get("/boom/opensubtasks"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TASK_HAS_SUBTASKS"));
    }

    @Test
    @DisplayName("US14-ERR-08: ProjectTransitionException mapea a 409 INVALID_PROJECT_TRANSITION")
    void projectTransitionBecomesConflict() throws Exception {
        mockMvc.perform(get("/boom/projecttransition"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_PROJECT_TRANSITION"));
    }

    @Test
    @DisplayName("US14-ERR-09: EntityNotFoundException da 404 y DataIntegrityViolationException da 409")
    void persistenceExceptionsMapToNotFoundAndConflict() throws Exception {
        mockMvc.perform(get("/boom/entitynotfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        mockMvc.perform(get("/boom/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DATA_INTEGRITY_VIOLATION"));
    }

    @Test
    @DisplayName("US14-ERR-10: una excepción inesperada mapea a 500 INTERNAL_ERROR")
    void genericExceptionBecomesInternalError() throws Exception {
        mockMvc.perform(get("/boom/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    /** Controlador de pruebas que lanza la excepción asociada a cada ruta. */
    @RestController
    @RequestMapping("/boom")
    static class BoomController {

        @GetMapping("/validation")
        void validation() {
            throw new DomainValidationException("El título es obligatorio");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException("Sin permiso");
        }

        @GetMapping("/notfound/task")
        void notFoundTask() {
            throw new NotFoundException("Tarea", 5);
        }

        @GetMapping("/notfound/project")
        void notFoundProject() {
            throw new NotFoundException("Proyecto", 5);
        }

        @GetMapping("/notfound/subtask")
        void notFoundSubtask() {
            throw new NotFoundException("Subtarea", 5);
        }

        @GetMapping("/version")
        void version() {
            throw new VersionConflictException("Tarea", 5);
        }

        @GetMapping("/optimistic")
        void optimistic() {
            throw new OptimisticLockingFailureException("conflicto");
        }

        @GetMapping("/budget")
        void budget() {
            throw new BudgetBelowAllocationsException(new BigDecimal("100.00"), new BigDecimal("250.00"));
        }

        @GetMapping("/transition")
        void transition() {
            throw new InvalidStatusTransitionException(WorkStatus.PENDIENTE, WorkStatus.COMPLETADA);
        }

        @GetMapping("/opensubtasks")
        void openSubtasks() {
            throw new OpenSubtasksException(9);
        }

        @GetMapping("/projecttransition")
        void projectTransition() {
            throw new ProjectTransitionException(ProjectStatus.PLANIFICADO, ProjectStatus.FINALIZADO);
        }

        @GetMapping("/entitynotfound")
        void entityNotFound() {
            throw new EntityNotFoundException("sin fila");
        }

        @GetMapping("/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("duplicado");
        }

        @GetMapping("/generic")
        void generic() {
            throw new IllegalStateException("algo se rompió");
        }
    }
}
