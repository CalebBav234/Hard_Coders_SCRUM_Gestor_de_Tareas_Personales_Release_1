# API V4 y adaptación del frontend

La interfaz funciona como antes (lista con cronómetro, subtareas, categorías, completadas, historial y búsqueda), pero ahora sobre la base migrada V004, con backend en arquitectura hexagonal y con **ámbito explícito por proyecto**, como propone `.kilo/plans/hexagonal_restructure_plan.md`.

## Estructura del backend

```
com.hardcoders.taskmanager
├── domain                    # Java puro: sin Spring, JPA ni Jackson
│   ├── model                 # Task, Subtask, Activity, Stage, Project, WorkStatus, TaskPriority,
│   │                         #   StatusChange, PlanRevision, TaskAssignment, ProgressNode, TimeTracking...
│   ├── exception             # NotFound, InvalidStatusTransition, VersionConflict, OpenSubtasks, DomainValidation
│   ├── policy                # BudgetPolicy, DelegationPolicy, CompletionPolicy, ProgressPolicy,
│   │                         #   ProjectAccessPolicy, AssignmentPolicy, WbsHierarchyPolicy
│   └── port/out              # TaskRepository, SubtaskRepository, HierarchyRepository,
│                             #   AssignmentRepository, ProjectRepository, UserDirectory, CurrentActor
├── application               # Java puro: casos de uso
│   ├── port/in               # Manage/Query Tasks, Subtasks, Projects, Hierarchy, Assignments, Progress
│   ├── command | result      # Comandos de entrada y resultados
│   └── usecase               # Task/Subtask/Project/Hierarchy/Assignment/Progress ...
├── adapter
│   ├── in/web                # Controladores REST, DTOs con validación, GlobalExceptionHandler, CORS
│   ├── in/security           # JWT, filtro, SecurityCurrentActorAdapter
│   └── out/persistence       # Entidades JPA (*Entity), repositorios Spring Data, adaptadores de los puertos
└── configuration             # Raíz de composición; envuelve cada caso de uso en una transacción
```

- Los casos de uso no llevan anotaciones de Spring: `configuration/TransactionalUseCases` los envuelve en una transacción. Así los triggers diferidos de V004 (historiales obligatorios) se validan al confirmar.
- `HexagonalArchitectureTest` falla si `domain` o `application` importan Spring, Jakarta, Hibernate, Jackson o los adaptadores.

## Cómo se representa el modelo anterior en V4

| Interfaz (como antes) | V4 |
|---|---|
| Tarea | `task` dentro de una `activity` de una `stage` de un `project` |
| Subtarea | `subtask` de la tarea (un solo nivel) |
| Categoría | `activity` (nivel 3 de la jerarquía); "Sin categoría" es una actividad más |
| INACTIVA / ACTIVA / TERMINADA | PENDIENTE / EN_PROGRESO / COMPLETADA |
| Eliminar (borrado lógico) | Pasar a CANCELADA; queda en el historial |
| Cronómetro (`total_active_seconds`) | Se calcula del historial de estados: cada tramo EN_PROGRESO es un segmento. `actual_duration_minutes` guarda el total en minutos |
| `version` (bloqueo optimista) | Token = `updated_at` en microsegundos (el trigger lo renueva en cada cambio) |
| Presupuesto / duración de la tarea | `allocated_budget_bob` y `estimated_duration_minutes`; cada cambio deja una revisión en `task_plan_history` |

**Ámbito de trabajo (US2.2).** Ya **no** se deduce el "primer proyecto": se eliminó `WorkspacePersistenceAdapter.currentWorkspace` y `WorkspaceRepository`. Crear una tarea exige `activityId`, y todas las lecturas reciben `projectId`. El backend comprueba en cada operación que la etapa/actividad/tarea pertenezca al proyecto indicado y que el actor del token pueda verlo o gestionarlo; un identificador de otro proyecto responde `404`.

- **Jefe:** ve todos los proyectos, crea, cambia presupuesto/duración, estados y delega.
- **Encargado:** ve y gestiona solo los proyectos que le fueron delegados; construye etapas, actividades, tareas, asigna Personal.
- **Personal:** `GET /api/projects` devuelve `[]` (no puede ver proyectos) y solo trabaja con lo que tiene asignado en `/api/tasks/assigned`, cuya respuesta **no incluye** `allocatedBudgetBob` ni `estimatedDurationMinutes` (AC-US3.1-02). Puede iniciar/pausar/completar/reabrir esas tareas y sus subtareas (AC-US3.2-01); si no está asignado recibe `403`.
- **Usuario:** todas las rutas exigen token JWT (ver [epica-1.md](epica-1.md)); los historiales se atribuyen al usuario que inició sesión.
- **Fuera de V4:** las relaciones entre tareas (US-18) y la prioridad y el cronómetro de las subtareas, porque el modelo V4 no tiene dónde guardarlos.

