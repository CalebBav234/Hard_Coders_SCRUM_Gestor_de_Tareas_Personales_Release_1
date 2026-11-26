# INFORME FINAL EC4 — Gestor de Tareas Personales

**Elemento de Competencia 4 — BDD + Gherkin + CRUD Full-Stack + Pruebas de Integración + Cobertura de Pruebas + CI/CD**  
**Informe consolidado del equipo (3 integrantes)**  
**Repositorio:** `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`  
**Rama:** `Refactor-E2`  
**SHA documentada:** `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3`  
**Docente:** M. Sc. Ing. Joel Reynaldo Alánez Durán  
**Asignatura:** Implementar Frameworks de Pruebas Automáticas (Unidad 4)  
**Fecha de entrega / versión:** 02/12/2026 — v1.0  
**Idioma:** Español  
**Documento canónico vinculado:** `BASE_DOCUMENTAL_EC4.md` (mismo directorio)

> **Nota sobre nombres de integrantes:** el repositorio y las fuentes académicas usan los placeholders `Integrante 1`, `Integrante 2` e `Integrante 3`. Estos nombres deben sustituirse por los nombres oficiales antes de la entrega formal. No se inventaron nombres, firmas ni aportes personales.

---

## 1. Portada

| Campo | Valor |
|---|---|
| Proyecto | Gestor de Tareas Personales — Release 1 |
| Entregable | Elemento de Competencia 4 (EC4) — Informe consolidado del equipo |
| Tema | BDD + Pruebas de Integración + Cobertura de Pruebas |
| Repositorio | https://github.com/CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1 |
| Rama | `Refactor-E2` |
| SHA documentada | `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3` |
| Integrantes | Integrante 1, Integrante 2, Integrante 3 |
| Docente | M. Sc. Ing. Joel Reynaldo Alánez Durán |
| Asignatura | Implementar Frameworks de Pruebas Automáticas (Unidad 4) |
| Fecha de entrega | 02/12/2026 |
| Versión del documento | 1.0 |

---

## 2. Introducción

### 2.1 Propósito del sistema

El Gestor de Tareas Personales organiza el trabajo de un equipo a través de una jerarquía **Proyecto → Etapa → Actividad → Tarea → Subtarea**, con gestión de usuarios (`app_user`) y asignación de trabajadores a tareas (`task_assignment`), autenticación JWT y control de acceso por roles (Jefe / Encargado / trabajador).

### 2.2 Contexto del sistema preexistente al EC4

Antes del EC4 el sistema ya existía (arquitectura hexagonal backend Java 21 / Spring Boot 4.1.0, SPA Angular 22.1, PostgreSQL 18). Las tablas `app_user` y `task_assignment` estaban modeladas (migraciones V004 y V006) con implementaciones parciales: `app_user` solo tenía creación (`POST /api/users`) y UI de creación; `task_assignment` tenía creación, listado y revocación con diálogo en la tarea. Faltaban consulta individual, listado, actualización y desactivación de usuarios, y consulta individual y actualización del motivo de asignaciones. No existía infraestructura BDD ni archivos `.feature`.

### 2.3 Objetivo del trabajo de calidad y automatización

Aplicar BDD con Gherkin sobre pruebas de integración reales contra PostgreSQL, completar el CRUD full-stack (backend + frontend/UI) de las dos tablas existentes sin añadir funcionalidades ajenas, medir cobertura (JaCoCo backend, Vitest frontend) con umbrales del 85 %, y dejar evidencia ejecutable y documentada para la defensa académica.

### 2.4 Qué se probó y por qué

Se probaron los 19 escenarios de la matriz académica (1 proceso principal + 9 `app_user` + 9 `task_assignment`) porque cubren el proceso principal y el CRUD completo de las dos tablas, incluidos caminos de error con un único dato incorrecto. Cada escenario es una prueba de integración real: HTTP contra el servidor Spring Boot en puerto aleatorio y verificación de persistencia en `gestor_tareas_test`, sin mocks de repositorio.

### 2.5 Alcance concreto del EC4

