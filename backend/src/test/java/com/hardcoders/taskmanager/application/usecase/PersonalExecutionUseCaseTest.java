package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.result.AssignedTaskResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Activity;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Subtask;
import com.hardcoders.taskmanager.domain.model.Task;
import com.hardcoders.taskmanager.domain.model.TaskAssignment;
import com.hardcoders.taskmanager.domain.model.TaskPriority;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.port.out.AssignmentRepository;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.HierarchyRepository;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalExecutionUseCaseTest {

    private static final Integer WORKER_ID = 17;
    private static final Integer TASK_ID = 5;
    private static final Integer SUBTASK_ID = 11;
    private static final Integer PROJECT_ID = 3;
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    private final TaskRepository tasks = mock(TaskRepository.class);
    private final SubtaskRepository subtasks = mock(SubtaskRepository.class);
    private final HierarchyRepository hierarchy = mock(HierarchyRepository.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final AssignmentRepository assignments = mock(AssignmentRepository.class);
    private Actor current = new Actor(WORKER_ID, false, true);
    private final CurrentActor currentActor = () -> current;
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private TaskCommandService taskCommands;
    private SubtaskCommandService subtaskCommands;
    private TaskQueryService taskQueries;
    private Task task;
    private Subtask subtask;

    @BeforeEach
    void setUp() {
        task = task(WorkStatus.PENDIENTE);
        subtask = Subtask.restore(SUBTASK_ID, TASK_ID, "Subtarea", null, WorkStatus.PENDIENTE,
                null, null, NOW.minusSeconds(60), NOW.minusSeconds(60));
        taskCommands = new TaskCommandService(tasks, subtasks, hierarchy, projects, assignments,
                currentActor, clock);
        subtaskCommands = new SubtaskCommandService(tasks, subtasks, hierarchy, projects, assignments,
                currentActor, clock);
        taskQueries = new TaskQueryService(tasks, subtasks, hierarchy, projects, assignments, currentActor);

        when(tasks.projectIdOf(TASK_ID)).thenReturn(Optional.of(PROJECT_ID));
        when(tasks.findAssignedForUpdate(WORKER_ID, TASK_ID)).thenReturn(Optional.of(task));
        when(tasks.findVisible(PROJECT_ID, TASK_ID)).thenReturn(Optional.of(task));
        when(tasks.save(task)).thenAnswer(invocation -> invocation.getArgument(0));
        when(subtasks.findVisibleByTaskIds(anyCollection())).thenReturn(List.of());
        when(subtasks.taskIdOf(SUBTASK_ID)).thenReturn(Optional.of(TASK_ID));
        when(subtasks.findForUpdate(TASK_ID, SUBTASK_ID)).thenReturn(Optional.of(subtask));
        when(subtasks.save(subtask)).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignments.find(TASK_ID, WORKER_ID))
                .thenReturn(Optional.of(new TaskAssignment(TASK_ID, WORKER_ID, NOW, 1)));
        when(assignments.findForTask(TASK_ID)).thenReturn(List.of());
    }

    @Test
    void assignedWorkerCanStartTaskAndPersistsTransition() {
        TaskResult result = taskCommands.start(new TransitionCommand(TASK_ID, null, task.version()));

        assertThat(result.status()).isEqualTo(WorkStatus.EN_PROGRESO.name());
        assertThat(result.currentSegmentStartedAt()).isEqualTo(NOW);
        assertThat(task.pendingChanges()).last().satisfies(change -> {
            assertThat(change.changedByUserId()).isEqualTo(WORKER_ID);
            assertThat(change.reason()).isEqualTo("Ejecutado por el Personal asignado");
        });
        verify(tasks).save(task);
    }

    @Test
    void unassignedWorkerGetsForbiddenForExistingTaskWithoutSaving() {
        when(tasks.findAssignedForUpdate(WORKER_ID, TASK_ID)).thenReturn(Optional.empty());
        when(assignments.find(TASK_ID, WORKER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskCommands.start(new TransitionCommand(TASK_ID, null, task.version())))
                .isInstanceOf(ForbiddenException.class);
        verify(tasks, never()).save(task);
    }

    @Test
    void chiefCanExecuteAssignedTaskWithoutOverrideReason() {
        current = new Actor(1, true, true);
        when(assignments.find(TASK_ID, 1))
                .thenReturn(Optional.of(new TaskAssignment(TASK_ID, 1, NOW, 1)));

        TaskResult result = taskCommands.start(new TransitionCommand(TASK_ID, null, task.version()));

        assertThat(result.status()).isEqualTo(WorkStatus.EN_PROGRESO.name());
        assertThat(task.pendingChanges()).last()
                .extracting(change -> change.reason())
                .isEqualTo("Ejecutado por el Personal asignado");
    }

    @Test
    void chiefCanOverrideUnassignedTaskAndReasonIsRecorded() {
        current = new Actor(1, true, true);
        when(assignments.find(TASK_ID, 1)).thenReturn(Optional.empty());

        taskCommands.start(new TransitionCommand(TASK_ID, null, task.version()));

        assertThat(task.pendingChanges()).last()
                .extracting(change -> change.reason())
                .isEqualTo("Override del Jefe (US1.3): sin motivo indicado");
    }

    @Test
    void unknownTaskReturnsNotFoundInsteadOfForbidden() {
        when(tasks.projectIdOf(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskCommands.start(new TransitionCommand(999, null, 1L)))
                .isInstanceOf(NotFoundException.class);
        verify(tasks, never()).save(task);
    }

    @Test
    void assignedWorkerCanCompleteSubtaskAndIsRecordedAsActor() {
        SubtaskResult started = subtaskCommands.start(
                new TransitionCommand(SUBTASK_ID, null, subtask.version()));
        SubtaskResult result = subtaskCommands.complete(
                new TransitionCommand(SUBTASK_ID, null, started.version()));

        assertThat(result.status()).isEqualTo(WorkStatus.COMPLETADA.name());
        assertThat(result.completedByUserId()).isEqualTo(WORKER_ID);
        assertThat(result.completedAt()).isEqualTo(NOW);
        verify(subtasks, times(2)).save(subtask);
    }

    @Test
    void unassignedWorkerCannotExecuteSubtaskEvenWhenItExists() {
        when(assignments.find(TASK_ID, WORKER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtaskCommands.start(
                new TransitionCommand(SUBTASK_ID, null, subtask.version())))
                .isInstanceOf(ForbiddenException.class);
        verify(subtasks, never()).save(subtask);
    }

    @Test
    void chiefCanExecuteSubtaskInAnyProject() {
        current = new Actor(1, true, true);
        when(tasks.projectIdOf(TASK_ID)).thenReturn(Optional.of(PROJECT_ID));
        when(subtasks.findOpenForUpdate(PROJECT_ID, SUBTASK_ID)).thenReturn(Optional.of(subtask));

        SubtaskResult result = subtaskCommands.start(
                new TransitionCommand(SUBTASK_ID, null, subtask.version()));

        assertThat(result.status()).isEqualTo(WorkStatus.EN_PROGRESO.name());
        verify(subtasks).findOpenForUpdate(PROJECT_ID, SUBTASK_ID);
    }

    @Test
    void deletedSubtaskBetweenLookupsReturnsNotFound() {
        when(subtasks.taskIdOf(SUBTASK_ID)).thenReturn(Optional.of(TASK_ID), Optional.empty());
        when(subtasks.findForUpdate(TASK_ID, SUBTASK_ID)).thenReturn(Optional.empty());
        when(assignments.find(TASK_ID, WORKER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtaskCommands.start(
                new TransitionCommand(SUBTASK_ID, null, subtask.version())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void personalTaskProjectionContainsExecutionDataButNoFinancialFields() {
        when(tasks.findAssignedTo(WORKER_ID, null)).thenReturn(List.of(task));

        List<AssignedTaskResult> result = taskQueries.listAssignedTasks(null);

        assertThat(result).singleElement().satisfies(assigned -> {
            assertThat(assigned.taskId()).isEqualTo(TASK_ID);
            assertThat(assigned.projectId()).isNull();
            assertThat(assigned.status()).isEqualTo(WorkStatus.PENDIENTE.name());
            assertThat(assigned.trackedSeconds()).isZero();
        });
        assertThat(Arrays.stream(AssignedTaskResult.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList())
                .doesNotContain("allocatedBudgetBob", "estimatedDurationMinutes");
        verify(subtasks).findVisibleByTaskIds(List.of(TASK_ID));
    }

    @Test
    void personalCannotReadUnassignedTaskByIdentifier() {
        when(tasks.findAssignedTo(WORKER_ID, null)).thenReturn(List.of());
        when(tasks.findVisible(PROJECT_ID, TASK_ID)).thenReturn(Optional.of(task));
        when(assignments.find(TASK_ID, WORKER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskQueries.getAssignedTask(TASK_ID))
                .isInstanceOf(NotFoundException.class);
    }

    private static Task task(WorkStatus status) {
        return Task.restore(TASK_ID, Activity.reference(8, "Implementación"), "Tarea privada", "Descripción",
                status, TaskPriority.ALTA, new BigDecimal("975.25"), 240, null, null, null,
                NOW.minusSeconds(60), NOW.minusSeconds(60), List.of());
    }
}
