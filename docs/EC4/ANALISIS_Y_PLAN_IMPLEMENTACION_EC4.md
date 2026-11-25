# ANALISIS Y PLAN DE IMPLEMENTACION EC4

**Elemento de Competencia 4 — BDD + Gherkin + CRUD Full-Stack + Pruebas de Integración + Cobertura + CI/CD**

**Repositorio:** `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`  
**Rama objetivo:** `Refactor-E2`  
**Rama auditada:** `Refactor-E2`  
**HEAD auditado:** `18334fddaf07c346f79f648a4ffdc040f80fa4f2`  
**Fecha de planificación:** 25/11/2026  
**Fecha límite académica:** 02/12/2026  
**Equipo:** 3 integrantes (Integrante 1, Integrante 2, Integrante 3)  
**Docente:** M. Sc. Ing. Joel Reynaldo Alánez Durán  
**Idioma:** Español  
**Estado:** Exclusivamente análisis y planificación. No se ha implementado código.

---

## 1. Portada

| Campo | Valor |
|---|---|
| Proyecto | Gestor de Tareas Personales — Release 1 |
| Entregable | Elemento de Competencia 4 (EC4) |
| Tema | Behavior Driven Development + Pruebas de Integración + Cobertura de Pruebas |
| Repositorio | https://github.com/CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1 |
| Rama | Refactor-E2 |
| Integrantes | Integrante 1, Integrante 2, Integrante 3 |
| Docente | M. Sc. Ing. Joel Reynaldo Alánez Durán |
| Asignatura | Implementar Frameworks de Pruebas Automáticas (Unidad 4) |
| Fecha de entrega | 02/12/2026 |
| Versión del documento | 1.0 (plan de implementación) |
| Alcance | Análisis, auditoría, diseño y planificación. Sin implementación. |

---

## 2. Resumen ejecutivo

El EC4 requiere aplicar BDD con Gherkin, pruebas de integración y medición de cobertura sobre el sistema existente, completando CRUD full-stack (backend + frontend/UI) en dos tablas ya presentes en el modelo: `app_user` y `task_assignment`. No se deben crear funcionalidades de negocio nuevas.

La auditoría confirma que ambas tablas existen en la base de datos (V004/V006) y tienen implementaciones parciales en backend y frontend, pero ninguna de las dos tiene un CRUD completo. El backend expone `POST /api/users` (CREATE de usuarios) y `POST/GET/DELETE /api/tasks/{taskId}/assignments` (asignaciones), pero carece de endpoints de consulta individual, listado de usuarios, actualización y eliminión de usuarios, así como de actualización del motivo de asignación. El frontend solo tiene la vista de creación de usuarios (`/users/new`) y el diálogo de asignación de tareas.

El plan propone:
- 19 ejecuciones BDD (1 proceso principal Happy Path + 9 por tabla CRUD).
- Cucumber-JVM (Java 21, Spring Boot, JUnit Platform) como framework BDD, no ReqnRoll (el stack es Java).
- JaCoCo (backend) y Vitest v8 (frontend) con umbrales del 85 %.
- Pipeline de GitHub Actions (CI) sin etapa de despliegue, por no existir destino real.
- Distribución del trabajo entre tres integrantes en seis etapas del 25/11 al 02/12/2026.

Este documento es la especificación completa para que una segunda ejecución inicie la implementación.

---

## 3. Contexto y fuentes académicas

Se utilizaron los tres materiales académicos incorporados en el enunciado:

1. **Documento 1 — GCS T4-1.pdf: Automatización en la Agilidad — Parte 1.** Concepto y propósitos de la automatización, mitos y límites, automatización en metodologías ágiles, CI/CD, pirámide de pruebas (unitarias, integración, funcionales), desafíos, factores clave para automatizar, criterios para no automatizar y herramientas.
2. **Documento 2 — GCS T4-2.pdf: BDD + Gherkin — Parte 2.** Concepto y propósitos de BDD, características, Gherkin, estructura de archivos `.feature`, palabras clave, buenas prácticas, ejemplos CRUD (cliente), ReqnRoll vs Cucumber-JVM, cuándo aplicar y cuándo no BDD.
3. **Documento 3 — Enunciado oficial EC4.** Alcance, requisitos, componentes del entregable, regla de generación de Test Cases (proceso principal: 1 Happy Path; INSERT/UPDATE: 2 Happy + 1 Unhappy por tabla; DELETE/SELECT: solo Happy Path), rúbrica de 100 puntos, Anexo 2 (informe obligatorio del proyecto) y fecha de entrega.

En caso de conflicto, prevalece: enunciado oficial > contexto técnico real del repositorio > recomendaciones complementarias.

---

## 4. Analisis consolidado de los tres documentos

### 4.1 Automatización (Documento 1)
- La automatización debe aportar consistencia, repetibilidad, velocidad de retroalimentación y protección frente a regresiones.
- No automatizar todo indiscriminadamente; priorizar comportamientos importantes, repetibles y estables.
- La automatización continua combina CI + CD: subir código a GitHub, ejecutar pruebas automáticamente (GitHub Actions), desplegar a ambiente de pruebas si pasan.
- Pirámide de pruebas: unitarias (base), integración (intermedio), funcionales (superior). No confundir una prueba unitaria con una de integración real.
- Desafíos: mantenimiento, ambientes y datos, costo inicial.
- Factores clave para automatizar: repetición, estabilidad, valor para regresión, costo manual, importancia para el negocio.
- No automatizar: UI en rediseño, pruebas subjetivas de usabilidad/estética, pruebas únicas, pruebas demasiado complejas.

### 4.2 BDD y Gherkin (Documento 2)
- BDD alinea a stakeholders y convierte requisitos en escenarios ejecutables.
- Gherkin usa Given/When/Then; debe ser comprensible para personas técnicas y no técnicas.
- Los escenarios deben ser breves, claros y centrados en comportamientos específicos.
- Gherkin describe el comportamiento; los Step Definitions conectan con código ejecutable.
- ReqnRoll es para C#; el repositorio usa Java 21 + Spring Boot, por lo que Cucumber-JVM es la alternativa compatible.
- Ejemplos CRUD académicos (cliente) deben adaptarse al modelo real (`app_user`, `task_assignment`), no copiar campos ajenos.

### 4.3 Enunciado oficial EC4 (Documento 3)
- Continuar con el sistema existente; no desarrollar funcionalidades adicionales.
- El sistema debe tener el proceso principal y CRUD completos en dos tablas.
- Completar operaciones faltantes y construir sus interfaces visuales sí forma parte del entregable.
- Matriz obligatoria: proceso principal (1 Happy Path); INSERT y UPDATE de cada tabla (2 Happy Path + 1 Unhappy Path, el Unhappy con un solo dato erróneo); DELETE y SELECT (solo Happy Path).
- Rúbrica de 100 puntos: documento 20, proceso principal BDD 15, CRUD INSERT/UPDATE 18, CRUD Delete/Select 12, cobertura 15, defensa 20.
- Cada integrante entrega un informe PDF individual basado en la Guía UCB con portada, introducción, desarrollo, escenarios, Step Definitions, ejecución, cobertura, conclusiones y anexos.

---

## 5. Requisitos oficiales y rúbrica

### 5.1 Requisitos obligatorios (enunciado EC4)

| ID | Requisito | Detalle |
|---|---|---|
| R1 | Sistema existente | Continuar con el sistema desarrollado en etapas anteriores. |
| R2 | Repositorio | GitHub con acceso para el docente; rama `Refactor-E2`. |
| R3 | No nuevas funcionalidades | No desarrollar funcionalidades ajenas al sistema existente. |
| R4 | Proceso principal y dos CRUD | CRUD completos en dos tablas existentes, incluido frontend/UI. |
| R5 | BDD + Gherkin proceso principal | Pruebas de integración del proceso principal con BDD + Gherkin (1 Happy Path). |
| R6 | BDD + Gherkin CRUD | Pruebas de integración de los CRUD de las dos tablas adicionales. |
| R7 | Cobertura | Medición con herramienta elegida (JaCoCo backend, Vitest frontend). |
| R8 | Presentación de resultados | Evidencias, capturas, informe por integrante, defensa. |

