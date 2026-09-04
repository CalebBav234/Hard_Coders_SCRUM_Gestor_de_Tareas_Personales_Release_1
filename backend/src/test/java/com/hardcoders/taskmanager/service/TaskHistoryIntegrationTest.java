package com.hardcoders.taskmanager.service;

import com.hardcoders.taskmanager.dto.TaskResponse;
import com.hardcoders.taskmanager.dto.UpdateTaskRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in: run only against a disposable PostgreSQL 18 database with V001/V002 (or newer). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "HISTORY_TEST_DB_URL", matches = ".+")
class TaskHistoryIntegrationTest {
    @Autowired TaskService service;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("HISTORY_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> "gestor_tareas_app");
        registry.add("spring.datasource.password", () -> System.getenv("HISTORY_TEST_DB_PASSWORD"));
    }

    @Test
    void completedTaskIsVisibleWithTimelineAndDescriptionSearch() {
        String title = "Reunión " + UUID.randomUUID();
        TaskResponse task = service.create(title, "ALTA", null, null);
        task = service.update(task.id(), new UpdateTaskRequest(
                title, "Documentación " + title, "ALTA", task.version()));
        task = service.activate(task.id(), task.version());
        task = service.complete(task.id(), task.version());

        var history = service.listHistory("documentacion " + title);
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().id()).isEqualTo(task.id());
        assertThat(history.getFirst().events()).extracting(event -> event.toStatus())
                .containsExactly("TERMINADA", "ACTIVA", "INACTIVA");
        assertThat(service.listHistory("")).extracting(item -> item.id()).contains(task.id());

        service.reopen(task.id(), task.version());
        assertThat(service.listHistory(title)).isEmpty();
    }

    @Test
    void deletedActiveTaskRetainsItsContextAndFrozenTime() {
        String title = "Historial literal_% " + UUID.randomUUID();
        TaskResponse task = service.create(title, "MEDIA", null,null);
        task = service.activate(task.id(), task.version());
        service.delete(task.id(), task.version());

        var history = service.listHistory(title);
        assertThat(history).hasSize(1);
        var archived = history.getFirst();
        assertThat(archived.deletedAt()).isNotNull();
        assertThat(archived.events()).hasSize(2);
        long segment = java.time.Duration.between(archived.activatedAt(), archived.deletedAt()).toSeconds();
        assertThat(archived.effectiveActiveSeconds()).isEqualTo(archived.totalActiveSeconds() + segment);
        assertThat(service.listTasks(title)).isEmpty();
        assertThat(service.listHistory("no-coincide-" + UUID.randomUUID())).isEmpty();
    }
}
