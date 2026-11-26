# INFORME EC4 — INTEGRANTE 3

**Elemento de Competencia 4 — BDD + Gherkin + CRUD Full-Stack + Pruebas de Integración + Cobertura de Pruebas + CI/CD**  
**Informe individual — Integrante 3**  
**Responsabilidad asignada:** CRUD full-stack `task_assignment` (API/backend de asignaciones, detalle y actualización del motivo en el diálogo de tareas, 9 escenarios BDD de asignaciones y sus Step Definitions, Vitest y Playwright de asignaciones, clave compuesta, validaciones de motivo y reglas de autorización)  
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
| Entregable | Elemento de Competencia 4 (EC4) — Informe individual (Integrante 3) |
| Tema | BDD + Pruebas de Integración + Cobertura de Pruebas |
| Integrante | Integrante 3 (CRUD full-stack `task_assignment`) |
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

El Gestor de Tareas Personales organiza el trabajo de un equipo a través de una jerarquía **Proyecto → Etapa → Actividad → Tarea → Subtarea**, con gestión de usuarios (`app_user`) y asignación de trabajadores a tareas (`task_assignment`), autenticación JWT y control de acceso por roles (Jefe / Encargado / trabajador).

### 2.2 Contexto del sistema preexistente al EC4

Antes del EC4 el sistema ya existía con arquitectura hexagonal backend (Java 21, Spring Boot 4.1.0, Maven, Spring Security + JWT, JPA/Hibernate, PostgreSQL 18) y SPA Angular 22.1. La tabla `task_assignment` estaba modelada (migraciones V004 y V006) con implementación parcial: `POST` (creación), `GET` listado y `DELETE` (revocación) de asignaciones, con diálogo de asignación en la tarea. Faltaban consulta individual por clave compuesta y actualización del motivo, tanto en backend como en frontend.

### 2.3 Objetivo del trabajo de calidad y automatización (Integrante 3)

Responsabilidad del Integrante 3: completar el CRUD full-stack de `task_assignment` en backend (endpoints GET individual por clave compuesta y PUT de motivo, manteniendo CREATE, listado y DELETE existentes), UI Angular (detalle, edición de motivo, revocación con confirmación dentro del diálogo de tareas), los 9 escenarios BDD de asignaciones y sus Step Definitions, y las pruebas Vitest y Playwright de asignaciones, incluyendo clave compuesta, validaciones de motivo y reglas de autorización.

### 2.4 Qué se probó y por qué

Se probaron los 9 escenarios BDD de `task_assignment` (ASG-INS-01..03, ASG-UPD-01..03, ASG-DEL-01, ASG-SEL-01, ASG-SEL-02) como pruebas de integración reales: HTTP contra el servidor Spring Boot en puerto aleatorio y verificación de persistencia en `gestor_tareas_test`, sin mocks de repositorio. Se priorizó la clave compuesta `(task_id, worker_user_id)`, la normalización del motivo (`DEFAULT_REASON = "Asignación de Personal"`), el límite de 500 caracteres y las reglas de autorización (solo Jefe/Encargado del proyecto).

### 2.5 Alcance concreto del EC4 (enfoque Integrante 3)

- CRUD `task_assignment`: INSERT (2 Happy + 1 Unhappy motivo > 500), UPDATE (2 Happy motivo + 1 Unhappy motivo > 500), DELETE (1 Happy revocación), SELECT individual por clave compuesta (1 Happy), SELECT listado de tarea (1 Happy).
- Endpoints backend: `GET /api/tasks/{taskId}/assignments/{workerUserId}` y `PUT /api/tasks/{taskId}/assignments/{workerUserId}` (CREATE, listado y DELETE ya existían).
- UI Angular: `AssignmentDialog` con detalle de asignación, edición de motivo, confirmación de revocación, estados saving/deleting, prevención doble envío, accesibilidad.
- Servicios Angular: `wbs.service.ts` con `getAssignment` y `updateAssignment`.
- Pruebas Vitest: `assignment-dialog.spec.ts` (detalle, editar motivo, cancelar, confirmar/revocar, errores, estados), `wbs.service.spec.ts` (`getAssignment`, `updateAssignment`).
- Pruebas Playwright E2E: `task-assignment.spec.ts` (3 tests: flujo completo, motivo vacío default, 403 actor no autorizado).

