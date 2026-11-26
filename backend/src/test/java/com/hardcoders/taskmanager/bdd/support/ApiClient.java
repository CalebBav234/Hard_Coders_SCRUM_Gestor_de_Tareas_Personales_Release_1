package com.hardcoders.taskmanager.bdd.support;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para llamadas REST reales contra la API del backend en ejecución.
 * No es un bean de Spring: se instancia manualmente pasando el puerto del servidor.
 * Utiliza RestTemplate y gestiona el token JWT de autenticación obtenido tras el login.
 */
public class ApiClient {

    private static final String PATH_LOGIN = "/api/auth/login";
    private static final String PATH_PROJECTS = "/api/projects";
    private static final String PATH_STAGES = "/api/projects/%d/stages";
    private static final String PATH_ACTIVITIES = "/api/stages/%d/activities";
    private static final String PATH_TASKS = "/api/tasks";
    private static final String PATH_SUBTASKS = "/api/tasks/%d/subtasks";
    private static final String PATH_USERS = "/api/users";
    private static final String PATH_USER = "/api/users/%d";
    private static final String PATH_ASSIGNMENTS = "/api/tasks/%d/assignments";
    private static final String PATH_ASSIGNMENT = "/api/tasks/%d/assignments/%d";

    private final RestTemplate restTemplate = new RestTemplate();
    private final int port;
    private final String baseUrl;

    public ApiClient(int port) {
        this.port = port;
        this.baseUrl = "http://localhost:" + port;
    }

