-- bootstrap dev/test DB inside the official postgres:18 container.
-- Runs AS superuser during docker-entrypoint-initdb.d (only on a fresh data dir).
-- Replicates database/scripts/setup-local.ps1 + migrate.ps1: creates roles, the
-- gestor_tareas database, and applies V001/V002 as the owner role.
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
VALUES (2, 'views and permissions', '985ff1238dfcd2f71ceec5bc848a90a2500d91c60617bba5e057e5b6d66f97b', CURRENT_USER)
ON CONFLICT (version) DO NOTHING;
