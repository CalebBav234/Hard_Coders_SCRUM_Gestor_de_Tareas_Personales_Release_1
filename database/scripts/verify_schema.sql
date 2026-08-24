\set ON_ERROR_STOP on

DO $verification$
DECLARE
    server_major integer := current_setting('server_version_num')::integer / 10000;
    missing_tables integer;
BEGIN
    IF server_major <> 18 THEN
        RAISE EXCEPTION 'Se esperaba PostgreSQL 18 y el servidor reporta versión mayor %', server_major;
    END IF;

    IF to_regnamespace('task_manager') IS NULL THEN
        RAISE EXCEPTION 'No existe el esquema task_manager';
    END IF;

    SELECT count(*)
    INTO missing_tables
    FROM unnest(ARRAY[
        'task_manager.categories',
        'task_manager.tags',
        'task_manager.tasks',
        'task_manager.task_tags',
        'task_manager.task_relations',
        'task_manager.task_status_history',
        'task_manager.task_time_entries'
    ]) AS expected_table(name)
    WHERE to_regclass(expected_table.name) IS NULL;

    IF missing_tables > 0 THEN
        RAISE EXCEPTION 'Faltan % tablas obligatorias', missing_tables;
    END IF;

    IF to_regclass('task_manager.v_tasks') IS NULL
       OR to_regclass('task_manager.v_pending_tasks') IS NULL
       OR to_regclass('task_manager.v_task_history') IS NULL THEN
        RAISE EXCEPTION 'Falta una o más vistas obligatorias';
    END IF;
END;
$verification$;

SELECT
    current_database() AS database_name,
    current_user AS connected_as,
    current_setting('server_version') AS postgres_version,
    current_setting('TimeZone') AS timezone;

SELECT version, description, applied_at, applied_by
FROM public.database_schema_migrations
ORDER BY version;

SELECT
    has_schema_privilege(current_user, 'task_manager', 'USAGE') AS can_use_schema,
    has_table_privilege(current_user, 'task_manager.tasks', 'SELECT') AS can_read_tasks,
    has_table_privilege(current_user, 'task_manager.tasks', 'INSERT') AS can_create_tasks,
    has_table_privilege(current_user, 'task_manager.tasks', 'UPDATE') AS can_update_tasks,
    has_table_privilege(current_user, 'task_manager.tasks', 'DELETE') AS can_hard_delete_tasks;

SELECT
    (SELECT count(*) FROM task_manager.v_tasks) AS visible_tasks,
    (SELECT count(*) FROM task_manager.v_pending_tasks) AS pending_tasks,
    (SELECT count(*) FROM task_manager.v_task_history) AS history_events;