### 5.2 Regla de Test Cases obligatoria

| Operación | Happy Path | Unhappy Path | Condición Unhappy |
|---|---:|---:|---|
| Proceso principal | 1 | 0 | Creación y persistencia de la jerarquía Proyecto → Etapa → Actividad → Tarea → Subtarea. |
| INSERT (por tabla) | 2 | 1 | Un solo dato erróneo, resto válidos. |
| UPDATE (por tabla) | 2 | 1 | Un solo dato erróneo, resto válidos. |
| DELETE (por tabla) | 1 | 0 | Solo Happy Path. |
| SELECT individual (por tabla) | 1 | 0 | Solo Happy Path. |
| SELECT listado (por tabla) | 1 | 0 | Solo Happy Path. |

Total planificado: **19 ejecuciones BDD** (1 proceso principal + 9 `app_user` + 9 `task_assignment`).

### 5.3 Rúbrica oficial (100 puntos)

| Elemento a evaluar | Puntaje máximo |
|---|---:|
| Documento en base a contenido mínimo | 20 |
| Pruebas de integración del proceso principal con BDD | 15 |
| Pruebas de integración de los CRUD de las dos tablas — Inserts y Updates | 18 |
| Pruebas de integración de los CRUD de las dos tablas — Delete y Select | 12 |
| Medición de cobertura de pruebas | 15 |
| Defensa y presentación (dos preguntas a cada integrante) | 20 |
| **TOTAL** | **100** |

---

## 6. Auditoria del repositorio

### 6.1 Estado de Git (solo lectura)

| Concepto | Valor |
|---|---|
| Rama actual | `Refactor-E2` |
| HEAD | `18334fddaf07c346f79f648a4ffdc040f80fa4f2` |
| Coincide con referencia | Sí |
| Último commit | `18334fd Fix assignment dialog E2E test and pass EC3 workflow` |
| Cambios sin commit | `frontend/playwright-report/index.html` (modificado, no se toca) |
| Directorio `.github/workflows` | No existe (sin CI/CD configurado) |

### 6.2 Auditoría backend

**Stack:** Java 21, Spring Boot 4.1.0, Maven, arquitectura hexagonal, Spring Security + JWT, JPA/JDBC, PostgreSQL 18, `ddl-auto: validate`, puerto 8081, JaCoCo 0.8.15 (umbrales LINE 85 %, BRANCH 85 %).

**Capa de dominio/modelo relevante:**
- `AppUserEntity` (tabla `task_manager.app_user`): `user_id`, `email`, `password_hash`, `given_names`, `family_names`, `is_chief`, `is_active`.
- `TaskAssignmentEntity` (tabla `task_manager.task_assignment`): PK compuesta `(task_id, worker_user_id)`, `assigned_at`, `assigned_by_user_id`, `reason` (varchar 500, nullable, V006).
- `TaskAssignmentId`: embeddable con `taskId` y `workerUserId`.
- `TaskAssignment` (domain): `REASON_MAX = 500`, `DEFAULT_REASON = "Asignación de Personal"`, `normalizeReason()`.

**Puertos de salida:**
- `UserDirectory` (solo lectura): `findById`, `findByIds`, `findActive`.
- `UserWriter` (escritura): `existsByEmail`, `save` (solo CREATE).
- `AssignmentRepository`: `find(taskId, workerUserId)`, `findForTask`, `findForTasks`, `findForWorker`, `save`, `delete`.

**Adaptadores de persistencia:**
- `UserDirectoryAdapter`: implementa lectura de `app_user` vía `EntityManager` (no usa `AppUserJpaRepository` para lectura).
- `UserWriterAdapter`: implementa `UserWriter` con `AppUserJpaRepository` (solo `save`, CREATE).
- `TaskAssignmentJpaRepository`: `findByTaskTaskId`, `findByWorkerUserId`.
- `AssignmentPersistenceAdapter`: implementa `AssignmentRepository` con `EntityManager` (find, findForTask, save, delete).

**Casos de uso:**
- `ManageUsersUseCase.create()` — solo CREATE de usuarios (solo Jefe activo).
- `ManageAssignmentsUseCase`: `assign`, `unassign`, `assignmentsForTask`, `assignableWorkers`.

**Controladores (endpoints existentes):**
- `POST /api/users` — CREATE usuario (UserController).
- `POST /api/tasks/{taskId}/assignments` — crear asignación (HierarchyController).
- `GET /api/tasks/{taskId}/assignments` — listado de asignaciones de una tarea.
- `DELETE /api/tasks/{taskId}/assignments/{workerUserId}` — revocar asignación.
- `GET /api/tasks/{taskId}/assignments/{workerUserId}` — **no existe** como endpoint HTTP (solo en dominio).
- `GET /api/users`, `GET /api/users/{userId}`, `PUT /api/users/{userId}`, `DELETE /api/users/{userId}` — **no existen**.
- `PUT /api/tasks/{taskId}/assignments/{workerUserId}` (actualizar motivo) — **no existe**.

**Manejo de errores:** `GlobalExceptionHandler` mapea `NotFoundException` (404), `ForbiddenException` (403), `DomainValidationException` (400), `DataIntegrityViolationException` (409), conflictos de versión (412).

**Pruebas backend existentes:** 381 tests (histórico, 20/11/2026), incluyendo `PostgresIntegrationTest` con `gestor_tareas_test`, `RANDOM_PORT`, `RestTemplate`, `JdbcTemplate`, `TransactionTemplate`.

**Cobertura backend histórica (JaCoCo, 20/11/2026):** LINE 94,97 %, BRANCH 85,09 %, INSTRUCTION 92,58 %. Son resultados históricos, no mediciones nuevas.

### 6.3 Auditoría frontend

**Stack:** Angular 22.1.x, TypeScript 6.0.x, Vitest 4.1.x (coverage v8, umbrales 85 % todas las métricas), Playwright 1.58.x, puerto 4200, proxy `/api` al backend.

**Servicios HTTP existentes:**
- `UserService.create()` — solo `POST /api/users`.
- `WbsService.listWorkers(projectId)`, `listAssignments(taskId)`, `assign(taskId, workerUserId, reason)`, `unassign(taskId, workerUserId)`.
- `TaskService`: operaciones de tareas/subtareas/actividades (no asignaciones directas).

**Modelos TypeScript:**
- `user.ts`: `CreateUserRequest`, `UserResponse`.
- `api.ts`: `AssignmentDto`, `AssignWorkerBody`.
- `project.ts`: `UserSummary`.

**Componentes existentes:**
- `UserForm` (`/users/new`): formulario de creación de usuarios, protegido con `chiefGuard`.
- `AssignmentDialog`: diálogo de asignación/revocation de Personal en una tarea (listado, crear, retirar, motivo).

**Rutas (`app.routes.ts`):**
- `/users/new` — `UserForm`, `authGuard` + `chiefGuard`.
- No existen `/users`, `/users/:userId`, `/users/:userId/edit`.

**Guard:** `authGuard` (sesión), `chiefGuard` (rol Jefe).

**Pruebas frontend existentes:** Vitest specs (`user-form.spec.ts`, `assignment-dialog.spec.ts`, etc.), Playwright E2E (`user-create.spec.ts`, `auth.spec.ts`, `main-flow.spec.ts`). 307 tests histórico (20/11/2026).

**Cobertura frontend histórica (Vitest, 20/11/2026):** Statements 92,47 %, Branches 88,30 %, Functions 86,62 %, Lines 93,51 %. Resultados históricos.

