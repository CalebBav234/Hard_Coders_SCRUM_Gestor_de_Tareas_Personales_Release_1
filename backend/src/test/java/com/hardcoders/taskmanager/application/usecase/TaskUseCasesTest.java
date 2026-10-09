package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.ChangeActivityCommand;
import com.hardcoders.taskmanager.application.command.CreateSubtaskCommand;
import com.hardcoders.taskmanager.application.command.CreateTaskCommand;
import com.hardcoders.taskmanager.application.command.UpdateTaskCommand;
import com.hardcoders.taskmanager.application.command.VersionedCommand;
import com.hardcoders.taskmanager.application.result.SubtaskResult;
import com.hardcoders.taskmanager.application.result.TaskHistoryResult;
import com.hardcoders.taskmanager.application.result.TaskResult;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.exception.OpenSubtasksException;
import com.hardcoders.taskmanager.domain.exception.VersionConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskUseCasesTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);
    private InMemoryPorts ports;
    private TaskCommandService tasks;
    private SubtaskCommandService subtasks;
    private TaskQueryService queries;

    @BeforeEach
    void setUp() {
        ports = new InMemoryPorts(clock);
        tasks = new TaskCommandService(ports.taskRepository, ports.subtaskRepository, ports.workspaces,
                ports.actor, clock);
        subtasks = new SubtaskCommandService(ports.taskRepository, ports.subtaskRepository, ports.workspaces,
                ports.actor, clock);
        queries = new TaskQueryService(ports.taskRepository, ports.subtaskRepository, ports.workspaces);
    }

    @Test
    void createsTasksInTheirActivityOrInSinCategoria() {
        TaskResult categorized = tasks.create(new CreateTaskCommand("Informe", "ALTA", " Trabajo "));
        TaskResult plain = tasks.create(new CreateTaskCommand("Comprar pan", null, ""));

        assertThat(categorized.activityName()).isEqualTo("Trabajo");
        assertThat(categorized.status()).isEqualTo("PENDIENTE");
        assertThat(categorized.priority()).isEqualTo("ALTA");
        assertThat(plain.activityName()).isNull();
        assertThat(plain.priority()).isEqualTo("MEDIA");
        assertThat(queries.activities()).extracting(a -> a.name()).containsExactly("Trabajo");
    }

    @Test
    void editsWithTheCurrentVersionAndRejectsStaleOnes() {
        TaskResult created = tasks.create(new CreateTaskCommand("Original", "MEDIA", null));

        TaskResult edited = tasks.update(new UpdateTaskCommand(created.taskId(), "Editada", "Detalle", "BAJA",
                created.version()));
        assertThat(edited.title()).isEqualTo("Editada");
        assertThat(edited.version()).isNotEqualTo(created.version());

        assertThatThrownBy(() -> tasks.update(new UpdateTaskCommand(created.taskId(), "Otra", null, "ALTA",
                created.version()))).isInstanceOf(VersionConflictException.class);
    }

    @Test
    void movesATaskToAnotherCategory() {
        TaskResult created = tasks.create(new CreateTaskCommand("Tarea", null, null));

        TaskResult moved = tasks.changeActivity(new ChangeActivityCommand(created.taskId(), "Casa", created.version()));

        assertThat(moved.activityName()).isEqualTo("Casa");
    }

    @Test
    void lifecycleThroughUseCasesEndsInHistory() {
        TaskResult created = tasks.create(new CreateTaskCommand("Tarea", null, null));
        TaskResult started = tasks.start(new VersionedCommand(created.taskId(), created.version()));
        assertThat(started.status()).isEqualTo("EN_PROGRESO");
        assertThat(started.currentSegmentStartedAt()).isNotNull();

        TaskResult completed = tasks.complete(new VersionedCommand(created.taskId(), started.version()));
        assertThat(completed.status()).isEqualTo("COMPLETADA");
        assertThat(queries.listTasks("")).extracting(TaskResult::taskId).contains(created.taskId());

        tasks.cancel(new VersionedCommand(created.taskId(), completed.version()));

        assertThat(queries.listTasks("")).isEmpty();
        assertThatThrownBy(() -> queries.getTask(created.taskId())).isInstanceOf(NotFoundException.class);
        TaskHistoryResult archived = queries.taskHistory("").getFirst();
        assertThat(archived.cancelledAt()).isNotNull();
        assertThat(archived.events()).extracting(e -> e.newStatus())
                .containsExactly("CANCELADA", "COMPLETADA", "EN_PROGRESO", "PENDIENTE");
    }

    @Test
    void subtasksBelongToTheirTaskAndBlockItsDeletion() {
        TaskResult parent = tasks.create(new CreateTaskCommand("Padre", null, null));
        SubtaskResult child = subtasks.create(new CreateSubtaskCommand(parent.taskId(), "Hija", null));

        assertThat(queries.getTask(parent.taskId()).subtasks()).extracting(SubtaskResult::subtaskId)
                .containsExactly(child.subtaskId());
        assertThatThrownBy(() -> tasks.cancel(new VersionedCommand(parent.taskId(), parent.version())))
                .isInstanceOf(OpenSubtasksException.class);

        SubtaskResult started = subtasks.start(new VersionedCommand(child.subtaskId(), child.version()));
        SubtaskResult done = subtasks.complete(new VersionedCommand(child.subtaskId(), started.version()));
        assertThat(done.status()).isEqualTo("COMPLETADA");

        TaskResult current = queries.getTask(parent.taskId());
        tasks.cancel(new VersionedCommand(parent.taskId(), current.version()));
        assertThat(queries.listTasks("")).isEmpty();
    }

    @Test
    void cannotAddSubtasksToMissingTasks() {
        assertThatThrownBy(() -> subtasks.create(new CreateSubtaskCommand(999, "Hija", null)))
                .isInstanceOf(NotFoundException.class);
    }
}
