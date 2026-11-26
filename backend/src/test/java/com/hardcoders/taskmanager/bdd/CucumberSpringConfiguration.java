package com.hardcoders.taskmanager.bdd;

import com.hardcoders.taskmanager.TaskManagerApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Configuración de contexto Spring para Cucumber-JVM.
 * Carga el contexto de la aplicación con servidor en puerto aleatorio (RANDOM_PORT)
 * para que los steps realicen llamadas HTTP reales contra la API en ejecución.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TaskManagerApplication.class,
        properties = {
                "spring.datasource.url=jdbc:postgresql://localhost:5432/gestor_tareas_test?currentSchema=task_manager",
                "spring.datasource.username=gestor_tareas_owner",
                "spring.datasource.password=ownerpass",
                "spring.jpa.hibernate.ddl-auto=validate",
                "app.security.jwt.secret=test-secret-for-etapa4-integration-0123456789",
                "app.security.jwt.expiration-minutes=60",
                "app.security.jwt.issuer=gestor-tareas-test"
        })
@CucumberContextConfiguration
public class CucumberSpringConfiguration {
    // Clase de configuración: el contexto Spring se carga mediante @SpringBootTest
    // y se comparte entre todos los escenarios de Cucumber.
}
