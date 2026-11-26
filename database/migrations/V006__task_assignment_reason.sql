-- EC3-02: persistir el motivo de la asignación de Personal (US2.3).
-- La columna es nullable para no romper las asignaciones existentes; las nuevas
-- asignaciones siempre guardan un motivo (el explícito o "Asignación de Personal").
-- El límite de 500 caracteres coincide con el input HTML y con AssignWorkerRequest (EC3-03).
ALTER TABLE task_manager.task_assignment ADD COLUMN IF NOT EXISTS reason varchar(500) NULL;

DO $v006$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_task_assignment_reason') THEN
    ALTER TABLE task_manager.task_assignment
      ADD CONSTRAINT ck_task_assignment_reason
      CHECK (reason IS NULL OR length(btrim(reason)) >= 1);
  END IF;
END $v006$;
