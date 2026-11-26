# INFORME EC4 — INTEGRANTE 2

**Elemento de Competencia 4 — BDD + Gherkin + CRUD Full-Stack + Pruebas de Integración + Cobertura de Pruebas + CI/CD**  
**Informe individual — Integrante 2**  
**Responsabilidad asignada:** CRUD full-stack `app_user` (API/backend de usuarios, UI Angular: listado, alta, detalle, edición, desactivación y rutas, 9 escenarios BDD de usuarios y sus Step Definitions, Vitest y Playwright de usuarios, seguridad, autorización, privacidad e integridad referencial)  
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
| Entregable | Elemento de Competencia 4 (EC4) — Informe individual (Integrante 2) |
| Tema | BDD + Pruebas de Integración + Cobertura de Pruebas |
| Integrante | Integrante 2 (CRUD full-stack `app_user`) |
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

Antes del EC4 el sistema ya existía con arquitectura hexagonal backend (Java 21, Spring Boot 4.1.0, Maven, Spring Security + JWT, JPA/Hibernate, PostgreSQL 18) y SPA Angular 22.1. La tabla `app_user` estaba modelada (migración V004) con implementación parcial: solo existía `POST /api/users` (creación) y la UI de creación en `/users/new`. Faltaban consulta individual, listado, actualización y desactivación de usuarios, tanto en backend como en frontend.

### 2.3 Objetivo del trabajo de calidad y automatización (Integrante 2)

Responsabilidad del Integrante 2: completar el CRUD full-stack de `app_user` en backend (endpoints GET individual, GET listado, PUT, DELETE manteniendo el CREATE existente), UI Angular (listado, detalle, edición, desactivación, rutas protegidas), los 9 escenarios BDD de usuarios y sus Step Definitions, y las pruebas Vitest y Playwright de usuarios, incluyendo seguridad, autorización, privacidad e integridad referencial.

### 2.4 Qué se probó y por qué

Se probaron los 9 escenarios BDD de `app_user` (USR-INS-01..03, USR-UPD-01..03, USR-DEL-01, USR-SEL-01, USR-SEL-02) como pruebas de integración reales: HTTP contra el servidor Spring Boot en puerto aleatorio y verificación de persistencia en `gestor_tareas_test`, sin mocks de repositorio. Se priorizó la privacidad (ninguna respuesta expone `password_hash`), la autorización (solo Jefe activo) y la integridad referencial (desactivación lógica por FK ON DELETE RESTRICT).

### 2.5 Alcance concreto del EC4 (enfoque Integrante 2)

- CRUD `app_user`: INSERT (2 Happy + 1 Unhappy email inválido), UPDATE (2 Happy nombres/email + 1 Unhappy email inválido), DELETE (1 Happy desactivación lógica), SELECT individual (1 Happy sin secretos), SELECT listado (1 Happy sin secretos).
- Endpoints backend: `GET /api/users`, `GET /api/users/{userId}`, `PUT /api/users/{userId}`, `DELETE /api/users/{userId}` (CREATE ya existía).
- UI Angular: `/users` (listado), `/users/:userId` (detalle), `/users/:userId/edit` (edición), reutilización de `/users/new` (creación), guards `authGuard` + `chiefGuard`.
- Servicios Angular: `user.service.ts` con `list`, `get`, `create`, `update`, `deactivate`.
- Pruebas Vitest: `user.service.spec.ts`, `user-list.spec.ts`, `user-edit.spec.ts`.
- Pruebas Playwright E2E: `user-crud.spec.ts` (8 tests).

### 2.6 Límites de alcance

No se añadieron funcionalidades de negocio ajenas. No se creó la tabla `app_user` (ya existía en V004). No se permitió eliminación física (bloqueada por FK y falta de permiso DELETE del rol `gestor_tareas_app`); se usó desactivación lógica.

---

## 3. Desarrollo del sistema: descripción de las funcionalidades

