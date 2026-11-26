# INFORME EC4 — INTEGRANTE 1

**Elemento de Competencia 4 — BDD + Gherkin + CRUD Full-Stack + Pruebas de Integración + Cobertura de Pruebas + CI/CD**  
**Informe individual — Integrante 1**  
**Responsabilidad asignada:** BDD, proceso principal e integración (infraestructura Cucumber-JVM, PP-01, contexto/hooks/cliente HTTP, configuración de integración, Maven Surefire/Failsafe, coordinación de regresión, CI GitHub Actions, consolidación de cobertura, documentación común)  
**Repositorio:** `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`  
**Rama:** `Refactor-E2`  
**SHA documentada:** `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3`  
**Docente:** M. Sc. Ing. Joel Reynaldo Alánez Durán  
**Asignatura:** Implementar Frameworks de Pruebas Automáticas (Unidad 4)  
**Fecha de entrega / versión:** 02/12/2026 — v1.0  
**Idioma:** Español  
**Documento canónico vinculado:** `BASE_DOCUMENTAL_EC4.md` (directorio padre)

> **Nota sobre nombres de integrantes:** el repositorio y las fuentes académicas usan los placeholders `Integrante 1`, `Integrante 2` e `Integrante 3`. Estos nombres deben sustituirse por los nombres oficiales antes de la entrega formal. No se inventaron nombres, firmas ni aportes personales.

---

## 1. Portada

| Campo | Valor |
|---|---|
| Proyecto | Gestor de Tareas Personales — Release 1 |
| Entregable | Elemento de Competencia 4 (EC4) — Informe individual (Integrante 1) |
| Tema | BDD + Pruebas de Integración + Cobertura de Pruebas |
| Integrante | Integrante 1 (BDD, proceso principal e integración) |
| Integrantes del equipo | Integrante 1, Integrante 2, Integrante 3 |
| Docente | M. Sc. Ing. Joel Reynaldo Alánez Durán |
| Asignatura | Implementar Frameworks de Pruebas Automáticas (Unidad 4) |
| Repositorio | https://github.com/CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1 |
| Rama | `Refactor-E2` |
| SHA documentada | `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3` |
| Fecha de entrega | 02/12/2026 |
| Versión del documento | 1.0 |

---

## 2. Introducción

### 2.1 Propósito del Gestor de Tareas Personales

El Gestor de Tareas Personales organiza el trabajo de un equipo a través de una jerarquía **Proyecto → Etapa → Actividad → Tarea → Subtarea**, con gestión de usuarios (`app_user`) y asignación de trabajadores a tareas (`task_assignment`), autenticación JWT y control de acceso por roles.

### 2.2 Contexto del sistema preexistente al EC4

Antes del EC4 el sistema ya existía con arquitectura hexagonal (Java 21, Spring Boot 4.1.0, Maven, Spring Security + JWT, JPA/Hibernate, PostgreSQL 18) y SPA Angular 22.1. Las tablas `app_user` y `task_assignment` estaban modeladas con implementaciones parciales. No existía infraestructura BDD ni archivos `.feature`.

### 2.3 Objetivo del trabajo de calidad y automatización (Integrante 1)

Responsabilidad del Integrante 1: levantar la infraestructura BDD con Cucumber-JVM, ejecutar el proceso principal como prueba de integración real, configurar la ejecución Maven (Surefire/Failsafe), coordinar la regresión, definir el pipeline CI de GitHub Actions y consolidar la cobertura de pruebas.

### 2.4 Qué se probó y por qué

Se probaron los 19 escenarios de la matriz académica (1 proceso principal + 9 `app_user` + 9 `task_assignment`) como pruebas de integración reales: HTTP contra el servidor Spring Boot en puerto aleatorio y verificación de persistencia en `gestor_tareas_test`, sin mocks de repositorio. El Integrante 1 fue responsable de la infraestructura que hizo posible esa ejecución y del escenario PP-01.

### 2.5 Alcance concreto del EC4 (enfoque Integrante 1)

- Proceso principal: creación y persistencia de la jerarquía (PP-01, 1 Happy Path).
- Infraestructura BDD: Cucumber-JVM 7.18.0, archivos `.feature`, Step Definitions, Hooks, `ScenarioContext`, `ApiClient`, `CucumberIntegrationTest`, `CucumberSpringConfiguration`.
- Ejecución Maven: `mvn test` / `mvn verify` (Surefire unitarias, integración `PostgresIntegrationTest`, Cucumber).
- CI GitHub Actions: workflow `.github/workflows/ec4-quality.yml` (jobs backend, frontend, e2e, quality-gate), definido y validado sintácticamente, no ejecutado en GitHub.
- Consolidación de cobertura JaCoCo (backend) y Vitest (frontend).