### 2.6 Límites de alcance

No se añadieron funcionalidades de negocio ajenas. No se cambió la PK compuesta ni el schema. No se modificaron campos de auditoría (`assigned_at`, `assigned_by_user_id`) en el UPDATE (solo `reason` mutable).

---

## 3. Desarrollo del sistema: descripción de las funcionalidades

### 3.1 Arquitectura general y tecnologías verificadas (relevantes para el Integrante 3)

Backend hexagonal: `adapter/in/web/task/HierarchyController.java`, `adapter/out/persistence/AssignmentPersistenceAdapter.java`, `application/usecase/AssignmentCommandService.java`, `application/port/in/ManageAssignmentsUseCase.java`, `domain/port/out/AssignmentRepository.java`, `domain/model/TaskAssignment.java`, `domain/policy/AssignmentPolicy.java`, `domain/policy/WbsHierarchyPolicy.java`.

Frontend: `frontend/src/app/core/services/wbs.service.ts`, `frontend/src/app/core/models/api.ts`, `frontend/src/app/features/tasks/assignment-dialog/assignment-dialog.ts`, `assignment-dialog.spec.ts`.

Tecnologías: Java 21 / Spring Boot 4.1.0 / Maven, Angular 22.1.5 / TypeScript 6 / Vitest 4.1.12 / Playwright 1.58.x, PostgreSQL 18.

### 3.2 Autenticación y autorización (Integrante 3)

JWT (HS256) vía `POST /api/auth/login`; token `Bearer` en llamadas protegidas. Solo Jefe o Encargado del proyecto asignan/revocar (`AssignmentPolicy.requireCanAssign`). Trabajador debe existir y estar activo; no autoasignación (`requireNotSelf`); proyecto abierto (`WbsHierarchyPolicy.requireOpen`). La autorización se revalida en backend en cada caso de uso.

### 3.3 Asignaciones `task_assignment` (responsabilidad Integrante 3)

Tabla `task_manager.task_assignment`: PK compuesta `(task_id, worker_user_id)`, `assigned_at` (default now), `assigned_by_user_id`, `reason` (varchar 500, nullable, check no en blanco si tiene valor). FK: `task_id → task.task_id` (ON DELETE CASCADE), `worker_user_id → app_user.user_id` (ON DELETE RESTRICT), `assigned_by_user_id → app_user.user_id` (ON DELETE RESTRICT).

Operaciones CRUD disponibles al finalizar el EC4:

| Operación | Endpoint | Estado | Nota |
|---|---|---|---|
| CREATE | `POST /api/tasks/{taskId}/assignments` | Preexistente, preservado | 200, Jefe/Encargado |
| READ individual | `GET /api/tasks/{taskId}/assignments/{workerUserId}` | Implementado EC4 | 200, clave compuesta |
| READ listado | `GET /api/tasks/{taskId}/assignments` | Preexistente, preservado | 200 |
| UPDATE | `PUT /api/tasks/{taskId}/assignments/{workerUserId}` | Implementado EC4 | 200, solo `reason` |
| DELETE | `DELETE /api/tasks/{taskId}/assignments/{workerUserId}` | Preexistente, preservado | 204, revocación |

**Reglas de negocio:** clave compuesta `(task_id, worker_user_id)`; solo Jefe o Encargado asignan/revocar; trabajador activo; no autoasignación; proyecto abierto; no duplicidad (PK compuesta, rechazo 409 por restricción de BD); motivo normalizado (trim, vacío/nulo → `DEFAULT_REASON = "Asignación de Personal"`, máximo 500); el UPDATE modifica únicamente `reason` (inmutables: `task_id`, `worker_user_id`, `assigned_at`, `assigned_by_user_id`).

