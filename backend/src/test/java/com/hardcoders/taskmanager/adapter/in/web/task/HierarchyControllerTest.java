package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.application.command.HierarchyCommands.ActivityStatusCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateStageCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.StageStatusCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateStageCommand;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.application.result.StageResult;
import com.hardcoders.taskmanager.application.result.TaskAssignmentResult;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.application.port.in.ManageAssignmentsUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageHierarchyUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryHierarchyUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryProjectsUseCase;
import com.hardcoders.taskmanager.domain.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HierarchyControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private ManageHierarchyUseCase manageHierarchy;
    private QueryHierarchyUseCase queryHierarchy;
    private ManageAssignmentsUseCase manageAssignments;
    private QueryProjectsUseCase queryProjects;
    private HierarchyController controller;

    @BeforeEach
    void setUp() {
        manageHierarchy = mock(ManageHierarchyUseCase.class);
        queryHierarchy = mock(QueryHierarchyUseCase.class);
        manageAssignments = mock(ManageAssignmentsUseCase.class);
        queryProjects = mock(QueryProjectsUseCase.class);
        controller = new HierarchyController(manageHierarchy, queryHierarchy, manageAssignments, queryProjects);
        when(manageHierarchy.createStage(any())).thenReturn(stage(11));
        when(manageHierarchy.updateStage(any())).thenReturn(stage(11));
        when(manageHierarchy.changeStageStatus(any())).thenReturn(stage(11));
        when(manageHierarchy.createActivity(any())).thenReturn(activity(21));
        when(manageHierarchy.updateActivity(any())).thenReturn(activity(21));
        when(manageHierarchy.changeActivityStatus(any())).thenReturn(activity(21));
        when(queryHierarchy.stagesOf(7)).thenReturn(List.of(stage(11)));
        when(queryHierarchy.activitiesOf(11)).thenReturn(List.of(activity(21)));
        when(queryHierarchy.tasksOf(21)).thenReturn(List.of());
        when(manageAssignments.assign(any())).thenReturn(assignment());
        when(manageAssignments.assignmentsForTask(31)).thenReturn(List.of(assignment()));
        when(manageAssignments.assignableWorkers(7)).thenReturn(List.of(
                new UserSummaryResult(3, "Pedro Rojas", "p@x.com", false, true)));
        when(queryProjects.listProjects()).thenReturn(List.of(project(7, false), project(8, true)));
    }

    @Test
    void mapsHierarchyReadsAndWritesToResponses() {
        assertThat(controller.stages(7)).extracting(TaskResponses.StageResponse::stageId).containsExactly(11);
        assertThat(controller.createStage(7, new HierarchyRequests.CreateStageRequest("Etapa", null))
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateStage(11, new HierarchyRequests.UpdateStageRequest("Etapa editada", null, 100L))
                .stageId()).isEqualTo(11);
        assertThat(controller.changeStageStatus(11,
                new HierarchyRequests.StatusRequest("EN_PROGRESO", 100L)).status()).isEqualTo("PENDIENTE");

        assertThat(controller.activities(11)).extracting(TaskResponses.ActivityResponse::activityId)
                .containsExactly(21);
        assertThat(controller.createActivity(11,
                new HierarchyRequests.CreateActivityRequest("Actividad", null)).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateActivity(21,
                new HierarchyRequests.UpdateActivityRequest("Actividad editada", null, 100L)).activityId())
                .isEqualTo(21);
        assertThat(controller.changeActivityStatus(21,
                new HierarchyRequests.StatusRequest("EN_PROGRESO", 100L)).status()).isEqualTo("PENDIENTE");
        assertThat(controller.tasksOfActivity(21)).isEmpty();

        verify(manageHierarchy).createStage(any(CreateStageCommand.class));
        verify(manageHierarchy).updateStage(any(UpdateStageCommand.class));
        verify(manageHierarchy).changeStageStatus(any(StageStatusCommand.class));
        verify(manageHierarchy).createActivity(any(CreateActivityCommand.class));
        verify(manageHierarchy).updateActivity(any(UpdateActivityCommand.class));
        verify(manageHierarchy).changeActivityStatus(any(ActivityStatusCommand.class));
    }

    @Test
    void mapsAssignmentRoutesAndFiltersProjectsByHierarchyPermission() {
        assertThat(controller.assign(31, new HierarchyRequests.AssignWorkerRequest(3, "Apoyo")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(controller.assignments(31)).hasSize(1);
        assertThat(controller.workers(7)).extracting(response -> response.userId()).containsExactly(3);
        assertThat(controller.manageableProjects()).extracting(response -> response.projectId()).containsExactly(8);
        assertThat(controller.unassign(31, 3).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    }

    private static StageResult stage(Integer id) {
        return new StageResult(id, 7, "Etapa", null, "PENDIENTE", NOW, NOW, 100L);
    }

    private static ActivityResult activity(Integer id) {
        return new ActivityResult(id, 11, "Actividad", null, "PENDIENTE", NOW, NOW, 100L);
    }

    private static TaskAssignmentResult assignment() {
        return new TaskAssignmentResult(31, "Tarea", 3, "Pedro Rojas", NOW, 1, "Ana Paz", "Apoyo");
    }

    private static ProjectResult project(Integer id, boolean canManage) {
        UserSummaryResult manager = new UserSummaryResult(2, "Ana Paz", "ana@x.com", false, true);
        UserSummaryResult chief = new UserSummaryResult(1, "Jefe", "jefe@x.com", true, true);
        return new ProjectResult(id, "Proyecto", null, "PLANIFICADO", false, new BigDecimal("1000.00"),
                BigDecimal.ZERO.setScale(2), 480, manager, chief, 1, NOW, NOW, 100L,
                true, false, false, canManage, List.of());
    }
}