### 2.6 Límites de alcance

No se añadieron funcionalidades de negocio ajenas. No hay despliegue continuo (CD) por falta de destino real. El workflow de CI no se ha disparado en GitHub (sin run real verificable).

---

## 3. Desarrollo del sistema: descripción de las funcionalidades

### 3.1 Arquitectura general y tecnologías verificadas (relevantes para el Integrante 1)

Backend hexagonal con capas `adapter/in/web`, `adapter/out/persistence`, `domain`, `application`, `configuration`. Tecnologías clave para la integración BDD:

| Componente | Versión | Origen |
|---|---|---|
| Java | 21 (pom); entorno JDK 24.0.2 | `backend/pom.xml` |
| Spring Boot | 4.1.0 | `backend/pom.xml` |
| Maven | 3.9.16 | ejecución real |
| Cucumber-JVM | 7.18.0 | `backend/pom.xml` |
| PostgreSQL | 18 | `database/docker-compose.yml` |
| JaCoCo | 0.8.15 | `backend/pom.xml` |

### 3.2 Autenticación y autorización (enfoque integración)

JWT (HS256) vía `POST /api/auth/login`; token `Bearer` en llamadas protegidas. Los escenarios BDD obtienen el token en `Hooks` (login de un jefe activo creado por escenario) y lo reutilizan en `ApiClient` para cada llamada HTTP.

### 3.3 Proceso principal y jerarquía (responsabilidad Integrante 1)

Flujo: Proyecto → Etapa → Actividad → Tarea → Subtarea. El escenario PP-01 verifica códigos HTTP 2xx y persistencia real en PostgreSQL mediante `JdbcTemplate` (`COUNT` por cada nivel con FK válidas). Este escenario es de exclusiva responsabilidad del Integrante 1.

### 3.4 Gestión de usuarios y asignaciones (contexto)

Las operaciones CRUD de `app_user` (responsabilidad Integrante 2) y `task_assignment` (responsabilidad Integrante 3) se ejecutan sobre la misma infraestructura BDD levantada por el Integrante 1. Se describen completo en `BASE_DOCUMENTAL_EC4.md` sección 3 y en los informes de Integrante 2 y 3.

### 3.5 Funcionalidades preexistentes vs implementadas en EC4

Véase `BASE_DOCUMENTAL_EC4.md` sección 3.6. El aporte del Integrante 1 no es funcionalidad de negocio sino la infraestructura de prueba y el escenario PP-01.

---

## 4. Formulación de los escenarios

Matriz completa de 19 escenarios. La columna "Responsable" indica la responsabilidad principal del contenido del escenario; el Integrante 1 es responsable de PP-01 y de la infraestructura que ejecuta todos los escenarios.

| Código | Tabla / Proceso | Operación | Tipo | Responsable | Resultado esperado | Resultado real |
|---|---|---|---|---|---|---|
| PP-01 | Proceso principal | Jerarquía | Happy | Integrante 1 | 2xx + persistencia BD | PASSED |
| USR-INS-01 | app_user | INSERT | Happy | Integrante 2 | 201, sin secretos | PASSED |
| USR-INS-02 | app_user | INSERT | Happy | Integrante 2 | 201 | PASSED |
| USR-INS-03 | app_user | INSERT | Unhappy | Integrante 2 | 400, sin creación | PASSED |
| USR-UPD-01 | app_user | UPDATE | Happy | Integrante 2 | 200, actualizado | PASSED |
| USR-UPD-02 | app_user | UPDATE | Happy | Integrante 2 | 200, email normalizado | PASSED |
| USR-UPD-03 | app_user | UPDATE | Unhappy | Integrante 2 | 400, sin cambio | PASSED |
| USR-DEL-01 | app_user | DELETE | Happy | Integrante 2 | 200, inactivo, fila preservada | PASSED |
| USR-SEL-01 | app_user | SELECT individual | Happy | Integrante 2 | 200, sin password_hash | PASSED |
| USR-SEL-02 | app_user | SELECT listado | Happy | Integrante 2 | 200, sin secretos | PASSED |
| ASG-INS-01 | task_assignment | INSERT | Happy | Integrante 3 | 200, razón persistida | PASSED |
| ASG-INS-02 | task_assignment | INSERT | Happy | Integrante 3 | 200, DEFAULT_REASON | PASSED |
| ASG-INS-03 | task_assignment | INSERT | Unhappy | Integrante 3 | 400, sin persistencia | PASSED |
| ASG-UPD-01 | task_assignment | UPDATE | Happy | Integrante 3 | 200, motivo actualizado | PASSED |
| ASG-UPD-02 | task_assignment | UPDATE | Happy | Integrante 3 | 200, DEFAULT_REASON | PASSED |
| ASG-UPD-03 | task_assignment | UPDATE | Unhappy | Integrante 3 | 400, conserva previo | PASSED |
| ASG-DEL-01 | task_assignment | DELETE | Happy | Integrante 3 | 204, revocada | PASSED |
| ASG-SEL-01 | task_assignment | SELECT individual | Happy | Integrante 3 | 200, clave compuesta | PASSED |
| ASG-SEL-02 | task_assignment | SELECT listado | Happy | Integrante 3 | 200, lista de tarea | PASSED |

