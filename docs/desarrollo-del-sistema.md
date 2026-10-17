# Desarrollo del sistema — Gestor de Tareas Personales (Hard Coders)

> Versión del documento: estado verificado a 17/10/2026.
> Refleja el **código implementado y ejecutado**, no el plan original de reestructuración. El proceso principal está implementado y cubierto por tests automatizados en backend, frontend y un smoke E2E contra PostgreSQL real.

---

## 1. Contexto y arquitectura

**Gestor de Tareas Personales Hard Coders** es una aplicación monolítica basada en la arquitectura hexagonal que gestiona el ciclo de vida de proyectos y su trabajo asociado siguiendo Metodología Ágil similar a SCRUM (Epica → Historias → Criterios de Aceptación).

### 1.1 Arquitectura de contenedores (C4)

```
Usuario (Jefe / Encargado / Personal)
        │  HTTPS
        ▼
┌─────────────────────┐       JSON/HTTPS        ┌──────────────────────────┐
│  Angular 22 SPA     │  ←→  /api/*             │  Spring Boot 4.1 (API)     │
│  (standalone, Vite) │  ─────────────────────► │  Java 24 / 21, Maven      │
└─────────────────────┘                          │  Hexagonal                 │
        ▲                                         │  Spring Security + JWT   │
        │ dev-proxy (proxy.conf.json)             └──────────────────────────┘
        │                                         JDBC / JPA (Hibernate)
        ▼                                                 │
  http://localhost:4200                              ┌────▼────┐
  http://localhost:8080/api/...                      │ PostgreSQL 18 │
                                                     │ gestor_tareas │
                                                     │ task_manager  │
                                                     └───────────┘
```

### 1.2 Arquitectura hexagonal interna (backend)

```
domain/                 →        application/        →        adapter/
   model/                      port/in/                    in/web/
   policy/                     usecase/                    in/security/
   port/out/                   result/                     out/persistence/
                               service/                    out/email/
```

- **Dominio puro**: `domain/model` (Actor, Proyecto, Etapa, Actividad, Tarea, Subtarea, WorkStatus, ProjectStatus…), `domain/policy` (reglas de negocio sin frameworks: `ProjectAccessPolicy`, `CompletionPolicy`, `BudgetPolicy`, `DelegationPolicy`, `WbsHierarchyPolicy`, `AssignmentPolicy`) y `domain/port/out`.
- **Casos de uso**: orquestan transacciones (`@Transactional`) y delegan en políticas. `TaskCommandService`, `ProjectQueryService`, `ProjectCommandService`, `HierarchyCommandService`, `AssignmentCommandService`.
- **Adaptadores de entrada**: `adapter/in/web/{project,task}/...Controller.java` (REST) + `adapter/in/security/` (JWT filter, `SecurityConfiguration`, `CurrentActor` resolver desde la base de datos).
- **Adaptadores de salida**: `adapter/out/persistence/` (entidades JPA, repositorios Spring Data, mappers) y trigger/DDL en `database/migrations/V001–V004`.

### 1.3 Modelo de datos (jerarquía V4)

```
Proyecto  ──►  Etapa  ──►  Actividad  ──►  Tarea  ──►  Subtarea
   │            │             │              │            │
   │            │             │              │            └─ WorkStatus {PENDIENTE, EN_PROGRESO, COMPLETADA, CANCELADA}
   │            │             │              └─ WorkStatus + budget + duración
   │            │             └─ WorkStatus
   │            └─ WorkStatus
   │
   └─ ProjectStatus {PLANIFICADO, EN_EJECUCION, EN_PAUSA, FINALIZADO, CANCELADO}
      + budgetBob / allocatedBudgetBob + plan_history
```

Relaciones y auditoría (`database/migrations/V004__v4.sql`):
- Un **Proyecto** tiene muchas etapas; una etapa muchas actividades; una actividad muchas tareas; una tarea muchas subtareas.
- `task_status_history` registra cada cambio de estado con `from_status → to_status`, `change_reason` y `changed_at` (trigger `trg_tasks_status_history`, `DEFERRABLE INITIALLY DEFERRED` para atomicidad al COMMIT).
- `task_assignment` (clave compuesta `task_id + worker_user_id`) asocia el **Personal** a las tareas con motivo.
- `project_plan_history` registra revisiones de plan/budget; la revisión 1 se crea atómicamente al crear el proyecto.

### 1.4 Actores y seguridad