### 6.4 Auditoría de base de datos

**Migraciones aplicadas:** V001 (esquema inicial legacy), V002 (vistas y permisos), V003 (task archive), V004 (modelo de gestión de proyectos), V005 (sort_order), V006 (task_assignment.reason).

**Tablas relevantes (`task_manager`):**
- `app_user` (V004): `user_id` PK identity, `email` UK (check lowercase + `_%@_%`), `password_hash`, `given_names`, `family_names`, `is_chief` (default false), `is_active` (default true), `created_at`, `updated_at`. Permisos app: SELECT, INSERT, UPDATE.
- `task_assignment` (V004): PK compuesta `(task_id, worker_user_id)`, `assigned_at` default now, `assigned_by_user_id`, FK a `task` (ON DELETE CASCADE), FK a `worker_user_id` (ON DELETE RESTRICT), FK a `assigned_by_user_id` (ON DELETE RESTRICT). `reason` añadido en V006 (varchar 500, NULL, check `reason IS NULL OR length(btrim(reason)) >= 1`).
- `project`, `stage`, `activity`, `task`, `subtask`, catálogos (`project_status`, `work_status`, `task_priority`).

**Base de datos de integración:** `gestor_tareas_test` (usada por `PostgresIntegrationTest`). Base dev: `gestor_tareas`.

**Scripts Docker:** `database/docker/init-dev-db.sql` (crea roles `gestor_tareas_owner`/`gestor_tareas_app`, BD `gestor_tareas`, aplica V001-V006), `seed-dev-users.sql` (usuarios jefe, encargado1, personal, inactivo con password `Gestor2026!`). `docker-compose.yml` con `postgres:18`, puerto 5432.

### 6.5 Auditoría de pruebas y cobertura existentes

| Reporte | Existe | Fecha | Origen |
|---|---|---|---|
| JaCoCo backend (`backend/target/site/jacoco`) | Sí | 20/11/2026 | Histórico |
| Surefire backend (`backend/target/surefire-reports`) | Sí | 20/11/2026 | Histórico |
| Cobertura frontend (`frontend/coverage`) | Sí | 20/11/2026 | Histórico |
| Playwright report (`frontend/playwright-report`) | Sí | 20/11/2026 | Histórico |
| GitHub Actions workflows | No | — | No configurado |

No se generaron reportes nuevos durante esta fase. Los valores históricos no se presentan como mediciones frescas.

---

## 7. Arquitectura actual

El backend sigue arquitectura hexagonal:
- `adapter/in/web`: controladores REST (`UserController`, `HierarchyController`, `TaskController`, `ProjectController`, `ProgressController`).
- `adapter/out/persistence`: adaptadores JPA (`UserDirectoryAdapter`, `UserWriterAdapter`, `AssignmentPersistenceAdapter`, etc.) y repositorios Spring Data.
- `domain/model`: entidades de dominio (`TaskAssignment`, `UserAccount`, `Project`, `Task`, etc.).
- `domain/port`: puertos de salida (`UserDirectory`, `UserWriter`, `AssignmentRepository`, `TaskRepository`, `ProjectRepository`).
- `domain/policy`: reglas de negocio (`AssignmentPolicy`, `ProjectAccessPolicy`, `WbsHierarchyPolicy`, `DelegationPolicy`, etc.).
- `application/command`, `application/usecase`, `application/port/in`, `application/result`: casos de uso, comandos y resultados.
- `configuration`: `UseCaseConfiguration`, `TransactionalUseCases`.

El frontend es una SPA Angular con servicios HTTP, componentes standalone, rutas protegidas por guard y diálogos.

---

## 8. Modelo de datos (resumen relevante)

```mermaid
erDiagram
    app_user ||--o{ task_assignment : "asigna (worker)"
    app_user ||--o{ task_assignment : "asigna por (assigned_by)"
    task ||--o{ task_assignment : "tiene asignaciones"
    project ||--o{ stage : contiene
    stage ||--o{ activity : contiene
    activity ||--o{ task : contiene
    task ||--o{ subtask : contiene

    app_user {
        int user_id PK
        varchar email UK
        varchar password_hash
        varchar given_names
        varchar family_names
        boolean is_chief
        boolean is_active
    }
    task_assignment {
        int task_id PK,FK
        int worker_user_id PK,FK
        timestamp assigned_at
        int assigned_by_user_id FK
        varchar(500) reason NULL
    }
    task {
        int task_id PK
        int activity_id FK
        varchar title
    }
```

Restricciones clave para el plan:
- `task_assignment.worker_user_id` → `app_user.user_id` ON DELETE RESTRICT: no se puede eliminar un usuario con asignaciones vigentes.
- `task_assignment.task_id` → `task.task_id` ON DELETE CASCADE.
- `reason` nullable, máximo 500 caracteres, no puede estar en blanco si tiene valor.
- Email único y normalizado (lowercase).

---

## 9. Auditoria CRUD (estado real)

Estado: **Implementado** = verificado en código actual; **Parcial** = implementación incompleta (falta endpoint/UI); **No implementado** = ausente tras auditoría.

### `app_user`

| Operación | Backend actual | Frontend actual | Estado | Brecha |
|---|---|---|---|---|
| CREATE | `POST /api/users` + `UserCommandService.create` | `UserService.create`, `UserForm`, `/users/new` | Implementado | Ninguna (CREATE ya completo) |
| READ individual | `UserDirectoryAdapter.findById` (solo dominio, sin endpoint HTTP) | No existe | Parcial | Falta endpoint `GET /api/users/{userId}` y UI de consulta |
| READ listado | No existe | No existe | No implementado | Falta endpoint `GET /api/users` y UI de listado |
| UPDATE | No existe | No existe | No implementado | Falta endpoint `PUT /api/users/{userId}` y UI de edición |
| DELETE | No existe | No existe | No implementado | Falta endpoint `DELETE /api/users/{userId}` y UI de eliminación/desactivación |

### `task_assignment`

| Operación | Backend actual | Frontend actual | Estado | Brecha |
|---|---|---|---|---|
| CREATE | `POST /api/tasks/{taskId}/assignments` | `WbsService.assign`, `AssignmentDialog` | Implementado | Ninguna |
| READ individual | `AssignmentRepository.find` (dominio, sin endpoint HTTP) | No existe | Parcial | Falta endpoint `GET /api/tasks/{taskId}/assignments/{workerUserId}` y UI de detalle |
| READ listado | `GET /api/tasks/{taskId}/assignments` | `WbsService.listAssignments`, cargado en `AssignmentDialog` | Implementado | Ninguna |
| UPDATE | No existe | No existe | No implementado | Falta endpoint `PUT /api/tasks/{taskId}/assignments/{workerUserId}` (solo motivo) y UI de edición |
| DELETE | `DELETE /api/tasks/{taskId}/assignments/{workerUserId}` | `WbsService.unassign`, botón "Retirar" | Implementado | Ninguna |

---

## 10. Seleccion de tablas y justificación

Se seleccionan **`app_user`** y **`task_assignment`** como las dos tablas para los CRUD.

**Justificación:**
1. Ambas tablas ya existen en el modelo de datos (migraciones V004 y V006 aplicadas), por lo que no se requieren migraciones nuevas ni nuevas tablas de negocio, respetando el requisito R3 (no funcionalidades adicionales).
2. Ambas tienen implementaciones parciales en backend y frontend, por lo que completar sus CRUD es factible dentro del cronograma (25/11–02/12/2026).
3. Ambas son operaciones críticas del negocio: gestión de usuarios (proceso complementario de seguridad/acceso) y asignación de Personal a tareas (proceso principal de ejecución US2.3/EC3-02), con claro valor para regresión.
4. No se seleccionan tablas nuevas porque el enunciado prohíbe funcionalidades de negocio ajenas al sistema existente y exige completar operaciones faltantes de tablas existentes.

