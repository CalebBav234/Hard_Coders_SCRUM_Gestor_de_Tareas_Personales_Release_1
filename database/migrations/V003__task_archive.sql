-- US-12: archivo consultable de tareas terminadas o eliminadas lógicamente.

CREATE OR REPLACE VIEW task_manager.v_task_archive
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
                THEN floor(EXTRACT(epoch FROM (
                    COALESCE(task.deleted_at, CURRENT_TIMESTAMP) - task.activated_at
                )))::bigint
            ELSE 0
          END AS effective_active_seconds,
    task.created_at,
    task.updated_at,
    task.deleted_at,
    task.version
FROM task_manager.tasks AS task
LEFT JOIN task_manager.categories AS category
    ON category.id = task.category_id
WHERE task.status = 'TERMINADA'
   OR task.deleted_at IS NOT NULL;

COMMENT ON VIEW task_manager.v_task_archive IS
    'Historial unificado de tareas terminadas y eliminadas, con su último estado y tiempos';

DO $grant_archive_permissions$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'gestor_tareas_app') THEN
        EXECUTE 'GRANT SELECT ON task_manager.v_task_archive TO gestor_tareas_app';
    END IF;
END;
$grant_archive_permissions$;