- `Actor` (`domain/model/Actor.java`) es un **record** `(userId, chief, active)` resuelto en cada request desde la base (no desde el cliente): `AppUser.isActive()` debe ser true y el rol se comprueba con `isActiveChief()`.
- **Jefe** (`chief=true`): crea/gestiona proyectos, cambia presupuesto, delega Encargados y **ejecuta override** (US1.3) sobre tareas ajenas registrando motivo.
- **Encargado** (`manager_user_id` del proyecto): ve y edita solo sus proyectos; construye la WBS; asigna personal.
- **Personal**: ve y ejecuta solo lo que le está asignado.

La autorización se resuelve 100 % server-side y el token JWT contiene el `userId`; el resto de permisos se consultan a la base por cada request.

---

## 2. Proceso principal implementado

El proceso principal del gestor es: **Crear proyecto → Construir WBS → Asignar Personal → Ejecutar tareas → Ver progreso**. A continuación cada fase con su API real y las reglas de negocio aplicadas.

### 2.1 Fase 1 — Crear proyecto y plan (US1.1) · Actor: Jefe

1. El **Jefe** crea un proyecto (`POST /api/projects`, `ProjectController.create`) con budget y duración. El trigger crea `project_plan_history` revisión 1 de forma atómica (`@Transactional` + trigger `DEFERRABLE`).
2. Budget negativo → `400`; budget < suma de allocations → `409 CONFLICT`; Encargado intenta crear/editar → `403`.
3. El Jefe delega un Encargado activo (`POST /api/projects/{id}/delegate`), registrado con `delegated_by` atribución inmutable; auto-delegar → `403`.

**Políticas**: `BudgetPolicy`, `DelegationPolicy`.

### 2.2 Fase 2 — Construir WBS (US2.2) · Actor: Encargado

Dentro de un proyecto delegado, el Encargado construye la jerarquía **Etapa → Actividad → Tarea → Subtarea**. API (`HierarchyController` / `TaskController`):

| Acción | Endpoint |
|---|---|
| Crear etapa | `POST /api/stages` |
| Renombrar/editar etapa | `PUT /api/stages/{id}` |
| Cambiar estado etapa/actividad | `PATCH /api/stages/{id}/status`, `/api/activities/{id}/status` |
| Crear actividad | `POST /api/activities` |
| Crear tarea en actividad | `POST /api/tasks` |
| Crear subtarea de tarea | `POST /api/tasks/{id}/subtasks` |
| Editar tarea/subtarea | `PUT /api/tasks/{id}` / `PUT /api/subtasks/{id}` |
| Cambiar de actividad (mis proyecto) | `PUT /api/tasks/{id}/activity` |
| Ajustar presupuesto/duración con motivo | `PUT /api/tasks/{id}/plan` |

Restricciones (`WbsHierarchyPolicy`):
- Parent cross-project → `404` (no se revela existencia ajena).
- La creación de una Tarea inserta `task`, `task_plan_history` rev-1 y `task_status_history` inicial atómicamente (trigger `trg_task_requires_histories` verifica al COMMIT).
- Campos financieros en DTOs de tarea están presentes pero el control de acceso los limita a Jefe/Encargado.

### 2.3 Fase 3 — Asignar Personal (US2.3) · Actor: Encargado

- `POST /api/assignments` ({ workerUserId, reason }) crea una `task_assignment`.
- Worker inactivo → `400`; asignación duplicada (`task_id + worker_user_id`) → `409`; **autoasignación** (worker == actor) → `403 Forbidden "No puedes asignarte trabajo a ti mismo"`.
- `DELETE /api/assignments/{taskId}/{workerId}` retira la asignación.
- Las tareas aparecen en la lista del worker asignado.

**Políticas**: `AssignmentPolicy.requireEligibleWorker`, `requireNotSelf`.

### 2.4 Fase 4 — Ejecutar tareas (US3.2 / US1.3) · Actor: Personal (y override de Jefe)

- El **Personal** ejecuta sobre lo que le está asignado: `POST /subtasks/{id}/complete`, `.../start`, `.../pause`, `.../reopen` (cuerpo `{ version, reason? }`).
- `WorkStatus` transita `PENDIENTE → EN_PROGRESO → COMPLETADA`; `cancel` archiva al historial. Bloqueo optimista con `version`.
- **Override (US1.3)**: el **Jefe** puede ejecutar cualquier tarea/subtarea aun sin estar asignada, siempre que no esté asignada a otro trabajador (`TaskCommandService.load` → `403` si existe pero no es suya; `404` si no existe, para no filtrar proyectos ajenos). El motivo del Jefe se normaliza en `overrideReason` y se registra como `"Override del Jefe (US1.3): <motivo>"` en `task_status_history`.
- `complete` requiere `CompletionPolicy.requireCanComplete(actor, assigned)` → `403` si no tiene permiso.

