import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { ActivityDetailDto, StageDto } from '../../../core/models/api';
import { Project } from '../../../core/models/project';
import { Task } from '../../../core/models/task';
import { HierarchyStateService } from './hierarchy-state.service';

describe('HierarchyStateService', () => {
  let service: HierarchyStateService;

  const project: Project = {
    projectId: 1,
    name: 'Proyecto',
    description: null,
    status: 'PENDIENTE',
    closed: false,
    budgetBob: 1000,
    allocatedBudgetBob: 0,
    estimatedDurationMinutes: 60,
    manager: { userId: 2, fullName: 'Ana', email: 'a@gestor.local', chief: false, active: true },
    createdBy: { userId: 2, fullName: 'Ana', email: 'a@gestor.local', chief: false, active: true },
    currentRevision: 1,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 1,
    canEditDetails: true,
    canEditPlan: true,
    canDelegate: true,
    canManageHierarchy: true,
    allowedStatuses: [],
  };

  const stage: StageDto = {
    stageId: 4,
    projectId: 1,
    name: 'Etapa',
    description: null,
    status: 'PENDIENTE',
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 1,
    activityCount: 0,
  };

  const activity: ActivityDetailDto = {
    activityId: 6,
    stageId: 4,
    name: 'Actividad',
    description: null,
    status: 'PENDIENTE',
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 1,
    taskCount: 0,
  };

  const task: Task = {
    id: 7,
    title: 'Tarea',
    description: null,
    status: 'PENDIENTE',
    priority: 'MEDIA',
    projectId: 1,
    activityId: 4,
    assignedToUserId: null,
    assignedWorkerIds: [],
    estimatedMinutes: 60,
    actualMinutes: null,
    completed: false,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 1,
    subtasks: [],
  };

  beforeEach(() => {
    TestBed.resetTestingModule();
    service = TestBed.inject(HierarchyStateService);
    service.setProjects([]);
    service.setStages([]);
    service.setActivities([]);
    service.setTasks([]);
    service.selectProject(null);
  });

  function first<T>(observable: Observable<T>): T {
    let value: T | null = null;
    observable.subscribe((v) => (value = v));
    return value as T;
  }

  it('setProjects exposes the projects list', () => {
    service.setProjects([project]);
    expect(first(service.projects$)).toEqual([project]);
  });

  it('selectProject with null resets the collections', () => {
    service.setStages([stage]);
    service.selectProject(null);

    expect(first(service.selectedProject$)).toBeNull();
    expect(first(service.stages$)).toEqual([]);
  });

  it('setStages, addStage, updateStage and removeStage', () => {
    service.setStages([stage]);
    expect(first(service.stages$).map((s) => s.stageId)).toEqual([4]);

    const added = { ...stage, stageId: 8, name: 'Nueva' };
    service.addStage(added);
    expect(first(service.stages$).map((s) => s.stageId)).toEqual([4, 8]);

    // Añadir una etapa duplicada no la duplica.
    service.addStage(added);
    expect(first(service.stages$).map((s) => s.stageId)).toEqual([4, 8]);

    const updated = { ...stage, name: 'Etapa editada' };
    service.updateStage(updated);
    expect(first(service.stages$).find((s) => s.stageId === 4)?.name).toBe('Etapa editada');

    service.removeStage(8);
    expect(first(service.stages$).map((s) => s.stageId)).toEqual([4]);
  });

  it('setActivities, addActivity and updateActivity', () => {
    service.setActivities([activity]);
    expect(first(service.activities$).map((a) => a.activityId)).toEqual([6]);

    const added = { ...activity, activityId: 9, name: 'Otra' };
    service.addActivity(added);
    expect(first(service.activities$).map((a) => a.activityId)).toEqual([6, 9]);

    // Añadir una actividad duplicada no la duplica.
    service.addActivity(added);
    expect(first(service.activities$).map((a) => a.activityId)).toEqual([6, 9]);

    const updated = { ...activity, name: 'Actividad editada' };
    service.updateActivity(updated);
    expect(first(service.activities$).find((a) => a.activityId === 6)?.name).toBe('Actividad editada');
  });

  it('setTasks, addTask, updateTask and removeTask', () => {
    service.setTasks([task]);
    expect(first(service.tasks$).map((t) => t.id)).toEqual([7]);

    const added = { ...task, id: 8, title: 'Otra tarea' };
    service.addTask(added);
    expect(first(service.tasks$).map((t) => t.id)).toEqual([8, 7]);

    // Añadir una tarea duplicada no la duplica.
    service.addTask(added);
    expect(first(service.tasks$).map((t) => t.id)).toEqual([8, 7]);

    const updated = { ...task, title: 'Tarea editada' };
    service.updateTask(updated);
    expect(first(service.tasks$).find((t) => t.id === 7)?.title).toBe('Tarea editada');

    service.removeTask(7);
    expect(first(service.tasks$).map((t) => t.id)).toEqual([8]);
  });
});
