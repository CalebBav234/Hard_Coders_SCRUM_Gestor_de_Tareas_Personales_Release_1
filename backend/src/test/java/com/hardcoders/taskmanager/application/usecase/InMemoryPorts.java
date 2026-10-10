package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.PlanRevision;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.Stage;
import com.hardcoders.taskmanager.domain.model.StatusChange;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.TaskAssignment;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.port.out.AssignmentRepository;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.HierarchyRepository;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Dobles de prueba en memoria de los puertos de salida (sin base de datos). */
final class InMemoryPorts {

    static final Integer ACTOR = 1;
    static final Integer PROJECT = 1;
    static final Integer STAGE = 2;
    static final Integer ACTIVITY = 3;
    static final Integer WORKER = 2;

    final Map<Integer, Project> projects = new LinkedHashMap<>();
    final Map<Integer, Stage> stages = new LinkedHashMap<>();
    final Map<Integer, Activity> activities = new LinkedHashMap<>();
    final Map<Integer, Task> tasks = new LinkedHashMap<>();
    final Map<Integer, Subtask> subtasks = new LinkedHashMap<>();
    final Map<String, TaskAssignment> assignments = new LinkedHashMap<>();

    private final Clock clock;
    private int nextId = 1;
    private int tick = 0;

    InMemoryPorts(Clock clock) {
        this.clock = clock;
    }

    /** Simula el trigger de updated_at: cada escritura produce una marca de tiempo distinta. */
    private Instant writeTime() {
        return Instant.now(clock).plusMillis(++tick);
    }

    Actor currentActor = new Actor(ACTOR, true, true);

    final CurrentActor actor = () -> currentActor;

    final ProjectRepository projectRepository = new ProjectRepository() {

        @Override
        public Optional<Project> findById(Integer projectId) {
            return Optional.ofNullable(projects.get(projectId));
        }

        @Override
        public Optional<Project> findForUpdate(Integer projectId) {
            return Optional.ofNullable(projects.get(projectId));
        }

        @Override
        public List<Project> findAll() {
            return List.copyOf(projects.values());
        }

        @Override
        public List<Project> findManagedBy(Integer managerUserId) {
            return projects.values().stream()
                    .filter(project -> Objects.equals(project.managerUserId(), managerUserId))
                    .toList();
        }

        @Override
        public java.math.BigDecimal allocatedBudget(Integer projectId) {
            return tasks.values().stream()
                    .filter(task -> projectIdOf(task).filter(id -> id.equals(projectId)).isPresent())
                    .filter(task -> task.status() != WorkStatus.CANCELADA)
                    .map(Task::allocatedBudgetBob)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                    .setScale(2, java.math.RoundingMode.UNNECESSARY);
        }

        @Override
        public List<com.hardcoders.taskmanager.domain.model.PlanRevision> planHistory(Integer projectId) {
            return List.of();
        }

        @Override
        public Project save(Project project) {
            Integer id = project.isNew() ? nextId++ : project.id();
            List<com.hardcoders.taskmanager.domain.model.PlanRevision> revisions =
                    new ArrayList<>(project.pendingRevisions());
            projects.put(id, restoreProject(project, id, writeTime(), revisions));
            return projects.get(id);
        }
    };

    final HierarchyRepository hierarchy = new HierarchyRepository() {

        @Override
        public Optional<Stage> findStage(Integer stageId) {
            return Optional.ofNullable(stages.get(stageId));
        }

        @Override
        public Optional<Stage> findStageForUpdate(Integer stageId) {
            return Optional.ofNullable(stages.get(stageId));
        }

        @Override
        public List<Stage> stagesOf(Integer projectId) {
            return stages.values().stream()
                    .filter(stage -> stage.projectId().equals(projectId))
                    .sorted(Comparator.comparing(Stage::createdAt))
                    .toList();
        }

        @Override
        public Stage save(Stage stage) {
            Integer id = stage.isNew() ? nextId++ : stage.id();
            Stage stored = Stage.restore(id, stage.projectId(), stage.name(), stage.description(), stage.status(),
                    stage.createdAt(), writeTime());
            stages.put(id, stored);
            return stored;
        }

        @Override
        public Optional<Activity> findActivity(Integer activityId) {
            return Optional.ofNullable(activities.get(activityId));
        }

        @Override
        public Optional<Activity> findActivityForUpdate(Integer activityId) {
            return Optional.ofNullable(activities.get(activityId));
        }

        @Override
        public List<Activity> activitiesOf(Integer stageId) {
            return activities.values().stream()
                    .filter(activity -> activity.stageId().equals(stageId))
                    .sorted(Comparator.comparing(Activity::name))
                    .toList();
        }

        @Override
        public Activity save(Activity activity) {
            Integer id = activity.isNew() ? nextId++ : activity.id();
            Activity stored = Activity.restore(id, activity.stageId(), activity.name(), activity.description(),
                    activity.status(), activity.createdAt(), writeTime());
            activities.put(id, stored);
            return stored;
        }
    };