### 2.5 Fase 5 — Ver progreso (US3.3) · Actor: quien tenga acceso

- `GET /api/projects/{id}/progress` (`ProgressController`) devuelve el árbol `Proyecto → Etapa → Actividad → Tarea → Subtarea` con conteos de hoja `COMPLETADA/total`.
- Fórmula de hoja: `Progress = completedLeafChildren / totalLeafChildren` (proyecto con 0 tareas → 0 %; nunca doble cuenta de task + subtasks).
- 100 % de progreso ≠ cierre automático del proyecto (estado gestionado explícitamente).

### 2.6 Reglas transversales de aislamiento (US2.1)

- El **Encargado** lista proyectos → solo `manager_user_id = actor`; un proyecto ajeno responde `404` (no se filtra su existencia).
- El **Personal** lista proyectos → `[]` (lista vacía).
- El **Jefe** ve todo. Todo filtrado server-side (no hay fugas de conteo/paginación).

---

## 3. Autorización (resumen de reglas implementadas)

| Operación | Jefe | Encargado (proyecto) | Personal |
|---|---|---|---|
| Crear/editar proyecto y presupuesto | ✅ | ❌ 403 | ❌ 403 |
| Delegar / cambiar Encargado | ✅ | ❌ 403 | ❌ |
| Construir WBS (etapas/actividades/tareas) | ✅ (override) | ✅ | ❌ |
| Asignar / retirar Personal | ✅ (override) | ✅ | ❌ |
| Ejecutar tareas/subtareas | ✅ (override, con motivo) | ✅ si asignada | ✅ solo asignadas |
| Ver proyectos ajenos / listados | ✅ todo | ❌ 404 | ❌ [] |
| Ver progreso del proyecto | ✅ | ✅ (propios) | ✅ (asignadas) |

---

## 4. Contrato API (puntos clave V4)

- Autenticación: `POST /api/auth/login` → devuelve JWT; el resto de endpoints exige `Authorization: Bearer <token>`.
- Concurrencia: los writes llevan `version` (bloqueo optimista); conflicto → `409`.
- Estados: `WorkStatus` (PENDIENTE/EN_PROGRESO/COMPLETADA/CANCELADA) y `ProjectStatus` (PLANIFICADO/EN_EJECUCION/EN_PAUSA/FINALIZADO/CANCELADO). Transiciones inválidas rechazadas por el dominio.
- Errores: `400` validación, `403` Forbidden, `404` NotFound (también para accesos ajenos, sin revelar existencia), `409` conflicto.
- El frontend (`frontend/src/app/core/services/task.service.ts`) expone `TaskService` como fachada: `listTasks`, `listAssignedTasks`, `createTask`, `updateTask`, `updatePlan`, `changeActivity`, `complete/start/pause/reopen`, `listCategories(stageId)` y `deleteTask` (cancela). La actividad ("categoría") se pasa explícita por `activityId`; **ya no existe el workspace deducido del primer proyecto**.

---

## 5. Evidencia de implementación y ejecución

### 5.1 Tests automatizados (verificados)

| Capa | Comando | Resultado |
|---|---|---|
| Backend (JUnit 5 + Mockito + MockMvc + Testcontainers) | `cd backend && mvn clean verify` | **227 tests, 0 fallos, 0 errores, 0 omitidos** · BUILD SUCCESS |
| Frontend (Vitest, Angular 22) | `cd frontend && npx ng test --watch=false` | **15 archivos / 229 tests, 0 fallos** |
| Build producción frontend | `npx ng build --configuration production` | Sin errores ni warnings de presupuesto CSS |
| Smoke E2E (backend + PostgreSQL reales) | flujo manual contra `localhost:8080` | **23/23 OK** (ver 5.3) |
| Smoke manual (14 chequeos en caliente) | — | **14/14 OK** |

### 5.2 Cobertura dinámica (JaCoCo / Vitest)

