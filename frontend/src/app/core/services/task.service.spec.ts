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
});
