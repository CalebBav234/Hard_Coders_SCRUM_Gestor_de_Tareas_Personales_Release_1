package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.StatusChange;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.model.Workspace;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;
import com.hardcoders.taskmanager.domain.port.out.WorkspaceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Dobles de prueba en memoria de los puertos de salida (sin base de datos). */
final class InMemoryPorts {

    static final Integer ACTOR = 1;
    final Workspace workspace = new Workspace(10, 20);
    final Map<Integer, Task> tasks = new LinkedHashMap<>();
    final Map<Integer, Subtask> subtasks = new LinkedHashMap<>();
    final Map<String, Activity> activities = new LinkedHashMap<>();
    private final Clock clock;
    private int nextId = 100;
    private int tick = 0;

    InMemoryPorts(Clock clock) {
        this.clock = clock;
    }

    /** Simula el trigger de updated_at: cada escritura produce una marca de tiempo distinta. */
    private Instant writeTime() {
        return Instant.now(clock).plusMillis(++tick);
    }

    final CurrentActor actor = () -> ACTOR;

    final WorkspaceRepository workspaces = new WorkspaceRepository() {
        @Override
        public Optional<Workspace> findWorkspace() {
            return Optional.of(workspace);
        }

        @Override
        public Workspace currentWorkspace(Integer actorId) {
            return workspace;
        }

        @Override
        public Activity findOrCreateActivity(Integer stageId, String name) {
            return activities.computeIfAbsent(name.toLowerCase(), key -> new Activity(nextId++, name));
        }

        @Override
        public List<Activity> activities(Integer stageId) {
            return activities.values().stream().sorted(Comparator.comparing(Activity::name)).toList();
        }
    };

    final TaskRepository taskRepository = new TaskRepository() {
        @Override
        public Optional<Task> findOpenForUpdate(Integer projectId, Integer taskId) {
            return Optional.ofNullable(tasks.get(taskId)).filter(t -> t.status() != WorkStatus.CANCELADA);
        }

        @Override
        public Optional<Task> findVisible(Integer projectId, Integer taskId) {
            return findOpenForUpdate(projectId, taskId);
        }

        @Override
        public List<Task> findVisible(Integer projectId, String likePattern) {
            return tasks.values().stream().filter(t -> t.status() != WorkStatus.CANCELADA).toList();
        }

        @Override
        public List<Task> findArchived(Integer projectId, String likePattern) {
            return tasks.values().stream()
                    .filter(t -> t.status() == WorkStatus.CANCELADA || t.status() == WorkStatus.COMPLETADA)
                    .toList();
        }

        @Override
        public Task save(Task task) {
            Integer id = task.isNew() ? nextId++ : task.id();
            List<StatusChange> history = new ArrayList<>(task.history());
            for (StatusChange change : task.pendingChanges()) {
                history.add(new StatusChange(nextId++, change.previousStatus(), change.newStatus(),
                        change.changedAt(), change.changedByUserId(), change.reason()));
            }
            Task stored = Task.restore(id, task.activity(), task.title(), task.description(), task.status(),
                    task.priority(), task.allocatedBudgetBob(), task.estimatedDurationMinutes(),
                    task.actualDurationMinutes(), task.completedAt(), task.completedByUserId(),
                    task.createdAt(), writeTime(), history);
            tasks.put(id, stored);
            return stored;
        }
    };

    final SubtaskRepository subtaskRepository = new SubtaskRepository() {
        @Override
        public Optional<Subtask> findOpenForUpdate(Integer projectId, Integer subtaskId) {
            return Optional.ofNullable(subtasks.get(subtaskId)).filter(s -> s.status() != WorkStatus.CANCELADA);
        }

        @Override
        public List<Subtask> findVisibleByTaskIds(Collection<Integer> taskIds) {
            return subtasks.values().stream()
                    .filter(s -> taskIds.contains(s.taskId()) && s.status() != WorkStatus.CANCELADA)
                    .toList();
        }

        @Override
        public List<Subtask> findArchived(Integer projectId, String likePattern) {
            return subtasks.values().stream()
                    .filter(s -> s.status() == WorkStatus.CANCELADA || s.status() == WorkStatus.COMPLETADA)
                    .toList();
        }

        @Override
        public boolean hasOpenSubtasks(Integer taskId) {
            return subtasks.values().stream().anyMatch(s -> s.taskId().equals(taskId) && s.status().isOpen());
        }

        @Override
        public Subtask save(Subtask subtask) {
            Integer id = subtask.isNew() ? nextId++ : subtask.id();
            Subtask stored = Subtask.restore(id, subtask.taskId(), subtask.title(), subtask.description(),
                    subtask.status(), subtask.completedAt(), subtask.completedByUserId(), subtask.createdAt(),
                    writeTime());
            subtasks.put(id, stored);
            return stored;
        }
    };
}
