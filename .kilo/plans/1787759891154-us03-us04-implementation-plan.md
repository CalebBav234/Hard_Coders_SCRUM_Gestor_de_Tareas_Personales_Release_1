# Plan de Implementación — US-03 y US-04

## 0. Preflight / Inspección

**Estado real del repositorio:**
- Frontend: Angular 22 con componentes `TaskForm` y `TaskList`, pero **sin servicios HTTP**, **sin routing**, y con **datos hardcodeados**.
- Backend: **No existe**. No hay `pom.xml`, no hay código Java, no hay entidades JPA ni controllers.
- Base de datos: Esquema `task_manager` completamente definido en `V001` y `V002`. Triggers, vistas, constraints y permisos listos.
- Configuración: `.env.example` y `database/examples/application.yml` definen la conexión esperada (`gestor_tareas_app` / esquema `task_manager` / `ddl-auto: validate`).

**Hallazgo crítico:**
Ni US-01 ni US-02 están completas. El frontend no consume API y el backend no existe. Por lo tanto, el plan incluye el mínimo necesario para dejar US-01 y US-02 funcionales antes de US-03/04.

---

## 1. Completar US-01 y US-02 (base mínima)

**Objetivo:** Tener un backend funcional que permita crear y listar tareas, y un frontend conectado a él.

**Acciones:**
1. Crear proyecto Spring Boot en `backend/` con Java 21, Spring Boot 4.1, Spring Data JPA, PostgreSQL driver, validation.
2. Configurar `application.yml` según `database/examples/application.yml` ( `ddl-auto: validate`, `currentSchema=task_manager`, usuario `gestor_tareas_app`).
3. Crear entidad JPA `Task` mapeando exactamente la tabla `task_manager.tasks`, incluyendo `@Version` en `version`.
4. Crear `TaskRepository` extendiendo `JpaRepository<Task, Long>`.
5. Crear `TaskService` con método `create(title: String): Task` que persista la tarea como `INACTIVA`, prioridad `MEDIA`, timestamps nulos.
6. Crear `TaskController` con `POST /api/tasks` (crear) y `GET /api/tasks` (listar desde `v_tasks` usando `@Query` nativa o proyección).
7. En el frontend:
   - Crear `TaskService` (Angular) con `HttpClient` para `POST /api/tasks` y `GET /api/tasks`.
   - Conectar `TaskForm` al servicio para crear tareas reales.
   - Conectar `TaskList` al servicio para mostrar tareas desde la API.
   - Eliminar datos hardcodeados.

**Resultado esperado:** US-01 y US-02 completamente funcionales sobre la base existente.

---

## 2. Verificar conexión PostgreSQL

**Objetivo:** Garantizar que la aplicación usa la base existente sin modificarla.

**Acciones:**
1. Confirmar que `spring.jpa.hibernate.ddl-auto: validate` está activo.
2. Confirmar `currentSchema=task_manager` en la URL JDBC.
3. Confirmar usuario `gestor_tareas_app` con permisos SELECT/INSERT/UPDATE sobre `task_manager.tasks`.
4. Verificar que la entidad JPA coincide exactamente con el esquema (nombres de columnas, tipos, constraints).
5. No ejecutar migraciones desde la aplicación (`spring.sql.init.mode: never`).

**Resultado esperado:** Backend conectado, esquema intacto, sin modificaciones automáticas.

---

## 3. Preparar modelo/backend para estados y tiempo

**Objetivo:** Tener la base para implementar transiciones de estado.

**Acciones:**
1. En `Task` entity: asegurar mapeo de `activatedAt`, `completedAt`, `totalActiveSeconds`, `status`, `version`.
2. En `TaskRepository`: consulta nativa o proyección para leer desde `v_tasks` (incluye `effective_active_seconds`).
3. En `TaskService`: lógica de validación de transiciones (INACTIVA→ACTIVA, ACTIVA→TERMINADA).
4. Definir excepción personalizada `InvalidTaskStateException` para estados incompatibles.

**Resultado esperado:** Modelo preparado para US-03 y US-04.

---

## 4. Implementar US-03 — Activar tarea

**Objetivo:** Transición INACTIVA → ACTIVA.

**Criterios de aceptación:**
1. Estado cambia a `ACTIVA`.
2. Se registra `activated_at`.
3. Empieza a contar tiempo activo.

