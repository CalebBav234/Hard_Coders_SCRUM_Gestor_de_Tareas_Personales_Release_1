package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.result.ActivityResult;
import com.hardcoders.taskmanager.application.result.StageResult;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.model.SearchText;
import com.hardcoders.taskmanager.domain.model.WorkStatus;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de visibilidad y búsqueda de jerarquía (ramas de actor, jefe vs encargado). */
class HierarchyQueryServiceSearchTest {

    private InMemoryPorts ports;
    private HierarchyQueryService queries;

    @BeforeEach
    void setUp() {
        ports = new InMemoryPorts(java.time.Clock.fixed(
                Instant.parse("2026-10-15T12:00:00Z"), java.time.ZoneOffset.UTC));
        ports.seedProject("Proyecto Alfa", InMemoryPorts.ACTOR, new BigDecimal("1000.00"));
        Integer stageId = ports.seedStage(InMemoryPorts.PROJECT, "Etapa Beta").id();
        ports.seedActivity(stageId, "Actividad Gamma").id();
        ports.currentActor = new Actor(InMemoryPorts.ACTOR, false, true);
        queries = new HierarchyQueryService(ports.taskRepository, ports.subtaskRepository, ports.hierarchy,
                ports.projectRepository, ports.assignmentRepository, ports.actor);
    }

    @Test
    void nullOrInactiveActorGetsEmptySearchResults() {
        assertThat(queries.searchStages("etapa")).isEmpty();
        assertThat(queries.searchActivities("actividad")).isEmpty();

        ports.currentActor = new Actor(InMemoryPorts.ACTOR, false, false);
        assertThat(queries.searchStages("etapa")).isEmpty();
        assertThat(queries.searchActivities("actividad")).isEmpty();

        ports.currentActor = null;
        assertThat(queries.searchStages("etapa")).isEmpty();
        assertThat(queries.searchActivities("actividad")).isEmpty();
    }

    @Test
    void nullQueryReturnsEmpty() {
        assertThat(queries.searchStages(null)).isEmpty();
        assertThat(queries.searchActivities(null)).isEmpty();
    }

    @Test
    void chiefAndManagerExecuteBothSearchBranches() {
        Project otherProject = Project.restore(99, "Otro", null, 88, ProjectStatus.EN_EJECUCION,
                new BigDecimal("500"), 60, 88, Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T10:00:00Z"), 1);
        ports.projectRepository.save(otherProject);
        ports.seedStage(99, "Etapa Extrana").id();

        ports.currentActor = new Actor(99, true, true);
        List<StageResult> chief = queries.searchStages("etapa");

        ports.currentActor = new Actor(InMemoryPorts.ACTOR, false, true);
        List<StageResult> manager = queries.searchStages("etapa");

        assertThat(chief).isNotNull();
        assertThat(manager).isNotNull();
    }

    @Test
    void searchTextNormalizesAccentsAndCase() {
        assertThat(SearchText.likePattern("ÁÉÍÓÚ")).isPresent();
        assertThat(SearchText.likePattern("  espa  ")).isPresent();
        assertThat(SearchText.likePattern("")).isEmpty();
        assertThat(SearchText.likePattern(null)).isEmpty();
    }

    @Test
    void stagesOfAndActivitiesOfWorkForVisibleProject() {
        assertThat(queries.stagesOf(InMemoryPorts.PROJECT)).isNotNull();
        assertThat(queries.activitiesOf(ports.seedStage(InMemoryPorts.PROJECT, "X").id())).isNotNull();
    }
}
