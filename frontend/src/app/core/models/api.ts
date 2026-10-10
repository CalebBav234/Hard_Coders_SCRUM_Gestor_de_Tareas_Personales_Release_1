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

/** US3.1: tarea asignada al usuario del token. Nunca trae presupuesto ni duración. */
export interface AssignedTaskDto {
  taskId: number;
  projectId: number | null;
  activityId: number;
  activityName: string | null;
  title: string;
  description: string | null;
  status: WorkStatusName;
  priority: PriorityName;
  actualDurationMinutes: number | null;
  trackedSeconds: number;
  currentSegmentStartedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  subtasks: SubtaskDto[];
}

export interface StageDto {
  stageId: number;
  projectId: number;
  name: string;
  description: string | null;
  status: WorkStatusName;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface ActivityDetailDto {
  activityId: number;
  stageId: number;
  name: string;
  description: string | null;
  status: WorkStatusName;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface AssignmentDto {
  taskId: number;
  taskTitle: string;
  workerUserId: number;
  workerName: string;
  assignedAt: string;
  assignedByUserId: number;
  assignedByName: string;
  reason: string | null;
}

export interface ProgressNodeDto {
  id: number | null;
  name: string;
  completed: number;
  total: number;
  leaf: boolean;
  ratio: number;
  children: ProgressNodeDto[];
}

export interface ProjectProgressDto {
  projectId: number;
  projectName: string;
  projectStatus: string;
  budgetBob: number;
  allocatedBudgetBob: number;
  estimatedDurationMinutes: number | null;
  root: ProgressNodeDto;
}

/** Alias de lectura para las pruebas y los componentes que solo necesitan el árbol. */
export type ProgressResponseLike = ProjectProgressDto;

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
  stageId: number;
  name: string;
  description: string | null;
  status: WorkStatusName;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateTaskBody {
  activityId: number;
  title: string;
  priority: PriorityName;
  allocatedBudgetBob: number;
  estimatedDurationMinutes: number;
}

export interface UpdateTaskBody {
  title: string;
  description: string | null;
  priority: PriorityName;
  version: number;
}

/** US2.2: presupuesto y duración de la tarea, con motivo obligatorio. */
export interface UpdateTaskPlanBody {
  allocatedBudgetBob: number;
  estimatedDurationMinutes: number;
  reason: string;
  version: number;
}

/** Transición de estado; el motivo es obligatorio cuando la ejecuta el Jefe. */
export interface TransitionBody {
  version: number;
  reason?: string;
}

export interface CreateSubtaskBody {
  title: string;
  description: string | null;
}

export interface CreateStageBody {
  name: string;
  description: string | null;
}

export interface CreateActivityBody {
  name: string;
  description: string | null;
}

export interface AssignWorkerBody {
  workerUserId: number;
  reason: string | null;
}

export interface UpdateSubtaskBody {
  title: string;
  description: string | null;
  version: number;
}