**Acciones:**
1. `TaskService.activate(taskId: Long, expectedVersion: Long): Task`
   - Cargar tarea por id (con bloqueo optimista por versión).
   - Validar: existe, estado = INACTIVA.
   - Validar concurrencia: versión coincide.
   - Setear `status = ACTIVA`, `activatedAt = Instant.now()`.
   - Persistir.
   - El trigger `trg_tasks_status_history` registra automáticamente la transición.
   - Retornar tarea actualizada.
2. `TaskController`: `POST /api/tasks/{id}/activate`
   - Request body opcional (para futura extensibilidad).
   - Response: `TaskResponse` con datos actualizados.
   - 404 si no existe, 409 si estado inválido, 412 si versión no coincide.
3. Frontend:
   - Botón "Activar" visible solo cuando `task.status === 'INACTIVA'`.
   - Al hacer clic: llamar a `POST /api/tasks/{id}/activate`, actualizar lista, mostrar feedback.
   - Manejar errores (toast o mensaje inline).

**Resultado esperado:** Tarea activable desde la UI, con feedback y manejo de errores.

---

## 5. Implementar US-04 — Completar tarea

**Objetivo:** Transición ACTIVA → TERMINADA.

**Criterios de aceptación:**
1. Estado cambia a `TERMINADA`.
2. Se registra finalización.
3. Se consolida el tiempo activo.

**Acciones:**
1. `TaskService.complete(taskId: Long, expectedVersion: Long): Task`
   - Cargar tarea por id (con bloqueo optimista).
   - Validar: existe, estado = ACTIVA.
   - Calcular tiempo transcurrido: `Instant.now().getEpochSecond() - activatedAt.getEpochSecond()`.
   - Sumar a `totalActiveSeconds`.
   - Setear `status = TERMINADA`, `completedAt = Instant.now()`, `activatedAt = null`.
   - Validar que cumple `ck_tasks_state_timestamps` (TERMINADA → activated_at NULL, completed_at NOT NULL).
   - Persistir dentro de `@Transactional`.
   - El trigger registra la transición.
   - Retornar tarea actualizada.
2. `TaskController`: `POST /api/tasks/{id}/complete`
   - Response: `TaskResponse`.
   - 404 si no existe, 409 si estado inválido, 412 si versión no coincide.
3. Frontend:
   - Botón "Completar" visible solo cuando `task.status === 'ACTIVA'`.
   - Al hacer clic: llamar a `POST /api/tasks/{id}/complete`, actualizar lista, mostrar feedback.
   - Mostrar tiempo consolidado en la tarjeta de tarea.

**Resultado esperado:** Tarea completable desde la UI, tiempo consolidado visible.

---

## 6. API — Contratos

**Endpoints:**

| Método | URL | Request | Response | Éxito | Error |
|--------|-----|---------|----------|-------|-------|
| POST | /api/tasks | `{ title: string }` | `TaskResponse` | 201 | 400 |
| GET | /api/tasks | — | `TaskResponse[]` | 200 | — |
| POST | /api/tasks/{id}/activate | `{ version: number }` | `TaskResponse` | 200 | 404, 409, 412 |
| POST | /api/tasks/{id}/complete | `{ version: number }` | `TaskResponse` | 200 | 404, 409, 412 |

**TaskResponse (DTO mínimo):**
- id, title, description, status, priority, categoryId, parentTaskId, activatedAt, completedAt, totalActiveSeconds, effectiveActiveSeconds (desde v_tasks), createdAt, updatedAt, version.

**Manejo de errores:**
- 400: validación de entrada.
- 404: tarea no encontrada o eliminada.
- 409: transición de estado no permitida.
- 412: conflicto de concurrencia (versión).
- 500: error inesperado.

---

## 7. Angular — Botones, estados y feedback

**Objetivo:** Integrar US-03 y US-04 en la UI existente.

**Acciones:**
1. Extender `Task` interface para incluir campos necesarios del response.
2. En `task-list.html`:
   - Mostrar estado con badge de color (INACTIVA: gris, ACTIVA: azul, TERMINADA: verde).
   - Mostrar `effectiveActiveSeconds` formateado (ej. "2h 30m") cuando corresponda.
   - Botones condicionales: "Activar" (solo INACTIVA), "Completar" (solo ACTIVA).
   - Usar texto + color para accesibilidad.
