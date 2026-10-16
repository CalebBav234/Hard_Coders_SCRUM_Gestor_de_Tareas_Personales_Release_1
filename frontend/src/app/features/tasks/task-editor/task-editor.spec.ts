import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TaskEditor } from './task-editor';
import { Task, TaskKind, TaskPriority } from '../../../core/models/task';
import { ActivityDetailDto, TaskDto } from '../../../core/models/api';

describe('TaskEditor (US1.3 / US2.2 / US3.2)', () => {
  let fixture: ComponentFixture<TaskEditor>;
  let http: HttpTestingController;
  let element: HTMLElement;

  const taskDto = (overrides: Partial<TaskDto> = {}): TaskDto => ({
    taskId: 3,
    activityId: 11,
    activityName: 'Fase 1',
    title: 'Título original',
    description: 'Descripción original',
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
    ...overrides,
  });

  const activityDetail = (id: number, name: string): ActivityDetailDto => ({
    activityId: id,
    stageId: 5,
    name,
    description: null,
    status: 'PENDIENTE',
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 1,
  });

  function task(overrides: Partial<Task> = {}): Task {
    return {
      id: 3,
      kind: 'TASK' as TaskKind,
      entityId: 3,
      title: 'Título original',
      description: 'Descripción original',
      status: 'INACTIVA',
      priority: 'MEDIA' as TaskPriority,
      version: 1,
      ...overrides,
    };
  }

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [TaskEditor],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskEditor);
    element = fixture.nativeElement;
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
  });

  it('carga las categorías de la etapa al abrir un TASK y las muestra', () => {
    fixture.componentRef.setInput('task', task());
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();

    expect(fixture.componentInstance.loadingCategories).toBe(true);
    http.expectOne('/api/activities?stageId=5').flush([
      activityDetail(10, 'Sin categoría'),
      activityDetail(11, 'Fase 1'),
    ]);
    expect(fixture.componentInstance.categories).toEqual([{ id: 11, name: 'Fase 1' }]);
  });

  it('no carga categorías al editar una SUBTASK', () => {
    fixture.componentRef.setInput('task', task({ id: -7, kind: 'SUBTASK' as TaskKind, entityId: 7 }));
    fixture.detectChanges();

    expect(fixture.componentInstance.isSubtask).toBe(true);
    http.expectNone('/api/activities/*');
  });

  it('marca errores de validación y no envía nada con el título vacío', () => {
    fixture.componentRef.setInput('task', task());
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();
    http.expectOne('/api/activities?stageId=5').flush([activityDetail(11, 'Fase 1')]);
    fixture.detectChanges();

    fixture.componentInstance.form.controls.title.setValue('   ');
    fixture.componentInstance.form.markAllAsTouched();

    fixture.componentInstance.save();
    expect(fixture.componentInstance.saving).toBe(false);
    expect(fixture.componentInstance.form.invalid).toBe(true);
  });

  it('guarda sin cambiar categoría y emite `saved`', async () => {
    const emitted: Task[] = [];
    fixture.componentInstance.saved.subscribe((taskValue) => emitted.push(taskValue));
    fixture.componentRef.setInput('task', task({ categoryName: 'Fase 1' }));
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();
    http.expectOne('/api/activities?stageId=5').flush([activityDetail(11, 'Fase 1')]);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      title: 'Título corregido',
      description: 'Descripción corregida',
      priority: 'ALTA' as TaskPriority,
      categoryName: 'Fase 1',
    });
    fixture.componentInstance.save();

    const request = http.expectOne('/api/tasks/3');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(expect.objectContaining({
      title: 'Título corregido',
      priority: 'ALTA',
      version: 1,
    }));
    expect(request.request.body).not.toHaveProperty('categoryName');
    request.flush(taskDto({ title: 'Título corregido', priority: 'ALTA', activityName: 'Fase 1' }));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(emitted.length).toBe(1);
    expect(emitted[0].title).toBe('Título corregido');
    expect(fixture.componentInstance.saving).toBe(false);
  });

  it('cambia de actividad al cambiar categoría y emite `saved` con la categoría', async () => {
    const emitted: Task[] = [];
    fixture.componentInstance.saved.subscribe((taskValue) => emitted.push(taskValue));
    fixture.componentRef.setInput('task', task({ categoryName: 'Fase 1' }));
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();
    http.expectOne('/api/activities?stageId=5').flush([
      activityDetail(11, 'Fase 1'),
      activityDetail(12, 'Fase 2'),
    ]);
    fixture.detectChanges();

    fixture.componentInstance.form.get('categoryName')!.setValue('Fase 2');
    fixture.componentInstance.save();

    fixture.componentInstance.form.setValue({
      title: 'Título original',
      description: 'Descripción original',
      priority: 'MEDIA',
      categoryName: 'Fase 2',
    });
    const update = http.expectOne('/api/tasks/3');
    expect(update.request.method).toBe('PUT');
    update.flush(taskDto({ activityName: 'Fase 2' }));
    const change = http.expectOne('/api/tasks/3/activity');
    expect(change.request.method).toBe('PUT');
    expect(change.request.body).toEqual({ activityId: 12, version: 1 });
    change.flush(taskDto({ activityName: 'Fase 2' }));

    await fixture.whenStable();
    fixture.detectChanges();

    expect(emitted.length).toBe(1);
    expect(emitted[0].categoryName).toBe('Fase 2');
    expect(fixture.componentInstance.saving).toBe(false);
  });

  it('muestra el error 412 de concurrencia al guardar', async () => {
    fixture.componentRef.setInput('task', task());
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();
    http.expectOne('/api/activities?stageId=5').flush([activityDetail(11, 'Fase 1')]);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      title: 'Título',
      description: '',
      priority: 'MEDIA',
      categoryName: '',
    });
    fixture.componentInstance.save();
    http
      .expectOne('/api/tasks/3')
      .flush({}, { status: 412, statusText: 'Precondition Failed' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toContain('cambió mientras la editabas');
    expect(element.querySelector('.editor-error')?.textContent).toContain('cambió mientras la editabas');
  });

  it('muestra el error 404 de tarea desaparecida al guardar', async () => {
    fixture.componentRef.setInput('task', task());
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();
    http.expectOne('/api/activities?stageId=5').flush([]);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      title: 'Título', description: '', priority: 'BAJA', categoryName: '',
    });
    fixture.componentInstance.save();
    http.expectOne('/api/tasks/3').flush({}, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toContain('ya no está disponible');
  });

  it('usa el mensaje del backend en fallos genéricos', async () => {
    fixture.componentRef.setInput('task', task());
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();
    http.expectOne('/api/activities?stageId=5').flush([]);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      title: 'Título', description: '', priority: 'BAJA', categoryName: '',
    });
    fixture.componentInstance.save();
    http
      .expectOne('/api/tasks/3')
      .flush({ message: 'Problema inesperado' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('Problema inesperado');
  });

  it('usa un mensaje genérico cuando el backend no envía message', async () => {
    fixture.componentRef.setInput('task', task());
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();
    http.expectOne('/api/activities?stageId=5').flush([]);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      title: 'Título', description: '', priority: 'BAJA', categoryName: '',
    });
    fixture.componentInstance.save();
    http.expectOne('/api/tasks/3').flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('No se pudieron guardar los cambios. Inténtalo de nuevo.');
  });

  it('avisa cuando el cambio de categoría falla tras guardar la tarea', async () => {
    fixture.componentRef.setInput('task', task({ categoryName: 'Fase 1' }));
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();
    http.expectOne('/api/activities?stageId=5').flush([activityDetail(11, 'Fase 1'), activityDetail(12, 'Fase 2')]);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      title: 'Título', description: '', priority: 'MEDIA', categoryName: 'Fase 2',
    });
    fixture.componentInstance.save();
    http.expectOne('/api/tasks/3').flush(taskDto({ activityName: 'Fase 2' }));
    http.expectOne('/api/tasks/3/activity').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toContain('no se pudo actualizar la categoría');
  });

  it('carga el error de categorías cuando el backend falla', () => {
    fixture.componentRef.setInput('task', task());
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();

    http.expectOne('/api/activities?stageId=5').flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.loadingCategories).toBe(false);
    expect(fixture.componentInstance.categoryError).toBe('No se pudieron cargar las categorías.');
  });

  it('cancela y emite `cancelled` mientras no está guardando', () => {
    const emitted: unknown[] = [];
    fixture.componentInstance.cancelled.subscribe((value) => emitted.push(value));
    fixture.componentRef.setInput('task', task({ id: -7, kind: 'SUBTASK' as TaskKind, entityId: 7 }));
    fixture.detectChanges();

    fixture.componentInstance.cancel();

    expect(emitted.length).toBe(1);
  });

  it('no emite `cancelled` mientras se está guardando', () => {
    const emitted: unknown[] = [];
    fixture.componentInstance.cancelled.subscribe((value) => emitted.push(value));
    fixture.componentRef.setInput('task', task());
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();
    http.expectOne('/api/activities?stageId=5').flush([]);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      title: 'Título', description: '', priority: 'ALTA', categoryName: '',
    });
    fixture.componentInstance.save();
    expect(fixture.componentInstance.saving).toBe(true);

    fixture.componentInstance.cancel();
    expect(emitted.length).toBe(0);

    http.expectOne('/api/tasks/3').flush({ ...task(), title: 'Nuevo' } as Task);
    fixture.detectChanges();
    expect(fixture.componentInstance.saving).toBe(false);
  });

  it('no dispara una segunda solicitud mientras se está guardando', () => {
    fixture.componentRef.setInput('task', task());
    fixture.componentInstance.stageIdForCategories = 5;
    fixture.detectChanges();
    http.expectOne('/api/activities?stageId=5').flush([]);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      title: 'Título', description: '', priority: 'ALTA', categoryName: '',
    });
    fixture.componentInstance.save();
    const request = http.expectOne('/api/tasks/3');
    expect(request.request.method).toBe('PUT');
    fixture.componentInstance.save();
    fixture.componentInstance.cancel();

    expect(fixture.componentInstance.saving).toBe(true);
    request.flush({ ...task(), title: 'Nuevo' } as Task);
    fixture.detectChanges();

    expect(fixture.componentInstance.saving).toBe(false);
  });

  it('guarda una SUBTASK con PUT a /api/subtasks', async () => {
    const emitted: Task[] = [];
    fixture.componentInstance.saved.subscribe((taskValue) => emitted.push(taskValue));
    const subtask = task({ id: -7, kind: 'SUBTASK' as TaskKind, entityId: 7, categoryName: null });
    fixture.componentRef.setInput('task', subtask);
    fixture.detectChanges();

    fixture.componentInstance.form.setValue({
      title: 'Subtarea editada', description: '', priority: 'BAJA', categoryName: '',
    });
    fixture.componentInstance.save();
    const request = http.expectOne('/api/subtasks/7');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(expect.objectContaining({ title: 'Subtarea editada' }));
    request.flush({ ...subtask, title: 'Subtarea editada', priority: 'BAJA' } as Task);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(emitted.length).toBe(1);
    expect(emitted[0].title).toBe('Subtarea editada');
    expect(fixture.componentInstance.saving).toBe(false);
  });
});