---

## 11. Brechas del sistema

| Área | Brecha identificada | Impacto EC4 |
|---|---|---|
| Backend `app_user` | Faltan GET individual, GET listado, PUT, DELETE | CRUD incompleto |
| Backend `task_assignment` | Falta GET individual, PUT (motivo) | CRUD incompleto |
| Frontend `app_user` | Falta listado, consulta individual, edición, eliminación | Sin UI full-stack |
| Frontend `task_assignment` | Falta detalle individual, edición de motivo | Sin UI full-stack |
| BDD | No existen archivos `.feature`, ni Step Definitions, ni runner Cucumber | No cumple requisito BDD |
| Pruebas de integración | Existen pruebas unitarias y una clase de integración (`PostgresIntegrationTest`), pero no escenarios BDD ejecutables para los CRUD | No cumple requisito de integración BDD |
| Cobertura | JaCoCo y Vitest configurados con umbrales 85 %, pero sin medición fresca del HEAD actual | Falta evidencia de cobertura |
| CI/CD | No existe `.github/workflows/` | Sin automatización CI |
| Documentación | No existe `docs/EC4/` ni informe común | Falta entregable documental |

---

## 12. Diseno futuro del CRUD `app_user`

### 12.1 Backend (propuesto)

Endpoints:
- `POST /api/users` — CREATE (ya existe, preservar).
- `GET /api/users` — listado de usuarios (sin `password_hash`).
- `GET /api/users/{userId}` — consulta individual (sin `password_hash`).
- `PUT /api/users/{userId}` — actualización de datos autorizados (nombres, apellidos, email; no `is_chief`, `is_active`, `password_hash`).
- `DELETE /api/users/{userId}` — eliminación de usuario de prueba no referenciado; respetar FK (`task_assignment` ON DELETE RESTRICT, `project.manager_user_id`, `project.created_by_user_id`, `task.completed_by_user_id`, `subtask.completed_by_user_id`). No usar cascadas que eliminen proyectos/tareas/asignaciones/historial.

Reglas:
- Solo Jefe activo puede gestionar usuarios (`ProjectAccessPolicy.requireChief`).
- Email único, normalizado, validación de formato.
- Respuesta nunca expone contraseña ni hash.
- Transacción en servicio de aplicación.
- DELETE: validar que no haya asignaciones vigentes ni referencias antes de eliminar; si hay referencias, responder error (no forzar eliminación). Justificar si se usa desactivación lógica (`is_active = false`) en lugar de eliminación física.

DTOs futuros (propuestos, no creados): `UserListResponse`, `UserUpdateRequest`, `UserDetailResponse`.

### 12.2 Frontend/UI (propuesto)

Pantallas y rutas:
- `/users` — listado de usuarios (tabla, empty state, búsqueda).
- `/users/new` — formulario de creación (ya existe, reutilizar).
- `/users/:userId` — consulta individual (solo lectura, sin secretos).
- `/users/:userId/edit` — formulario de edición (datos autorizados).
- Eliminación/desactivación con confirmación (diálogo).

Elementos UX: mensajes de éxito/error, estados de carga, validaciones (email, nombres, apellidos, contraseña), protección de ruta (`chiefGuard`), prevención de doble submit, actualización del listado tras operación.

Rol: protegido para Jefe. Los guards de Angular no sustituyen autorización en backend.

---

## 13. Diseno futuro del CRUD `task_assignment`

### 13.1 Backend (propuesto)

Endpoints:
- `POST /api/tasks/{taskId}/assignments` — CREATE (ya existe, preservar).
- `GET /api/tasks/{taskId}/assignments` — listado (ya existe, preservar).
- `GET /api/tasks/{taskId}/assignments/{workerUserId}` — consulta individual por clave compuesta (nuevo).
- `PUT /api/tasks/{taskId}/assignments/{workerUserId}` — actualizar únicamente `reason` (nuevo).
- `DELETE /api/tasks/{taskId}/assignments/{workerUserId}` — revocación (ya existe, preservar).

Reglas (preservar existentes):
- Clave compuesta `(task_id, worker_user_id)`.
- Solo Encargado del proyecto o Jefe puede asignar/revocar (`AssignmentPolicy.requireCanAssign`).
- Trabajador debe existir y estar activo (`AssignmentPolicy.requireEligibleWorker`).
- No autoasignación (`AssignmentPolicy.requireNotSelf`).
- Proyecto abierto (`WbsHierarchyPolicy.requireOpen`).
- No duplicidad (clave compuesta PK).
- Motivo: normalizar (trim, vacío/nulo → `DEFAULT_REASON`, máximo 500).
- Valor predeterminado: `"Asignación de Personal"`.
- UPDATE: modificar únicamente `reason`; no modificar tarea, trabajador, fecha ni usuario que asignó.
- `assigned_at` y `assigned_by` no se modifican en UPDATE.

DTOs futuros (propuestos): `AssignmentDetailResponse`, `AssignmentUpdateRequest` (solo `reason`).

### 13.2 Frontend/UI (propuesto)

Pantallas:
- Listado de asignaciones dentro del contexto de la tarea (ya parcialmente en `AssignmentDialog`).
- Formulario de creación con selección de trabajador activo (ya parcialmente).
- Detalle individual de asignación (nuevo).
- Edición del motivo (nuevo).
- Revocación con confirmación (ya parcialmente, botón "Retirar").

Elementos UX: mensajes de éxito/error, estados de carga, refresh del listado, protección por roles, no forzar entrada manual de IDs de tarea (contexto implícito).

Integración natural en la vista existente de tareas (diálogo de asignación).

---

## 14. Diseno de frontend/UI (servicios Angular futuros)

Nombres conceptuales (adaptar a servicios reales):

Usuarios:
- `listUsers(): Observable<UserResponse[]>`
- `getUser(userId): Observable<UserResponse>`
- `createUser(request): Observable<UserResponse>`
- `updateUser(userId, request): Observable<UserResponse>`
- `deleteUser(userId): Observable<void>`

Asignaciones:
- `listAssignments(taskId): Observable<AssignmentDto[]>`
- `getAssignment(taskId, workerUserId): Observable<AssignmentDto>`
- `createAssignment(taskId, request): Observable<AssignmentDto>`
- `updateAssignment(taskId, workerUserId, request): Observable<AssignmentDto>`
- `deleteAssignment(taskId, workerUserId): Observable<void>`

UX futura: loading, empty state, success, validation error, server error, forbidden, not found, saving, deleting, confirmation, accesibilidad, navegación por teclado, prevención de doble submit, prevención de solicitudes duplicadas, actualización desde backend, manejo centralizado de errores. Sin rediseño innecesario de la aplicación.

Pruebas UI futuras (complementarias a BDD):
- Vitest: servicios, componentes, formularios, validaciones, estados de carga, errores HTTP, confirmaciones, actualización de datos, guards.
- Playwright Usuarios: login Jefe → listado → crear → consultar → editar → verificar → eliminar/desactivar → verificar estado final.
- Playwright Asignaciones: login autorizado → abrir tarea → consultar asignaciones → crear → consultar detalle → editar motivo → verificar → revocar → comprobar que ya no aparece.

---

## 15. Estrategia BDD y Gherkin

### 15.1 Framework futuro

Cucumber-JVM con Java 21, Spring Boot, JUnit Platform, Spring Test, Maven, PostgreSQL real (`gestor_tareas_test`). Justificación: ReqnRoll (Documento 2) es para C#; el repositorio es Java. No se introduce ReqnRoll ni se migra el backend a C#.