Total: 19 ejecuciones = 14 Happy Path + 5 Unhappy Path. Resultado real: 19/19 aprobados, 123 pasos, 0 fallos, 0 omitidos. Fuente: salida Cucumber y `backend/target/surefire-reports/TEST-...CucumberIntegrationTest.xml`. Fecha: 26/11/2026. Comando: `mvn test -Dtest=CucumberIntegrationTest`.

Archivos `.feature` reales (rutas relativas desde `docs/EC4/informes/`):
- `../../../backend/src/test/resources/features/ec4/proceso-principal.feature` (`@PP-01`)
- `../../../backend/src/test/resources/features/ec4/app-user-crud.feature` (`@USR`)
- `../../../backend/src/test/resources/resources/features/ec4/task-assignment-crud.feature` (`@ASG`)

> Nota: las rutas anteriores se escriben desde `docs/EC4/informes/`; equivalentes desde `docs/EC4/`: `../../backend/src/test/resources/features/ec4/...`.

---

## 5. Formulación de los Step Definitions

### 5.1 Arquitectura de integración (responsabilidad Integrante 1)

Los escenarios Gherkin se ejecutan con **Cucumber-JVM 7.18.0** sobre JUnit Platform. Punto de entrada: `CucumberIntegrationTest.java` (`@Test` JUnit 5, `Main.run()`, tags `@PP-01 or @USR or @ASG`, tres archivos `.feature`). Contexto Spring: `CucumberSpringConfiguration.java` (`@SpringBootTest webEnvironment = RANDOM_PORT`, `gestor_tareas_test`). Cliente HTTP: `ApiClient.java` (`RestTemplate` instanciado manualmente, llamadas HTTP reales a `http://localhost:<local.server.port>`). Contexto aislado: `ScenarioContext.java` (`@Component`, `reset()` en `@Before`). Limpieza y jefe por escenario: `Hooks.java` (`DELETE` orden inverso de FK, email único por escenario, login JWT).

**Evidencia de integración real (no prueba unitaria disfrazada):** cada paso atraviesa HTTP real → controlador → casos de uso → dominio → políticas → JPA/JDBC → PostgreSQL `gestor_tareas_test`; los `@Then` de BD verifican con `JdbcTemplate`. No hay mocks de repositorio.

### 5.2 Clases de Step Definitions (código fuente real)

Todos los archivos están bajo `backend/src/test/java/com/hardcoders/taskmanager/bdd/`. Rutas relativas desde `docs/EC4/informes/` (prefix `../../../backend/...`; desde `docs/EC4/`: `../../backend/...`).

#### 5.2.1 `CucumberIntegrationTest.java` — punto de entrada (Integrante 1)

Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/CucumberIntegrationTest.java`

```java
package com.hardcoders.taskmanager.bdd;

import io.cucumber.core.cli.Main;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
```

Salida real de ejecución: `19 Scenarios (19 passed), 123 Steps (123 passed)`, `CUCUMBER_EXIT_CODE=0`.

#### 5.2.2 `CucumberSpringConfiguration.java` — contexto Spring (Integrante 1)

Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/CucumberSpringConfiguration.java`

Configura `@SpringBootTest(webEnvironment = RANDOM_PORT)` con propiedades sobre `gestor_tareas_test` (usuario `gestor_tareas_owner`, `ddl-auto: validate`, JWT secreto de prueba).

