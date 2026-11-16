package com.hardcoders.taskmanager.domain.model;

import com.hardcoders.taskmanager.domain.exception.BudgetBelowAllocationsException;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ProjectTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectTest {

    private static final Instant NOW = Instant.parse("2026-10-10T10:00:00Z");
    private static final Integer CHIEF = 1;

    @Test
    void createsAPlannedProjectWithRevisionOne() {
        Project project = Project.create("  Edificio A ", null, 2, new BigDecimal("1000"), 600, "", CHIEF, NOW);

        assertThat(project.name()).isEqualTo("Edificio A");
        assertThat(project.status()).isEqualTo(ProjectStatus.PLANIFICADO);
        assertThat(project.budgetBob()).isEqualByComparingTo("1000.00");
        assertThat(project.budgetBob().scale()).isEqualTo(2);
        assertThat(project.pendingRevisions()).singleElement().satisfies(revision -> {
            assertThat(revision.revisionNumber()).isEqualTo(1);
            assertThat(revision.reason()).isEqualTo("Plan inicial");
            assertThat(revision.changedByUserId()).isEqualTo(CHIEF);
        });
    }

    @Test
    void replanningAddsTheNextRevisionAndRespectsAllocations() {
        Project project = persisted(ProjectStatus.EN_EJECUCION, 3);

        project.replan(new BigDecimal("2500.50"), 900, new BigDecimal("2000.00"), " Ampliación ", CHIEF, NOW);

        assertThat(project.budgetBob()).isEqualByComparingTo("2500.50");
        assertThat(project.pendingRevisions()).singleElement().satisfies(revision -> {
            assertThat(revision.revisionNumber()).isEqualTo(4);
            assertThat(revision.reason()).isEqualTo("Ampliación");
        });
        assertThatThrownBy(() -> project.replan(new BigDecimal("10"), 900, new BigDecimal("50"), "Recorte", CHIEF, NOW))
                .isInstanceOf(BudgetBelowAllocationsException.class);
    }

    @Test
    void replanningRequiresAReasonAndAnActualChange() {
        Project project = persisted(ProjectStatus.PLANIFICADO, 1);

        assertThatThrownBy(() -> project.replan(new BigDecimal("500"), 60, BigDecimal.ZERO, " ", CHIEF, NOW))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> project.replan(new BigDecimal("1000.00"), 600, BigDecimal.ZERO, "Igual", CHIEF, NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void closedProjectsCannotBeReplannedOrDelegated() {
        Project project = persisted(ProjectStatus.FINALIZADO, 2);
        UserAccount next = new UserAccount(9, "n@x.com", "Nora", "Vaca", false, true);

        assertThatThrownBy(() -> project.replan(new BigDecimal("5"), 1, BigDecimal.ZERO, "x", CHIEF, NOW))
                .isInstanceOf(ProjectTransitionException.class);
        assertThatThrownBy(() -> project.delegateTo(next, null, "x", CHIEF, NOW))
                .isInstanceOf(ProjectTransitionException.class);
    }

    @Test
    void delegationChangesTheManagerAndIsRecordedAsARevision() {
        Project project = persisted(ProjectStatus.EN_EJECUCION, 2);
        UserAccount previous = new UserAccount(2, "a@x.com", "Ana", "Paz", false, true);
        UserAccount next = new UserAccount(9, "n@x.com", "Nora", "Vaca", false, true);

        project.delegateTo(next, previous, "Vacaciones", CHIEF, NOW);

        assertThat(project.managerUserId()).isEqualTo(9);
        assertThat(project.pendingRevisions()).singleElement().satisfies(revision -> {
            assertThat(revision.revisionNumber()).isEqualTo(3);
            assertThat(revision.budgetBob()).isEqualByComparingTo(project.budgetBob());
            assertThat(revision.reason()).isEqualTo("Delegación de Ana Paz a Nora Vaca: Vacaciones");
        });
        assertThatThrownBy(() -> project.delegateTo(next, next, "Otra vez", CHIEF, NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void followsTheProjectLifecycle() {
        Project project = persisted(ProjectStatus.PLANIFICADO, 1);

        project.changeStatus(ProjectStatus.EN_EJECUCION);
        project.changeStatus(ProjectStatus.EN_PAUSA);
        project.changeStatus(ProjectStatus.EN_EJECUCION);
        project.changeStatus(ProjectStatus.FINALIZADO);

        assertThat(project.status().isClosed()).isTrue();
        assertThatThrownBy(() -> project.changeStatus(ProjectStatus.EN_EJECUCION))
                .isInstanceOf(ProjectTransitionException.class);
        assertThatThrownBy(() -> persisted(ProjectStatus.PLANIFICADO, 1).changeStatus(ProjectStatus.FINALIZADO))
                .isInstanceOf(ProjectTransitionException.class);
    }

    @Test
    void r9_createUsesFactoryDefaultsAndPendingInitialRevision() {
        Project created = Project.create("  Nuevo  ", "  desc  ", 5, new BigDecimal("99.9"), 30, null, CHIEF, NOW);

        assertThat(created.isNew()).isTrue();
        assertThat(created.name()).isEqualTo("Nuevo");
        assertThat(created.description()).isEqualTo("desc");
        assertThat(created.managerUserId()).isEqualTo(5);
        assertThat(created.status()).isEqualTo(ProjectStatus.PLANIFICADO);
        assertThat(created.budgetBob()).isEqualByComparingTo("99.90");
        assertThat(created.estimatedDurationMinutes()).isEqualTo(30);
        assertThat(created.createdByUserId()).isEqualTo(CHIEF);
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.updatedAt()).isEqualTo(NOW);
        assertThat(created.lastRevisionNumber()).isZero();
        assertThat(created.pendingRevisions()).singleElement().satisfies(r -> {
            assertThat(r.revisionNumber()).isEqualTo(1);
            assertThat(r.reason()).isEqualTo("Plan inicial");
        });
        assertThat(created.version()).isEqualTo(com.hardcoders.taskmanager.domain.model.VersionToken.of(NOW));
    }

    @Test
    void r9_restoreAndOfRebuildPersistedStateWithoutPendingRevisions() {
        Project restored = Project.restore(11, "Repo", "d", 3, ProjectStatus.EN_EJECUCION,
                new BigDecimal("10.00"), 45, CHIEF, NOW.minusSeconds(5), NOW, 2);
        Project viaOf = Project.of(12, "Of", null, 4, ProjectStatus.PLANIFICADO,
                new BigDecimal("1.00"), null, CHIEF, NOW.minusSeconds(2), NOW.minusSeconds(1), 0);

        assertThat(restored.isNew()).isFalse();
        assertThat(restored.id()).isEqualTo(11);
        assertThat(restored.pendingRevisions()).isEmpty();
        assertThat(restored.lastRevisionNumber()).isEqualTo(2);
        assertThat(viaOf.estimatedDurationMinutes()).isZero();
        assertThat(viaOf.lastRevisionNumber()).isZero();
    }

    @Test
    void r9_editDetailsNormalizesAndVersionChecksApply() {
        Project project = persisted(ProjectStatus.PLANIFICADO, 1);
        project.verifyVersion(null);
        project.verifyVersion(project.version());
        project.editDetails("  Renombrado  ", "  texto  ");
        assertThat(project.name()).isEqualTo("Renombrado");
        assertThat(project.description()).isEqualTo("texto");
        assertThatThrownBy(() -> project.verifyVersion(project.version() - 1))
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.VersionConflictException.class);
    }

    private static Project persisted(ProjectStatus status, int revisions) {
        return Project.restore(10, "Proyecto", null, 2, status, new BigDecimal("1000.00"), 600, CHIEF,
                NOW.minusSeconds(3600), NOW.minusSeconds(60), revisions);
    }
}
