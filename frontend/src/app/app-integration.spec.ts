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
    fixture.detectChanges();
    http.expectOne('/api/tasks').flush([]);
    http.expectOne('/api/activities').flush([]);
    await fixture.whenStable();

    const form = fixture.debugElement.query(By.directive(TaskForm)).componentInstance as TaskForm;
    form.title = 'Tarea integrada';
    form.selectedPriority = 'ALTA';
    form.selectedCategoryName = 'Trabajo';
    form.crearTarea();

    const post = http.expectOne('/api/tasks');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ title: 'Tarea integrada', priority: 'ALTA', activityName: 'Trabajo' });
    post.flush(created);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.task-title')?.textContent).toContain('Tarea integrada');

    const list = fixture.debugElement.query(By.directive(TaskList)).componentInstance as TaskList;
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
    http.expectOne('/api/tasks').flush([
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