#### 5.2.3 `Hooks.java` — ciclo de vida (Integrante 1)

Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/hooks/Hooks.java`

`@Before` por escenario: limpia tablas en orden inverso de FK (`task_assignment`, `task_plan_history`, `task_status_history`, `subtask`, `task`, `activity`, `stage`, `project`, `project_plan_history`, `app_user`) solo en `gestor_tareas_test`; crea un jefe activo con email único (`chief.ec4@<timestamp>@gestor.local`); hace `ApiClient.login` y guarda token y email en `ScenarioContext`; llama `context.reset()`. Sin estado estático compartido entre escenarios.

#### 5.2.4 `ScenarioContext.java` — estado aislado (Integrante 1)

Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/context/ScenarioContext.java`

Bean `@Component` con `projectId`, `stageId`, `activityId`, `taskId`, `subtaskId`, `workerUserId`, `createdUserId`, `authToken`, `chiefEmail`, `lastPutResponse`, `lastGetResponse`, `lastListResponse`. `reset()` los limpia antes de cada escenario; `isComplete()` valida la jerarquía PP-01.

#### 5.2.5 `ApiClient.java` — cliente HTTP real (Integrante 1)

Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/support/ApiClient.java`

Clase POJO con `RestTemplate`. Métodos: `login`, `createProject`, `createStage`, `createActivity`, `createTask`, `createSubtask`, `postUser`, `getUser`, `listUsers`, `putUser`, `deleteUser`, `postAssignment`, `listAssignments`, `getAssignment`, `putAssignmentReason`, `deleteAssignment`. Captura `HttpClientErrorException`/`HttpServerErrorException` y devuelve `ResponseEntity` con el código de estado real.

#### 5.2.6 `PrincipalProcessSteps.java` — PP-01 (Integrante 1)

Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/PrincipalProcessSteps.java`

Métodos (código fuente completo en `BASE_DOCUMENTAL_EC4.md` sección 5.3.1):
- `an_authenticated_chief_user_with_email_and_password`
- `i_create_a_project_with_name_and_description`
- `i_create_a_stage_with_name_in_the_project`
- `i_create_an_activity_with_name_in_the_stage`
- `i_create_a_task_with_title_in_the_activity`
- `i_create_a_subtask_with_title_in_the_task`
- `the_hierarchy_is_persisted` (verificación JDBC `COUNT` de cada nivel)
- `the_project_has_1_stage`, `the_stage_has_1_activity`, `the_activity_has_1_task`, `the_task_has_1_subtask`

Ejemplo representativo (`the_hierarchy_is_persisted`): ejecuta `jdbc.queryForObject("SELECT COUNT(*) FROM task_manager.project WHERE project_id = ?", Integer.class, context.getProjectId())` y afirma `== 1`, igual para `stage`, `activity`, `task`, `subtask` con sus FK.

#### 5.2.7 `UserCrudSteps.java` — escenarios USR (Integrante 2)

Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/UserCrudSteps.java`

Métodos únicos: `i_create_a_valid_user_with_given_names_and_family_names`, `i_create_a_user_with_email_and_valid_names_and_password`, `a_user_exists_with_given_names_and_family_names`, `a_second_user_exists_with_given_names_and_family_names`, `i_update_the_user_s_names_to_and`, `i_update_the_user_s_email_to`, `i_deactivate_the_user`, `i_consult_the_user_by_id`, `i_list_users`, y Then helpers (`the_user_is_created_with_status_201`, `the_response_has_no_password_hash_password_or_token`, `the_user_is_stored_in_postgresql_as_active_and_non_chief`, `the_response_is_400`, `no_user_is_stored_in_postgresql`, `the_response_is_200`, `the_user_s_names_are_and_in_postgresql`, `the_user_s_email_is_stored_normalized_in_postgresql`, `the_user_s_previous_names_are_preserved_in_postgresql`, `the_user_is_inactive_in_postgresql`, `the_user_row_is_preserved_not_physically_deleted`, `the_response_shows_given_names_family_names_email_and_status`, `the_list_contains_the_created_users`, `no_entry_exposes_password_hash_password_or_token`). Código fuente completo en `BASE_DOCUMENTAL_EC4.md` sección 5.3.2.

#### 5.2.8 `AssignmentCrudSteps.java` — escenarios ASG (Integrante 3)

Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/AssignmentCrudSteps.java`

