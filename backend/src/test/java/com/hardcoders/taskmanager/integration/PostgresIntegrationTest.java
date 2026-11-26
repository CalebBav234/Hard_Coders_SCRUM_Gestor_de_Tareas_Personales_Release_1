package com.hardcoders.taskmanager.integration;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.repository.AppUserJpaRepository;
import com.hardcoders.taskmanager.domain.model.TaskAssignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostgresIntegrationTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/gestor_tareas_test");
        r.add("spring.datasource.username", () -> "gestor_tareas_owner");
        r.add("spring.datasource.password", () -> "ownerpass");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("app.security.jwt.secret", () -> "test-secret-for-etapa4-integration-0123456789");
        r.add("app.security.jwt.expiration-minutes", () -> "60");
        r.add("app.security.jwt.issuer", () -> "gestor-tareas-test");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired AppUserJpaRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired PlatformTransactionManager txManager;
    @LocalServerPort int port;

    private final RestTemplate rest = new RestTemplate();
    private TransactionTemplate tx;

    @BeforeEach
    void clean() {
        tx = new TransactionTemplate(txManager);
        rest.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override public boolean hasError(ClientHttpResponse response) throws IOException { return false; }
        });
        tx.executeWithoutResult(s -> {
            jdbc.update("DELETE FROM task_manager.task_assignment");
            jdbc.update("DELETE FROM task_manager.task_plan_history");
            jdbc.update("DELETE FROM task_manager.task_status_history");
            jdbc.update("DELETE FROM task_manager.subtask");
            jdbc.update("DELETE FROM task_manager.task");
            jdbc.update("DELETE FROM task_manager.activity");
            jdbc.update("DELETE FROM task_manager.stage");
            jdbc.update("DELETE FROM task_manager.project_plan_history");
            jdbc.update("DELETE FROM task_manager.project");
            jdbc.update("DELETE FROM task_manager.app_user");
        });
    }

    private String uniqueEmail(String prefix) { return prefix + "-" + UUID.randomUUID() + "@gestor.local"; }
    private String url(String path) { return "http://localhost:" + port + path; }
    private HttpHeaders authJson(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }
    private HttpHeaders json() { HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON); return h; }

    private AppUserEntity saveUser(String email, boolean chief, boolean active, String rawPassword) {
        AppUserEntity u = new AppUserEntity();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setGivenNames("Name");
        u.setFamilyNames("Last");
        u.setIsChief(chief);
        u.setIsActive(active);
        return users.save(u);
    }
    private AppUserEntity saveUser(String email, boolean chief, String rawPassword) { return saveUser(email, chief, true, rawPassword); }

    @SuppressWarnings("unchecked")
    private String login(String email, String password) {
        ResponseEntity<Map> r = rest.exchange(url("/api/auth/login"), HttpMethod.POST,
                new HttpEntity<>(Map.of("email", email, "password", password), json()), Map.class);
        assertThat(r.getStatusCode()).as("login status body=%s", r.getBody()).isEqualTo(HttpStatus.OK);
        return (String) Objects.requireNonNull(r.getBody()).get("token");
    }

    private int projectStatusId(String name) { return jdbc.queryForObject("SELECT project_status_id FROM task_manager.project_status WHERE name=?", Integer.class, name); }
    private int workStatusId(String name) { return jdbc.queryForObject("SELECT work_status_id FROM task_manager.work_status WHERE name=?", Integer.class, name); }
    private int priorityId(String name) { return jdbc.queryForObject("SELECT task_priority_id FROM task_manager.task_priority WHERE name=?", Integer.class, name); }

    private void seedHierarchy(int projectId, String projectStatus, int chiefId, int managerId, int taskId) {
        tx.executeWithoutResult(s -> {
            int planned = projectStatusId(projectStatus);
            int pend = workStatusId("PENDIENTE");
            int prio = priorityId("MEDIA");
            jdbc.update("INSERT INTO task_manager.project(project_id,name,manager_user_id,project_status_id,budget_bob,estimated_duration_minutes,created_by_user_id,created_at,updated_at) VALUES (?,?,?,?,0,0,?,now(),now())", projectId, "P"+projectId, managerId, planned, chiefId);
            jdbc.update("INSERT INTO task_manager.project_plan_history(project_id,revision_number,budget_bob,estimated_duration_minutes,changed_by_user_id,reason,changed_at) VALUES (?,?,0,0,?,'init',now())", projectId, 1, chiefId);
            jdbc.update("INSERT INTO task_manager.stage(stage_id,project_id,name,work_status_id,created_at,updated_at,sort_order) VALUES (?,?, 'S', ?,now(),now(),0)", projectId, projectId, pend);
            jdbc.update("INSERT INTO task_manager.activity(activity_id,stage_id,name,work_status_id,created_at,updated_at,sort_order) VALUES (?,?, 'A', ?,now(),now(),0)", projectId, projectId, pend);
            jdbc.update("INSERT INTO task_manager.task(task_id,activity_id,title,work_status_id,task_priority_id,allocated_budget_bob,estimated_duration_minutes,created_at,updated_at,sort_order) VALUES (?,?, 'T', ?,?,0,0,now(),now(),0)", taskId, projectId, pend, prio);
            jdbc.update("INSERT INTO task_manager.task_plan_history(task_id,revision_number,allocated_budget_bob,estimated_duration_minutes,changed_by_user_id,reason,changed_at) VALUES (?,?,0,0,?,'init',now())", taskId, 1, chiefId);
            jdbc.update("INSERT INTO task_manager.task_status_history(task_id,previous_status_id,new_status_id,changed_by_user_id,reason,changed_at) VALUES (?,NULL,?,?, 'init',now())", taskId, pend, chiefId);
        });
    }

    @Nested class MigrationsAndReason {
        @Test @DisplayName("MIG-V001..V006 y reason nullable")
        void migrationsAppliedAndReasonColumn() {
            List<Integer> versions = jdbc.queryForList("SELECT version FROM public.database_schema_migrations ORDER BY version", Integer.class);
            assertThat(versions).containsExactly(1, 2, 3, 4, 5, 6);
            String nullable = jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema='task_manager' AND table_name='task_assignment' AND column_name='reason'", String.class);
            assertThat(nullable).isEqualTo("YES");
        }

        @Test @DisplayName("CA-ASG-12/COMPAT: reason explícito, default y lectura histórica NULL")
        void taskAssignmentReasonWriteReadAndHistoricalNull() {
            AppUserEntity chief = saveUser(uniqueEmail("chief"), true, "ChiefPass123!");
            AppUserEntity mgr = saveUser(uniqueEmail("mgr"), false, "SeedPass123!");
            AppUserEntity worker = saveUser(uniqueEmail("wrk"), false, "SeedPass123!");
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), mgr.getUserId(), 100);
            tx.executeWithoutResult(s -> {
                jdbc.update("INSERT INTO task_manager.task_assignment(task_id,worker_user_id,assigned_at,assigned_by_user_id,reason) VALUES (100,?,?,?,NULL)", worker.getUserId(), Timestamp.from(Instant.now()), chief.getUserId());
                assertThat((Object) jdbc.queryForObject("SELECT reason FROM task_manager.task_assignment WHERE task_id=100 AND worker_user_id=?", String.class, worker.getUserId())).isNull();
                jdbc.update("DELETE FROM task_manager.task_assignment WHERE task_id=100");
                jdbc.update("INSERT INTO task_manager.task_assignment(task_id,worker_user_id,assigned_at,assigned_by_user_id,reason) VALUES (100,?,?,?,?)", worker.getUserId(), Timestamp.from(Instant.now()), chief.getUserId(), "Motivo IT");
                assertThat(jdbc.queryForObject("SELECT reason FROM task_manager.task_assignment WHERE task_id=100 AND worker_user_id=?", String.class, worker.getUserId())).isEqualTo("Motivo IT");
                jdbc.update("DELETE FROM task_manager.task_assignment WHERE task_id=100");
                jdbc.update("INSERT INTO task_manager.task_assignment(task_id,worker_user_id,assigned_at,assigned_by_user_id,reason) VALUES (100,?,?,?,?)", worker.getUserId(), Timestamp.from(Instant.now()), chief.getUserId(), TaskAssignment.DEFAULT_REASON);
                assertThat(jdbc.queryForObject("SELECT reason FROM task_manager.task_assignment WHERE task_id=100 AND worker_user_id=?", String.class, worker.getUserId())).isEqualTo(TaskAssignment.DEFAULT_REASON);
            });
        }

        @Test @DisplayName("CA-ASG-07 (BD): PK compuesta rechaza duplicada")
        void duplicateAssignmentRejectedByPrimaryKey() {
            AppUserEntity chief = saveUser(uniqueEmail("chiefd"), true, "ChiefPass123!");
            AppUserEntity mgr = saveUser(uniqueEmail("mgrd"), false, "SeedPass123!");
            AppUserEntity worker = saveUser(uniqueEmail("wrkd"), false, "SeedPass123!");
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), mgr.getUserId(), 100);
            tx.executeWithoutResult(s -> jdbc.update("INSERT INTO task_manager.task_assignment(task_id,worker_user_id,assigned_at,assigned_by_user_id,reason) VALUES (100,?,?,?,?)", worker.getUserId(), Timestamp.from(Instant.now()), chief.getUserId(), TaskAssignment.DEFAULT_REASON));
            assertThatThrownBy(() -> tx.executeWithoutResult(s -> jdbc.update("INSERT INTO task_manager.task_assignment(task_id,worker_user_id,assigned_at,assigned_by_user_id,reason) VALUES (100,?,?,?,?)", worker.getUserId(), Timestamp.from(Instant.now()), chief.getUserId(), TaskAssignment.DEFAULT_REASON))).isInstanceOf(Exception.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.task_assignment WHERE task_id=100 AND worker_user_id=?", Integer.class, worker.getUserId())).isEqualTo(1);
        }
    }

    @Nested class AssignmentApi {
        private AppUserEntity chief, manager, worker, other;
        private String chiefToken, managerToken, workerToken;
        private final String CHIEF_PWD = "ChiefPass123!";

        @BeforeEach void setupUsers() {
            chief = saveUser(uniqueEmail("chief"), true, CHIEF_PWD);
            manager = saveUser(uniqueEmail("manager"), false, CHIEF_PWD);
            worker = saveUser(uniqueEmail("worker"), false, CHIEF_PWD);
            other = saveUser(uniqueEmail("other"), false, CHIEF_PWD);
            chiefToken = login(chief.getEmail(), CHIEF_PWD);
            managerToken = login(manager.getEmail(), CHIEF_PWD);
            workerToken = login(worker.getEmail(), CHIEF_PWD);
        }

        @SuppressWarnings("unchecked")
        private ResponseEntity<Map> assign(String token, int taskId, Integer workerId, String reason) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("workerUserId", workerId);
            body.put("reason", reason);
            HttpEntity<?> req = token == null ? new HttpEntity<>(body, json()) : new HttpEntity<>(body, authJson(token));
            return rest.exchange(url("/api/tasks/" + taskId + "/assignments"), HttpMethod.POST, req, Map.class);
        }
        @SuppressWarnings("unchecked")
        private ResponseEntity<List> list(String token, int taskId) {
            return rest.exchange(url("/api/tasks/" + taskId + "/assignments"), HttpMethod.GET, new HttpEntity<>(authJson(token)), List.class);
        }

        @Test @DisplayName("DT-ASG-R1 / CA-ASG-01,12: asignación válida, reason trim y lectura coherente")
        void assignHappyExplicitReason() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            ResponseEntity<Map> r = assign(chiefToken, 100, worker.getUserId(), "  Motivo válido  ");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().get("reason")).isEqualTo("Motivo válido");
            ResponseEntity<List> list = list(chiefToken, 100);
            assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(list.getBody()).hasSize(1);
            assertThat(((Map<?,?>)list.getBody().get(0)).get("reason")).isEqualTo("Motivo válido");
            assertThat(jdbc.queryForObject("SELECT reason FROM task_manager.task_assignment WHERE task_id=100 AND worker_user_id=?", String.class, worker.getUserId())).isEqualTo("Motivo válido");
        }

        @Test @DisplayName("CA-ASG-02: reason null/omitido -> default")
        void assignNullReasonDefaults() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            ResponseEntity<Map> r = assign(chiefToken, 100, worker.getUserId(), null);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().get("reason")).isEqualTo(TaskAssignment.DEFAULT_REASON);
            assertThat(jdbc.queryForObject("SELECT reason FROM task_manager.task_assignment WHERE task_id=100 AND worker_user_id=?", String.class, worker.getUserId())).isEqualTo(TaskAssignment.DEFAULT_REASON);
        }

        @Test @DisplayName("CA-ASG-03: reason vacío/espacios -> default")
        void assignBlankReasonDefaults() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            ResponseEntity<Map> r = assign(chiefToken, 100, worker.getUserId(), "   ");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().get("reason")).isEqualTo(TaskAssignment.DEFAULT_REASON);
        }

        @Test @DisplayName("CA-ASG-04: reason > 500 -> 400 sin persistencia")
        void assignReasonTooLong() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            String longReason = "x".repeat(501);
            ResponseEntity<Map> r = assign(chiefToken, 100, worker.getUserId(), longReason);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.task_assignment WHERE task_id=100", Integer.class)).isZero();
        }

        @Test @DisplayName("DT-ASG-R4 / CA-ASG-05: trabajador inactivo -> 400")
        void assignInactiveWorker() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            AppUserEntity inactive = saveUser(uniqueEmail("inactive"), false, false, CHIEF_PWD);
            ResponseEntity<Map> r = assign(chiefToken, 100, inactive.getUserId(), "x");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.task_assignment WHERE task_id=100 AND worker_user_id=?", Integer.class, inactive.getUserId())).isZero();
        }

        @Test @DisplayName("DT-ASG-R5 / CA-ASG-06: autoasignación -> 403")
        void selfAssignForbidden() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            ResponseEntity<Map> r = assign(managerToken, 100, manager.getUserId(), "x");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.task_assignment WHERE task_id=100", Integer.class)).isZero();
        }

        @Test @DisplayName("DT-ASG-R6 / CA-ASG-07: duplicada -> 400/409 sin nueva fila")
        void duplicateAssignmentRejected() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            assertThat(assign(chiefToken, 100, worker.getUserId(), "ok").getStatusCode()).isEqualTo(HttpStatus.OK);
            ResponseEntity<Map> r = assign(chiefToken, 100, worker.getUserId(), "otra");
            assertThat(r.getStatusCode().value()).isIn(400, 409);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.task_assignment WHERE task_id=100 AND worker_user_id=?", Integer.class, worker.getUserId())).isEqualTo(1);
        }

        @Test @DisplayName("DT-ASG-R3 / CA-ASG-08: proyecto FINALIZADO/CANCELADO -> 409")
        void assignClosedProject() {
            seedHierarchy(101, "FINALIZADO", chief.getUserId(), manager.getUserId(), 101);
            ResponseEntity<Map> r1 = assign(chiefToken, 101, worker.getUserId(), "x");
            assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            seedHierarchy(102, "CANCELADO", chief.getUserId(), manager.getUserId(), 102);
            ResponseEntity<Map> r2 = assign(chiefToken, 102, worker.getUserId(), "x");
            assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.task_assignment", Integer.class)).isZero();
        }

        @Test @DisplayName("DT-ASG-R2 / CA-ASG-09: actor no autorizado (worker sin gestión) -> 403")
        void assignUnauthorizedActor() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            // workerToken is a plain personal user, not manager/chief of project
            ResponseEntity<Map> r = assign(workerToken, 100, other.getUserId(), "x");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.task_assignment", Integer.class)).isZero();
        }

        @Test @DisplayName("DT-ASG-R10 / CA-ASG-10: no autenticado -> 401")
        void assignUnauthenticated() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            ResponseEntity<Map> r = assign(null, 100, worker.getUserId(), "x");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.task_assignment", Integer.class)).isZero();
        }

        @Test @DisplayName("DT-ASG-R7 / CA-ASG-11: tarea inexistente -> 404")
        void assignTaskNotFound() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            ResponseEntity<Map> r = assign(chiefToken, 999999, worker.getUserId(), "x");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test @DisplayName("DT-ASG-R8: trabajador inexistente -> 400/404")
        void assignWorkerNotFound() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            ResponseEntity<Map> r = assign(chiefToken, 100, 999999, "x");
            assertThat(r.getStatusCode().value()).isIn(400, 404);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.task_assignment", Integer.class)).isZero();
        }

        @Test @DisplayName("DT-ASG-R9: relación inconsistente/no visible -> 404/400/403 sin persistencia")
        void assignInconsistentRelation() {
            // manager B tries to act on task in project managed by manager A via chief? easier: other manager not in project
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            AppUserEntity otherMgr = saveUser(uniqueEmail("mgr2"), false, CHIEF_PWD);
            String otherMgrToken = login(otherMgr.getEmail(), CHIEF_PWD);
            // otherMgr cannot manage project 100 -> forbidden by policy (context not valid for actor)
            ResponseEntity<Map> r = assign(otherMgrToken, 100, worker.getUserId(), "x");
            assertThat(r.getStatusCode().value()).isIn(400, 403, 404);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.task_assignment", Integer.class)).isZero();
        }

        @Test @DisplayName("CA-ASG-12: lectura posterior coherente tras creaciones válidas")
        void readBackCoherent() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            assertThat(assign(chiefToken, 100, worker.getUserId(), "Motivo").getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(assign(chiefToken, 100, other.getUserId(), null).getStatusCode()).isEqualTo(HttpStatus.OK);
            ResponseEntity<List> list = list(chiefToken, 100);
            assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(list.getBody()).hasSize(2);
            @SuppressWarnings("unchecked") List<Map<String,Object>> body = list.getBody();
            assertThat(body).extracting(m -> (String)m.get("reason")).containsExactlyInAnyOrder("Motivo", TaskAssignment.DEFAULT_REASON);
        }

        @SuppressWarnings("unchecked")
        private ResponseEntity<Map> getAssignment(String token, int taskId, int workerId) {
            return rest.exchange(url("/api/tasks/" + taskId + "/assignments/" + workerId), HttpMethod.GET,
                    new HttpEntity<>(authJson(token)), Map.class);
        }

        @SuppressWarnings("unchecked")
        private ResponseEntity<Map> updateAssignmentReason(String token, int taskId, int workerId, String reason) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("reason", reason);
            HttpEntity<?> req = new HttpEntity<>(body, authJson(token));
            return rest.exchange(url("/api/tasks/" + taskId + "/assignments/" + workerId), HttpMethod.PUT, req, Map.class);
        }

        @Test @DisplayName("CA-ASG-13: GET individual asignación por clave compuesta (200)")
        void getIndividualAssignment() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            assertThat(assign(chiefToken, 100, worker.getUserId(), "Motivo de consulta").getStatusCode()).isEqualTo(HttpStatus.OK);
            ResponseEntity<Map> r = getAssignment(chiefToken, 100, worker.getUserId());
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().get("taskId")).isEqualTo(100);
            assertThat(r.getBody().get("workerUserId")).isEqualTo(worker.getUserId());
            assertThat(r.getBody().get("reason")).isEqualTo("Motivo de consulta");
        }

        @Test @DisplayName("CA-ASG-14: GET individual asignación inexistente (404)")
        void getIndividualAssignmentNotFound() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            ResponseEntity<Map> r = getAssignment(chiefToken, 100, 999999);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test @DisplayName("CA-ASG-15: PUT actualiza solo reason, conserva identidad y auditoría")
        void putUpdatesOnlyReason() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            assertThat(assign(chiefToken, 100, worker.getUserId(), "Motivo original").getStatusCode()).isEqualTo(HttpStatus.OK);

            // Capturar valores de auditoría antes del PUT
            Map<String, Object> before = (Map<String, Object>) jdbc.queryForObject(
                    "SELECT task_id, worker_user_id, assigned_at, assigned_by_user_id, reason FROM task_manager.task_assignment WHERE task_id = 100 AND worker_user_id = ?",
                    (rs, rowNum) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("taskId", rs.getInt("task_id"));
                        m.put("workerUserId", rs.getInt("worker_user_id"));
                        m.put("assignedAt", rs.getTimestamp("assigned_at").toInstant());
                        m.put("assignedByUserId", rs.getInt("assigned_by_user_id"));
                        m.put("reason", rs.getString("reason"));
                        return m;
                    },
                    worker.getUserId());

            ResponseEntity<Map> r = updateAssignmentReason(chiefToken, 100, worker.getUserId(), "Motivo actualizado");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().get("reason")).isEqualTo("Motivo actualizado");
            assertThat(r.getBody().get("taskId")).isEqualTo(100);
            assertThat(r.getBody().get("workerUserId")).isEqualTo(worker.getUserId());
            assertThat(r.getBody().get("assignedByUserId")).isEqualTo(before.get("assignedByUserId"));

            // Verificar que assigned_at no cambió
            Map<String, Object> after = (Map<String, Object>) jdbc.queryForObject(
                    "SELECT task_id, worker_user_id, assigned_at, assigned_by_user_id, reason FROM task_manager.task_assignment WHERE task_id = 100 AND worker_user_id = ?",
                    (rs, rowNum) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("taskId", rs.getInt("task_id"));
                        m.put("workerUserId", rs.getInt("worker_user_id"));
                        m.put("assignedAt", rs.getTimestamp("assigned_at").toInstant());
                        m.put("assignedByUserId", rs.getInt("assigned_by_user_id"));
                        m.put("reason", rs.getString("reason"));
                        return m;
                    },
                    worker.getUserId());
            assertThat(after.get("assignedAt")).isEqualTo(before.get("assignedAt"));
            assertThat(after.get("assignedByUserId")).isEqualTo(before.get("assignedByUserId"));
            assertThat(after.get("taskId")).isEqualTo(before.get("taskId"));
            assertThat(after.get("workerUserId")).isEqualTo(before.get("workerUserId"));
            assertThat(after.get("reason")).isEqualTo("Motivo actualizado");
        }

        @Test @DisplayName("CA-ASG-16: PUT reason vacío -> DEFAULT_REASON, campos de auditoría intactos")
        void putEmptyReasonDefaults() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            assertThat(assign(chiefToken, 100, worker.getUserId(), "Motivo previo").getStatusCode()).isEqualTo(HttpStatus.OK);
            Instant beforeAssignedAt = (Instant) jdbc.queryForObject(
                    "SELECT assigned_at FROM task_manager.task_assignment WHERE task_id = 100 AND worker_user_id = ?",
                    (rs, rowNum) -> rs.getTimestamp("assigned_at").toInstant(), worker.getUserId());

            ResponseEntity<Map> r = updateAssignmentReason(chiefToken, 100, worker.getUserId(), "");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().get("reason")).isEqualTo(TaskAssignment.DEFAULT_REASON);
            assertThat(jdbc.queryForObject("SELECT reason FROM task_manager.task_assignment WHERE task_id = 100 AND worker_user_id = ?",
                    String.class, worker.getUserId())).isEqualTo(TaskAssignment.DEFAULT_REASON);
            Instant afterAssignedAt = (Instant) jdbc.queryForObject(
                    "SELECT assigned_at FROM task_manager.task_assignment WHERE task_id = 100 AND worker_user_id = ?",
                    (rs, rowNum) -> rs.getTimestamp("assigned_at").toInstant(), worker.getUserId());
            assertThat(afterAssignedAt).isEqualTo(beforeAssignedAt);
        }

        @Test @DisplayName("CA-ASG-17: PUT reason > 500 -> 400, reason y auditoría intactos")
        void putReasonTooLong() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            assertThat(assign(chiefToken, 100, worker.getUserId(), "Motivo vigente").getStatusCode()).isEqualTo(HttpStatus.OK);
            String longReason = "x".repeat(501);
            Instant beforeAssignedAt = (Instant) jdbc.queryForObject(
                    "SELECT assigned_at FROM task_manager.task_assignment WHERE task_id = 100 AND worker_user_id = ?",
                    (rs, rowNum) -> rs.getTimestamp("assigned_at").toInstant(), worker.getUserId());

            ResponseEntity<Map> r = updateAssignmentReason(chiefToken, 100, worker.getUserId(), longReason);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(jdbc.queryForObject("SELECT reason FROM task_manager.task_assignment WHERE task_id = 100 AND worker_user_id = ?",
                    String.class, worker.getUserId())).isEqualTo("Motivo vigente");
            Instant afterAssignedAt = (Instant) jdbc.queryForObject(
                    "SELECT assigned_at FROM task_manager.task_assignment WHERE task_id = 100 AND worker_user_id = ?",
                    (rs, rowNum) -> rs.getTimestamp("assigned_at").toInstant(), worker.getUserId());
            assertThat(afterAssignedAt).isEqualTo(beforeAssignedAt);
        }

        @Test @DisplayName("CA-ASG-18: PUT reason exactamente 500 caracteres -> 200")
        void putReasonExactly500() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            assertThat(assign(chiefToken, 100, worker.getUserId(), "Motivo original").getStatusCode()).isEqualTo(HttpStatus.OK);
            String reason500 = "a".repeat(500);
            ResponseEntity<Map> r = updateAssignmentReason(chiefToken, 100, worker.getUserId(), reason500);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().get("reason")).isEqualTo(reason500);
        }

        @Test @DisplayName("CA-ASG-19: PUT individual asignación inexistente (404)")
        void putIndividualAssignmentNotFound() {
            seedHierarchy(100, "PLANIFICADO", chief.getUserId(), manager.getUserId(), 100);
            ResponseEntity<Map> r = updateAssignmentReason(chiefToken, 100, 999999, "Nuevo motivo");
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested class UserApi {
        private final String CHIEF_PWD = "ChiefPass123!";
        private AppUserEntity chief;
        private String chiefToken;

        @BeforeEach void setupChief() {
            chief = saveUser(uniqueEmail("chiefu"), true, CHIEF_PWD);
            chiefToken = login(chief.getEmail(), CHIEF_PWD);
        }

        @SuppressWarnings("unchecked")
        private ResponseEntity<Map> createUser(String token, Map<String, Object> body) {
            HttpEntity<?> req = token == null ? new HttpEntity<>(body, json()) : new HttpEntity<>(body, authJson(token));
            return rest.exchange(url("/api/users"), HttpMethod.POST, req, Map.class);
        }

        @SuppressWarnings("unchecked")
        private ResponseEntity<List> listUsers(String token) {
            return rest.exchange(url("/api/users"), HttpMethod.GET, new HttpEntity<>(authJson(token)), List.class);
        }

        @SuppressWarnings("unchecked")
        private ResponseEntity<Map> getUser(String token, Integer userId) {
            return rest.exchange(url("/api/users/" + userId), HttpMethod.GET, new HttpEntity<>(authJson(token)), Map.class);
        }

        @SuppressWarnings("unchecked")
        private ResponseEntity<Map> updateUser(String token, Integer userId, Map<String, Object> body) {
            HttpEntity<?> req = new HttpEntity<>(body, authJson(token));
            return rest.exchange(url("/api/users/" + userId), HttpMethod.PUT, req, Map.class);
        }

        private ResponseEntity<Void> deactivateUser(String token, Integer userId) {
            return rest.exchange(url("/api/users/" + userId), HttpMethod.DELETE, new HttpEntity<>(authJson(token)), Void.class);
        }

        private Map<String, Object> userBody(String email, String givenNames, String familyNames, String password) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("email", email);
            body.put("givenNames", givenNames);
            body.put("familyNames", familyNames);
            body.put("password", password);
            return body;
        }

        @Test @DisplayName("CA-USR-01,02,10: creación válida, normalización, hash BCrypt, login, sin secretos")
        void createValidNormalizedSecure() {
            String base = "nuevo-" + UUID.randomUUID() + "@GESTOR.LOCAL";
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("email", base);
            req.put("givenNames", "  Ana  ");
            req.put("familyNames", "  Perez  ");
            req.put("password", "UserPass123!");
            ResponseEntity<Map> resp = createUser(chiefToken, req);
            assertThat(resp.getStatusCode()).as("body=%s", resp.getBody()).isEqualTo(HttpStatus.CREATED);
            assertThat(resp.getBody()).doesNotContainKeys("password", "passwordHash", "password_hash", "token");
            String normalized = base.toLowerCase();
            assertThat(resp.getBody().get("email")).isEqualTo(normalized);
            assertThat(resp.getBody().get("chief")).isEqualTo(false);
            assertThat(resp.getBody().get("active")).isEqualTo(true);
            String hash = jdbc.queryForObject("SELECT password_hash FROM task_manager.app_user WHERE email=?", String.class, normalized);
            assertThat(hash).isNotEqualTo("UserPass123!");
            assertThat(passwordEncoder.matches("UserPass123!", hash)).isTrue();
            ResponseEntity<Map> loginNew = rest.exchange(url("/api/auth/login"), HttpMethod.POST, new HttpEntity<>(Map.of("email", normalized, "password", "UserPass123!"), json()), Map.class);
            assertThat(loginNew.getStatusCode()).isEqualTo(HttpStatus.OK);
            @SuppressWarnings("unchecked") Map<String,Object> user = (Map<String,Object>) loginNew.getBody().get("user");
            assertThat(user.get("chief")).isEqualTo(false);
            assertThat(user.get("active")).isEqualTo(true);
        }

        @Test @DisplayName("CA-USR-03: duplicado email -> 400/409 sin nuevo usuario ni fuga")
        void duplicateEmailRejected() {
            String target = uniqueEmail("dup");
            Map<String, Object> req = Map.of("email", target, "givenNames", "A", "familyNames", "B", "password", "UserPass123!");
            assertThat(createUser(chiefToken, req).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            long before = jdbc.queryForObject("SELECT count(*) FROM task_manager.app_user WHERE email=?", Long.class, target);
            ResponseEntity<Map> dup = createUser(chiefToken, Map.of("email", target.toUpperCase(), "givenNames", "A", "familyNames", "B", "password", "UserPass123!"));
            assertThat(dup.getStatusCode().value()).isIn(400, 409);
            long after = jdbc.queryForObject("SELECT count(*) FROM task_manager.app_user WHERE email=?", Long.class, target);
            assertThat(after).isEqualTo(before);
            assertThat(String.valueOf(dup.getBody())).doesNotContain("UserPass123!").doesNotContain("password_hash").doesNotContain("$2");
        }

        @Test @DisplayName("CA-USR-04: nombres/apellidos inválidos o límites -> 400")
        void invalidNames() {
            String bad101 = "x".repeat(101);
            assertThat(createUser(chiefToken, Map.of("email", uniqueEmail("n1"), "givenNames", " ", "familyNames", "B", "password", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(createUser(chiefToken, Map.of("email", uniqueEmail("n2"), "givenNames", "A", "familyNames", " ", "password", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(createUser(chiefToken, Map.of("email", uniqueEmail("n3"), "givenNames", bad101, "familyNames", "B", "password", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(createUser(chiefToken, Map.of("email", uniqueEmail("n4"), "givenNames", "A", "familyNames", bad101, "password", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test @DisplayName("CA-USR-05/BVA3-USR-PWD: fronteras 7/8/72/73-bytes-ascii y 201 (73 bytes -> 400)")
        void passwordBoundaries() {
            assertThat(createUser(chiefToken, Map.of("email", uniqueEmail("p7"), "givenNames", "A", "familyNames", "B", "password", "1234567")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(createUser(chiefToken, Map.of("email", uniqueEmail("p201"), "givenNames", "A", "familyNames", "B", "password", "x".repeat(201))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(createUser(chiefToken, Map.of("email", uniqueEmail("p8"), "givenNames", "A", "familyNames", "B", "password", "12345678")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            // BCrypt solo admite 72 bytes: 72 aceptado; 73 se rechaza con 400 antes de hashear (DIV-E4-01 corregido)
            assertThat(createUser(chiefToken, Map.of("email", uniqueEmail("p72"), "givenNames", "A", "familyNames", "B", "password", "x".repeat(72))).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            ResponseEntity<java.util.Map> overBcrypt = createUser(chiefToken, Map.of("email", uniqueEmail("p73"), "givenNames", "A", "familyNames", "B", "password", "x".repeat(73)));
            assertThat(overBcrypt.getStatusCode()).as("73 ASCII chars exceed BCrypt 72-byte limit").isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test @DisplayName("CA-USR-06: email inválido o >254 -> 400")
        void invalidOrLongEmail() {
            assertThat(createUser(chiefToken, Map.of("email", "invalido", "givenNames", "A", "familyNames", "B", "password", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            String longLocal = "a".repeat(243) + "@gestor.local"; // >254
            assertThat(createUser(chiefToken, Map.of("email", longLocal, "givenNames", "A", "familyNames", "B", "password", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test @DisplayName("CA-USR-07: no Jefe (manager/personal) -> 403")
        void nonChiefForbidden() {
            AppUserEntity mgr = saveUser(uniqueEmail("mgr"), false, CHIEF_PWD);
            String mgrToken = login(mgr.getEmail(), CHIEF_PWD);
            ResponseEntity<Map> r = createUser(mgrToken, Map.of("email", uniqueEmail("x"), "givenNames", "A", "familyNames", "B", "password", "UserPass123!"));
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test @DisplayName("CA-USR-08: no autenticado -> 401")
        void unauthenticated() {
            ResponseEntity<Map> r = createUser(null, Map.of("email", uniqueEmail("y"), "givenNames", "A", "familyNames", "B", "password", "UserPass123!"));
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test @DisplayName("CA-USR-09: tampering privilegios ignorado, persiste no-jefe/activo")
        void privilegeTamperingIgnored() {
            String email = uniqueEmail("tamper");
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("email", email);
            req.put("givenNames", "Ana");
            req.put("familyNames", "Perez");
            req.put("password", "UserPass123!");
            req.put("isChief", true);
            req.put("chief", true);
            req.put("isActive", false);
            req.put("active", false);
            ResponseEntity<Map> resp = createUser(chiefToken, req);
            assertThat(resp.getStatusCode().value()).isIn(201, 400); // 400 if bean rejects unknown? unlikely; record ignores extras
            if (resp.getStatusCode().is2xxSuccessful()) {
                assertThat(resp.getBody().get("chief")).isEqualTo(false);
                assertThat(resp.getBody().get("active")).isEqualTo(true);
                Boolean chiefDb = jdbc.queryForObject("SELECT is_chief FROM task_manager.app_user WHERE email=?", Boolean.class, email.toLowerCase());
                Boolean activeDb = jdbc.queryForObject("SELECT is_active FROM task_manager.app_user WHERE email=?", Boolean.class, email.toLowerCase());
                assertThat(chiefDb).isFalse();
                assertThat(activeDb).isTrue();
            }
        }

        @Test @DisplayName("CA-USR-11: listado devuelve usuarios creados sin secretos")
        void listUsersReturnsCreatedUsersWithoutSecrets() {
            String e1 = uniqueEmail("u1");
            String e2 = uniqueEmail("u2");
            assertThat(createUser(chiefToken, userBody(e1, "Ana", "Pérez", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(createUser(chiefToken, userBody(e2, "Luis", "Gómez", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            ResponseEntity<List> list = listUsers(chiefToken);
            assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
            @SuppressWarnings("unchecked") List<Map<String, Object>> body = list.getBody();
            // El jefe del escenario más los dos usuarios creados
            assertThat(body).hasSize(3);
            assertThat(body).extracting(m -> m.get("email")).containsExactlyInAnyOrder(e1, e2, chief.getEmail());
            for (Map<String, Object> row : body) {
                assertThat(row).doesNotContainKeys("password", "passwordHash", "password_hash", "token");
            }
            // Persistencia real en BD
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.app_user WHERE email=?", Integer.class, e1)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.app_user WHERE email=?", Integer.class, e2)).isEqualTo(1);
        }

        @Test @DisplayName("CA-USR-12: consulta individual devuelve datos sin secretos")
        void getUserReturnsSafeDetails() {
            String email = uniqueEmail("uget");
            assertThat(createUser(chiefToken, userBody(email, "Ana", "Pérez", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Integer id = jdbc.queryForObject("SELECT user_id FROM task_manager.app_user WHERE email=?", Integer.class, email);
            ResponseEntity<Map> r = getUser(chiefToken, id);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().get("userId")).isEqualTo(id);
            assertThat(r.getBody().get("email")).isEqualTo(email);
            assertThat(r.getBody().get("givenNames")).isEqualTo("Ana");
            assertThat(r.getBody().get("familyNames")).isEqualTo("Pérez");
            assertThat(r.getBody().get("chief")).isEqualTo(false);
            assertThat(r.getBody().get("active")).isEqualTo(true);
            assertThat(r.getBody()).doesNotContainKeys("password", "passwordHash", "password_hash", "token");
        }

        @Test @DisplayName("CA-USR-13: usuario inexistente -> 404")
        void getUserNotFound() {
            assertThat(getUser(chiefToken, 999999).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test @DisplayName("CA-USR-14: actualizar nombres y apellidos persiste")
        void updateNamesHappy() {
            String email = uniqueEmail("uupd");
            assertThat(createUser(chiefToken, userBody(email, "Ana", "Pérez", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Integer id = jdbc.queryForObject("SELECT user_id FROM task_manager.app_user WHERE email=?", Integer.class, email);
            ResponseEntity<Map> r = updateUser(chiefToken, id, Map.of("givenNames", "María", "familyNames", "López"));
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().get("givenNames")).isEqualTo("María");
            assertThat(r.getBody().get("familyNames")).isEqualTo("López");
            assertThat(jdbc.queryForObject("SELECT given_names FROM task_manager.app_user WHERE user_id=?", String.class, id)).isEqualTo("María");
            assertThat(jdbc.queryForObject("SELECT family_names FROM task_manager.app_user WHERE user_id=?", String.class, id)).isEqualTo("López");
            // email intacto
            assertThat(jdbc.queryForObject("SELECT email FROM task_manager.app_user WHERE user_id=?", String.class, id)).isEqualTo(email);
        }

        @Test @DisplayName("CA-USR-15: actualizar email válido único persiste normalizado")
        void updateEmailHappy() {
            String email = uniqueEmail("uem");
            String nuevo = "NUEVO.MAIL@GESTOR.LOCAL";
            assertThat(createUser(chiefToken, userBody(email, "Ana", "Pérez", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Integer id = jdbc.queryForObject("SELECT user_id FROM task_manager.app_user WHERE email=?", Integer.class, email);
            ResponseEntity<Map> r = updateUser(chiefToken, id, Map.of("email", nuevo));
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            String normalizado = nuevo.toLowerCase(Locale.ROOT);
            assertThat(r.getBody().get("email")).isEqualTo(normalizado);
            assertThat(jdbc.queryForObject("SELECT email FROM task_manager.app_user WHERE user_id=?", String.class, id)).isEqualTo(normalizado);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.app_user WHERE email=?", Integer.class, normalizado)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.app_user WHERE email=?", Integer.class, email)).isZero();
        }

        @Test @DisplayName("CA-USR-16: email inválido en actualización -> 400 y datos previos conservados")
        void updateInvalidEmailNoChange() {
            String email = uniqueEmail("uinv");
            assertThat(createUser(chiefToken, userBody(email, "Ana", "Pérez", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Integer id = jdbc.queryForObject("SELECT user_id FROM task_manager.app_user WHERE email=?", Integer.class, email);
            ResponseEntity<Map> r = updateUser(chiefToken, id, Map.of("email", "invalido"));
            assertThat(r.getStatusCode().value()).isIn(400);
            // Datos previos conservados (sin cambio parcial)
            assertThat(jdbc.queryForObject("SELECT email FROM task_manager.app_user WHERE user_id=?", String.class, id)).isEqualTo(email);
            assertThat(jdbc.queryForObject("SELECT given_names FROM task_manager.app_user WHERE user_id=?", String.class, id)).isEqualTo("Ana");
        }

        @Test @DisplayName("CA-USR-17: mass assignment de is_chief/is_active/password_hash ignorado en PUT")
        void updateMassAssignmentIgnored() {
            String email = uniqueEmail("umass");
            assertThat(createUser(chiefToken, userBody(email, "Ana", "Pérez", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Integer id = jdbc.queryForObject("SELECT user_id FROM task_manager.app_user WHERE email=?", Integer.class, email);
            ResponseEntity<Map> r = updateUser(chiefToken, id, Map.of(
                    "givenNames", "María",
                    "isChief", true,
                    "chief", true,
                    "isActive", false,
                    "active", false,
                    "password_hash", "$2a$10$injected",
                    "password", "injected"
            ));
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody().get("chief")).isEqualTo(false);
            assertThat(r.getBody().get("active")).isEqualTo(true);
            assertThat(jdbc.queryForObject("SELECT is_chief FROM task_manager.app_user WHERE user_id=?", Boolean.class, id)).isFalse();
            assertThat(jdbc.queryForObject("SELECT is_active FROM task_manager.app_user WHERE user_id=?", Boolean.class, id)).isTrue();
            assertThat(jdbc.queryForObject("SELECT password_hash FROM task_manager.app_user WHERE user_id=?", String.class, id)).isNotEqualTo("$2a$10$injected");
        }

        @Test @DisplayName("CA-USR-18: actualizar usuario inexistente -> 404")
        void updateUserNotFound() {
            assertThat(updateUser(chiefToken, 999999, Map.of("givenNames", "X")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test @DisplayName("CA-USR-19: desactivación lógica preserva identidad, FK e historial")
        void deactivateHappy() {
            String email = uniqueEmail("udel");
            assertThat(createUser(chiefToken, userBody(email, "Ana", "Pérez", "UserPass123!")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            Integer id = jdbc.queryForObject("SELECT user_id FROM task_manager.app_user WHERE email=?", Integer.class, email);
            ResponseEntity<Void> r = deactivateUser(chiefToken, id);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(jdbc.queryForObject("SELECT is_active FROM task_manager.app_user WHERE user_id=?", Boolean.class, id)).isFalse();
            // Identidad y fila conservadas (no borrado físico)
            assertThat(jdbc.queryForObject("SELECT count(*) FROM task_manager.app_user WHERE user_id=?", Integer.class, id)).isEqualTo(1);
            // Respuesta sin secretos
            ResponseEntity<Map> after = getUser(chiefToken, id);
            assertThat(after.getBody().get("active")).isEqualTo(false);
            assertThat(after.getBody()).doesNotContainKeys("password", "passwordHash", "password_hash", "token");
        }

        @Test @DisplayName("CA-USR-20: desactivar usuario inexistente -> 404")
        void deactivateUserNotFound() {
            assertThat(deactivateUser(chiefToken, 999999).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test @DisplayName("CA-USR-21: usuario inactivo no puede iniciar sesión")
        void inactiveUserCannotLogin() {
            String email = uniqueEmail("inact");
            saveUser(email, false, false, "InactPass123!");
            ResponseEntity<Map> r = rest.exchange(url("/api/auth/login"), HttpMethod.POST,
                    new HttpEntity<>(Map.of("email", email, "password", "InactPass123!"), json()), Map.class);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test @DisplayName("CA-USR-22: token existente de usuario desactivado es rechazado en operaciones protegidas")
        void deactivatedUserTokenRejected() {
            String email = uniqueEmail("tok");
            saveUser(email, true, true, "TokPass123!");
            String token = login(email, "TokPass123!");
            // Desactivar directamente en BD (simula baja por otro canal)
            jdbc.update("UPDATE task_manager.app_user SET is_active = false WHERE email=?", email);
            // El token ya no autoriza operaciones protegidas (el actor se resuelve desde BD en cada petición)
            ResponseEntity<Map> r = getUser(token, 1);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}