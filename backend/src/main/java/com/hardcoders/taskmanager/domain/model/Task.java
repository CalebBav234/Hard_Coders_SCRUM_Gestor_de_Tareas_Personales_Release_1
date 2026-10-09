package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;
import com.hardcoders.taskmanager.domain.exception.OpenSubtasksException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Tarea (V004 {@code task}). Encapsula el ciclo de vida
 * PENDIENTE ⇄ EN_PROGRESO → COMPLETADA → EN_PROGRESO (reabrir) y la cancelación,
 * registrando cada transición como {@link StatusChange} pendiente de persistir.
 */
public final class Task {

    public static final String ENTITY = "Tarea";

    private final Integer id;
    private Activity activity;
    private String title;
    private String description;
    private WorkStatus status;
    private TaskPriority priority;
    private final BigDecimal allocatedBudgetBob;
    private final int estimatedDurationMinutes;
    private Integer actualDurationMinutes;
    private Instant completedAt;
    private Integer completedByUserId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<StatusChange> history;
    private final List<StatusChange> pendingChanges = new ArrayList<>();

    private Task(Integer id, Activity activity, String title, String description, WorkStatus status,
                 TaskPriority priority, BigDecimal allocatedBudgetBob, int estimatedDurationMinutes,
                 Integer actualDurationMinutes, Instant completedAt, Integer completedByUserId,
                 Instant createdAt, Instant updatedAt, List<StatusChange> history) {
        this.id = id;
        this.activity = Objects.requireNonNull(activity, "activity");
        this.title = title;
        this.description = description;
        this.status = Objects.requireNonNull(status, "status");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.allocatedBudgetBob = allocatedBudgetBob == null ? BigDecimal.ZERO.setScale(2) : allocatedBudgetBob;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.actualDurationMinutes = actualDurationMinutes;
        this.completedAt = completedAt;
        this.completedByUserId = completedByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.history = new ArrayList<>(history);
    }

    /** Nueva tarea en estado PENDIENTE con su registro de creación. */
    public static Task create(String title, TaskPriority priority, Activity activity, Integer actorId, Instant now) {
        Task task = new Task(null, activity, WorkText.title(title), null, WorkStatus.PENDIENTE,
                priority == null ? TaskPriority.MEDIA : priority, null, 0, null, null, null,
                now, now, List.of());
        task.record(null, WorkStatus.PENDIENTE, actorId, now, "Tarea creada");
        return task;
    }

    /** Reconstruye una tarea persistida. */
    public static Task restore(Integer id, Activity activity, String title, String description, WorkStatus status,
                               TaskPriority priority, BigDecimal allocatedBudgetBob, int estimatedDurationMinutes,
                               Integer actualDurationMinutes, Instant completedAt, Integer completedByUserId,
                               Instant createdAt, Instant updatedAt, List<StatusChange> history) {
        return new Task(id, activity, title, description, status, priority, allocatedBudgetBob,
                estimatedDurationMinutes, actualDurationMinutes, completedAt, completedByUserId,
                createdAt, updatedAt, history);
    }

    public void verifyVersion(Long expectedVersion) {
        VersionToken.verify(ENTITY, id, expectedVersion, updatedAt);
    }

    public void edit(String newTitle, String newDescription, TaskPriority newPriority) {
        this.title = WorkText.title(newTitle);
        this.description = WorkText.description(newDescription);
        this.priority = newPriority == null ? this.priority : newPriority;
    }

    public void moveTo(Activity newActivity) {
        this.activity = Objects.requireNonNull(newActivity, "activity");
    }

    public void start(Integer actorId, Instant now) {
        transition(WorkStatus.PENDIENTE, WorkStatus.EN_PROGRESO, actorId, now, "Tarea iniciada");
    }

    public void pause(Integer actorId, Instant now) {
        transition(WorkStatus.EN_PROGRESO, WorkStatus.PENDIENTE, actorId, now, "Tarea pausada");
        refreshActualDuration(now);
    }

    public void complete(Integer actorId, Instant now) {
        transition(WorkStatus.EN_PROGRESO, WorkStatus.COMPLETADA, actorId, now, "Tarea completada");
        this.completedAt = now;
        this.completedByUserId = actorId;
        refreshActualDuration(now);
    }

    public void reopen(Integer actorId, Instant now) {
        transition(WorkStatus.COMPLETADA, WorkStatus.EN_PROGRESO, actorId, now, "Tarea reabierta");
        this.completedAt = null;
        this.completedByUserId = null;
    }

    /** Equivale al borrado lógico del modelo anterior: la tarea pasa a CANCELADA y sale de la lista. */
    public void cancel(boolean hasOpenSubtasks, Integer actorId, Instant now) {
        if (status == WorkStatus.CANCELADA) {
            throw new InvalidStatusTransitionException(status, WorkStatus.CANCELADA);
        }
        if (hasOpenSubtasks) {
            throw new OpenSubtasksException(id);
        }
        WorkStatus previous = status;
        if (previous == WorkStatus.EN_PROGRESO) {
            refreshActualDuration(now);
        }
        this.status = WorkStatus.CANCELADA;
        record(previous, WorkStatus.CANCELADA, actorId, now, "Tarea eliminada");
    }

    /** Tiempo trabajado según el historial (incluye los cambios aún no persistidos). */
    public TimeTracking timeTracking() {
        TimeTracking tracking = TimeTracking.from(allChanges(), status);
        // Tareas migradas sin segmentos en el historial conservan sus minutos registrados.
        if (tracking.trackedSeconds() == 0 && tracking.currentSegmentStart() == null
                && actualDurationMinutes != null && actualDurationMinutes > 0) {
            return new TimeTracking(actualDurationMinutes * 60L, null);
        }
        return tracking;
    }

    private void transition(WorkStatus expected, WorkStatus target, Integer actorId, Instant now, String reason) {
        if (status != expected) {
            throw new InvalidStatusTransitionException(status, target);
        }
        WorkStatus previous = status;
        this.status = target;
        record(previous, target, actorId, now, reason);
    }

    private void refreshActualDuration(Instant now) {
        long seconds = TimeTracking.from(allChanges(), WorkStatus.EN_PROGRESO).totalSecondsAt(now);
        Integer minutes = TimeTracking.toMinutes(seconds);
        if (minutes != null) {
            this.actualDurationMinutes = minutes;
        }
    }

    private void record(WorkStatus previous, WorkStatus next, Integer actorId, Instant now, String reason) {
        pendingChanges.add(new StatusChange(null, previous, next, now, actorId, reason));
    }

    private List<StatusChange> allChanges() {
        List<StatusChange> all = new ArrayList<>(history);
        all.addAll(pendingChanges);
        return all;
    }

    public boolean isNew() {
        return id == null;
    }

    public Integer id() {
        return id;
    }

    public Activity activity() {
        return activity;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public WorkStatus status() {
        return status;
    }

    public TaskPriority priority() {
        return priority;
    }

    public BigDecimal allocatedBudgetBob() {
        return allocatedBudgetBob;
    }

    public int estimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public Integer actualDurationMinutes() {
        return actualDurationMinutes;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public Integer completedByUserId() {
        return completedByUserId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return VersionToken.of(updatedAt);
    }

    public List<StatusChange> history() {
        return Collections.unmodifiableList(history);
    }

    public List<StatusChange> pendingChanges() {
        return Collections.unmodifiableList(pendingChanges);
    }
}