### 3.4 DTOs (Integrante 3)

- `TaskAssignmentResponse`: `taskId`, `workerUserId`, `workerName`, `assignedAt`, `assignedByUserId`, `assignedByName`, `reason`. Reusado para GET individual y PUT.
- `UpdateAssignmentReasonRequest`: solo `reason` (opcional, `@Size(max=500)`). Actualización parcial; ningún otro campo cambia.
- `AssignWorkerRequest`: `workerUserId` (`@NotNull`) y `reason` (`@Size(max=500)`).

### 3.5 Errores controlados

Vía `GlobalExceptionHandler`: 400 (validación `@Size`/`@NotNull`), 403 (no autorizado), 404 (tarea o asignación inexistente), 409 (duplicidad PK compuesta). Nunca stack traces ni SQL.

### 3.6 Dominio: `TaskAssignment.normalizeReason()`

Lógica única del dominio (no duplicada en frontend ni controlador): `trim`, null/vacío → `DEFAULT_REASON ("Asignación de Personal")`, longitud máxima 500. `REASON_MAX = 500`.

### 3.7 Funcionalidades preexistentes vs implementadas en EC4

| Funcionalidad | Preexistente | Implementada en EC4 (Integrante 3) |
|---|---|---|
| `POST /api/tasks/{taskId}/assignments` | Sí | No (preservada) |
| `GET /api/tasks/{taskId}/assignments` (listado) | Sí | No (preservada) |
| `GET /api/tasks/{taskId}/assignments/{workerUserId}` (individual) | No | Sí |
| `PUT /api/tasks/{taskId}/assignments/{workerUserId}` (motivo) | No | Sí (solo `reason`) |
| `DELETE /api/tasks/{taskId}/assignments/{workerUserId}` | Sí | No (preservada) |
| UI detalle de asignación | No | Sí |
| UI edición de motivo | No | Sí |
| UI revocación con confirmación | Parcial (botón "Retirar") | Completado (confirmación) |
| Servicios Angular `getAssignment` / `updateAssignment` | No | Sí |

---

## 4. Formulación de los escenarios

Matriz completa de 19 escenarios (responsabilidad Integrante 3 destacada en `task_assignment`; todos los escenarios incluidos por requisito del Anexo 2):

| Código | Tabla / Proceso | Operación | Tipo | Responsable | Resultado esperado | Resultado real | Requisito |
|---|---|---|---|---|---|---|---|
| PP-01 | Proceso principal | Jerarquía | Happy | Integrante 1 | 2xx + persistencia BD | PASSED | R4, R5 |
| USR-INS-01 | app_user | INSERT | Happy | Integrante 2 | 201, creado sin secretos | PASSED | R4, R6 |
| USR-INS-02 | app_user | INSERT | Happy | Integrante 2 | 201, creado | PASSED | R4, R6 |
| USR-INS-03 | app_user | INSERT | Unhappy | Integrante 2 | 400, sin creación | PASSED | R4, R6 |
| USR-UPD-01 | app_user | UPDATE | Happy | Integrante 2 | 200, datos actualizados | PASSED | R4, R6 |
| USR-UPD-02 | app_user | UPDATE | Happy | Integrante 2 | 200, email normalizado | PASSED | R4, R6 |
| USR-UPD-03 | app_user | UPDATE | Unhappy | Integrante 2 | 400, sin cambio | PASSED | R4, R6 |
| USR-DEL-01 | app_user | DELETE | Happy | Integrante 2 | 200, inactivo, fila preservada | PASSED | R4, R6 |
| USR-SEL-01 | app_user | SELECT individual | Happy | Integrante 2 | 200, sin `password_hash` | PASSED | R4, R6 |
| USR-SEL-02 | app_user | SELECT listado | Happy | Integrante 2 | 200, lista sin secretos | PASSED | R4, R6 |
| ASG-INS-01 | task_assignment | INSERT | Happy | Integrante 3 | 200, razón persistida | PASSED | R4, R6 |
| ASG-INS-02 | task_assignment | INSERT | Happy | Integrante 3 | 200, DEFAULT_REASON | PASSED | R4, R6 |
| ASG-INS-03 | task_assignment | INSERT | Unhappy | Integrante 3 | 400, sin persistencia | PASSED | R4, R6 |
| ASG-UPD-01 | task_assignment | UPDATE | Happy | Integrante 3 | 200, motivo actualizado | PASSED | R4, R6 |
| ASG-UPD-02 | task_assignment | UPDATE | Happy | Integrante 3 | 200, DEFAULT_REASON | PASSED | R4, R6 |
| ASG-UPD-03 | task_assignment | UPDATE | Unhappy | Integrante 3 | 400, conserva previo | PASSED | R4, R6 |
| ASG-DEL-01 | task_assignment | DELETE | Happy | Integrante 3 | 204, revocada | PASSED | R4, R6 |
| ASG-SEL-01 | task_assignment | SELECT individual | Happy | Integrante 3 | 200, clave compuesta | PASSED | R4, R6 |
| ASG-SEL-02 | task_assignment | SELECT listado | Happy | Integrante 3 | 200, lista de tarea | PASSED | R4, R6 |