## Endpoints

| Método | URL | Cuerpo | Respuesta |
|---|---|---|---|
| POST | `/api/auth/login` | `{ email, password }` | `200` `{ token, expiresAt, user }`; `401 INVALID_CREDENTIALS` si el correo o la contraseña no coinciden; `403 USER_INACTIVE` si la cuenta existe pero está desactivada |
| GET | `/api/auth/me` | — | `CurrentUserResponse` del token |
| GET | `/api/projects` | — | Proyectos visibles para el actor (Jefe: todos; Encargado: los delegados; Personal: `[]`) |
| GET | `/api/projects/{id}` | — | `ProjectResponse` o `404` si es de otro Encargado |
| POST | `/api/projects` | `{ name, description?, managerUserId, budgetBob, estimatedDurationMinutes }` | `201` `ProjectResponse` (solo Jefe) |
| PUT | `/api/projects/{id}/plan` | `{ budgetBob, estimatedDurationMinutes, reason, version }` | `ProjectResponse`; `409` si el presupuesto queda por debajo de lo asignado |
| PUT | `/api/projects/{id}/status` | `{ status, reason?, version }` | `ProjectResponse` |
| POST | `/api/projects/{id}/delegate` | `{ managerUserId, reason, version }` | `ProjectResponse` (solo Jefe) |
| GET | `/api/projects/eligible-managers` | — | Usuarios Jefe/Encargado activos |
| GET | `/api/projects/{projectId}/stages` | — | `StageResponse[]` |
| POST | `/api/projects/{projectId}/stages` | `{ name, description? }` | `201` `StageResponse` (Encargado del proyecto o Jefe) |
| PUT | `/api/stages/{stageId}` | `{ name, description?, version }` | `StageResponse` (renombrar/redescribir) |
| PUT | `/api/stages/{stageId}/status` | `{ status, version }` | `StageResponse` (PENDIENTE/EN_PROGRESO/COMPLETADA/CANCELADA) |
| GET | `/api/stages/{stageId}/activities` | — | `ActivityResponse[]` |
| POST | `/api/stages/{stageId}/activities` | `{ name, description? }` | `201` `ActivityResponse` |
| PUT | `/api/activities/{activityId}` | `{ name, description?, version }` | `ActivityResponse` (renombrar/redescribir) |
| PUT | `/api/activities/{activityId}/status` | `{ status, version }` | `ActivityResponse` (PENDIENTE/EN_PROGRESO/COMPLETADA/CANCELADA) |
| GET | `/api/tasks?projectId=&q=` | — | `TaskResponse[]` no canceladas del proyecto, con `subtasks` |
| GET | `/api/tasks/assigned?q=` | — | `AssignedTaskResponse[]` (solo lo asignado al usuario del token) |
| GET | `/api/tasks/{id}` | — | `TaskResponse` si es visible o está asignado |
| POST | `/api/tasks` | `{ activityId, title, priority?, allocatedBudgetBob, estimatedDurationMinutes }` | `201` `TaskResponse` |
| PUT | `/api/tasks/{id}` | `{ title, description, priority, version }` | `TaskResponse` |
| PUT | `/api/tasks/{id}/activity` | `{ activityId, version }` | `TaskResponse` (misma jerarquía de proyecto) |
| PUT | `/api/tasks/{id}/plan` | `{ allocatedBudgetBob, estimatedDurationMinutes, reason, version }` | `TaskResponse` |
| POST | `/api/tasks/{id}/start` · `/pause` · `/complete` · `/reopen` | `{ version, reason? }` | `TaskResponse` |
| DELETE | `/api/tasks/{id}?version=` | — | `204` (CANCELADA) |
| GET | `/api/tasks/history?projectId=&q=` | — | `TaskHistoryResponse[]` (COMPLETADA o CANCELADA, con eventos) |
| POST | `/api/tasks/{id}/subtasks` | `{ title, description? }` | `201` `SubtaskResponse` |
| GET | `/api/tasks/{id}/assignments` | — | `TaskAssignmentResponse[]` |
| POST | `/api/tasks/{id}/assignments` | `{ workerUserId, reason? }` | `TaskAssignmentResponse`; `403` si es el propio usuario, `400` si está duplicado |
| DELETE | `/api/tasks/{id}/assignments/{workerUserId}` | — | `204` |
| GET | `/api/projects/{projectId}/workers` | — | Usuarios activos asignables |
| GET | `/api/projects/{projectId}/progress` | — | Árbol de progreso (solo trabajo hoja) |
| PUT | `/api/subtasks/{id}` | `{ title, description, version }` | `SubtaskResponse` |
| POST | `/api/subtasks/{id}/start` · `/pause` · `/complete` · `/reopen` | `{ version, reason? }` | `SubtaskResponse` |
| DELETE | `/api/subtasks/{id}?version=` | — | `204` (CANCELADA) |
| GET | `/api/subtasks/history?projectId=&q=` | — | `SubtaskResponse[]` |
| GET | `/api/activities?stageId=` | — | `ActivityResponse[]` sin "Sin categoría" |