Estructura propuesta (no se crea ahora):
```
backend/src/test/
├── java/com/hardcoders/taskmanager/bdd/
│   ├── CucumberTestConfiguration.java
│   ├── CucumberIntegrationTest.java
│   ├── context/
│   ├── hooks/
│   ├── steps/
│   └── support/
└── resources/features/ec4/
    ├── proceso-principal.feature
    ├── app-user-crud.feature
    └── task-assignment-crud.feature
```

### 15.2 Regla del proceso principal

Una sola entrada Happy Path. Flujo: Proyecto → Etapa → Actividad → Tarea → Subtarea. Comprobar creación y persistencia de la jerarquía.

### 15.3 Matriz de escenarios BDD

| Tabla/proceso | Operación | Happy Path | Unhappy Path |
|---|---|---:|---:|
| app_user | INSERT | 2 | 1 |
| app_user | UPDATE | 2 | 1 |
| app_user | DELETE | 1 | 0 |
| app_user | SELECT individual | 1 | 0 |
| app_user | SELECT listado | 1 | 0 |
| task_assignment | INSERT | 2 | 1 |
| task_assignment | UPDATE | 2 | 1 |
| task_assignment | DELETE | 1 | 0 |
| task_assignment | SELECT individual | 1 | 0 |
| task_assignment | SELECT listado | 1 | 0 |
| Proceso principal | Creación jerarquía | 1 | 0 |
| **Total** | | **14** | **5** = **19** |

Cada Unhappy Path contiene un solo dato erróneo (resto válidos). DELETE y SELECT son exclusivamente Happy Path.

### 15.4 Datos de prueba futuros

**Usuarios:**
- INSERT Happy 1: usuario válido.
- INSERT Happy 2: segundo usuario distinto.
- INSERT Unhappy: email inválido.
- UPDATE Happy 1: edición válida del perfil.
- UPDATE Happy 2: actualización válida del email.
- UPDATE Unhappy: email inválido.
- DELETE: eliminar usuario de prueba no referenciado según política de integridad.
- SELECT: consultar usuario por ID; listar usuarios sin exposición de secretos.

**Asignaciones:**
- INSERT Happy 1: motivo explícito normalizado.
- INSERT Happy 2: motivo nulo/vacío con valor predeterminado.
- INSERT Unhappy: motivo > 500 caracteres.
- UPDATE Happy 1: modificar motivo.
- UPDATE Happy 2: motivo vacío/nulo normalizado.
- UPDATE Unhappy: motivo > 500 caracteres.
- DELETE: revocar asignación existente.
- SELECT: consultar asignación por clave compuesta; listar asignaciones de una tarea.

### 15.5 Step Definitions futuros

Conectar pasos Gherkin con operaciones HTTP reales (`RestTemplate` / `TestRestTemplate`). Verificar: código HTTP, respuesta, persistencia en BD, relaciones, ausencia de cambios indebidos en Unhappy Path. No usar mocks como sustituto de integración.

### 15.6 Buenas prácticas (Documento 2)

- Lenguaje claro y sencillo, sin detalles técnicos (clases Java, SQL, métodos privados) en pasos de negocio.
- Cada scenario comprueba un comportamiento específico.
- Usar `Background` para contexto común.
- Escenarios cortos.
- `Scenario Outline` + `Examples` para datos múltiples (una ejecución por fila).

---

## 16. Matriz de escenarios

| Código | Tabla/proceso | Operación | Happy/Unhappy | Datos | Resultado esperado | Responsable |
|---|---|---|---|---|---|---|
| PP-01 | Proceso principal | Crear jerarquía | Happy | Proyecto→Etapa→Actividad→Tarea→Subtarea | 201/200 y persistencia verificada | Integrante 1 |
| USR-INS-01 | app_user | INSERT | Happy | Usuario válido | 201, usuario creado sin secretos | Integrante 2 |
| USR-INS-02 | app_user | INSERT | Happy | Segundo usuario distinto | 201, usuario creado | Integrante 2 |
| USR-INS-03 | app_user | INSERT | Unhappy | Email inválido | 400, sin creación | Integrante 2 |
| USR-UPD-01 | app_user | UPDATE | Happy | Edición perfil válida | 200, datos actualizados | Integrante 2 |
| USR-UPD-02 | app_user | UPDATE | Happy | Email válido | 200, email actualizado | Integrante 2 |
| USR-UPD-03 | app_user | UPDATE | Unhappy | Email inválido | 400, sin cambio | Integrante 2 |
| USR-DEL-01 | app_user | DELETE | Happy | Usuario de prueba no referenciado | 200/204, usuario eliminado | Integrante 2 |
| USR-SEL-01 | app_user | SELECT individual | Happy | userId existente | 200, datos sin password_hash | Integrante 2 |
| USR-SEL-02 | app_user | SELECT listado | Happy | — | 200, lista sin secretos | Integrante 2 |
| ASG-INS-01 | task_assignment | INSERT | Happy | Motivo explícito | 200, razón persistida | Integrante 3 |
| ASG-INS-02 | task_assignment | INSERT | Happy | Motivo nulo → default | 200, `DEFAULT_REASON` | Integrante 3 |
| ASG-INS-03 | task_assignment | INSERT | Unhappy | Razón > 500 | 400, sin persistencia | Integrante 3 |
| ASG-UPD-01 | task_assignment | UPDATE | Happy | Modificar motivo | 200, razón actualizada | Integrante 3 |
| ASG-UPD-02 | task_assignment | UPDATE | Happy | Motivo vacío → default | 200, `DEFAULT_REASON` | Integrante 3 |
| ASG-UPD-03 | task_assignment | UPDATE | Unhappy | Razón > 500 | 400, sin cambio | Integrante 3 |
| ASG-DEL-01 | task_assignment | DELETE | Happy | Asignación existente | 200/204, revocada | Integrante 3 |
| ASG-SEL-01 | task_assignment | SELECT individual | Happy | Clave compuesta | 200, detalle de asignación | Integrante 3 |
| ASG-SEL-02 | task_assignment | SELECT listado | Happy | taskId existente | 200, lista de asignaciones | Integrante 3 |

---

## 17. Diseño de integración

Flujo: Gherkin → Cucumber Step Definitions → HTTP → Spring Boot → Application Services → Domain/Policies → JPA/JDBC → PostgreSQL 18.

Base de integración: `gestor_tareas_test`.

