package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.AssignmentCommands.AssignWorkerCommand;
import com.hardcoders.taskmanager.application.command.AssignmentCommands.UnassignWorkerCommand;
import com.hardcoders.taskmanager.application.command.ChangeActivityCommand;
import com.hardcoders.taskmanager.application.command.CreateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateActivityCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.CreateStageCommand;
import com.hardcoders.taskmanager.application.command.HierarchyCommands.UpdateStageCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand;
import com.hardcoders.taskmanager.application.command.TaskCommands.UpdateTaskPlanCommand;
import com.hardcoders.taskmanager.application.command.UpdateTaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.result.AssignedTaskResult;
import com.hardcoders.taskmanager.application.result.StageResult;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskAssignmentResult;
import com.hardcoders.taskmanager.application.result.TaskHistoryResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.exception.OpenSubtasksException;
import com.hardcoders.taskmanager.domain.exception.VersionConflictException;
import com.hardcoders.taskmanager.domain.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskUseCasesTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);
    private Integer foreignProjectId;
    private InMemoryPorts ports;
    private TaskCommandService tasks;
    private SubtaskCommandService subtasks;
    private TaskQueryService queries;
    private HierarchyCommandService hierarchy;
    private AssignmentCommandService assignmentService;
    private ProgressQueryService progress;

    @BeforeEach
    void setUp() {
        ports = new InMemoryPorts(clock);
        ports.seedProject("Proyecto A", InMemoryPorts.ACTOR, new BigDecimal("1000.00"));
        ports.seedStage(InMemoryPorts.PROJECT, "Etapa 1");
        ports.seedActivity(InMemoryPorts.STAGE, "Analisis");
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        tasks = new TaskCommandService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor, clock);
        subtasks = new SubtaskCommandService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor, clock);
        queries = new TaskQueryService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor);
        hierarchy = new HierarchyCommandService(ports.projectRepository, ports.hierarchy, ports.actor, clock);
        assignmentService = new AssignmentCommandService(ports.taskRepository, ports.projectRepository,
                ports.assignmentRepository, new FixedDirectory(), ports.actor, clock);
        progress = new ProgressQueryService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.actor);
    }

    @Test
    void createsTasksInTheirActivityWithBudgetAndDuration() {
        TaskResult created = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Informe", "ALTA",
                new BigDecimal("250.50"), 90));

        assertThat(created.activityName()).isEqualTo("Analisis");
        assertThat(created.status()).isEqualTo("PENDIENTE");
        assertThat(created.priority()).isEqualTo("ALTA");
        assertThat(created.allocatedBudgetBob()).isEqualByComparingTo("250.50");
        assertThat(created.estimatedDurationMinutes()).isEqualTo(90);
    }

    @Test
    void rejectsNegativeBudgetOrDurationOnCreation() {
        assertThatThrownBy(() -> tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Malo", "ALTA",
                new BigDecimal("-1.00"), 30))).isInstanceOf(
                com.hardcoders.taskmanager.domain.exception.DomainValidationException.class);
        assertThatThrownBy(() -> tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Malo", "ALTA",
                BigDecimal.ZERO, -30))).isInstanceOf(
                com.hardcoders.taskmanager.domain.exception.DomainValidationException.class);
    }

    @Test
    void editsWithTheCurrentVersionAndRejectsStaleOnes() {
        TaskResult created = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Original", "MEDIA",
                BigDecimal.ZERO, 60));

        TaskResult edited = tasks.update(new UpdateTaskCommand(created.taskId(), "Editada", "Detalle", "BAJA",
                created.version()));
        assertThat(edited.title()).isEqualTo("Editada");
        assertThat(edited.version()).isNotEqualTo(created.version());

        assertThatThrownBy(() -> tasks.update(new UpdateTaskCommand(created.taskId(), "Otra", null, "ALTA",
                created.version()))).isInstanceOf(VersionConflictException.class);
    }

    @Test
    void movesATaskToAnotherActivityOfTheSameProject() {
        com.hardcoders.taskmanager.domain.model.Activity second =
                ports.seedActivity(InMemoryPorts.STAGE, "Diseno");
        TaskResult created = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", null,
                BigDecimal.ZERO, 60));

        TaskResult moved = tasks.changeActivity(
                new ChangeActivityCommand(created.taskId(), second.id(), created.version()));

        assertThat(moved.activityName()).isEqualTo("Diseno");
    }

    @Test
    void rejectsMovingATaskToAnActivityOfAnotherProject() {
        foreignProjectId = ports.seedProject("Proyecto B", InMemoryPorts.WORKER,
                new BigDecimal("10.00")).id();
        com.hardcoders.taskmanager.domain.model.Stage otherStage = ports.seedStage(foreignProjectId, "Etapa B");
        com.hardcoders.taskmanager.domain.model.Activity foreign =
                ports.seedActivity(otherStage.id(), "Ajena");
        TaskResult created = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", null,
                BigDecimal.ZERO, 60));

        assertThatThrownBy(() -> tasks.changeActivity(
                new ChangeActivityCommand(created.taskId(), foreign.id(), created.version())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void lifecycleThroughUseCasesEndsInHistory() {
        TaskResult created = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", null,
                BigDecimal.ZERO, 60));
        TaskResult started = tasks.start(new TransitionCommand(created.taskId(), null, created.version()));
        assertThat(started.status()).isEqualTo("EN_PROGRESO");
        assertThat(started.currentSegmentStartedAt()).isNotNull();

        TaskResult completed = tasks.complete(new TransitionCommand(created.taskId(), null, started.version()));
        assertThat(completed.status()).isEqualTo("COMPLETADA");
        assertThat(queries.listTasks(InMemoryPorts.PROJECT, ""))
                .extracting(TaskResult::taskId).contains(created.taskId());

        tasks.cancel(new VersionedCommand(created.taskId(), completed.version()));

        assertThat(queries.listTasks(InMemoryPorts.PROJECT, "")).isEmpty();
        assertThatThrownBy(() -> queries.getTask(created.taskId())).isInstanceOf(NotFoundException.class);
        TaskHistoryResult archived = queries.taskHistory(InMemoryPorts.PROJECT, "").getFirst();
        assertThat(archived.cancelledAt()).isNotNull();
        assertThat(archived.events()).extracting(e -> e.newStatus())
                .containsExactly("CANCELADA", "COMPLETADA", "EN_PROGRESO", "PENDIENTE");
    }

    @Test
    void subtasksBelongToTheirTaskAndBlockItsDeletion() {
        TaskResult parent = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Padre", null,
                BigDecimal.ZERO, 60));
        SubtaskResult child = subtasks.create(new CreateSubtaskCommand(parent.taskId(), "Hija", null));

        assertThat(queries.getTask(parent.taskId()).subtasks()).extracting(SubtaskResult::subtaskId)
                .containsExactly(child.subtaskId());
        assertThatThrownBy(() -> tasks.cancel(new VersionedCommand(parent.taskId(), parent.version())))
                .isInstanceOf(OpenSubtasksException.class);

        SubtaskResult started = subtasks.start(new TransitionCommand(child.subtaskId(), null, child.version()));
        SubtaskResult done = subtasks.complete(
                new TransitionCommand(child.subtaskId(), null, started.version()));
        assertThat(done.status()).isEqualTo("COMPLETADA");

        TaskResult current = queries.getTask(parent.taskId());
        tasks.cancel(new VersionedCommand(parent.taskId(), current.version()));
        assertThat(queries.listTasks(InMemoryPorts.PROJECT, "")).isEmpty();
    }

    @Test
    void cannotAddSubtasksToMissingTasks() {
        assertThatThrownBy(() -> subtasks.create(new CreateSubtaskCommand(999, "Hija", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void taskBudgetCannotExceedTheProjectBudget() {
        TaskResult created = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", null,
                new BigDecimal("600.00"), 60));

        assertThatThrownBy(() -> tasks.updatePlan(new UpdateTaskPlanCommand(created.taskId(),
                new BigDecimal("1500.00"), 60, "Ampliacion necesaria", created.version())))
                .isInstanceOf(
                        com.hardcoders.taskmanager.domain.exception.BudgetBelowAllocationsException.class);
        assertThat(ports.tasks.get(created.taskId()).allocatedBudgetBob())
                .isEqualByComparingTo("600.00");

        TaskResult replanned = tasks.updatePlan(new UpdateTaskPlanCommand(created.taskId(),
                new BigDecimal("900.00"), 120, "Ampliacion autorizada", created.version()));
        assertThat(replanned.allocatedBudgetBob()).isEqualByComparingTo("900.00");
        assertThat(replanned.estimatedDurationMinutes()).isEqualTo(120);
    }

    // ---------------------------------------------------------------- US2.1 / US2.2

    @Test
    void onlyTheProjectManagerCreatesStagesAndActivities() {
        StageResult stage = hierarchy.createStage(new CreateStageCommand(InMemoryPorts.PROJECT, "Etapa 2", null));
        assertThat(stage.projectId()).isEqualTo(InMemoryPorts.PROJECT);
        assertThat(ports.hierarchy.activitiesOf(stage.stageId())).isEmpty();

        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        assertThatThrownBy(() -> hierarchy.createStage(new CreateStageCommand(InMemoryPorts.PROJECT, "X", null)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> hierarchy.createStage(new CreateStageCommand(999, "X", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void aStageFromAnotherProjectIsInvisibleToItsManager() {
        ports.seedProject("Proyecto B", InMemoryPorts.WORKER, new BigDecimal("10.00"));
        ports.seedStage(11, "Etapa B");

        Actor managerA = new Actor(InMemoryPorts.ACTOR, false, true);
        ports.currentActor = managerA;
        StageResult stage = hierarchy.createStage(new CreateStageCommand(InMemoryPorts.PROJECT, "Etapa", null));
        assertThat(stage.stageId()).isNotNull();

        Actor managerB = new Actor(InMemoryPorts.WORKER, false, true);
        ports.currentActor = managerB;
        assertThatThrownBy(() -> hierarchy.updateStage(
                new UpdateStageCommand(stage.stageId(), "Renombrar", null, stage.version())))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void activityCreationNeedsTheEnchargedRoleOnTheProject() {
        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        assertThatThrownBy(() -> hierarchy.createActivity(
                new CreateActivityCommand(InMemoryPorts.STAGE, "Actividad", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    // ------------------------------------------------------------------- US2.3

    @Test
    void assignsAndRevokesPersonalOnATaskOfTheProject() {
        TaskResult task = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", null,
                BigDecimal.ZERO, 60));

        TaskAssignmentResult assigned = assignmentService.assign(
                new AssignWorkerCommand(task.taskId(), InMemoryPorts.WORKER, "Carga de trabajo"));
        assertThat(assigned.workerUserId()).isEqualTo(InMemoryPorts.WORKER);
        assertThat(assigned.assignedByUserId()).isEqualTo(InMemoryPorts.ACTOR);

        assertThat(assignmentService.assignmentsForTask(task.taskId()))
                .extracting(TaskAssignmentResult::workerUserId)
                .containsExactly(InMemoryPorts.WORKER);

        assignmentService.unassign(new UnassignWorkerCommand(task.taskId(), InMemoryPorts.WORKER, null));
        assertThat(assignmentService.assignmentsForTask(task.taskId())).isEmpty();
    }

    @Test
    void rejectsAssigningAnInactiveOrUnknownWorkerAndSelfAssignment() {
        TaskResult task = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", null,
                BigDecimal.ZERO, 60));

        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        assertThatThrownBy(() -> assignmentService.assign(
                new AssignWorkerCommand(task.taskId(), InMemoryPorts.ACTOR, "yo")))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> assignmentService.assign(
                new AssignWorkerCommand(task.taskId(), 777, "nadie")))
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.DomainValidationException.class);

        assertThatThrownBy(() -> assignmentService.assign(
                new AssignWorkerCommand(task.taskId(), FixedDirectory.INACTIVE, "inactivo")))
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.DomainValidationException.class);
    }

    @Test
    void personalCannotAssignTasks() {
        TaskResult task = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", null,
                BigDecimal.ZERO, 60));
        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);

        assertThatThrownBy(() -> assignmentService.assign(
                new AssignWorkerCommand(task.taskId(), InMemoryPorts.WORKER, "intento")))
                .isInstanceOf(ForbiddenException.class);
    }

    // --------------------------------------------------------------- US3.1 / US3.2

    @Test
    void personalOnlySeesTheTasksAssignedToThemAndWithoutFinancialData() {
        com.hardcoders.taskmanager.domain.model.Activity privateActivity =
                ports.seedActivity(InMemoryPorts.STAGE, "Privada");
        TaskResult mine = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Mia", null,
                new BigDecimal("100.00"), 60));
        TaskResult other = tasks.create(new CreateTaskCommand(privateActivity.id(), "Ajena", null,
                new BigDecimal("100.00"), 60));
        assignmentService.assign(new AssignWorkerCommand(mine.taskId(), InMemoryPorts.WORKER, "asignada"));

        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        List<AssignedTaskResult> assigned = queries.listAssignedTasks("");
        assertThat(assigned).extracting(AssignedTaskResult::taskId).containsExactly(mine.taskId());
        assertThat(assigned).extracting(AssignedTaskResult::title).doesNotContain(other.title());
        // AC-US3.1-02: el Personal no recibe los campos financieros de la tarea.
        assertThat(AssignedTaskResult.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("allocatedBudgetBob", "estimatedDurationMinutes");
    }

    @Test
    void personalCannotReadAnotherProjectEvenWithAValidId() {
        foreignProjectId = ports.seedProject("Proyecto B", InMemoryPorts.WORKER,
                new BigDecimal("10.00")).id();
        // Actor sin relación con el proyecto: ni Jefe ni Encargado ni Personal asignado.
        ports.currentActor = new Actor(9, false, true);

        assertThatThrownBy(() -> queries.taskHistory(foreignProjectId, ""))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> queries.listTasks(foreignProjectId, ""))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> progress.progress(foreignProjectId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unassignedPersonalCannotExecuteATask() {
        TaskResult task = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", null,
                BigDecimal.ZERO, 60));
        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);

        assertThatThrownBy(() -> tasks.complete(
                new TransitionCommand(task.taskId(), null, task.version())))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assignedPersonalCompletesTheirTaskAndTheChiefCanOverride() {
        TaskResult task = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", "MEDIA",
                new BigDecimal("30.00"), 60));
        assignmentService.assign(new AssignWorkerCommand(task.taskId(), InMemoryPorts.WORKER, "asignada"));
        SubtaskResult sub = subtasks.create(new CreateSubtaskCommand(task.taskId(), "Subtarea", null));

        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);
        SubtaskResult started = subtasks.start(new TransitionCommand(sub.subtaskId(), null, sub.version()));
        assertThat(started.status()).isEqualTo("EN_PROGRESO");
        SubtaskResult done = subtasks.complete(new TransitionCommand(sub.subtaskId(), null, started.version()));
        assertThat(done.status()).isEqualTo("COMPLETADA");
        assertThat(done.completedByUserId()).isEqualTo(InMemoryPorts.WORKER);

        // US1.3: el Jefe completa la tarea de otro trabajador y el motivo queda auditado.
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        TaskResult asChief = tasks.start(new TransitionCommand(task.taskId(), "cierre de release", task.version()));
        TaskResult completedByChief = tasks.complete(
                new TransitionCommand(task.taskId(), "cierre de release", asChief.version()));
        assertThat(completedByChief.status()).isEqualTo("COMPLETADA");
        assertThat(completedByChief.completedByUserId()).isEqualTo(InMemoryPorts.ACTOR);
        assertThat(queries.taskHistory(InMemoryPorts.PROJECT, "").getFirst().events())
                .extracting(event -> event.reason())
                .anyMatch(reason -> reason.contains("Override del Jefe (US1.3)"));
    }

    @Test
    void personalWithoutAssignmentGetsNotFoundOnAForeignTask() {
        TaskResult task = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", null,
                BigDecimal.ZERO, 60));
        ports.currentActor = new Actor(InMemoryPorts.WORKER, false, true);

        assertThatThrownBy(() -> queries.getAssignedTask(task.taskId()))
                .isInstanceOf(NotFoundException.class);
    }

    // ------------------------------------------------------------------- US3.3

    @Test
    void progressCountsOnlyLeafWorkAndStaysZeroWithoutTasks() {
        com.hardcoders.taskmanager.application.result.ProjectProgressResult empty = progress.progress(InMemoryPorts.PROJECT);
        assertThat(empty.root().ratio()).isZero();
        assertThat(empty.root().total()).isZero();

        TaskResult task = tasks.create(new CreateTaskCommand(InMemoryPorts.ACTIVITY, "Tarea", null,
                BigDecimal.ZERO, 60));
        assertThat(progress.progress(InMemoryPorts.PROJECT).root().ratio()).isZero();

        SubtaskResult first = subtasks.create(new CreateSubtaskCommand(task.taskId(), "A", null));
        subtasks.create(new CreateSubtaskCommand(task.taskId(), "B", null));
        assertThat(progress.progress(InMemoryPorts.PROJECT).root().ratio()).isZero();

        SubtaskResult started = subtasks.start(new TransitionCommand(first.subtaskId(), null, first.version()));
        subtasks.complete(new TransitionCommand(first.subtaskId(), null, started.version()));
        assertThat(progress.progress(InMemoryPorts.PROJECT).root().ratio()).isEqualTo(0.5);
    }

    @Test
    void projectsClosedDoNotAcceptHierarchyChanges() {
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, true, true);
        ports.projects.get(InMemoryPorts.PROJECT).changeStatus(
                com.hardcoders.taskmanager.domain.model.ProjectStatus.EN_EJECUCION);
        ports.projects.get(InMemoryPorts.PROJECT).changeStatus(
                com.hardcoders.taskmanager.domain.model.ProjectStatus.FINALIZADO);

        assertThatThrownBy(() -> hierarchy.createStage(new CreateStageCommand(InMemoryPorts.PROJECT, "X", null)))
                .isInstanceOf(com.hardcoders.taskmanager.domain.exception.ProjectTransitionException.class);
    }

    /** Directorio de usuarios fijo para las pruebas de asignación. */
    private static final class FixedDirectory implements com.hardcoders.taskmanager.domain.port.out.UserDirectory {

        private static final Integer INACTIVE = 5;

        @Override
        public java.util.Optional<com.hardcoders.taskmanager.domain.model.UserAccount> findById(Integer userId) {
            if (InMemoryPorts.WORKER.equals(userId)) {
                return java.util.Optional.of(new com.hardcoders.taskmanager.domain.model.UserAccount(
                        userId, "personal@gestor.local", "Ana", "Perez", false, true));
            }
            if (INACTIVE.equals(userId)) {
                return java.util.Optional.of(new com.hardcoders.taskmanager.domain.model.UserAccount(
                        userId, "inactivo@gestor.local", "Luis", "Luna", false, false));
            }
            if (InMemoryPorts.ACTOR.equals(userId)) {
                return java.util.Optional.of(new com.hardcoders.taskmanager.domain.model.UserAccount(
                        userId, "jefe@gestor.local", "Jefe", "Jefe", true, true));
            }
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Map<Integer, com.hardcoders.taskmanager.domain.model.UserAccount> findByIds(
                java.util.Collection<Integer> userIds) {
            return userIds.stream().collect(java.util.stream.Collectors.toMap(id -> id, this::findByIdRequiring));
        }

        @Override
        public List<com.hardcoders.taskmanager.domain.model.UserAccount> findActive() {
            return List.of(findByIdRequiring(InMemoryPorts.WORKER), findByIdRequiring(InMemoryPorts.ACTOR));
        }

        private com.hardcoders.taskmanager.domain.model.UserAccount findByIdRequiring(Integer userId) {
            return findById(userId).orElseThrow();
        }
    }
}