### 3.1 Arquitectura general y tecnologías verificadas (relevantes para el Integrante 2)

Backend hexagonal: `adapter/in/web/user/UserController.java`, `adapter/out/persistence/UserDirectoryAdapter.java`, `UserWriterAdapter.java`, `application/usecase/UserCommandService.java`, `application/port/in/ManageUsersUseCase.java`, `domain/port/out/UserDirectory.java`, `UserWriter.java`, `domain/policy/ProjectAccessPolicy.java`.

Frontend: `frontend/src/app/core/services/user.service.ts`, `frontend/src/app/core/models/user.ts`, `frontend/src/app/features/users/user-list/`, `user-detail/`, `user-edit/`, `frontend/src/app/app.routes.ts`.

Tecnologías: Java 21 / Spring Boot 4.1.0 / Maven, Angular 22.1.5 / TypeScript 6 / Vitest 4.1.12 / Playwright 1.58.x, PostgreSQL 18.

### 3.2 Autenticación y autorización (Integrante 2)

JWT (HS256) vía `POST /api/auth/login`; token `Bearer` en llamadas protegidas. Solo Jefe activo gestiona usuarios (`ProjectAccessPolicy.requireChief` en cada caso de uso). `authGuard` + `chiefGuard` protegen rutas del frontend, pero la autorización se revalida en backend. Usuario con `is_active = false` recibe 403 al login y sus tokens son rechazados en operaciones protegidas (`CurrentActor` lee `is_active` en cada petición).

### 3.3 Gestión de usuarios `app_user` (responsabilidad Integrante 2)

Tabla `task_manager.app_user`: `user_id` PK identity, `email` UK (check lowercase + `_%@_%`), `password_hash`, `given_names`, `family_names`, `is_chief` (default false), `is_active` (default true), `created_at`, `updated_at`.

Operaciones CRUD disponibles al finalizar el EC4:

| Operación | Endpoint | Estado | Nota |
|---|---|---|---|
| CREATE | `POST /api/users` | Preexistente, preservado | 201, Jefe activo |
| READ individual | `GET /api/users/{userId}` | Implementado EC4 | 200, sin `password_hash` |
| READ listado | `GET /api/users` | Implementado EC4 | 200, sin secretos |
| UPDATE | `PUT /api/users/{userId}` | Implementado EC4 | 200, solo perfil, sin mass assignment |
| DELETE | `DELETE /api/users/{userId}` | Implementado EC4 | 200, desactivación lógica (`is_active = false`) |

**Reglas de negocio:** solo Jefe activo gestiona usuarios; email único y normalizado; respuesta nunca expone contraseña ni hash; `UpdateUserRequest` tiene todos los campos opcionales e ignora `is_chief`, `is_active`, `password_hash`, `password` (mass assignment rechazado). La eliminación física está bloqueada por FK (`task_assignment.worker_user_id` ON DELETE RESTRICT, `project.manager_user_id`, etc.) y por falta de permiso `DELETE` del rol `gestor_tareas_app` en `app_user`; por tanto se usa desactivación lógica y la UI comunica "desactivado", nunca "borrado".

### 3.4 DTOs (Integrante 2)

- `UserResponse`: expone `userId, email, givenNames, familyNames, fullName, chief, active` — nunca `password_hash`, `password`, `token`.
- `UpdateUserRequest`: todos los campos opcionales (`email?`, `givenNames?`, `familyNames?`), actualización parcial.
- `CreateUserRequest`: `email, givenNames, familyNames, password`.

### 3.5 Errores controlados

Vía `GlobalExceptionHandler`: 400 (validación), 403 (no Jefe / Jefe inactivo), 404 (usuario inexistente), 409 (email duplicado). Nunca se filtran stack traces ni SQL.

### 3.6 Funcionalidades preexistentes vs implementadas en EC4

