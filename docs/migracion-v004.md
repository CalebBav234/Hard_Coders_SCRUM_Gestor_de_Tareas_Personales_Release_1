# Migración V004 - Modelo de Gestor de Proyectos

La migración `database/migrations/V004__project_management_model.sql` reemplaza el modelo de tareas personales por el de **Gestor de Proyectos** definido en los diagramas ER/UML (`database/diagram/erd.mmd`, detalle en `database/docs/data-dictionary.md`).

```
project ─┬─ stage ── activity ── task ─┬─ subtask
         │                             ├─ task_assignment (app_user)
         └─ project_plan_history       ├─ task_plan_history
                                       └─ task_status_history
catálogos: project_status, work_status, task_priority
tipo de cambio: exchange_rate_source ── exchange_rate_quote
```

## Qué pasa con los datos existentes

- Las tablas de V001-V003 se **archivan** sin cambios en el esquema `task_manager_legacy` (no se borra nada).
- Las tareas existentes pasan a un **"Proyecto legado"**: cada categoría es una actividad, las tareas raíz son `task` y las hijas/nietas son `subtask` (conservan su id). Estados: `INACTIVA → PENDIENTE`, `ACTIVA → EN_PROGRESO`, `TERMINADA → COMPLETADA`, eliminadas → `CANCELADA`.
- Una base vacía queda solo con los catálogos sembrados.

## Cómo levantar todo

### Requisitos

- Docker Desktop abierto.
- Java 21 + Maven 3.9 (o usar Maven por Docker, ver más abajo).
- Node.js >= 24.15 para el frontend.

### Opción A: base nueva con Docker (recomendada)

El script de inicialización del contenedor solo corre cuando el volumen de datos está **vacío**. Si ya tenías el contenedor de antes, recréalo (esto borra los datos de prueba locales):

```powershell
git fetch origin
git checkout feature/db-migracion-gestor-proyectos
docker compose down -v      # elimina el contenedor y su volumen
docker compose up -d
docker compose logs db | Select-String "ERROR|init process complete"
```

Verifica (debe listar las migraciones 1 a 4):

```powershell
docker cp database\scripts\verify_schema.sql gestor-tareas-db:/tmp/
docker compose exec -T db psql "host=localhost port=5432 user=gestor_tareas_app dbname=gestor_tareas" -f /tmp/verify_schema.sql
```

### Opción B: conservar tus datos (PostgreSQL ya creado con V001-V003)

Aplica solo la V004 con el dueño de las migraciones. **Debe ir en una sola transacción**:

```powershell
# Base en Docker
docker compose exec -T db psql -v ON_ERROR_STOP=1 --single-transaction -U gestor_tareas_owner -d gestor_tareas -f /migrations/V004__project_management_model.sql -c "INSERT INTO public.database_schema_migrations (version, description, checksum) VALUES (4, 'project management model', 'dd55ed8ca35a66eb96221efa330941db84c17bfdac29b7e7826dff3b0498b5ac')"

# PostgreSQL 18 instalado en Windows
.\database\scripts\migrate.ps1
```

### Backend

```powershell
cd backend
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/gestor_tareas?currentSchema=task_manager"
$env:SPRING_DATASOURCE_USERNAME="gestor_tareas_app"
$env:SPRING_DATASOURCE_PASSWORD="apppass"
mvn spring-boot:run
```

Ajusta el puerto si tu `docker-compose.yml` publica otro (por ejemplo `5433:5432` → `localhost:5433`). Con `ddl-auto: validate`, si arranca es que las entidades JPA coinciden con la base.

Sin Maven instalado puedes compilar con Docker:

```powershell
docker run --rm -v "${PWD}\backend:/app" -w /app maven:3.9-eclipse-temurin-21 mvn -B -DskipTests package
```

### Frontend

```powershell
cd frontend
npm install
npm start
```

> El frontend todavía llama a los endpoints `/api/tasks` del modelo anterior, que se retiraron en esta rama. Mostrará errores hasta que se implementen los nuevos servicios y controladores.

## Estado del backend en esta rama

- Entidades JPA en `backend/.../entity` (una por tabla, según el UML) y repositorios Spring Data en `backend/.../repository`.
- Se retiraron los servicios, controladores, DTOs y pruebas del modelo anterior porque dependían de tablas que ya no existen; siguen disponibles en el historial de `develop`.
- Reglas que deben respetar los nuevos servicios: ver la sección 7 de `database/README.md`.

## Borrar el archivo del modelo anterior

Cuando el equipo confirme que no lo necesita:

```sql
DROP SCHEMA task_manager_legacy CASCADE;  -- ejecutar como gestor_tareas_owner
```
