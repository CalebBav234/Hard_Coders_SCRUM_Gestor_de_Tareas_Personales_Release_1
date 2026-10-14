import {
  StatusEventDto,
  SubtaskDto,
  TaskDto,
  TaskHistoryDto,
  WorkStatusName,
} from '../models/api';
import { Task, TaskHistory, TaskPriority, TaskStatus, TaskStatusHistory } from '../models/task';

/** Traducción explícita entre el contrato V4 y el modelo de vista. */

const UI_STATUS: Record<Exclude<WorkStatusName, 'CANCELADA'>, TaskStatus> = {
  PENDIENTE: 'INACTIVA',
  EN_PROGRESO: 'ACTIVA',
  COMPLETADA: 'TERMINADA',
};

export function toUiStatus(status: WorkStatusName | null | undefined, fallback: TaskStatus = 'INACTIVA'): TaskStatus {
  if (!status || status === 'CANCELADA') {
    return fallback;
  }
  return UI_STATUS[status];
}

export function subtaskUiId(subtaskId: number): number {
  return -subtaskId;
}

export function taskToView(dto: TaskDto, now = Date.now()): Task {
  const startedAt = dto.currentSegmentStartedAt ? new Date(dto.currentSegmentStartedAt).getTime() : null;
  const running = startedAt === null ? 0 : Math.max(0, Math.floor((now - startedAt) / 1000));
  return {
    id: dto.taskId,
    kind: 'TASK',
    entityId: dto.taskId,
    title: dto.title,
    description: dto.description,
    status: toUiStatus(dto.status),
    priority: dto.priority,
    categoryId: dto.activityName ? dto.activityId : null,
    categoryName: dto.activityName,
    assignedWorkerIds: dto.assignedWorkerIds ?? [],
    parentTaskId: null,
    activatedAt: dto.currentSegmentStartedAt,
    completedAt: dto.completedAt,
    totalActiveSeconds: dto.trackedSeconds,
    effectiveActiveSeconds: dto.trackedSeconds + running,
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
    version: dto.version,
  };
}

/** Las subtareas no tienen prioridad propia en V4: heredan la de su tarea solo para mostrarse. */
export function subtaskToView(dto: SubtaskDto, parentPriority: TaskPriority = 'MEDIA'): Task {
  return {
    id: subtaskUiId(dto.subtaskId),
    kind: 'SUBTASK',
    entityId: dto.subtaskId,
    title: dto.title,
    description: dto.description,
    status: toUiStatus(dto.status, dto.completedAt ? 'TERMINADA' : 'INACTIVA'),
    priority: parentPriority,
    categoryId: null,
    categoryName: null,
    parentTaskId: dto.taskId,
    activatedAt: null,
    completedAt: dto.completedAt,
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
    version: dto.version,
  };
}

/** Lista plana como la esperaba la interfaz: cada tarea seguida de sus subtareas. */
export function flattenTasks(dtos: TaskDto[], now = Date.now()): Task[] {
  return dtos.flatMap((dto) => [
    taskToView(dto, now),
    ...dto.subtasks.map((subtask) => subtaskToView(subtask, dto.priority)),
  ]);
}

function eventToView(event: StatusEventDto): TaskStatusHistory {
  return {
    id: event.eventId,
    fromStatus: event.previousStatus ? toUiStatus(event.previousStatus) : null,
    toStatus: toUiStatus(event.newStatus),
    changeReason: event.reason,
    changedAt: event.changedAt,
  };
}

/**
 * En V4 eliminar = pasar a CANCELADA. Para la interfaz, la tarea conserva el estado que tenía
 * antes de eliminarse y la cancelación se muestra como fecha de eliminación.
 */
export function taskHistoryToView(dto: TaskHistoryDto): TaskHistory {
  const base = taskToView(dto.task);
  const cancellation = dto.events.find((event) => event.newStatus === 'CANCELADA');
  return {
    ...base,
    status: dto.task.status === 'CANCELADA'
      ? toUiStatus(cancellation?.previousStatus, 'INACTIVA')
      : base.status,
    effectiveActiveSeconds: dto.task.trackedSeconds,
    deletedAt: dto.cancelledAt,
    events: dto.events.filter((event) => event.newStatus !== 'CANCELADA').map(eventToView),
  };
}

export function subtaskHistoryToView(dto: SubtaskDto): TaskHistory {
  return {
    ...subtaskToView(dto),
    deletedAt: dto.status === 'CANCELADA' ? dto.updatedAt : null,
    events: [],
  };
}

export function archiveTime(task: TaskHistory): number {
  const value = task.deletedAt ?? task.completedAt ?? task.updatedAt ?? task.createdAt;
  return value ? new Date(value).getTime() : 0;
}
