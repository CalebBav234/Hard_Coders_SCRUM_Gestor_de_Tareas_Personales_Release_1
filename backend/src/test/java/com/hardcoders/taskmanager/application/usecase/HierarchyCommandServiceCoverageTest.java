package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.HierarchyCommands.ActivityStatusCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateStageCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.StageStatusCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateStageCommand;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.StageResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.exception.ProjectTransitionException;
import com.hardcoders.taskmanager.domain.exception.VersionConflictException;
import com.hardcoders.taskmanager.domain.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HierarchyCommandServiceCoverageTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
    private InMemoryPorts ports;
    private HierarchyCommandService service;
    private Integer stageId;
    private Integer activityId;

    @BeforeEach
    void setUp() {
        ports = new InMemoryPorts(clock);
        ports.seedProject("Proyecto A", InMemoryPorts.ACTOR, new BigDecimal("1000.00"));
        stageId = ports.seedStage(InMemoryPorts.PROJECT, "Etapa inicial").id();
        activityId = ports.seedActivity(stageId, "Actividad inicial").id();
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, false, true);
        service = new HierarchyCommandService(ports.projectRepository, ports.hierarchy, ports.actor, clock);
    }

    @Test
    void createsAndUpdatesStagesAndActivitiesWithNormalizedText() {
        StageResult stage = service.createStage(new CreateStageCommand(InMemoryPorts.PROJECT, "  Diseño  ", "  Fase 1  "));
        assertThat(stage.name()).isEqualTo("Diseño");
        assertThat(stage.description()).isEqualTo("Fase 1");

        StageResult updatedStage = service.updateStage(
                new UpdateStageCommand(stage.stageId(), "  Construcción  ", null, stage.version()));
        assertThat(updatedStage.name()).isEqualTo("Construcción");
        assertThat(updatedStage.description()).isNull();

        ActivityResult activity = service.createActivity(
                new CreateActivityCommand(stageId, "  Preparación  ", "  Inicio  "));
        ActivityResult updatedActivity = service.updateActivity(
                new UpdateActivityCommand(activity.activityId(), "Ejecución", "Trabajo", activity.version()));
        assertThat(updatedActivity.name()).isEqualTo("Ejecución");
        assertThat(updatedActivity.description()).isEqualTo("Trabajo");
    }

    @Test
    void rejectsBlankNamesAndUnknownOrClosedProjects() {
        assertThatThrownBy(() -> service.createStage(new CreateStageCommand(InMemoryPorts.PROJECT, "  ", null)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.createActivity(new CreateActivityCommand(stageId, null, null)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.createStage(new CreateStageCommand(999, "Etapa", null)))
                .isInstanceOf(NotFoundException.class);

        ports.projects.get(InMemoryPorts.PROJECT).changeStatus(
                com.hardcoders.taskmanager.domain.model.ProjectStatus.EN_EJECUCION);
        ports.projects.get(InMemoryPorts.PROJECT).changeStatus(
                com.hardcoders.taskmanager.domain.model.ProjectStatus.FINALIZADO);
        assertThatThrownBy(() -> service.createActivity(new CreateActivityCommand(stageId, "Otra", null)))
                .isInstanceOf(ProjectTransitionException.class);
    }

    @Test
    void rejectsUnauthorizedActorsAndMissingHierarchyParents() {
        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        assertThatThrownBy(() -> service.createStage(new CreateStageCommand(InMemoryPorts.PROJECT, "Etapa", null)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.createActivity(new CreateActivityCommand(stageId, "Actividad", null)))
                .isInstanceOf(ForbiddenException.class);

        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        assertThatThrownBy(() -> service.updateStage(new UpdateStageCommand(999, "X", null, 0L)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.changeStageStatus(new StageStatusCommand(999, "PENDIENTE", 0L)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.updateActivity(new UpdateActivityCommand(999, "X", null, 0L)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.changeActivityStatus(new ActivityStatusCommand(999, "PENDIENTE", 0L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsUpdatesWithStaleVersionsAndUnknownStatuses() {
        StageResult stage = service.updateStage(
                new UpdateStageCommand(stageId, "Renombrada", null, ports.stages.get(stageId).version()));
        assertThatThrownBy(() -> service.updateStage(
                new UpdateStageCommand(stage.stageId(), "Obsoleta", null, 0L)))
                .isInstanceOf(VersionConflictException.class);
        assertThatThrownBy(() -> service.changeStageStatus(
                new StageStatusCommand(stage.stageId(), "NO_EXISTE", stage.version())))
                .isInstanceOf(DomainValidationException.class);

        ActivityResult activity = service.updateActivity(new UpdateActivityCommand(activityId,
                "Renombrada", null, ports.activities.get(activityId).version()));
        assertThatThrownBy(() -> service.updateActivity(new UpdateActivityCommand(
                activity.activityId(), "Obsoleta", null, 0L)))
                .isInstanceOf(VersionConflictException.class);
        assertThatThrownBy(() -> service.changeActivityStatus(
                new ActivityStatusCommand(activity.activityId(), null, activity.version())))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void appliesEachValidStageAndActivityStatusTransition() {
        StageResult stage = service.createStage(new CreateStageCommand(InMemoryPorts.PROJECT, "Estados", null));
        stage = service.changeStageStatus(new StageStatusCommand(stage.stageId(), " en_progreso ", stage.version()));
        assertThat(stage.status()).isEqualTo("EN_PROGRESO");
        stage = service.changeStageStatus(new StageStatusCommand(stage.stageId(), "PENDIENTE", stage.version()));
        stage = service.changeStageStatus(new StageStatusCommand(stage.stageId(), "EN_PROGRESO", stage.version()));
        stage = service.changeStageStatus(new StageStatusCommand(stage.stageId(), "COMPLETADA", stage.version()));
        assertThat(stage.status()).isEqualTo("COMPLETADA");

        StageResult cancelledStage = service.createStage(
                new CreateStageCommand(InMemoryPorts.PROJECT, "Cancelar", null));
        cancelledStage = service.changeStageStatus(new StageStatusCommand(
                cancelledStage.stageId(), "CANCELADA", cancelledStage.version()));
        assertThat(cancelledStage.status()).isEqualTo("CANCELADA");

        ActivityResult activity = service.createActivity(
                new CreateActivityCommand(stageId, "Estados", null));
        activity = service.changeActivityStatus(new ActivityStatusCommand(
                activity.activityId(), "EN_PROGRESO", activity.version()));
        activity = service.changeActivityStatus(new ActivityStatusCommand(
                activity.activityId(), "PENDIENTE", activity.version()));
        activity = service.changeActivityStatus(new ActivityStatusCommand(
                activity.activityId(), "EN_PROGRESO", activity.version()));
        activity = service.changeActivityStatus(new ActivityStatusCommand(
                activity.activityId(), "COMPLETADA", activity.version()));
        assertThat(activity.status()).isEqualTo("COMPLETADA");

        ActivityResult cancelledActivity = service.createActivity(
                new CreateActivityCommand(stageId, "Cancelar", null));
        cancelledActivity = service.changeActivityStatus(new ActivityStatusCommand(
                cancelledActivity.activityId(), "CANCELADA", cancelledActivity.version()));
        assertThat(cancelledActivity.status()).isEqualTo("CANCELADA");
    }
}