| Funcionalidad | Preexistente | Implementada en EC4 (Integrante 2) |
|---|---|---|
| `POST /api/users` | Sí | No (preservada) |
| `GET /api/users` | No | Sí |
| `GET /api/users/{userId}` | No | Sí |
| `PUT /api/users/{userId}` | No | Sí |
| `DELETE /api/users/{userId}` | No | Sí (desactivación lógica) |
| UI `/users` (listado) | No | Sí |
| UI `/users/:userId` (detalle) | No | Sí |
| UI `/users/:userId/edit` (edición) | No | Sí |
| UI `/users/new` (creación) | Sí | No (reutilizada) |
| Servicios Angular `user.service.ts` (list/get/update/deactivate) | Parcial (solo create) | Completado |
| Rutas protegidas `/users`, `/users/:userId`, `/users/:userId/edit` | No | Sí |

---

## 4. Formulación de los escenarios

Matriz completa de 19 escenarios (responsabilidad Integrante 2 destacada en `app_user`; todos los escenarios incluidos por requisito del Anexo 2):

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

Enfoque Integrante 2 (9 escenarios `app_user`): USR-INS-01..03, USR-UPD-01..03, USR-DEL-01, USR-SEL-01, USR-SEL-02 — todos PASSED.

Archivos `.feature` reales (rutas desde `docs/EC4/informes/`):
- Proceso principal: `../../../backend/src/test/resources/features/ec4/proceso-principal.feature` (`@PP-01`)
- CRUD `app_user`: `../../../backend/src/test/resources/features/ec4/app-user-crud.feature` (`@USR`)
- CRUD `task_assignment`: `../../../backend/src/test/resources/features/ec4/task-assignment-crud.feature` (`@ASG`)

---

## 5. Formulación de los Step Definitions (Integrante 2)

### 5.1 Clase de Step Definitions: `UserCrudSteps.java`

Ruta desde `docs/EC4/informes/`: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/UserCrudSteps.java`

Todas las llamadas son HTTP reales contra la API del backend y la verificación de persistencia se hace contra PostgreSQL de integración (`gestor_tareas_test`). No hay mocks de repositorio.

Métodos únicos (código fuente completo en `BASE_DOCUMENTAL_EC4.md` sección 5.3.2):

| Paso Gherkin | Método | Escenarios que lo usan |
|---|---|---|
| `an authenticated chief user` | `an_authenticated_chief_user()` | Todos USR (Background) |
| `I create a valid user with given names {string} and family names {string}` | `i_create_a_valid_user_with_given_names_and_family_names` | USR-INS-01, USR-INS-02 |
| `I create a user with email {string} and valid names and password` | `i_create_a_user_with_email_and_valid_names_and_password` | USR-INS-03 |
| `a user exists with given names {string} and family names {string}` | `a_user_exists_with_given_names_and_family_names` | USR-UPD-01..03, USR-DEL-01, USR-SEL-01 |
| `a second user exists with given names {string} and family names {string}` | `a_second_user_exists_with_given_names_and_family_names` | USR-SEL-02 |
| `I update the user's names to {string} and {string}` | `i_update_the_user_s_names_to_and` | USR-UPD-01 |
| `I update the user's email to {string}` | `i_update_the_user_s_email_to` | USR-UPD-02, USR-UPD-03 |
| `I deactivate the user` | `i_deactivate_the_user` | USR-DEL-01 |
| `I consult the user by id` | `i_consult_the_user_by_id` | USR-SEL-01 |
| `I list users` | `i_list_users` | USR-SEL-02 |

Then helpers reutilizados: `the_user_is_created_with_status_201`, `the_response_has_no_password_hash_password_or_token`, `the_user_is_stored_in_postgresql_as_active_and_non_chief`, `the_response_is_400`, `no_user_is_stored_in_postgresql`, `the_response_is_200`, `the_user_s_names_are_and_in_postgresql`, `the_user_s_email_is_stored_normalized_in_postgresql`, `the_user_s_previous_names_are_preserved_in_postgresql`, `the_user_is_inactive_in_postgresql`, `the_user_row_is_preserved_not_physically_deleted`, `the_response_shows_given_names_family_names_email_and_status`, `the_list_contains_the_created_users`, `no_entry_exposes_password_hash_password_or_token`.