Total: 19 ejecuciones = 14 Happy Path + 5 Unhappy Path. Resultado real: 19/19 aprobados, 123 pasos, 0 fallos, 0 omitidos. Fuente: salida Cucumber y `backend/target/surefire-reports/TEST-...CucumberIntegrationTest.xml`. Fecha: 26/11/2026. Comando: `mvn test -Dtest=CucumberIntegrationTest`.

Enfoque Integrante 3 (9 escenarios `task_assignment`): ASG-INS-01..03, ASG-UPD-01..03, ASG-DEL-01, ASG-SEL-01, ASG-SEL-02 — todos PASSED.

Archivos `.feature` reales (rutas desde `docs/EC4/informes/`):
- Proceso principal: `../../../backend/src/test/resources/features/ec4/proceso-principal.feature` (`@PP-01`)
- CRUD `app_user`: `../../../backend/src/test/resources/features/ec4/app-user-crud.feature` (`@USR`)
- CRUD `task_assignment`: `../../../backend/src/test/resources/features/ec4/task-assignment-crud.feature` (`@ASG`)

---

## 5. Formulación de los Step Definitions (Integrante 3)

### 5.1 Clase de Step Definitions: `AssignmentCrudSteps.java`

Ruta desde `docs/EC4/informes/`: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/AssignmentCrudSteps.java`

Todas las llamadas son HTTP reales contra la API del backend y la verificación de persistencia se hace contra PostgreSQL de integración (`gestor_tareas_test`). No hay mocks de repositorio.

Métodos únicos (código fuente completo en `BASE_DOCUMENTAL_EC4.md` sección 5.3.3):

| Paso Gherkin | Método | Escenarios que lo usan |
|---|---|---|
| `a task exists for the authenticated chief's project` | `a_task_exists_for_the_authenticated_chiefs_project()` | Todos ASG (Background) |
| `an active worker exists` | `an_active_worker_exists()` | ASG-INS-01..03, ASG-UPD-01..03, ASG-DEL-01, ASG-SEL-01 |
| `two active workers exist` | `two_active_workers_exist()` | ASG-SEL-02 |
| `the worker is assigned to the task` | `the_worker_is_assigned_to_the_task()` | ASG-DEL-01 |
| `the worker is assigned to the task with reason {string}` | `the_worker_is_assigned_to_the_task_with_reason` | ASG-UPD-01..03, ASG-SEL-01 |
| `both workers are assigned to the task` | `both_workers_are_assigned_to_the_task()` | ASG-SEL-02 |
| `I assign the worker to the task with reason {string}` | `i_assign_the_worker_to_the_task_with_reason` | ASG-INS-01 |
| `I assign the worker to the task without a reason` | `i_assign_the_worker_to_the_task_without_a_reason` | ASG-INS-02 |
| `I assign the worker to the task with a reason of 501 characters` | `i_assign_the_worker_to_the_task_with_a_reason_of_501_characters` | ASG-INS-03 |
| `I update the assignment reason to {string}` | `i_update_the_assignment_reason_to` | ASG-UPD-01 |
| `I update the assignment reason to an empty value` | `i_update_the_assignment_reason_to_an_empty_value` | ASG-UPD-02 |
| `I update the assignment reason to a value of {int} characters` | `i_update_the_assignment_reason_to_a_value_of_characters` | ASG-UPD-03 |
| `I revoke the worker's assignment from the task` | `i_revoke_the_workers_assignment_from_the_task` | ASG-DEL-01 |
| `I consult the assignment by task and worker` | `i_consult_the_assignment_by_task_and_worker` | ASG-SEL-01 |
| `I list assignments of the task` | `i_list_assignments_of_the_task` | ASG-SEL-02 |

