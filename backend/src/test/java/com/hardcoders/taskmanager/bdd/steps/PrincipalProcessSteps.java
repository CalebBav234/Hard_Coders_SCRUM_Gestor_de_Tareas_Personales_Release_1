package com.hardcoders.taskmanager.bdd.steps;

import com.hardcoders.taskmanager.bdd.context.ScenarioContext;
import com.hardcoders.taskmanager.bdd.support.ApiClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;

/**
 * Step Definitions del proceso principal PP-01:
 * Proyecto -> Etapa -> Actividad -> Tarea -> Subtarea.
 * Todas las llamadas son HTTP reales contra la API del backend en ejecucion.
 * Nota: sin @Component; Cucumber-Spring registra las glue classes como beans Spring.
 */
public class PrincipalProcessSteps {

    @Autowired
    private Environment env;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ScenarioContext context;

    private ApiClient apiClient;

    @PostConstruct
    void init() {
        int port = env.getProperty("local.server.port", Integer.class, 8080);
        this.apiClient = new ApiClient(port);
    }

    @Given("an authenticated chief user with email {string} and password {string}")
    public void an_authenticated_chief_user_with_email_and_password(String email, String password) {
        // El usuario ya se creo y autentico en Hooks.beforeScenario();
        // este step solo valida que el token exista.
        String token = context.getAuthToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No hay token de autenticacion. El usuario jefe no se autentico.");
        }
        // No sobrescribir context.chiefEmail: el email real es el generado en Hooks
        // (el email del Gherkin es solo documentacion).
    }

    @When("I create a project with name {string} and description {string}")
    public void i_create_a_project_with_name_and_description(String name, String description) {
        Integer managerUserId = jdbc.queryForObject(
                "SELECT user_id FROM task_manager.app_user WHERE email = ? AND is_chief = true AND is_active = true",
                Integer.class, context.getChiefEmail()
        );
        if (managerUserId == null) {
            throw new IllegalStateException("No se encontro usuario jefe activo para el email: " + context.getChiefEmail());
        }

        Integer projectId = apiClient.createProject(
                context.getAuthToken(),
                name, description, managerUserId,
                "10000.00", "120", "Plan inicial del proyecto"
        );
        context.setProjectId(projectId);
    }

    @And("I create a stage with name {string} in the project")
    public void i_create_a_stage_with_name_in_the_project(String stageName) {
        Integer stageId = apiClient.createStage(
                context.getAuthToken(),
                context.getProjectId(),
                stageName, "Etapa de prueba para EC4"
        );
        context.setStageId(stageId);
    }

    @And("I create an activity with name {string} in the stage")
    public void i_create_an_activity_with_name_in_the_stage(String activityName) {
        Integer activityId = apiClient.createActivity(
                context.getAuthToken(),
                context.getStageId(),
                activityName, "Actividad de prueba para EC4"
        );
        context.setActivityId(activityId);
    }

    @And("I create a task with title {string} in the activity")
    public void i_create_a_task_with_title_in_the_activity(String taskTitle) {
        Integer taskId = apiClient.createTask(
                context.getAuthToken(),
                context.getActivityId(),
                taskTitle, "MEDIA", "500.00", "60"
        );
        context.setTaskId(taskId);
    }

    @And("I create a subtask with title {string} in the task")
    public void i_create_a_subtask_with_title_in_the_task(String subtaskTitle) {
        Integer subtaskId = apiClient.createSubtask(
                context.getAuthToken(),
                context.getTaskId(),
                subtaskTitle, "Subtarea de prueba para EC4"
        );
        context.setSubtaskId(subtaskId);
    }

    @Then("the hierarchy Project - Stage - Activity - Task - Subtask is persisted")
    public void the_hierarchy_is_persisted() {
        if (!context.isComplete()) {
            throw new IllegalStateException("La jerarquia no esta completa. Falta identificadores: " + context);
        }

        Integer projectCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_manager.project WHERE project_id = ?",
                Integer.class, context.getProjectId()
        );
        if (projectCount == null || projectCount != 1) {
            throw new IllegalStateException("Proyecto no persistido: project_id=" + context.getProjectId());
        }

        Integer stageCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_manager.stage WHERE stage_id = ? AND project_id = ?",
                Integer.class, context.getStageId(), context.getProjectId()
        );
        if (stageCount == null || stageCount != 1) {
            throw new IllegalStateException("Etapa no persistida: stage_id=" + context.getStageId());
        }

        Integer activityCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_manager.activity WHERE activity_id = ? AND stage_id = ?",
                Integer.class, context.getActivityId(), context.getStageId()
        );
        if (activityCount == null || activityCount != 1) {
            throw new IllegalStateException("Actividad no persistida: activity_id=" + context.getActivityId());
        }

        Integer taskCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_manager.task WHERE task_id = ? AND activity_id = ?",
                Integer.class, context.getTaskId(), context.getActivityId()
        );
        if (taskCount == null || taskCount != 1) {
            throw new IllegalStateException("Tarea no persistida: task_id=" + context.getTaskId());
        }

        Integer subtaskCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_manager.subtask WHERE subtask_id = ? AND task_id = ?",
                Integer.class, context.getSubtaskId(), context.getTaskId()
        );
        if (subtaskCount == null || subtaskCount != 1) {
            throw new IllegalStateException("Subtarea no persistida: subtask_id=" + context.getSubtaskId());
        }
    }

    @And("the project has 1 stage")
    public void the_project_has_1_stage() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_manager.stage WHERE project_id = ?",
                Integer.class, context.getProjectId()
        );
        if (count == null || count != 1) {
            throw new IllegalStateException("El proyecto deberia tener exactamente 1 etapa, tiene: " + count);
        }
    }

    @And("the stage has 1 activity")
    public void the_stage_has_1_activity() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_manager.activity WHERE stage_id = ?",
                Integer.class, context.getStageId()
        );
        if (count == null || count != 1) {
            throw new IllegalStateException("La etapa deberia tener exactamente 1 actividad, tiene: " + count);
        }
    }

    @And("the activity has 1 task")
    public void the_activity_has_1_task() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_manager.task WHERE activity_id = ?",
                Integer.class, context.getActivityId()
        );
        if (count == null || count != 1) {
            throw new IllegalStateException("La actividad deberia tener exactamente 1 tarea, tiene: " + count);
        }
    }

    @And("the task has 1 subtask")
    public void the_task_has_1_subtask() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_manager.subtask WHERE task_id = ?",
                Integer.class, context.getTaskId()
        );
        if (count == null || count != 1) {
            throw new IllegalStateException("La tarea deberia tener exactamente 1 subtarea, tiene: " + count);
        }
    }
}
