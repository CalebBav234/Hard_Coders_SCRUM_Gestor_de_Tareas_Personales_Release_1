package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.adapter.in.web.project.ProjectResponses;
import com.hardcoders.taskmanager.application.command.AssignmentCommands.AssignWorkerCommand;
import com.hardcoders.taskmanager.application.command.AssignmentCommands.UnassignWorkerCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.ActivityStatusCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateStageCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.StageStatusCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateStageCommand;
import com.hardcoders.taskmanager.application.port.in.ManageAssignmentsUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageHierarchyUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryHierarchyUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryProjectsUseCase;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.StageResult;
import com.hardcoders.taskmanager.application.result.TaskAssignmentResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Jerarquía Proyecto → Etapa → Actividad → Tarea y asignaciones (Épica 2).
 * Cada ruta valida en el servidor que el recurso pertenezca al proyecto indicado y que el actor
 * del token pueda gestionarlo; un identificador ajeno responde 404.
 */
@RestController
@RequestMapping("/api")
public class HierarchyController {

    private final ManageHierarchyUseCase manageHierarchy;
    private final QueryHierarchyUseCase queryHierarchy;
    private final ManageAssignmentsUseCase manageAssignments;
    private final QueryProjectsUseCase queryProjects;

    public HierarchyController(ManageHierarchyUseCase manageHierarchy, QueryHierarchyUseCase queryHierarchy,
                               ManageAssignmentsUseCase manageAssignments, QueryProjectsUseCase queryProjects) {
        this.manageHierarchy = manageHierarchy;
        this.queryHierarchy = queryHierarchy;
        this.manageAssignments = manageAssignments;
        this.queryProjects = queryProjects;
    }

    // ---------------------------------------------------------------- etapas

    @GetMapping("/projects/{projectId}/stages")
    public List<TaskResponses.StageResponse> stages(@PathVariable Integer projectId) {
        return queryHierarchy.stagesOf(projectId).stream().map(TaskResponses.StageResponse::from).toList();
    }

    @PostMapping("/projects/{projectId}/stages")
    public ResponseEntity<TaskResponses.StageResponse> createStage(
            @PathVariable Integer projectId,
            @Valid @RequestBody HierarchyRequests.CreateStageRequest request) {
        StageResult created = manageHierarchy.createStage(
                new CreateStageCommand(projectId, request.name(), request.description()));
        return ResponseEntity.created(URI.create("/api/stages/" + created.stageId()))
                .body(TaskResponses.StageResponse.from(created));
    }

    @PutMapping("/stages/{stageId}")
    public TaskResponses.StageResponse updateStage(@PathVariable Integer stageId,
                                                   @Valid @RequestBody HierarchyRequests.UpdateStageRequest request) {
        return TaskResponses.StageResponse.from(manageHierarchy.updateStage(
                new UpdateStageCommand(stageId, request.name(), request.description(), request.version())));
    }

    @PutMapping("/stages/{stageId}/status")
    public TaskResponses.StageResponse changeStageStatus(
            @PathVariable Integer stageId,
            @Valid @RequestBody HierarchyRequests.StatusRequest request) {
        return TaskResponses.StageResponse.from(manageHierarchy.changeStageStatus(
                new StageStatusCommand(stageId, request.status(), request.version())));
    }

    // ------------------------------------------------------------ actividades

    @GetMapping("/stages/{stageId}/activities")
    public List<TaskResponses.ActivityResponse> activities(@PathVariable Integer stageId) {
        return queryHierarchy.activitiesOf(stageId).stream().map(TaskResponses.ActivityResponse::from).toList();
    }

    @PostMapping("/stages/{stageId}/activities")
    public ResponseEntity<TaskResponses.ActivityResponse> createActivity(
            @PathVariable Integer stageId,
            @Valid @RequestBody HierarchyRequests.CreateActivityRequest request) {
        ActivityResult created = manageHierarchy.createActivity(
                new CreateActivityCommand(stageId, request.name(), request.description()));
        return ResponseEntity.created(URI.create("/api/activities/" + created.activityId()))
                .body(TaskResponses.ActivityResponse.from(created));
    }

    @PutMapping("/activities/{activityId}")
    public TaskResponses.ActivityResponse updateActivity(
            @PathVariable Integer activityId,
            @Valid @RequestBody HierarchyRequests.UpdateActivityRequest request) {
        return TaskResponses.ActivityResponse.from(manageHierarchy.updateActivity(
                new UpdateActivityCommand(activityId, request.name(), request.description(), request.version())));
    }

    @PutMapping("/activities/{activityId}/status")
    public TaskResponses.ActivityResponse changeActivityStatus(
            @PathVariable Integer activityId,
            @Valid @RequestBody HierarchyRequests.StatusRequest request) {
        return TaskResponses.ActivityResponse.from(manageHierarchy.changeActivityStatus(
                new ActivityStatusCommand(activityId, request.status(), request.version())));
    }

    @GetMapping("/activities/{activityId}/tasks")
    public List<TaskResponses.TaskResponse> tasksOfActivity(@PathVariable Integer activityId) {
        return toResponses(queryHierarchy.tasksOf(activityId));
    }

    // ------------------------------------------------------------- asignaciones

    @PostMapping("/tasks/{taskId}/assignments")
    public ResponseEntity<TaskResponses.TaskAssignmentResponse> assign(
            @PathVariable Integer taskId,
            @Valid @RequestBody HierarchyRequests.AssignWorkerRequest request) {
        TaskAssignmentResult result = manageAssignments.assign(
                new AssignWorkerCommand(taskId, request.workerUserId(), request.reason()));
        return ResponseEntity.ok(TaskResponses.TaskAssignmentResponse.from(result));
    }

    @DeleteMapping("/tasks/{taskId}/assignments/{workerUserId}")
    public ResponseEntity<Void> unassign(@PathVariable Integer taskId, @PathVariable Integer workerUserId) {
        manageAssignments.unassign(new UnassignWorkerCommand(taskId, workerUserId, null));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks/{taskId}/assignments")
    public List<TaskResponses.TaskAssignmentResponse> assignments(@PathVariable Integer taskId) {
        return manageAssignments.assignmentsForTask(taskId).stream()
                .map(TaskResponses.TaskAssignmentResponse::from).toList();
    }

    /** Usuarios activos que el Encargado puede asignar en el proyecto. */
    @GetMapping("/projects/{projectId}/workers")
    public List<ProjectResponses.UserSummaryResponse> workers(@PathVariable Integer projectId) {
        return manageAssignments.assignableWorkers(projectId).stream()
                .map(ProjectResponses.UserSummaryResponse::from).toList();
    }

    /** Proyectos válidos para el selector de la interfaz, con los permisos del actor. */
    @GetMapping("/projects/manageable")
    public List<ProjectResponses.ProjectResponse> manageableProjects() {
        return queryProjects.listProjects().stream()
                .filter(project -> project.canManageHierarchy())
                .map(ProjectResponses.ProjectResponse::from)
                .toList();
    }

    static List<TaskResponses.TaskResponse> toResponses(List<TaskResult> results) {
        return results.stream()
                .map(result -> TaskResponses.TaskResponse.from(result, result.assignedWorkerIds()))
                .toList();
    }
}