Then helpers reutilizados: `the_assignment_is_created_with_status_200`, `the_response_reason_is_normalized_to`, `the_response_reason_is_the_default`, `the_response_reason_is`, `the_assignment_is_stored_in_postgresql_with_the_normalized_reason`, `the_assignment_is_stored_in_postgresql_with_the_default_reason`, `no_assignment_is_stored_in_postgresql_for_that_worker`, `the_assignment_keeps_the_same_task_worker_and_audit_fields`, `the_assignment_still_has_reason_in_postgresql`, `the_response_is_204`, `the_assignment_no_longer_exists_in_postgresql`, `the_response_shows_the_worker_task_and_reason`, `the_list_contains_both_assignments`.

Ejemplo representativo (`i_assign_the_worker_to_the_task_without_a_reason`): usa `LinkedHashMap` con `body.put("reason", null)` (evita NPE de `Map.of` con null), llama `apiClient.postAssignment`, guarda respuesta en `ScenarioContext`.

Ejemplo (`the_assignment_keeps_the_same_task_worker_and_audit_fields`): afirma que `taskId` y `workerUserId` del cuerpo de respuesta coinciden con los del contexto, y que `assignedAt` y `assignedByUserId` no son nulos (inmutabilidad de auditoría).

### 5.2 Hooks, contexto y helpers (compartidos, responsabilidad Integrante 1)