Ejemplo representativo (`i_create_a_valid_user_with_given_names_and_family_names`): genera email único (`usr.<uuid>@gestor.local`), llama `apiClient.postUser` con `givenNames`, `familyNames`, `password`, afirma `201 Created`, verifica ausencia de secretos en el cuerpo, guarda `createdUserId` en `ScenarioContext`.

Ejemplo (`the_user_is_stored_in_postgresql_as_active_and_non_chief`): ejecuta `jdbc.queryForObject("SELECT is_active FROM task_manager.app_user WHERE user_id = ?", Boolean.class, userId)` y `SELECT is_chief`, afirma `active == true` y `chief == false`.

### 5.2 Hooks, contexto y helpers (compartidos, responsabilidad Integrante 1)

- `Hooks.java`: limpia BD en `@Before`, crea jefe activo con email único, login JWT, `context.reset()`. Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/hooks/Hooks.java`.
- `ScenarioContext.java`: estado aislado (`createdUserId`, `authToken`, `chiefEmail`, respuestas HTTP). Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/context/ScenarioContext.java`.
- `ApiClient.java`: `postUser`, `getUser`, `listUsers`, `putUser`, `deleteUser` con manejo de errores 4xx/5xx sin lanzar excepción. Ruta: `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/support/ApiClient.java`.

### 5.3 Evidencia de integración real (no prueba unitaria disfrazada)

Cada paso USR atraviesa HTTP real → `UserController` → `ManageUsersUseCase` → `UserCommandService` → `UserWriterAdapter`/`UserDirectoryAdapter` → JPA/JDBC → PostgreSQL `gestor_tareas_test`. Los `@Then` de BD verifican con `JdbcTemplate` (`is_active`, `is_chief`, `given_names`, `family_names`, `email`, conteo de filas). El paso `the_response_has_no_password_hash_password_or_token` afirma que el cuerpo de respuesta no contiene claves sensibles.

---

## 6. Ejecución de pruebas (enfoque Integrante 2)

### 6.1 Resultado por escenario USR (9 escenarios)