- Proceso principal: creación y persistencia de la jerarquía (1 Happy Path).
- CRUD `app_user`: INSERT (2H+1U), UPDATE (2H+1U), DELETE (1H), SELECT individual (1H), SELECT listado (1H).
- CRUD `task_assignment`: INSERT (2H+1U), UPDATE (2H+1U), DELETE (1H), SELECT individual (1H), SELECT listado (1H).
- Integración real contra `gestor_tareas_test` (nunca `gestor_tareas` / producción).
- BDD con Cucumber-JVM 7.18.0 y Gherkin; cobertura JaCoCo y Vitest; UI completa para ambos CRUD.

### 2.6 Límites de alcance

No se añadieron funcionalidades de negocio ajenas, no se crearon tablas ni migraciones de negocio, no hay despliegue continuo (CD) por falta de destino real, y `main` no forma parte del flujo de CI de EC4.

---

## 3. Desarrollo del sistema: descripción de las funcionalidades

### 3.1 Arquitectura y tecnologías verificadas

Backend hexagonal: `adapter/in/web` (controladores REST), `adapter/out/persistence` (adaptadores JPA y repositorios Spring Data), `domain/model` (entidades), `domain/port` (puertos de salida), `domain/policy` (reglas de negocio), `application/usecase` / `application/port/in` / `application/command` / `application/result` (casos de uso, comandos, resultados), `configuration`.

Tecnologías verificadas:

| Componente | Versión | Origen |
|---|---|---|
| Java | 21 (pom); entorno JDK 24.0.2 | `backend/pom.xml`, `java -version` |
| Spring Boot | 4.1.0 | `backend/pom.xml` |
| Maven | 3.9.16 | ejecución real |
| Cucumber-JVM | 7.18.0 | `backend/pom.xml` |
| PostgreSQL | 18 | `database/docker-compose.yml` |
| JaCoCo | 0.8.15 | `backend/pom.xml` |
| Angular | 22.1.5 | `frontend/package.json` |
| TypeScript | 6.x | `frontend/package.json` |
| Vitest | 4.1.12 | `frontend/package.json` |
| Proveedor cobertura | v8 (`@vitest/coverage-v8`) | `frontend/package.json` |
| Playwright | 1.58.x | `frontend/package.json` |
| Node | v24.15.0, npm 11.12.1 | entorno real |

### 3.2 Autenticación y autorización

JWT (HS256) vía `POST /api/auth/login`; token `Bearer` en llamadas protegidas. `authGuard` y `chiefGuard` en Angular; la autorización se revalida en backend en cada caso de uso (`ProjectAccessPolicy.requireChief`, `AssignmentPolicy.requireCanAssign`). Usuario `is_active = false` recibe 403 al login y sus tokens son rechazados en operaciones protegidas.

### 3.3 Proceso principal y jerarquía

Flujo: Proyecto → Etapa → Actividad → Tarea → Subtarea. PP-01 verifica códigos HTTP 2xx y persistencia real en PostgreSQL mediante `JdbcTemplate` (`COUNT` por nivel con FK válidas).

### 3.4 Gestión de usuarios `app_user`

Tabla `task_manager.app_user`: `user_id` PK, `email` UK, `password_hash`, `given_names`, `family_names`, `is_chief`, `is_active`, `created_at`, `updated_at`.

| Operación | Endpoint | Estado |
|---|---|---|
| CREATE | `POST /api/users` | Preexistente, preservado |
| READ individual | `GET /api/users/{userId}` | Implementado EC4 |
| READ listado | `GET /api/users` | Implementado EC4 |
| UPDATE | `PUT /api/users/{userId}` | Implementado EC4 (solo perfil, sin mass assignment) |
| DELETE | `DELETE /api/users/{userId}` | Implementado EC4 (desactivación lógica `is_active = false`) |

Reglas: solo Jefe activo gestiona usuarios; email único y normalizado; respuesta nunca expone contraseña ni hash; eliminación física bloqueada por FK y falta de permiso `DELETE` del rol `gestor_tareas_app`.

