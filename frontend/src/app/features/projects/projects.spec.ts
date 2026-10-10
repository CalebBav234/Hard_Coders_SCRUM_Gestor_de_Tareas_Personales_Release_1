import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SESSION_STORAGE_KEY } from '../../core/auth/auth.service';
import { Project, UserSummary } from '../../core/models/project';
import { DelegationDialog } from './delegation-dialog/delegation-dialog';
import { PlanForm } from './plan-form/plan-form';
import { ProjectForm } from './project-form/project-form';
import { ProjectList } from './project-list/project-list';

const ANA: UserSummary = { userId: 2, fullName: 'Ana Paz', email: 'a@x.com', chief: false, active: true };
const LUIS: UserSummary = { userId: 3, fullName: 'Luis Vaca', email: 'l@x.com', chief: false, active: true };
const JULIA: UserSummary = { userId: 1, fullName: 'Julia Mendoza', email: 'j@x.com', chief: true, active: true };

function project(overrides: Partial<Project> = {}): Project {
  return {
    projectId: 10,
    name: 'Edificio A',
    description: 'Obra gruesa',
    status: 'PLANIFICADO',
    closed: false,
    budgetBob: 15000.5,
    allocatedBudgetBob: 800,
    estimatedDurationMinutes: 600,
    manager: ANA,
    createdBy: JULIA,
    currentRevision: 1,
    createdAt: '2026-10-10T12:00:00Z',
    updatedAt: '2026-10-10T12:00:00Z',
    version: 111,
    canEditDetails: true,
    canEditPlan: true,
    canDelegate: true,
    allowedStatuses: ['CANCELADO', 'EN_EJECUCION'],
    ...overrides,
  };
}

function signIn(chief: boolean): void {
  sessionStorage.setItem(
    SESSION_STORAGE_KEY,
    JSON.stringify({
      token: 't',
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      user: { userId: chief ? 1 : 2, email: 'x', givenNames: 'X', familyNames: 'Y', fullName: chief ? 'Julia Mendoza' : 'Ana Paz', chief, active: true },
    }),
  );
}

function buttonLabels(element: HTMLElement): string[] {
  return Array.from(element.querySelectorAll('button')).map((b) => b.textContent?.trim() ?? '');
}

function setValue(element: HTMLElement, selector: string, value: string): void {
  const control = element.querySelector(selector) as HTMLInputElement | HTMLSelectElement;
  control.value = value;
  control.dispatchEvent(new Event(control.tagName === 'SELECT' ? 'change' : 'input'));
}

