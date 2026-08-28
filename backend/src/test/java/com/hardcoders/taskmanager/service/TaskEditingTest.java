package com.hardcoders.taskmanager.service;

import com.hardcoders.taskmanager.dto.UpdateTaskRequest;
import com.hardcoders.taskmanager.entity.Task;
import com.hardcoders.taskmanager.exception.OptimisticLockConflictException;
import com.hardcoders.taskmanager.exception.TaskHasSubtasksException;
import com.hardcoders.taskmanager.exception.TaskNotFoundException;
import com.hardcoders.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskEditingTest {
    @Mock TaskRepository repository;
    TaskService service;
    final Instant created = Instant.parse("2026-08-20T12:00:00Z");
    final Instant lifecycleTime = Instant.parse("2026-08-20T13:00:00Z");

    @BeforeEach
    void setUp() { service = new TaskService(repository); }

    private Task task(String status) {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Original");
        task.setDescription("Descripción original");
        task.setPriority("MEDIA");
        task.setStatus(status);
        task.setCreatedAt(created);
        task.setUpdatedAt(created);
        task.setActivatedAt("ACTIVA".equals(status) ? lifecycleTime : null);
        task.setCompletedAt("TERMINADA".equals(status) ? lifecycleTime : null);
        task.setTotalActiveSeconds(120L);
        task.setCategoryId(2L);
        task.setParentTaskId(3L);
        task.setVersion(4L);
        return task;
    }

    private UpdateTaskRequest request(long version) {
        return new UpdateTaskRequest("  Corregida  ", "Nueva descripción", "ALTA", version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVA", "ACTIVA", "TERMINADA"})
    void editingOnlyChangesAllowedFields(String status) {
        Task task = task(status);
        Instant activated = task.getActivatedAt();
        Instant completed = task.getCompletedAt();
        when(repository.findById(1L)).thenReturn(Optional.of(task));
        when(repository.findSummaryById(1L)).thenAnswer(invocation -> new Object[]{
                task.getId(), task.getTitle(), task.getDescription(), task.getStatus(), task.getPriority(),
                task.getCategoryId(), task.getParentTaskId(), task.getActivatedAt(), task.getCompletedAt(),
                task.getTotalActiveSeconds(), 120L, task.getCreatedAt(), task.getUpdatedAt(), 5L
        });

        var response = service.update(1L, request(4));

        assertThat(response.title()).isEqualTo("Corregida");
        assertThat(response.description()).isEqualTo("Nueva descripción");
        assertThat(response.priority()).isEqualTo("ALTA");
        assertThat(task.getStatus()).isEqualTo(status);
        assertThat(task.getCreatedAt()).isEqualTo(created);
        assertThat(task.getActivatedAt()).isEqualTo(activated);
        assertThat(task.getCompletedAt()).isEqualTo(completed);
        assertThat(task.getTotalActiveSeconds()).isEqualTo(120L);
        assertThat(task.getCategoryId()).isEqualTo(2L);
        assertThat(task.getParentTaskId()).isEqualTo(3L);
        assertThat(task.getUpdatedAt()).isAfter(created);
        assertThat(task.getDeletedAt()).isNull();
        verify(repository).save(task);
        verify(repository).flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVA", "ACTIVA", "TERMINADA"})
    void deletionIsLogicalAndKeepsHistoryData(String status) {
        Task task = task(status);
        Instant activated = task.getActivatedAt();
        Instant completed = task.getCompletedAt();
        when(repository.findById(1L)).thenReturn(Optional.of(task));

        service.delete(1L, 4L);

        assertThat(task.getDeletedAt()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(status);
        assertThat(task.getCreatedAt()).isEqualTo(created);
        assertThat(task.getActivatedAt()).isEqualTo(activated);
        assertThat(task.getCompletedAt()).isEqualTo(completed);
        assertThat(task.getTitle()).isEqualTo("Original");
        verify(repository).save(task);
        verify(repository).flush();
        verify(repository, never()).delete(any(Task.class));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void staleVersionCannotEditOrDelete() {
        Task task = task("INACTIVA");
        when(repository.findById(1L)).thenReturn(Optional.of(task));
        assertThatThrownBy(() -> service.update(1L, request(3))).isInstanceOf(OptimisticLockConflictException.class);
        assertThatThrownBy(() -> service.delete(1L, 3L)).isInstanceOf(OptimisticLockConflictException.class);
        assertThat(task.getTitle()).isEqualTo("Original");
        assertThat(task.getDeletedAt()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    void missingTaskCannotEditOrDelete() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, request(4))).isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L, 4L)).isInstanceOf(TaskNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deletedTasksCannotBeEditedDeletedActivatedOrCompleted() {
        Task task = task("ACTIVA");
        task.setDeletedAt(Instant.now());
        when(repository.findById(1L)).thenReturn(Optional.of(task));
        assertThatThrownBy(() -> service.update(1L, request(4))).isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L, 4L)).isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> service.activate(1L, 4L)).isInstanceOf(TaskNotFoundException.class);
        assertThatThrownBy(() -> service.complete(1L, 4L)).isInstanceOf(TaskNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deletionDoesNotLeaveVisibleSubtasksWithoutTheirParent() {
        Task task = task("INACTIVA");
        when(repository.findById(1L)).thenReturn(Optional.of(task));
        when(repository.existsByParentTaskIdAndDeletedAtIsNull(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 4L)).isInstanceOf(TaskHasSubtasksException.class);
        assertThat(task.getDeletedAt()).isNull();
        verify(repository, never()).save(any());
    }
}
