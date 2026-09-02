import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TaskService } from './task.service';
import { Task, UpdateTaskRequest } from '../models/task';

describe('TaskService edit/delete contract', () => {
  let service: TaskService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TaskService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('sends only editable fields and version in PUT', () => {
    const request: UpdateTaskRequest = {
      title: 'Editada',
      description: null,
      priority: 'ALTA',
      version: 2,
    };
    service.updateTask(7, request).subscribe();
    const req = http.expectOne('/api/tasks/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 7, ...request, version: 3 });
  });

  it('uses DELETE with the displayed task version and accepts 204', () => {
    const task: Task = { id: 7, title: 'Tarea', status: 'INACTIVA', priority: 'MEDIA', version: 3 };
    service.deleteTask(task).subscribe();
    const req = http.expectOne('/api/tasks/7?version=3');
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body).toBeNull();
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('creates a task carrying title, priority and category name', () => {
    const expected = {
      id: 1,
      title: 'Nueva',
      priority: 'ALTA',
      categoryId: 3,
      categoryName: 'Trabajo',
      version: 0,
    };
    service.createTask('Nueva', 'ALTA', 'Trabajo').subscribe((task) => {
      expect(task).toEqual(expected);
    });
    const req = http.expectOne('/api/tasks');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'Nueva', priority: 'ALTA', categoryName: 'Trabajo' });
    req.flush(expected);
  });

  it('posts the version to reopen a terminated task', () => {
    const task: Task = {
      id: 7,
      title: 'Tarea',
      status: 'TERMINADA',
      priority: 'MEDIA',
      version: 3,
    };
    service.reopen(task).subscribe();
    const req = http.expectOne('/api/tasks/7/reopen');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ version: 3 });
    req.flush({ id: 7, title: 'Tarea', status: 'ACTIVA', priority: 'MEDIA', version: 4 });
  });

  it('posts the version to pause an active task', () => {
    const task: Task = { id: 7, title: 'Tarea', status: 'ACTIVA', priority: 'MEDIA', version: 3 };
    service.pause(task).subscribe();
    const req = http.expectOne('/api/tasks/7/pause');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ version: 3 });
    req.flush({ id: 7, title: 'Tarea', status: 'INACTIVA', priority: 'MEDIA', version: 4 });
  });

  it('sends categoryName and version when assigning a category', () => {
    const task: Task = { id: 7, title: 'Tarea', status: 'ACTIVA', priority: 'MEDIA', version: 3 };
    service.changeCategory(task, 'Trabajo').subscribe();
    const req = http.expectOne('/api/tasks/7/category');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ categoryName: 'Trabajo', version: 3 });
    req.flush({
      id: 7,
      title: 'Tarea',
      categoryId: 5,
      categoryName: 'Trabajo',
      status: 'ACTIVA',
      priority: 'MEDIA',
      version: 4,
    });
  });

  it('sends a null categoryName to clear the category', () => {
    const task: Task = {
      id: 7,
      title: 'Tarea',
      status: 'ACTIVA',
      priority: 'MEDIA',
      categoryId: 5,
      categoryName: 'Trabajo',
      version: 3,
    };
    service.changeCategory(task, null).subscribe();
    const req = http.expectOne('/api/tasks/7/category');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ categoryName: null, version: 3 });
    req.flush({
      id: 7,
      title: 'Tarea',
      categoryId: null,
      categoryName: null,
      status: 'ACTIVA',
      priority: 'MEDIA',
      version: 4,
    });
  });

  it('loads the category catalog from /api/categories', () => {
    service.listCategories().subscribe((result) => {
      expect(result).toEqual([
        { id: 1, name: 'Trabajo' },
        { id: 2, name: 'Personal' },
      ]);
    });
    const req = http.expectOne('/api/categories');
    expect(req.request.method).toBe('GET');
    req.flush([
      { id: 1, name: 'Trabajo' },
      { id: 2, name: 'Personal' },
    ]);
  });

  it('searches visible tasks by title or description through the backend', () => {
    service.listTasks('informe mensual').subscribe((result) => expect(result).toEqual([]));

    const req = http.expectOne(
      (request) => request.url === '/api/tasks' && request.params.get('q') === 'informe mensual',
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('loads and searches the unified task history', () => {
    service.listHistory('reunión').subscribe((result) => expect(result).toEqual([]));

    const req = http.expectOne(
      (request) => request.url === '/api/tasks/history' && request.params.get('q') === 'reunión',
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