### 3.5 Asignaciones `task_assignment`

Tabla `task_manager.task_assignment`: PK compuesta `(task_id, worker_user_id)`, `assigned_at`, `assigned_by_user_id`, `reason` (varchar 500, nullable).

| Operación | Endpoint | Estado |
|---|---|---|
| CREATE | `POST /api/tasks/{taskId}/assignments` | Preexistente, preservado |
| READ individual | `GET /api/tasks/{taskId}/assignments/{workerUserId}` | Implementado EC4 |
| READ listado | `GET /api/tasks/{taskId}/assignments` | Preexistente, preservado |
| UPDATE | `PUT /api/tasks/{taskId}/assignments/{workerUserId}` | Implementado EC4 (solo `reason`) |
| DELETE | `DELETE /api/tasks/{taskId}/assignments/{workerUserId}` | Preexistente, preservado |

Reglas: solo Jefe/Encargado asignan/revocar; trabajador activo; no autoasignación; proyecto abierto; no duplicidad (PK); motivo normalizado (trim, vacío/nulo → `DEFAULT_REASON = "Asignación de Personal"`, máximo 500); UPDATE inmutable excepto `reason`.

### 3.6 Preexistente vs EC4

Se detalla en `BASE_DOCUMENTAL_EC4.md` sección 3.6. Resumen: CREATE y listado/DELETE de asignaciones y CREATE de usuarios ya existían; el resto (GET individual/UPDATE/DELETE de usuarios, GET individual/UPDATE motivo de asignaciones, UI de ambos CRUD, archivos `.feature` y Step Definitions) se implementó en EC4.

---

## 4. Formulación de los escenarios

Matriz de 19 escenarios (códigos del plan preservados como tags en los `.feature`):

| Código | Tabla/Proceso | Operación | Tipo | Resultado esperado | Resultado real | Requisito |
|---|---|---|---|---|---|---|
| PP-01 | Proceso principal | Jerarquía | Happy | 2xx + persistencia BD | PASSED | R4, R5 |
| USR-INS-01 | app_user | INSERT | Happy | 201, sin secretos | PASSED | R4, R6 |
| USR-INS-02 | app_user | INSERT | Happy | 201 | PASSED | R4, R6 |
| USR-INS-03 | app_user | INSERT | Unhappy (email inválido) | 400, sin creación | PASSED | R4, R6 |
| USR-UPD-01 | app_user | UPDATE | Happy | 200, datos actualizados | PASSED | R4, R6 |
| USR-UPD-02 | app_user | UPDATE | Happy | 200, email normalizado | PASSED | R4, R6 |
| USR-UPD-03 | app_user | UPDATE | Unhappy (email inválido) | 400, sin cambio | PASSED | R4, R6 |
| USR-DEL-01 | app_user | DELETE | Happy | 200, inactivo, fila preservada | PASSED | R4, R6 |
| USR-SEL-01 | app_user | SELECT individual | Happy | 200, sin password_hash | PASSED | R4, R6 |
| USR-SEL-02 | app_user | SELECT listado | Happy | 200, sin secretos | PASSED | R4, R6 |
| ASG-INS-01 | task_assignment | INSERT | Happy | 200, razón persistida | PASSED | R4, R6 |
| ASG-INS-02 | task_assignment | INSERT | Happy | 200, DEFAULT_REASON | PASSED | R4, R6 |
| ASG-INS-03 | task_assignment | INSERT | Unhappy (razón >500) | 400, sin persistencia | PASSED | R4, R6 |
| ASG-UPD-01 | task_assignment | UPDATE | Happy | 200, motivo actualizado | PASSED | R4, R6 |
| ASG-UPD-02 | task_assignment | UPDATE | Happy | 200, DEFAULT_REASON | PASSED | R4, R6 |
| ASG-UPD-03 | task_assignment | UPDATE | Unhappy (razón >500) | 400, conserva previo | PASSED | R4, R6 |
| ASG-DEL-01 | task_assignment | DELETE | Happy | 204, revocada | PASSED | R4, R6 |
| ASG-SEL-01 | task_assignment | SELECT individual | Happy | 200, detalle clave compuesta | PASSED | R4, R6 |
| ASG-SEL-02 | task_assignment | SELECT listado | Happy | 200, lista de tarea | PASSED | R4, R6 |

