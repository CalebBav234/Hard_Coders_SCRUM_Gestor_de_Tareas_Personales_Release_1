package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ProjectTransitionException;
import com.hardcoders.taskmanager.domain.policy.BudgetPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Proyecto (V004 {@code project}). Cada cambio de plan (presupuesto, duración) y cada delegación
 * agrega una revisión inmutable a {@code project_plan_history}, que el adaptador persiste en la
 * misma transacción que el proyecto.
 */
public final class Project {

    public static final String ENTITY = "Proyecto";

    private final Integer id;
    private String name;
    private String description;
    private Integer managerUserId;
    private ProjectStatus status;
    private BigDecimal budgetBob;
    private int estimatedDurationMinutes;
    private final Integer createdByUserId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final int lastRevisionNumber;
    private final List<PlanRevision> pendingRevisions = new ArrayList<>();

    private Project(Integer id, String name, String description, Integer managerUserId, ProjectStatus status,
                    BigDecimal budgetBob, int estimatedDurationMinutes, Integer createdByUserId,
                    Instant createdAt, Instant updatedAt, int lastRevisionNumber) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.managerUserId = Objects.requireNonNull(managerUserId, "managerUserId");
        this.status = Objects.requireNonNull(status, "status");
        this.budgetBob = Objects.requireNonNull(budgetBob, "budgetBob");
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastRevisionNumber = lastRevisionNumber;
    }

    /** AC-US1.1-01: nuevo proyecto PLANIFICADO con su revisión 1. */
    public static Project create(String name, String description, Integer managerUserId, BigDecimal budgetBob,
                                 Integer estimatedDurationMinutes, String reason, Integer actorId, Instant now) {
        BudgetPolicy.validatePositive(budgetBob);
        BudgetPolicy.validateDuration(estimatedDurationMinutes);
        Project project = new Project(null, WorkText.title(name), WorkText.description(description), managerUserId,
                ProjectStatus.PLANIFICADO, BudgetPolicy.normalize(budgetBob), estimatedDurationMinutes, actorId,
                now, now, 0);
        project.addRevision(reasonOrDefault(reason, "Plan inicial"), actorId, now);
        return project;
    }

    public static Project restore(Integer id, String name, String description, Integer managerUserId,
                                  ProjectStatus status, BigDecimal budgetBob, int estimatedDurationMinutes,
                                  Integer createdByUserId, Instant createdAt, Instant updatedAt,
                                  int lastRevisionNumber) {
        return new Project(id, name, description, managerUserId, status, budgetBob, estimatedDurationMinutes,
                createdByUserId, createdAt, updatedAt, lastRevisionNumber);
    }

    public void verifyVersion(Long expectedVersion) {
        VersionToken.verify(ENTITY, id, expectedVersion, updatedAt);
    }

    public void editDetails(String newName, String newDescription) {
        this.name = WorkText.title(newName);
        this.description = WorkText.description(newDescription);
    }

    /**
     * Nueva revisión del plan. {@code allocatedBob} es lo ya asignado a las tareas del proyecto:
     * el presupuesto no puede quedar por debajo (AC-US1.1-04).
     */
    public void replan(BigDecimal newBudgetBob, Integer newEstimatedDurationMinutes, BigDecimal allocatedBob,
                       String reason, Integer actorId, Instant now) {
        requireOpen("cambiar el plan");
        BudgetPolicy.validatePositive(newBudgetBob);
        BudgetPolicy.validateDuration(newEstimatedDurationMinutes);
        BudgetPolicy.validateAllocationCap(newBudgetBob, allocatedBob);
        String motive = requiredReason(reason);
        BigDecimal normalized = BudgetPolicy.normalize(newBudgetBob);
        if (normalized.compareTo(budgetBob) == 0 && newEstimatedDurationMinutes == estimatedDurationMinutes) {
            throw new DomainValidationException("El plan no cambia: modifica el presupuesto o la duración");
        }
        this.budgetBob = normalized;
        this.estimatedDurationMinutes = newEstimatedDurationMinutes;
        addRevision(motive, actorId, now);
    }

    /** US1.2: cambia el Encargado y deja la delegación registrada como revisión del plan. */
    public void delegateTo(UserAccount newManager, UserAccount previousManager, String reason,
                           Integer actorId, Instant now) {
        requireOpen("delegar");
        if (newManager.userId().equals(managerUserId)) {
            throw new DomainValidationException("Ese usuario ya es el Encargado del proyecto");
        }
        String from = previousManager == null ? "usuario " + managerUserId : previousManager.fullName();
        String motive = "Delegación de " + from + " a " + newManager.fullName() + ": " + requiredReason(reason);
        this.managerUserId = newManager.userId();
        addRevision(truncate(motive), actorId, now);
    }

    public void changeStatus(ProjectStatus target) {
        if (target == null || !status.canMoveTo(target)) {
            throw new ProjectTransitionException(status, target);
        }
        this.status = target;
    }

    private void requireOpen(String operation) {
        if (status.isClosed()) {
            throw new ProjectTransitionException("No se puede " + operation + " en un proyecto " + status);
        }
    }

    private void addRevision(String reason, Integer actorId, Instant now) {
        int next = lastRevisionNumber + pendingRevisions.size() + 1;
        pendingRevisions.add(new PlanRevision(next, budgetBob, estimatedDurationMinutes, now, actorId, reason));
    }

    private static String requiredReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new DomainValidationException("El motivo del cambio es obligatorio");
        }
        String stripped = reason.strip();
        if (stripped.length() > 300) {
            throw new DomainValidationException("El motivo no puede superar 300 caracteres");
        }
        return stripped;
    }

    private static String reasonOrDefault(String reason, String fallback) {
        return reason == null || reason.isBlank() ? fallback : requiredReason(reason);
    }

    private static String truncate(String text) {
        return text.length() <= PlanRevision.REASON_MAX ? text : text.substring(0, PlanRevision.REASON_MAX);
    }

    public boolean isNew() {
        return id == null;
    }

    public Integer id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Integer managerUserId() {
        return managerUserId;
    }

    public ProjectStatus status() {
        return status;
    }

    public BigDecimal budgetBob() {
        return budgetBob;
    }

    public int estimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public Integer createdByUserId() {
        return createdByUserId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public int lastRevisionNumber() {
        return lastRevisionNumber;
    }

    public long version() {
        return VersionToken.of(updatedAt);
    }

    public List<PlanRevision> pendingRevisions() {
        return Collections.unmodifiableList(pendingRevisions);
    }
}
