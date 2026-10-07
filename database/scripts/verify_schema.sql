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
        'task_manager.app_user',
        'task_manager.project_status',
        'task_manager.work_status',
        'task_manager.task_priority',
        'task_manager.exchange_rate_source',
        'task_manager.exchange_rate_quote',
        'task_manager.project',
        'task_manager.stage',
        'task_manager.activity',
        'task_manager.task',
        'task_manager.subtask',
        'task_manager.task_assignment',
        'task_manager.project_plan_history',
        'task_manager.task_plan_history',
        'task_manager.task_status_history'
    ]) AS expected_table(name)
    WHERE to_regclass(expected_table.name) IS NULL;

    IF missing_tables > 0 THEN
        RAISE EXCEPTION 'Faltan % tablas obligatorias', missing_tables;
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
    has_table_privilege(current_user, 'task_manager.task', 'SELECT') AS can_read_tasks,
    has_table_privilege(current_user, 'task_manager.task', 'INSERT') AS can_create_tasks,
    has_table_privilege(current_user, 'task_manager.task', 'UPDATE') AS can_update_tasks,
    has_table_privilege(current_user, 'task_manager.task_status_history', 'UPDATE') AS can_rewrite_history,
    has_table_privilege(current_user, 'task_manager.work_status', 'INSERT') AS can_edit_catalogs;

SELECT
    (SELECT count(*) FROM task_manager.project) AS projects,
    (SELECT count(*) FROM task_manager.task) AS tasks,
    (SELECT count(*) FROM task_manager.subtask) AS subtasks,
    (SELECT count(*) FROM task_manager.work_status) AS work_statuses,
    (SELECT count(*) FROM task_manager.task_priority) AS priorities;
