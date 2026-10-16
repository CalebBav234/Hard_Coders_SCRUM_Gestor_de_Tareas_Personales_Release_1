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
    canManageHierarchy: true,
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

/** Pulsa un botón por el texto que muestra y refresca la vista. */
function clickButton(
  fixture: ComponentFixture<unknown>,
  label: string,
  scope: ParentNode = fixture.nativeElement,
): void {
  const button = Array.from(scope.querySelectorAll('button')).find((b) => b.textContent?.includes(label));
  expect(button, `botón "${label}"`).toBeTruthy();
  button!.click();
  fixture.detectChanges();
}

/** Envía el formulario del diálogo como lo hace el navegador con el botón de tipo submit. */
function submitForm(fixture: ComponentFixture<unknown>, scope: ParentNode = fixture.nativeElement): void {
  const form = scope.querySelector('form') as HTMLFormElement;
  expect(form, 'formulario del diálogo').toBeTruthy();
  form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  fixture.detectChanges();
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

    it('shows the loading message and the empty state of each role', async () => {
      signIn(true);
      const fixture = TestBed.createComponent(ProjectList);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Cargando proyectos…');

      http.expectOne('/api/projects').flush([]);
      await fixture.whenStable();
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Aún no hay proyectos. Crea el primero.');
    });

    it('tells a manager that he does not lead any project yet', async () => {
      signIn(false);
      const fixture = await render([]);
      expect(fixture.nativeElement.textContent).toContain('No diriges ningún proyecto todavía.');
    });

    it('reports that the projects could not be loaded', async () => {
      signIn(true);
      const fixture = TestBed.createComponent(ProjectList);
      fixture.detectChanges();
      http
        .expectOne('/api/projects')
        .flush({ error: 'VALIDATION_ERROR', message: 'Sin sesión' }, { status: 401, statusText: 'Unauthorized' });
      fixture.detectChanges();

      expect(fixture.componentInstance.error).toBe('Sin sesión');
      expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('Sin sesión');
      // Sin proyectos no se muestra el aviso de lista vacía: lo que hay es el error.
      expect(fixture.nativeElement.textContent).not.toContain('Aún no hay proyectos');
    });

    it('reports that the plan history could not be loaded and hides it again', async () => {
      signIn(true);
      const fixture = await render([project()]);
      const element: HTMLElement = fixture.nativeElement;

      const historyButton = Array.from(element.querySelectorAll('button')).find(
        (b) => b.textContent?.includes('Historial del plan'),
      ) as HTMLButtonElement;
      historyButton.click();
      http.expectOne('/api/projects/10/plan-history').flush(null, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      expect(element.querySelector('.gt-alert-error')?.textContent).toContain('No se pudo cargar el historial');
      expect(fixture.componentInstance.revisions(10)).toEqual([]);

      // El historial fallido se puede volver a pedir: ahora llega vacío.
      historyButton.click();
      http.expectOne('/api/projects/10/plan-history').flush([]);
      fixture.detectChanges();
      expect(fixture.componentInstance.revisions(10)).toEqual([]);

      // Y el segundo toque repliega el panel del historial.
      historyButton.click();
      fixture.detectChanges();
      expect(fixture.componentInstance.history[10]).toBeUndefined();
      expect(element.textContent).not.toContain('No se pudo cargar el historial');
    });

    it('reports a failed status change and ignores the empty ones', async () => {
      signIn(true);
      const fixture = await render([project()]);
      const element: HTMLElement = fixture.nativeElement;

      // Sin opción elegida no se envía nada.
      const apply = Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Aplicar') as HTMLButtonElement;
      apply.click();
      http.expectNone('/api/projects/10/status');

      setValue(element, '#status-10', 'CANCELADO');
      fixture.detectChanges();
      apply.click();
      http
        .expectOne('/api/projects/10/status')
        .flush({ error: 'INVALID_TRANSITION', message: 'No se puede cancelar' }, { status: 409, statusText: 'Conflict' });
      fixture.detectChanges();

      expect(fixture.componentInstance.error).toBe('No se puede cancelar');
      expect(fixture.componentInstance.busyProjectId).toBeNull();
    });

    it('does not change the status of another project while one is in flight', async () => {
      signIn(true);
      const fixture = await render([project(), project({ projectId: 11, name: 'Edificio B' })]);
      const element: HTMLElement = fixture.nativeElement;

      setValue(element, '#status-10', 'CANCELADO');
      fixture.detectChanges();
      const apply = Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Aplicar') as HTMLButtonElement;
      apply.click();

      // El proyecto 10 está ocupado: el 11 se queda esperando.
      const request = http.expectOne('/api/projects/10/status');
      fixture.componentInstance.changeStatus(fixture.componentInstance.projects[1]);
      http.expectNone('/api/projects/11/status');

      request.flush(project({ status: 'CANCELADO', version: 112 }));
      fixture.detectChanges();
      expect(fixture.componentInstance.busyProjectId).toBeNull();
    });

    it('opens the dialog of every kind and closes it with its buttons', async () => {
      signIn(true);
      const fixture = await render([project()]);
      const element: HTMLElement = fixture.nativeElement;

      // Crear proyecto: el formulario pide los encargados disponibles.
      clickButton(fixture as ComponentFixture<unknown>, '＋ Nuevo proyecto');
      expect(element.textContent).toContain('Nuevo proyecto');
      http.expectOne('/api/projects/eligible-managers').flush([JULIA, ANA]);
      fixture.detectChanges();
      expect(fixture.componentInstance.dialogProject).toBeNull();
      clickButton(fixture as ComponentFixture<unknown>, '×', element.querySelector('.gt-modal')!);
      expect(fixture.componentInstance.dialog).toBeNull();

      // Editar datos: el formulario llega con el nombre del proyecto.
      clickButton(fixture as ComponentFixture<unknown>, '✎ Editar datos');
      expect(element.textContent).toContain('Editar datos');
      expect(element.querySelector<HTMLInputElement>('#project-name')?.value).toBe('Edificio A');
      clickButton(fixture as ComponentFixture<unknown>, '×', element.querySelector('.gt-modal')!);
      expect(fixture.componentInstance.dialog).toBeNull();

      // Nueva revisión del plan.
      clickButton(fixture as ComponentFixture<unknown>, 'Bs Editar plan');
      expect(element.textContent).toContain('Nueva revisión del plan');
      expect(element.querySelector('#plan-budget')).not.toBeNull();
      // También se cierra pulsando el fondo del modal.
      (element.querySelector('.gt-modal-backdrop') as HTMLElement).click();
      fixture.detectChanges();
      expect(fixture.componentInstance.dialog).toBeNull();

      // Delegación: el diálogo carga los usuarios disponibles.
      clickButton(fixture as ComponentFixture<unknown>, '⇄ Delegar');
      expect(element.textContent).toContain('Delegar proyecto');
      http.expectOne('/api/projects/eligible-managers').flush([LUIS]);
      fixture.detectChanges();
      expect(fixture.componentInstance.dialogProject?.projectId).toBe(10);
      clickButton(fixture as ComponentFixture<unknown>, 'Cancelar', element.querySelector('.gt-modal')!);
      expect(fixture.componentInstance.dialog).toBeNull();
    });

    it('delegates from the list and replaces the manager of the project', async () => {
      signIn(true);
      const fixture = await render([project()]);
      const element: HTMLElement = fixture.nativeElement;

      clickButton(fixture as ComponentFixture<unknown>, '⇄ Delegar');
      http.expectOne('/api/projects/eligible-managers').flush([LUIS, JULIA]);
      fixture.detectChanges();

      setValue(element, '#delegate-manager', '3');
      setValue(element, '#delegate-reason', 'Carga de trabajo');
      fixture.detectChanges();
      clickButton(fixture as ComponentFixture<unknown>, 'Delegar proyecto', element.querySelector('.gt-modal')!);

      const request = http.expectOne('/api/projects/10/delegate');
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual({ managerUserId: 3, reason: 'Carga de trabajo', version: 111 });
      request.flush(project({ manager: LUIS, version: 112 }));
      fixture.detectChanges();

      expect(fixture.componentInstance.dialog).toBeNull();
      expect(fixture.componentInstance.success).toBe('Proyecto delegado.');
      expect(fixture.componentInstance.projects[0].manager.fullName).toBe('Luis Vaca');
      expect(element.querySelector('.gt-alert-success')?.textContent).toContain('Proyecto delegado.');
    });

    it('adds a project created in the dialog to the top of the list', async () => {
      signIn(true);
      const fixture = await render([project()]);
      const element: HTMLElement = fixture.nativeElement;

      clickButton(fixture as ComponentFixture<unknown>, '＋ Nuevo proyecto');
      http.expectOne('/api/projects/eligible-managers').flush([ANA]);
      fixture.detectChanges();

      setValue(element, '#project-name', '  Edificio C ');
      setValue(element, '#project-manager', '2');
      setValue(element, '#project-budget', '15000.50');
      setValue(element, '#project-duration', '600');
      setValue(element, '#project-reason', 'Plan inicial');
      fixture.detectChanges();
      submitForm(fixture as ComponentFixture<unknown>);

      const request = http.expectOne('/api/projects');
      expect(request.request.body).toEqual({
        name: 'Edificio C',
        description: null,
        managerUserId: 2,
        budgetBob: 15000.5,
        estimatedDurationMinutes: 600,
        reason: 'Plan inicial',
      });
      request.flush(project({ projectId: 20, name: 'Edificio C' }));
      fixture.detectChanges();

      expect(fixture.componentInstance.dialog).toBeNull();
      expect(fixture.componentInstance.success).toBe('Proyecto creado con su plan inicial.');
      expect(fixture.componentInstance.projects.map((current) => current.name)).toEqual(['Edificio C', 'Edificio A']);
      expect(element.querySelectorAll('.project-card h3')[0]?.textContent).toContain('Edificio C');
    });

    it('replaces the project edited in the details dialog', async () => {
      signIn(true);
      const fixture = await render([project()]);
      const element: HTMLElement = fixture.nativeElement;

      clickButton(fixture as ComponentFixture<unknown>, '✎ Editar datos');
      setValue(element, '#project-name', 'Edificio A - Fase 2');
      fixture.detectChanges();
      submitForm(fixture as ComponentFixture<unknown>);

      const request = http.expectOne('/api/projects/10');
      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toEqual({ name: 'Edificio A - Fase 2', description: 'Obra gruesa', version: 111 });
      request.flush(project({ name: 'Edificio A - Fase 2', version: 112 }));
      fixture.detectChanges();

      expect(fixture.componentInstance.dialog).toBeNull();
      expect(fixture.componentInstance.success).toBe('Datos del proyecto actualizados.');
      expect(fixture.componentInstance.projects[0].name).toBe('Edificio A - Fase 2');
      expect(element.querySelector('.project-card h3')?.textContent).toContain('Edificio A - Fase 2');
    });

    it('closes the dialog when the form it holds is cancelled', async () => {
      signIn(true);
      const fixture = await render([project()]);

      clickButton(fixture as ComponentFixture<unknown>, '✎ Editar datos');
      expect(fixture.componentInstance.dialog?.kind).toBe('details');
      clickButton(fixture as ComponentFixture<unknown>, 'Cancelar', fixture.nativeElement.querySelector('.gt-modal')!);

      expect(fixture.componentInstance.dialog).toBeNull();
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

    it('explains that the available managers could not be loaded', () => {
      const fixture = TestBed.createComponent(ProjectForm);
      fixture.detectChanges();
      http
        .expectOne('/api/projects/eligible-managers')
        .flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      expect(fixture.componentInstance.error).toBe('No se pudieron cargar los encargados.');
      expect(fixture.componentInstance.managers).toEqual([]);
    });

    it('asks for a non-blank name and for a manager', () => {
      const fixture = TestBed.createComponent(ProjectForm);
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([JULIA, ANA]);
      fixture.detectChanges();
      const element: HTMLElement = fixture.nativeElement;
      const form = element.querySelector('form') as HTMLFormElement;

      setValue(element, '#project-name', '   ');
      setValue(element, '#project-budget', '1000');
      setValue(element, '#project-duration', '60');
      form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
      fixture.detectChanges();
      http.expectNone('/api/projects');
      expect(element.textContent).toContain('El nombre es obligatorio');

      // Con nombre pero sin encargado tampoco se envía nada.
      setValue(element, '#project-name', 'Edificio A');
      setValue(element, '#project-description', '  Obra gruesa y acabados  ');
      form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
      fixture.detectChanges();
      http.expectNone('/api/projects');
      expect(element.querySelector('.gt-field-error')?.textContent).toContain('Elige quién dirigirá');
    });

    it('trims the description and the optional reason of a new project', () => {
      const fixture = TestBed.createComponent(ProjectForm);
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([ANA]);
      fixture.detectChanges();

      setValue(fixture.nativeElement, '#project-name', 'Edificio A');
      setValue(fixture.nativeElement, '#project-description', '  Obra gruesa  ');
      setValue(fixture.nativeElement, '#project-manager', '2');
      setValue(fixture.nativeElement, '#project-budget', '15000.50');
      setValue(fixture.nativeElement, '#project-duration', '600');
      setValue(fixture.nativeElement, '#project-reason', '  Plan inicial  ');
      fixture.componentInstance.save();

      expect(http.expectOne('/api/projects').request.body).toEqual({
        name: 'Edificio A',
        description: 'Obra gruesa',
        managerUserId: 2,
        budgetBob: 15000.5,
        estimatedDurationMinutes: 600,
        reason: 'Plan inicial',
      });
    });

    it('reports a backend error when creating and when editing', () => {
      const fixture = TestBed.createComponent(ProjectForm);
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([JULIA]);
      fixture.detectChanges();
      const element: HTMLElement = fixture.nativeElement;

      setValue(element, '#project-name', 'Edificio A');
      setValue(element, '#project-manager', '1');
      setValue(element, '#project-budget', '1000');
      setValue(element, '#project-duration', '60');
      fixture.componentInstance.save();
      http
        .expectOne('/api/projects')
        .flush({ error: 'VALIDATION_ERROR', message: 'Ese nombre ya existe' }, { status: 409, statusText: 'Conflict' });
      fixture.detectChanges();
      expect(element.querySelector('[role="alert"]')?.textContent).toContain('Ese nombre ya existe');

      // Al editar la respuesta del backend se muestra igual.
      const edit = TestBed.createComponent(ProjectForm);
      edit.componentRef.setInput('project', project());
      edit.detectChanges();
      setValue(edit.nativeElement, '#project-name', 'Edificio A');
      edit.componentInstance.save();
      http.expectOne('/api/projects/10').flush({}, { status: 500, statusText: 'Server Error' });
      expect(edit.componentInstance.error).toBe('No se pudo guardar el proyecto.');
      expect(edit.componentInstance.saving).toBe(false);
    });

    it('does not save twice while the first request is in flight and can cancel', () => {
      const fixture = TestBed.createComponent(ProjectForm);
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([ANA]);
      fixture.detectChanges();
      const cancelled = vi.fn();
      fixture.componentInstance.cancelled.subscribe(cancelled);

      setValue(fixture.nativeElement, '#project-name', 'Edificio A');
      setValue(fixture.nativeElement, '#project-manager', '2');
      setValue(fixture.nativeElement, '#project-budget', '1000');
      setValue(fixture.nativeElement, '#project-duration', '60');
      fixture.componentInstance.save();
      fixture.componentInstance.save();
      http.expectOne('/api/projects');

      clickButton(fixture as ComponentFixture<unknown>, 'Cancelar');
      expect(cancelled).toHaveBeenCalled();
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

    it('registers the revision and tells the list about it', () => {
      const fixture = render();
      const saved = vi.fn();
      fixture.componentInstance.saved.subscribe(saved);

      setValue(fixture.nativeElement, '#plan-budget', '20000');
      setValue(fixture.nativeElement, '#plan-duration', '900');
      setValue(fixture.nativeElement, '#plan-reason', 'Ampliacion');
      fixture.detectChanges();
      // También se puede registrar desde el botón del formulario.
      submitForm(fixture as ComponentFixture<unknown>);

      http.expectOne('/api/projects/10/plan').flush(
        project({ budgetBob: 20000, estimatedDurationMinutes: 900, currentRevision: 2 }),
      );
      expect(saved).toHaveBeenCalled();
      expect(fixture.componentInstance.saving).toBe(false);
    });

    it('requires a reason and allows cancelling the revision', () => {
      const fixture = render();
      const element: HTMLElement = fixture.nativeElement;
      const cancelled = vi.fn();
      fixture.componentInstance.cancelled.subscribe(cancelled);

      setValue(element, '#plan-budget', '20000');
      setValue(element, '#plan-reason', '   ');
      fixture.componentInstance.save();
      http.expectNone('/api/projects/10/plan');
      fixture.detectChanges();
      expect(element.querySelector('.gt-field-error')?.textContent).toContain('El motivo es obligatorio');

      clickButton(fixture as ComponentFixture<unknown>, 'Cancelar');
      expect(cancelled).toHaveBeenCalled();
    });

    it('warns about a budget that is not a valid amount', () => {
      const fixture = render();
      const element: HTMLElement = fixture.nativeElement;

      // El backend rechaza los importes con más de dos decimales.
      fixture.componentInstance.form.controls.budgetBob.setValue('12345.678' as unknown as number);
      setValue(element, '#plan-reason', 'Recorte');
      fixture.componentInstance.save();
      fixture.detectChanges();
      http.expectNone('/api/projects/10/plan');
      expect(element.querySelector('.gt-field-error')?.textContent).toContain('hasta 2 decimales');
    });

    it('falls back to a generic message when the revision fails without one', () => {
      const fixture = render();
      setValue(fixture.nativeElement, '#plan-budget', '20000');
      setValue(fixture.nativeElement, '#plan-reason', 'Ampliacion');
      fixture.componentInstance.save();

      http.expectOne('/api/projects/10/plan').flush({}, { status: 500, statusText: 'Server Error' });
      expect(fixture.componentInstance.error).toBe('No se pudo registrar el nuevo plan.');
      expect(fixture.componentInstance.saving).toBe(false);
    });

    it('does not send a second revision while the first is in flight', () => {
      const fixture = render();
      setValue(fixture.nativeElement, '#plan-budget', '20000');
      setValue(fixture.nativeElement, '#plan-reason', 'Ampliacion');
      fixture.componentInstance.save();
      fixture.componentInstance.save();

      http.expectOne('/api/projects/10/plan');
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

    it('explains that the available users could not be loaded', () => {
      const fixture = TestBed.createComponent(DelegationDialog);
      fixture.componentRef.setInput('project', project());
      fixture.detectChanges();

      http.expectOne('/api/projects/eligible-managers').flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      expect(fixture.componentInstance.error).toBe('No se pudieron cargar los usuarios disponibles.');
      expect(fixture.componentInstance.loading).toBe(false);
      expect(fixture.componentInstance.candidates).toEqual([]);
      // Con la búsqueda terminada el selector ya no está "ocupado".
      expect(fixture.nativeElement.querySelector('#delegate-manager')?.getAttribute('aria-busy')).toBe('false');
    });

    it('shows the loading label and marks the chief candidates', () => {
      const fixture = TestBed.createComponent(DelegationDialog);
      fixture.componentRef.setInput('project', project());
      fixture.detectChanges();
      // Los usuarios todavía viajan: el selector lo dice y no se puede enviar.
      expect(fixture.nativeElement.textContent).toContain('Cargando usuarios…');
      expect(fixture.nativeElement.querySelector('button[type="submit"]')?.hasAttribute('disabled')).toBe(true);

      http.expectOne('/api/projects/eligible-managers').flush([JULIA, LUIS]);
      fixture.detectChanges();

      const options = Array.from(fixture.nativeElement.querySelectorAll('#delegate-manager option')).map(
        (option) => (option as HTMLOptionElement).textContent?.trim(),
      );
      expect(options).toContain('Julia Mendoza (Jefe)');
      expect(options).toContain('Luis Vaca');
      expect(fixture.nativeElement.querySelector('button[type="submit"]')?.hasAttribute('disabled')).toBe(false);
    });

    it('asks for a user and a reason before delegating', () => {
      const fixture = TestBed.createComponent(DelegationDialog);
      fixture.componentRef.setInput('project', project());
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([LUIS]);
      fixture.detectChanges();
      const element: HTMLElement = fixture.nativeElement;
      const form = element.querySelector('form') as HTMLFormElement;

      // Sin usuario no se envía nada.
      setValue(element, '#delegate-reason', 'Carga de trabajo');
      form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
      fixture.detectChanges();
      http.expectNone('/api/projects/10/delegate');
      expect(element.querySelector('.gt-field-error')?.textContent).toContain('Elige a quién delegar');

      // Un motivo en blanco tampoco vale.
      setValue(element, '#delegate-manager', '3');
      setValue(element, '#delegate-reason', '   ');
      form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
      fixture.detectChanges();
      http.expectNone('/api/projects/10/delegate');
      expect(element.querySelector('.gt-field-error')?.textContent).toContain('El motivo es obligatorio');
    });

    it('does not delegate twice while the first request is in flight', () => {
      const fixture = TestBed.createComponent(DelegationDialog);
      fixture.componentRef.setInput('project', project());
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([LUIS]);
      fixture.detectChanges();
      const element: HTMLElement = fixture.nativeElement;

      setValue(element, '#delegate-manager', '3');
      setValue(element, '#delegate-reason', 'Carga');
      fixture.componentInstance.save();
      fixture.componentInstance.save();
      http.expectOne('/api/projects/10/delegate');
    });

    it('cancels the delegation with its cancel button', () => {
      const fixture = TestBed.createComponent(DelegationDialog);
      fixture.componentRef.setInput('project', project());
      const cancelled = vi.fn();
      fixture.componentInstance.cancelled.subscribe(cancelled);
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([LUIS]);
      fixture.detectChanges();

      clickButton(fixture as ComponentFixture<unknown>, 'Cancelar');
      expect(cancelled).toHaveBeenCalled();
    });

    it('falls back to a generic message when the delegation fails without one', () => {
      const fixture = TestBed.createComponent(DelegationDialog);
      fixture.componentRef.setInput('project', project());
      fixture.detectChanges();
      http.expectOne('/api/projects/eligible-managers').flush([LUIS]);
      fixture.detectChanges();
      const element: HTMLElement = fixture.nativeElement;

      setValue(element, '#delegate-manager', '3');
      setValue(element, '#delegate-reason', 'Carga');
      fixture.componentInstance.save();
      http.expectOne('/api/projects/10/delegate').flush({}, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      expect(fixture.componentInstance.error).toBe('No se pudo delegar el proyecto.');
      expect(fixture.componentInstance.saving).toBe(false);
    });
  });
});
