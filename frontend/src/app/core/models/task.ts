export type TaskStatus = 'INACTIVA' | 'ACTIVA' | 'TERMINADA';
export type TaskPriority = 'ALTA' | 'MEDIA' | 'BAJA';

export interface Task {
  id: number;
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  categoryId?: number | null;
  parentTaskId?: number | null;
  activatedAt?: string | null;
  completedAt?: string | null;
  totalActiveSeconds?: number;
  effectiveActiveSeconds?: number;
  createdAt?: string | null;
  updatedAt?: string | null;
  version: number;
}
