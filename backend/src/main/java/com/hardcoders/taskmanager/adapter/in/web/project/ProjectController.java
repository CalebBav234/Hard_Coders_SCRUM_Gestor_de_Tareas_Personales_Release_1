package com.hardcoders.taskmanager.adapter.in.web.project;

import com.hardcoders.taskmanager.adapter.in.web.project.ProjectRequests.ChangeProjectStatusRequest;
import com.hardcoders.taskmanager.adapter.in.web.project.ProjectRequests.CreateProjectRequest;
import com.hardcoders.taskmanager.adapter.in.web.project.ProjectRequests.DelegateProjectRequest;
import com.hardcoders.taskmanager.adapter.in.web.project.ProjectRequests.ReplanProjectRequest;
import com.hardcoders.taskmanager.adapter.in.web.project.ProjectRequests.UpdateProjectDetailsRequest;
import com.hardcoders.taskmanager.application.command.ChangeProjectStatusCommand;
import com.hardcoders.taskmanager.application.command.CreateProjectCommand;
import com.hardcoders.taskmanager.application.command.DelegateProjectCommand;
import com.hardcoders.taskmanager.application.command.ReplanProjectCommand;
import com.hardcoders.taskmanager.application.command.UpdateProjectDetailsCommand;
import com.hardcoders.taskmanager.application.port.in.ManageProjectsUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryProjectsUseCase;
import com.hardcoders.taskmanager.adapter.in.web.project.ProjectResponses.PlanRevisionResponse;
import com.hardcoders.taskmanager.adapter.in.web.project.ProjectResponses.ProjectResponse;
import com.hardcoders.taskmanager.adapter.in.web.project.ProjectResponses.UserSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Épica 1 (Jefe): US1.1 ciclo de vida y presupuesto, US1.2 delegación.
 * Los permisos los decide el caso de uso con el actor del token; aquí solo se traduce HTTP.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ManageProjectsUseCase manageProjects;
    private final QueryProjectsUseCase queries;

    public ProjectController(ManageProjectsUseCase manageProjects, QueryProjectsUseCase queries) {
        this.manageProjects = manageProjects;
        this.queries = queries;
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return queries.listProjects().stream().map(ProjectResponse::from).toList();
    }

    /** Usuarios activos a los que el Jefe puede delegar. */
    @GetMapping("/eligible-managers")
    public List<UserSummaryResponse> eligibleManagers() {
        return queries.eligibleManagers().stream().map(UserSummaryResponse::from).toList();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable Integer projectId) {
        return ProjectResponse.from(queries.getProject(projectId));
    }

    @GetMapping("/{projectId}/plan-history")
    public List<PlanRevisionResponse> planHistory(@PathVariable Integer projectId) {
        return queries.planHistory(projectId).stream().map(PlanRevisionResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return ProjectResponse.from(manageProjects.create(new CreateProjectCommand(request.name(), request.description(),
                request.managerUserId(), request.budgetBob(), request.estimatedDurationMinutes(), request.reason())));
    }

    @PutMapping("/{projectId}")
    public ProjectResponse updateDetails(@PathVariable Integer projectId,
                                       @Valid @RequestBody UpdateProjectDetailsRequest request) {
        return ProjectResponse.from(manageProjects.updateDetails(new UpdateProjectDetailsCommand(projectId, request.name(),
                request.description(), request.version())));
    }

    @PutMapping("/{projectId}/plan")
    public ProjectResponse replan(@PathVariable Integer projectId, @Valid @RequestBody ReplanProjectRequest request) {
        return ProjectResponse.from(manageProjects.replan(new ReplanProjectCommand(projectId, request.budgetBob(),
                request.estimatedDurationMinutes(), request.reason(), request.version())));
    }

    @PutMapping("/{projectId}/status")
    public ProjectResponse changeStatus(@PathVariable Integer projectId,
                                      @Valid @RequestBody ChangeProjectStatusRequest request) {
        return ProjectResponse.from(manageProjects.changeStatus(new ChangeProjectStatusCommand(projectId, request.status(),
                request.version())));
    }

    @PostMapping("/{projectId}/delegate")
    public ProjectResponse delegate(@PathVariable Integer projectId,
                                  @Valid @RequestBody DelegateProjectRequest request) {
        return ProjectResponse.from(manageProjects.delegate(new DelegateProjectCommand(projectId, request.managerUserId(),
                request.reason(), request.version())));
    }
}
