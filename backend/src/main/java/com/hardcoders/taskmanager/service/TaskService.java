package com.hardcoders.taskmanager.service;

import com.hardcoders.taskmanager.dto.TaskResponse;
import com.hardcoders.taskmanager.entity.Task;
import com.hardcoders.taskmanager.exception.InvalidTaskStateException;
import com.hardcoders.taskmanager.exception.OptimisticLockConflictException;
import com.hardcoders.taskmanager.exception.TaskNotFoundException;
import com.hardcoders.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse create(String title) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus("INACTIVA");
        task.setPriority("MEDIA");
        task.setTotalActiveSeconds(0L);
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        Task saved = taskRepository.save(task);
        taskRepository.flush();
        return toResponse(taskRepository.findSummaryById(saved.getId()));
    }

    public TaskResponse activate(Long taskId, Long expectedVersion) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        verifyVersion(taskId, expectedVersion, task.getVersion());
        if (!"INACTIVA".equals(task.getStatus())) {
            throw new InvalidTaskStateException(task.getStatus(), "ACTIVA");
        }
        task.setStatus("ACTIVA");
        task.setActivatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);
        taskRepository.flush();
        return toResponse(taskRepository.findSummaryById(taskId));
    }

    public TaskResponse complete(Long taskId, Long expectedVersion) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        verifyVersion(taskId, expectedVersion, task.getVersion());
        if (!"ACTIVA".equals(task.getStatus())) {
            throw new InvalidTaskStateException(task.getStatus(), "TERMINADA");
        }
        long elapsedSeconds = Instant.now().getEpochSecond() - task.getActivatedAt().getEpochSecond();
        task.setTotalActiveSeconds(task.getTotalActiveSeconds() + elapsedSeconds);
        task.setStatus("TERMINADA");
        task.setCompletedAt(Instant.now());
        task.setActivatedAt(null);
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);
        taskRepository.flush();
        return toResponse(taskRepository.findSummaryById(taskId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks() {
        return taskRepository.findSummaries().stream()
                .map(this::toResponse)
                .toList();
    }

    private void verifyVersion(Long taskId, Long expectedVersion, Long actualVersion) {
        if (expectedVersion != null && !expectedVersion.equals(actualVersion)) {
            throw new OptimisticLockConflictException(taskId, expectedVersion, actualVersion);
        }
    }

    private TaskResponse toResponse(Object[] row) {
        if (row.length == 1 && row[0] instanceof Object[] inner) {
            row = inner;
        }
        return new TaskResponse(
                toLong(row[0]),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                toLong(row[5]),
                toLong(row[6]),
                toInstant(row[7]),
                toInstant(row[8]),
                toLong(row[9]),
                toLong(row[10]),
                toInstant(row[11]),
                toInstant(row[12]),
                toLong(row[13])
        );
    }

    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalArgumentException("Tipo de timestamp no soportado: " + value.getClass().getName());
    }
}
