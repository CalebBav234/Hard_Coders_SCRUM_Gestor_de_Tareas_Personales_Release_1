package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.port.in.QueryProjectsUseCase;
import com.hardcoders.taskmanager.application.result.PlanRevisionResult;
import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.policy.ProjectAccessPolicy;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;

import java.util.List;

/** Consultas de proyectos con el filtrado por actor hecho en el servidor. */
public class ProjectQueryService implements QueryProjectsUseCase {

    private final ProjectRepository projects;
    private final UserDirectory users;
    private final CurrentActor currentActor;
    private final ProjectResultAssembler assembler;

    public ProjectQueryService(ProjectRepository projects, UserDirectory users, CurrentActor currentActor) {
        this.projects = projects;
        this.users = users;
        this.currentActor = currentActor;
        this.assembler = new ProjectResultAssembler(projects, users);
    }

    @Override
    public List<ProjectResult> listProjects() {
        Actor actor = currentActor.current();
        if (!actor.active()) {
            return List.of();
        }
        List<Project> visible = actor.chief() ? projects.findAll() : projects.findManagedBy(actor.userId());
        return assembler.toResults(actor, visible);
    }

    @Override
    public ProjectResult getProject(Integer projectId) {
        Actor actor = currentActor.current();
        return assembler.toResult(actor, loadVisible(actor, projectId));
    }

    @Override
    public List<PlanRevisionResult> planHistory(Integer projectId) {
        Actor actor = currentActor.current();
        Project project = loadVisible(actor, projectId);
        return assembler.toRevisionResults(projects.planHistory(project.id()));
    }

    @Override
    public List<UserSummaryResult> eligibleManagers() {
        ProjectAccessPolicy.requireChief(currentActor.current(), "consultar los encargados disponibles");
        return users.findActive().stream().map(ProjectResultAssembler::summary).toList();
    }

    private Project loadVisible(Actor actor, Integer projectId) {
        return projects.findById(projectId)
                .filter(project -> ProjectAccessPolicy.canView(actor, project))
                .orElseThrow(() -> new NotFoundException(Project.ENTITY, projectId));
    }
}