Patrones existentes a aprovechar (`PostgresIntegrationTest`):
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@DynamicPropertySource` (URL `gestor_tareas_test`, usuario `gestor_tareas_owner`, password `ownerpass`, `ddl-auto: validate`, JWT secret test)
- `RestTemplate`, `JdbcTemplate`, `TransactionTemplate`, `PlatformTransactionManager`
- Limpieza por escenario en `@BeforeEach` (orden inverso de FK: task_assignment, historiales, subtask, task, activity, stage, project, project_plan_history, app_user)
- Emails únicos (`UUID.randomUUID()`)
- Login JWT para obtener token de autenticación

Planificación:
- Aislamiento por escenario (limpieza selectiva).
- Datos únicos por ejecución.
- Limpieza controlada por escenario, orden inverso de FK.
- Repetibilidad.
- Sin pruebas destructivas sobre `gestor_tareas`.
- Sin borrado de volúmenes.
- Sin estado mutable compartido entre escenarios.
- Surefire (unitarias) y Failsafe (integración) para separar niveles de prueba (pendiente de configurar).

---

## 18. Estrategia de cobertura

### 18.1 Configuración conocida

**Backend (JaCoCo 0.8.15, `backend/pom.xml`):**
| Métrica | Umbral |
|---|---:|
| LINE | 85 % |
| BRANCH | 85 % |
| Comando | `cd backend && mvn verify` |

**Frontend (Vitest 4.1.x, `frontend/vitest.config.ts`):**
| Métrica | Umbral |
|---|---:|
| Statements | 85 % |
| Branches | 85 % |
| Functions | 85 % |
| Lines | 85 % |
| Comando | `cd frontend && npx --no-install ng test --watch=false --coverage` |

### 18.2 Línea base histórica (no presentar como nueva)

| Proyecto | Métrica | Valor | Fecha | Fuente |
|---|---|---|---|---|
| Backend | Tests | 381 | 20/11/2026 | surefire-reports |
| Backend | LINE | 94,97 % | 20/11/2026 | jacoco.csv |
| Backend | BRANCH | 85,09 % | 20/11/2026 | jacoco.csv |
| Backend | INSTRUCTION | 92,58 % | 20/11/2026 | jacoco.csv |
| Frontend | Tests | 307 | 20/11/2026 | coverage |
| Frontend | Statements | 92,47 % | 20/11/2026 | coverage-final.json |
| Frontend | Branches | 88,30 % | 20/11/2026 | coverage-final.json |
| Frontend | Functions | 86,62 % | 20/11/2026 | coverage-final.json |
| Frontend | Lines | 93,51 % | 20/11/2026 | coverage-final.json |

### 18.3 Plan de cobertura

- Tomar línea base fresca del HEAD actual (Etapa 0).
- Añadir tests para operaciones CRUD (backend y frontend).
- Añadir tests para Angular (servicios, componentes, formularios, guards).
- Incluir lógica BDD dentro de la medición cuando corresponda.
- Mantener todos los thresholds (85 %).
- Evitar exclusiones artificiales.
- No eliminar tests para obtener resultados verdes.
- Cubrir ramas y errores (incluidos Unhappy Path).
- Generar reportes reproducibles.
- Objetivo mínimo obligatorio: 85 % o superior en todas las métricas configuradas.

---

## 19. Propuesta CI/CD (GitHub Actions)

### 19.1 Estado actual

No existe `.github/workflows/`. No hay CI/CD configurado en la rama `Refactor-E2`.

### 19.2 Workflow propuesto (no crear ahora)

`.github/workflows/ec4-quality.yml`

**Triggers:** push a `Refactor-E2`, push a `main`, pull requests, `workflow_dispatch`.

**Jobs:**

1. **Backend:**
   - Checkout, Java 21, Maven, cache.
   - PostgreSQL 18 (servicio), migraciones V001-V006.
   - Pruebas unitarias (Surefire), pruebas de integración (Failsafe), Cucumber BDD.
   - JaCoCo (cobertura, reporte, check de umbrales).
   - Artefactos de reportes.

2. **Frontend:**
   - Node 24, `npm ci`.
   - Vitest con cobertura (umbrales 85 %).
   - Build de Angular (`ng build`).
   - Artefactos.

3. **E2E (opcional/condicional):**
   - PostgreSQL, Backend, Frontend, Chromium.
   - Playwright pruebas CRUD UI.
   - Reportes y traces.

4. **Consolidación:**
   - Resultado de jobs, quality gates, artefactos, estado global.

### 19.3 Seguridad (planificada)

- Menor privilegio, `GITHUB_TOKEN` con permisos mínimos.
- Actions fijadas a SHA completo verificado.
- Protección de secretos; no imprimir `.env`, no publicar tokens.
- Lockfiles, caches seguros.
- No exponer secretos a PRs no confiables.

### 19.4 CD

No existe destino de despliegue real y verificable. Por tanto:
- CI funcional (pruebas, build, cobertura).
- Artefactos de entrega y reportes.
- Documentación de la limitación.
- No se inventa etapa de despliegue.

---

## 20. Matriz de trazabilidad

| Código | Requisito | Tabla/proceso | Backend | Frontend/UI | BDD | Vitest/Playwright | Responsable | Etapa |
|---|---|---|---|---|---|---|---|---|
| PP-01 | R4, R5 | Proceso principal | API jerarquía | — | Cucumber | — | Integrante 1 | 1 |
| USR-CRUD | R4 | app_user | CRUD completo | Listado, detalle, editar, eliminar | Cucumber | Vitest, Playwright | Integrante 2 | 2 |
| ASG-CRUD | R4 | task_assignment | GET individual, PUT motivo | Detalle, editar motivo | Cucumber | Vitest, Playwright | Integrante 3 | 3 |
| COV | R7 | Ambos | JaCoCo | Vitest | — | Cobertura | Integrante 1 | 5 |
| CI | R2 | — | Pipeline | Pipeline | — | — | Integrante 1 | 5 |

---

## 21. Matriz CRUD (estado actual y objetivo)

| Tabla | Operación | Estado actual | Brecha | Implementación futura | Evidencia futura |
|---|---|---|---|---|---|
| app_user | CREATE | Implementado | — | Preservar | E2E user-create |
| app_user | READ individual | Parcial | Sin endpoint ni UI | `GET /api/users/{userId}` + UI consulta | Cucumber + Playwright |
| app_user | READ listado | No implementado | Sin endpoint ni UI | `GET /api/users` + UI listado | Cucumber + Playwright |
| app_user | UPDATE | No implementado | Sin endpoint ni UI | `PUT /api/users/{userId}` + UI edición | Cucumber + Playwright |
| app_user | DELETE | No implementado | Sin endpoint ni UI | `DELETE /api/users/{userId}` + UI eliminación | Cucumber + Playwright |
| task_assignment | CREATE | Implementado | — | Preservar | E2E main-flow |
| task_assignment | READ individual | Parcial | Sin endpoint ni UI | `GET /api/tasks/{id}/assignments/{workerId}` + UI detalle | Cucumber + Playwright |
| task_assignment | READ listado | Implementado | — | Preservar | Cucumber + Playwright |
| task_assignment | UPDATE | No implementado | Sin endpoint ni UI | `PUT .../assignments/{workerId}` (solo reason) + UI | Cucumber + Playwright |
| task_assignment | DELETE | Implementado | — | Preservar | Cucumber + Playwright |

---

## 22. Matriz de riesgos

| Riesgo | Probabilidad | Impacto | Mitigación | Responsable |
|---|---|---|---|---|
| FK RESTRICT al eliminar usuario con asignaciones | Media | Alta | Validar referencias antes de DELETE; usar usuario de prueba no referenciado; justificar desactivación lógica vs física | Integrante 2 |
| Exposición de datos sensibles (password_hash) | Media | Alta | Respuestas sin hash/contraseña; pruebas verifican ausencia de secretos | Integrante 2 |
| Elevación de privilegios (usuario no-Jefe gestiona usuarios) | Baja | Alta | Autorización en backend (`requireChief`) + guard Angular | Integrante 2 |
| Clave compuesta de asignaciones duplicada | Media | Media | PK compuesta + validación de duplicidad + mensaje 400/409 | Integrante 3 |
| Solicitudes duplicadas / doble submit | Media | Media | Botón desactivado durante `saving`, prevención en frontend | Integrante 2, 3 |
| Pruebas frágiles (acopladas a UI/interno) | Media | Media | Gherkin sin detalles técnicos, Step Definitions estables, contratos claros | Integrante 1 |
| Aislamiento de PostgreSQL (`gestor_tareas_test`) | Media | Media | Base de integración aislada, limpieza por escenario, sin volúmenes borrados | Integrante 1 |
| Compatibilidad Cucumber/JUnit/Spring | Baja | Media | Usar versiones existentes del stack, validar en Etapa 1 | Integrante 1 |
| Instrumentación JaCoCo / caída de cobertura | Media | Media | Línea base fresca, thresholds 85 %, sin exclusiones artificiales | Integrante 1 |
| Fallos Playwright (entorno, selectores) | Media | Media | Traces, retries en CI, selectores estables (`data-testid`) | Integrante 2, 3 |
| Dependencias incompatibles (Node 24, Java 21) | Baja | Media | Verificar versiones en README, usar contenedores de CI | Integrante 1 |
| Seguridad CI (secretos expuestos) | Baja | Alta | `GITHUB_TOKEN` mínimo, acciones con SHA, sin `.env` en CI | Integrante 1 |
| Tiempo disponible (entrega 02/12/2026) | Alta | Alta | Cronograma realista, trabajo paralelo de CRUD, revisión cruzada | Los tres |
| Conflictos entre integrantes (merge) | Media | Media | Ramas cortas por integrante, PR con revisión cruzada, integración en Etapa 4 | Los tres |

---

## 23. Distribucion entre tres integrantes

### Integrante 1 — Arquitectura BDD, proceso principal e integración
- Auditoría (línea base).
- Arquitectura BDD y runner Cucumber-JVM.
- Hooks, Scenario Context, cliente HTTP.
- Proceso principal Happy Path (jerarquía).
- Integración Maven (Surefire/Failsafe).
- PostgreSQL de CI (`gestor_tareas_test`).
- GitHub Actions (CI).
- Integración final y consolidación de resultados.
- Documentación común.

### Integrante 2 — CRUD full-stack `app_user`
Backend: SELECT individual, SELECT listado, UPDATE, DELETE (preservar CREATE), reglas, autorización, DTOs, persistencia.
Frontend/UI: listado, creación (reutilizar), detalle, edición, eliminación/desactivación, rutas, servicios Angular, formularios, guards, mensajes, UX.
Testing: Gherkin, Step Definitions, integración PostgreSQL, Vitest, Playwright, evidencias, documentación.

### Integrante 3 — CRUD full-stack `task_assignment`
Backend: SELECT individual, UPDATE motivo (preservar CREATE, listado, DELETE), reglas, integridad, autorización, DTOs.
Frontend/UI: listado, creación (reutilizar), detalle, edición de motivo, revocación, integración con tareas, servicios Angular, formularios, UX.
Testing: Gherkin, Step Definitions, integración PostgreSQL, Vitest, Playwright, evidencias, documentación.

### Revisión cruzada
- Integrante 1 revisa arquitectura y pruebas.
- Integrante 2 revisa escenarios de Integrante 3.
- Integrante 3 revisa escenarios de Integrante 2.
- Los tres revisan los CRUD UI.
- Los tres participan en la regresión.
- Cada integrante prepara dos preguntas para la defensa.

El trabajo de frontend no es secundario; los integrantes 2 y 3 son responsables de completar sus CRUD de extremo a extremo.

---

## 24. Plan por etapas y cronograma

Fecha de planificación: 25/11/2026. Entrega: 02/12/2026.

| Etapa | Fechas | Objetivo | Responsable principal | Apoyo | Dependencias | Archivos futuros (propuestos) | Entregables | Criterio de salida | Riesgos |
|---|---|---|---|---|---|---|---|---|---|
| 0 — Auditoría y línea base | 25/11 | Confirmar estado, métricas base | Integrante 1 | Los tres | — | docs/EC4 (este documento) | Línea base documentada | Auditoría completa | Tiempo |
| 1 — BDD y proceso principal | 25–26/11 | Runner Cucumber + proceso principal | Integrante 1 | — | Etapa 0 | `backend/src/test/.../bdd/`, `resources/features/ec4/proceso-principal.feature` | 1 escenario BDD ejecutable | Proceso principal Happy Path pasa | Compatibilidad Cucumber/Spring |
| 2 — CRUD full-stack app_user | 26–28/11 | Completar CRUD usuarios | Integrante 2 | — | Etapa 0 | Endpoints users, UI /users, DTOs, .feature, steps | CRUD app_user completo | 9 escenarios BDD app_user + UI | FK al eliminar usuario |
| 3 — CRUD full-stack task_assignment | 26–28/11 | Completar CRUD asignaciones | Integrante 3 | — | Etapa 0 | GET individual, PUT motivo, UI detalle/editar, .feature, steps | CRUD task_assignment completo | 9 escenarios BDD task_assignment + UI | Clave compuesta duplicada |
| 4 — Integración y regresión | 29–30/11 | Ejecutar todo junto, corregir | Integrante 1 | Los三 | Etapas 1, 2, 3 | — | Regresión verde | Todos los tests pasan | Conflictos de merge |
| 5 — CI/CD y cobertura | 30/11–01/12 | Pipeline GitHub Actions + cobertura | Integrante 1 | Los dos | Etapa 4 | `.github/workflows/ec4-quality.yml` | Pipeline funcionando, cobertura ≥85% | CI verde en PR | Seguridad CI |
| 6 — Documentación y cierre | 01–02/12 | Informe común + PDFs individuales | Los tres | — | Etapa 5 | docs/EC4, informes PDF | Documentación completa | Entrega lista | Tiempo |

Los CRUD de `app_user` y `task_assignment` se desarrollan en paralelo (Etapas 2 y 3).

---

## 25. Criterios de aceptación

| ID | Criterio | Medición |
|---|---|---|
| CA-01 | Proceso principal con 1 Happy Path BDD ejecutable y aprobado | Ejecución Cucumber |
| CA-02 | `app_user`: INSERT (2H+1U), UPDATE (2H+1U), DELETE (1H), SELECT individual (1H), SELECT listado (1H) en BDD | Ejecución Cucumber |
| CA-03 | `task_assignment`: INSERT (2H+1U), UPDATE (2H+1U), DELETE (1H), SELECT individual (1H), SELECT listado (1H) en BDD | Ejecución Cucumber |
| CA-04 | Ambos CRUD completos en backend (PostgreSQL, persistencia, dominio, aplicación, controller, DTOs, API REST) | Revisión de código |
| CA-05 | Ambos CRUD completos en frontend/UI (servicios, modelos, componentes, formularios, rutas, guards, navegación) | Revisión de UI |
| CA-06 | Cobertura ≥ 85 % en todas las métricas configuradas (backend JaCoCo, frontend Vitest) | Reportes JaCoCo/Vitest |
| CA-07 | CI de GitHub Actions ejecuta backend, frontend, BDD, cobertura y build | Pipeline verde |
| CA-08 | Evidencias de ejecución de cada escenario (resultados) | Capturas/logs |
| CA-09 | Documentación común + informe PDF por integrante | Archivos entregados |
| CA-10 | Defensa: dos preguntas por integrante respondidas | Presentación |

---

## 26. Mapeo de tareas a la rúbrica

| Criterio rúbrica | Puntaje | Actividades previstas | Responsable | Etapa | Evidencias | Criterios de aceptación | Riesgos |
|---|---:|---|---|---|---|---|---|
| Documento en base a contenido mínimo | 20 | Este documento + informes PDF individuales | Los tres | 6 | `docs/EC4/...`, PDFs | Contenido mínimo cubierto | Tiempo |
| Pruebas de integración proceso principal con BDD | 15 | Runner Cucumber, proceso principal Happy Path, integración PostgreSQL | Integrante 1 | 1, 4 | `.feature`, pasos, logs | 1 escenario aprobado | Compatibilidad Cucumber/Spring |
| CRUD Inserts y Updates (dos tablas) | 18 | Endpoints POST/PUT, DTOs, reglas, UI creación/edición, BDD 2H+1U por operación | Integrante 2, 3 | 2, 3, 4 | Código, `.feature`, pasos | 4 escenarios INSERT + 4 UPDATE aprobados | FK, clave compuesta |
| CRUD Delete y Select (dos tablas) | 12 | Endpoints GET/DELETE, UI detalle/listado/eliminación, BDD Happy Path | Integrante 2, 3 | 2, 3, 4 | Código, `.feature`, pasos | 2 DELETE + 4 SELECT aprobados | FK RESTRICT |
| Medición de cobertura | 15 | JaCoCo backend, Vitest frontend, umbrales 85 % | Integrante 1 | 5 | Reportes JaCoCo/Vitest | ≥85 % todas métricas | Caída de cobertura |
| Defensa y presentación | 20 | Dos preguntas por integrante, respuestas técnicas | Los tres | 6 | Presentación | Respuestas correctas | Tiempo |

---

## 27. Preparacion para defensa

Dos preguntas probables por integrante y sus respuestas técnicas:

**Integrante 1 (BDD, proceso principal, integración, CI/CD):**
1. P: ¿Por qué eligieron Cucumber-JVM y no ReqnRoll?  
   R: El backend es Java 21 + Spring Boot; ReqnRoll es para C#. Cucumber-JVM es compatible con el stack existente (JUnit Platform, Spring Test) y no requiere migrar el backend.
2. P: ¿Cómo garantizan que las pruebas de integración son reales y no unitarias con mocks?  
   R: Usan `@SpringBootTest` con `RANDOM_PORT`, `RestTemplate` contra el servidor real, `JdbcTemplate` contra PostgreSQL `gestor_tareas_test`, y limpian los datos entre escenarios sin mocks de repositorio.

**Integrante 2 (CRUD `app_user`, seguridad, UI, pruebas):**
1. P: ¿Cómo manejan la eliminación de usuarios dado que `task_assignment` tiene FK `ON DELETE RESTRICT`?  
   R: Se valida que no existan asignaciones vigentes ni referencias antes de eliminar; se usa un usuario de prueba no referenciado. Se justifica si se prefiere desactivación lógica (`is_active = false`) en lugar de eliminación física.
2. P: ¿Cómo evitan exponer el hash de contraseña en la UI y en las respuestas?  
   R: Los DTOs de respuesta (`UserResponse`, `UserDetailResponse`) excluyen `password_hash`; las pruebas BDD verifican que la respuesta no contenga claves sensibles.

**Integrante 3 (CRUD `task_assignment`, clave compuesta, reglas, UI, pruebas):**
1. P: ¿Cómo funciona la clave compuesta de `task_assignment` y qué operación se actualiza?  
   R: La PK es `(task_id, worker_user_id)`. El `PUT` actualiza únicamente el campo `reason` (máximo 500, normalizado, valor predeterminado `Asignación de Personal`); no se modifica tarea, trabajador, fecha ni asignador.
2. P: ¿Qué reglas de autorización aplican a las asignaciones?  
   R: Solo el Encargado del proyecto o el Jefe pueden asignar/revocar (`AssignmentPolicy.requireCanAssign`); el trabajador debe existir y estar activo; no se permite autoasignación (`requireNotSelf`); el proyecto debe estar abierto (`requireOpen`).

---

## 28. Referencias y anexos técnicos

### 28.1 Referencias académicas
- Documento 1: GCS T4-1.pdf — Automatización en la Agilidad — Parte 1 (M. Sc. Ing. Joel Reynaldo Alánez Durán).
- Documento 2: GCS T4-2.pdf — BDD + Gherkin — Parte 2 (M. Sc. Ing. Joel Reynaldo Alánez Durán).
- Documento 3: Enunciado oficial EC4 — Elemento de Competencia 4 — Entregable Único.

### 28.2 Archivos auditados (solo lectura)
- `backend/pom.xml` (Spring Boot 4.1.0, Java 21, JaCoCo 0.8.15)
- `backend/src/main/resources/application.yml` (puerto 8081, `ddl-auto: validate`)
- `backend/src/main/java/com/hardcoders/taskmanager/adapter/in/web/user/UserController.java`
- `backend/src/main/java/com/hardcoders/taskmanager/adapter/in/web/task/HierarchyController.java`
- `backend/src/main/java/com/hardcoders/taskmanager/adapter/out/persistence/entity/AppUserEntity.java`
- `backend/src/main/java/com/hardcoders/taskmanager/adapter/out/persistence/entity/TaskAssignmentEntity.java`
- `backend/src/main/java/com/hardcoders/taskmanager/adapter/out/persistence/entity/TaskAssignmentId.java`
- `backend/src/main/java/com/hardcoders/taskmanager/adapter/out/persistence/repository/AppUserJpaRepository.java`
- `backend/src/main/java/com/hardcoders/taskmanager/adapter/out/persistence/repository/TaskAssignmentJpaRepository.java`
- `backend/src/main/java/com/hardcoders/taskmanager/adapter/out/persistence/UserWriterAdapter.java`
- `backend/src/main/java/com/hardcoders/taskmanager/adapter/out/persistence/UserDirectoryAdapter.java`
- `backend/src/main/java/com/hardcoders/taskmanager/adapter/out/persistence/AssignmentPersistenceAdapter.java`
- `backend/src/main/java/com/hardcoders/taskmanager/application/usecase/UserCommandService.java`
- `backend/src/main/java/com/hardcoders/taskmanager/application/usecase/AssignmentCommandService.java`
- `backend/src/main/java/com/hardcoders/taskmanager/application/port/in/ManageUsersUseCase.java`
- `backend/src/main/java/com/hardcoders/taskmanager/application/port/in/ManageAssignmentsUseCase.java`
- `backend/src/main/java/com/hardcoders/taskmanager/domain/policy/AssignmentPolicy.java`
- `backend/src/main/java/com/hardcoders/taskmanager/domain/model/TaskAssignment.java`
- `backend/src/test/java/com/hardcoders/taskmanager/integration/PostgresIntegrationTest.java`
- `frontend/package.json` (Angular 22.1.x, TS 6.0.x, Vitest 4.1.x, Playwright 1.58.x)
- `frontend/vitest.config.ts` (umbrales 85 %)
- `frontend/playwright.config.ts`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/core/services/user.service.ts`
- `frontend/src/app/core/services/wbs.service.ts`
- `frontend/src/app/core/models/user.ts`
- `frontend/src/app/core/models/api.ts`
- `frontend/src/app/features/users/user-form/user-form.ts`
- `frontend/src/app/features/tasks/assignment-dialog/assignment-dialog.ts`
- `frontend/e2e/users/user-create.spec.ts`
- `frontend/e2e/auth/auth.spec.ts`
- `frontend/e2e/tasks/main-flow.spec.ts`
- `frontend/e2e/fixtures/helpers.ts`
- `database/migrations/V001__initial_schema.sql` a `V006__task_assignment_reason.sql`
- `database/docker/init-dev-db.sql`, `seed-dev-users.sql`
- `docker-compose.yml`
- `README.md`