    final TaskRepository taskRepository = new TaskRepository() {

        @Override
        public Optional<Task> findOpenForUpdate(Integer projectId, Integer taskId) {
            return Optional.ofNullable(tasks.get(taskId))
                    .filter(task -> task.status() != WorkStatus.CANCELADA)
                    .filter(task -> InMemoryPorts.this.projectIdOf(task).filter(id -> id.equals(projectId)).isPresent());
        }

        @Override
        public Optional<Task> findVisible(Integer projectId, Integer taskId) {
            return findOpenForUpdate(projectId, taskId);
        }

        @Override
        public List<Task> findVisible(Integer projectId, String likePattern) {
            return tasks.values().stream()
                    .filter(task -> InMemoryPorts.this.projectIdOf(task).filter(id -> id.equals(projectId)).isPresent())
                    .filter(task -> task.status() != WorkStatus.CANCELADA)
                    .toList();
        }

        @Override
        public List<Task> findArchived(Integer projectId, String likePattern) {
            return tasks.values().stream()
                    .filter(task -> InMemoryPorts.this.projectIdOf(task).filter(id -> id.equals(projectId)).isPresent())
                    .filter(task -> task.status() == WorkStatus.CANCELADA || task.status() == WorkStatus.COMPLETADA)
                    .toList();
        }

        @Override
        public List<Task> findAssignedTo(Integer workerUserId, String likePattern) {
            return assignedTaskIds(workerUserId).stream().map(tasks::get).filter(Objects::nonNull).toList();
        }

        @Override
        public Optional<Task> findAssignedForUpdate(Integer workerUserId, Integer taskId) {
            if (!assignedTaskIds(workerUserId).contains(taskId)) {
                return Optional.empty();
            }
            return findOpenForUpdate(projectIdOf(taskId).orElse(null), taskId);
        }

        @Override
        public Optional<Integer> projectIdOf(Integer taskId) {
            return Optional.ofNullable(tasks.get(taskId)).flatMap(task -> InMemoryPorts.this.projectIdOf(task));
        }

        @Override
        public Task save(Task task) {
            Integer id = task.isNew() ? nextId++ : task.id();
            List<StatusChange> history = new ArrayList<>(task.history());
            for (StatusChange change : task.pendingChanges()) {
                history.add(new StatusChange(nextId++, change.previousStatus(), change.newStatus(),
                        change.changedAt(), change.changedByUserId(), change.reason()));
            }
            int revision = task.lastRevisionNumber() + task.pendingRevisions().size();
            Task stored = Task.restore(id, task.activity(), task.title(), task.description(), task.status(),
                    task.priority(), task.allocatedBudgetBob(), task.estimatedDurationMinutes(),
                    task.actualDurationMinutes(), task.completedAt(), task.completedByUserId(),
                    task.createdAt(), writeTime(), history, revision);
            tasks.put(id, stored);
            return stored;
        }
    };