describe('Épica 1 — Proyectos', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  describe('ProjectList', () => {
    async function render(projects: Project[]): Promise<ComponentFixture<ProjectList>> {
      const fixture = TestBed.createComponent(ProjectList);
      fixture.detectChanges();
      http.expectOne('/api/projects').flush(projects);
      await fixture.whenStable();
      fixture.detectChanges();
      return fixture;
    }

    it('shows the chief every action the backend allows', async () => {
      signIn(true);
      const fixture = await render([project()]);
      const element: HTMLElement = fixture.nativeElement;

      expect(element.querySelector('.project-card h3')?.textContent).toContain('Edificio A');
      expect(element.textContent).toContain('Ana Paz');
      expect(element.textContent).toContain('15.000,50');
      const labels = buttonLabels(element);
      expect(labels).toContain('＋ Nuevo proyecto');
      expect(labels).toContain('Bs Editar plan');
      expect(labels).toContain('⇄ Delegar');
    });

    it('hides budget, delegation and status controls from a manager', async () => {
      signIn(false);
      const fixture = await render([project({ canEditPlan: false, canDelegate: false, allowedStatuses: [] })]);
      const element: HTMLElement = fixture.nativeElement;

      const labels = buttonLabels(element);
      expect(labels).not.toContain('＋ Nuevo proyecto');
      expect(labels).not.toContain('Bs Editar plan');
      expect(labels).not.toContain('⇄ Delegar');
      expect(labels).toContain('✎ Editar datos');
      expect(element.querySelector('.project-status-change')).toBeNull();
    });

    it('changes the status with the displayed version', async () => {
      signIn(true);
      const fixture = await render([project()]);
      const element: HTMLElement = fixture.nativeElement;

      setValue(element, '#status-10', 'EN_EJECUCION');
      fixture.detectChanges();
      (Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Aplicar') as HTMLButtonElement).click();

      const request = http.expectOne('/api/projects/10/status');
      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toEqual({ status: 'EN_EJECUCION', version: 111 });
      request.flush(project({ status: 'EN_EJECUCION', version: 112, allowedStatuses: ['CANCELADO', 'EN_PAUSA', 'FINALIZADO'] }));
      await fixture.whenStable();
      fixture.detectChanges();

      expect(element.querySelector('.project-status')?.textContent).toContain('En ejecución');
      expect(fixture.componentInstance.success).toContain('En ejecución');
    });

    it('loads the plan history on demand', async () => {
      signIn(true);
      const fixture = await render([project({ currentRevision: 2 })]);
      const element: HTMLElement = fixture.nativeElement;

      fixture.componentInstance.toggleHistory(fixture.componentInstance.projects[0]);
      http.expectOne('/api/projects/10/plan-history').flush([
        { revisionNumber: 2, budgetBob: 20000, estimatedDurationMinutes: 900, changedAt: '2026-10-11T12:00:00Z', changedBy: JULIA, reason: 'Ampliación' },
        { revisionNumber: 1, budgetBob: 15000.5, estimatedDurationMinutes: 600, changedAt: '2026-10-10T12:00:00Z', changedBy: JULIA, reason: 'Plan inicial' },
      ]);
      fixture.detectChanges();

      const items = element.querySelectorAll('.plan-history li');
      expect(items.length).toBe(2);
      expect(items[0].textContent).toContain('Ampliación');
    });
  });

  describe('ProjectForm', () => {
    it('creates a project with its initial plan', () => {
      signIn(true);
      const fixture = TestBed.createComponent(ProjectForm);
      const element: HTMLElement = fixture.nativeElement;
      const saved = vi.fn();
      fixture.componentInstance.saved.subscribe(saved);
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([JULIA, ANA]);
      fixture.detectChanges();

      setValue(element, '#project-name', '  Edificio A ');
      setValue(element, '#project-manager', '2');
      setValue(element, '#project-budget', '15000.50');
      setValue(element, '#project-duration', '600');
      fixture.componentInstance.save();

      const request = http.expectOne('/api/projects');
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual({
        name: 'Edificio A',
        description: null,
        managerUserId: 2,
        budgetBob: 15000.5,
        estimatedDurationMinutes: 600,
        reason: null,
      });
      request.flush(project());
      expect(saved).toHaveBeenCalled();
    });

    it('does not send a project without manager or with a negative budget', () => {
      signIn(true);
      const fixture = TestBed.createComponent(ProjectForm);
      const element: HTMLElement = fixture.nativeElement;
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([ANA]);

      setValue(element, '#project-name', 'Edificio A');
      setValue(element, '#project-budget', '-1');
      fixture.componentInstance.save();
      http.expectNone('/api/projects');
    });

    it('edits only name and description of an existing project', () => {
      const fixture = TestBed.createComponent(ProjectForm);
      fixture.componentRef.setInput('project', project());
      fixture.detectChanges();
      const element: HTMLElement = fixture.nativeElement;
      expect(element.querySelector('#project-budget')).toBeNull();

      setValue(element, '#project-name', 'Edificio A - Fase 1');
      fixture.componentInstance.save();
      const request = http.expectOne('/api/projects/10');
      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toEqual({ name: 'Edificio A - Fase 1', description: 'Obra gruesa', version: 111 });
      request.flush(project({ name: 'Edificio A - Fase 1' }));
    });
  });

  describe('PlanForm', () => {
    function render(): ComponentFixture<PlanForm> {
      const fixture = TestBed.createComponent(PlanForm);
      fixture.componentRef.setInput('project', project());
      fixture.detectChanges();
      return fixture;
    }

    it('requires a reason and blocks budgets below the allocations', () => {
      const fixture = render();
      const element: HTMLElement = fixture.nativeElement;

      fixture.componentInstance.save();
      http.expectNone('/api/projects/10/plan');

      setValue(element, '#plan-budget', '799.99');
      setValue(element, '#plan-reason', 'Recorte');
      fixture.componentInstance.save();
      fixture.detectChanges();
      http.expectNone('/api/projects/10/plan');
      expect(element.textContent).toContain('No puede ser menor que lo ya asignado');
    });

    it('sends the new revision and shows backend errors', () => {
      const fixture = render();
      const element: HTMLElement = fixture.nativeElement;
      setValue(element, '#plan-budget', '20000');
      setValue(element, '#plan-duration', '900');
      setValue(element, '#plan-reason', '  Ampliación del alcance ');
      fixture.componentInstance.save();

      const request = http.expectOne('/api/projects/10/plan');
      expect(request.request.body).toEqual({
        budgetBob: 20000,
        estimatedDurationMinutes: 900,
        reason: 'Ampliación del alcance',
        version: 111,
      });
      request.flush({ error: 'FORBIDDEN', message: 'Solo el Jefe puede cambiar el presupuesto' }, { status: 403, statusText: 'Forbidden' });
      fixture.detectChanges();
      expect(element.querySelector('[role="alert"]')?.textContent).toContain('Solo el Jefe');
    });
  });

  describe('DelegationDialog', () => {
    it('offers other active users and delegates with a reason', () => {
      const fixture = TestBed.createComponent(DelegationDialog);
      fixture.componentRef.setInput('project', project());
      const saved = vi.fn();
      fixture.componentInstance.saved.subscribe(saved);
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([JULIA, ANA, LUIS]);
      fixture.detectChanges();
      const element: HTMLElement = fixture.nativeElement;

      const options = Array.from(element.querySelectorAll('#delegate-manager option')).map((o) => o.textContent?.trim());
      expect(options).toContain('Luis Vaca');
      expect(options).not.toContain('Ana Paz');

      setValue(element, '#delegate-manager', '3');
      setValue(element, '#delegate-reason', 'Carga de trabajo');
      fixture.componentInstance.save();
      const request = http.expectOne('/api/projects/10/delegate');
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual({ managerUserId: 3, reason: 'Carga de trabajo', version: 111 });
      request.flush(project({ manager: LUIS, currentRevision: 2 }));
      expect(saved).toHaveBeenCalled();
    });

    it('explains when the target user is inactive', () => {
      const fixture = TestBed.createComponent(DelegationDialog);
      fixture.componentRef.setInput('project', project());
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([LUIS]);
      fixture.detectChanges();
      const element: HTMLElement = fixture.nativeElement;

      setValue(element, '#delegate-manager', '3');
      setValue(element, '#delegate-reason', 'Cambio');
      fixture.componentInstance.save();
      http
        .expectOne('/api/projects/10/delegate')
        .flush({ error: 'VALIDATION_ERROR', message: 'No se puede delegar a un usuario inactivo' }, { status: 400, statusText: 'Bad Request' });
      fixture.detectChanges();
      expect(element.querySelector('[role="alert"]')?.textContent).toContain('usuario inactivo');
    });
  });
});