Errores (`{ error, message, timestamp }`):

| Código HTTP | `error` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Datos inválidos o asignación duplicada |
| 401 | `UNAUTHORIZED` | Token ausente, forjado o caducado |
| 403 | `FORBIDDEN` / `USER_INACTIVE` | Sin permiso para la operación, o cuenta desactivada al iniciar sesión |
| 404 | `NOT_FOUND` | No existe, o pertenece a otro proyecto/Encargado |
| 409 | `INVALID_STATE_TRANSITION` / `BUDGET_BELOW_ALLOCATIONS` | Transición no permitida o presupuesto del proyecto insuficiente |
| 412 | `VERSION_CONFLICT` | La `version` enviada está desactualizada |

## Frontend

- `core/models/api.ts`: contrato HTTP V4 (incluye `StageDto`, `ActivityDetailDto`, `AssignmentDto`, `AssignedTaskDto`, `ProgressNodeDto`).
- `core/models/task.ts`: modelo de vista con el vocabulario de la pantalla. Las subtareas usan su id en negativo (`kind: 'SUBTASK'`), porque `task` y `subtask` tienen secuencias independientes.
- `core/models/project.ts`: contrato de proyectos (incluye `canManageHierarchy`, permiso que devuelve el backend para mostrar u ocultar acciones de US2.2/US2.3).
- `core/services/task-mapper.ts`: traducción V4 ⇄ vista (mapea también `assignedWorkerIds` para el badge de Personal de US2.3).
- `core/services/task.service.ts`: fachada; todas las llamadas llevan `projectId` o `activityId` explícitos.
- `core/services/wbs.service.ts`: etapas, actividades, asignaciones de Personal y progreso (US2.2, US2.3, US3.3); además de crear, expone `updateStage`, `updateActivity`, `changeStageStatus` y `changeActivityStatus` (US2.2).
- `features/tasks/wbs/wbs-panel.ts`: constructor de la WBS (US2.2). Con el proyecto elegido, crea/renombra etapas y actividades y cambia sus estados; avisa con `structureChanged` para recargar las "categorías" y la lista.
- `features/tasks/assignment-dialog/assignment-dialog.ts`: asignación de Personal (US2.3). Lista el Personal activo del proyecto y las asignaciones actuales, asigna con motivo y retira asignaciones; el `<select>` entrega el id como texto y se normaliza con `Number(...)`, igual que el diálogo de delegación.
- `features/tasks/progress-panel/progress-panel.ts`: vista de progreso en cascada (US3.3) con el árbol Proyecto → Etapa → Actividad → Tarea → Subtarea y el porcentaje de cada nodo; publica el árbol con `loaded` para que la pantalla muestre el ratio del proyecto.
- `features/tasks/task-list.ts`: integra los tres paneles. Si `GET /api/projects` devuelve `[]` (Personal), activa el alcance "Mis tareas": carga y busca en `/api/tasks/assigned` y oculta el selector de proyectos (US3.1); si el proyecto elegido tiene `canManageHierarchy`, muestra el botón "Estructura" y "👥 Asignar" en cada tarea con el contador de Personal asignado.
- `src/design-system.css`: primitivas compartidas (botones, campos, avisos, tarjetas, modal, panel lateral, post-its). Vive en la hoja global porque la usan varios features.
- Las subtareas no muestran prioridad, categoría ni cronómetro. US-18 (relaciones) se quitó del servicio, porque no tenía pantalla.

## Pruebas

```powershell
cd backend
mvn clean test                 # dominio, casos de uso con dobles en memoria y arquitectura (sin base de datos)

cd ..\frontend
npx ng test --watch=false      # Vitest (mocks HTTP)
npx ng build --configuration production
```