Métodos únicos: `a_task_exists_for_the_authenticated_chiefs_project`, `an_active_worker_exists`, `two_active_workers_exist`, `createActiveWorker`, `the_worker_is_assigned_to_the_task`, `the_worker_is_assigned_to_the_task_with_reason`, `both_workers_are_assigned_to_the_task`, `i_assign_the_worker_to_the_task_with_reason`, `i_assign_the_worker_to_the_task_without_a_reason`, `i_assign_the_worker_to_the_task_with_a_reason_of_501_characters`, `i_update_the_assignment_reason_to`, `i_update_the_assignment_reason_to_an_empty_value`, `i_update_the_assignment_reason_to_a_value_of_characters`, `i_revoke_the_workers_assignment_from_the_task`, `i_consult_the_assignment_by_task_and_worker`, `i_list_assignments_of_the_task`, y Then helpers. Código fuente completo en `BASE_DOCUMENTAL_EC4.md` sección 5.3.3.

### 5.3 Matriz de trazabilidad: 19 escenarios → pasos, métodos, clases, archivos

| Código | Paso Gherkin representativo | Método Step Definition | Clase | Archivo (desde `docs/EC4/`) |
|---|---|---|---|---|
| PP-01 | When crear proyecto / Then jerarquía persistida | `i_create_a_project_with_name_and_description`, `the_hierarchy_is_persisted` | `PrincipalProcessSteps` | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/PrincipalProcessSteps.java` |
| USR-INS-01 | When crear usuario válido / Then 201 / Then sin secretos | `i_create_a_valid_user_with_given_names_and_family_names`, `the_user_is_created_with_status_201`, `the_response_has_no_password_hash_password_or_token` | `UserCrudSteps` | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/UserCrudSteps.java` |
| USR-INS-02 | idem USR-INS-01 (segundo usuario) | mismos métodos de `UserCrudSteps` | `UserCrudSteps` | idem |
| USR-INS-03 | When crear email inválido / Then 400 / Then no usuario en BD | `i_create_a_user_with_email_and_valid_names_and_password`, `the_response_is_400`, `no_user_is_stored_in_postgresql` | `UserCrudSteps` | idem |
| USR-UPD-01 | When actualizar nombres / Then 200 / Then nombres en BD | `i_update_the_user_s_names_to_and`, `the_response_is_200`, `the_user_s_names_are_and_in_postgresql` | `UserCrudSteps` | idem |
| USR-UPD-02 | When actualizar email / Then 200 / Then email normalizado | `i_update_the_user_s_email_to`, `the_response_is_200`, `the_user_s_email_is_stored_normalized_in_postgresql` | `UserCrudSteps` | idem |
| USR-UPD-03 | When actualizar email inválido / Then 400 / Then nombres preservados | `i_update_the_user_s_email_to`, `the_response_is_400`, `the_user_s_previous_names_are_preserved_in_postgresql` | `UserCrudSteps` | idem |
| USR-DEL-01 | When desactivar / Then 200 / Then inactivo / Then fila preservada | `i_deactivate_the_user`, `the_response_is_200`, `the_user_is_inactive_in_postgresql`, `the_user_row_is_preserved_not_physically_deleted` | `UserCrudSteps` | idem |
| USR-SEL-01 | When consultar por id / Then 200 / Then sin secretos | `i_consult_the_user_by_id`, `the_response_is_200`, `the_response_has_no_password_hash_password_or_token` | `UserCrudSteps` | idem |
| USR-SEL-02 | When listar / Then 200 / Then ninguna expone secretos | `i_list_users`, `the_response_is_200`, `no_entry_exposes_password_hash_password_or_token` | `UserCrudSteps` | idem |
| ASG-INS-01 | When asignar con razón / Then 200 / Then razón normalizada / Then en BD | `i_assign_the_worker_to_the_task_with_reason`, `the_assignment_is_created_with_status_200`, `the_response_reason_is_normalized_to`, `the_assignment_is_stored_in_postgresql_with_the_normalized_reason` | `AssignmentCrudSteps` | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/AssignmentCrudSteps.java` |
| ASG-INS-02 | When asignar sin razón / Then DEFAULT_REASON | `i_assign_the_worker_to_the_task_without_a_reason`, `the_response_reason_is_the_default`, `the_assignment_is_stored_in_postgresql_with_the_default_reason` | `AssignmentCrudSteps` | idem |
| ASG-INS-03 | When asignar razón 501 chars / Then 400 / Then no persistencia | `i_assign_the_worker_to_the_task_with_a_reason_of_501_characters`, `no_assignment_is_stored_in_postgresql_for_that_worker` | `AssignmentCrudSteps` | idem |
| ASG-UPD-01 | When actualizar razón / Then 200 / Then mantiene auditoría | `i_update_the_assignment_reason_to`, `the_assignment_keeps_the_same_task_worker_and_audit_fields` | `AssignmentCrudSteps` | idem |
| ASG-UPD-02 | When actualizar a vacío / Then DEFAULT_REASON | `i_update_the_assignment_reason_to_an_empty_value`, `the_response_reason_is_the_default` | `AssignmentCrudSteps` | idem |
| ASG-UPD-03 | When actualizar a 501 chars / Then 400 / Then conserva previo | `i_update_the_assignment_reason_to_a_value_of_characters(501)`, `the_assignment_still_has_reason_in_postgresql` | `AssignmentCrudSteps` | idem |
| ASG-DEL-01 | When revocar / Then 204 / Then no existe en BD | `i_revoke_the_workers_assignment_from_the_task`, `the_response_is_204`, `the_assignment_no_longer_exists_in_postgresql` | `AssignmentCrudSteps` | idem |
| ASG-SEL-01 | When consultar por tarea y trabajador / Then 200 | `i_consult_the_assignment_by_task_and_worker`, `the_response_shows_the_worker_task_and_reason` | `AssignmentCrudSteps` | idem |
| ASG-SEL-02 | When listar asignaciones / Then 200 / Then ambas | `i_list_assignments_of_the_task`, `the_list_contains_both_assignments` | `AssignmentCrudSteps` | idem |

Cuando un método se reutiliza entre escenarios (ej. `i_create_a_valid_user_with_given_names_and_family_names` en USR-INS-01 y USR-INS-02), se cita el mismo identificador del catálogo; `ScenarioContext` se resetea entre escenarios por `Hooks.beforeScenario()`.

---

## 6. Ejecución de pruebas (enfoque Integrante 1)

### 6.1 Resultado por escenario (19 escenarios)

Idéntico a la tabla de la sección 4 (todos PASSED, 26/11/2026, comando `mvn test -Dtest=CucumberIntegrationTest`). Fuente: salida Cucumber y surefire XML.

### 6.2 Resumen consolidado (backend, responsabilidad Integrante 1)

| Nivel | Planificados | Ejecutados | Aprobados | Fallados | Omitidos | Pendientes / no ejecutados | Fuente |
|---|---:|---:|---:|---:|---:|---|---|
| BDD Cucumber (19 escenarios) | 19 | 19 | 19 | 0 | 0 | 0 | salida Cucumber + surefire XML |
| Backend unitarias (Surefire) | ~343 | ~343 | ~343 | 0 | 0 | 0 | `backend/target/surefire-reports/` |
| Backend integración (`PostgresIntegrationTest`) | 44 | 44 | 44 | 0 | 0 | 0 | `backend/target/surefire-reports/` |
| Backend total (`mvn verify`) | 412 | 412 | 412 | 0 | 0 | 0 | `mvn verify` BUILD SUCCESS |

No se contó ningún escenario dos veces por ejecución local y CI: no existe run real de GitHub Actions para esta rama. El workflow `.github/workflows/ec4-quality.yml` está definido (Etapa 5) pero no se ha disparado en GitHub. Todos los resultados son de **validación local**.

### 6.3 Entorno y comando de ejecución (Integrante 1)

- Rama: `Refactor-E2`, SHA `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3`
- JDK 24.0.2, Maven 3.9.16, PostgreSQL 18 (contenedor `gestor-tareas-db`, localhost:5432), base `gestor_tareas_test`
- Comando: `cd backend && mvn test -Dtest=CucumberIntegrationTest` (BDD) y `cd backend && mvn verify` (regresión completa con JaCoCo)
- Duración por escenario: no registrada en el reporte de Cucumber (salida de consola)

---

## 7. Cobertura de pruebas (Integrante 1 — consolidación backend JaCoCo)

### 7.1 Herramienta backend

**JaCoCo 0.8.15** (`backend/pom.xml`): instrumenta bytecode durante `mvn verify` y ejecuta `jacoco:check` en la fase `verify`. Mide cobertura de instrucciones, líneas, ramas, métodos y complejidad sobre el código de producción ejecutado por pruebas (unitarias Surefire + integración + Cucumber).

### 7.2 Umbrales configurados (backend)

| Métrica | Umbral | Origen |
|---|---|---|
| LINE | ≥ 85 % | `backend/pom.xml` (`jacoco:check`) |
| BRANCH | ≥ 85 % | `backend/pom.xml` (`jacoco:check`) |

### 7.3 Resultado real backend (fecha y SHA del reporte)

Generado en validación local del 26/11/2026 sobre la rama `Refactor-E2` (código equivalente a HEAD `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3`). `backend/target/` está en `.gitignore`.

| Métrica | Cubiertas | Total | % | Umbral | Estado |
|---|---:|---:|---:|---:|---|
| LINE | 2820 | 2964 | 95,14 % | ≥ 85 % | CUMPLE |
| BRANCH | 642 | 753 | 85,26 % | ≥ 85 % | CUMPLE |

`mvn verify` → `jacoco:check`: "All coverage checks have been met."

Reportes: `../../backend/target/site/jacoco/index.html` (`jacoco.csv`, `jacoco.xml`), `../../backend/target/jacoco.exec`.

### 7.4 Cobertura frontend (Vitest, contexto general)

Responsable general de consolidación; detalle en `BASE_DOCUMENTAL_EC4.md` sección 7 y en informes de Integrante 2 y 3.

| Métrica | % | Umbral | Estado |
|---|---:|---:|---|
| Statements | 92,95 % | ≥ 85 % | CUMPLE |
| Branches | 88,53 % | ≥ 85 % | CUMPLE |
| Functions | 87,37 % | ≥ 85 % | CUMPLE |
| Lines | 94,05 % | ≥ 85 % | CUMPLE |

### 7.5 Capturas de cobertura

**BLOQUEO documentado:** no fue posible crear capturas auténticas de los reportes HTML de JaCoCo ni Vitest porque las herramientas de navegador bloquean el protocolo `file://` y no se levantó un servidor HTTP local para servirlos. No existen `docs/EC4/evidencias/cobertura/jacoco-resumen.png` ni `vitest-resumen.png`. No se generaron gráficos ficticios ni imágenes decorativas.