- `Hooks.java`: limpia BD en `@Before` (incluye `task_assignment` primero, orden inverso de FK), crea jefe activo con email único, login JWT, `context.reset()`. Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/hooks/Hooks.java`.
- `ScenarioContext.java`: estado aislado (`workerUserId`, `taskId`, respuestas HTTP). Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/context/ScenarioContext.java`.
- `ApiClient.java`: `postAssignment`, `listAssignments`, `getAssignment`, `putAssignmentReason`, `deleteAssignment` con manejo de errores 4xx/5xx. Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/support/ApiClient.java`.

### 5.3 Evidencia de integración real (no prueba unitaria disfrazada)

Cada paso ASG atraviesa HTTP real → `HierarchyController` → `ManageAssignmentsUseCase` → `AssignmentCommandService` → `AssignmentPersistenceAdapter` → JPA → PostgreSQL `gestor_tareas_test`. Los `@Then` de BD verifican con `JdbcTemplate` (`reason` en `task_assignment`, conteo de filas, ausencia de persistencia en Unhappy Path). La inmutabilidad de campos de auditoría se verifica en `the_assignment_keeps_the_same_task_worker_and_audit_fields`.

---

## 6. Ejecución de pruebas (enfoque Integrante 3)

### 6.1 Resultado por escenario ASG (9 escenarios)

| # | Código | Nombre del escenario | Tipo | Resultado real | Fecha | Comando | Código HTTP | Fuente | Observaciones |
|---|---|---|---|---|---|---|---|---|---|
| 1 | ASG-INS-01 | Create an assignment with explicit reason (Happy Path) | Happy | PASSED | 26/11/2026 | `mvn test -Dtest=CucumberIntegrationTest` | 200 | salida Cucumber + surefire XML | razón normalizada |
| 2 | ASG-INS-02 | Create an assignment without reason uses default (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 200 | idem | `DEFAULT_REASON` |
| 3 | ASG-INS-03 | Reject assignment with reason over 500 characters (Unhappy Path) | Unhappy | PASSED | 26/11/2026 | idem | 400 | idem | 501 caracteres, sin persistencia |
| 4 | ASG-UPD-01 | Update the reason of an existing assignment (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 200 | idem | |
| 5 | ASG-UPD-02 | Update assignment reason to empty uses default (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 200 | idem | `DEFAULT_REASON` |
| 6 | ASG-UPD-03 | Reject reason over 500 characters on update and preserve previous reason (Unhappy Path) | Unhappy | PASSED | 26/11/2026 | idem | 400 | idem | conserva motivo previo |
| 7 | ASG-DEL-01 | Revoke an existing assignment (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 204 | idem | revocación |
| 8 | ASG-SEL-01 | Consult an individual assignment by composite key (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 200 | idem | clave compuesta |
| 9 | ASG-SEL-02 | List assignments of a task (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 200 | idem | lista de ambas asignaciones |

Resultado ASG: 9/9 aprobados, 0 fallos, 0 omitidos.

### 6.2 Pruebas complementarias Vitest (Integrante 3)

| Espec | Pruebas | Resultado | Fuente |
|---|---:|---|---|
| `assignment-dialog.spec.ts` | detalle, editar motivo, cancelar editar, confirmar/revocar, cancelar revocación, error al guardar, error al revocar, sin edición previa, no revoca sin confirmación | PASSED | `frontend/coverage/gestor-tareas/` + salida Vitest |
| `wbs.service.spec.ts` | `getAssignment` (GET individual), `updateAssignment` (PUT con reason y con null) | PASSED | idem |

Total Vitest (todas las specs, incluyendo las de Integrante 2): 23 archivos, 349 pruebas, 0 fallos.

### 6.3 Pruebas complementarias Playwright E2E (Integrante 3)

| Spec | Pruebas | Resultado | Fuente |
|---|---:|---|---|
| `e2e/tasks/task-assignment.spec.ts` | 3 tests (flujo completo crear/listar/detalle/editar/revocación, motivo vacío default, 403 actor no autorizado) | 3 PASSED | salida Playwright (directorio temporal) |

Total E2E asignaciones: 3 pruebas, 0 fallos.

### 6.4 Resumen consolidado (enfoque Integrante 3)

| Nivel | Planificados | Ejecutados | Aprobados | Fallados | Omitidos | Pendientes / no ejecutados |
|---|---:|---:|---:|---:|---:|---|
| BDD ASG (9 escenarios) | 9 | 9 | 9 | 0 | 0 | 0 |
| Vitest asignaciones (2 specs) | — | — | PASSED | 0 | 0 | — |
| Playwright asignaciones (1 spec) | 3 | 3 | 3 | 0 | 0 | 0 |

No se contó ningún escenario dos veces por ejecución local y CI: no existe run real de GitHub Actions para esta rama.

---

## 7. Cobertura de pruebas (enfoque Integrante 3)

### 7.1 Herramientas

**Backend — JaCoCo 0.8.15** (`backend/pom.xml`): umbrales LINE ≥ 85 %, BRANCH ≥ 85 %.  
**Frontend — Vitest 4.1.12 con proveedor v8** (`@vitest/coverage-v8`, `frontend/vitest.config.ts`): umbrales Statements/Branches/Functions/Lines ≥ 85 %. `@vitest/coverage-v8` es el proveedor v8; Vitest es la versión 4.x.

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

### 7.4 Capturas de cobertura

**BLOQUEO documentado:** no fue posible crear capturas auténticas de los reportes HTML de JaCoCo ni Vitest porque las herramientas de navegador bloquean el protocolo `file://` y no se levantó un servidor HTTP local para servirlos. No existen `docs/EC4/evidencias/cobertura/jacoco-resumen.png` ni `vitest-resumen.png`. No se generaron gráficos ficticios ni imágenes decorativas.