| # | Código | Nombre del escenario | Tipo | Resultado real | Fecha | Comando | Código HTTP | Fuente | Observaciones |
|---|---|---|---|---|---|---|---|---|---|
| 1 | USR-INS-01 | Create a valid user (Happy Path) | Happy | PASSED | 26/11/2026 | `mvn test -Dtest=CucumberIntegrationTest` | 201 | salida Cucumber + surefire XML | email `usr.<uuid>@gestor.local` |
| 2 | USR-INS-02 | Create a second valid and distinct user (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 201 | idem | segundo usuario distinto |
| 3 | USR-INS-03 | Reject creation with invalid email (Unhappy Path) | Unhappy | PASSED | 26/11/2026 | idem | 400 | idem | email `invalido`, sin creación |
| 4 | USR-UPD-01 | Update names and family names (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 200 | idem | |
| 5 | USR-UPD-02 | Update email to a valid unique email (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 200 | idem | normalizado `maria.nueva@gestor.local` |
| 6 | USR-UPD-03 | Reject update with invalid email and preserve previous data (Unhappy Path) | Unhappy | PASSED | 26/11/2026 | idem | 400 | idem | nombres previos preservados |
| 7 | USR-DEL-01 | Deactivate a non-referenced test user (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 200 | idem | desactivación lógica, fila preservada |
| 8 | USR-SEL-01 | Consult an existing individual user without secrets (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 200 | idem | sin `password_hash` |
| 9 | USR-SEL-02 | List users representing status without secrets (Happy Path) | Happy | PASSED | 26/11/2026 | idem | 200 | idem | sin secretos |

Resultado USR: 9/9 aprobados, 0 fallos, 0 omitidos.

### 6.2 Pruebas complementarias Vitest (Integrante 2)

| Espec | Pruebas | Resultado | Fuente |
|---|---:|---|---|
| `user.service.spec.ts` | HTTP servicio (URL, verbo, body, errores) | PASSED | `frontend/coverage/gestor-tareas/` + salida Vitest |
| `user-list.spec.ts` | Listado: carga, renderizado, vacío, error, navegación, desactivación, inactivo | PASSED | idem |
| `user-edit.spec.ts` | Edición: carga, inexistente, guarda solo perfil, correo inválido, doble envío, cancelar, error | PASSED | idem |

Total Vitest (todas las specs, incluyendo las de Integrante 3): 23 archivos, 349 pruebas, 0 fallos.

### 6.3 Pruebas complementarias Playwright E2E (Integrante 2)

| Spec | Pruebas | Resultado | Fuente |
|---|---:|---|---|
| `e2e/users/user-crud.spec.ts` | 8 tests (crear 2 usuarios, detalle/edición con persistencia, email normalizado, correo inválido, mass assignment, desactivación, login denegado a inactivo, no-Jefe 403) | 8 PASSED | salida Playwright (directorio temporal) |
| `e2e/users/user-create.spec.ts` | 4 tests | 4 PASSED | idem |

Total E2E usuarios: 12 pruebas, 0 fallos.

### 6.4 Resumen consolidado (enfoque Integrante 2)

| Nivel | Planificados | Ejecutados | Aprobados | Fallados | Omitidos | Pendientes / no ejecutados |
|---|---:|---:|---:|---:|---:|---|
| BDD USR (9 escenarios) | 9 | 9 | 9 | 0 | 0 | 0 |
| Vitest usuarios (3 specs) | — | — | PASSED | 0 | 0 | — |
| Playwright usuarios (2 specs) | 12 | 12 | 12 | 0 | 0 | 0 |

No se contó ningún escenario dos veces por ejecución local y CI: no existe run real de GitHub Actions para esta rama.

---

## 7. Cobertura de pruebas (enfoque Integrante 2)

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

## 8. Conclusiones y lecciones aprendidas (Integrante 2)

### 8.1 Conclusiones técnicas (hechos verificables)

- Los 9 escenarios BDD de `app_user` se ejecutaron y aprobaron (9/9, 0 fallos) como pruebas de integración reales contra API Spring Boot y PostgreSQL `gestor_tareas_test`.
- El CRUD completo de `app_user` quedó implementado en backend y frontend, sin añadir funcionalidades ajenas al sistema.
- La desactivación lógica (`is_active = false`) de usuarios fue necesaria porque el rol de aplicación no tiene permiso `DELETE` en `app_user` y existen FK con ON DELETE RESTRICT.
- Ninguna respuesta de usuario expone `password_hash`, `password` ni `token` (verificado en USR-SEL-01, USR-SEL-02 y helper `the_response_has_no_password_hash_password_or_token`).
- El `PUT` de usuario ignora `is_chief`, `is_active`, `password_hash`, `password` (mass assignment rechazado).
- Cobertura por encima del 85 % en backend (JaCoCo LINE 95,14 %, BRANCH 85,26 %) y frontend (Vitest Statements 92,95 %, Branches 88,53 %, Functions 87,37 %, Lines 94,05 %).

### 8.2 Lecciones aprendidas

- La autorización en backend no puede sustituirse por guards del frontend; cada caso de uso revalida `requireChief`.
- La eliminación física de usuarios está bloqueada por FK y permisos de BD; la desactivación lógica preserva integridad e historial.
- Los emails únicos por escenario (`UUID.randomUUID()`) evitan colisiones en `gestor_tareas_test`.

### 8.3 Reflexiones personales

**[REFLEXIÓN PERSONAL A VALIDAR POR EL INTEGRANTE 2]** Bloque reservado para la experiencia subjetiva del Integrante 2 durante el EC4 (dificultades, resoluciones, trabajo en equipo, limitaciones pendientes). No hay evidencia objetiva en el historial para atribuirla.

### 8.4 Limitaciones pendientes

- Capturas de pantalla de reportes de cobertura (bloqueo `file://`, sección 7.4).
- Pipeline de GitHub Actions definido pero no ejecutado en GitHub (sin run real verificable).
- Validación de navegador real vía Playwright MCP no disponible; las pruebas E2E sí se ejecutaron con Playwright Test.

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
| Feature CRUD `app_user` | `../../../backend/src/test/resources/features/ec4/app-user-crud.feature` |
| Step Definitions `app_user` | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/UserCrudSteps.java` |
| Hooks | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/hooks/Hooks.java` |
| ScenarioContext | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/context/ScenarioContext.java` |
| ApiClient | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/support/ApiClient.java` |
| CucumberIntegrationTest | `../../../backend/src/test/java/com/hardcoders/taskmanager/bdd/CucumberIntegrationTest.java` |
| UserController | `../../../backend/src/main/java/com/hardcoders/taskmanager/adapter/in/web/user/UserController.java` |
| UserCommandService | `../../../backend/src/main/java/com/hardcoders/taskmanager/application/usecase/UserCommandService.java` |
| ManageUsersUseCase | `../../../backend/src/main/java/com/hardcoders/taskmanager/application/port/in/ManageUsersUseCase.java` |
| UserDirectory / UserWriter | `../../../backend/src/main/java/com/hardcoders/taskmanager/domain/port/out/` |
| UserDirectoryAdapter / UserWriterAdapter | `../../../backend/src/main/java/com/hardcoders/taskmanager/adapter/out/persistence/` |
| ProjectAccessPolicy | `../../../backend/src/main/java/com/hardcoders/taskmanager/domain/policy/ProjectAccessPolicy.java` |
| user.service.ts | `../../../frontend/src/app/core/services/user.service.ts` |
| user.service.spec.ts | `../../../frontend/src/app/core/services/user.service.spec.ts` |
| user-list.ts / user-list.spec.ts | `../../../frontend/src/app/features/users/user-list/` |
| user-detail.ts | `../../../frontend/src/app/features/users/user-detail/user-detail.ts` |
| user-edit.ts / user-edit.spec.ts | `../../../frontend/src/app/features/users/user-edit/` |
| app.routes.ts | `../../../frontend/src/app/app.routes.ts` |
| user.ts (modelo) | `../../../frontend/src/app/core/models/user.ts` |
| user-crud.spec.ts (E2E) | `../../../frontend/e2e/users/user-crud.spec.ts` |
| user-create.spec.ts (E2E) | `../../../frontend/e2e/users/user-create.spec.ts` |
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

### 9.7 Preguntas para la defensa (Integrante 2)

1. P: ¿Cómo manejan la eliminación de usuarios dado que `task_assignment` tiene FK `ON DELETE RESTRICT`?  
   R: Se valida que no existan asignaciones vigentes ni referencias antes de eliminar; se usa un usuario de prueba no referenciado. Se prefiere desactivación lógica (`is_active = false`) porque el rol de aplicación no tiene permiso `DELETE` en `app_user`. Soporte: `UserWriter.deactivate`, `UserController.DELETE /api/users/{userId}`, escenario USR-DEL-01, `IMPLEMENTACION_EC4_BITACORA.md` (decisión DELETE).

2. P: ¿Cómo evitan exponer el hash de contraseña en la UI y en las respuestas?  
   R: Los DTOs de respuesta (`UserResponse`) excluyen `password_hash`; las pruebas BDD verifican que la respuesta no contenga claves sensibles (`doesNotContain("password", "passwordHash", "password_hash", "token")`). Soporte: `UserCrudSteps.the_response_has_no_password_hash_password_or_token`, `UserResponse`, escenarios USR-SEL-01 / USR-SEL-02.

---
*Fin del INFORME EC4 — INTEGRANTE 2.*
