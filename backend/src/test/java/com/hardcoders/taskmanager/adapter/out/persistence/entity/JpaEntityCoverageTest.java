package com.hardcoders.taskmanager.adapter.out.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobertura de los POJO de persistencia de V004: getters/setters, valores por defecto,
 * contratos equals/hashCode de las claves compuestas y los callbacks @PrePersist/@PreUpdate
 * que fijan las columnas de auditoría cuando la aplicación no las propaga.
 */
class JpaEntityCoverageTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-15T13:00:00Z");

    @Test
    @DisplayName("ENT-TASK-01: TaskEntity expone y guarda todos sus campos con valores por defecto")
    void taskEntityGettersAndSetters() {
        TaskEntity entity = new TaskEntity();
        assertThat(entity.getAllocatedBudgetBob()).isEqualByComparingTo("0.00");
        assertThat(entity.getEstimatedDurationMinutes()).isZero();

        entity.setTaskId(5);
        ActivityEntity activity = new ActivityEntity();
        activity.setActivityId(21);
        entity.setActivity(activity);
        entity.setTitle("Informe");
        entity.setDescription("Detalle");
        WorkStatusEntity status = new WorkStatusEntity();
        status.setName("PENDIENTE");
        entity.setStatus(status);
        TaskPriorityEntity priority = new TaskPriorityEntity();
        priority.setName("ALTA");
        entity.setPriority(priority);
        entity.setAllocatedBudgetBob(new BigDecimal("150.75"));
        entity.setEstimatedDurationMinutes(90);
        entity.setActualDurationMinutes(45);
        entity.setCompletedAt(LATER);
        AppUserEntity completer = new AppUserEntity();
        completer.setUserId(1);
        entity.setCompletedBy(completer);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(LATER);

        assertThat(entity.getTaskId()).isEqualTo(5);
        assertThat(entity.getActivity()).isEqualTo(activity);
        assertThat(entity.getTitle()).isEqualTo("Informe");
        assertThat(entity.getDescription()).isEqualTo("Detalle");
        assertThat(entity.getStatus()).isEqualTo(status);
        assertThat(entity.getPriority()).isEqualTo(priority);
        assertThat(entity.getAllocatedBudgetBob()).isEqualByComparingTo("150.75");
        assertThat(entity.getEstimatedDurationMinutes()).isEqualTo(90);
        assertThat(entity.getActualDurationMinutes()).isEqualTo(45);
        assertThat(entity.getCompletedAt()).isEqualTo(LATER);
        assertThat(entity.getCompletedBy()).isEqualTo(completer);
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
        assertThat(entity.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("ENT-PROJ-01: ProjectEntity expone y guarda todos sus campos con valores por defecto")
    void projectEntityGettersAndSetters() {
        ProjectEntity entity = new ProjectEntity();
        assertThat(entity.getBudgetBob()).isEqualByComparingTo("0.00");
        assertThat(entity.getEstimatedDurationMinutes()).isZero();

        entity.setProjectId(7);
        entity.setName("Edificio A");
        entity.setDescription("Obra");
        AppUserEntity manager = new AppUserEntity();
        manager.setUserId(2);
        entity.setManager(manager);
        ProjectStatusEntity status = new ProjectStatusEntity();
        status.setName("PLANIFICADO");
        entity.setStatus(status);
        entity.setBudgetBob(new BigDecimal("9000.00"));
        entity.setEstimatedDurationMinutes(600);
        AppUserEntity createdBy = new AppUserEntity();
        createdBy.setUserId(1);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(LATER);

        assertThat(entity.getProjectId()).isEqualTo(7);
        assertThat(entity.getName()).isEqualTo("Edificio A");
        assertThat(entity.getDescription()).isEqualTo("Obra");
        assertThat(entity.getManager()).isEqualTo(manager);
        assertThat(entity.getStatus()).isEqualTo(status);
        assertThat(entity.getBudgetBob()).isEqualByComparingTo("9000.00");
        assertThat(entity.getEstimatedDurationMinutes()).isEqualTo(600);
        assertThat(entity.getCreatedBy()).isEqualTo(createdBy);
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
        assertThat(entity.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("ENT-STAGE-01: StageEntity y ActivityEntity exponen sus campos y referencias")
    void stageAndActivityGettersAndSetters() {
        ProjectEntity project = new ProjectEntity();
        project.setProjectId(7);
        StageEntity stage = new StageEntity();
        stage.setStageId(11);
        stage.setProject(project);
        stage.setName("Etapa 1");
        stage.setDescription("Detalle");
        WorkStatusEntity status = new WorkStatusEntity();
        status.setName("PENDIENTE");
        stage.setStatus(status);
        stage.setCreatedAt(NOW);
        stage.setUpdatedAt(LATER);

        assertThat(stage.getStageId()).isEqualTo(11);
        assertThat(stage.getProject()).isEqualTo(project);
        assertThat(stage.getName()).isEqualTo("Etapa 1");
        assertThat(stage.getDescription()).isEqualTo("Detalle");
        assertThat(stage.getStatus()).isEqualTo(status);
        assertThat(stage.getCreatedAt()).isEqualTo(NOW);
        assertThat(stage.getUpdatedAt()).isEqualTo(LATER);

        ActivityEntity activity = new ActivityEntity();
        activity.setActivityId(21);
        activity.setStage(stage);
        activity.setName("Analisis");
        activity.setDescription("Desc");
        activity.setStatus(status);
        activity.setCreatedAt(NOW);
        activity.setUpdatedAt(LATER);

        assertThat(activity.getActivityId()).isEqualTo(21);
        assertThat(activity.getStage()).isEqualTo(stage);
        assertThat(activity.getName()).isEqualTo("Analisis");
        assertThat(activity.getDescription()).isEqualTo("Desc");
        assertThat(activity.getStatus()).isEqualTo(status);
        assertThat(activity.getCreatedAt()).isEqualTo(NOW);
        assertThat(activity.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("ENT-SUBTASK-01: SubtaskEntity expone tarea contenedora, estado y completado")
    void subtaskEntityGettersAndSetters() {
        TaskEntity task = new TaskEntity();
        task.setTaskId(31);
        SubtaskEntity entity = new SubtaskEntity();
        entity.setSubtaskId(41);
        entity.setTask(task);
        entity.setTitle("Pedir permisos");
        entity.setDescription("Municipalidad");
        WorkStatusEntity status = new WorkStatusEntity();
        status.setName("COMPLETADA");
        entity.setStatus(status);
        entity.setCompletedAt(LATER);
        AppUserEntity completer = new AppUserEntity();
        completer.setUserId(1);
        entity.setCompletedBy(completer);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(LATER);

        assertThat(entity.getSubtaskId()).isEqualTo(41);
        assertThat(entity.getTask()).isEqualTo(task);
        assertThat(entity.getTitle()).isEqualTo("Pedir permisos");
        assertThat(entity.getDescription()).isEqualTo("Municipalidad");
        assertThat(entity.getStatus()).isEqualTo(status);
        assertThat(entity.getCompletedAt()).isEqualTo(LATER);
        assertThat(entity.getCompletedBy()).isEqualTo(completer);
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
        assertThat(entity.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("ENT-USER-01: AppUserEntity expone credenciales y banderas con valores por defecto")
    void appUserEntityGettersAndSetters() {
        AppUserEntity entity = new AppUserEntity();
        assertThat(entity.getIsChief()).isFalse();
        assertThat(entity.getIsActive()).isTrue();

        entity.setUserId(3);
        entity.setEmail("jefe@gestor.local");
        entity.setPasswordHash("hash");
        entity.setGivenNames("Julia");
        entity.setFamilyNames("Mendoza");
        entity.setIsChief(true);
        entity.setIsActive(false);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(LATER);

        assertThat(entity.getUserId()).isEqualTo(3);
        assertThat(entity.getEmail()).isEqualTo("jefe@gestor.local");
        assertThat(entity.getPasswordHash()).isEqualTo("hash");
        assertThat(entity.getGivenNames()).isEqualTo("Julia");
        assertThat(entity.getFamilyNames()).isEqualTo("Mendoza");
        assertThat(entity.getIsChief()).isTrue();
        assertThat(entity.getIsActive()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
        assertThat(entity.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("ENT-CAT-01: catálogos WorkStatus/TaskPriority/ProjectStatus exponen sus banderas")
    void catalogEntitiesGettersAndSetters() {
        WorkStatusEntity work = new WorkStatusEntity();
        assertThat(work.getIsCompleted()).isFalse();
        assertThat(work.getIsEnabled()).isTrue();
        work.setWorkStatusId(4);
        work.setName("EN_PROGRESO");
        work.setIsCompleted(false);
        work.setIsEnabled(true);
        assertThat(work.getWorkStatusId()).isEqualTo(4);
        assertThat(work.getName()).isEqualTo("EN_PROGRESO");

        TaskPriorityEntity priority = new TaskPriorityEntity();
        assertThat(priority.getIsEnabled()).isTrue();
        priority.setTaskPriorityId(2);
        priority.setName("MEDIA");
        priority.setSortOrder(2);
        priority.setIsEnabled(false);
        assertThat(priority.getTaskPriorityId()).isEqualTo(2);
        assertThat(priority.getName()).isEqualTo("MEDIA");
        assertThat(priority.getSortOrder()).isEqualTo(2);
        assertThat(priority.getIsEnabled()).isFalse();

        ProjectStatusEntity projectStatus = new ProjectStatusEntity();
        assertThat(projectStatus.getIsClosed()).isFalse();
        assertThat(projectStatus.getIsEnabled()).isTrue();
        projectStatus.setProjectStatusId(1);
        projectStatus.setName("PLANIFICADO");
        projectStatus.setIsClosed(false);
        projectStatus.setIsEnabled(false);
        assertThat(projectStatus.getProjectStatusId()).isEqualTo(1);
        assertThat(projectStatus.getName()).isEqualTo("PLANIFICADO");
        assertThat(projectStatus.getIsClosed()).isFalse();
        assertThat(projectStatus.getIsEnabled()).isFalse();
    }

    @Test
    @DisplayName("ENT-FX-01: ExchangeRateSource/Quote exponen sus campos y fijan recorded_at en @PrePersist")
    void exchangeRateEntitiesGettersAndSettersAndPrePersist() {
        ExchangeRateSourceEntity source = new ExchangeRateSourceEntity();
        assertThat(source.getIsEnabled()).isTrue();
        source.setExchangeRateSourceId(1);
        source.setName("BCB");
        source.setWebsiteUrl("https://www.bcb.gob.bo");
        source.setIsEnabled(false);
        assertThat(source.getExchangeRateSourceId()).isEqualTo(1);
        assertThat(source.getName()).isEqualTo("BCB");
        assertThat(source.getWebsiteUrl()).isEqualTo("https://www.bcb.gob.bo");
        assertThat(source.getIsEnabled()).isFalse();

        ExchangeRateQuoteEntity quote = new ExchangeRateQuoteEntity();
        quote.setExchangeRateQuoteId(9);
        quote.setSource(source);
        quote.setRateKind("OFICIAL");
        quote.setBobPerUsd(new BigDecimal("6.960000"));
        quote.setQuotedAt(NOW);
        // recorded_at nula se rellena en @PrePersist.
        assertThat(quote.getRecordedAt()).isNull();
        quote.onCreate();
        assertThat(quote.getRecordedAt()).isNotNull();
        // Con recorded_at ya fijado, el callback la respeta.
        quote.setRecordedAt(LATER);
        quote.onCreate();
        assertThat(quote.getRecordedAt()).isEqualTo(LATER);

        assertThat(quote.getExchangeRateQuoteId()).isEqualTo(9);
        assertThat(quote.getSource()).isEqualTo(source);
        assertThat(quote.getRateKind()).isEqualTo("OFICIAL");
        assertThat(quote.getBobPerUsd()).isEqualByComparingTo("6.960000");
        assertThat(quote.getQuotedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("ENT-PROJHIST-01: ProjectPlanHistoryId/Entity exponen campos y respetan changed_at")
    void projectPlanHistoryGettersAndPrePersist() {
        ProjectPlanHistoryId id = new ProjectPlanHistoryId(7, 1);
        assertThat(id.getProjectId()).isEqualTo(7);
        assertThat(id.getRevisionNumber()).isEqualTo(1);
        ProjectPlanHistoryId settable = new ProjectPlanHistoryId();
        settable.setProjectId(8);
        settable.setRevisionNumber(2);
        assertThat(settable.getProjectId()).isEqualTo(8);
        assertThat(settable.getRevisionNumber()).isEqualTo(2);

        ProjectEntity project = new ProjectEntity();
        project.setProjectId(7);
        AppUserEntity author = new AppUserEntity();
        author.setUserId(1);
        ProjectPlanHistoryEntity entity = new ProjectPlanHistoryEntity();
        assertThat(entity.getId()).isNotNull();
        entity.setId(id);
        entity.setProject(project);
        entity.setBudgetBob(new BigDecimal("9000.00"));
        entity.setEstimatedDurationMinutes(600);
        ExchangeRateQuoteEntity quote = new ExchangeRateQuoteEntity();
        entity.setExchangeRateQuote(quote);
        entity.setChangedBy(author);
        entity.setReason("Plan inicial");
        entity.onCreate();
        assertThat(entity.getChangedAt()).isNotNull();
        entity.setChangedAt(LATER);
        entity.onCreate();
        assertThat(entity.getChangedAt()).isEqualTo(LATER);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getProject()).isEqualTo(project);
        assertThat(entity.getBudgetBob()).isEqualByComparingTo("9000.00");
        assertThat(entity.getEstimatedDurationMinutes()).isEqualTo(600);
        assertThat(entity.getExchangeRateQuote()).isEqualTo(quote);
        assertThat(entity.getChangedBy()).isEqualTo(author);
        assertThat(entity.getReason()).isEqualTo("Plan inicial");
    }

    @Test
    @DisplayName("ENT-TASKHIST-01: TaskPlanHistoryId/Entity exponen campos y respetan changed_at")
    void taskPlanHistoryGettersAndPrePersist() {
        TaskPlanHistoryId id = new TaskPlanHistoryId(31, 1);
        assertThat(id.getTaskId()).isEqualTo(31);
        assertThat(id.getRevisionNumber()).isEqualTo(1);
        TaskPlanHistoryId settable = new TaskPlanHistoryId();
        settable.setTaskId(32);
        settable.setRevisionNumber(2);
        assertThat(settable.getTaskId()).isEqualTo(32);
        assertThat(settable.getRevisionNumber()).isEqualTo(2);

        TaskEntity task = new TaskEntity();
        task.setTaskId(31);
        AppUserEntity author = new AppUserEntity();
        author.setUserId(1);
        TaskPlanHistoryEntity entity = new TaskPlanHistoryEntity();
        assertThat(entity.getId()).isNotNull();
        entity.setId(id);
        entity.setTask(task);
        entity.setAllocatedBudgetBob(new BigDecimal("150.75"));
        entity.setEstimatedDurationMinutes(90);
        ExchangeRateQuoteEntity quote = new ExchangeRateQuoteEntity();
        entity.setExchangeRateQuote(quote);
        entity.setChangedBy(author);
        entity.setReason("Plan inicial");
        entity.onCreate();
        assertThat(entity.getChangedAt()).isNotNull();
        entity.setChangedAt(LATER);
        entity.onCreate();
        assertThat(entity.getChangedAt()).isEqualTo(LATER);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getTask()).isEqualTo(task);
        assertThat(entity.getAllocatedBudgetBob()).isEqualByComparingTo("150.75");
        assertThat(entity.getEstimatedDurationMinutes()).isEqualTo(90);
        assertThat(entity.getExchangeRateQuote()).isEqualTo(quote);
        assertThat(entity.getChangedBy()).isEqualTo(author);
        assertThat(entity.getReason()).isEqualTo("Plan inicial");
    }

    @Test
    @DisplayName("ENT-STATUSHIST-01: TaskStatusHistoryEntity expone su transición y respeta changed_at")
    void taskStatusHistoryGettersAndPrePersist() {
        TaskEntity task = new TaskEntity();
        task.setTaskId(31);
        WorkStatusEntity previous = new WorkStatusEntity();
        previous.setName("PENDIENTE");
        WorkStatusEntity next = new WorkStatusEntity();
        next.setName("EN_PROGRESO");
        AppUserEntity author = new AppUserEntity();
        author.setUserId(1);

        TaskStatusHistoryEntity entity = new TaskStatusHistoryEntity();
        entity.setTaskStatusHistoryId(100);
        entity.setTask(task);
        entity.setPreviousStatus(previous);
        entity.setNewStatus(next);
        entity.setChangedBy(author);
        entity.setReason("Tarea iniciada");
        entity.onCreate();
        assertThat(entity.getChangedAt()).isNotNull();
        entity.setChangedAt(LATER);
        entity.onCreate();
        assertThat(entity.getChangedAt()).isEqualTo(LATER);

        assertThat(entity.getTaskStatusHistoryId()).isEqualTo(100);
        assertThat(entity.getTask()).isEqualTo(task);
        assertThat(entity.getPreviousStatus()).isEqualTo(previous);
        assertThat(entity.getNewStatus()).isEqualTo(next);
        assertThat(entity.getChangedBy()).isEqualTo(author);
        assertThat(entity.getReason()).isEqualTo("Tarea iniciada");
    }

    @Test
    @DisplayName("ENT-ASSIGN-01: TaskAssignmentId/Entity exponen su clave compuesta y fijan assigned_at")
    void taskAssignmentGettersAndPrePersist() {
        TaskAssignmentId id = new TaskAssignmentId(31, 2);
        assertThat(id.getTaskId()).isEqualTo(31);
        assertThat(id.getWorkerUserId()).isEqualTo(2);
        TaskAssignmentId settable = new TaskAssignmentId();
        settable.setTaskId(32);
        settable.setWorkerUserId(3);
        assertThat(settable.getTaskId()).isEqualTo(32);
        assertThat(settable.getWorkerUserId()).isEqualTo(3);

        TaskEntity task = new TaskEntity();
        task.setTaskId(31);
        AppUserEntity worker = new AppUserEntity();
        worker.setUserId(2);
        AppUserEntity chief = new AppUserEntity();
        chief.setUserId(1);

        TaskAssignmentEntity entity = new TaskAssignmentEntity();
        assertThat(entity.getId()).isNotNull();
        entity.setId(id);
        entity.setTask(task);
        entity.setWorker(worker);
        entity.setAssignedBy(chief);
        // assigned_at nula se rellena en @PrePersist.
        assertThat(entity.getAssignedAt()).isNull();
        entity.onCreate();
        assertThat(entity.getAssignedAt()).isNotNull();
        entity.setAssignedAt(LATER);
        entity.onCreate();
        assertThat(entity.getAssignedAt()).isEqualTo(LATER);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getTask()).isEqualTo(task);
        assertThat(entity.getWorker()).isEqualTo(worker);
        assertThat(entity.getAssignedBy()).isEqualTo(chief);
    }

    @Test
    @DisplayName("ENT-AUDIT-01: AuditedEntity.onCreate/onUpdate fijan las marcas de tiempo de auditoría")
    void auditedEntityLifecycleCallbacks() {
        TaskEntity fresh = new TaskEntity();
        assertThat(fresh.getCreatedAt()).isNull();
        assertThat(fresh.getUpdatedAt()).isNull();
        fresh.onCreate();
        assertThat(fresh.getCreatedAt()).isNotNull();
        assertThat(fresh.getUpdatedAt()).isEqualTo(fresh.getCreatedAt());
        Instant created = fresh.getCreatedAt();

        // Con las marcas ya definidas, onCreate no las pisa.
        TaskEntity dated = new TaskEntity();
        dated.setCreatedAt(NOW);
        dated.setUpdatedAt(NOW);
        dated.onCreate();
        assertThat(dated.getCreatedAt()).isEqualTo(NOW);
        assertThat(dated.getUpdatedAt()).isEqualTo(NOW);

        // onUpdate siempre refresca updated_at conservando created_at.
        dated.onUpdate();
        assertThat(dated.getCreatedAt()).isEqualTo(NOW);
        assertThat(dated.getUpdatedAt()).isNotEqualTo(NOW);
        assertThat(created).isNotNull();
    }

    @Test
    @DisplayName("ENT-ID-EQUALS-01: las claves compuestas cumplen equals/hashCode simétricos")
    void compositeIdEqualsAndHashCode() {
        ProjectPlanHistoryId projectA = new ProjectPlanHistoryId(7, 1);
        ProjectPlanHistoryId projectB = new ProjectPlanHistoryId(7, 1);
        ProjectPlanHistoryId projectC = new ProjectPlanHistoryId(8, 1);
        assertThat(projectA).isEqualTo(projectA);
        assertThat(projectA).isEqualTo(projectB);
        assertThat(projectA).hasSameHashCodeAs(projectB);
        assertThat(projectA).isNotEqualTo(projectC);
        assertThat(projectA).doesNotHaveSameHashCodeAs(projectC);
        assertThat(projectA).isNotEqualTo(null);
        assertThat(projectA).isNotEqualTo("no-id");

        TaskPlanHistoryId taskA = new TaskPlanHistoryId(31, 1);
        TaskPlanHistoryId taskB = new TaskPlanHistoryId(31, 1);
        assertThat(taskA).isEqualTo(taskB).hasSameHashCodeAs(taskB);
        assertThat(taskA).isNotEqualTo(new TaskPlanHistoryId(31, 2));
        assertThat(taskA).isNotEqualTo(null);

        TaskAssignmentId assignmentA = new TaskAssignmentId(31, 2);
        TaskAssignmentId assignmentB = new TaskAssignmentId(31, 2);
        assertThat(assignmentA).isEqualTo(assignmentB).hasSameHashCodeAs(assignmentB);
        assertThat(assignmentA).isNotEqualTo(new TaskAssignmentId(31, 3));
        assertThat(assignmentA).isNotEqualTo(new TaskPlanHistoryId(31, 2));
        assertThat(assignmentA).isNotEqualTo(null);
    }
}
