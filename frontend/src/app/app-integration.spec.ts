import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TaskDto } from './core/models/api';
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
    list.loadTasks();
    http
      .expectOne((r) => r.url === '/api/tasks' && r.params.get('projectId') === '42')
      .flush([]);
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
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.subtasks-count')?.textContent?.trim()).toBe('1');
    http.verify();
    fixture.destroy();
  });
});