3. En `task-list.ts`:
   - Llamar a `TaskService` para cargar tareas.
   - Métodos `activate(task)` y `complete(task)` que llaman al API y actualizan la lista local.
   - Manejo de loading y errores.
4. CSS:
   - Estilos para badges de estado.
   - Estilos para botones de acción (deshabilitados cuando no aplican).
5. Feedback: mensaje de éxito o error después de cada operación.

**Resultado esperado:** UI coherente, acciones visibles solo cuando corresponden.

---

## 8. Persistencia / Transacción / Tiempo

**Objetivo:** Garantizar consistencia sin cambiar DB.

**Acciones:**
1. `complete()` debe ejecutarse en `@Transactional`.
2. La consolidación de tiempo se hace en memoria dentro de la transacción.
3. No se insertan registros manuales en `task_status_history` (el trigger lo hace).
4. No se modifica `task_time_entries` para US-03/04 (no es necesario; el esquema actual soporta el flujo con `activated_at`/`completed_at`/`total_active_seconds`).
5. Bloqueo optimista con `@Version` en la entidad JPA.

**Resultado esperado:** Operaciones atómicas, sin estados intermedios.

---

## 9. Integración

**Acciones:**
1. Conectar frontend a backend mediante proxy de Angular (si aplica) o URL configurable.
2. Probar flujo completo: crear → activar → completar.
3. Verificar en la base que:
   - `status` cambia correctamente.
   - `activated_at` y `completed_at` se setean/limpian según约束.
   - `total_active_seconds` se incrementa al completar.
   - `task_status_history` registra las transiciones (trigger).
4. Verificar que `effective_active_seconds` en `v_tasks` refleja el tiempo mientras la tarea está ACTIVA.

**Resultado esperado:** Flujo end-to-end funcional sobre la base existente.

---

## 10. Git / Trello

**Rama:**
- `feature/US-03-04-activar-completar` desde `develop`.
- Si el equipo prefiere separadas: `feature/US-03-activar` y `feature/US-04-completar`.

**Commits lógicos:**
1. `feat(backend): proyecto Spring Boot base + entidad Task + repositorio`
2. `feat(backend): endpoints crear/listar tareas (US-01/02)`
3. `feat(frontend): conectar TaskForm y TaskList a API (US-01/02)`
4. `feat(backend): endpoint activar tarea (US-03)`
5. `feat(backend): endpoint completar tarea (US-04)`
6. `feat(frontend): botones activar/completar y feedback (US-03/04)`
7. `docs: actualizar README con instrucciones de backend`

**PR:**
- PR a `develop` con revisión de otro integrante.
- Incluir evidencia funcional (capturas o video corto).
- Trello: mover tarjetas US-03 y US-04 por el flujo definido (Product Backlog → Sprint Backlog → Ready → In Progress → Review/Integración → Done).

---

## 11. Evidencia funcional

- Video o capturas mostrando:
  1. Crear tarea → aparece en lista.
  2. Activar tarea INACTIVA → pasa a ACTIVA, `activated_at` seteado.
  3. Mientras ACTIVA, `effective_active_seconds` aumenta en la vista.
  4. Completar tarea ACTIVA → pasa a TERMINADA, `completed_at` seteado, `total_active_seconds` consolidado.
  5. Intentar activar tarea TERMINADA → error controlado.
  6. Intentar completar tarea INACTIVA → error controlado.

---

## 12. Definition of Done

- [ ] US-01 funcional: crear tarea por API, aparece en lista frontend.
- [ ] US-02 funcional: listar tareas desde `v_tasks` en frontend.
- [ ] US-03 funcional: activar tarea con transición correcta y feedback.
- [ ] US-04 funcional: completar tarea con tiempo consolidado y feedback.
- [ ] Esquema PostgreSQL sin modificaciones.
- [ ] Triggers y vistas respetados.
- [ ] Bloqueo optimista (`@Version`) implementado.
- [ ] Código integrado en `develop` mediante PR.
- [ ] Trello actualizado.
- [ ] Evidencia funcional disponible.

---

## Tabla de cambios

