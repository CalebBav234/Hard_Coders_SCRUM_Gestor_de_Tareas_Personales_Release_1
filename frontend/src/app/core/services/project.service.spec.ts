import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Project } from '../models/project';
import { ProjectService, apiErrorMessage } from './project.service';

describe('ProjectService', () => {
  let service: ProjectService;
  let http: HttpTestingController;

  const project = {
    projectId: 10, name: 'P', description: null, status: 'EN_EJECUCION', closed: false,
    budgetBob: 100, allocatedBudgetBob: 0, estimatedDurationMinutes: 60,
    manager: { userId: 2, fullName: 'Ana', email: 'a@x.com', chief: false, active: true },
    createdBy: { userId: 1, fullName: 'J', email: 'j@x.com', chief: true, active: true },
    currentRevision: 1, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', version: 5,
    canEditDetails: true, canEditPlan: true, canDelegate: true, canManageHierarchy: true, allowedStatuses: [],
  } as unknown as Project;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProjectService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches, lists and gets projects', () => {
    service.search(' abc ').subscribe();
    http.expectOne((r) => r.url === '/api/projects' && r.params.get('q') === ' abc ').flush([]);

    service.search('   ').subscribe();
    const noQ = http.expectOne((r) => r.url === '/api/projects');
    expect(noQ.request.params.has('q')).toBe(false);
    noQ.flush([]);

    service.list().subscribe();
    http.expectOne('/api/projects').flush([]);

    service.get(10).subscribe();
    http.expectOne('/api/projects/10').flush(project);

    service.planHistory(10).subscribe();
    http.expectOne('/api/projects/10/plan-history').flush([]);

    service.eligibleManagers().subscribe();
    http.expectOne('/api/projects/eligible-managers').flush([]);
  });

  it('creates, updates, replans, changes status and delegates', () => {
    service.create({ name: 'N', description: null, managerUserId: 2, budgetBob: 1, estimatedDurationMinutes: 1, reason: 'r' } as any).subscribe();
    http.expectOne('/api/projects').flush(project);

    service.updateDetails(project, 'Nuevo', 'desc').subscribe();
    const upd = http.expectOne('/api/projects/10');
    expect(upd.request.method).toBe('PUT');
    expect(upd.request.body).toEqual({ name: 'Nuevo', description: 'desc', version: 5 });
    upd.flush(project);

    service.replan(project, 200, 90, 'motivo').subscribe();
    const plan = http.expectOne('/api/projects/10/plan');
    expect(plan.request.body).toEqual({ budgetBob: 200, estimatedDurationMinutes: 90, reason: 'motivo', version: 5 });
    plan.flush(project);

    service.changeStatus(project, 'FINALIZADO').subscribe();
    http.expectOne('/api/projects/10/status').flush(project);

    service.delegate(project, 3, 'delegar').subscribe();
    const del = http.expectOne('/api/projects/10/delegate');
    expect(del.request.method).toBe('POST');
    expect(del.request.body).toEqual({ managerUserId: 3, reason: 'delegar', version: 5 });
    del.flush(project);
  });

  it('formats api errors', () => {
    expect(apiErrorMessage({ status: 412 }, 'x')).toContain('Cierra');
    expect(apiErrorMessage({ status: 403, error: { message: 'no' } }, 'x')).toBe('no');
    expect(apiErrorMessage({ status: 403 }, 'x')).toContain('permiso');
    expect(apiErrorMessage({ status: 400, error: { message: 'bad' } }, 'x')).toBe('bad');
    expect(apiErrorMessage({}, 'fallback')).toBe('fallback');
  });
});
