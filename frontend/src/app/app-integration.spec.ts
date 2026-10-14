import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ProjectProgressDto, TaskDto } from './core/models/api';
import { TaskForm } from './features/tasks/task-form/task-form';
import { TaskHome } from './features/tasks/task-home/task-home';
import { TaskList } from './features/tasks/task-list/task-list';

describe('Pantalla de tareas integrada con la API V4', () => {
  const created: TaskDto = {
    taskId: 42,
    activityId: 3,
    activityName: 'Trabajo',
    title: 'Tarea integrada',
    description: null,
    status: 'PENDIENTE',
    priority: 'ALTA',
    allocatedBudgetBob: 0,
    estimatedDurationMinutes: 0,
    actualDurationMinutes: null,
    trackedSeconds: 0,
    currentSegmentStartedAt: null,
    completedAt: null,
    createdAt: '2026-10-09T12:00:00Z',
    updatedAt: '2026-10-09T12:00:00Z',
    version: 1,
    assignedWorkerIds: [],
    subtasks: [],
  };

  it('creates a task and then a subtask, showing both without reloading the page', async () => {
    await TestBed.configureTestingModule({
      imports: [TaskHome],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(TaskHome);
    const element: HTMLElement = fixture.nativeElement;
    const list = fixture.debugElement.query(By.directive(TaskList)).componentInstance as TaskList;
    // El usuario elige el proyecto: la pantalla ya no carga ninguno por defecto.
    list.selectedProjectId = 42;
    fixture.detectChanges();
    http.expectOne('/api/projects').flush([
      {
        projectId: 42,
        name: 'Proyecto integrado',
        description: null,
        status: 'EN_EJECUCION',
        closed: false,
        budgetBob: 5000,
        allocatedBudgetBob: 0,
        estimatedDurationMinutes: 600,
        manager: null,
        createdBy: null,
        currentRevision: 1,
        createdAt: '2026-10-09T12:00:00Z',
        updatedAt: '2026-10-09T12:00:00Z',
        version: 1,
        canEditDetails: true,
        canEditPlan: true,
        canDelegate: true,
        canManageHierarchy: true,
        allowedStatuses: [],
      },
    ]);
    // US3.3: el panel de progreso en cascada pide el árbol al renderizarse.
    const emptyProgress: ProjectProgressDto = {
      projectId: 42,
      projectName: 'Proyecto integrado',
      projectStatus: 'EN_EJECUCION',
      budgetBob: 5000,
      allocatedBudgetBob: 0,
      estimatedDurationMinutes: 600,
      root: { id: null, name: 'Proyecto integrado', completed: 0, total: 0, leaf: true, ratio: 0, children: [] },
    };
    http.expectOne('/api/projects/42/progress').flush(emptyProgress);
    list.loadTasks();
    http
      .expectOne((r) => r.url === '/api/tasks' && r.params.get('projectId') === '42')
      .flush([]);
    // US3.3: la pantalla recalcula el progreso en cascada tras cargar las tareas.
    http.expectOne('/api/projects/42/progress').flush(emptyProgress);
    await fixture.whenStable();

    const form = fixture.debugElement.query(By.directive(TaskForm)).componentInstance as TaskForm;
    form.title = 'Tarea integrada';
    form.selectedPriority = 'ALTA';
    form.activityId = 3;
    form.crearTarea();

    const post = http.expectOne('/api/tasks');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({
      activityId: 3,
      title: 'Tarea integrada',
      priority: 'ALTA',
      allocatedBudgetBob: 0,
      estimatedDurationMinutes: 0,
    });
    post.flush(created);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.task-title')?.textContent).toContain('Tarea integrada');

    list.createSubtask(list.parentTasks[0]);
    fixture.detectChanges();

    const subtaskForm = fixture.debugElement
      .queryAll(By.directive(TaskForm))
      .map((debug) => debug.componentInstance as TaskForm)
      .find((candidate) => candidate.parentTaskId === 42)!;
    subtaskForm.title = 'Paso 1';
    subtaskForm.crearTarea();

    const subtaskPost = http.expectOne('/api/tasks/42/subtasks');
    expect(subtaskPost.request.body).toEqual({ title: 'Paso 1', description: null });
    subtaskPost.flush({
      subtaskId: 1,
      taskId: 42,
      title: 'Paso 1',
      description: null,
      status: 'PENDIENTE',
      completedAt: null,
      createdAt: '2026-10-09T12:01:00Z',
      updatedAt: '2026-10-09T12:01:00Z',
      version: 2,
    });
    http
      .expectOne((r) => r.url === '/api/tasks' && r.params.get('projectId') === '42')
      .flush([
        {
          ...created,
          subtasks: [
            {
              subtaskId: 1,
              taskId: 42,
              title: 'Paso 1',
              description: null,
              status: 'PENDIENTE',
              completedAt: null,
              createdAt: '2026-10-09T12:01:00Z',
              updatedAt: '2026-10-09T12:01:00Z',
              version: 2,
            },
        ],
      },
    ]);
    // US3.3: cada carga de tareas recalcula el progreso del proyecto.
    http.expectOne('/api/projects/42/progress').flush(emptyProgress);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.subtasks-count')?.textContent?.trim()).toBe('1');
    http.verify();
    fixture.destroy();
  });

  it('hands the chosen activity to the create form (US2.2)', async () => {
    await TestBed.configureTestingModule({
      imports: [TaskHome],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(TaskHome);
    const element: HTMLElement = fixture.nativeElement;
    const list = fixture.debugElement.query(By.directive(TaskList)).componentInstance as TaskList;
    list.selectedProjectId = 42;
    fixture.detectChanges();
    http.expectOne('/api/projects').flush([
      {
        projectId: 42,
        name: 'Proyecto integrado',
        description: null,
        status: 'EN_EJECUCION',
        closed: false,
        budgetBob: 5000,
        allocatedBudgetBob: 0,
        estimatedDurationMinutes: 600,
        manager: null,
        createdBy: null,
        currentRevision: 1,
        createdAt: '2026-10-09T12:00:00Z',
        updatedAt: '2026-10-09T12:00:00Z',
        version: 1,
        canEditDetails: true,
        canEditPlan: true,
        canDelegate: true,
        canManageHierarchy: true,
        allowedStatuses: [],
      },
    ]);
    const emptyProgress: ProjectProgressDto = {
      projectId: 42,
      projectName: 'Proyecto integrado',
      projectStatus: 'EN_EJECUCION',
      budgetBob: 5000,
      allocatedBudgetBob: 0,
      estimatedDurationMinutes: 600,
      root: { id: null, name: 'Proyecto integrado', completed: 0, total: 0, leaf: true, ratio: 0, children: [] },
    };
    http.expectOne('/api/projects/42/progress').flush(emptyProgress);
    list.loadTasks();
    http.expectOne((r) => r.url === '/api/tasks' && r.params.get('projectId') === '42').flush([]);
    http.expectOne('/api/projects/42/progress').flush(emptyProgress);
    await fixture.whenStable();
    fixture.detectChanges();

    // El usuario elige la etapa en la barra de trabajo.
    list.selectStage(1);
    http
      .expectOne((request) => request.url === '/api/activities' && request.params.get('stageId') === '1')
      .flush([
        {
          activityId: 9,
          stageId: 1,
          name: 'Cimentación',
          description: null,
          status: 'PENDIENTE',
          createdAt: '2026-10-09T12:00:00Z',
          updatedAt: '2026-10-09T12:00:00Z',
          version: 1,
        },
      ]);
    await fixture.whenStable();
    fixture.detectChanges();

    // El formulario de crear tarea recibe la actividad elegida: sin esto no se podía crear.
    const form = fixture.debugElement.query(By.directive(TaskForm)).componentInstance as TaskForm;
    expect(form.activityId).toBe(9);
    expect(form.activityName).toBe('Cimentación');

    // Y al crear, la petición lleva esa actividad.
    form.title = 'Excavar terreno';
    form.selectedPriority = 'ALTA';
    form.crearTarea();
    const created = http.expectOne('/api/tasks');
    expect(created.request.body.activityId).toBe(9);
    created.flush({
      taskId: 43,
      activityId: 9,
      activityName: 'Cimentación',
      title: 'Excavar terreno',
      description: null,
      status: 'PENDIENTE',
      priority: 'ALTA',
      allocatedBudgetBob: 0,
      estimatedDurationMinutes: 0,
      actualDurationMinutes: null,
      trackedSeconds: 0,
      currentSegmentStartedAt: null,
      completedAt: null,
      createdAt: '2026-10-09T12:05:00Z',
      updatedAt: '2026-10-09T12:05:00Z',
      version: 1,
      assignedWorkerIds: [],
      subtasks: [],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(form.error).toBeNull();
    expect(element.textContent).toContain('Cimentación');
    http.verify();
    fixture.destroy();
  });
});