Total: 19 ejecuciones = 14 Happy Path + 5 Unhappy Path. Cada Unhappy Path tiene un único dato incorrecto. DELETE y SELECT son exclusivamente Happy Path.

**Resultado real:** 19/19 aprobados, 123 pasos, 0 fallos, 0 omitidos. Fuente: salida Cucumber (`19 Scenarios (19 passed), 123 Steps (123 passed)`) y `backend/target/surefire-reports/TEST-...CucumberIntegrationTest.xml`. Fecha de ejecución: 26/11/2026. Entorno: local, JDK 24.0.2, Maven 3.9.16, PostgreSQL 18 `gestor_tareas_test`. Comando: `mvn test -Dtest=CucumberIntegrationTest`.

Los `.feature` reales son: `proceso-principal.feature` (`@PP-01`), `app-user-crud.feature` (`@USR` + tags por escenario), `task-assignment-crud.feature` (`@ASG` + tags por escenario). Rutas relativas desde `docs/EC4/`: `../../backend/src/test/resources/features/ec4/`.

---

## 5. Formulación de los Step Definitions

El catálogo completo de código fuente de cada método único, la matriz de trazabilidad 19 escenarios → pasos/métodos/clases/archivos, y los Hooks/Contexto/ApiClient se encuentran en `BASE_DOCUMENTAL_EC4.md` sección 5 (Formulación de los Step Definitions). A continuación se resume el mapeo por integrante y se señalan los archivos clave.

### 5.1 Arquitectura de integración (resumen)

Punto de entrada: `CucumberIntegrationTest.java` (`@Test` JUnit 5, `Main.run()`, tags `@PP-01 or @USR or @ASG`, tres archivos `.feature`). Contexto Spring: `CucumberSpringConfiguration.java` (`@SpringBootTest RANDOM_PORT`, `gestor_tareas_test`). Cliente HTTP: `ApiClient.java` (`RestTemplate`, llamadas reales). Contexto aislado: `ScenarioContext.java` (`@Component`, `reset()` en `@Before`). Limpieza y jefe por escenario: `Hooks.java`.

### 5.2 Clases de Step Definitions