### 28.3 Glosario de estado
- **Implementado:** verificado en el código actual.
- **Parcial:** implementación incompleta (falta endpoint o UI).
- **No implementado:** ausente tras auditoría.
- **Propuesto:** diseño futuro para la fase de implementación.
- **Pendiente de confirmar:** no existe evidencia suficiente.

---

## 29. Conclusión y cierre

Esta ejecución fue exclusivamente de análisis y planificación. Se confirmó:
- Rama `Refactor-E2`, HEAD `18334fd` (coincide con referencia).
- No se modificó código, configuración, dependencias, endpoints, componentes, pruebas, workflows ni base de datos.
- No se ejecutaron suites de pruebas ni se generaron reportes nuevos.
- El único archivo creado es `docs/EC4/ANALISIS_Y_PLAN_IMPLEMENTACION_EC4.md`.

El documento brinda la especificación completa para que una segunda ejecución inicie la implementación del EC4: dos CRUD full-stack (`app_user` y `task_assignment`), BDD con Cucumber-JVM (19 escenarios), pruebas de integración contra PostgreSQL real, cobertura mínima 85 % (JaCoCo/Vitest), propuesta de CI/CD con GitHub Actions, distribución entre tres integrantes, cronograma del 25/11 al 02/12/2026, riesgos, criterios de aceptación y preparación para defensa, todo alineado con la rúbrica de 100 puntos.

**EC4 no está implementado.** Este es solo el plan.