Reportes de origen (rutas desde `docs/EC4/informes/`): `../../../backend/target/site/jacoco/index.html` y `../../../frontend/coverage/gestor-tareas/index.html`.

**Cobertura de código vs cantidad de pruebas:** la cobertura mide fracción de código de producción ejecutado, no el número de pruebas. Backend: 412 pruebas y JaCoCo LINE 95,14 % / BRANCH 85,26 %.

---

## 8. Conclusiones y lecciones aprendidas (Integrante 1)

### 8.1 Conclusiones técnicas (hechos verificables)

- Los 19 escenarios BDD se ejecutaron y aprobaron (19/19, 123 pasos, 0 fallos) como pruebas de integración reales contra API Spring Boot y PostgreSQL `gestor_tareas_test`.
- La infraestructura BDD (Cucumber-JVM 7.18.0, Hooks, `ScenarioContext`, `ApiClient`, `CucumberIntegrationTest`) está operativa y ejecuta todos los escenarios sin estado compartido.
- El backend pasa 412/412 pruebas (`mvn verify` BUILD SUCCESS) con JaCoCo LINE 95,14 % y BRANCH 85,26 % (umbrales ≥ 85 % cumplidos).
- Cucumber-JVM es el framework BDD adecuado para este stack Java/Spring Boot; ReqnRoll es para C# y no aplica.
- No existe run real de GitHub Actions: el workflow `.github/workflows/ec4-quality.yml` está definido y validado sintácticamente, pero no se ha disparado en GitHub.

