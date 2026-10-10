import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivityDetailDto, AssignmentDto, ProgressResponseLike, StageDto } from '../../core/models/api';
import { WbsService } from './wbs.service';

const stage: StageDto = {
  stageId: 4,
  projectId: 42,
  name: 'Diseno',
  description: 'Fase de diseno',
  status: 'PENDIENTE',
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
  version: 11,
};

const activity: ActivityDetailDto = {
  activityId: 6,
  stageId: 4,
  name: 'Analisis',
  description: null,
  status: 'PENDIENTE',
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
  version: 21,
};

describe('WbsService (jerarquía, asignaciones y progreso)', () => {
  let service: WbsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(WbsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('asks for the stages of an explicit project and never for the first one', () => {
    service.listStages(42).subscribe((stages) => expect(stages).toEqual([stage]));

    const request = http.expectOne((r) => r.url === '/api/projects/42/stages');
    expect(request.request.method).toBe('GET');
    // La petición no debe llevar ningún "primer proyecto" implícito.
    expect(request.request.urlWithParams).not.toContain('first');
    request.flush([stage]);
  });

  it('creates stages inside the given project', () => {
    service.createStage(42, 'Construccion', null).subscribe((created) => expect(created).toEqual(stage));

    const request = http.expectOne('/api/projects/42/stages');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Construccion', description: null });
    request.flush(stage);
  });

  it('lists and creates activities inside a stage', () => {
    service.listActivities(4).subscribe((activities) => expect(activities).toEqual([activity]));
    http.expectOne('/api/stages/4/activities').flush([activity]);

    service.createActivity(4, 'Analisis', 'Levantamiento').subscribe((created) => expect(created).toEqual(activity));
    const request = http.expectOne('/api/stages/4/activities');
    expect(request.request.body).toEqual({ name: 'Analisis', description: 'Levantamiento' });
    request.flush(activity);
  });

  it('assigns and revokes Personal, sending the task explicitly', () => {
    const assignment: AssignmentDto = {
      taskId: 8,
      taskTitle: 'Levantar requisitos',
      workerUserId: 5,
      workerName: 'Ana Perez',
      assignedAt: '2026-10-02T10:00:00Z',
      assignedByUserId: 3,
      assignedByName: 'Julia Mendoza',
      reason: 'Carga de trabajo',
    };

    service.assign(8, 5, 'Carga de trabajo').subscribe((result) => expect(result).toEqual(assignment));
    const created = http.expectOne('/api/tasks/8/assignments');
    expect(created.request.method).toBe('POST');
    expect(created.request.body).toEqual({ workerUserId: 5, reason: 'Carga de trabajo' });
    created.flush(assignment);

    service.unassign(8, 5).subscribe();
    const removed = http.expectOne('/api/tasks/8/assignments/5');
    expect(removed.request.method).toBe('DELETE');
    removed.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('asks for the assignable workers of the project', () => {
    service.listWorkers(42).subscribe((workers) =>
      expect(workers.map((worker) => worker.userId)).toEqual([5, 6]),
    );
    http
      .expectOne('/api/projects/42/workers')
      .flush([{ userId: 5, fullName: 'Ana Perez', active: true, chief: false }, { userId: 6, fullName: 'Luis Luna', active: true, chief: false }]);
  });

  it('computes the cascade progress from the leaf counters of the project tree', () => {
    const tree: ProgressResponseLike = {
      projectId: 42,
      projectName: 'Proyecto de pruebas',
      projectStatus: 'EN_EJECUCION',
      budgetBob: 1000,
      allocatedBudgetBob: 250,
      estimatedDurationMinutes: 600,
      root: {
        id: null,
        name: 'Proyecto de pruebas',
        completed: 1,
        total: 2,
        leaf: false,
        ratio: 0.5,
        children: [
          {
            id: 4,
            name: 'Diseno',
            completed: 0,
            total: 1,
            leaf: false,
            ratio: 0,
            children: [
              {
                id: 6,
                name: 'Analisis',
                completed: 1,
                total: 1,
                leaf: false,
                ratio: 1,
                children: [
                  {
                    id: 8,
                    name: 'Levantar requisitos',
                    completed: 1,
                    total: 1,
                    leaf: false,
                    ratio: 1,
                    children: [{ id: 9, name: 'Entrevistas', completed: 1, total: 1, leaf: true, ratio: 1, children: [] }],
                  },
                ],
              },
            ],
          },
        ],
      },
    };

    service.progress(42).subscribe((progress) => {
      expect(progress.projectId).toBe(42);
      expect(progress.root.total).toBe(2);
      expect(progress.root.ratio).toBe(0.5);
    });
    http.expectOne('/api/projects/42/progress').flush(tree);
  });
});
