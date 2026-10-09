/**
 * Contrato HTTP del backend (modelo V4: tarea → subtarea, actividades, historial de estados).
 * La interfaz no usa estos tipos directamente: TaskService los traduce al modelo de vista (task.ts).
 */

export type WorkStatusName = 'PENDIENTE' | 'EN_PROGRESO' | 'COMPLETADA' | 'CANCELADA';
export type PriorityName = 'ALTA' | 'MEDIA' | 'BAJA';

export interface SubtaskDto {
  subtaskId: number;
  taskId: number;
  title: string;
  description: string | null;
  status: WorkStatusName;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface TaskDto {
  taskId: number;
  activityId: number;
  /** Nulo cuando la tarea está en la actividad "Sin categoría". */
  activityName: string | null;
  title: string;
  description: string | null;
  status: WorkStatusName;
  priority: PriorityName;
  allocatedBudgetBob: number;
  estimatedDurationMinutes: number;
  actualDurationMinutes: number | null;
  /** Segundos trabajados en segmentos ya cerrados. */
  trackedSeconds: number;
  /** Inicio del segmento en curso (solo EN_PROGRESO). */
  currentSegmentStartedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  subtasks: SubtaskDto[];
}

export interface StatusEventDto {
  eventId: number;
  previousStatus: WorkStatusName | null;
  newStatus: WorkStatusName;
  reason: string;
  changedAt: string;
}

export interface TaskHistoryDto {
  task: TaskDto;
  cancelledAt: string | null;
  events: StatusEventDto[];
}

export interface ActivityDto {
  activityId: number;
  name: string;
}

export interface CreateTaskBody {
  title: string;
  priority: PriorityName;
  activityName: string | null;
}

export interface UpdateTaskBody {
  title: string;
  description: string | null;
  priority: PriorityName;
  version: number;
}

export interface CreateSubtaskBody {
  title: string;
  description: string | null;
}

export interface UpdateSubtaskBody {
  title: string;
  description: string | null;
  version: number;
}
