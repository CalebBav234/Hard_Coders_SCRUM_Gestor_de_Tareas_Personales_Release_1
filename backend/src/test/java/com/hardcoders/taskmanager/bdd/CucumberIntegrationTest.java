package com.hardcoders.taskmanager.bdd;

import io.cucumber.core.cli.Main;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Punto de entrada JUnit 5 para Cucumber-JVM.
 * Invoca al motor Cucumber programáticamente para ejecutar el escenario PP-01
 * y los escenarios de CRUD de usuarios (USR-INS-01..03, USR-UPD-01..03, USR-DEL-01,
 * USR-SEL-01, USR-SEL-02) contra la API real del backend y PostgreSQL de integracion.
 * Ejecutado por Maven Surefire (proveedor JUnit Platform) como parte de mvn test.
 */
class CucumberIntegrationTest {

    @Test
    void ejecutarEscenariosEC4() {
        String[] args = {
                "--glue", "com.hardcoders.taskmanager.bdd",
                "--plugin", "pretty",
                "--tags", "@PP-01 or @USR or @ASG",
                "src/test/resources/features/ec4/proceso-principal.feature",
                "src/test/resources/features/ec4/app-user-crud.feature",
                "src/test/resources/features/ec4/task-assignment-crud.feature"
        };
        int exitCode = Main.run(args, Thread.currentThread().getContextClassLoader());
        assertEquals(0, exitCode, "Los escenarios EC4 deben ejecutarse con código de salida 0");
    }
}
