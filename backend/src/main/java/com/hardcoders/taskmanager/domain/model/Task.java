package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.InvalidStatusTransitionException;
import com.hardcoders.taskmanager.domain.exception.OpenSubtasksException;
import com.hardcoders.taskmanager.domain.policy.BudgetPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Tarea (V004 {@code task}). Encapsula el ciclo de vida
 * PENDIENTE ⇄ EN_PROGRESO → COMPLETADA → EN_PROGRESO (reabrir) y la cancelación,
 * registrando cada transición como {@link StatusChange} pendiente de persistir, y cada cambio de
 * presupuesto o duración como {@link PlanRevision} pendiente.
 */
public final class Task {

    public static final String ENTITY = "Tarea";

    private final Integer id;
    private Activity activity;
    private String title;
    private String description;
    private WorkStatus status;
    private TaskPriority priority;
    private BigDecimal allocatedBudgetBob;
    private int estimatedDurationMinutes;
    private Integer actualDurationMinutes;
    private Instant completedAt;
    private Integer completedByUserId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<StatusChange> history;
    private final int lastRevisionNumber;
    private final List<PlanRevision> pendingRevisions;
    private final List<StatusChange> pendingChanges = new ArrayList<>();

    private Task(Integer id, Activity activity, String title, String description, WorkStatus status,
                 TaskPriority priority, BigDecimal allocatedBudgetBob, int estimatedDurationMinutes,
                 Integer actualDurationMinutes, Instant completedAt, Integer completedByUserId,
                 Instant createdAt, Instant updatedAt, List<StatusChange> history, int lastRevisionNumber,
                 List<PlanRevision> revisions) {
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
        this.lastRevisionNumber = lastRevisionNumber;
        this.pendingRevisions = revisions == null ? new ArrayList<>() : new ArrayList<>(revisions);
    }

    /**
     * Nueva tarea en PENDIENTE con su registro de creación y la revisión 1 de su plan.
     * El presupuesto asignado cuenta para el tope de presupuesto del proyecto (AC-US1.1-04).
     */
    public static Task create(String title, TaskPriority priority, Activity activity, Integer actorId, Instant now,
                              BigDecimal allocatedBudgetBob, Integer estimatedDurationMinutes) {
        BudgetPolicy.validatePositive(allocatedBudgetBob);
        BudgetPolicy.validateDuration(estimatedDurationMinutes);
        Task task = new Task(null, activity, WorkText.title(title), null, WorkStatus.PENDIENTE,
                priority == null ? TaskPriority.MEDIA : priority, BudgetPolicy.normalize(allocatedBudgetBob),
                estimatedDurationMinutes == null ? 0 : estimatedDurationMinutes, null, null, null,
                now, now, List.of(), 0, List.of());
        task.record(null, WorkStatus.PENDIENTE, actorId, now, "Tarea creada");
        task.addRevision("Plan inicial", actorId, now);
        return task;
    }

    /** Reconstruye una tarea persistida. */
    public static Task restore(Integer id, Activity activity, String title, String description, WorkStatus status,
                               TaskPriority priority, BigDecimal allocatedBudgetBob, int estimatedDurationMinutes,
                               Integer actualDurationMinutes, Instant completedAt, Integer completedByUserId,
                               Instant createdAt, Instant updatedAt, List<StatusChange> history) {
        return new Task(id, activity, title, description, status, priority, allocatedBudgetBob,
                estimatedDurationMinutes, actualDurationMinutes, completedAt, completedByUserId,
                createdAt, updatedAt, history, 0, List.of());
    }

