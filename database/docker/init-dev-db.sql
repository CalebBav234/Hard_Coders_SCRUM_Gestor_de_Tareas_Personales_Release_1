-- bootstrap dev/test DB inside the official postgres:18 container.
-- Runs AS superuser during docker-entrypoint-initdb.d (only on a fresh data dir).
-- Replicates database/scripts/setup-local.ps1 + migrate.ps1: creates roles, the
-- gestor_tareas database, and applies V001..V006 as the owner role.
-- DEV ONLY: passwords below are local/test defaults. Do not use in production.

-- 1. Roles
DO $init$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'gestor_tareas_owner') THEN
    CREATE ROLE gestor_tareas_owner LOGIN PASSWORD 'ownerpass' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'gestor_tareas_app') THEN
    CREATE ROLE gestor_tareas_app LOGIN PASSWORD 'apppass' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
  END IF;
END $init$;

-- 2. Database + connect grant
SELECT format(
  'CREATE DATABASE gestor_tareas OWNER gestor_tareas_owner ENCODING ''UTF8'' TEMPLATE template0'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'gestor_tareas')
\gexec
GRANT CONNECT ON DATABASE gestor_tareas TO gestor_tareas_app;

-- 3. Connect as the owner so the schema and objects are owned by it.
--    Auth is 'trust' (see docker-compose.yml), so no password is needed here.
\connect gestor_tareas gestor_tareas_owner

-- 4. Migration-tracking table (required by verify-local.ps1 / migrate.ps1)
CREATE TABLE IF NOT EXISTS public.database_schema_migrations (
    version     integer PRIMARY KEY,
    description varchar(200) NOT NULL,
    checksum    char(64) NOT NULL,
    applied_at  timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_by  name NOT NULL DEFAULT CURRENT_USER
);
REVOKE ALL ON public.database_schema_migrations FROM PUBLIC;

-- 5. Apply migrations (single source of truth = /migrations)
\i /migrations/V001__initial_schema.sql
INSERT INTO public.database_schema_migrations (version, description, checksum, applied_by)
VALUES (1, 'initial schema', 'c4d290198c22ecd90108c7afafd6002f52ee8f2cf2f8939a2bbeaf6daed0086d', CURRENT_USER)
ON CONFLICT (version) DO NOTHING;

\i /migrations/V002__views_and_permissions.sql
INSERT INTO public.database_schema_migrations (version, description, checksum, applied_by)
VALUES (2, 'views and permissions', '985ff1238dfcd2f71ceec5bc848a90a2500d91c60617bbaa5e057e5b6d66f97b', CURRENT_USER)
ON CONFLICT (version) DO NOTHING;

\i /migrations/V003__task_archive.sql
INSERT INTO public.database_schema_migrations (version, description, checksum, applied_by)
VALUES (3, 'task archive', 'a5b8d1568195c6ea03e58babd4f41ee4ed0b38207ab6c89c287e98a1c4b31772', CURRENT_USER)
ON CONFLICT (version) DO NOTHING;

-- V004 must run in ONE transaction: its deferred constraint triggers require the
-- initial plan/status history rows to be inserted together with each project/task.
BEGIN;
\i /migrations/V004__project_management_model.sql
INSERT INTO public.database_schema_migrations (version, description, checksum, applied_by)
VALUES (4, 'project management model', 'dd55ed8ca35a66eb96221efa330941db84c17bfdac29b7e7826dff3b0498b5ac', CURRENT_USER)
ON CONFLICT (version) DO NOTHING;
COMMIT;

-- V005: sort_order para reordenar por drag-and-drop (R6). Sin esta migración el backend
-- no arranca (ddl-auto: validate exige stage/activity/task.sort_order).
\i /migrations/V005__add_sort_order.sql
INSERT INTO public.database_schema_migrations (version, description, checksum, applied_by)
VALUES (5, 'add sort order', '4bb94a97158a0f808f167079194fe2732f3468b1f42b844f3a5e3baa852dc3ab', CURRENT_USER)
ON CONFLICT (version) DO NOTHING;

-- V006: motivo persistido de la asignación de Personal (EC3-02).
\i /migrations/V006__task_assignment_reason.sql
INSERT INTO public.database_schema_migrations (version, description, checksum, applied_by)
VALUES (6, 'task assignment reason', 'e1bd108bc203360e4e7e5a363d8aa1ff4ca0be4b65161f21552ab92ab9ac1dec', CURRENT_USER)
ON CONFLICT (version) DO NOTHING;
