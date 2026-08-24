-- Vistas de consulta y permisos mínimos para la cuenta de la aplicación.

CREATE OR REPLACE VIEW task_manager.v_tasks
WITH (security_invoker = true)
AS
SELECT
    task.id,
    task.title,
    task.description,
    task.status,
    task.priority,
    task.category_id,
    category.name AS category_name,
    task.parent_task_id,
    task.activated_at,
    task.completed_at,
    task.total_active_seconds,
    task.total_active_seconds
        + CASE
            WHEN task.status = 'ACTIVA' AND task.activated_at IS NOT NULL
                THEN floor(EXTRACT(epoch FROM (CURRENT_TIMESTAMP - task.activated_at)))::bigint
            ELSE 0
          END AS effective_active_seconds,
    task.created_at,
    task.updated_at,
    task.version,
    COALESCE(tag_values.tags, ARRAY[]::varchar[]) AS tags
FROM task_manager.tasks AS task
LEFT JOIN task_manager.categories AS category
    ON category.id = task.category_id
   AND category.deleted_at IS NULL
LEFT JOIN LATERAL (
    SELECT array_agg(tag.name ORDER BY lower(tag.name)) AS tags
    FROM task_manager.task_tags AS task_tag
    JOIN task_manager.tags AS tag ON tag.id = task_tag.tag_id
    WHERE task_tag.task_id = task.id
      AND tag.deleted_at IS NULL
) AS tag_values ON true
WHERE task.deleted_at IS NULL;

COMMENT ON VIEW task_manager.v_tasks IS
    'Tareas visibles con categoría, etiquetas y tiempo activo efectivo';

CREATE OR REPLACE VIEW task_manager.v_pending_tasks
WITH (security_invoker = true)
AS
SELECT *
FROM task_manager.v_tasks
WHERE status IN ('INACTIVA', 'ACTIVA');

COMMENT ON VIEW task_manager.v_pending_tasks IS
    'Vista derivada de pendientes; no introduce un estado adicional';

CREATE OR REPLACE VIEW task_manager.v_task_history
WITH (security_invoker = true)
AS
SELECT
    history.id,
    history.task_id,
    task.title AS task_title,
    history.from_status,
    history.to_status,
    history.change_reason,
    history.changed_at,
    task.deleted_at AS task_deleted_at
FROM task_manager.task_status_history AS history
JOIN task_manager.tasks AS task ON task.id = history.task_id;

COMMENT ON VIEW task_manager.v_task_history IS
    'Historial de transiciones, incluidas las tareas con borrado lógico';

REVOKE ALL ON SCHEMA task_manager FROM PUBLIC;

DO $grant_permissions$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'gestor_tareas_app') THEN
        EXECUTE 'GRANT USAGE ON SCHEMA task_manager TO gestor_tareas_app';

        EXECUTE 'GRANT SELECT, INSERT, UPDATE '
             || 'ON task_manager.categories, task_manager.tags, task_manager.tasks '
             || 'TO gestor_tareas_app';

        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE '
             || 'ON task_manager.task_tags, task_manager.task_relations, '
             || 'task_manager.task_time_entries TO gestor_tareas_app';

        EXECUTE 'GRANT SELECT, INSERT '
             || 'ON task_manager.task_status_history TO gestor_tareas_app';

        EXECUTE 'GRANT SELECT '
             || 'ON task_manager.v_tasks, task_manager.v_pending_tasks, '
             || 'task_manager.v_task_history TO gestor_tareas_app';

        EXECUTE 'GRANT SELECT ON public.database_schema_migrations '
             || 'TO gestor_tareas_app';

        EXECUTE 'GRANT USAGE, SELECT ON ALL SEQUENCES '
             || 'IN SCHEMA task_manager TO gestor_tareas_app';

        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA task_manager '
             || 'GRANT SELECT, INSERT, UPDATE ON TABLES TO gestor_tareas_app';

        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA task_manager '
             || 'GRANT USAGE, SELECT ON SEQUENCES TO gestor_tareas_app';
    END IF;
END;
$grant_permissions$;
