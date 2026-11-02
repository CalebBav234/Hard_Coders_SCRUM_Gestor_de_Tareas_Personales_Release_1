import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { toUiStatus, subtaskUiId, taskToView, subtaskToView, flattenTasks, taskHistoryToView, subtaskHistoryToView, archiveTime } from './task-mapper';
import { TaskDto, SubtaskDto, StatusEventDto, TaskHistoryDto } from '../models/api';

const baseTaskDto: TaskDto = {
  taskId: 7,
  activityId: 4,
  activityName: 'Cimentación',
  title: 'Excavar',
  description: null,
  status: 'PENDIENTE',
  priority: 'ALTA',
  allocatedBudgetBob: 0,
  estimatedDurationMinutes: 60,
  actualDurationMinutes: null,
  trackedSeconds: 100,
  currentSegmentStartedAt: null,
  completedAt: null,
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
  version: 3,
  assignedWorkerIds: [2],
  subtasks: [],
};

describe('task-mapper', () => {
  it('toUiStatus maps known statuses and falls back for null/CANCELADA', () => {
    expect(toUiStatus('PENDIENTE')).toBe('INACTIVA');
    expect(toUiStatus('EN_PROGRESO')).toBe('ACTIVA');
    expect(toUiStatus('COMPLETADA')).toBe('TERMINADA');
    expect(toUiStatus(null)).toBe('INACTIVA');
    expect(toUiStatus(undefined)).toBe('INACTIVA');
    expect(toUiStatus('CANCELADA')).toBe('INACTIVA');
    expect(toUiStatus('CANCELADA', 'ACTIVA')).toBe('ACTIVA');
  });

  it('subtaskUiId negates the id', () => {
    expect(subtaskUiId(7)).toBe(-7);
  });

  it('taskToView maps a dto to a view task', () => {
    const view = taskToView(baseTaskDto, 1_000_000);
    expect(view.id).toBe(7);
    expect(view.kind).toBe('TASK');
    expect(view.status).toBe('INACTIVA');
    expect(view.categoryId).toBe(4);
    expect(view.categoryName).toBe('Cimentación');
    expect(view.assignedWorkerIds).toEqual([2]);
    expect(view.effectiveActiveSeconds).toBe(100);
  });

  it('taskToView handles missing segment start and missing activity name', () => {
    const view = taskToView({ ...baseTaskDto, currentSegmentStartedAt: null, activityName: null });
    expect(view.categoryId).toBeNull();
    expect(view.effectiveActiveSeconds).toBe(100);
  });

  it('taskToView computes running seconds from an active segment', () => {
    const started = '2026-10-01T09:00:00Z';
    const now = new Date(started).getTime() + 5000;
    const view = taskToView({ ...baseTaskDto, currentSegmentStartedAt: started, trackedSeconds: 10 }, now);
    expect(view.effectiveActiveSeconds).toBe(15);
  });

  it('taskToView falls back to an empty worker list when none is assigned', () => {
    const view = taskToView({ ...baseTaskDto, assignedWorkerIds: null });
    expect(view.assignedWorkerIds).toEqual([]);
  });

  it('subtaskToView inherits parent priority and marks completed', () => {
    const dto: SubtaskDto = {
      subtaskId: 3,
      taskId: 7,
      title: 'Hija',
      description: null,
      status: 'COMPLETADA',
      completedAt: '2026-10-02T10:00:00Z',
      createdAt: '2026-10-01T10:00:00Z',
      updatedAt: '2026-10-01T10:00:00Z',
      version: 1,
    };
    const view = subtaskToView(dto, 'BAJA');
    expect(view.id).toBe(-3);
    expect(view.kind).toBe('SUBTASK');
    expect(view.priority).toBe('BAJA');
    expect(view.status).toBe('TERMINADA');
    expect(view.parentTaskId).toBe(7);
  });

  it('subtaskToView marks an incomplete subtask as INACTIVA', () => {
    const dto: SubtaskDto = {
      subtaskId: 3,
      taskId: 7,
      title: 'Hija',
      description: null,
      status: 'PENDIENTE',
      completedAt: null,
      createdAt: 'x',
      updatedAt: 'y',
      version: 1,
    };
    const view = subtaskToView(dto, 'BAJA');
    expect(view.status).toBe('INACTIVA');
    expect(view.deletedAt).toBeUndefined();
  });

  it('flattenTasks produces task followed by subtasks', () => {
    const dto: TaskDto = {
      ...baseTaskDto,
      subtasks: [{ subtaskId: 1, taskId: 7, title: 'Hija', description: null, status: 'PENDIENTE', completedAt: null, createdAt: 'x', updatedAt: 'y', version: 1 }],
    };
    const views = flattenTasks([dto]);
    expect(views.length).toBe(2);
    expect(views[0].id).toBe(7);
    expect(views[1].id).toBe(-1);
  });

  it('taskHistoryToView handles cancelled and non-cancelled tasks', () => {
    const dto: TaskHistoryDto = {
      task: { ...baseTaskDto, status: 'CANCELADA', trackedSeconds: 50 },
      events: [
        { eventId: 1, previousStatus: 'PENDIENTE', newStatus: 'EN_PROGRESO', reason: 'Inicio', changedAt: 'a' },
        { eventId: 2, previousStatus: 'EN_PROGRESO', newStatus: 'CANCELADA', reason: 'Baja', changedAt: 'b' },
      ],
      cancelledAt: 'c',
    };
    const history = taskHistoryToView(dto);
    expect(history.status).toBe('ACTIVA');
    expect(history.effectiveActiveSeconds).toBe(50);
    expect(history.deletedAt).toBe('c');
    expect(history.events.length).toBe(1);
    expect(history.events[0].fromStatus).toBe('INACTIVA');
    expect(history.events[0].toStatus).toBe('ACTIVA');
  });

  it('taskHistoryToView keeps the base status when not cancelled', () => {
    const dto: TaskHistoryDto = {
      task: { ...baseTaskDto, status: 'EN_PROGRESO', trackedSeconds: 30 },
      events: [{ eventId: 1, previousStatus: 'PENDIENTE', newStatus: 'EN_PROGRESO', reason: 'Inicio', changedAt: 'a' }],
      cancelledAt: null,
    };
    const history = taskHistoryToView(dto);
    expect(history.status).toBe('ACTIVA');
    expect(history.deletedAt).toBeNull();
    expect(history.events.length).toBe(1);
  });

  it('subtaskHistoryToView marks deleted when cancelled', () => {
    const dto: SubtaskDto = {
      subtaskId: 3,
      taskId: 7,
      title: 'Hija',
      description: null,
      status: 'CANCELADA',
      completedAt: null,
      createdAt: 'x',
      updatedAt: 'd',
      version: 1,
    };
    const history = subtaskHistoryToView(dto);
    expect(history.deletedAt).toBe('d');
    expect(history.events).toEqual([]);
  });

  it('subtaskHistoryToView does not set deletedAt when not cancelled', () => {
    const dto: SubtaskDto = {
      subtaskId: 3,
      taskId: 7,
      title: 'Hija',
      description: null,
      status: 'PENDIENTE',
      completedAt: null,
      createdAt: 'x',
      updatedAt: 'y',
      version: 1,
    };
    const history = subtaskHistoryToView(dto);
    expect(history.deletedAt).toBeNull();
  });

  it('archiveTime falls back through updatedAt and createdAt', () => {
    expect(archiveTime({ deletedAt: null, completedAt: null, updatedAt: 'b', createdAt: 'a' } as any)).toBe(new Date('b').getTime());
    expect(archiveTime({ deletedAt: null, completedAt: null, updatedAt: null, createdAt: 'a' } as any)).toBe(new Date('a').getTime());
  });

  it('archiveTime returns 0 when there are no dates', () => {
    expect(archiveTime({ deletedAt: null, completedAt: null, updatedAt: null, createdAt: null } as any)).toBe(0);
  });
});
