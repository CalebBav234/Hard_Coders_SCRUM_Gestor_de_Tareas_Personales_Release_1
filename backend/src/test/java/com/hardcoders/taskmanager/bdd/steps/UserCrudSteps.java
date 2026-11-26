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
 * Step Definitions del CRUD de {@code app_user} (USR-INS-01..03, USR-UPD-01..03, USR-DEL-01,
 * USR-SEL-01, USR-SEL-02). Todas las llamadas son HTTP reales contra la API del backend y la
 * verificacion de persistencia se hace contra PostgreSQL de integracion (gestor_tareas_test).
 */
public class UserCrudSteps {

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

    @Given("an authenticated chief user")
    public void an_authenticated_chief_user() {
        String token = context.getAuthToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No hay token de autenticacion. El usuario jefe no se autentico.");
        }
    }

    @When("I create a valid user with given names {string} and family names {string}")
    public void i_create_a_valid_user_with_given_names_and_family_names(String givenNames, String familyNames) {
        String email = "usr." + UUID.randomUUID() + "@gestor.local";
        ResponseEntity r = apiClient.postUser(context.getAuthToken(),
                Map.of("email", email, "givenNames", givenNames, "familyNames", familyNames, "password", "UserPass123!"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map body = (Map) r.getBody();
        assertNoSecrets(body);
        assertThat(body.get("email")).isEqualTo(email.toLowerCase());
        assertThat(body.get("chief")).isEqualTo(false);
        assertThat(body.get("active")).isEqualTo(true);
        context.setCreatedUserId(((Number) body.get("userId")).intValue());
        context.setLastPutResponse(r);
    }

    @When("I create a user with email {string} and valid names and password")
    public void i_create_a_user_with_email_and_valid_names_and_password(String email) {
        ResponseEntity r = apiClient.postUser(context.getAuthToken(),
                Map.of("email", email, "givenNames", "Ana", "familyNames", "Perez", "password", "UserPass123!"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        context.setLastPutResponse(r);
    }

    @And("a user exists with given names {string} and family names {string}")
    public void a_user_exists_with_given_names_and_family_names(String givenNames, String familyNames) {
        String email = "usr." + UUID.randomUUID() + "@gestor.local";
        ResponseEntity r = apiClient.postUser(context.getAuthToken(),
                Map.of("email", email, "givenNames", givenNames, "familyNames", familyNames, "password", "UserPass123!"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        context.setCreatedUserId(((Number) ((Map) r.getBody()).get("userId")).intValue());
    }

    @And("a second user exists with given names {string} and family names {string}")
    public void a_second_user_exists_with_given_names_and_family_names(String givenNames, String familyNames) {
        String email = "usr." + UUID.randomUUID() + "@gestor.local";
        ResponseEntity r = apiClient.postUser(context.getAuthToken(),
                Map.of("email", email, "givenNames", givenNames, "familyNames", familyNames, "password", "UserPass123!"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @When("I update the user's names to {string} and {string}")
    public void i_update_the_user_s_names_to_and(String givenNames, String familyNames) {
        Integer userId = requireUserId();
        ResponseEntity r = apiClient.putUser(context.getAuthToken(), userId,
                Map.of("givenNames", givenNames, "familyNames", familyNames));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        context.setLastPutResponse(r);
        Map body = (Map) r.getBody();
        assertThat(body.get("givenNames")).isEqualTo(givenNames);
        assertThat(body.get("familyNames")).isEqualTo(familyNames);
    }

    @When("I update the user's email to {string}")
    public void i_update_the_user_s_email_to(String email) {
        Integer userId = requireUserId();
        ResponseEntity r = apiClient.putUser(context.getAuthToken(), userId,
                Map.of("email", email));
        context.setLastPutResponse(r);
    }

    @When("I deactivate the user")
    public void i_deactivate_the_user() {
        Integer userId = requireUserId();
        ResponseEntity r = apiClient.deleteUser(context.getAuthToken(), userId);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        context.setLastPutResponse(r);
    }

    @When("I consult the user by id")
    public void i_consult_the_user_by_id() {
        Integer userId = requireUserId();
        ResponseEntity r = apiClient.getUser(context.getAuthToken(), userId);
        context.setLastGetResponse(r);
    }

    @When("I list users")
    public void i_list_users() {
        ResponseEntity r = apiClient.listUsers(context.getAuthToken());
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        context.setLastListResponse(r);
    }

    @Then("the user is created with status 201")
    public void the_user_is_created_with_status_201() {
        // Afirmacion explicita del comportamiento ya verificado en el paso When.
    }

    @Then("the response has no password_hash, password or token")
    public void the_response_has_no_password_hash_password_or_token() {
        Map body = lastBody();
        assertThat(body.keySet()).doesNotContain("password", "passwordHash", "password_hash", "token");
    }

    @Then("the user is stored in PostgreSQL as active and non-chief")
    public void the_user_is_stored_in_postgresql_as_active_and_non_chief() {
        Integer userId = requireUserId();
        Boolean active = jdbc.queryForObject("SELECT is_active FROM task_manager.app_user WHERE user_id = ?",
                Boolean.class, userId);
        Boolean chief = jdbc.queryForObject("SELECT is_chief FROM task_manager.app_user WHERE user_id = ?",
                Boolean.class, userId);
        assertThat(active).isTrue();
        assertThat(chief).isFalse();
    }

    @Then("the response is 400")
    public void the_response_is_400() {
        assertThat(context.getLastPutResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Then("no user is stored in PostgreSQL")
    public void no_user_is_stored_in_postgresql() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM task_manager.app_user WHERE email = 'invalido'",
                Integer.class);
        assertThat(count).isZero();
    }

    @Then("the response is 200")
    public void the_response_is_200() {
        ResponseEntity r = context.getLastPutResponse();
        if (r == null) {
            r = context.getLastGetResponse();
        }
        if (r == null) {
            r = context.getLastListResponse();
        }
        assertThat(r).isNotNull();
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Then("the user's names are {string} and {string} in PostgreSQL")
    public void the_user_s_names_are_and_in_postgresql(String givenNames, String familyNames) {
        Integer userId = requireUserId();
        String g = jdbc.queryForObject("SELECT given_names FROM task_manager.app_user WHERE user_id = ?",
                String.class, userId);
        String f = jdbc.queryForObject("SELECT family_names FROM task_manager.app_user WHERE user_id = ?",
                String.class, userId);
        assertThat(g).isEqualTo(givenNames);
        assertThat(f).isEqualTo(familyNames);
    }

    @Then("the user's email is stored normalized in PostgreSQL")
    public void the_user_s_email_is_stored_normalized_in_postgresql() {
        Integer userId = requireUserId();
        String email = jdbc.queryForObject("SELECT email FROM task_manager.app_user WHERE user_id = ?",
                String.class, userId);
        assertThat(email).isEqualTo("maria.nueva@gestor.local");
    }

    @Then("the user's previous names are preserved in PostgreSQL")
    public void the_user_s_previous_names_are_preserved_in_postgresql() {
        Integer userId = requireUserId();
        String g = jdbc.queryForObject("SELECT given_names FROM task_manager.app_user WHERE user_id = ?",
                String.class, userId);
        String f = jdbc.queryForObject("SELECT family_names FROM task_manager.app_user WHERE user_id = ?",
                String.class, userId);
        assertThat(g).isEqualTo("Ana");
        assertThat(f).isEqualTo("P\u00E9rez");
    }

    @Then("the user is inactive in PostgreSQL")
    public void the_user_is_inactive_in_postgresql() {
        Integer userId = requireUserId();
        Boolean active = jdbc.queryForObject("SELECT is_active FROM task_manager.app_user WHERE user_id = ?",
                Boolean.class, userId);
        assertThat(active).isFalse();
    }

    @Then("the user row is preserved and not physically deleted")
    public void the_user_row_is_preserved_not_physically_deleted() {
        Integer userId = requireUserId();
        Integer count = jdbc.queryForObject("SELECT count(*) FROM task_manager.app_user WHERE user_id = ?",
                Integer.class, userId);
        assertThat(count).isEqualTo(1);
    }

    @Then("the response shows given names, family names, email and status")
    public void the_response_shows_given_names_family_names_email_and_status() {
        Map body = lastBody();
        assertThat(body.get("givenNames")).isNotNull();
        assertThat(body.get("familyNames")).isNotNull();
        assertThat(body.get("email")).isNotNull();
        assertThat(body.get("active")).isNotNull();
    }

    @Then("the list contains the created users")
    public void the_list_contains_the_created_users() {
        ResponseEntity r = context.getLastListResponse();
        List body = (List) r.getBody();
        assertThat(body).hasSize(3); // jefe del escenario + 2 usuarios creados
        assertThat(body).extracting(m -> ((Map) m).get("givenNames")).contains("Ana", "Luis");
        for (Object row : body) {
            assertThat(((Map) row).keySet()).doesNotContain("password", "passwordHash", "password_hash", "token");
        }
    }

    @Then("no entry exposes password_hash, password or token")
    public void no_entry_exposes_password_hash_password_or_token() {
        ResponseEntity r = context.getLastListResponse();
        List body = (List) r.getBody();
        assertThat(body).isNotNull();
        for (Object row : body) {
            assertThat(((Map) row).keySet()).doesNotContain("password", "passwordHash", "password_hash", "token");
        }
    }

    /* ---------------- helpers ---------------- */

    private Integer requireUserId() {
        Integer userId = context.getCreatedUserId();
        if (userId == null) {
            throw new IllegalStateException("No hay un usuario creado en el contexto del escenario.");
        }
        return userId;
    }

    private void assertNoSecrets(Map body) {
        assertThat(body.keySet()).doesNotContain("password", "passwordHash", "password_hash", "token");
    }

    @SuppressWarnings("unchecked")
    private Map lastBody() {
        ResponseEntity r = context.getLastPutResponse();
        if (r != null && r.getBody() != null) {
            return (Map) r.getBody();
        }
        ResponseEntity g = context.getLastGetResponse();
        if (g != null && g.getBody() != null) {
            return (Map) g.getBody();
        }
        throw new IllegalStateException("No hay respuesta de cuerpo en el contexto del escenario.");
    }
}
