package com.hardcoders.taskmanager.bdd.hooks;

import com.hardcoders.taskmanager.bdd.context.ScenarioContext;
import com.hardcoders.taskmanager.bdd.support.ApiClient;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Hooks de ciclo de vida de escenarios Cucumber.
 * Limpia la base de datos de integración antes de cada escenario y crea un usuario
 * jefe autenticado cuyo token JWT se reutiliza en los steps del escenario.
 * No hay estado estático compartido entre escenarios.
 * Nota: sin @Component; Cucumber-Spring registra las glue classes como beans Spring.
 */
public class Hooks {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment env;

    @Autowired
    private ScenarioContext context;

    private static boolean contextInitialized = false;

    @BeforeAll
    public static void beforeAll() {
        // No hay operación de clase necesaria; el contexto Spring se carga vía CucumberSpringConfiguration.
    }

    @Before
    public void beforeScenario() {
        int port = env.getProperty("local.server.port", Integer.class, 8080);
        ApiClient apiClient = new ApiClient(port);

        // 1. Limpiar tablas en orden inverso de FK (patrón de PostgresIntegrationTest)
        jdbc.execute("DELETE FROM task_manager.task_assignment");
        jdbc.execute("DELETE FROM task_manager.task_plan_history");
        jdbc.execute("DELETE FROM task_manager.task_status_history");
        jdbc.execute("DELETE FROM task_manager.subtask");
        jdbc.execute("DELETE FROM task_manager.task");
        jdbc.execute("DELETE FROM task_manager.activity");
        jdbc.execute("DELETE FROM task_manager.stage");
        jdbc.execute("DELETE FROM task_manager.project");
        jdbc.execute("DELETE FROM task_manager.project_plan_history");
        jdbc.execute("DELETE FROM task_manager.app_user");

        // 2. Crear usuario jefe activo para autenticación
        String email = "chief.ec4@" + System.currentTimeMillis() + "@gestor.local";
        String hash = passwordEncoder.encode("ChiefPass123!");
        jdbc.update(
                "INSERT INTO task_manager.app_user (email, password_hash, given_names, family_names, is_chief, is_active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                email.toLowerCase(), hash, "Chief", "EC4", true, true, Timestamp.from(Instant.now()), Timestamp.from(Instant.now())
        );

        // 3. Login y almacenar token en el contexto de escenario
        String token = apiClient.login(email.toLowerCase(), "ChiefPass123!");
        context.setAuthToken(token);
        context.setChiefEmail(email.toLowerCase());

        // 4. Resetear estado de escenario
        context.reset();
        context.setAuthToken(token);
        context.setChiefEmail(email.toLowerCase());
    }

    @After
    public void afterScenario() {
        // No hay operación necesaria tras cada escenario; la limpieza ocurre antes del siguiente.
    }

    @AfterAll
    public static void afterAll() {
        // No hay operación de clase necesaria.
    }
}