| Escenarios | Clase | Archivo (relativo desde `docs/EC4/`) |
|---|---|---|
| PP-01 | `PrincipalProcessSteps` | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/PrincipalProcessSteps.java` |
| USR-INS-01..03, USR-UPD-01..03, USR-DEL-01, USR-SEL-01, USR-SEL-02 | `UserCrudSteps` | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/UserCrudSteps.java` |
| ASG-INS-01..03, ASG-UPD-01..03, ASG-DEL-01, ASG-SEL-01, ASG-SEL-02 | `AssignmentCrudSteps` | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/AssignmentCrudSteps.java` |

### 5.3 Matriz de trazabilidad escenario → método (resumen)

Cada escenario mapea a pasos concretos de las clases anteriores (código fuente completo en `BASE_DOCUMENTAL_EC4.md` sección 5.3). Ejemplo de correspondencia:

- USR-INS-01 → `i_create_a_valid_user_with_given_names_and_family_names` + `the_user_is_created_with_status_201` + `the_response_has_no_password_hash_password_or_token` + `the_user_is_stored_in_postgresql_as_active_and_non_chief` (`UserCrudSteps`).
- USR-UPD-03 → `i_update_the_user_s_email_to` + `the_response_is_400` + `the_user_s_previous_names_are_preserved_in_postgresql` (`UserCrudSteps`).
- ASG-INS-03 → `i_assign_the_worker_to_the_task_with_a_reason_of_501_characters` + verificación 400 + `no_assignment_is_stored_in_postgresql_for_that_worker` (`AssignmentCrudSteps`).
- ASG-UPD-01 → `i_update_the_assignment_reason_to` + `the_assignment_keeps_the_same_task_worker_and_audit_fields` (`AssignmentCrudSteps`).
- PP-01 → 11 pasos en `PrincipalProcessSteps` con verificación JDBC de los 5 niveles de la jerarquía.

Evidencia de integración real: no hay mocks de repositorio; cada paso atraviesa HTTP real → controlador → casos de uso → dominio → políticas → JPA/JDBC → PostgreSQL `gestor_tareas_test`, y los `@Then` de BD verifican con `JdbcTemplate`.

---

## 6. Ejecución de pruebas

### 6.1 Resultado por escenario (19 escenarios)

Idéntico a la tabla de la sección 4 (todos PASSED, 26/11/2026, comando `mvn test -Dtest=CucumberIntegrationTest`). Fuente única de verdad: `BASE_DOCUMENTAL_EC4.md` sección 6.1.

### 6.2 Resumen consolidado

| Nivel | Planificados | Ejecutados | Aprobados | Fallados | Omitidos | Pendientes / no ejecutados | Fuente |
|---|---:|---:|---:|---:|---:|---|---|
| BDD Cucumber (19 escenarios) | 19 | 19 | 19 | 0 | 0 | 0 | salida Cucumber + surefire XML |
| Backend unitarias (Surefire) | ~343 | ~343 | ~343 | 0 | 0 | 0 | `backend/target/surefire-reports/` |
| Backend integración (`PostgresIntegrationTest`) | 44 | 44 | 44 | 0 | 0 | 0 | `backend/target/surefire-reports/` |
| Backend total (`mvn verify`) | 412 | 412 | 412 | 0 | 0 | 0 | `mvn verify` BUILD SUCCESS |
| Frontend Vitest | 349 | 349 | 349 | 0 | 0 | 0 | `npx ng test --watch=false --coverage` |
| E2E Playwright Chromium | 25 | 25 | 25 | 0 | 0 | 0 | `npx playwright test --output <temp>` |

No se contó ningún escenario dos veces por ejecución local y CI: no existe run real de GitHub Actions para esta rama. El workflow `.github/workflows/ec4-quality.yml` está definido (Etapa 5) pero no se ha disparado en GitHub (ver sección 9 y `docs/EC4/CI_EC4.md`). Todos los resultados son de **validación local**.

### 6.3 Pruebas complementarias

- `PostgresIntegrationTest`: 44 pruebas de integración (CA-USR-01..22, CA-ASG-01..19), 0 fallos.
- Vitest: 23 archivos, 349 pruebas, 0 fallos.
- Playwright E2E: 6 specs, 25 pruebas (`auth` 6, `main-flow` 1, `task-assignment` 3, `validations` 3, `user-create` 4, `user-crud` 8), 0 fallos. Salida redirigida a directorio temporal para no sobrescribir `frontend/playwright-report/index.html` (archivo protegido, 20/11/2026).

---

## 7. Cobertura de pruebas

### 7.1 Herramientas y qué mide cada métrica

**Backend — JaCoCo 0.8.15** (`backend/pom.xml`): instrumenta bytecode durante `mvn verify` y ejecuta `jacoco:check`. Mide cobertura de instrucciones, líneas, ramas, métodos y complejidad sobre el código de producción ejecutado por pruebas (unitarias + integración + Cucumber).

**Frontend — Vitest 4.1.12 con proveedor v8** (`@vitest/coverage-v8`, `frontend/vitest.config.ts`): ejecuta specs de Angular y genera cobertura sobre código fuente instrumentado. Mide Statements, Branches, Functions, Lines. `@vitest/coverage-v8` es el proveedor v8; Vitest es la versión 4.x.

### 7.2 Umbrales configurados

| Nivel | Métrica | Umbral | Origen |
|---|---|---|---|
| Backend JaCoCo | LINE | ≥ 85 % | `backend/pom.xml` |
| Backend JaCoCo | BRANCH | ≥ 85 % | `backend/pom.xml` |
| Frontend Vitest/v8 | Statements | ≥ 85 % | `frontend/vitest.config.ts` |
| Frontend Vitest/v8 | Branches | ≥ 85 % | `frontend/vitest.config.ts` |
| Frontend Vitest/v8 | Functions | ≥ 85 % | `frontend/vitest.config.ts` |
| Frontend Vitest/v8 | Lines | ≥ 85 % | `frontend/vitest.config.ts` |

### 7.3 Resultados reales (fecha y SHA del reporte)

Generados en validación local del 26/11/2026 sobre la rama `Refactor-E2` (código equivalente a HEAD `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3`). `backend/target/` y `frontend/coverage/` están en `.gitignore`.

**Backend — JaCoCo (suma de `backend/target/site/jacoco/jacoco.csv`, 167 clases):**

| Métrica | Cubiertas | Total | % | Umbral | Estado |
|---|---:|---:|---:|---:|---|
| LINE | 2820 | 2964 | 95,14 % | ≥ 85 % | CUMPLE |
| BRANCH | 642 | 753 | 85,26 % | ≥ 85 % | CUMPLE |

`mvn verify` → `jacoco:check`: "All coverage checks have been met."

**Frontend — Vitest/v8 (suma de `frontend/coverage/gestor-tareas/coverage-final.json`, 28 archivos):**

| Métrica | Cubiertas | Total | % | Umbral | Estado |
|---|---:|---:|---:|---:|---|
| Statements | 3760 | 4045 | 92,95 % | ≥ 85 % | CUMPLE |
| Branches | 1127 | 1273 | 88,53 % | ≥ 85 % | CUMPLE |
| Functions | 567 | 649 | 87,37 % | ≥ 85 % | CUMPLE |
| Lines | — | — | 94,05 % | ≥ 85 % | CUMPLE |

Todas las métricas superan el umbral del 85 %.

### 7.4 Capturas de cobertura

**BLOQUEO documentado:** no fue posible crear capturas auténticas de los reportes HTML de JaCoCo ni Vitest porque las herramientas de navegador bloquean el protocolo `file://` y no se levantó un servidor HTTP local para servirlos. No existen `docs/EC4/evidencias/cobertura/jacoco-resumen.png` ni `vitest-resumen.png`. No se generaron gráficos ficticios ni imágenes decorativas.

