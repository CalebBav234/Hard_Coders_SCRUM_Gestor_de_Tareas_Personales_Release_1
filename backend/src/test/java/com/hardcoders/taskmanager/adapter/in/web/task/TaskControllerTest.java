package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.application.port.in.ManageSubtasksUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryTasksUseCase;
import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.AssignedTaskResult;
import com.hardcoders.taskmanager.application.result.StatusEventResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskHistoryResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private QueryTasksUseCase queryTasks;
    private ManageTasksUseCase manageTasks;
    private ManageSubtasksUseCase manageSubtasks;
    private TaskController controller;

    @BeforeEach
    void setUp() {
        queryTasks = mock(QueryTasksUseCase.class);
        manageTasks = mock(ManageTasksUseCase.class);
        manageSubtasks = mock(ManageSubtasksUseCase.class);
        controller = new TaskController(queryTasks, manageTasks, manageSubtasks);

        when(queryTasks.listTasks(7, null)).thenReturn(List.of(task()));
        when(queryTasks.listAssignedTasks(null)).thenReturn(List.of(assignedTask()));
        when(queryTasks.getTask(31)).thenReturn(task());
        when(queryTasks.subtasksOfTask(31)).thenReturn(List.of(subtask()));
        when(queryTasks.taskHistory(7, null)).thenReturn(List.of(
                new TaskHistoryResult(task(), null, List.of(new StatusEventResult(1,
                        "EN_PROGRESO", "COMPLETADA", "Listo", NOW)))));
        when(queryTasks.subtaskHistory(7, null)).thenReturn(List.of(subtask()));
        when(queryTasks.activities(11)).thenReturn(List.of(activity()));
        when(queryTasks.getAssignedTask(31)).thenReturn(assignedTask());

        when(manageTasks.create(any())).thenReturn(task());
        when(manageTasks.update(any())).thenReturn(task());
        when(manageTasks.changeActivity(any())).thenReturn(task());
        when(manageTasks.updatePlan(any())).thenReturn(task());
        when(manageTasks.start(any())).thenReturn(task());
        when(manageTasks.pause(any())).thenReturn(task());
        when(manageTasks.complete(any())).thenReturn(task());
        when(manageTasks.reopen(any())).thenReturn(task());

        when(manageSubtasks.create(any())).thenReturn(subtask());
        when(manageSubtasks.update(any())).thenReturn(subtask());
        when(manageSubtasks.start(any())).thenReturn(subtask());
        when(manageSubtasks.pause(any())).thenReturn(subtask());
        when(manageSubtasks.complete(any())).thenReturn(subtask());
        when(manageSubtasks.reopen(any())).thenReturn(subtask());
    }

    @Test
    void mapsTaskReadAndWriteRoutes() {
        assertThat(controller.listTasks(7, null)).hasSize(1);
        assertThat(controller.assignedTasks(null)).hasSize(1);
        assertThat(controller.getTask(31).taskId()).isEqualTo(31);
        assertThat(controller.createTask(new TaskRequests.CreateTaskRequest(
                21, "Tarea", "MEDIA", BigDecimal.ZERO, 60)).getStatusCode().value()).isEqualTo(201);
        assertThat(controller.updateTask(31,
                new TaskRequests.UpdateTaskRequest("Editada", null, "ALTA", 100L)).taskId()).isEqualTo(31);
        assertThat(controller.changeActivity(31,
                new TaskRequests.ChangeActivityRequest(22, 100L)).taskId()).isEqualTo(31);
        assertThat(controller.updatePlan(31,
                new TaskRequests.UpdateTaskPlanRequest(BigDecimal.ONE, 90, "Plan", 100L)).taskId())
                .isEqualTo(31);
        assertThat(controller.start(31, new TaskRequests.TransitionRequest(100L, "Inicio")).taskId()).isEqualTo(31);
        assertThat(controller.pause(31, new TaskRequests.TransitionRequest(100L, null)).taskId()).isEqualTo(31);
        assertThat(controller.complete(31, new TaskRequests.TransitionRequest(100L, "Listo")).taskId())
                .isEqualTo(31);
        assertThat(controller.reopen(31, new TaskRequests.TransitionRequest(100L, "Reabrir")).taskId())
                .isEqualTo(31);
        assertThat(controller.deleteTask(31, 100L).getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void mapsSubtaskHistoryAndActivityRoutes() {
        assertThat(controller.subtasks(31)).hasSize(1);
        assertThat(controller.createSubtask(31,
                new TaskRequests.CreateSubtaskRequest("Hija", null)).getStatusCode().value()).isEqualTo(201);
        assertThat(controller.updateSubtask(41,
                new TaskRequests.UpdateSubtaskRequest("Editada", "Detalle", 100L)).subtaskId()).isEqualTo(41);
        assertThat(controller.startSubtask(41,
                new TaskRequests.TransitionRequest(100L, null)).subtaskId()).isEqualTo(41);
        assertThat(controller.pauseSubtask(41,
                new TaskRequests.TransitionRequest(100L, null)).subtaskId()).isEqualTo(41);
        assertThat(controller.completeSubtask(41,
                new TaskRequests.TransitionRequest(100L, "Listo")).subtaskId()).isEqualTo(41);
        assertThat(controller.reopenSubtask(41,
                new TaskRequests.TransitionRequest(100L, "Reabrir")).subtaskId()).isEqualTo(41);
        assertThat(controller.deleteSubtask(41, 100L).getStatusCode().value()).isEqualTo(204);
        assertThat(controller.taskHistory(7, null)).hasSize(1);
        assertThat(controller.subtaskHistory(7, null)).hasSize(1);
        assertThat(controller.activities(11)).extracting(TaskResponses.ActivityResponse::activityId)
                .containsExactly(21);
        assertThat(controller.assignedTask(31).taskId()).isEqualTo(31);
    }

    private static TaskResult task() {
        return new TaskResult(31, 21, "Actividad", "Tarea", null, "PENDIENTE", "MEDIA",
                BigDecimal.ZERO, 60, null, 0, null, null, null, NOW, NOW, 100L,
                List.of(4), List.of(subtask()));
    }

    private static SubtaskResult subtask() {
        return new SubtaskResult(41, 31, "Subtarea", null, "PENDIENTE", null, null, NOW, NOW, 100L);
    }

    private static AssignedTaskResult assignedTask() {
        return new AssignedTaskResult(31, 7, 21, "Actividad", "Tarea", null, "EN_PROGRESO", "MEDIA",
                10, 30, NOW, null, NOW, NOW, 100L, List.of(subtask()));
    }

    private static ActivityResult activity() {
        return new ActivityResult(21, 11, "Actividad", null, "PENDIENTE", NOW, NOW, 100L);
    }
}
