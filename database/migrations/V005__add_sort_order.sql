-- Add sort_order for drag-and-drop reordering (R6)
ALTER TABLE task_manager.stage ADD COLUMN IF NOT EXISTS sort_order integer NOT NULL DEFAULT 0;
ALTER TABLE task_manager.activity ADD COLUMN IF NOT EXISTS sort_order integer NOT NULL DEFAULT 0;
ALTER TABLE task_manager.task ADD COLUMN IF NOT EXISTS sort_order integer NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS ix_stage_project_sort_order ON task_manager.stage (project_id, sort_order, stage_id);
CREATE INDEX IF NOT EXISTS ix_activity_stage_sort_order ON task_manager.activity (stage_id, sort_order, activity_id);
CREATE INDEX IF NOT EXISTS ix_task_activity_sort_order ON task_manager.task (activity_id, sort_order, task_id);
