/**
 * Modelo de vista de la interfaz. Conserva el vocabulario de la pantalla (INACTIVA / ACTIVA /
 * TERMINADA, "categoría") y TaskService lo construye a partir del contrato V4 (api.ts).
 */
export type TaskStatus = 'INACTIVA' | 'ACTIVA' | 'TERMINADA';
export type TaskPriority = 'ALTA' | 'MEDIA' | 'BAJA';

/** TASK = fila de task; SUBTASK = fila de subtask. */
export type TaskKind = 'TASK' | 'SUBTASK';

export interface CategorySummary {
  id: number;
  name: string;
}

export interface UpdateTaskRequest {
  title: string;
  description: string | null;
  priority: TaskPriority;
  version: number;
}

export interface Task {
  /**
   * Identificador en la interfaz: el id de la tarea, o el id de la subtarea en negativo
   * (task y subtask tienen secuencias independientes y sus ids pueden coincidir).
   */
  id: number;
  kind?: TaskKind;
  /** Id real en su tabla (task_id o subtask_id). */
  entityId?: number;
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  categoryId?: number | null;
  categoryName?: string | null;
  /** US2.3: ids del Personal asignado a la tarea (solo en la vista de proyecto). */
  assignedWorkerIds?: number[];
  parentTaskId?: number | null;
  activatedAt?: string | null;
  completedAt?: string | null;
  /** Solo las tareas registran tiempo; en subtareas queda indefinido y el cronómetro no se muestra. */
  totalActiveSeconds?: number;
  effectiveActiveSeconds?: number;
  createdAt?: string | null;
  updatedAt?: string | null;
  version: number;
}

export interface TaskStatusHistory {
  id: number;
  fromStatus?: TaskStatus | null;
  toStatus: TaskStatus;
  changeReason?: string | null;
  changedAt: string;
}

export interface TaskHistory extends Task {
  deletedAt?: string | null;
  events: TaskStatusHistory[];
}

export function isSubtask(task: Pick<Task, 'id' | 'kind'>): boolean {
  return task.kind === 'SUBTASK' || task.id < 0;
}
