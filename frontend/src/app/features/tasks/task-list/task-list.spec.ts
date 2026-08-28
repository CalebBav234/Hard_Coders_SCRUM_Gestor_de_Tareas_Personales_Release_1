import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Task } from '../../../core/models/task';
import { TaskList } from './task-list';

describe('TaskList editing and deletion', () => {
  let fixture: ComponentFixture<TaskList>;
  let http: HttpTestingController;
  let element: HTMLElement;
  const original: Task = {
    id: 7,
    title: 'Original',
    description: 'Descripción inicial',
    priority: 'MEDIA',
    status: 'ACTIVA',
    version: 2,
    createdAt: '2026-08-20T12:00:00Z',
    activatedAt: '2026-08-20T13:00:00Z',
    completedAt: null,
    totalActiveSeconds: 60,
  };

  function click(label: string): void {
    const button = Array.from(element.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === label,
    );
    expect(button).toBeTruthy();
    button!.click();
    fixture.detectChanges();
  }

  function input(selector: string, value: string): void {
    const control = element.querySelector(selector) as HTMLInputElement;
    control.value = value;
    control.dispatchEvent(new Event(control.tagName === 'SELECT' ? 'change' : 'input'));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskList],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskList);
    element = fixture.nativeElement;
    fixture.detectChanges();
    http.expectOne('/api/tasks').flush([original]);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('renders persisted tasks after the asynchronous response', () => {
    expect(element.querySelector('h3')?.textContent).toBe('Original');
    expect(element.textContent).toContain('Descripción inicial');
  });

  it('edits through HTTP and immediately renders the authoritative response', async () => {
    click('Editar');
    input('input', '  Corregida  ');
    input('textarea', 'Descripción enriquecida');
    input('select', 'ALTA');
    click('Guardar cambios');
    const request = http.expectOne('/api/tasks/7');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      title: 'Corregida',
      description: 'Descripción enriquecida',
      priority: 'ALTA',
      version: 2,
    });
    request.flush({ ...original, ...request.request.body, version: 3 });
    await fixture.whenStable();
    expect(element.querySelector('h3')?.textContent).toBe('Corregida');
    expect(element.querySelector('.priority')?.textContent).toContain('ALTA');
    expect(element.textContent).toContain('Descripción enriquecida');
    expect(element.querySelector('app-task-editor')).toBeNull();
    expect(fixture.componentInstance.tasks[0].activatedAt).toBe(original.activatedAt);
    expect(fixture.componentInstance.tasks[0].version).toBe(3);
  });

  it('cancels editing without mutating or sending changes', () => {
    click('Editar');
    input('input', 'No guardar');
    click('Cancelar edición');
    http.expectNone((request) => request.method === 'PUT');
    expect(element.querySelector('h3')?.textContent).toBe('Original');
    expect(fixture.componentInstance.tasks[0]).toEqual(original);
  });

  it('rejects blank titles without sending a request', () => {
    click('Editar');
    input('input', '   ');
    click('Guardar cambios');
    http.expectNone((request) => request.method === 'PUT');
    expect(element.textContent).toContain('no solo espacios');
  });

  it('keeps the draft on a version conflict and explains recovery', async () => {
    click('Editar');
    input('input', 'Mi borrador');
    click('Guardar cambios');
    http
      .expectOne('/api/tasks/7')
      .flush({ message: 'VERSION_CONFLICT' }, { status: 412, statusText: 'Conflict' });
    await fixture.whenStable();
    expect(element.textContent).toContain('La tarea cambió mientras la editabas');
    expect((element.querySelector('input') as HTMLInputElement).value).toBe('Mi borrador');
    expect(element.querySelector('h3')?.textContent).toBe('Original');
    expect((element.querySelector('input') as HTMLInputElement).disabled).toBe(false);
  });

  it('keeps the task when deletion is cancelled and never sends DELETE', () => {
    click('Eliminar');
    expect(element.querySelector('.delete-confirmation')?.textContent).toContain('Original');
    http.expectNone((request) => request.method === 'DELETE');
    click('Cancelar eliminación');
    http.expectNone((request) => request.method === 'DELETE');
    expect(element.querySelector('h3')?.textContent).toBe('Original');
    expect(element.querySelector('.delete-confirmation')).toBeNull();
  });

  it('deletes only after confirmation, blocks double submissions and updates the count', async () => {
    click('Eliminar');
    click('Confirmar eliminación');
    fixture.componentInstance.confirmDelete();
    const request = http.expectOne('/api/tasks/7?version=2');
    expect(request.request.method).toBe('DELETE');
    expect(element.querySelector('h3')?.textContent).toBe('Original');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();
    expect(element.querySelector('.task-card')).toBeNull();
    expect(element.querySelector('.task-count')?.textContent).toContain('0 tareas');
    expect(element.textContent).toContain('eliminada');
  });

  it('does not hide the task when the server rejects deletion', async () => {
    click('Eliminar');
    click('Confirmar eliminación');
    http
      .expectOne('/api/tasks/7?version=2')
      .flush(
        { message: 'Elimina primero las subtareas.' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();
    expect(element.querySelector('h3')?.textContent).toBe('Original');
    expect(element.textContent).toContain('Elimina primero las subtareas.');
    expect(fixture.componentInstance.busyTaskId).toBeNull();
  });
});