Reportes de origen (rutas relativas desde `docs/EC4/`):
- JaCoCo: `../../backend/target/site/jacoco/index.html` (`jacoco.csv`, `jacoco.xml`)
- Vitest: `../../frontend/coverage/gestor-tareas/index.html` (`coverage-final.json`, `clover.xml`)

**Cobertura de código vs cantidad de pruebas:** la cobertura mide fracción de código de producción ejecutado, no el número de pruebas. Backend: 412 pruebas y JaCoCo LINE 95,14 % / BRANCH 85,26 %. Frontend: 349 pruebas Vitest y Statements 92,95 % / Lines 94,05 %.

---

## 8. Conclusiones y lecciones aprendidas

### 8.1 Conclusiones técnicas (hechos verificables)

- Los 19 escenarios BDD planificados se ejecutaron y aprobaron (19/19, 123 pasos, 0 fallos) como pruebas de integración reales contra API Spring Boot y PostgreSQL `gestor_tareas_test`.
- El CRUD completo de `app_user` y `task_assignment` quedó implementado en backend y frontend, sin añadir funcionalidades ajenas al sistema.
- Cobertura por encima del 85 % en backend (JaCoCo LINE 95,14 %, BRANCH 85,26 %) y frontend (Vitest Statements 92,95 %, Branches 88,53 %, Functions 87,37 %, Lines 94,05 %).
- 412 pruebas backend, 349 pruebas frontend y 25 pruebas E2E Playwright pasaron sin fallos en la validación local del 26/11/2026.
- La desactivación lógica (`is_active = false`) de usuarios fue necesaria por falta de permiso `DELETE` del rol `gestor_tareas_app` y FK con ON DELETE RESTRICT.
- No existe run real de GitHub Actions: el workflow `.github/workflows/ec4-quality.yml` está definido y validado sintácticamente, pero no se ha disparado en GitHub.

