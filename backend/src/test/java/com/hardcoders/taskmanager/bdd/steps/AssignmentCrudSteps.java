package com.hardcoders.taskmanager.bdd.steps;

import com.hardcoders.taskmanager.bdd.context.ScenarioContext;
import com.hardcoders.taskmanager.bdd.support.ApiClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step Definitions del CRUD de {@code task_assignment} (ASG-INS-01..03, ASG-UPD-01..03,
 * ASG-DEL-01, ASG-SEL-01, ASG-SEL-02). Todas las llamadas son HTTP reales contra la API del
 * backend y la verificacion de persistencia se hace contra PostgreSQL de integracion
 * (gestor_tareas_test).
 */
public class AssignmentCrudSteps {

    @Autowired
    private Environment env;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ScenarioContext context;

    private ApiClient apiClient;

    @jakarta.annotation.PostConstruct
    void init() {
        int port = env.getProperty("local.server.port", Integer.class, 8080);
        this.apiClient = new ApiClient(port);
    }

    /* ==================== Given ==================== */

    @Given("a task exists for the authenticated chief's project")
    public void a_task_exists_for_the_authenticated_chiefs_project() {
        // Obtener el userId del jefe autenticado.
        Integer chiefUserId = jdbc.queryForObject(
                "SELECT user_id FROM task_manager.app_user WHERE email = ? AND is_chief = true AND is_active = true",
                Integer.class, context.getChiefEmail());
        if (chiefUserId == null) {
            throw new IllegalStateException("No se encontro usuario jefe activo para el email: " + context.getChiefEmail());
        }

        // Crear proyecto, etapa, actividad y tarea usando el jefe autenticado.
        Integer projectId = apiClient.createProject(
                context.getAuthToken(),
                "Proyecto ASG " + UUID.randomUUID(),
                "Proyecto para pruebas de asignacion",
                chiefUserId, "1000.00", "120", "Plan inicial");
        context.setProjectId(projectId);

        Integer stageId = apiClient.createStage(
                context.getAuthToken(), projectId, "Etapa ASG", null);
        context.setStageId(stageId);

        Integer activityId = apiClient.createActivity(
                context.getAuthToken(), stageId, "Actividad ASG", null);
        context.setActivityId(activityId);

        Integer taskId = apiClient.createTask(
                context.getAuthToken(), activityId, "Tarea ASG", "MEDIA", "100.00", "60");
        context.setTaskId(taskId);
    }

