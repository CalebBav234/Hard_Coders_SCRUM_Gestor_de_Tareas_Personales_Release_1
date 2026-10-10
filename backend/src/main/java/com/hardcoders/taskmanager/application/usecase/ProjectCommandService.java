package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.ChangeProjectStatusCommand;
import com.hardcoders.taskmanager.application.command.CreateProjectCommand;
import com.hardcoders.taskmanager.application.command.DelegateProjectCommand;
import com.hardcoders.taskmanager.application.command.ReplanProjectCommand;
import com.hardcoders.taskmanager.application.command.UpdateProjectDetailsCommand;
import com.hardcoders.taskmanager.application.port.in.ManageProjectsUseCase;
import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.policy.DelegationPolicy;
import com.hardcoders.taskmanager.domain.policy.ProjectAccessPolicy;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/** Casos de uso de la Épica 1 (Jefe). La transacción la aplica la configuración. */
public class ProjectCommandService implements ManageProjectsUseCase {

    private final ProjectRepository projects;
    private final UserDirectory users;
    private final CurrentActor currentActor;
    private final Clock clock;
    private final ProjectResultAssembler assembler;

    public ProjectCommandService(ProjectRepository projects, UserDirectory users, CurrentActor currentActor,
                                 Clock clock) {
        this.projects = projects;
        this.users = users;
        this.currentActor = currentActor;
        this.clock = clock;
        this.assembler = new ProjectResultAssembler(projects, users);
    }

    @Override
    public ProjectResult create(CreateProjectCommand command) {
        Actor actor = currentActor.current();
        ProjectAccessPolicy.requireChief(actor, "crear proyectos");
        UserAccount manager = command.managerUserId() == null ? null
                : users.findById(command.managerUserId()).orElse(null);
        DelegationPolicy.requireEligibleManager(manager);
        Project project = Project.create(command.name(), command.description(), manager.userId(),
                command.budgetBob(), command.estimatedDurationMinutes(), command.reason(), actor.userId(),
                Instant.now(clock));
        return assembler.toResult(actor, projects.save(project));
    }

    @Override
    public ProjectResult updateDetails(UpdateProjectDetailsCommand command) {
        Actor actor = currentActor.current();
        Project project = loadVisible(actor, command.projectId());
        ProjectAccessPolicy.requireCanEditDetails(actor, project);
        project.verifyVersion(command.version());
        project.editDetails(command.name(), command.description());
        return assembler.toResult(actor, projects.save(project));
    }

    @Override
    public ProjectResult replan(ReplanProjectCommand command) {
        Actor actor = currentActor.current();
        Project project = loadVisible(actor, command.projectId());
        ProjectAccessPolicy.requireChief(actor, "cambiar el presupuesto o la duración del proyecto");
        project.verifyVersion(command.version());
        project.replan(command.budgetBob(), command.estimatedDurationMinutes(),
                projects.allocatedBudget(project.id()), command.reason(), actor.userId(), Instant.now(clock));
        return assembler.toResult(actor, projects.save(project));
    }

    @Override
    public ProjectResult changeStatus(ChangeProjectStatusCommand command) {
        Actor actor = currentActor.current();
        Project project = loadVisible(actor, command.projectId());
        ProjectAccessPolicy.requireChief(actor, "cambiar el estado del proyecto");
        project.verifyVersion(command.version());
        project.changeStatus(parseStatus(command.status()));
        return assembler.toResult(actor, projects.save(project));
    }

    @Override
    public ProjectResult delegate(DelegateProjectCommand command) {
        Actor actor = currentActor.current();
        Project project = loadVisible(actor, command.projectId());
        DelegationPolicy.requireCanDelegate(actor);
        project.verifyVersion(command.version());
        UserAccount newManager = command.managerUserId() == null ? null
                : users.findById(command.managerUserId()).orElse(null);
        DelegationPolicy.requireEligibleManager(newManager);
        UserAccount previous = users.findById(project.managerUserId()).orElse(null);
        project.delegateTo(newManager, previous, command.reason(), actor.userId(), Instant.now(clock));
        return assembler.toResult(actor, projects.save(project));
    }

    /** Un proyecto que el actor no puede ver se trata como inexistente (no se filtra su existencia). */
    private Project loadVisible(Actor actor, Integer projectId) {
        return projects.findForUpdate(projectId)
                .filter(project -> ProjectAccessPolicy.canView(actor, project))
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
    }

    private static ProjectStatus parseStatus(String value) {
        try {
            return ProjectStatus.valueOf(value == null ? "" : value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("Estado de proyecto desconocido: " + value);
        }
    }
}
