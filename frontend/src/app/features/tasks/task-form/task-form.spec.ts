import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskDto, SubtaskDto } from '../../../core/models/api';
import { Task } from '../../../core/models/task';
import { TaskForm } from './task-form';

describe('TaskForm', () => {
  let fixture: ComponentFixture<TaskForm>;
  let http: HttpTestingController;
  let element: HTMLElement;

  const taskDto: TaskDto = {
    taskId: 11,
    activityId: 3,
    activityName: 'Cimentación',
    title: 'Nueva tarea',
    description: null,
    status: 'PENDIENTE',
    priority: 'MEDIA',
    allocatedBudgetBob: 0,
    estimatedDurationMinutes: 0,
    actualDurationMinutes: null,
    trackedSeconds: 0,
    currentSegmentStartedAt: null,
    completedAt: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 1,
    assignedWorkerIds: [],
    subtasks: [],
  };

  const subtaskDto: SubtaskDto = {
    subtaskId: 12,
    taskId: 11,
    title: 'Subtarea vinculada',
    description: null,
    status: 'PENDIENTE',
    completedAt: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 1,
  };

  function setValue(selector: string, value: string): void {
    const control = element.querySelector(selector) as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;
    control.value = value;
    control.dispatchEvent(new Event(control.tagName === 'SELECT' ? 'change' : 'input'));
    fixture.detectChanges();
  }

  function buttons(scope: ParentNode = element): HTMLButtonElement[] {
    return Array.from(scope.querySelectorAll('button'));
  }

  function click(label: string, scope: ParentNode = element): void {
    const button = buttons(scope).find((b) => b.textContent?.includes(label));
    expect(button, `botón "${label}"`).toBeTruthy();
    button!.click();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [TaskForm],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskForm);
    element = fixture.nativeElement;
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
  });

  it('renders the task creation form with the activity name', () => {
    fixture.componentRef.setInput('activityId', 3);
    fixture.componentRef.setInput('activityName', 'Cimentación');
    fixture.detectChanges();

    expect(element.textContent).toContain('Crear nueva tarea');
    expect(element.textContent).toContain('Cimentación');
    expect(element.querySelector('#priority')).not.toBeNull();
    expect(element.querySelector('#category')).not.toBeNull();
    expect(element.querySelector('button[type="button"]')?.textContent).toContain('Crear tarea');
    expect(element.querySelector('button[type="button"]')?.hasAttribute('disabled')).toBe(false);
  });

  it('renders the subtask creation form without priority or category fields', () => {
    fixture.componentRef.setInput('parentTaskId', 11);
    fixture.detectChanges();

    expect(element.textContent).toContain('Crear nueva subtarea');
    expect(element.querySelector('#priority')).toBeNull();
    expect(element.querySelector('#category')).toBeNull();
    expect(element.querySelector('button[type="button"]')?.textContent).toContain('Crear subtarea');
    expect(buttons().some((b) => b.textContent?.includes('Cancelar'))).toBe(true);
  });

  it('requires a title before sending anything', () => {
    fixture.componentRef.setInput('activityId', 3);
    fixture.detectChanges();

    setValue('#title', '  ');
    click('Crear tarea');

    expect(fixture.componentInstance.submitted).toBe(true);
    expect(element.querySelector('.error-message')?.textContent).toContain('El título es obligatorio');
    http.expectNone((req) => req.method === 'POST');
    expect(fixture.componentInstance.loading).toBe(false);
  });

  it('warns when there is no activity chosen and it is not a subtask', () => {
    fixture.detectChanges();

    setValue('#title', 'Nueva tarea');
    click('Crear tarea');

    expect(fixture.componentInstance.loading).toBe(false);
    expect(fixture.componentInstance.error).toBe('Elige una etapa y su actividad en la barra de trabajo antes de crear la tarea.');
    http.expectNone((req) => req.method === 'POST');
  });

  it('creates a task in the chosen activity and emits it', () => {
    const emitted: Task[] = [];
    fixture.componentInstance.taskCreated.subscribe((task) => emitted.push(task));
    fixture.componentRef.setInput('activityId', 3);
    fixture.detectChanges();

    setValue('#title', '  Tarea editada  ');
    setValue('#priority', 'ALTA');
    click('Crear tarea');

    const request = http.expectOne('/api/tasks');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      activityId: 3,
      title: 'Tarea editada',
      priority: 'ALTA',
      allocatedBudgetBob: 0,
      estimatedDurationMinutes: 0,
    });
    request.flush({ ...taskDto, title: 'Tarea editada', priority: 'ALTA' });
    fixture.detectChanges();

    expect(emitted.length).toBe(1);
    expect(emitted[0].title).toBe('Tarea editada');
    expect(emitted[0].priority).toBe('ALTA');
    expect(fixture.componentInstance.submitted).toBe(false);
    expect(fixture.componentInstance.loading).toBe(false);
    expect(fixture.componentInstance.title).toBe('');
    expect(element.querySelector('button[type="button"]')?.textContent).toContain('Crear tarea');
  });

  // ---------------------------------------------------------------- EC3-04 / R2
  function createTaskWith(hours: string, budget: string): void {
    fixture.componentRef.setInput('activityId', 3);
    fixture.detectChanges();
    setValue('#title', 'Vaciado de losa');
    setValue('#estimatedHours', hours);
    setValue('#allocatedBudgetBob', budget);
    click('Crear tarea');
  }

  it('EC3-04: sends the duration in minutes and the budget in their own fields', () => {
    createTaskWith('1.5', '250.5');

    const request = http.expectOne('/api/tasks');
    expect(request.request.body).toEqual({
      activityId: 3,
      title: 'Vaciado de losa',
      priority: 'MEDIA',
      allocatedBudgetBob: 250.5,
      estimatedDurationMinutes: 90,
    });
    request.flush({ ...taskDto, estimatedDurationMinutes: 90, allocatedBudgetBob: 250.5 });
    fixture.detectChanges();

    expect(fixture.componentInstance.estimatedHours).toBeNull();
    expect(fixture.componentInstance.allocatedBudgetBob).toBeNull();
  });

  it('EC3-04: shows the equivalent working time while typing the hours', () => {
    fixture.componentRef.setInput('activityId', 3);
    fixture.detectChanges();
    setValue('#estimatedHours', '2.25');

    expect(element.querySelector('.gt-field-hint')?.textContent).toContain('2 h 15 min');
  });

  it.each([
    ['0', 0],
    ['0.25', 15],
    ['8', 480],
  ])('BVA3 duration: %s h is accepted and sent as %i min', (hours: string, minutes: number) => {
    createTaskWith(hours, '0');

    const request = http.expectOne('/api/tasks');
    expect(request.request.body.estimatedDurationMinutes).toBe(minutes);
    request.flush(taskDto);
  });

  it('BVA3 duration: -0.25 h is rejected without calling the backend', () => {
    createTaskWith('-0.25', '0');

    expect(fixture.componentInstance.durationError).toBe('La duración debe ser un número mayor o igual a 0.');
    expect(element.textContent).toContain('La duración debe ser un número mayor o igual a 0.');
    http.expectNone('/api/tasks');
  });

  it.each([
    ['0.1', 6],
    ['0.3', 18],
  ])('EC3-04: duration %s h (no multiple of 0.25 h) is rejected without calling the backend', (hours: string) => {
    createTaskWith(hours, '0');

    expect(fixture.componentInstance.durationError).toBe('La duración debe usar incrementos de 0.25 h (15 min).');
    expect(element.textContent).toContain('La duración debe usar incrementos de 0.25 h (15 min).');
    http.expectNone('/api/tasks');
  });

  it('rejects an absurdly large duration that does not fit the integer column', () => {
    createTaskWith('99999999', '0');

    expect(fixture.componentInstance.durationError).toBe('La duración estimada es demasiado grande.');
    http.expectNone('/api/tasks');
  });

  it.each([
    ['0', 0],
    ['0.01', 0.01],
    ['9999999999999.99', 9999999999999.99],
  ])('BVA3 budget: %s Bs is accepted', (budget: string, expected: number) => {
    createTaskWith('1', budget);

    const request = http.expectOne('/api/tasks');
    expect(request.request.body.allocatedBudgetBob).toBe(expected);
    request.flush(taskDto);
  });

  it.each([
    ['-0.01', 'El presupuesto debe ser mayor o igual a 0.'],
    ['10.001', 'El presupuesto admite hasta 13 dígitos enteros y 2 decimales.'],
    ['10000000000000', 'El presupuesto admite hasta 13 dígitos enteros y 2 decimales.'],
  ])('BVA3/EP budget: %s Bs is rejected without calling the backend', (budget: string, message: string) => {
    createTaskWith('1', budget);

    expect(fixture.componentInstance.budgetError).toBe(message);
    expect(element.textContent).toContain(message);
    http.expectNone('/api/tasks');
  });

  it('sends 0 for empty duration and budget fields', () => {
    fixture.componentRef.setInput('activityId', 3);
    fixture.detectChanges();
    setValue('#title', 'Sin plan');
    setValue('#estimatedHours', '');
    setValue('#allocatedBudgetBob', '');
    click('Crear tarea');

    const request = http.expectOne('/api/tasks');
    expect(request.request.body.estimatedDurationMinutes).toBe(0);
    expect(request.request.body.allocatedBudgetBob).toBe(0);
    request.flush(taskDto);
  });

  it('subtasks do not show nor send duration and budget fields', () => {
    fixture.componentRef.setInput('parentTaskId', 11);
    fixture.detectChanges();

    expect(element.querySelector('#estimatedHours')).toBeNull();
    expect(element.querySelector('#allocatedBudgetBob')).toBeNull();
  });

  it('creates a subtask under the given parent task', () => {
    const emitted: Task[] = [];
    fixture.componentInstance.taskCreated.subscribe((task) => emitted.push(task));
    fixture.componentRef.setInput('parentTaskId', 11);
    fixture.detectChanges();

    setValue('#title', 'Subtarea vinculada');
    click('Crear subtarea');

    const request = http.expectOne('/api/tasks/11/subtasks');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ title: 'Subtarea vinculada', description: null });
    request.flush(subtaskDto);
    fixture.detectChanges();

    expect(emitted.length).toBe(1);
    expect(emitted[0].title).toBe('Subtarea vinculada');
    expect(emitted[0].parentTaskId).toBe(11);
    expect(fixture.componentInstance.loading).toBe(false);
  });

  it('keeps the written title and shows the backend error when creation fails', () => {
    fixture.componentRef.setInput('activityId', 3);
    fixture.detectChanges();

    setValue('#title', 'Error al crear');
    click('Crear tarea');

    http
      .expectOne('/api/tasks')
      .flush({ error: 'VALIDATION_ERROR', message: 'El título ya existe en la actividad' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(fixture.componentInstance.loading).toBe(false);
    expect(fixture.componentInstance.error).toBe('El título ya existe en la actividad');
    expect(fixture.componentInstance.title).toBe('Error al crear');
    expect(element.querySelector('.error-message')?.textContent).toContain('El título ya existe en la actividad');
  });

  it('falls back to a generic error message when the backend sends nothing useful', () => {
    fixture.componentRef.setInput('activityId', 3);
    fixture.detectChanges();

    setValue('#title', 'Algo falla');
    click('Crear tarea');

    http.expectOne('/api/tasks').flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.loading).toBe(false);
    expect(fixture.componentInstance.error).toBe('No se pudo crear la tarea.');
  });

  it('shows a saving indicator while creating and disables the form', () => {
    fixture.componentRef.setInput('activityId', 3);
    fixture.detectChanges();

    setValue('#title', 'Tarea en progreso');
    click('Crear tarea');

    expect(fixture.componentInstance.loading).toBe(true);
    expect(element.querySelector('button[type="button"]')?.textContent).toContain('Creando...');
    expect(element.querySelector('button[type="button"]')?.hasAttribute('disabled')).toBe(true);
    expect(element.querySelector('#title')?.hasAttribute('disabled')).not.toBeNull();

    http.expectOne('/api/tasks').flush(taskDto);
    fixture.detectChanges();

    expect(fixture.componentInstance.loading).toBe(false);
  });

  it('emits cancelled when the cancel button is pressed', () => {
    const emitted: unknown[] = [];
    fixture.componentInstance.cancelled.subscribe((value) => emitted.push(value));
    fixture.componentRef.setInput('parentTaskId', 11);
    fixture.detectChanges();

    click('Cancelar');

    expect(emitted.length).toBe(1);
  });

  it('shows the activity placeholder when no activity name is provided', () => {
    fixture.componentRef.setInput('activityId', null);
    fixture.componentRef.setInput('activityName', null);
    fixture.detectChanges();

    expect(element.querySelector('.task-form-activity')?.textContent).toContain('Elige una etapa y su actividad');
    expect(element.querySelector('.task-form-activity')?.classList.contains('empty')).toBe(true);
  });

  it('disables the form while creating so no second request is fired', () => {
    fixture.componentRef.setInput('activityId', 3);
    fixture.detectChanges();

    setValue('#title', 'Tarea doble');
    click('Crear tarea');

    // Mientras carga, el botón está deshabilitado y no se dispara otra petición.
    expect(fixture.componentInstance.loading).toBe(true);
    const request = http.expectOne('/api/tasks');
    expect((element.querySelector('button[type="button"]') as HTMLButtonElement).disabled).toBe(true);

    request.flush(taskDto);
    fixture.detectChanges();

    expect(fixture.componentInstance.loading).toBe(false);
  });
});

