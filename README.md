# Gestor de Tareas Personales - Release 1

Aplicación web académica gestionada con Scrum, con frontend Angular, API Spring Boot y PostgreSQL.

## Estado actual

> **Modelo V4:** la base usa el modelo de Gestor de Proyectos (V004) y el backend sigue la arquitectura hexagonal del plan `.kilo/plans/hexagonal_restructure_plan.md`. La interfaz funciona como antes (tareas, subtareas, categorías, cronómetro, historial y búsqueda) sobre la API V4. Guías: [docs/migracion-v004.md](docs/migracion-v004.md), [docs/api-v4.md](docs/api-v4.md) y [docs/epica-1.md](docs/epica-1.md) (login JWT, proyectos, presupuesto y delegación; usuarios de prueba con contraseña `Gestor2026!`, por ejemplo `jefe@gestor.local`).

- PostgreSQL objetivo: 18.
- Esquema relacional versionado y reproducible.
- Scripts locales para crear roles, base de datos, aplicar migraciones y verificar la instalación.
- Backend conectado al esquema existente y frontend con proxy hacia la API.
- US-06 (editar) y US-07 (eliminar con confirmación) implementadas. Consulta [la guía de uso e integración](docs/us-06-us-07.md).
- US-12 (historial unificado) y US-13 (búsqueda por título/descripción) implementadas. Consulta [la guía funcional](docs/us-12-us-13.md).
- Integración con el contador de tiempo de `develop`: el historial tiene acceso directo en la cabecera, conserva las actualizaciones durante cargas simultáneas y funciona con bases V001/V002 aunque V003 aún no esté aplicada.

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

## Calidad estática con SonarQube

El proyecto incorpora una pila local reproducible de SonarQube Community Build y PostgreSQL 18, completamente separada de la base de datos de tareas. Consulta [la guía de SonarQube](docs/sonarqube.md) para iniciarla, crear un token local, ejecutar los análisis A1/A2/A3 y configurar el Quality Profile y Quality Gate de la práctica.

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

API V4 bajo `/api/tasks`, `/api/subtasks` y `/api/activities`. La tabla completa y los códigos de error están en [docs/api-v4.md](docs/api-v4.md).

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

> El contenedor `db` crea los roles `gestor_tareas_owner` (dueño) y `gestor_tareas_app` (backend) con contraseñas de **desarrollo local solamente** (`ownerpass`/`apppass`/`secret`), la base `gestor_tareas` y aplica `V001`…`V004` sobre el esquema `task_manager`.

Verifica la base:

```powershell
docker cp database\scripts\verify_schema.sql gestor-tareas-db:/tmp/
docker compose exec -T db psql "host=localhost port=5432 user=gestor_tareas_app dbname=gestor_tareas" -f /tmp/verify_schema.sql
```

Resultado esperado: PostgreSQL 18, zona horaria `UTC`, migraciones `V1` a `V4`, `can_rewrite_history = false` y `can_edit_catalogs = false`.

> Si tienes PostgreSQL 18 instalado localmente en vez de Docker, usa `.\database\scripts\setup-local.ps1` y `.\database\scripts\verify-local.ps1` (piden las contraseñas de forma interactiva).

### 2. Pruebas manuales rápidas

```bash
# Crear (queda PENDIENTE = "INACTIVA" en la interfaz) y guardar taskId y version de la respuesta
curl -i -X POST http://localhost:8080/api/tasks -H "Content-Type: application/json" \
  -d '{"title":"Mi primera tarea","priority":"ALTA","activityName":"Trabajo"}'

# Listar con subtareas, iniciar y completar (usa la version de la última respuesta)
curl -s http://localhost:8080/api/tasks
curl -i -X POST http://localhost:8080/api/tasks/1/start    -H "Content-Type: application/json" -d '{"version":<version>}'
curl -i -X POST http://localhost:8080/api/tasks/1/complete -H "Content-Type: application/json" -d '{"version":<version>}'
```

- Una `version` vieja devuelve `412 VERSION_CONFLICT`; completar una tarea PENDIENTE devuelve `409 INVALID_STATE_TRANSITION`.
- Cada transición queda en `task_status_history` con su usuario y motivo.

### 3. Pruebas automáticas

```powershell
# Backend: dominio, casos de uso con dobles en memoria y reglas de arquitectura (sin base de datos)
cd backend
mvn test

# Frontend: Angular + Vitest (requiere Node >= 24.15 o >= 22.22.3)
cd frontend
npx ng test --watch=false
```

> El backend valida el esquema al arrancar (`spring.jpa.hibernate.ddl-auto: validate`): si las entidades no coinciden con V004, no arranca. Si cambias de rama, compila con `mvn clean` para que no queden clases viejas en `target`.

### 4. Acceso directo a la base de datos

```powershell
docker compose exec -T db psql "host=localhost port=5432 user=gestor_tareas_app dbname=gestor_tareas"
```

Consultas de verificación:

```sql
-- Tareas con su estado y actividad ("categoría")
SELECT t.task_id, t.title, ws.name AS estado, a.name AS actividad, t.actual_duration_minutes
FROM task_manager.task t
JOIN task_manager.work_status ws ON ws.work_status_id = t.work_status_id
JOIN task_manager.activity a ON a.activity_id = t.activity_id
ORDER BY t.task_id;

-- Historial de estados (lo escribe el backend, con usuario y motivo)
SELECT task_id, previous_status_id, new_status_id, reason, changed_at
FROM task_manager.task_status_history
ORDER BY task_id, changed_at;

-- El rol de la app no puede reescribir historiales
UPDATE task_manager.task_status_history SET reason = 'x';  -- -> ERROR de permisos
```

> Usa `gestor_tareas_app` siempre para consultas y para el backend. Usa `gestor_tareas_owner` solo para nuevas migraciones (`.\database\scripts\migrate.ps1`).

### 5. Notas de prueba

- El cronómetro se calcula a partir del historial de estados (segmentos EN_PROGRESO), con precisión de segundos; `actual_duration_minutes` guarda el total redondeado hacia arriba.
- Las contraseñas de desarrollo (`secret`, `ownerpass`, `apppass`) son solo para pruebas locales y **no** deben subirse a Git.