    @Given("an active worker exists")
    public void an_active_worker_exists() {
        String email = "worker." + UUID.randomUUID() + "@gestor.local";
        ResponseEntity r = apiClient.postUser(context.getAuthToken(),
                Map.of("email", email, "givenNames", "Worker", "familyNames", "Test", "password", "UserPass123!"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        context.setWorkerUserId(((Number) ((Map) r.getBody()).get("userId")).intValue());
    }

    @Given("two active workers exist")
    public void two_active_workers_exist() {
        // Primer trabajador (ya puede existir del Given anterior, pero creamos dos nuevos).
        createActiveWorker("worker.a");
        createActiveWorker("worker.b");
    }

    private void createActiveWorker(String prefix) {
        String email = prefix + "." + UUID.randomUUID() + "@gestor.local";
        ResponseEntity r = apiClient.postUser(context.getAuthToken(),
                Map.of("email", email, "givenNames", prefix, "familyNames", "Test", "password", "UserPass123!"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Given("the worker is assigned to the task")
    public void the_worker_is_assigned_to_the_task() {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        ResponseEntity r = apiClient.postAssignment(context.getAuthToken(), taskId,
                Map.of("workerUserId", workerId, "reason", "Motivo de asignacion previa"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Given("the worker is assigned to the task with reason {string}")
    public void the_worker_is_assigned_to_the_task_with_reason(String reason) {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        ResponseEntity r = apiClient.postAssignment(context.getAuthToken(), taskId,
                Map.of("workerUserId", workerId, "reason", reason));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        context.setLastPutResponse(r);
    }

    @Given("both workers are assigned to the task")
    public void both_workers_are_assigned_to_the_task() {
        // Los dos trabajadores creados en el Given anterior se asignan a la tarea.
        // Recuperamos los userId de app_user creados recientemente.
        List<Map<String, Object>> users = jdbc.queryForList(
                "SELECT user_id FROM task_manager.app_user WHERE email LIKE 'worker.%@gestor.local' ORDER BY user_id DESC LIMIT 2");
        assertThat(users).hasSize(2);
        Integer taskId = requireTaskId();
        for (Map<String, Object> row : users) {
            Integer workerId = ((Number) row.get("user_id")).intValue();
            ResponseEntity r = apiClient.postAssignment(context.getAuthToken(), taskId,
                    Map.of("workerUserId", workerId, "reason", "Motivo de asignacion"));
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    /* ==================== When ==================== */

    @When("I assign the worker to the task with reason {string}")
    public void i_assign_the_worker_to_the_task_with_reason(String reason) {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        ResponseEntity r = apiClient.postAssignment(context.getAuthToken(), taskId,
                Map.of("workerUserId", workerId, "reason", reason));
        context.setLastPutResponse(r);
    }

    @When("I assign the worker to the task without a reason")
    public void i_assign_the_worker_to_the_task_without_a_reason() {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("workerUserId", workerId);
        body.put("reason", null);
        ResponseEntity r = apiClient.postAssignment(context.getAuthToken(), taskId, body);
        context.setLastPutResponse(r);
    }

    @When("I assign the worker to the task with a reason of 501 characters")
    public void i_assign_the_worker_to_the_task_with_a_reason_of_501_characters() {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        String longReason = "x".repeat(501);
        ResponseEntity r = apiClient.postAssignment(context.getAuthToken(), taskId,
                Map.of("workerUserId", workerId, "reason", longReason));
        context.setLastPutResponse(r);
    }

    @When("I update the assignment reason to {string}")
    public void i_update_the_assignment_reason_to(String reason) {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        ResponseEntity r = apiClient.putAssignmentReason(context.getAuthToken(), taskId, workerId,
                Map.of("reason", reason));
        context.setLastPutResponse(r);
    }

    @When("I update the assignment reason to an empty value")
    public void i_update_the_assignment_reason_to_an_empty_value() {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        ResponseEntity r = apiClient.putAssignmentReason(context.getAuthToken(), taskId, workerId,
                Map.of("reason", ""));
        context.setLastPutResponse(r);
    }

    @When("I update the assignment reason to a value of {int} characters")
    public void i_update_the_assignment_reason_to_a_value_of_characters(int length) {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        String longReason = "x".repeat(length);
        ResponseEntity r = apiClient.putAssignmentReason(context.getAuthToken(), taskId, workerId,
                Map.of("reason", longReason));
        context.setLastPutResponse(r);
    }

    @When("I revoke the worker's assignment from the task")
    public void i_revoke_the_workers_assignment_from_the_task() {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        ResponseEntity r = apiClient.deleteAssignment(context.getAuthToken(), taskId, workerId);
        context.setLastPutResponse(r);
    }

    @When("I consult the assignment by task and worker")
    public void i_consult_the_assignment_by_task_and_worker() {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        ResponseEntity r = apiClient.getAssignment(context.getAuthToken(), taskId, workerId);
        context.setLastGetResponse(r);
    }

    @When("I list assignments of the task")
    public void i_list_assignments_of_the_task() {
        Integer taskId = requireTaskId();
        ResponseEntity r = apiClient.listAssignments(context.getAuthToken(), taskId);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        context.setLastListResponse(r);
    }

    /* ==================== Then ==================== */

    @Then("the assignment is created with status 200")
    public void the_assignment_is_created_with_status_200() {
        assertThat(context.getLastPutResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Then("the response reason is normalized to {string}")
    public void the_response_reason_is_normalized_to(String expectedReason) {
        Map body = (Map) context.getLastPutResponse().getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("reason")).isEqualTo(expectedReason);
    }

    @Then("the response reason is the default {string}")
    public void the_response_reason_is_the_default(String expectedReason) {
        Map body = (Map) context.getLastPutResponse().getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("reason")).isEqualTo(expectedReason);
    }

    @Then("the assignment is stored in PostgreSQL with the normalized reason")
    public void the_assignment_is_stored_in_postgresql_with_the_normalized_reason() {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        String reason = jdbc.queryForObject(
                "SELECT reason FROM task_manager.task_assignment WHERE task_id = ? AND worker_user_id = ?",
                String.class, taskId, workerId);
        assertThat(reason).isEqualTo("Experiencia en cimentación");
    }

    @Then("the assignment is stored in PostgreSQL with the default reason")
    public void the_assignment_is_stored_in_postgresql_with_the_default_reason() {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        String reason = jdbc.queryForObject(
                "SELECT reason FROM task_manager.task_assignment WHERE task_id = ? AND worker_user_id = ?",
                String.class, taskId, workerId);
        assertThat(reason).isEqualTo("Asignación de Personal");
    }

    @Then("no assignment is stored in PostgreSQL for that worker")
    public void no_assignment_is_stored_in_postgresql_for_that_worker() {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM task_manager.task_assignment WHERE task_id = ? AND worker_user_id = ?",
                Integer.class, taskId, workerId);
        assertThat(count).isZero();
    }

    @Then("the response reason is {string}")
    public void the_response_reason_is(String expectedReason) {
        Map body = (Map) context.getLastPutResponse().getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("reason")).isEqualTo(expectedReason);
    }

    @Then("the assignment keeps the same task, worker and audit fields")
    public void the_assignment_keeps_the_same_task_worker_and_audit_fields() {
        // Verifica que la consulta individual devuelve los mismos identificadores de tarea y trabajador.
        Map body = (Map) context.getLastPutResponse().getBody();
        assertThat(body.get("taskId")).isEqualTo(requireTaskId());
        assertThat(body.get("workerUserId")).isEqualTo(requireWorkerId());
        assertThat(body.get("assignedAt")).isNotNull();
        assertThat(body.get("assignedByUserId")).isNotNull();
    }

    @Then("the assignment still has reason {string} in PostgreSQL")
    public void the_assignment_still_has_reason_in_postgresql(String expectedReason) {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        String reason = jdbc.queryForObject(
                "SELECT reason FROM task_manager.task_assignment WHERE task_id = ? AND worker_user_id = ?",
                String.class, taskId, workerId);
        assertThat(reason).isEqualTo(expectedReason);
    }

    @Then("the response is 204")
    public void the_response_is_204() {
        assertThat(context.getLastPutResponse().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Then("the assignment no longer exists in PostgreSQL")
    public void the_assignment_no_longer_exists_in_postgresql() {
        Integer taskId = requireTaskId();
        Integer workerId = requireWorkerId();
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM task_manager.task_assignment WHERE task_id = ? AND worker_user_id = ?",
                Integer.class, taskId, workerId);
        assertThat(count).isZero();
    }

    @Then("the response shows the worker, task and reason")
    public void the_response_shows_the_worker_task_and_reason() {
        Map body = (Map) context.getLastGetResponse().getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("taskId")).isEqualTo(requireTaskId());
        assertThat(body.get("workerUserId")).isEqualTo(requireWorkerId());
        assertThat(body.get("reason")).isEqualTo("Motivo de consulta");
        assertThat(body.get("workerName")).isNotNull();
    }

    @Then("the list contains both assignments")
    public void the_list_contains_both_assignments() {
        ResponseEntity r = context.getLastListResponse();
        List body = (List) r.getBody();
        assertThat(body).hasSize(2);
    }

    /* ==================== Helpers ==================== */

    private Integer requireTaskId() {
        Integer taskId = context.getTaskId();
        if (taskId == null) {
            throw new IllegalStateException("No hay una tarea en el contexto del escenario.");
        }
        return taskId;
    }

    private Integer requireWorkerId() {
        Integer workerId = context.getWorkerUserId();
        if (workerId == null) {
            throw new IllegalStateException("No hay un trabajador en el contexto del escenario.");
        }
        return workerId;
    }
}
