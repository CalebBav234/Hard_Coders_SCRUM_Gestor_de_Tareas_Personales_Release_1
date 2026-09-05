export type TaskStatus = 'INACTIVA' | 'ACTIVA' | 'TERMINADA';
export type TaskPriority = 'ALTA' | 'MEDIA' | 'BAJA';
export type RelationType = 'RELACIONADA' | 'BLOQUEA' | 'DEPENDE_DE';

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
  id: number;
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  categoryId?: number | null;
  categoryName?: string | null;
  parentTaskId?: number | null;
  activatedAt?: string | null;
  completedAt?: string | null;
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

export interface TaskRelationResponse {
  id: number;
  targetTaskId: number;
  targetTaskTitle: string;
  relationType: RelationType;
  createdAt: string;
}

export interface CreateTaskRelationRequest {
  targetTaskId: number;
  relationType: RelationType;
}
