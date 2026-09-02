# Gestor de Tareas Personales - Release 1

Aplicación web académica gestionada con Scrum, con frontend Angular, API Spring Boot y PostgreSQL.

## Estado actual

- PostgreSQL objetivo: 18.
- Esquema relacional versionado y reproducible.
- Scripts locales para crear roles, base de datos, aplicar migraciones y verificar la instalación.
- Backend conectado al esquema existente y frontend con proxy hacia la API.
- US-06 (editar) y US-07 (eliminar con confirmación) implementadas. Consulta [la guía de uso e integración](docs/us-06-us-07.md).
- US-12 (historial unificado) y US-13 (búsqueda por título/descripción) implementadas. Consulta [la guía funcional](docs/us-12-us-13.md).

## Inicio rápido de base de datos

Consulta [database/README.md](database/README.md). En Windows, con PostgreSQL 18 instalado:

```powershell
.\database\scripts\setup-local.ps1
.\database\scripts\verify-local.ps1
```

Los scripts solicitan las contraseñas de forma interactiva. No guardes credenciales reales en Git.

## Flujo de ramas

- `main`: versión estable/release.
- `develop`: integración del equipo.
- Ramas cortas: `feature/US-XX-descripcion`, `fix/...`, `docs/...` o una rama técnica equivalente.
- Cada cambio llega a `develop` mediante Pull Request y revisión de otro integrante.

## Backend (Spring Boot)

API REST sobre el esquema existente en `task_manager` (sin migraciones desde la app: `ddl-auto: validate`).

### Requisitos

- Java 21+
- Maven 3.9+
- PostgreSQL 18 con el esquema aplicado (`database/scripts`) y el rol `gestor_tareas_app`.

### Configuración

Define las variables de entorno en la terminal donde arrancas el backend. Spring Boot no carga un archivo `.env` automáticamente:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/gestor_tareas?currentSchema=task_manager"
$env:SPRING_DATASOURCE_USERNAME="gestor_tareas_app"
$env:SPRING_DATASOURCE_PASSWORD="TU_PASSWORD"
```

### Ejecución

```powershell
cd backend
mvn spring-boot:run
```

El servidor queda en `http://localhost:8080`.

### Endpoints

| Método | URL | Cuerpo | Respuesta |
|--------|-----|--------|-----------|
| POST | `/api/tasks` | `{ "title": "..." }` | `201` + `TaskResponse` |
| GET | `/api/tasks` | — | `200` + `TaskResponse[]` (desde `v_tasks`) |
| GET | `/api/tasks?q=texto` | — | `200` + tareas cuyo título o descripción coincide |
| GET | `/api/tasks/history?q=texto` | — | `200` + tareas terminadas/eliminadas con transiciones |
| PUT | `/api/tasks/{id}` | `{ "title": "...", "description": null, "priority": "MEDIA", "version": 0 }` | `200` + `TaskResponse` |
| DELETE | `/api/tasks/{id}?version=0` | — | `204` (borrado lógico) |
| POST | `/api/tasks/{id}/activate` | `{ "version": 0 }` | `200` + `TaskResponse` |
| POST | `/api/tasks/{id}/complete` | `{ "version": 0 }` | `200` + `TaskResponse` |

Errores: `404` tarea inexistente o eliminada, `409` transición inválida o tarea con subtareas visibles, `412` conflicto de versión (concurrencia), `400` validación.

## Frontend (Angular)

```powershell
cd frontend
npm install
npm start
```

El proxy está configurado directamente en `angular.json`, por lo que tanto `npm start` como `ng serve` redirigen `/api` al backend en `http://localhost:8080`.

## Testing / Pruebas

Checklist paso a paso para verificar que **US-01 a US-04** funcionan.

### Requisitos

- **Docker + `docker compose`**: para PostgreSQL 18.
- **JDK 21+** y **Maven 3.9+**: para el backend.
- **Node.js >= 24.15** (o >= 22.22.3): Angular CLI 22 lo exige. Node 24.8 **no** alcanza (el CLI lo rechaza).
- PowerShell 5.1+ (Windows).

### 1. Arranque del entorno

```powershell
# 1) PostgreSQL 18 + roles + esquema + migraciones (un solo comando)
docker compose up -d
docker compose exec db pg_isready -U postgres

# 2) Backend (usa .env.example / variables; sin contraseña por defecto)
cd backend
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/gestor_tareas?currentSchema=task_manager"
$env:SPRING_DATASOURCE_USERNAME="gestor_tareas_app"
$env:SPRING_DATASOURCE_PASSWORD="apppass"
mvn spring-boot:run      # -> http://localhost:8080

# 3) Frontend (nueva terminal)
cd frontend
npm install              # la primera vez
npm start                # -> http://localhost:4200
```

> El contenedor `db` crea los roles `gestor_tareas_owner` (dueño) y `gestor_tareas_app` (backend) con contraseñas de **desarrollo local solamente** (`ownerpass`/`apppass`/`secret`), la base `gestor_tareas` y aplica `V001`/`V002` sobre el esquema `task_manager`.

Verifica la base:

```powershell
docker cp database\scripts\verify_schema.sql gestor-tareas-db:/tmp/
docker compose exec -T db psql "host=localhost port=5432 user=gestor_tareas_app dbname=gestor_tareas" -f /tmp/verify_schema.sql
```

Resultado esperado: PostgreSQL 18.6, zona horaria `UTC`, migraciones `V1` y `V2`, y `can_hard_delete_tasks = false`.

