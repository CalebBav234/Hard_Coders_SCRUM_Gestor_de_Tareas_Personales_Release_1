import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivityDetailDto, AssignmentDto, ProjectProgressDto, StageDto, SubtaskDto, TaskDto } from '../../../core/models/api';
import { Project } from '../../../core/models/project';
import { Task } from '../../../core/models/task';
import { TaskEditor } from '../task-editor/task-editor';
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

  /** Proyecto de trabajo elegido en la pantalla. */
  const manager = { userId: 2, fullName: 'Ana Paz', email: 'a@gestor.local', chief: false, active: true };
  const project: Project = {
    projectId: 42,
    name: 'Proyecto de pruebas',
    description: null,
    status: 'EN_EJECUCION',
    closed: false,
    budgetBob: 1000,
    allocatedBudgetBob: 0,
    estimatedDurationMinutes: 60,
    manager,
    createdBy: manager,
    currentRevision: 1,
    createdAt: '2026-08-20T12:00:00Z',
    updatedAt: '2026-08-20T12:00:00Z',
    version: 1,
    canEditDetails: true,
    canEditPlan: true,
    canDelegate: true,
    canManageHierarchy: true,
    allowedStatuses: [],
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
    assignedWorkerIds: [],
    subtasks: [child],
  };

  /** US3.3: árbol de progreso en cascada del proyecto seleccionado. */
  const progress: ProjectProgressDto = {
    projectId: 42,
    projectName: 'Proyecto de pruebas',
    projectStatus: 'EN_EJECUCION',
    budgetBob: 1000,
    allocatedBudgetBob: 0,
    estimatedDurationMinutes: 60,
    root: {
      id: null,
      name: 'Proyecto de pruebas',
      completed: 0,
      total: 1,
      leaf: false,
      ratio: 0,
      children: [
        {
          id: 1,
          name: 'Construcción',
          completed: 0,
          total: 1,
          leaf: false,
          ratio: 0,
          children: [
            {
              id: 1,
              name: 'Cimentación',
              completed: 0,
              total: 1,
              leaf: false,
              ratio: 0,
              children: [{ id: 7, name: 'Original', completed: 0, total: 1, leaf: true, ratio: 0, children: [] }],
            },
          ],
        },
      ],
    },
  };

  function buttons(scope: ParentNode = element): HTMLButtonElement[] {
    return Array.from(scope.querySelectorAll('button'));
  }

  /** Hace clic en un botón: por el texto que muestra o por selector CSS. */
  function click(label: string, scope: ParentNode = element): void {
    const bySelector = label.startsWith('.') || label.startsWith('#');
    const button = bySelector
      ? scope.querySelector<HTMLButtonElement>(label)
      : buttons(scope).find((b) => b.textContent?.includes(label));
    expect(button, `botón "${label}"`).toBeTruthy();
    button!.click();
    fixture.detectChanges();
  }

  /** Valor por DOM más el evento que Angular necesita según el tipo de control. */
  function setValue(selector: string, value: string, scope: ParentNode = element): void {
    const control = scope.querySelector(selector) as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;
    control.value = value;
    control.dispatchEvent(new Event(control.tagName === 'SELECT' ? 'change' : 'input'));
    fixture.detectChanges();
  }

  /** Refresca la vista tras cambiar un campo que no viene de un evento del DOM. */
  function refresh(): void {
    fixture.componentInstance['changeDetector'].markForCheck();
    fixture.detectChanges();
  }

  /**
   * Envía el formulario como haría el navegador al pulsar el botón de tipo submit:
   * dentro de un <dialog> jsdom no dispara el evento de envío con el clic.
   */
  function submitForm(scope: ParentNode = element): void {
    const form = scope.querySelector('form') as HTMLFormElement;
    expect(form, 'formulario').toBeTruthy();
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }

  /** Primer componente del tipo pedido que haya en el árbol renderizado. */
  function componentIn<T>(kind: new (...args: never[]) => unknown): T {
    const find = (node: { componentInstance?: unknown; children: unknown[] }): T | null => {
      if (node.componentInstance instanceof kind) {
        return node.componentInstance as T;
      }
      for (const child of node.children) {
        const found = find(child as { componentInstance?: unknown; children: unknown[] });
        if (found) {
          return found;
        }
      }
      return null;
    };
    const found = find(fixture.debugElement as unknown as { componentInstance?: unknown; children: unknown[] });
    expect(found, `instancia de ${kind.name}`).toBeTruthy();
    return found as T;
  }

  function subtasksModal(): HTMLElement {
    const modal = element.querySelector('.subtasks-modal') as HTMLElement | null;
    expect(modal, 'popup de subtareas').toBeTruthy();
    return modal!;
  }

  /** Evento de puntero sintético: el componente solo lee clientX y pointerId. */
  function pointerEvent(type: string, clientX: number): Event {
    const event = new Event(type, { bubbles: true, cancelable: true });
    Object.assign(event, { clientX, pointerId: 1 });
    return event;
  }

  function stages(): StageDto[] {
    return [
      { stageId: 4, projectId: 42, name: 'Cimentación', description: null, status: 'PENDIENTE', createdAt: '2026-08-20T12:00:00Z', updatedAt: '2026-08-20T12:00:00Z', version: 1 },
      { stageId: 5, projectId: 42, name: 'Estructura', description: null, status: 'EN_PROGRESO', createdAt: '2026-08-20T12:00:00Z', updatedAt: '2026-08-20T12:00:00Z', version: 2 },
    ];
  }

  function activities(): ActivityDetailDto[] {
    return [
      { activityId: 3, stageId: 4, name: 'Excavación', description: null, status: 'PENDIENTE', createdAt: '2026-08-20T12:00:00Z', updatedAt: '2026-08-20T12:00:00Z', version: 1 },
      { activityId: 4, stageId: 4, name: 'Nivelación', description: null, status: 'PENDIENTE', createdAt: '2026-08-20T12:00:00Z', updatedAt: '2026-08-20T12:00:00Z', version: 1 },
    ];
  }

  /** Tarea terminada junto con una subtarea terminada (panel de completadas). */
  const finished: TaskDto = {
    ...original,
    status: 'COMPLETADA',
    currentSegmentStartedAt: null,
    completedAt: '2026-08-27T10:00:00Z',
    activityName: 'Excavación',
    activityId: 3,
    subtasks: [
      {
        ...child,
        status: 'COMPLETADA',
        completedAt: '2026-08-20T12:30:00Z',
        title: 'Subtarea terminada',
        description: 'Detalle de la subtarea',
      },
    ],
  };

  /** Tarea principal y subtarea ya traducidas al modelo de vista. */
  function parentView(overrides: Partial<Task> = {}): Task {
    return {
      id: 7,
      kind: 'TASK',
      entityId: 7,
      title: 'Original',
      description: null,
      status: 'INACTIVA',
      priority: 'ALTA',
      categoryId: null,
      categoryName: null,
      assignedWorkerIds: [],
      parentTaskId: null,
      activatedAt: null,
      completedAt: null,
      createdAt: '2026-08-20T12:00:00Z',
      updatedAt: '2026-08-20T12:00:00Z',
      version: 2,
      ...overrides,
    };
  }

  function subtaskView(overrides: Partial<Task> = {}): Task {
    return {
      ...parentView(),
      id: -7,
      kind: 'SUBTASK',
      entityId: 7,
      title: 'Hija',
      parentTaskId: 7,
      totalActiveSeconds: undefined,
      ...overrides,
    };
  }

  /** El proyecto es obligatorio: la lista se pide siempre con {@code projectId}. */
  async function reload(tasks: TaskDto[]): Promise<void> {
    http
      .expectOne((candidate) => candidate.url === '/api/tasks' && candidate.params.get('projectId') === '42')
      .flush(tasks);
    // US3.3: al cargar las tareas la pantalla recalcula el progreso en cascada.
    http.expectOne('/api/projects/42/progress').flush(progress);
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
    http.expectOne('/api/projects').flush([project]);
    // US3.3: el panel de progreso en cascada pide el árbol al renderizarse.
    http.expectOne('/api/projects/42/progress').flush(progress);
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
    // US3.3: cada carga de tareas recalcula el progreso del proyecto.
    http.expectOne('/api/projects/42/progress').flush(progress);
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

  it('recalculates the cascade progress after every task load (US3.3)', async () => {
    fixture.componentInstance.loadTasks();
    http
      .expectOne((candidate) => candidate.url === '/api/tasks' && candidate.params.get('projectId') === '42')
      .flush([original]);
    const done = {
      ...progress,
      root: { ...progress.root, ratio: 1, completed: 1 },
    };
    http.expectOne('/api/projects/42/progress').flush(done);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.progressRatio).toBe(100);
    expect(element.textContent).toContain('100 %');
  });

  it('reloads the cascade progress when another project is chosen (US3.3)', () => {
    const component = fixture.componentInstance;
    component.projects = [...component.projects, { ...project, projectId: 7, name: 'Otro proyecto' }];
    component['changeDetector'].markForCheck();
    fixture.detectChanges();

    component.selectProject(7);
    http.expectOne('/api/projects/7/stages').flush([]);
    component['changeDetector'].markForCheck();
    fixture.detectChanges();

    // El árbol del proyecto anterior ya no se consulta: se pide el del nuevo.
    http.expectNone((r) => r.url === '/api/projects/42/progress');
    const second = http.expectOne('/api/projects/7/progress');
    expect(second.request.method).toBe('GET');
    second.flush({ ...progress, projectId: 7, root: { ...progress.root, ratio: 1, completed: 1 } });
    fixture.detectChanges();

    // El evento `loaded` del panel actualiza el porcentaje de la barra del proyecto nuevo.
    expect(component.progressRatio).toBe(100);
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

  it('reloads the list when the assignment dialog changes and closes with its button', async () => {
    click('Asignar');
    http.expectOne('/api/projects/42/workers').flush([
      { userId: 5, fullName: 'Ana Perez', email: 'a@x.com', chief: false, active: true },
    ]);
    http.expectOne('/api/tasks/7/assignments').flush([]);
    const dialog = element.querySelector('app-assignment-dialog')!;
    fixture.detectChanges();

    setValue('#assignment-worker', '5', dialog);
    click('Asignar', dialog);
    http.expectOne('/api/tasks/7/assignments').flush({
      taskId: 7,
      taskTitle: 'Original',
      workerUserId: 5,
      workerName: 'Ana Perez',
      assignedAt: '2026-08-20T12:00:00Z',
      assignedByUserId: 1,
      assignedByName: 'Ana Paz',
      reason: null,
    } satisfies AssignmentDto);
    http.expectOne('/api/tasks/7/assignments').flush([]);
    await fixture.whenStable();

    // El diálogo avisa del cambio: la lista se recarga y muestra el mensaje.
    await reload([{ ...original, assignedWorkerIds: [5] }]);
    expect(fixture.componentInstance.successMessage).toBe('Asignación de Personal actualizada.');
    expect(element.textContent).toContain('Asignar');

    click('×', dialog);
    expect(element.querySelector('app-assignment-dialog')).toBeNull();
    expect(fixture.componentInstance.assigningTask).toBeNull();
  });

  it('reports that the projects could not be loaded', () => {
    fixture.componentInstance.loadProjects();
    http
      .expectOne('/api/projects')
      .flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage).toBe('No se pudieron cargar los proyectos.');
    expect(fixture.componentInstance.loading).toBe(false);
  });

  it('asks for a project when nothing is selected', () => {
    fixture.componentInstance.selectProject(null);
    fixture.detectChanges();

    expect(element.textContent).toContain('Selecciona un proyecto');
    // Sin proyecto no se pide ninguna tarea.
    http.expectNone((request) => request.url === '/api/tasks');
  });

  it('keeps the chosen project when the projects are reloaded', () => {
    fixture.componentInstance.loadProjects();
    http.expectOne('/api/projects').flush([project]);
    fixture.detectChanges();

    expect(fixture.componentInstance.selectedProjectId).toBe(42);
    expect(fixture.componentInstance.selectedProject?.projectId).toBe(42);
    // El proyecto sigue elegido en el selector, sin volver a pedirlo.
    const options = Array.from(element.querySelectorAll('#workspace-project option')).map(
      (option) => (option as HTMLOptionElement).value,
    );
    expect(options).toEqual(['0: null', '1: 42']);
    expect(element.textContent).toContain('Proyecto de pruebas');
  });

  it('chooses the stage and the activity where new tasks are created', () => {
    const component = fixture.componentInstance;
    component.selectStage(4);

    const request = http.expectOne(
      (candidate) => candidate.url === '/api/activities' && candidate.params.get('stageId') === '4',
    );
    expect(request.request.method).toBe('GET');
    request.flush(activities());
    fixture.detectChanges();

    expect(component.selectedStageId).toBe(4);
    expect(component.categories.map((category) => category.name)).toEqual(['Excavación', 'Nivelación']);
    // La primera actividad de la etapa queda preseleccionada para crear tareas.
    expect(component.selectedActivityName).toBe('Excavación');

    component.selectActivity(4);
    fixture.detectChanges();
    expect(component.selectedActivityId).toBe(4);
    expect(component.selectedActivityName).toBe('Nivelación');

    // Los selectores de etapa y actividad aparecen en la barra de trabajo.
    expect(element.querySelector('#workspace-stage')).not.toBeNull();
    expect(element.querySelector('#workspace-activity')).not.toBeNull();
    expect(element.textContent).toContain('Nivelación');
  });

  it('drops the chosen activity when the stage changes', () => {
    const component = fixture.componentInstance;
    component.selectStage(4);
    http.expectOne((request) => request.url === '/api/activities').flush(activities());
    component.selectActivity(4);
    expect(component.selectedActivityName).toBe('Nivelación');

    // La otra etapa ofrece otras actividades: la anterior deja de existir.
    component.selectStage(5);
    http
      .expectOne(
        (candidate) => candidate.url === '/api/activities' && candidate.params.get('stageId') === '5',
      )
      .flush(activities());

    expect(component.selectedStageId).toBe(5);
    expect(component.categories.map((category) => category.name)).toEqual(['Excavación', 'Nivelación']);
    expect(component.selectedActivityId).toBe(3);
    expect(component.selectedActivityName).toBe('Excavación');
  });

  it('reports that the activities of the stage could not be loaded', () => {
    fixture.componentInstance.selectStage(4);
    http
      .expectOne((request) => request.url === '/api/activities')
      .flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.categories).toEqual([]);
    expect(fixture.componentInstance.selectedActivityId).toBeNull();
    expect(fixture.componentInstance.selectedActivityName).toBeNull();
  });

  it('reports that the tasks could not be loaded', () => {
    fixture.componentInstance.loadTasks();
    http
      .expectOne((request) => request.url === '/api/tasks')
      .flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage).toBe('No se pudieron cargar las tareas.');
    expect(fixture.componentInstance.loading).toBe(false);
  });

  it('reports that the search could not be completed', () => {
    fixture.componentInstance.searchTerm = 'excavacion';
    fixture.componentInstance.searchTasks();

    const request = http.expectOne(
      (candidate) => candidate.url === '/api/tasks' && candidate.params.get('q') === 'excavacion',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage).toBe('No se pudieron buscar las tareas.');
    expect(fixture.componentInstance.loading).toBe(false);
  });

  it('searches the whole project again when the search box is emptied', async () => {
    fixture.componentInstance.searchTerm = '   ';
    fixture.componentInstance.searchTasks();
    await reload([original]);

    expect(fixture.componentInstance.tasks.map((task) => task.title)).toEqual([
      'Original',
      'Subtarea con el mismo id',
    ]);
  });

  it('does not search anything when no project is selected', () => {
    fixture.componentInstance.selectProject(null);
    fixture.componentInstance.searchTerm = 'excavacion';
    fixture.componentInstance.searchTasks();
    fixture.detectChanges();

    http.expectNone((request) => request.url === '/api/tasks');
    expect(fixture.componentInstance.loading).toBe(false);
  });

  it('opens and closes the structure panel above the tasks (US2.2)', () => {
    const component = fixture.componentInstance;
    expect(component.wbsPanelOpen).toBe(false);

    click('▤ Estructura');
    expect(component.wbsPanelOpen).toBe(true);
    http.expectOne('/api/projects/42/stages').flush(stages());
    fixture.detectChanges();
    expect(element.querySelector('app-wbs-panel')).not.toBeNull();

    click('▤ Estructura');
    expect(component.wbsPanelOpen).toBe(false);
    expect(element.querySelector('app-wbs-panel')).toBeNull();
  });

  it('collects the finished tasks in a side panel with their subtasks', async () => {
    fixture.componentInstance.loadTasks();
    await reload([finished]);

    expect(element.textContent).toContain('✓ Completadas');
    click('✓ Completadas');

    const aside = element.querySelector('.completed-aside')!;
    expect(aside.textContent).toContain('2 tarea(s) terminada(s)');
    // La subtarea terminada se muestra bajo su tarea principal.
    expect(aside.textContent).toContain('SUBTAREA');
    expect(aside.textContent).toContain('Tarea principal:');
    expect(aside.textContent).toContain('Subtarea terminada');
    expect(aside.textContent).toContain('Detalle de la subtarea');
    expect(aside.textContent).toContain('Excavación');
    // La prioridad mostrada en el panel es la de la tarea terminada.
    expect(aside.textContent).toContain('MEDIA');
    expect(aside.querySelector('.completed-empty')).toBeNull();

    // La tarea más reciente va primero.
    const cards = Array.from(aside.querySelectorAll('article')).map((card) => card.textContent ?? '');
    expect(cards.length).toBe(2);
    expect(cards[0]).toContain('Original');
    expect(cards[1]).toContain('Subtarea terminada');

    // Reabrir desde el panel mueve la tarea de nuevo a la lista.
    click('↻ Reabrir', aside);
    const request = http.expectOne('/api/tasks/7/reopen');
    expect(request.request.method).toBe('POST');
    request.flush({ ...finished, status: 'EN_PROGRESO', completedAt: null, version: 3 });

    expect(fixture.componentInstance.completedTasks.map((task) => task.id)).toEqual([-7]);
    expect(fixture.componentInstance.successMessage).toContain('Tarea reabierta');

    click('×', element.querySelector('.completed-aside')!);
    expect(element.querySelector('.completed-aside')).toBeNull();
  });

  it('explains that there is nothing finished yet when the panel is opened', () => {
    fixture.componentInstance.openCompletedAside();
    fixture.detectChanges();

    expect(element.querySelector('.completed-empty')?.textContent).toContain('No hay tareas completadas');
  });

  it('disables the actions of a subtask whose parent is still inactive', () => {
    const component = fixture.componentInstance;
    const parent = parentView();
    const subtask = subtaskView();
    component.tasks = [parent, subtask];
    refresh();

    expect(component.isParentInactive(subtask)).toBe(true);
    expect(component.actionsDisabled(subtask)).toBe(true);
    // La tarea principal de la que cuelga se mantiene operativa.
    expect(component.getParentTask(subtask)?.title).toBe('Original');
    expect(component.actionsDisabled(parent)).toBe(false);
  });

  it('shows how many people are assigned to the task', async () => {
    fixture.componentInstance.loadTasks();
    await reload([{ ...original, assignedWorkerIds: [5, 6] }]);

    const assignButton = buttons().find((b) => b.textContent?.includes('Asignar'));
    expect(assignButton?.textContent).toContain('2');
  });

  it('swipes the card to the right to finish it and to the left to ask for the deletion', async () => {
    const component = fixture.componentInstance;
    fixture.componentInstance.loadTasks();
    await reload([{ ...original, subtasks: [] }]);

    const card = element.querySelector('#task-card-7') as HTMLElement;
    card.dispatchEvent(pointerEvent('pointerdown', 100));
    expect(component.getSwipeOffset(7)).toBe(0);

    card.dispatchEvent(pointerEvent('pointermove', 240));
    fixture.detectChanges();
    expect(component.isSwiping(7)).toBe(true);
    expect(component.getSwipeOffset(7)).toBe(component['maxSwipeDistance']);
    expect(element.querySelector('.swipe-complete')?.classList.contains('revealed')).toBe(true);

    card.dispatchEvent(pointerEvent('pointerup', 240));
    fixture.detectChanges();
    expect(component.getSwipeOffset(7)).toBe(0);
    const request = http.expectOne('/api/tasks/7/complete');
    expect(request.request.method).toBe('POST');
    request.flush({ ...original, status: 'COMPLETADA' });
    // La tarea sigue en pantalla porque aún tiene una subtarea sin terminar.
    await reload([{ ...finished, status: 'EN_PROGRESO', currentSegmentStartedAt: '2026-08-20T12:00:00Z' }]);

    // El gesto hacia la izquierda abre la confirmación de eliminación.
    const sameCard = element.querySelector('#task-card-7') as HTMLElement;
    sameCard.dispatchEvent(pointerEvent('pointerdown', 0));
    sameCard.dispatchEvent(pointerEvent('pointermove', -240));
    fixture.detectChanges();
    expect(component.getSwipeOffset(7)).toBe(-component['maxSwipeDistance']);
    expect(element.querySelector('.swipe-delete')?.classList.contains('revealed')).toBe(true);

    sameCard.dispatchEvent(pointerEvent('pointerup', -240));
    fixture.detectChanges();
    expect(element.querySelector('.delete-confirmation')?.textContent).toContain('Original');
    http.expectNone((request) => request.method === 'DELETE');
  });

  it('ignores the gestures that do not end in an action', () => {
    const component = fixture.componentInstance;
    const card = element.querySelector('#task-card-7') as HTMLElement;

    // Un gesto que empieza sobre un botón no debe desplazar la tarjeta.
    const insideButton = element.querySelector('#task-card-7 button') as HTMLElement;
    insideButton.dispatchEvent(pointerEvent('pointerdown', 40));
    expect(component.getSwipeOffset(7)).toBe(0);

    // Un desplazamiento mínimo se considera un toque, no un gesto.
    card.dispatchEvent(pointerEvent('pointerdown', 0));
    card.dispatchEvent(pointerEvent('pointermove', 3));
    expect(component['swipeMoved']).toBe(false);

    // Los movimientos de otra tarjeta se ignoran.
    card.dispatchEvent(pointerEvent('pointermove', 60));
    component['activeSwipeTaskId'] = 99;
    card.dispatchEvent(pointerEvent('pointerup', 60));
    expect(component.getSwipeOffset(7)).toBe(60);

    // Un gesto cancelado devuelve la tarjeta a su sitio.
    component['activeSwipeTaskId'] = 7;
    component.cancelSwipe(7);
    fixture.detectChanges();
    expect(component.getSwipeOffset(7)).toBe(0);
    expect(component.isSwiping(7)).toBe(false);
  });

  it('refuses to delete a task while another deletion is in flight', async () => {
    click('Eliminar');
    click('Eliminar', element.querySelector('.delete-confirmation')!);
    const request = http.expectOne('/api/tasks/7?version=2');

    // La tarea queda ocupada: un segundo intento no envía nada.
    fixture.componentInstance.deleteTask(fixture.componentInstance.tasks[0]);
    http.expectNone((candidate) => candidate.method === 'DELETE' && !candidate.url.endsWith('7?version=2'));

    request.flush(null, { status: 204, statusText: 'No Content' });
    await reload([]);
  });

  it('reports a failed completion and reopening', async () => {
    const component = fixture.componentInstance;
    component.loadTasks();
    await reload([{ ...original, subtasks: [] }]);

    click('Completar');
    http
      .expectOne('/api/tasks/7/complete')
      .flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    expect(component.errorMessage).toBe('No se pudo completar la tarea.');
    expect(component.busyTaskId).toBeNull();

    component.reopen(component.tasks[0]);
    http
      .expectOne('/api/tasks/7/reopen')
      .flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    expect(component.errorMessage).toBe('No se pudo reabrir la tarea.');
    expect(component.busyTaskId).toBeNull();
  });

  it('reports a failed activation and a failed pause', async () => {
    const component = fixture.componentInstance;
    component.loadTasks();
    await reload([{ ...original, status: 'PENDIENTE', subtasks: [] }]);

    click('▶ Activar');
    http
      .expectOne('/api/tasks/7/start')
      .flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(component.errorMessage).toBe('No se pudo activar la tarea.');

    click('▶ Activar');
    http.expectOne('/api/tasks/7/start').flush({
      ...original,
      status: 'EN_PROGRESO',
      currentSegmentStartedAt: '2026-08-20T12:00:00Z',
      subtasks: [],
    });
    await reload([{ ...original, status: 'EN_PROGRESO', currentSegmentStartedAt: '2026-08-20T12:00:00Z', subtasks: [] }]);

    click('⏸ Pausar');
    http
      .expectOne('/api/tasks/7/pause')
      .flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(component.errorMessage).toBe('No se pudo pausar la tarea.');
  });

  it('opens the subtasks popup, creates a subtask and comes back to the list', async () => {
    const component = fixture.componentInstance;
    click('Subtareas');
    expect(subtasksModal().textContent).toContain('Subtareas de "Original"');
    expect(element.querySelector('.subtasks-modal-summary')?.textContent).toContain('1 subtarea(s)');

    click('+ Nueva subtarea');
    expect(component.subtaskModalMode).toBe('create');

    setValue('#title', 'Nueva subtarea desde el popup', subtasksModal());
    click('Crear subtarea', subtasksModal());
    const request = http.expectOne('/api/tasks/7/subtasks');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ title: 'Nueva subtarea desde el popup', description: null });
    request.flush({ ...child, subtaskId: 8, title: 'Nueva subtarea desde el popup' });
    await reload([{ ...original, subtasks: [{ ...child }, { ...child, subtaskId: 8 }] }]);

    expect(component.subtaskModalMode).toBe('list');
    expect(component.successMessage).toBe('Subtarea creada correctamente.');
    expect(element.querySelector('.subtasks-modal-summary')?.textContent).toContain('2 subtarea(s)');
  });

  it('edits a subtask from inside the popup', () => {
    const component = fixture.componentInstance;
    click('Subtareas');
    click('✎ Editar', subtasksModal());
    expect(component.subtaskModalMode).toBe('edit');
    expect(component.editingSubtask?.title).toBe('Subtarea con el mismo id');
    // El editor de una subtarea no consulta categorías.
    http.expectNone((request) => request.url === '/api/activities');
    expect(subtasksModal().querySelector('app-task-editor')).not.toBeNull();
    expect(subtasksModal().textContent).toContain('Editar subtarea');

    // El editor avisa de la subtarea guardada: el popup vuelve a la lista.
    const edited: Task = { ...component.getSubtasks(7)[0], title: 'Subtarea editada' };
    componentIn<TaskEditor>(TaskEditor).saved.emit(edited);
    fixture.detectChanges();

    expect(component.subtaskModalMode).toBe('list');
    expect(component.editingSubtask).toBeNull();
    expect(component.getSubtasks(7)[0].title).toBe('Subtarea editada');
    expect(component.successMessage).toBe('Subtarea actualizada correctamente.');
    expect(subtasksModal().textContent).toContain('Subtarea editada');
  });

  it('returns from the subtask form to the list of subtasks', () => {
    const component = fixture.componentInstance;
    click('Subtareas');
    click('+ Nueva subtarea');
    click('Cancelar', subtasksModal());

    expect(component.subtaskModalMode).toBe('list');
    expect(subtasksModal().textContent).toContain('Subtarea con el mismo id');
  });

  it('closes the subtasks popup with its close button', () => {
    const component = fixture.componentInstance;
    click('Subtareas');
    click('×', subtasksModal());

    expect(component.subtasksModalParent).toBeNull();
    expect(element.querySelector('.subtasks-modal')).toBeNull();
  });

  it('deletes a subtask after confirming inside the popup', async () => {
    const component = fixture.componentInstance;
    click('Subtareas');
    click('🗑 Eliminar', subtasksModal());
    expect(subtasksModal().textContent).toContain('¿Eliminar "Subtarea con el mismo id"?');

    click('Eliminar', subtasksModal().querySelector('.subtask-delete-confirm')!);
    const request = http.expectOne('/api/subtasks/7?version=55');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await reload([original]);

    expect(component.successMessage).toContain('eliminada');
  });

  it('changes the category of a subtask from the popup editor', async () => {
    const component = fixture.componentInstance;
    component.selectStage(4);
    http.expectOne((request) => request.url === '/api/activities').flush(activities());

    click('Subtareas');
    const subtask = component.getSubtasks(7)[0];
    component.startCategoryEdit(subtask);
    refresh();
    expect(component.editingCategoryId).toBe(subtask.id);
    expect(component.selectedCategoryName).toBeNull();

    // Sin actividad elegida no se envía nada.
    component.saveCategory(subtask);
    expect(component.errorMessage).toBe('Selecciona una actividad válida.');

    component.selectedCategoryName = 'Excavación';
    component.saveCategory(subtask);
    const request = http.expectOne('/api/tasks/7/activity');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ activityId: 3, version: 55 });
    request.flush({ ...finished, status: 'EN_PROGRESO' });
    await reload([original]);

    expect(component.successMessage).toBe('Categoría actualizada.');
    expect(component.editingCategoryId).toBeNull();
    expect(component.selectedCategoryName).toBeNull();
  });

  it('reports a failed category change', () => {
    const component = fixture.componentInstance;
    component.selectStage(4);
    http.expectOne((request) => request.url === '/api/activities').flush(activities());
    component.startCategoryEdit(component.tasks[0]);
    component.selectedCategoryName = 'Excavación';
    component.saveCategory(component.tasks[0]);

    http
      .expectOne('/api/tasks/7/activity')
      .flush({ error: 'ACTIVITY_NOT_FOUND' }, { status: 404, statusText: 'Not Found' });

    expect(component.errorMessage).toBe('No se pudo actualizar la categoría.');
    expect(component.busyTaskId).toBeNull();
  });

  it('does not open the popup of a task that is busy with something else', () => {
    const component = fixture.componentInstance;
    const subtask = component.getSubtasks(7)[0];

    // Mientras se confirma el borrado de otra tarjeta, la principal queda inhabilitada.
    component.confirmingDelete = subtask.id;
    component.openSubtasksModal(component.tasks[0]);
    expect(component.subtasksModalParent).toBeNull();

    // Durante la carga tampoco se abre nada.
    component.confirmingDelete = null;
    component.loading = true;
    component.openSubtasksModal(component.tasks[0]);
    component.createSubtask(component.tasks[0]);
    component.editTask(component.tasks[0]);
    expect(component.subtasksModalParent).toBeNull();

    // Sin carga volvemos a poder abrir el popup.
    component.loading = false;
    component.openSubtasksModal(component.tasks[0]);
    expect(component.subtasksModalParent?.title).toBe('Original');
  });
});

