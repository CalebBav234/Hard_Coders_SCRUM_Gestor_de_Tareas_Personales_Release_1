package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.HierarchyCommands.ActivityStatusCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateStageCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.StageStatusCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateStageCommand;
import com.hardcoders.taskmanager.application.port.in.ManageHierarchyUseCase;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.StageResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.Stage;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.policy.WbsHierarchyPolicy;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.HierarchyRepository;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/**
 * US2.1 / US2.2: altas y ediciones de etapas y actividades. El caso de uso comprueba, en cada
 * operación, que la etapa o la actividad pertenece al proyecto que el actor puede gestionar; un
 * identificador de otro proyecto devuelve 404 (AC-US2.1-02, AC-US2.2-02).
 */
public class HierarchyCommandService implements ManageHierarchyUseCase {

    private final ProjectRepository projects;
    private final HierarchyRepository hierarchy;
    private final CurrentActor currentActor;
    private final Clock clock;

    public HierarchyCommandService(ProjectRepository projects, HierarchyRepository hierarchy,
                                   CurrentActor currentActor, Clock clock) {
        this.projects = projects;
        this.hierarchy = hierarchy;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Override
    public StageResult createStage(CreateStageCommand command) {
        Actor actor = currentActor.current();
        Project project = loadManageable(actor, command.projectId());
        WbsHierarchyPolicy.requireOpen(project, "crear etapas");
        String name = WbsHierarchyPolicy.requireName(command.name(), 160, "nombre");
        String description = WbsHierarchyPolicy.requireDescription(command.description(), 2000, "descripción");
        Stage stage = Stage.create(project.id(), name, description, Instant.now(clock));
        return toResult(hierarchy.save(stage));
    }

    @Override
    public StageResult updateStage(UpdateStageCommand command) {
        Actor actor = currentActor.current();
        Stage stage = hierarchy.findStageForUpdate(command.stageId())
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, command.stageId()));
        Project project = requireManageableProject(actor, stage);
        WbsHierarchyPolicy.requireOpen(project, "editar etapas");
        stage.verifyVersion(command.version());
        String name = WbsHierarchyPolicy.requireName(command.name(), 160, "nombre");
        String description = WbsHierarchyPolicy.requireDescription(command.description(), 2000, "descripción");
        stage.edit(name, description);
        return toResult(hierarchy.save(stage));
    }

    @Override
    public StageResult changeStageStatus(StageStatusCommand command) {
        Actor actor = currentActor.current();
        Stage stage = hierarchy.findStageForUpdate(command.stageId())
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, command.stageId()));
        Project project = requireManageableProject(actor, stage);
        WbsHierarchyPolicy.requireOpen(project, "cambiar el estado de la etapa");
        stage.verifyVersion(command.version());
        apply(stage, parse(command.status()));
        return toResult(hierarchy.save(stage));
    }

    @Override
    public ActivityResult createActivity(CreateActivityCommand command) {
        Actor actor = currentActor.current();
        Stage stage = hierarchy.findStage(command.stageId())
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, command.stageId()));
        Project project = requireManageableProject(actor, stage);
        WbsHierarchyPolicy.requireOpen(project, "crear actividades");
        String name = WbsHierarchyPolicy.requireName(command.name(), 160, "nombre");
        String description = WbsHierarchyPolicy.requireDescription(command.description(), 2000, "descripción");
        Activity activity = Activity.create(stage.id(), name, description, Instant.now(clock));
        return toResult(hierarchy.save(activity));
    }

    @Override
    public ActivityResult updateActivity(UpdateActivityCommand command) {
        Actor actor = currentActor.current();
        Activity activity = hierarchy.findActivityForUpdate(command.activityId())
                .orElseThrow(() -> new NotFoundException(Activity.ENTITY, command.activityId()));
        Stage stage = hierarchy.findStage(activity.stageId())
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, activity.stageId()));
        Project project = requireManageableProject(actor, stage);
        WbsHierarchyPolicy.requireOpen(project, "editar actividades");
        activity.verifyVersion(command.version());
        String name = WbsHierarchyPolicy.requireName(command.name(), 160, "nombre");
        String description = WbsHierarchyPolicy.requireDescription(command.description(), 2000, "descripción");
        activity.edit(name, description);
        return toResult(hierarchy.save(activity));
    }

    @Override
    public ActivityResult changeActivityStatus(ActivityStatusCommand command) {
        Actor actor = currentActor.current();
        Activity activity = hierarchy.findActivityForUpdate(command.activityId())
                .orElseThrow(() -> new NotFoundException(Activity.ENTITY, command.activityId()));
        Stage stage = hierarchy.findStage(activity.stageId())
                .orElseThrow(() -> new NotFoundException(Stage.ENTITY, activity.stageId()));
        Project project = requireManageableProject(actor, stage);
        WbsHierarchyPolicy.requireOpen(project, "cambiar el estado de la actividad");
        activity.verifyVersion(command.version());
        apply(activity, parse(command.status()));
        return toResult(hierarchy.save(activity));
    }

    private Project loadManageable(Actor actor, Integer projectId) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
        WbsHierarchyPolicy.requireCanManage(actor, project, "gestionar este proyecto");
        return project;
    }

    /** La etapa debe pertenecer a un proyecto que el actor pueda gestionar. */
    private Project requireManageableProject(Actor actor, Stage stage) {
        Project project = projects.findById(stage.projectId())
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, stage.projectId()));
        WbsHierarchyPolicy.requireSameProject(stage.projectId(), project.id(), Project.ENTITY);
        WbsHierarchyPolicy.requireCanManage(actor, project, "gestionar este proyecto");
        return project;
    }

    private static WorkStatus parse(String value) {
        try {
            return WorkStatus.valueOf(value == null ? "" : value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Estado desconocido: " + value);
        }
    }

    private static void apply(Stage stage, WorkStatus target) {
        switch (target) {
            case EN_PROGRESO -> stage.start();
            case PENDIENTE -> stage.pause();
            case COMPLETADA -> stage.complete();
            case CANCELADA -> stage.cancel();
        }
    }

    private static void apply(Activity activity, WorkStatus target) {
        switch (target) {
            case EN_PROGRESO -> activity.start();
            case PENDIENTE -> activity.pause();
            case COMPLETADA -> activity.complete();
            case CANCELADA -> activity.cancel();
        }
    }

    private static StageResult toResult(Stage stage) {
        return new StageResult(stage.id(), stage.projectId(), stage.name(), stage.description(),
                stage.status().name(), stage.createdAt(), stage.updatedAt(), stage.version());
    }

    private static ActivityResult toResult(Activity activity) {
        return new ActivityResult(activity.id(), activity.stageId(), activity.name(), activity.description(),
                activity.status().name(), activity.createdAt(), activity.updatedAt(), activity.version());
    }
}