### 8.2 Lecciones aprendidas

- Una prueba de integración real requiere servidor arrancado (`@SpringBootTest RANDOM_PORT`), HTTP real y base de datos aislada (`gestor_tareas_test`); los mocks de repositorio no demuestran integración.
- El aislamiento de datos por escenario (limpieza de BD en `@Before`, emails únicos) evita filtraciones entre escenarios.
- La cobertura de código es útil pero insuficiente por sí sola: cumplir umbrales no garantiza que se hayan probado todos los requisitos de negocio.

### 8.3 Reflexiones personales

**[REFLEXIÓN PERSONAL A VALIDAR POR EL INTEGRANTE 1]** Bloque reservado para la experiencia subjetiva del Integrante 1 durante el EC4 (dificultades, resoluciones, trabajo en equipo, limitaciones pendientes). No hay evidencia objetiva en el historial para atribuirla.

### 8.4 Limitaciones pendientes

- Capturas de pantalla de reportes de cobertura (bloqueo `file://`, sección 7.5).
- Pipeline de GitHub Actions definido pero no ejecutado en GitHub (sin run real verificable).
- Validación de navegador real vía Playwright MCP no disponible (bitacora Etapa 3, BLOCKED); las pruebas E2E sí se ejecutaron con Playwright Test (25/25).

---

## 9. Anexos

### 9.1 Repositorio y versión