Reportes de origen (rutas desde `docs/EC4/informes/`): `../../../backend/target/site/jacoco/index.html` y `../../../frontend/coverage/gestor-tareas/index.html`.

**Cobertura de código vs cantidad de pruebas:** la cobertura mide fracción de código de producción ejecutado, no el número de pruebas. Backend: 412 pruebas y JaCoCo LINE 95,14 % / BRANCH 85,26 %. Frontend: 349 pruebas Vitest y Statements 92,95 % / Lines 94,05 %.

---

## 8. Conclusiones y lecciones aprendidas (Integrante 3)

### 8.1 Conclusiones técnicas (hechos verificables)

- Los 9 escenarios BDD de `task_assignment` se ejecutaron y aprobaron (9/9, 0 fallos) como pruebas de integración reales contra API Spring Boot y PostgreSQL `gestor_tareas_test`.
- El CRUD completo de `task_assignment` quedó implementado en backend y frontend, sin añadir funcionalidades ajenas al sistema.
- La clave compuesta `(task_id, worker_user_id)` se respeta en todas las operaciones (consulta individual por clave compuesta, rechazo de duplicidad 409).
- El `PUT` de asignación modifica únicamente `reason`; `task_id`, `worker_user_id`, `assigned_at`, `assigned_by_user_id` son inmutables (verificado en `the_assignment_keeps_the_same_task_worker_and_audit_fields` y `CA-ASG-15`).
- El motivo se normaliza: trim, null/vacío → `DEFAULT_REASON ("Asignación de Personal")`, máximo 500 caracteres (rechazo 400 si excede).
- Reglas de autorización aplicadas: solo Jefe/Encargado del proyecto, trabajador activo, no autoasignación, proyecto abierto.
- Cobertura por encima del 85 % en backend (JaCoCo LINE 95,14 %, BRANCH 85,26 %) y frontend (Vitest Statements 92,95 %, Branches 88,53 %, Functions 87,37 %, Lines 94,05 %).

### 8.2 Lecciones aprendidas

- La lógica de dominio (`TaskAssignment.normalizeReason`) debe vivir en un solo lugar para no duplicarse en frontend ni controlador.
- Los campos de auditoría (`assigned_at`, `assigned_by_user_id`) no deben exponerse para edición en el `PUT` de motivo.
- La clave compuesta requiere validación tanto en BD (PK) como en aplicación (409 por duplicidad).

### 8.3 Reflexiones personales

**[REFLEXIÓN PERSONAL A VALIDAR POR EL INTEGRANTE 3]** Bloque reservado para la experiencia subjetiva del Integrante 3 durante el EC4 (dificultades, resoluciones, trabajo en equipo, limitaciones pendientes). No hay evidencia objetiva en el historial para atribuirla.

### 8.4 Limitaciones pendientes

