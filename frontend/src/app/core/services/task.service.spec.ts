import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SubtaskDto, TaskDto, TaskHistoryDto } from '../models/api';
import { Task, TaskHistory } from '../models/task';
import { TaskService } from './task.service';

function taskDto(overrides: Partial<TaskDto> = {}): TaskDto {
  return {
    taskId: 7,
    activityId: 3,
    activityName: 'Trabajo',
    title: 'Tarea',
    description: null,
    status: 'PENDIENTE',
    priority: 'ALTA',
    allocatedBudgetBob: 0,
    estimatedDurationMinutes: 0,
    actualDurationMinutes: null,
    trackedSeconds: 0,
    currentSegmentStartedAt: null,
    completedAt: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 1001,
    subtasks: [],
    ...overrides,
  };
}

function subtaskDto(overrides: Partial<SubtaskDto> = {}): SubtaskDto {
  return {
    subtaskId: 7,
    taskId: 7,
    title: 'Subtarea',
    description: null,
    status: 'PENDIENTE',
    completedAt: null,
    createdAt: '2026-10-01T11:00:00Z',
    updatedAt: '2026-10-01T11:00:00Z',
    version: 2002,
    ...overrides,
  };
}

describe('TaskService (fachada sobre la API V4)', () => {
  let service: TaskService;
  let http: HttpTestingController;

  const task: Task = { id: 7, kind: 'TASK', entityId: 7, title: 'Tarea', status: 'ACTIVA', priority: 'MEDIA', version: 3 };
  const subtask: Task = {
    id: -9,
    kind: 'SUBTASK',
    entityId: 9,
    parentTaskId: 7,
    title: 'Subtarea',
    status: 'ACTIVA',
    priority: 'MEDIA',
    version: 4,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TaskService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('flattens tasks and their subtasks, keeping subtask ids apart from task ids', () => {
    let result: Task[] = [];
    service.listTasks(42, '  informe ').subscribe((tasks) => (result = tasks));

    const req = http.expectOne((r) => r.url === '/api/tasks' && r.params.get('projectId') === '42' && r.params.get('q') === 'informe');
    req.flush([
      taskDto({
        status: 'EN_PROGRESO',
        trackedSeconds: 60,
        currentSegmentStartedAt: new Date(Date.now() - 5000).toISOString(),
        subtasks: [subtaskDto({ status: 'COMPLETADA', completedAt: '2026-10-01T12:00:00Z' })],
      }),
    ]);

    expect(result.map((t) => t.id)).toEqual([7, -7]);
    expect(result[0]).toMatchObject({
      kind: 'TASK',
      status: 'ACTIVA',
      priority: 'ALTA',
      categoryId: 3,
      categoryName: 'Trabajo',
      totalActiveSeconds: 60,
      version: 1001,
    });
    expect(result[0].effectiveActiveSeconds).toBeGreaterThanOrEqual(65);
    expect(result[1]).toMatchObject({
      kind: 'SUBTASK',
      entityId: 7,
      parentTaskId: 7,
      status: 'TERMINADA',
      version: 2002,
    });
    expect(result[1].totalActiveSeconds).toBeUndefined();
  });

  it('creates a task in its activity with the budget and duration that the Encargado fija', () => {
    service.createTask(3, 'Nueva', 'ALTA', 250.5, 120).subscribe();
    const categorized = http.expectOne('/api/tasks');
    expect(categorized.request.method).toBe('POST');
    expect(categorized.request.body).toEqual({
      activityId: 3,
      title: 'Nueva',
      priority: 'ALTA',
      allocatedBudgetBob: 250.5,
      estimatedDurationMinutes: 120,
    });
    categorized.flush(taskDto());

    service.createTask(3, 'Sin nada', 'MEDIA', 10, 30).subscribe();
    const plain = http.expectOne('/api/tasks');
    expect(plain.request.body).toEqual({
      activityId: 3,
      title: 'Sin nada',
      priority: 'MEDIA',
      allocatedBudgetBob: 10,
      estimatedDurationMinutes: 30,
    });
    plain.flush(taskDto({ activityName: null }));
  });

  it('creates subtasks under their task', () => {
    let created: Task | undefined;
    service.createTask(3, 'Hija', 'MEDIA', 0, 0, 7).subscribe((t) => (created = t));

    const req = http.expectOne('/api/tasks/7/subtasks');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'Hija', description: null });
    req.flush(subtaskDto({ subtaskId: 12, title: 'Hija' }));

    expect(created).toMatchObject({ id: -12, kind: 'SUBTASK', parentTaskId: 7 });
  });

  it('routes edits to the task or subtask endpoint', () => {
    const edit = { title: 'Editada', description: null, priority: 'ALTA' as const, version: 2 };

    service.updateTask(7, edit).subscribe();
    const taskReq = http.expectOne('/api/tasks/7');
    expect(taskReq.request.method).toBe('PUT');
    expect(taskReq.request.body).toEqual(edit);
    taskReq.flush(taskDto({ title: 'Editada' }));

    service.updateTask(-9, edit).subscribe();
    const subtaskReq = http.expectOne('/api/subtasks/9');
    expect(subtaskReq.request.method).toBe('PUT');
    expect(subtaskReq.request.body).toEqual({ title: 'Editada', description: null, version: 2 });
    subtaskReq.flush(subtaskDto({ subtaskId: 9, title: 'Editada' }));
  });

  it('maps lifecycle actions to start/pause/complete/reopen with the displayed version', () => {
    service.activate(task).subscribe();
    const start = http.expectOne('/api/tasks/7/start');
    expect(start.request.method).toBe('POST');
    expect(start.request.body).toEqual({ version: 3 });
    start.flush(taskDto({ status: 'EN_PROGRESO' }));

    service.pause(subtask).subscribe();
    const pause = http.expectOne('/api/subtasks/9/pause');
    expect(pause.request.body).toEqual({ version: 4 });
    pause.flush(subtaskDto({ subtaskId: 9 }));

    service.complete(task).subscribe();
    http.expectOne('/api/tasks/7/complete').flush(taskDto({ status: 'COMPLETADA' }));

    service.reopen(subtask).subscribe();
    http.expectOne('/api/subtasks/9/reopen').flush(subtaskDto({ subtaskId: 9, status: 'EN_PROGRESO' }));
  });

  it('deletes tasks and subtasks sending the version and accepting 204', () => {
    service.deleteTask(task).subscribe();
    const taskReq = http.expectOne('/api/tasks/7?version=3');
    expect(taskReq.request.method).toBe('DELETE');
    taskReq.flush(null, { status: 204, statusText: 'No Content' });

    service.deleteTask(subtask).subscribe();
    const subtaskReq = http.expectOne('/api/subtasks/9?version=4');
    expect(subtaskReq.request.method).toBe('DELETE');
    subtaskReq.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('changes the category through the task activity', () => {
    let updated: Task | undefined;
    service.changeActivity(task, 0).subscribe((t) => (updated = t));

    const req = http.expectOne('/api/tasks/7/activity');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ activityId: 0, version: 3 });
    req.flush(taskDto({ activityName: null, activityId: 1 }));

    expect(updated?.categoryName).toBeNull();
    expect(updated?.categoryId).toBeNull();
  });

  it('loads the categories of the selected stage from /api/activities', () => {
    service.listCategories(5).subscribe((result) =>
      expect(result).toEqual([
        { id: 1, name: 'Casa' },
        { id: 2, name: 'Trabajo' },
      ]),
    );
    http
      .expectOne((r) => r.url === '/api/activities' && r.params.get('stageId') === '5')
      .flush([
        {
          activityId: 1,
          stageId: 5,
          name: 'Casa',
          description: null,
          status: 'PENDIENTE',
          createdAt: '2026-10-01T10:00:00Z',
          updatedAt: '2026-10-01T10:00:00Z',
          version: 1,
        },
        {
          activityId: 2,
          stageId: 5,
          name: 'Trabajo',
          description: null,
          status: 'PENDIENTE',
          createdAt: '2026-10-01T10:00:00Z',
          updatedAt: '2026-10-01T10:00:00Z',
          version: 1,
        },
      ]);
  });

  it('does not expose financial fields of assigned tasks to the Personal', () => {
    let assigned: Task[] = [];
    service.listAssignedTasks().subscribe((tasks) => (assigned = tasks));

    http.expectOne('/api/tasks/assigned').flush([
      {
        taskId: 9,
        projectId: 42,
        activityId: 3,
        activityName: 'Analisis',
        title: 'Entrevistar usuarios',
        description: null,
        status: 'EN_PROGRESO',
        priority: 'ALTA',
        actualDurationMinutes: 30,
        trackedSeconds: 90,
        currentSegmentStartedAt: null,
        completedAt: null,
        createdAt: '2026-10-01T10:00:00Z',
        updatedAt: '2026-10-01T10:00:00Z',
        version: 7,
        subtasks: [],
      },
    ]);

    expect(assigned).toHaveLength(1);
    expect(assigned[0]).toMatchObject({ id: 9, title: 'Entrevistar usuarios', status: 'ACTIVA' });
    expect(Object.keys(assigned[0] as unknown as Record<string, unknown>)).not.toContain('allocatedBudgetBob');
  });

  it('merges task and subtask history, showing cancellations as deletions', () => {
    let history: TaskHistory[] = [];
    service.listHistory(42, 'reunión').subscribe((items) => (history = items));

    const cancelled: TaskHistoryDto = {
      task: taskDto({ taskId: 1, title: 'Cancelada', status: 'CANCELADA', trackedSeconds: 90 }),
      cancelledAt: '2026-10-03T10:00:00Z',
      events: [
        { eventId: 3, previousStatus: 'COMPLETADA', newStatus: 'CANCELADA', reason: 'Tarea eliminada', changedAt: '2026-10-03T10:00:00Z' },
        { eventId: 2, previousStatus: 'EN_PROGRESO', newStatus: 'COMPLETADA', reason: 'Tarea completada', changedAt: '2026-10-02T10:00:00Z' },
        { eventId: 1, previousStatus: null, newStatus: 'PENDIENTE', reason: 'Tarea creada', changedAt: '2026-10-01T10:00:00Z' },
      ],
    };
    http
      .expectOne((r) => r.url === '/api/tasks/history' && r.params.get('q') === 'reunión')
      .flush([cancelled]);
    http
      .expectOne((r) => r.url === '/api/subtasks/history' && r.params.get('q') === 'reunión')
      .flush([subtaskDto({ subtaskId: 5, status: 'COMPLETADA', completedAt: '2026-10-04T10:00:00Z' })]);

    expect(history.map((item) => item.id)).toEqual([-5, 1]);
    expect(history[1]).toMatchObject({
      status: 'TERMINADA',
      deletedAt: '2026-10-03T10:00:00Z',
      effectiveActiveSeconds: 90,
    });
    expect(history[1].events.map((event) => event.toStatus)).toEqual(['TERMINADA', 'INACTIVA']);
    expect(history[0]).toMatchObject({ status: 'TERMINADA', deletedAt: null, events: [] });
  });
});
