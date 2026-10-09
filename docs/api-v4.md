# API V4 y adaptación del frontend

La interfaz funciona igual que antes (lista con cronómetro, subtareas, categorías, completadas, historial y búsqueda), pero ahora sobre la base migrada V004 y con el backend en arquitectura hexagonal, como propone `.kilo/plans/hexagonal_restructure_plan.md`.

## Estructura del backend

```
com.hardcoders.taskmanager
├── domain                    # Java puro: sin Spring, JPA ni Jackson
│   ├── model                 # Task, Subtask, WorkStatus, TaskPriority, StatusChange, TimeTracking, VersionToken...
│   ├── exception             # NotFound, InvalidStatusTransition, VersionConflict, OpenSubtasks, DomainValidation
│   └── port/out              # TaskRepository, SubtaskRepository, WorkspaceRepository, CurrentActor
├── application               # Java puro: casos de uso
│   ├── port/in               # ManageTasksUseCase, ManageSubtasksUseCase, QueryTasksUseCase
│   ├── command | result      # Comandos de entrada y resultados
│   └── usecase               # TaskCommandService, SubtaskCommandService, TaskQueryService
├── adapter
│   ├── in/web                # Controladores REST, DTOs con validación, GlobalExceptionHandler, CORS
│   └── out/persistence       # Entidades JPA (*Entity), repositorios Spring Data, adaptadores de los puertos
└── configuration             # Raíz de composición; envuelve cada caso de uso en una transacción
```

- Los casos de uso no llevan anotaciones de Spring: `configuration/TransactionalUseCases` los envuelve en una transacción. Así los triggers diferidos de V004 (historiales obligatorios) se validan al confirmar.
- `HexagonalArchitectureTest` falla si `domain` o `application` importan Spring, Jakarta, Hibernate, Jackson o los adaptadores.

## Cómo se representa el modelo anterior en V4

| Interfaz (como antes) | V4 |
|---|---|
| Tarea | `task` dentro del espacio de trabajo |
| Subtarea | `subtask` de la tarea (un solo nivel) |
| Categoría | `activity` de la etapa; "Sin categoría" es una actividad más |
| INACTIVA / ACTIVA / TERMINADA | PENDIENTE / EN_PROGRESO / COMPLETADA |
| Eliminar (borrado lógico) | Pasar a CANCELADA; queda en el historial |
| Cronómetro (`total_active_seconds`) | Se calcula del historial de estados: cada tramo EN_PROGRESO es un segmento. `actual_duration_minutes` guarda el total en minutos |
| `version` (bloqueo optimista) | Token = `updated_at` en microsegundos (el trigger lo renueva en cada cambio) |

- **Espacio de trabajo:** es el primer proyecto y su primera etapa. Con datos migrados es "Proyecto legado"; en una base nueva se crea "Mis tareas" / "General" la primera vez que se crea una tarea.
- **Usuario:** mientras no exista JWT, los historiales se atribuyen a un usuario local (`app.local-user.email`, por defecto `usuario.local@gestor-tareas.local`). Se crea solo y no puede iniciar sesión.
- **Fuera de V4:** las relaciones entre tareas (US-18) y la prioridad y el cronómetro de las subtareas, porque el modelo V4 no tiene dónde guardarlos.

## Endpoints

| Método | URL | Cuerpo | Respuesta |
|---|---|---|---|
| GET | `/api/tasks?q=` | — | `TaskResponse[]` no canceladas, con `subtasks` |
| GET | `/api/tasks/{id}` | — | `TaskResponse` |
| POST | `/api/tasks` | `{ title, priority?, activityName? }` | `201` `TaskResponse` |
| PUT | `/api/tasks/{id}` | `{ title, description, priority, version }` | `TaskResponse` |
| PUT | `/api/tasks/{id}/activity` | `{ activityName, version }` | `TaskResponse` |
| POST | `/api/tasks/{id}/start` · `/pause` · `/complete` · `/reopen` | `{ version }` | `TaskResponse` |
| DELETE | `/api/tasks/{id}?version=` | — | `204` (CANCELADA) |
| GET | `/api/tasks/history?q=` | — | `TaskHistoryResponse[]` (COMPLETADA o CANCELADA, con eventos) |
| POST | `/api/tasks/{id}/subtasks` | `{ title, description? }` | `201` `SubtaskResponse` |
| PUT | `/api/subtasks/{id}` | `{ title, description, version }` | `SubtaskResponse` |
| POST | `/api/subtasks/{id}/start` · `/pause` · `/complete` · `/reopen` | `{ version }` | `SubtaskResponse` |
| DELETE | `/api/subtasks/{id}?version=` | — | `204` (CANCELADA) |
| GET | `/api/subtasks/history?q=` | — | `SubtaskResponse[]` |
| GET | `/api/activities` | — | `[{ activityId, name }]` sin "Sin categoría" |

Errores (`{ error, message, timestamp }`):

| Código HTTP | `error` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Datos inválidos |
| 404 | `TASK_NOT_FOUND` / `SUBTASK_NOT_FOUND` | No existe o está cancelada |
| 409 | `INVALID_STATE_TRANSITION` | Por ejemplo, completar una tarea PENDIENTE |
| 409 | `TASK_HAS_SUBTASKS` | Eliminar una tarea con subtareas abiertas |
| 412 | `VERSION_CONFLICT` | La `version` enviada está desactualizada |

## Frontend

- `core/models/api.ts`: contrato HTTP V4.
- `core/models/task.ts`: modelo de vista con el vocabulario de la pantalla. Las subtareas usan su id en negativo (`kind: 'SUBTASK'`), porque `task` y `subtask` tienen secuencias independientes.
- `core/services/task-mapper.ts`: traducción V4 ⇄ vista.
- `core/services/task.service.ts`: fachada con la misma interfaz pública que antes, para que los componentes no cambien. Decide si cada llamada va a `/api/tasks` o a `/api/subtasks`.
- Las subtareas no muestran prioridad, categoría ni cronómetro. US-18 (relaciones) se quitó del servicio, porque no tenía pantalla.

## Pruebas

```powershell
cd backend
mvn test                 # dominio, casos de uso con dobles en memoria y arquitectura (sin base de datos)

cd ..\frontend
npx ng test --watch=false
```
