import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SubtaskDto, TaskDto } from '../../../core/models/api';
import { TaskList } from './task-list';

describe('TaskList sobre la API V4', () => {
  let fixture: ComponentFixture<TaskList>;
  let http: HttpTestingController;
  let element: HTMLElement;

  const child: SubtaskDto = {
    subtaskId: 7,
    taskId: 7,
    title: 'Subtarea con el mismo id',
    description: null,
    status: 'PENDIENTE',
    completedAt: null,
    createdAt: '2026-08-20T12:30:00Z',
    updatedAt: '2026-08-20T12:30:00Z',
    version: 55,
  };

  const original: TaskDto = {
    taskId: 7,
    activityId: 1,
    activityName: null,
    title: 'Original',
    description: 'Descripción inicial',
    status: 'EN_PROGRESO',
    priority: 'MEDIA',
    allocatedBudgetBob: 0,
    estimatedDurationMinutes: 0,
    actualDurationMinutes: 1,
    trackedSeconds: 60,
    currentSegmentStartedAt: '2026-08-20T13:00:00Z',
    completedAt: null,
    createdAt: '2026-08-20T12:00:00Z',
    updatedAt: '2026-08-20T13:00:00Z',
    version: 2,
    subtasks: [child],
  };

  function buttons(scope: ParentNode = element): HTMLButtonElement[] {
    return Array.from(scope.querySelectorAll('button'));
  }

  function click(label: string, scope: ParentNode = element): void {
    const button = buttons(scope).find((b) => b.textContent?.includes(label));
    expect(button, `botón "${label}"`).toBeTruthy();
    button!.click();
    fixture.detectChanges();
  }

  /** El proyecto es obligatorio: la lista se pide siempre con {@code projectId}. */
  async function reload(tasks: TaskDto[]): Promise<void> {
    http
      .expectOne((candidate) => candidate.url === '/api/tasks' && candidate.params.get('projectId') === '42')
      .flush(tasks);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [TaskList],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskList);
    element = fixture.nativeElement;
    const component = fixture.componentInstance;
    // El usuario elige el proyecto: sin uno seleccionado no se carga nada.
    component.selectedProjectId = 42;
    fixture.detectChanges();
    http.expectOne('/api/projects').flush([
      {
        projectId: 42,
        name: 'Proyecto de pruebas',
        description: null,
        status: 'EN_EJECUCION',
        closed: false,
        budgetBob: 1000,
        allocatedBudgetBob: 0,
        estimatedDurationMinutes: 60,
        manager: null,
        createdBy: null,
        currentRevision: 1,
        createdAt: '2026-08-20T12:00:00Z',
        updatedAt: '2026-08-20T12:00:00Z',
        version: 1,
        canEditDetails: true,
        canEditPlan: true,
        canDelegate: true,
        canManageHierarchy: true,
        allowedStatuses: [],
      },
    ]);
    component.loadTasks();
    await reload([original]);
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
  });

  it('renders tasks with their timer and subtask counter', () => {
    expect(element.querySelector('.task-title')?.textContent).toContain('Original');
    expect(element.textContent).toContain('Descripción inicial');
    expect(element.querySelector('.task-status')?.textContent).toContain('ACTIVA');
    expect(element.querySelector('.active-time')).not.toBeNull();
    expect(element.querySelector('.subtasks-count')?.textContent?.trim()).toBe('1');
    expect(fixture.componentInstance.tasks.map((task) => task.id)).toEqual([7, -7]);
  });

  it('searches by title or description through the backend', async () => {
    const input = element.querySelector('#task-search') as HTMLInputElement;
    input.value = 'documentación';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    click('Buscar');

    const request = http.expectOne(
      (candidate) => candidate.url === '/api/tasks' && candidate.params.get('q') === 'documentación',
    );
    request.flush([{ ...original, title: 'Actualizar manual', subtasks: [] }]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.task-title')?.textContent).toContain('Actualizar manual');
  });

  it('pauses an active task through /start-/pause and reloads the list', async () => {
    click('Pausar');
    const req = http.expectOne('/api/tasks/7/pause');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ version: 2 });
    req.flush({ ...original, status: 'PENDIENTE', currentSegmentStartedAt: null, version: 3 });
    await reload([{ ...original, status: 'PENDIENTE', currentSegmentStartedAt: null, version: 3 }]);

    expect(fixture.componentInstance.tasks[0].status).toBe('INACTIVA');
    const labels = buttons().map((b) => b.textContent?.trim());
    expect(labels).toContain('▶ Activar');
    expect(labels).not.toContain('⏸ Pausar');
  });

  it('pauses the open subtasks together with their task', async () => {
    const running = { ...original, subtasks: [{ ...child, status: 'EN_PROGRESO' as const }] };
    fixture.componentInstance.loadTasks();
    await reload([running]);

    click('Pausar');
    http.expectOne('/api/tasks/7/pause').flush({ ...running, status: 'PENDIENTE', version: 3 });
    const subtaskPause = http.expectOne('/api/subtasks/7/pause');
    expect(subtaskPause.request.body).toEqual({ version: 55 });
    subtaskPause.flush({ ...child, status: 'PENDIENTE', version: 56 });
    await reload([{ ...original, status: 'PENDIENTE', currentSegmentStartedAt: null, version: 3 }]);

    expect(fixture.componentInstance.successMessage).toContain('subtareas pausadas');
  });

  it('completes and reopens a task', async () => {
    click('Completar');
    http.expectOne('/api/tasks/7/complete').flush({ ...original, status: 'COMPLETADA', version: 3 });
    const completed = {
      ...original,
      status: 'COMPLETADA' as const,
      currentSegmentStartedAt: null,
      completedAt: '2026-08-26T18:00:00Z',
      version: 3,
      subtasks: [],
    };
    await reload([completed]);
    expect(fixture.componentInstance.completedTasks.map((task) => task.id)).toEqual([7]);

    fixture.componentInstance.reopen(fixture.componentInstance.completedTasks[0]);
    const req = http.expectOne('/api/tasks/7/reopen');
    expect(req.request.body).toEqual({ version: 3 });
    req.flush({ ...completed, status: 'EN_PROGRESO', completedAt: null, version: 4 });
    await fixture.whenStable();

    expect(fixture.componentInstance.tasks[0].status).toBe('ACTIVA');
  });

  it('deletes only after confirmation and sends the displayed version', async () => {
    click('Eliminar');
    expect(element.querySelector('.delete-confirmation')?.textContent).toContain('Original');
    http.expectNone((request) => request.method === 'DELETE');

    click('Eliminar', element.querySelector('.delete-confirmation')!);
    const request = http.expectOne('/api/tasks/7?version=2');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await reload([]);

    expect(element.querySelector('.task-card')).toBeNull();
    expect(fixture.componentInstance.successMessage).toContain('eliminada');
  });

  it('keeps the task when the server rejects the deletion', async () => {
    click('Eliminar');
    click('Eliminar', element.querySelector('.delete-confirmation')!);
    http
      .expectOne('/api/tasks/7?version=2')
      .flush({ error: 'TASK_HAS_SUBTASKS' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.task-title')?.textContent).toContain('Original');
    expect(fixture.componentInstance.errorMessage).toContain('No se pudo eliminar');
    expect(fixture.componentInstance.busyTaskId).toBeNull();
  });

  it('acts on subtasks through /api/subtasks without timer or priority', async () => {
    click('Subtareas');
    const modal = element.querySelector('dialog, .subtasks-modal, [role="dialog"]') ?? element;
    expect(modal.textContent).toContain('Subtarea con el mismo id');
    expect(fixture.componentInstance.getSubtasks(7)[0].totalActiveSeconds).toBeUndefined();

    fixture.componentInstance.activate(fixture.componentInstance.getSubtasks(7)[0]);
    const req = http.expectOne('/api/subtasks/7/start');
    expect(req.request.body).toEqual({ version: 55 });
    req.flush({ ...child, status: 'EN_PROGRESO', version: 56 });
    await reload([{ ...original, subtasks: [{ ...child, status: 'EN_PROGRESO', version: 56 }] }]);

    expect(fixture.componentInstance.getSubtasks(7)[0].status).toBe('ACTIVA');
  });
});