    /** Reconstruye una tarea persistida indicando su última revisión de plan. */
    public static Task restore(Integer id, Activity activity, String title, String description, WorkStatus status,
                               TaskPriority priority, BigDecimal allocatedBudgetBob, int estimatedDurationMinutes,
                               Integer actualDurationMinutes, Instant completedAt, Integer completedByUserId,
                               Instant createdAt, Instant updatedAt, List<StatusChange> history,
                               int lastRevisionNumber) {
        return new Task(id, activity, title, description, status, priority, allocatedBudgetBob,
                estimatedDurationMinutes, actualDurationMinutes, completedAt, completedByUserId,
                createdAt, updatedAt, history, lastRevisionNumber, List.of());
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

    /**
     * US2.2: cambia el presupuesto asignado o la duración estimada y deja la revisión
     * correspondiente en {@code task_plan_history}. El motivo es obligatorio.
     */
    public void replan(BigDecimal newAllocatedBudgetBob, Integer newEstimatedDurationMinutes, String reason,
                       Integer actorId, Instant now) {
        BudgetPolicy.validatePositive(newAllocatedBudgetBob);
        BudgetPolicy.validateDuration(newEstimatedDurationMinutes);
        String motive = requiredReason(reason);
        BigDecimal normalized = BudgetPolicy.normalize(newAllocatedBudgetBob);
        int minutes = newEstimatedDurationMinutes == null ? 0 : newEstimatedDurationMinutes;
        if (normalized.compareTo(allocatedBudgetBob) == 0 && minutes == estimatedDurationMinutes) {
            throw new DomainValidationException("El plan de la tarea no cambia: modifica el presupuesto o la duración");
        }
        this.allocatedBudgetBob = normalized;
        this.estimatedDurationMinutes = minutes;
        addRevision(motive, actorId, now);
    }

    private void addRevision(String reason, Integer actorId, Instant now) {
        int next = lastRevisionNumber + pendingRevisions.size() + 1;
        pendingRevisions.add(new PlanRevision(next, allocatedBudgetBob, estimatedDurationMinutes, now, actorId, reason));
    }

    public void start(Integer actorId, Instant now) {
        start(actorId, now, "Tarea iniciada");
    }

    public void start(Integer actorId, Instant now, String reason) {
        transition(WorkStatus.PENDIENTE, WorkStatus.EN_PROGRESO, actorId, now, reason);
    }

    public void pause(Integer actorId, Instant now) {
        pause(actorId, now, "Tarea pausada");
    }

    public void pause(Integer actorId, Instant now, String reason) {
        transition(WorkStatus.EN_PROGRESO, WorkStatus.PENDIENTE, actorId, now, reason);
        refreshActualDuration(now);
    }

    public void complete(Integer actorId, Instant now) {
        complete(actorId, now, "Tarea completada");
    }

    public void complete(Integer actorId, Instant now, String reason) {
        transition(WorkStatus.EN_PROGRESO, WorkStatus.COMPLETADA, actorId, now, reason);
        this.completedAt = now;
        this.completedByUserId = actorId;
        refreshActualDuration(now);
    }

    public void reopen(Integer actorId, Instant now) {
        reopen(actorId, now, "Tarea reabierta");
    }

    public void reopen(Integer actorId, Instant now, String reason) {
        transition(WorkStatus.COMPLETADA, WorkStatus.EN_PROGRESO, actorId, now, reason);
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

    /** Revisiones de plan pendientes de persistir. */
    public List<PlanRevision> pendingRevisions() {
        return Collections.unmodifiableList(pendingRevisions);
    }

    public int lastRevisionNumber() {
        return lastRevisionNumber;
    }

    private static String requiredReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new DomainValidationException("El motivo del cambio es obligatorio");
        }
        String stripped = reason.strip();
        if (stripped.length() > PlanRevision.REASON_MAX) {
            throw new DomainValidationException("El motivo no puede superar " + PlanRevision.REASON_MAX + " caracteres");
        }
        return stripped;
    }

    public List<StatusChange> history() {
        return Collections.unmodifiableList(history);
    }

    public List<StatusChange> pendingChanges() {
        return Collections.unmodifiableList(pendingChanges);
    }
}