| Componente | Archivo existente | Cambio | Motivo |
| ---------- | ----------------- | ------ | ------ |
| Backend | `backend/pom.xml` | Nuevo | Dependencias Spring Boot 4.1 + JPA + PostgreSQL |
| Backend | `backend/src/main/resources/application.yml` | Nuevo | Configuración de conexión y JPA |
| Backend | `backend/src/main/java/.../entity/Task.java` | Nuevo | Entidad JPA de `task_manager.tasks` |
| Backend | `backend/src/main/java/.../repository/TaskRepository.java` | Nuevo | Acceso a datos |
| Backend | `backend/src/main/java/.../service/TaskService.java` | Nuevo | Lógica de negocio y transiciones |
| Backend | `backend/src/main/java/.../controller/TaskController.java` | Nuevo | API REST |
| Backend | `backend/src/main/java/.../dto/TaskResponse.java` | Nuevo | DTO de respuesta |
| Backend | `backend/src/main/java/.../exception/InvalidTaskStateException.java` | Nuevo | Error de transición |
| Frontend | `frontend/src/app/core/services/task.service.ts` | Nuevo | Servicio HTTP Angular |
| Frontend | `frontend/src/app/core/models/task.ts` | Modificar | Extender interface con campos de API |
| Frontend | `frontend/src/app/features/tasks/task-form/task-form.ts` | Modificar | Llamar a API en lugar de console.log |
| Frontend | `frontend/src/app/features/tasks/task-list/task-list.ts` | Modificar | Cargar datos desde API, botones activate/complete |
| Frontend | `frontend/src/app/features/tasks/task-list/task-list.html` | Modificar | Botones condicionales y badges de estado |
| Frontend | `frontend/src/app/features/tasks/task-list/task-list.css` | Modificar | Estilos para badges y botones de acción |
| Config | `.env` | Nuevo (no versionado) | Credenciales locales de PostgreSQL |

---

## Archivos que probablemente deberán modificarse

### Backend
- `backend/pom.xml` (nuevo)
- `backend/src/main/resources/application.yml` (nuevo)
- `backend/src/main/java/.../TaskManagerApplication.java` (nuevo)
- `backend/src/main/java/.../entity/Task.java` (nuevo)
- `backend/src/main/java/.../repository/TaskRepository.java` (nuevo)
- `backend/src/main/java/.../service/TaskService.java` (nuevo)
- `backend/src/main/java/.../controller/TaskController.java` (nuevo)
- `backend/src/main/java/.../dto/TaskResponse.java` (nuevo)
- `backend/src/main/java/.../exception/InvalidTaskStateException.java` (nuevo)
- `backend/src/main/java/.../exception/GlobalExceptionHandler.java` (nuevo)

### Frontend
- `frontend/src/app/core/services/task.service.ts` (nuevo)
- `frontend/src/app/core/models/task.ts` (modificar)
- `frontend/src/app/features/tasks/task-form/task-form.ts` (modificar)
- `frontend/src/app/features/tasks/task-form/task-form.html` (posible ajuste menor)
- `frontend/src/app/features/tasks/task-list/task-list.ts` (modificar)
- `frontend/src/app/features/tasks/task-list/task-list.html` (modificar)
- `frontend/src/app/features/tasks/task-list/task-list.css` (modificar)

### Configuración
- `.env` (nuevo, en `.gitignore`)

---

## Archivos que NO deben modificarse

- `database/migrations/V001__initial_schema.sql`
- `database/migrations/V002__views_and_permissions.sql`
- `database/scripts/verify_schema.sql`
- `database/scripts/setup-local.ps1`
- `database/scripts/verify-local.ps1`
- `database/scripts/migrate.ps1`
- Cualquier otro archivo dentro de `database/`
- `frontend/package.json` (a menos que se necesite agregar `@angular/common/http` — pero ya está en Angular 22 por defecto)

---

## Riesgos y validaciones

| Riesgo | Mitigación |
|--------|-----------|
| Backend no existe; todo debe crearse | Seguir arquitectura monolito modular por capas del Manual Operativo. |
| Esquema puede no coincidir con entidad JPA | Usar `ddl-auto: validate` y verificar nombres de columnas exactos. |
| Trigger ya registra historial; no duplicar | No insertar en `task_status_history` desde código. |
| Estados inválidos en DB | Constraints en SQL previenen inconsistencias; backend valida antes de persistir. |
| Concurrencia | Usar `@Version` y parámetro `expectedVersion` en requests. |

---

## Próximo paso

Ejecutar el plan en orden: 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12.