    final SubtaskRepository subtaskRepository = new SubtaskRepository() {

        @Override
        public Optional<Subtask> findOpenForUpdate(Integer projectId, Integer subtaskId) {
            return Optional.ofNullable(subtasks.get(subtaskId)).filter(subtask -> {
                        Task task = tasks.get(subtask.taskId());
                        return task != null && task.status() != WorkStatus.CANCELADA;
                    })
                    .filter(subtask -> subtask.status() != WorkStatus.CANCELADA);
        }

        @Override
        public Optional<Subtask> findForUpdate(Integer taskId, Integer subtaskId) {
            return Optional.ofNullable(subtasks.get(subtaskId))
                    .filter(subtask -> subtask.taskId().equals(taskId))
                    .filter(subtask -> subtask.status() != WorkStatus.CANCELADA);
        }

        @Override
        public Optional<Integer> taskIdOf(Integer subtaskId) {
            return Optional.ofNullable(subtasks.get(subtaskId)).map(Subtask::taskId);
        }

        @Override
        public List<Subtask> findVisibleByTaskIds(Collection<Integer> taskIds) {
            return subtasks.values().stream()
                    .filter(subtask -> taskIds.contains(subtask.taskId())
                            && subtask.status() != WorkStatus.CANCELADA)
                    .toList();
        }

        @Override
        public List<Subtask> findArchived(Integer projectId, String likePattern) {
            return subtasks.values().stream()
                    .filter(subtask -> subtask.status() == WorkStatus.CANCELADA
                            || subtask.status() == WorkStatus.COMPLETADA)
                    .toList();
        }

        @Override
        public boolean hasOpenSubtasks(Integer taskId) {
            return subtasks.values().stream()
                    .anyMatch(subtask -> subtask.taskId().equals(taskId) && subtask.status().isOpen());
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

    final AssignmentRepository assignmentRepository = new AssignmentRepository() {

        @Override
        public Optional<TaskAssignment> find(Integer taskId, Integer workerUserId) {
            return Optional.ofNullable(assignments.get(key(taskId, workerUserId)));
        }

        @Override
        public List<TaskAssignment> findForTask(Integer taskId) {
            return assignments.values().stream()
                    .filter(assignment -> assignment.taskId().equals(taskId))
                    .toList();
        }

        @Override
        public Map<Integer, List<TaskAssignment>> findForTasks(Collection<Integer> taskIds) {
            Map<Integer, List<TaskAssignment>> byTask = new LinkedHashMap<>();
            assignments.values().stream()
                    .filter(assignment -> taskIds.contains(assignment.taskId()))
                    .forEach(assignment -> byTask
                            .computeIfAbsent(assignment.taskId(), ignored -> new ArrayList<>())
                            .add(assignment));
            return byTask;
        }

        @Override
        public List<TaskAssignment> findForWorker(Integer workerUserId) {
            return assignments.values().stream()
                    .filter(assignment -> assignment.workerUserId().equals(workerUserId)).toList();
        }

        @Override
        public TaskAssignment save(TaskAssignment assignment) {
            assignments.put(key(assignment.taskId(), assignment.workerUserId()), assignment);
            return assignment;
        }

        @Override
        public void delete(Integer taskId, Integer workerUserId) {
            assignments.remove(key(taskId, workerUserId));
        }
    };

    private static String key(Integer taskId, Integer workerUserId) {
        return taskId + ":" + workerUserId;
    }

    private List<Integer> assignedTaskIds(Integer workerUserId) {
        return assignments.values().stream()
                .filter(assignment -> assignment.workerUserId().equals(workerUserId))
                .map(TaskAssignment::taskId)
                .toList();
    }

    Optional<Integer> projectIdOf(Task task) {
        return Optional.ofNullable(activities.get(task.activity().id()))
                .map(Activity::stageId)
                .flatMap(stageId -> Optional.ofNullable(stages.get(stageId)).map(Stage::projectId));
    }

    private static Project restoreProject(Project project, Integer id, Instant updatedAt,
                                          List<com.hardcoders.taskmanager.domain.model.PlanRevision> revisions) {
        Project stored = Project.restore(id, project.name(), project.description(), project.managerUserId(),
                project.status(), project.budgetBob(), project.estimatedDurationMinutes(),
                project.createdByUserId(), project.createdAt(), updatedAt,
                project.lastRevisionNumber() + revisions.size());
        return stored;
    }

    /** Utilidades de preparación usadas por las pruebas de casos de uso. */
    Project seedProject(String name, Integer managerUserId, java.math.BigDecimal budget) {
        Project project = Project.create(name, "desc", managerUserId, budget, 480, "Plan inicial", managerUserId,
                Instant.now(clock));
        Project saved = projectRepository.save(project);
        projects.put(saved.id(), saved);
        return saved;
    }

    Stage seedStage(Integer projectId, String name) {
        Stage stage = Stage.create(projectId, name, null, Instant.now(clock));
        Stage saved = hierarchy.save(stage);
        stages.put(saved.id(), saved);
        return saved;
    }

    Activity seedActivity(Integer stageId, String name) {
        Activity activity = Activity.create(stageId, name, null, Instant.now(clock));
        Activity saved = hierarchy.save(activity);
        activities.put(saved.id(), saved);
        return saved;
    }

    Task seedTask(Integer activityId, String title, Integer actorId) {
        Task task = Task.create(title, com.hardcoders.taskmanager.domain.model.TaskPriority.MEDIA,
                activities.get(activityId), actorId, Instant.now(clock), java.math.BigDecimal.ZERO, 60);
        Task saved = taskRepository.save(task);
        tasks.put(saved.id(), saved);
        return saved;
    }

    Subtask seedSubtask(Integer taskId, String title) {
        Subtask subtask = Subtask.create(taskId, title, null, Instant.now(clock));
        Subtask saved = subtaskRepository.save(subtask);
        subtasks.put(saved.id(), saved);
        return saved;
    }

    void assign(Integer taskId, Integer workerUserId, Integer assignedBy) {
        assignmentRepository.save(new TaskAssignment(taskId, workerUserId, Instant.now(clock), assignedBy));
    }

    /** Marca el error esperado si la prueba pretende comprobar una transición inválida. */
    static void expectInvalid(Runnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("Se esperaba InvalidStatusTransitionException");
        } catch (InvalidStatusTransitionException expected) {
            // correcto
        }
    }
}