- Capturas de pantalla de reportes de cobertura (bloqueo `file://`, sección 7.4).
- Pipeline de GitHub Actions definido pero no ejecutado en GitHub (sin run real verificable).
- Validación de navegador real vía Playwright MCP no disponible; las pruebas E2E sí se ejecutaron con Playwright Test (3/3 para asignaciones).

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
| Feature CRUD `task_assignment` | `../../../backend/src/test/resources/features/ec4/task-assignment-crud.feature` |
| Step Definitions `task_assignment` | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/AssignmentCrudSteps.java` |
| Hooks | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/hooks/Hooks.java` |
| ScenarioContext | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/context/ScenarioContext.java` |
| ApiClient | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/support/ApiClient.java` |
| CucumberIntegrationTest | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/CucumberIntegrationTest.java` |
| HierarchyController | `../../../backend/src/main/java/com/hardcoders/taskmanager/adapter/in/web/task/HierarchyController.java` |
| AssignmentCommandService | `../../../backend/src/main/java/com/hardcoders/taskmanager/application/usecase/AssignmentCommandService.java` |
| ManageAssignmentsUseCase | `../../../backend/src/main/java/com/hardcoders/taskmanager/application/port/in/ManageAssignmentsUseCase.java` |
| AssignmentRepository | `../../../backend/src/main/java/com/hardcoders/taskmanager/domain/port/out/AssignmentRepository.java` |
| AssignmentPersistenceAdapter | `../../../backend/src/main/java/com/hardcoders/taskmanager/adapter/out/persistence/AssignmentPersistenceAdapter.java` |
| TaskAssignment (dominio) | `../../../backend/src/main/java/com/hardcoders/taskmanager/domain/model/TaskAssignment.java` |
| AssignmentPolicy | `../../../backend/src/main/java/com/hardcoders/taskmanager/domain/policy/AssignmentPolicy.java` |
| WbsHierarchyPolicy | `../../../backend/src/main/java/com/hardcoders/taskmanager/domain/policy/WbsHierarchyPolicy.java` |
| wbs.service.ts | `../../../frontend/src/app/core/services/wbs.service.ts` |
| wbs.service.spec.ts | `../../../frontend/src/app/core/services/wbs.service.spec.ts` |
| api.ts (modelo) | `../../../frontend/src/app/core/models/api.ts` |
| assignment-dialog.ts | `../../../frontend/src/app/features/tasks/assignment-dialog/assignment-dialog.ts` |
| assignment-dialog.spec.ts | `../../../frontend/src/app/features/tasks/assignment-dialog/assignment-dialog.spec.ts` |
| task-assignment.spec.ts (E2E) | `../../../frontend/e2e/tasks/task-assignment.spec.ts` |
| Workflow CI | `../../../.github/workflows/ec4-quality.yml` |
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

Véase `BASE_DOCUMENTAL_EC4.md` sección 9.

### 9.5 Glosario

Véase `BASE_DOCUMENTAL_EC4.md` sección 9.5.

### 9.6 Referencias técnicas de apoyo

Véase `BASE_DOCUMENTAL_EC4.md` sección 9.6.

### 9.7 Preguntas para la defensa (Integrante 3)

1. P: ¿Cómo funciona la clave compuesta de `task_assignment` y qué operación se actualiza?  
   R: La PK es `(task_id, worker_user_id)`. El `PUT` actualiza únicamente el campo `reason` (máximo 500, normalizado, valor predeterminado `Asignación de Personal`); no se modifica tarea, trabajador, fecha ni asignador. Soporte: `AssignmentCrudSteps.the_assignment_keeps_the_same_task_worker_and_audit_fields`, `TaskAssignment.normalizeReason`, `AssignmentPersistenceAdapter.updateReason`, escenarios ASG-SEL-01 / ASG-UPD-01..03.

2. P: ¿Qué reglas de autorización aplican a las asignaciones?  
   R: Solo el Encargado del proyecto o el Jefe pueden asignar/revocar (`AssignmentPolicy.requireCanAssign`); el trabajador debe existir y estar activo; no se permite autoasignación (`requireNotSelf`); el proyecto debe estar abierto (`WbsHierarchyPolicy.requireOpen`). Soporte: `AssignmentPolicy`, `ManageAssignmentsUseCase`, escenario ASG-INS-01 y E2E 403 para actor no autorizado.

---
*Fin del INFORME EC4 — INTEGRANTE 3.*
