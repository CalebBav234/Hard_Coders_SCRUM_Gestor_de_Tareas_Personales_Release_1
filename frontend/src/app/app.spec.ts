import { By } from '@angular/platform-browser';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Task } from './core/models/task';
import { TaskService } from './core/services/task.service';
import { TaskForm } from './features/tasks/task-form/task-form';
import { TaskList } from './features/tasks/task-list/task-list';
import { App } from './app';

describe('App', () => {
  const createdTask: Task = {
    id: 42,
    title: 'Tarea integrada',
    status: 'INACTIVA',
    priority: 'MEDIA',
    version: 0,
  };

  const taskService = {
    listTasks: () => of([]),
    listHistory: () => of([]),
    listCategories: () => of([]),
    createTask: () => of(createdTask),
    activate: () => of(createdTask),
    complete: () => of(createdTask),
  } as unknown as TaskService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{ provide: TaskService, useValue: taskService }],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Gestor de Tareas');
  });

  it('should display the task returned by the backend immediately after creation', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    const taskForm = fixture.debugElement.query(By.directive(TaskForm))
      .componentInstance as TaskForm;
    const taskList = fixture.debugElement.query(By.directive(TaskList))
      .componentInstance as TaskList;

    taskForm.title = createdTask.title;
    taskForm.crearTarea();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(taskList.tasks).toEqual([createdTask]);
    expect(compiled.querySelector('.task-card h3')?.textContent).toContain(createdTask.title);
    expect(compiled.querySelector('.task-count')?.textContent).toContain('1 tareas');
  });
});