| Artefacto | Métrica | Valor | Umbral |
|---|---|---:|---|
| Backend (JaCoCo) | Líneas | **95,15 %** (2396/2518) | ≥ 70 % |
| Backend (JaCoCo) | Ramas | **83,78 %** (470/561) | ≥ 70 % |
| Frontend (Vitest v8) | Líneas | **94,81 %** (2448/2582) | ≥ 70 % |
| Frontend (Vitest v8) | Ramas | **88,95 %** (878/987) | ≥ 70 % |

Los métodos clave de US1.3/US2.1 están al 100 % de líneas y ramas, salvo caminos de error menos frecuentes reportados como parciales en `docs/pruebas-unitarias/US13-US21-informe.md`.

### 5.3 Smoke E2E principal (Proceso principal end-to-end)

1. Login Jefe → creación de proyecto (201) + revisión 1 atómica.
2. US2.2: crear etapa → renombrar con `version` → crear actividad → pasar a `EN_PROGRESO` → crear tarea en la actividad.
3. US2.3: listar Personal, asignar, listar asignaciones, **asignación duplicada rechazada (400)**.
4. US3.1: el Personal ve su tarea en `/tasks/assigned` y la respuesta **no trae campos financieros** (`assignedWorkerIds`, budget omitidos).
5. US3.2: el Personal inicia y completa la tarea; estado registrado en `task_status_history`.
6. US3.3: árbol de progreso Proyecto→Etapa→Actividad al **100 %** sobre 1 unidad hoja.
7. US2.3: retirar la asignación.
8. US2.1: el Encargado de **otro** proyecto recibe **404** (aislamiento sin revelar existencia).

### 5.4 Comandos de arranque / verificación

```bash
# Backend (raíz repo)
cd backend
mvn clean verify                                  # tests + JaCoCo
mvn spring-boot:run                               # -Dserver.port=8081 si 8080 ocupado

# Frontend (raíz repo)
cd frontend
npm ci
npx ng test --watch=false --coverage              # Vitest + cobertura v8
npx ng build --configuration production           # build productivo

# Base de datos
docker start gestor-tareas-db
docker cp database/docker/seed-dev-users.sql gestor-tareas-db:/tmp/seed.sql
docker exec -i gestor-tareas-db psql -U postgres -d gestor_tareas -f /tmp/seed.sql
```

Entornos locales: frontend `http://localhost:4200` (proxy `/api` → `localhost:8080`), backend `http://localhost:8080/api/...`, DB `gestor_tareas` esquema `task_manager`. Fixtures: `jefe@gestor.local`, `encargado1@gestor.local`, `encargado2@gestor.local`, `personal@gestor.local`, `inactivo@gestor.local` (password `Gestor2026!`; el inactivo no autentica).

---

## 6. Decisiones y aprendizaje del proceso

- `mvn clean` es obligatorio: dejar clases obsoletas en `target/` produce `NoUniqueBeanDefinitionException` por beans `@Component` duplicados (ej. el antiguo `LocalUserActorAdapter` vs. `SecurityCurrentActorAdapter`).
- El proxy `/api` del dev-server debe apuntar al backend real; el token se obtiene de `/api/auth/login` y el Jefe/Encargado/Personal se resuelve desde la base por `userId`.
- El 80 % de los incidentes del ciclo de desarrollo fueron de infraestructura (puerto 8080 ocupado por proceso `java` huérfano; `npm ci` bloqueado por `esbuild`/`ng serve` activo), no de lógica de negocio.
- El aislamiento US2.1 se impone devolviendo `404` (no `403`) para proyectos/tareas ajenas, evitando fugas de existencia.

---

## 7. Archivos de interés

- Políticas: `backend/src/main/java/com/hardcoders/taskmanager/domain/policy/{CompletionPolicy,ProjectAccessPolicy,BudgetPolicy,DelegationPolicy,WbsHierarchyPolicy,AssignmentPolicy}.java`
- Casos de uso: `backend/src/main/java/com/hardcoders/taskmanager/application/usecase/{TaskCommandService,ProjectQueryService,ProjectCommandService,HierarchyCommandService,AssignmentCommandService}.java`
- Controladores: `backend/src/main/java/com/hardcoders/taskmanager/adapter/in/web/{project,task}/`
- Seguridad: `backend/src/main/java/com/hardcoders/taskmanager/adapter/in/security/`
- API frontend: `frontend/src/app/core/models/api.ts`, `frontend/src/app/core/services/task.service.ts`
- Migraciones: `database/migrations/V001__initial_schema.sql` … `V004__v4.sql`
- Evidencias de pruebas: `docs/pruebas-unitarias/`