> Si tienes PostgreSQL 18 instalado localmente en vez de Docker, usa `.\database\scripts\setup-local.ps1` y `.\database\scripts\verify-local.ps1` (piden las contraseñas de forma interactiva).

### 2. Pruebas manuales (US-01 a US-04)

#### US-01 — Crear tarea -> `INACTIVA`, prioridad `MEDIA`, `version:0`
```bash
curl -i -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Mi primera tarea"}'
```
- `201 Created` con `TaskResponse`: `status:"INACTIVA"`, `priority:"MEDIA"`, `version:0`, `activatedAt:null`, `completedAt:null`.
- En la UI: escribes el titulo en el formulario y envias; la tarjeta aparece en gris.
- En base: la fila esta en `task_manager.tasks` y en `v_tasks`; el trigger inserta `null -> INACTIVA` en `task_status_history`.

#### US-02 — Listar tareas
```bash
curl -s http://localhost:8080/api/tasks
```
- `200` con el array `TaskResponse[]`. La vista `v_tasks` incluye `effectiveActiveSeconds` y `totalActiveSeconds`.

#### US-03 — Activar (`INACTIVA -> ACTIVA`)
```bash
curl -i -X POST http://localhost:8080/api/tasks/1/activate \
  -H "Content-Type: application/json" \
  -d '{"version":0}'
```
- `200`: `status:"ACTIVA"`, `activatedAt` definido, `version:1`.
- En base: `activated_at` pasa de `NULL` a timestamp; trigger registra `INACTIVA -> ACTIVA`.

#### US-04 — Completar (`ACTIVA -> TERMINADA`)
```bash
curl -i -X POST http://localhost:8080/api/tasks/1/complete \
  -H "Content-Type: application/json" \
  -d '{"version":1}'
```
- `200`: `status:"TERMINADA"`, `completedAt` definido, `activatedAt:null`, `totalActiveSeconds` consolidado (segundos enteros), `version:2`.
- En base: trigger registra `ACTIVA -> TERMINADA`; la constraint `ck_tasks_state_timestamps` exige `activated_at=NULL` cuando esta `TERMINADA`.

#### Campos de error controlados
| Accion | Codigo | Error | Causa |
|---|---|---|---|
| Activar/Completar una `TERMINADA` | `409` | `INVALID_STATE_TRANSITION` | `TERMINADA -> ACTIVA` / `TERMINADA -> TERMINADA` |
| Completar una `INACTIVA` | `409` | `INVALID_STATE_TRANSITION` | `INACTIVA -> TERMINADA` |
| id inexistente | `404` | `TASK_NOT_FOUND` | — |
| `version` enviado != `version` en base | `412` | `VERSION_CONFLICT` | bloqueo optimista (`@Version`) |
| titulo en blanco | `400` | `VALIDATION_ERROR` | `@NotBlank` |

> La regla de versiones se comprueba **antes** que el estado: con un `version` incorrecto devuelve `412`; con el `version` correcto pero estado invalido devuelve `409`.

### 3. Pruebas automáticas

```powershell
# Backend: Mockito (sin base de datos). Cubre TaskService y el mapeo de timestamps de la vista v_tasks
cd backend
mvn test

# Frontend: Angular + Vitest (requiere Node >= 24.15)
cd frontend
npx ng test --watch=false
```

- `mvn test`: cubre crear/listar/activar/completar, edición sin modificar el ciclo de vida, borrado lógico y errores de concurrencia/estado/no encontrado.
- `ng test`: cubre creación visible, contrato HTTP de edición/borrado, validaciones, cancelación, confirmación y actualización inmediata de la interfaz.

> El backend valida el esquema en arranque con `spring.jpa.hibernate.ddl-auto: validate`; si la entidad `Task` no cuadra con `task_manager.tasks`, la aplicacion no arranca.

### 4. Acceso directo a la base de datos

```powershell
docker compose exec -T db psql "host=localhost port=5432 user=gestor_tareas_app dbname=gestor_tareas"
```

Consultas de verificacion:

```sql
-- Tareas visibles (la vista que consume el backend)
SELECT id, title, status, activated_at, completed_at, total_active_seconds, version
FROM task_manager.v_tasks;

-- Auditoria de transiciones (generada por el trigger trg_tasks_status_history, NO por el codigo)
SELECT task_id, from_status, to_status, changed_at
FROM task_manager.task_status_history
ORDER BY task_id, changed_at;

-- El rol de app NO puede borrar fisicamente (can_hard_delete_tasks = false)
DELETE FROM task_manager.tasks WHERE id = 1;  -- -> ERROR de permisos
```

> Usa `gestor_tareas_app` siempre para consultas y para el backend. Usa `gestor_tareas_owner` solo para nuevas migraciones (`.\database\scripts\migrate.ps1`).

### 5. Notas de prueba

- El tiempo activo se consolida en **segundos enteros** (`Instant.now().getEpochSecond() - activatedAt.getEpochSecond()`); activar y completar en menos de 1 s dejan `total_active_seconds = 0`.
- La columna `effectiveActiveSeconds` de la vista puede salir `-1` en el instante de activar si el reloj del contenedor y de PostgreSQL estan desincronizados; no afecta al negocio y la UI lo filtra (`> 0`).
- Las contraseñas de desarrollo (`secret`, `ownerpass`, `apppass`) son solo para pruebas locales y **no** deben subirse a Git.