### 8.2 Lecciones aprendidas

- Cucumber-JVM es el framework BDD adecuado para este stack Java/Spring Boot; ReqnRoll es para C# y no aplica.
- Una prueba de integración real requiere servidor arrancado (`@SpringBootTest RANDOM_PORT`), HTTP real y base de datos aislada (`gestor_tareas_test`); los mocks de repositorio no demuestran integración.
- El aislamiento de datos por escenario (limpieza de BD en `@Before`, emails únicos) evita filtraciones entre escenarios.
- La cobertura de código es útil pero insuficiente por sí sola: cumplir umbrales no garantiza que se hayan probado todos los requisitos de negocio.
- Los guards del frontend no sustituyen la autorización en backend; cada caso de uso revalida permisos y reglas de dominio.

### 8.3 Reflexiones personales

**[REFLEXIÓN PERSONAL A VALIDAR POR CADA INTEGRANTE]** Bloque reservado para que cada integrante registre su experiencia subjetiva durante el EC4 (dificultades, resoluciones, trabajo en equipo, limitaciones pendientes). No hay evidencia objetiva en el historial para atribuir experiencias individuales; cada integrante debe completar y validar este apartado antes de la entrega.

### 8.4 Limitaciones pendientes

- Capturas de pantalla de reportes de cobertura (bloqueo `file://`, sección 7.4).
- Pipeline de GitHub Actions definido pero no ejecutado en GitHub (sin run real verificable).
- Validación de navegador real vía Playwright MCP no disponible en esta sesión (bitacora Etapa 3, BLOCKED); las pruebas E2E sí se ejecutaron con Playwright Test (25/25).
- Datos de prueba acumulados en `gestor_tareas_test` (emails únicos, sin colisiones; limpieza manual opcional).

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
| Modificaciones de producto | Ninguna (solo documentación `.md` creada en esta etapa) |

### 9.2 Enlaces a archivos fuente (relativos desde `docs/EC4/`)

Véase `BASE_DOCUMENTAL_EC4.md` sección 9.2 (Feature, Step Definitions, Hooks, ApiClient, ScenarioContext, CucumberIntegrationTest, CucumberSpringConfiguration, Workflow CI, Migraciones, README).

### 9.3 Enlaces a reportes de ejecución y cobertura

Véase `BASE_DOCUMENTAL_EC4.md` sección 9.3 (JaCoCo, Surefire, Cucumber, Vitest, Playwright histórico). Los reportes de `target/` y `coverage/` son artefactos locales del 26/11/2026, no versionados en Git (`.gitignore`). No existen links a runs de GitHub Actions porque el workflow no se ha disparado.

### 9.4 Documentación relacionada (docs/EC4/)

- `BASE_DOCUMENTAL_EC4.md` — documento canónico maestro.
- `BASELINE_EC4.md` — auditoría, línea base e infraestructura BDD (Etapas 0 y 1).
- `IMPLEMENTACION_EC4_BITACORA.md` — decisiones, cambios y resultados por etapa (Etapas 1 a 5).
- `REPORTE_REGRESION_EC4.md` — resultados de integración y regresión (Etapa 4).
- `CI_EC4.md` — configuración del pipeline GitHub Actions (Etapa 5).
- `informes/INFORME_EC4_INTEGRANTE_{1,2,3}.md` — informes individuales.
- `evidencias/README.md` — índice de evidencias.

### 9.5 Glosario

Véase `BASE_DOCUMENTAL_EC4.md` sección 9.5.

### 9.6 Referencias técnicas de apoyo

Véase `BASE_DOCUMENTAL_EC4.md` sección 9.6 (referencias industriales, no académicas).

### 9.7 Preguntas para la defensa (dos por integrante)

Véase `BASE_DOCUMENTAL_EC4.md` sección 9.7 (Integrante 1, 2 y 3, con respuestas basadas en código y evidencias reales).

---
*Fin del INFORME FINAL EC4.*
