package com.hardcoders.taskmanager.service;

import com.hardcoders.taskmanager.dto.TaskResponse;
import com.hardcoders.taskmanager.entity.Task;
import com.hardcoders.taskmanager.exception.InvalidTaskStateException;
import com.hardcoders.taskmanager.exception.OptimisticLockConflictException;
import com.hardcoders.taskmanager.exception.TaskNotFoundException;
import com.hardcoders.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-level regression coverage for TaskService.
 *
 * The service maps the {@code task_manager.v_tasks} view via native queries whose
 * columns arrive as {@link java.time.Instant} (not {@code java.sql.Timestamp}) on
 * modern PgJDBC drivers. The create/list/activate/complete responses therefore
 * must tolerate Instant-typed timestamps (see the ClassCastException fixed in
 * TaskService.toInstant). These tests use a mocked repository so no database is required.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
    }

    // Column order matches TaskRepository.SUMMARY_COLUMNS (v_tasks).
    private static Object[] row(Long id, String title, String status, String priority,
                                 Long totalActiveSeconds, Long effectiveActiveSeconds,
                                 Instant activatedAt, Instant completedAt, Long version) {
        Instant now = Instant.parse("2026-08-26T18:00:00Z");
        return new Object[]{
                id,                         // 0  id
                title,                      // 1  title
                null,                       // 2  description
                status,                     // 3  status
                priority,                   // 4  priority
                null,                       // 5  category_id
                null,                       // 6  parent_task_id
                activatedAt,                // 7  activated_at (Instant on PgJDBC)
                completedAt,                // 8  completed_at (Instant on PgJDBC)
                totalActiveSeconds,         // 9  total_active_seconds
                effectiveActiveSeconds,     // 10 effective_active_seconds
                now,                        // 11 created_at
                now,                        // 12 updated_at
                version                     // 13 version
        };
    }

    @Test
    void create_mapsViewRowWithInstantTimestamps() {
        Instant now = Instant.parse("2026-08-26T18:00:00Z");
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Mi primera tarea", "INACTIVA", "MEDIA", 0L, 0L, null, null, 0L));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TaskResponse response = taskService.create("Mi primera tarea");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Mi primera tarea");
        assertThat(response.status()).isEqualTo("INACTIVA");
        assertThat(response.priority()).isEqualTo("MEDIA");
        assertThat(response.activatedAt()).isNull();
        assertThat(response.completedAt()).isNull();
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.version()).isZero();
    }

    @Test
    void activate_transitionsToActivaAndBumpsVersion() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Tarea");
        task.setStatus("INACTIVA");
        task.setPriority("MEDIA");
        task.setVersion(0L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Tarea", "ACTIVA", "MEDIA", 0L, 0L,
                        Instant.parse("2026-08-26T18:00:00Z"), null, 1L));

        TaskResponse response = taskService.activate(1L, 0L);

        assertThat(response.status()).isEqualTo("ACTIVA");
        assertThat(response.activatedAt()).isNotNull();
        assertThat(response.version()).isEqualTo(1L);
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVA");
        assertThat(captor.getValue().getActivatedAt()).isNotNull();
    }

    @Test
    void activate_whenAlreadyActiva_throwsInvalidState() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("ACTIVA");
        task.setVersion(0L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.activate(1L, 0L))
                .isInstanceOf(InvalidTaskStateException.class);
    }

    @Test
    void activate_whenVersionMismatch_throwsOptimisticLock() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("INACTIVA");
        task.setVersion(0L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.activate(1L, 99L))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    @Test
    void activate_whenMissing_throwsNotFound() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.activate(42L, 0L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void complete_transitionsToTerminadaAndClearsActiveAt() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("ACTIVA");
        task.setPriority("MEDIA");
        task.setTotalActiveSeconds(5L);
        task.setActivatedAt(Instant.parse("2026-08-26T17:59:58Z"));
        task.setVersion(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Tarea", "TERMINADA", "MEDIA", 7L, 7L,
                        null, Instant.parse("2026-08-26T18:00:00Z"), 2L));

        TaskResponse response = taskService.complete(1L, 1L);

        assertThat(response.status()).isEqualTo("TERMINADA");
        assertThat(response.completedAt()).isNotNull();
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("TERMINADA");
        assertThat(saved.getActivatedAt()).isNull();
        assertThat(saved.getCompletedAt()).isNotNull();
    }

    @Test
    void complete_whenInactivo_throwsInvalidState() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("INACTIVA");
        task.setVersion(0L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.complete(1L, 0L))
                .isInstanceOf(InvalidTaskStateException.class);
    }

    @Test
    void complete_whenMissing_throwsNotFound() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.complete(42L, 0L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void listTasks_mapsEveryRowWithoutThrowing() {
        when(taskRepository.findSummaries())
                .thenReturn(List.of(
                        row(1L, "Tarea A", "INACTIVA", "ALTA", 0L, 0L, null, null, 0L),
                        row(2L, "Tarea B", "TERMINADA", "BAJA", 120L, 120L,
                                null, Instant.parse("2026-08-26T18:00:00Z"), 3L)));

        List<TaskResponse> responses = taskService.listTasks();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).title()).isEqualTo("Tarea A");
        assertThat(responses.get(1).status()).isEqualTo("TERMINADA");
        assertThat(responses.get(1).completedAt()).isNotNull();
    }
}
