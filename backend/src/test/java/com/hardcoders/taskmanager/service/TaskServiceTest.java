package com.hardcoders.taskmanager.service;

import com.hardcoders.taskmanager.dto.TaskResponse;
import com.hardcoders.taskmanager.dto.TaskHistoryResponse;
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
import static org.mockito.Mockito.never;
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

    @Mock
    CategoryService categoryService;

    TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, categoryService);
    }

    // Column order matches TaskRepository.SUMMARY_COLUMNS (v_tasks).
    private static Object[] row(Long id, String title, String status, String priority,
                                 String categoryName,
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
                categoryName,               // 6  category_name
                null,                       // 7  parent_task_id
                activatedAt,                // 8  activated_at (Instant on PgJDBC)
                completedAt,                // 9  completed_at (Instant on PgJDBC)
                totalActiveSeconds,         // 10 total_active_seconds
                effectiveActiveSeconds,     // 11 effective_active_seconds
                now,                        // 12 created_at
                now,                        // 13 updated_at
                version                     // 14 version
        };
    }

    private static Object[] archiveRow(Long id, String title, String description,
                                       String status, Instant completedAt, Instant deletedAt) {
        Instant now = Instant.parse("2026-08-26T18:00:00Z");
        return new Object[]{
                id, title, description, status, "MEDIA", null, null, null,
                null, completedAt, 120L, 120L, now, now, deletedAt, 3L
        };
    }

    @Test
    void create_mapsViewRowWithInstantTimestamps() {
        Instant now = Instant.parse("2026-08-26T18:00:00Z");
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Mi primera tarea", "INACTIVA", "MEDIA", null, 0L, 0L, null, null, 0L));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TaskResponse response = taskService.create("Mi primera tarea", "MEDIA", null, null);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Mi primera tarea");
        assertThat(response.status()).isEqualTo("INACTIVA");
        assertThat(response.priority()).isEqualTo("MEDIA");
        assertThat(response.categoryName()).isNull();
        assertThat(response.activatedAt()).isNull();
        assertThat(response.completedAt()).isNull();
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.version()).isZero();
    }

    @Test
    void create_persistsProvidedPriorityAndCategory() {
        when(categoryService.findOrCreateVisibleByName("Trabajo")).thenReturn(5L);
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Con categoría", "INACTIVA", "ALTA", "Trabajo", 0L, 0L, null, null, 0L));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TaskResponse response = taskService.create("Con categoría", "ALTA", "Trabajo", null);

        assertThat(response.priority()).isEqualTo("ALTA");
        assertThat(response.categoryName()).isEqualTo("Trabajo");
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo("ALTA");
        assertThat(captor.getValue().getCategoryId()).isEqualTo(5L);
    }

    @Test
    void create_defaultsPriorityToMediaWhenNotProvided() {
        when(categoryService.findOrCreateVisibleByName(null)).thenReturn(null);
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Sin prioridad", "INACTIVA", "MEDIA", null, 0L, 0L, null, null, 0L));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TaskResponse response = taskService.create("Sin prioridad", null, null,null);

        assertThat(response.priority()).isEqualTo("MEDIA");
        assertThat(response.categoryName()).isNull();
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo("MEDIA");
        assertThat(captor.getValue().getCategoryId()).isNull();
    }

    @Test
    void create_blankCategoryNameLeavesCategoryAsSinCategoria() {
        when(categoryService.findOrCreateVisibleByName("   ")).thenReturn(null);
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Sin categoría", "INACTIVA", "MEDIA", null, 0L, 0L, null, null, 0L));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TaskResponse response = taskService.create("Sin categoría", "MEDIA", "   ",null);

        assertThat(response.categoryName()).isNull();
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoryId()).isNull();
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
                .thenReturn(row(1L, "Tarea", "ACTIVA", "MEDIA", null, 0L, 0L,
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
                .thenReturn(row(1L, "Tarea", "TERMINADA", "MEDIA", null, 7L, 7L,
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
                        row(1L, "Tarea A", "INACTIVA", "ALTA", null, 0L, 0L, null, null, 0L),
                        row(2L, "Tarea B", "TERMINADA", "BAJA", null, 120L, 120L,
                                null, Instant.parse("2026-08-26T18:00:00Z"), 3L)));

        List<TaskResponse> responses = taskService.listTasks();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).title()).isEqualTo("Tarea A");
        assertThat(responses.get(1).status()).isEqualTo("TERMINADA");
        assertThat(responses.get(1).completedAt()).isNotNull();
    }

    @Test
    void listTasks_searchesTitleOrDescriptionUsingSafeNormalizedPattern() {
        when(taskRepository.findSummariesBySearchPattern("%reunion\\_100\\%%"))
                .thenReturn(List.<Object[]>of(row(
                        1L, "Reunión_100%", "INACTIVA", "ALTA",
                        null, 0L, 0L, null, null, 0L)));

        List<TaskResponse> responses = taskService.listTasks("  REUNIÓN_100%  ");

        assertThat(responses).extracting(TaskResponse::title).containsExactly("Reunión_100%");
        verify(taskRepository).findSummariesBySearchPattern("%reunion\\_100\\%%");
        verify(taskRepository, never()).findSummaries();
    }

    @Test
    void listTasks_blankSearchKeepsCompleteList() {
        when(taskRepository.findSummaries()).thenReturn(List.of());

        assertThat(taskService.listTasks("   ")).isEmpty();

        verify(taskRepository).findSummaries();
        verify(taskRepository, never()).findSummariesBySearchPattern(any());
    }

    @Test
    void listHistory_unifiesArchivedTasksWithTheirStateEvents() {
        Instant completedAt = Instant.parse("2026-08-26T18:00:00Z");
        when(taskRepository.findArchivedSummariesBySearchPattern("%agenda%"))
                .thenReturn(List.<Object[]>of(archiveRow(
                        7L, "Preparar reunión", "Agenda semanal", "TERMINADA",
                        completedAt, null)));
        when(taskRepository.findHistoryEventsByTaskIds(List.of(7L)))
                .thenReturn(List.of(
                        new Object[]{2L, 7L, "ACTIVA", "TERMINADA", null, completedAt},
                        new Object[]{1L, 7L, "INACTIVA", "ACTIVA", null,
                                Instant.parse("2026-08-26T17:00:00Z")}));

        List<TaskHistoryResponse> responses = taskService.listHistory("agenda");

        assertThat(responses).hasSize(1);
        TaskHistoryResponse archived = responses.getFirst();
        assertThat(archived.title()).isEqualTo("Preparar reunión");
        assertThat(archived.deletedAt()).isNull();
        assertThat(archived.events()).hasSize(2);
        assertThat(archived.events().getFirst().toStatus()).isEqualTo("TERMINADA");
    }

    @Test
    void listHistory_withoutMatchesDoesNotQueryEvents() {
        when(taskRepository.findArchivedSummaries()).thenReturn(List.of());

        assertThat(taskService.listHistory("")).isEmpty();

        verify(taskRepository, never()).findHistoryEventsByTaskIds(any());
    }

    @Test
    void reopen_preservesAccumulatedTimeAndStartsNewActiveSegment() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Tarea");
        task.setStatus("TERMINADA");
        task.setPriority("MEDIA");
        task.setTotalActiveSeconds(60L);
        task.setCompletedAt(Instant.parse("2026-08-26T17:59:00Z"));
        task.setActivatedAt(null);
        task.setVersion(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Tarea", "ACTIVA", "MEDIA", "Trabajo", 60L, 60L,
                        Instant.parse("2026-08-26T18:00:00Z"), null, 2L));

        TaskResponse response = taskService.reopen(1L, 1L);

        assertThat(response.status()).isEqualTo("ACTIVA");
        assertThat(response.completedAt()).isNull();
        assertThat(response.activatedAt()).isNotNull();
        assertThat(response.totalActiveSeconds()).isEqualTo(60L);
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("ACTIVA");
        assertThat(saved.getCompletedAt()).isNull();
        assertThat(saved.getActivatedAt()).isNotNull();
        assertThat(saved.getTotalActiveSeconds()).isEqualTo(60L);
    }

    @Test
    void reopen_whenAlreadyActiva_throwsInvalidState() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("ACTIVA");
        task.setVersion(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.reopen(1L, 1L))
                .isInstanceOf(InvalidTaskStateException.class);
    }

    @Test
    void reopen_whenVersionMismatch_throwsOptimisticLock() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("TERMINADA");
        task.setVersion(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.reopen(1L, 99L))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    @Test
    void reopen_whenMissing_throwsNotFound() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.reopen(42L, 0L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void pause_accumulatesElapsedTimeClearsActivatedAtAndKeepsCompletedNull() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Tarea");
        task.setStatus("ACTIVA");
        task.setPriority("MEDIA");
        task.setTotalActiveSeconds(10L);
        task.setActivatedAt(Instant.parse("2026-08-26T17:59:58Z"));
        task.setCompletedAt(null);
        task.setVersion(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Tarea", "INACTIVA", "MEDIA", "Trabajo", 30L, 30L, null, null, 2L));

        TaskResponse response = taskService.pause(1L, 1L);

        assertThat(response.status()).isEqualTo("INACTIVA");
        assertThat(response.activatedAt()).isNull();
        assertThat(response.totalActiveSeconds()).isEqualTo(30L);
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("INACTIVA");
        assertThat(saved.getActivatedAt()).isNull();
        assertThat(saved.getCompletedAt()).isNull();
        assertThat(saved.getTotalActiveSeconds()).isGreaterThan(10L);
    }

    @Test
    void pause_whenNotActiva_throwsInvalidState() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("INACTIVA");
        task.setVersion(0L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.pause(1L, 0L))
                .isInstanceOf(InvalidTaskStateException.class);
    }

    @Test
    void pause_whenVersionMismatch_throwsOptimisticLock() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus("ACTIVA");
        task.setActivatedAt(Instant.parse("2026-08-26T17:59:58Z"));
        task.setVersion(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.pause(1L, 99L))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    @Test
    void pause_whenMissing_throwsNotFound() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.pause(42L, 0L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void changeCategory_assignsFoundOrCreateCategoryAndReturnsName() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Tarea");
        task.setCategoryId(null);
        task.setVersion(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(categoryService.findOrCreateVisibleByName("Nuevo")).thenReturn(9L);
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Tarea", "INACTIVA", "MEDIA", "Nuevo", 0L, 0L, null, null, 2L));

        TaskResponse response = taskService.changeCategory(1L, "Nuevo", 1L);

        assertThat(response.categoryName()).isEqualTo("Nuevo");
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoryId()).isEqualTo(9L);
    }

    @Test
    void changeCategory_withNull_clearsCategory() {
        Task task = new Task();
        task.setId(1L);
        task.setCategoryId(5L);
        task.setVersion(1L);
        when(categoryService.findOrCreateVisibleByName(null)).thenReturn(null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Tarea", "INACTIVA", "MEDIA", null, 0L, 0L, null, null, 2L));

        TaskResponse response = taskService.changeCategory(1L, null, 1L);

        assertThat(task.getCategoryId()).isNull();
        assertThat(response.categoryName()).isNull();
    }

    @Test
    void changeCategory_blankName_clearsCategory() {
        Task task = new Task();
        task.setId(1L);
        task.setCategoryId(5L);
        task.setVersion(1L);
        when(categoryService.findOrCreateVisibleByName("   ")).thenReturn(null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.findSummaryById(1L))
                .thenReturn(row(1L, "Tarea", "INACTIVA", "MEDIA", null, 0L, 0L, null, null, 2L));

        TaskResponse response = taskService.changeCategory(1L, "   ", 1L);

        assertThat(task.getCategoryId()).isNull();
        assertThat(response.categoryName()).isNull();
    }

    @Test
    void changeCategory_whenVersionMismatch_throwsOptimisticLock() {
        Task task = new Task();
        task.setId(1L);
        task.setCategoryId(null);
        task.setVersion(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.changeCategory(1L, "Nuevo", 99L))
                .isInstanceOf(OptimisticLockConflictException.class);
        verify(taskRepository, never()).save(any());
        verify(categoryService, never()).findOrCreateVisibleByName(any());
    }
}