describe('TaskList como Personal — US3.1 (solo tareas asignadas)', () => {
  let fixture: ComponentFixture<TaskList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [TaskList],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskList);
    // El Personal no tiene proyectos: la lista de proyectos llega vacía.
    fixture.detectChanges();
    http.expectOne('/api/projects').flush([]);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
  });

  it('loads the assigned tasks instead of any project workspace', async () => {
    const request = http.expectOne((r) => r.url === '/api/tasks/assigned');
    expect(request.request.method).toBe('GET');
    request.flush([
      {
        taskId: 5,
        projectId: 42,
        activityId: 1,
        activityName: 'Cimentación',
        title: 'Excavar',
        description: null,
        status: 'PENDIENTE',
        priority: 'ALTA',
        actualDurationMinutes: null,
        trackedSeconds: 0,
        currentSegmentStartedAt: null,
        completedAt: null,
        createdAt: '2026-10-01T10:00:00Z',
        updatedAt: '2026-10-01T10:00:00Z',
        version: 3,
        subtasks: [],
      },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.usesPersonalScope).toBe(true);
    expect(fixture.componentInstance.tasks.map((task) => task.title)).toEqual(['Excavar']);
    // Sin proyecto seleccionado nunca se pide la lista de tareas de un proyecto.
    http.expectNone((r) => r.url === '/api/tasks' && r.params.get('projectId') !== null);
    // El Personal no ve el selector de proyectos ni el de etapas.
    expect(fixture.nativeElement.querySelector('#workspace-project')).toBeNull();
    // La pantalla se presenta como tareas asignadas, no como el espacio de trabajo.
    expect(fixture.nativeElement.querySelector('.page-title')?.textContent).toContain('Tareas asignadas');
  });

  it('searches inside the assigned tasks on the server side', async () => {
    http.expectOne('/api/tasks/assigned').flush([]);
    await fixture.whenStable();

    const component = fixture.componentInstance;
    component.searchTerm = 'excavar';
    component.searchTasks();
    fixture.detectChanges();

    const request = http.expectOne((r) => r.url === '/api/tasks/assigned' && r.params.get('q') === 'excavar');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
