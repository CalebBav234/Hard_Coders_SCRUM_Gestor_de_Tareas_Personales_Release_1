package com.hardcoders.taskmanager.service;

import com.hardcoders.taskmanager.dto.TaskHistoryResponse;
import com.hardcoders.taskmanager.dto.TaskResponse;
import com.hardcoders.taskmanager.dto.TaskStatusHistoryResponse;
import com.hardcoders.taskmanager.dto.UpdateTaskRequest;
import com.hardcoders.taskmanager.entity.Task;
import com.hardcoders.taskmanager.exception.InvalidParentTaskException;
import com.hardcoders.taskmanager.exception.InvalidTaskStateException;
import com.hardcoders.taskmanager.exception.OptimisticLockConflictException;
import com.hardcoders.taskmanager.exception.TaskNotFoundException;
import com.hardcoders.taskmanager.exception.TaskHasSubtasksException;
import com.hardcoders.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryService categoryService;

    public TaskService(TaskRepository taskRepository, CategoryService categoryService) {
        this.taskRepository = taskRepository;
        this.categoryService = categoryService;
    }

    public TaskResponse create(String title, String priority, String categoryName, Long parentTaskId) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus("INACTIVA");
        task.setPriority(priority != null && !priority.isBlank() ? priority : "MEDIA");
        task.setCategoryId(categoryService.findOrCreateVisibleByName(categoryName));
        task.setTotalActiveSeconds(0L);
        if (parentTaskId != null) {
            Task parentTask = findVisibleTask(parentTaskId);
            if (parentTask.getParentTaskId() != null) {
                throw new InvalidParentTaskException(parentTaskId);
            }
            task.setParentTaskId(parentTaskId);
        }
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        Task saved = taskRepository.save(task);
        taskRepository.flush();
        return toResponse(taskRepository.findSummaryById(saved.getId()));
    }

    public TaskResponse activate(Long taskId, Long expectedVersion) {
        Task task = findVisibleTask(taskId);
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
        Task task = findVisibleTask(taskId);
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

    public TaskResponse reopen(Long taskId, Long expectedVersion) {
        Task task = findVisibleTask(taskId);
        verifyVersion(taskId, expectedVersion, task.getVersion());
        if (!"TERMINADA".equals(task.getStatus())) {
            throw new InvalidTaskStateException(task.getStatus(), "ACTIVA");
        }
        Instant now = Instant.now();
        task.setStatus("ACTIVA");
        task.setActivatedAt(now);
        task.setCompletedAt(null);
        task.setUpdatedAt(now);
        taskRepository.save(task);
        taskRepository.flush();
        return toResponse(taskRepository.findSummaryById(taskId));
    }

    public TaskResponse pause(Long taskId, Long expectedVersion) {
        Task task = findVisibleTask(taskId);
        verifyVersion(taskId, expectedVersion, task.getVersion());
        if (!"ACTIVA".equals(task.getStatus())) {
            throw new InvalidTaskStateException(task.getStatus(), "INACTIVA");
        }
        long elapsedSeconds = Instant.now().getEpochSecond() - task.getActivatedAt().getEpochSecond();
        task.setTotalActiveSeconds(task.getTotalActiveSeconds() + elapsedSeconds);
        task.setActivatedAt(null);
        task.setStatus("INACTIVA");
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);
        taskRepository.flush();
        return toResponse(taskRepository.findSummaryById(taskId));
    }

    public TaskResponse update(Long taskId, UpdateTaskRequest request) {
        Task task = findVisibleTask(taskId);
        verifyVersion(taskId, request.version(), task.getVersion());
        task.setTitle(request.title().strip());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);
        taskRepository.flush();
        return toResponse(taskRepository.findSummaryById(taskId));
    }

    public TaskResponse changeCategory(Long taskId, String categoryName, Long expectedVersion) {
        Task task = findVisibleTask(taskId);
        verifyVersion(taskId, expectedVersion, task.getVersion());
        task.setCategoryId(categoryService.findOrCreateVisibleByName(categoryName));
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);
        taskRepository.flush();
        return toResponse(taskRepository.findSummaryById(taskId));
    }

    public void delete(Long taskId, Long expectedVersion) {
        Task task = findVisibleTask(taskId);
        verifyVersion(taskId, expectedVersion, task.getVersion());
        if (taskRepository.existsByParentTaskIdAndDeletedAtIsNull(taskId)) {
            throw new TaskHasSubtasksException();
        }
        // Preserve the task and its history. v_tasks already excludes deleted_at IS NOT NULL.
        Instant now = Instant.now();
        task.setDeletedAt(now);
        task.setUpdatedAt(now);
        taskRepository.save(task);
        taskRepository.flush();
    }

    private Task findVisibleTask(Long taskId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getDeletedAt() == null)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks() {
        return listTasks("");
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(String query) {
        String searchPattern = toSearchPattern(query);
        List<Object[]> rows = searchPattern == null
                ? taskRepository.findSummaries()
                : taskRepository.findSummariesBySearchPattern(searchPattern);
        return rows.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> listHistory(String query) {
        String searchPattern = toSearchPattern(query);
        List<Object[]> archivedRows = searchPattern == null
                ? taskRepository.findArchivedSummaries()
                : taskRepository.findArchivedSummariesBySearchPattern(searchPattern);
        if (archivedRows.isEmpty()) {
            return List.of();
        }

        List<Long> taskIds = archivedRows.stream()
                .map(row -> toLong(unwrap(row)[0]))
                .toList();
        Map<Long, List<TaskStatusHistoryResponse>> eventsByTask = new HashMap<>();
        for (Object[] rawEvent : taskRepository.findHistoryEventsByTaskIds(taskIds)) {
            Object[] event = unwrap(rawEvent);
            Long taskId = toLong(event[1]);
            eventsByTask.computeIfAbsent(taskId, ignored -> new ArrayList<>())
                    .add(new TaskStatusHistoryResponse(
                            toLong(event[0]),
                            (String) event[2],
                            (String) event[3],
                            (String) event[4],
                            toInstant(event[5])));
        }

        return archivedRows.stream()
                .map(row -> toHistoryResponse(row, eventsByTask))
                .toList();
    }

    private TaskHistoryResponse toHistoryResponse(
            Object[] rawRow,
            Map<Long, List<TaskStatusHistoryResponse>> eventsByTask) {
        Object[] row = unwrap(rawRow);
        Long taskId = toLong(row[0]);
        return new TaskHistoryResponse(
                taskId,
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                toLong(row[5]),
                (String) row[6],
                toLong(row[7]),
                toInstant(row[8]),
                toInstant(row[9]),
                toLong(row[10]),
                toLong(row[11]),
                toInstant(row[12]),
                toInstant(row[13]),
                toInstant(row[14]),
                toLong(row[15]),
                eventsByTask.getOrDefault(taskId, List.of())
        );
    }

    static String toSearchPattern(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(query.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + normalized + "%";
    }

    private static Object[] unwrap(Object[] row) {
        return row.length == 1 && row[0] instanceof Object[] inner ? inner : row;
    }

    private TaskResponse toResponse(Object[] rawRow) {
        Object[] row = unwrap(rawRow);
        return new TaskResponse(
                toLong(row[0]),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                toLong(row[5]),
                (String) row[6],
                toLong(row[7]),
                toInstant(row[8]),
                toInstant(row[9]),
                toLong(row[10]),
                toLong(row[11]),
                toInstant(row[12]),
                toInstant(row[13]),
                toLong(row[14])
        );
    }

    private void verifyVersion(Long taskId, Long expectedVersion, Long actualVersion) {
        if (expectedVersion != null && !expectedVersion.equals(actualVersion)) {
            throw new OptimisticLockConflictException(taskId, expectedVersion, actualVersion);
        }
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