| Ítem | Valor |
|---|---|
| URL del repositorio | https://github.com/CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1 |
| Rama | `Refactor-E2` |
| SHA documentada | `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3` |
| Fecha del SHA | 2026-11-26 14:15:10 -0400 |
| Estado de trabajo | Limpio (`## Refactor-E2...origin/Refactor-E2`, sin cambios sin commit) |
| PDFs generados | Ninguno |
| Modificaciones de producto | Ninguna |

### 9.2 Enlaces a archivos fuente (relativos desde `docs/EC4/informes/`)

| Artefacto | Ruta relativa |
|---|---|
| Feature proceso principal | `../../../backend/src/test/resources/features/ec4/proceso-principal.feature` |
| Feature CRUD `app_user` | `../../../backend/src/test/resources/features/ec4/app-user-crud.feature` |
| Feature CRUD `task_assignment` | `../../../backend/src/test/resources/features/ec4/task-assignment-crud.feature` |
| CucumberIntegrationTest | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/CucumberIntegrationTest.java` |
| CucumberSpringConfiguration | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/CucumberSpringConfiguration.java` |
| Hooks | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/hooks/Hooks.java` |
| ScenarioContext | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/context/ScenarioContext.java` |
| ApiClient | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/support/ApiClient.java` |
| PrincipalProcessSteps | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/PrincipalProcessSteps.java` |
| UserCrudSteps | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/UserCrudSteps.java` |
| AssignmentCrudSteps | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/AssignmentCrudSteps.java` |
| Workflow CI | `../../../.github/workflows/ec4-quality.yml` |
| Migraciones DB | `../../../database/migrations/` |
| Documento canónico | `../BASE_DOCUMENTAL_EC4.md` |

### 9.3 Enlaces a reportes de ejecución y cobertura (desde `docs/EC4/informes/`)

| Reporte | Ruta relativa | Tipo |
|---|---|---|
| JaCoCo index | `../../../backend/target/site/jacoco/index.html` | HTML (local, gitignorado) |
| JaCoCo CSV | `../../../backend/target/site/jacoco/jacoco.csv` | CSV (local, gitignorado) |
| Surefire reports | `../../../backend/target/surefire-reports/` | XML por clase (local, gitignorado) |
| Cucumber report | `../../../backend/target/surefire-reports/TEST-com.hardcoders.taskmanager.bdd.CucumberIntegrationTest.xml` | XML (local, gitignorado) |
| Vitest coverage index | `../../../frontend/coverage/gestor-tareas/index.html` | HTML (local, gitignorado) |
| Vitest coverage JSON | `../../../frontend/coverage/gestor-tareas/coverage-final.json` | JSON (local, gitignorado) |
| Playwright report (histórico) | `../../../frontend/playwright-report/index.html` | HTML (20/11/2026, preservado, no regenerado) |

> Los reportes de `target/` y `coverage/` son artefactos locales del 26/11/2026, no versionados en Git (`.gitignore`). No existen links a runs de GitHub Actions porque el workflow no se ha disparado.

### 9.4 Documentación relacionada

Véase `BASE_DOCUMENTAL_EC4.md` sección 9 (`BASELINE_EC4.md`, `IMPLEMENTACION_EC4_BITACORA.md`, `REPORTE_REGRESION_EC4.md`, `CI_EC4.md`, `INFORME_FINAL_EC4.md`, informes de Integrante 2 y 3, `evidencias/README.md`).

### 9.5 Glosario

Véase `BASE_DOCUMENTAL_EC4.md` sección 9.5.

### 9.6 Referencias técnicas de apoyo

Véase `BASE_DOCUMENTAL_EC4.md` sección 9.6 (referencias industriales).

### 9.7 Preguntas para la defensa (Integrante 1)

1. P: ¿Por qué eligieron Cucumber-JVM y no ReqnRoll?  
   R: El backend es Java 21 + Spring Boot; ReqnRoll es para C#. Cucumber-JVM es compatible con el stack existente (JUnit Platform, Spring Test) y no requiere migrar el backend. Soporte: `backend/pom.xml` (cucumber-java/junit/spring 7.18.0) y `IMPLEMENTACION_EC4_BITACORA.md` (decisión de diseño).

2. P: ¿Cómo garantizan que las pruebas de integración son reales y no unitarias con mocks?  
   R: Usan `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `RestTemplate` contra el servidor real, `JdbcTemplate` contra PostgreSQL `gestor_tareas_test`, y limpian los datos entre escenarios sin mocks de repositorio. Soporte: `CucumberSpringConfiguration.java`, `Hooks.java`, `JdbcTemplate` en `PrincipalProcessSteps.java`.

---
*Fin del INFORME EC4 — INTEGRANTE 1.*