    /**
     * Autentica un usuario y devuelve el token JWT.
     */
    public String login(String email, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + PATH_LOGIN, entity, Map.class);
        Map<String, Object> bodyMap = response.getBody();
        if (bodyMap == null || !bodyMap.containsKey("token")) {
            throw new IllegalStateException("Login fallido: no se obtuvo token. Estado: " + response.getStatusCode());
        }
        return (String) bodyMap.get("token");
    }

    /**
     * Crea un proyecto y devuelve el ID asignado.
     */
    public Integer createProject(String token, String name, String description,
                                 Integer managerUserId, String budgetBob, String estimatedDurationMinutes,
                                 String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("managerUserId", managerUserId);
        body.put("budgetBob", new java.math.BigDecimal(budgetBob));
        body.put("estimatedDurationMinutes", Integer.parseInt(estimatedDurationMinutes));
        body.put("reason", reason);

        HttpEntity<Map<String, Object>> entity = authEntity(token, body);
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + PATH_PROJECTS, entity, Map.class);
        Map<String, Object> bodyMap = response.getBody();
        if (bodyMap == null || !bodyMap.containsKey("projectId")) {
            throw new IllegalStateException("Creación de proyecto fallida. Estado: " + response.getStatusCode());
        }
        return ((Number) bodyMap.get("projectId")).intValue();
    }

    /**
     * Crea una etapa dentro de un proyecto y devuelve el ID asignado.
     */
    public Integer createStage(String token, Integer projectId, String name, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", description);

        HttpEntity<Map<String, Object>> entity = authEntity(token, body);
        String url = baseUrl + String.format(PATH_STAGES, projectId);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        Map<String, Object> bodyMap = response.getBody();
        if (bodyMap == null || !bodyMap.containsKey("stageId")) {
            throw new IllegalStateException("Creación de etapa fallida. Estado: " + response.getStatusCode());
        }
        return ((Number) bodyMap.get("stageId")).intValue();
    }

    /**
     * Crea una actividad dentro de una etapa y devuelve el ID asignado.
     */
    public Integer createActivity(String token, Integer stageId, String name, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", description);

        HttpEntity<Map<String, Object>> entity = authEntity(token, body);
        String url = baseUrl + String.format(PATH_ACTIVITIES, stageId);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        Map<String, Object> bodyMap = response.getBody();
        if (bodyMap == null || !bodyMap.containsKey("activityId")) {
            throw new IllegalStateException("Creación de actividad fallida. Estado: " + response.getStatusCode());
        }
        return ((Number) bodyMap.get("activityId")).intValue();
    }

    /**
     * Crea una tarea dentro de una actividad y devuelve el ID asignado.
     */
    public Integer createTask(String token, Integer activityId, String title, String priority,
                              String allocatedBudgetBob, String estimatedDurationMinutes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("activityId", activityId);
        body.put("title", title);
        body.put("priority", priority);
        body.put("allocatedBudgetBob", new java.math.BigDecimal(allocatedBudgetBob));
        body.put("estimatedDurationMinutes", Integer.parseInt(estimatedDurationMinutes));

        HttpEntity<Map<String, Object>> entity = authEntity(token, body);
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + PATH_TASKS, entity, Map.class);
        Map<String, Object> bodyMap = response.getBody();
        if (bodyMap == null || !bodyMap.containsKey("taskId")) {
            throw new IllegalStateException("Creación de tarea fallida. Estado: " + response.getStatusCode());
        }
        return ((Number) bodyMap.get("taskId")).intValue();
    }

    /**
     * Crea una subtarea dentro de una tarea y devuelve el ID asignado.
     */
    public Integer createSubtask(String token, Integer taskId, String title, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("description", description);

        HttpEntity<Map<String, Object>> entity = authEntity(token, body);
        String url = baseUrl + String.format(PATH_SUBTASKS, taskId);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        Map<String, Object> bodyMap = response.getBody();
        if (bodyMap == null || !bodyMap.containsKey("subtaskId")) {
            throw new IllegalStateException("Creación de subtarea fallida. Estado: " + response.getStatusCode());
        }
        return ((Number) bodyMap.get("subtaskId")).intValue();
    }

    /* ==================== Usuarios (CRUD app_user) ==================== */

    public ResponseEntity<Map> postUser(String token, Map<String, Object> body) {
        HttpEntity<Map<String, Object>> entity = authEntity(token, body);
        try {
            return restTemplate.exchange(baseUrl + PATH_USERS, HttpMethod.POST, entity, Map.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getStatusText()));
        }
    }

    public ResponseEntity<Map> getUser(String token, Integer userId) {
        HttpEntity<Map<String, Object>> entity = authEntity(token, null);
        try {
            return restTemplate.exchange(baseUrl + String.format(PATH_USER, userId), HttpMethod.GET, entity, Map.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getStatusText()));
        }
    }

    public ResponseEntity<List> listUsers(String token) {
        HttpEntity<Map<String, Object>> entity = authEntity(token, null);
        try {
            return restTemplate.exchange(baseUrl + PATH_USERS, HttpMethod.GET, entity, List.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(List.of());
        }
    }

    public ResponseEntity<Map> putUser(String token, Integer userId, Map<String, Object> body) {
        HttpEntity<Map<String, Object>> entity = authEntity(token, body);
        try {
            return restTemplate.exchange(baseUrl + String.format(PATH_USER, userId), HttpMethod.PUT, entity, Map.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getStatusText()));
        }
    }

    public ResponseEntity<Void> deleteUser(String token, Integer userId) {
        HttpEntity<Map<String, Object>> entity = authEntity(token, null);
        try {
            return restTemplate.exchange(baseUrl + String.format(PATH_USER, userId), HttpMethod.DELETE, entity, Void.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }

    /* ==================== Asignaciones (CRUD task_assignment) ==================== */

    public ResponseEntity<Map> postAssignment(String token, Integer taskId, Map<String, Object> body) {
        HttpEntity<Map<String, Object>> entity = authEntity(token, body);
        try {
            return restTemplate.exchange(baseUrl + String.format(PATH_ASSIGNMENTS, taskId), HttpMethod.POST, entity, Map.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getStatusText()));
        }
    }

    public ResponseEntity<List> listAssignments(String token, Integer taskId) {
        HttpEntity<Map<String, Object>> entity = authEntity(token, null);
        try {
            return restTemplate.exchange(baseUrl + String.format(PATH_ASSIGNMENTS, taskId), HttpMethod.GET, entity, List.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(List.of());
        }
    }

    public ResponseEntity<Map> getAssignment(String token, Integer taskId, Integer workerUserId) {
        HttpEntity<Map<String, Object>> entity = authEntity(token, null);
        try {
            return restTemplate.exchange(baseUrl + String.format(PATH_ASSIGNMENT, taskId, workerUserId), HttpMethod.GET, entity, Map.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getStatusText()));
        }
    }

    public ResponseEntity<Map> putAssignmentReason(String token, Integer taskId, Integer workerUserId, Map<String, Object> body) {
        HttpEntity<Map<String, Object>> entity = authEntity(token, body);
        try {
            return restTemplate.exchange(baseUrl + String.format(PATH_ASSIGNMENT, taskId, workerUserId), HttpMethod.PUT, entity, Map.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getStatusText()));
        }
    }

    public ResponseEntity<Void> deleteAssignment(String token, Integer taskId, Integer workerUserId) {
        HttpEntity<Map<String, Object>> entity = authEntity(token, null);
        try {
            return restTemplate.exchange(baseUrl + String.format(PATH_ASSIGNMENT, taskId, workerUserId), HttpMethod.DELETE, entity, Void.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }

    private HttpEntity<Map<String, Object>> authEntity(String token, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
