import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SubtaskDto, TaskHistoryDto } from '../../../core/models/api';
import { TaskHistory as TaskHistoryModel } from '../../../core/models/task';
import { TaskHistory } from './task-history';

describe('TaskHistory', () => {
  let fixture: ComponentFixture<TaskHistory>;
  let http: HttpTestingController;
  let element: HTMLElement;

  const archived: TaskHistoryDto = {
    task: {
      taskId: 12,
      activityId: 2,
      activityName: 'Trabajo',
      title: 'Preparar reunión',
      description: 'Agenda del equipo',
      status: 'COMPLETADA',
      priority: 'ALTA',
      allocatedBudgetBob: 0,
      estimatedDurationMinutes: 0,
      actualDurationMinutes: 62,
      trackedSeconds: 3661,
      currentSegmentStartedAt: null,
      completedAt: '2026-08-31T18:00:00Z',
      createdAt: '2026-08-31T16:00:00Z',
      updatedAt: '2026-08-31T18:00:00Z',
      version: 3,
      assignedWorkerIds: [],
      subtasks: [],
    },
    cancelledAt: null,
    events: [
      {
        eventId: 2,
        previousStatus: 'EN_PROGRESO',
        newStatus: 'COMPLETADA',
        reason: 'Tarea completada',
        changedAt: '2026-08-31T18:00:00Z',
      },
    ],
  };

  /** Tarea archivada a la que el backend no dejó fechas. */
  const withoutDates: TaskHistoryModel = {
    id: 1,
    title: 'Sin fechas',
    description: null,
    status: 'TERMINADA',
    priority: 'BAJA',
    categoryId: null,
    categoryName: null,
    parentTaskId: null,
    completedAt: null,
    deletedAt: null,
    createdAt: null,
    updatedAt: null,
    version: 1,
    events: [],
  };

  function flushHistory(tasks: TaskHistoryDto[] = [], query = ''): void {    http
      .expectOne(
        (r) =>
          r.url === '/api/tasks/history' &&
          r.params.get('projectId') === '7' &&
          (r.params.get('q') ?? '') === query,
      )
      .flush(tasks);
    http
      .expectOne(
        (r) =>
          r.url === '/api/subtasks/history' &&
          r.params.get('projectId') === '7' &&
          (r.params.get('q') ?? '') === query,
      )
      .flush([]);
  }

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [TaskHistory],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskHistory);
    element = fixture.nativeElement;
    // Al entrar solo se piden los proyectos entre los que elegir.
    fixture.detectChanges();
    http
      .expectOne('/api/projects')
      .flush([{ projectId: 7, name: 'Proyecto del historial', description: null }]);
    fixture.detectChanges();
    // El histórico pertenece al proyecto que elige el usuario; nunca al "primero".
    fixture.componentInstance.selectProject(7);
    // Títulos de las tareas principales (para mostrar a qué tarea pertenece una subtarea).
    http.expectOne((r) => r.url === '/api/tasks' && r.params.get('projectId') === '7').flush([]);
  });

  afterEach(() => http.verify());

  it('replaces an in-flight load when a task changes instead of losing the refresh', async () => {
    const stale = http.expectOne((r) => r.url === '/api/tasks/history' && r.params.get('projectId') === '7');
    http.expectOne((r) => r.url === '/api/subtasks/history' && r.params.get('projectId') === '7');
    fixture.componentInstance.loadHistory();
    expect(stale.cancelled).toBe(true);
    flushHistory([archived]);
    await fixture.whenStable();
    expect(fixture.componentInstance.history.map((item) => item.title)).toEqual(['Preparar reunión']);
  });

  it('does not present a load error as an empty archive', async () => {
    http.expectOne((r) => r.url === '/api/subtasks/history' && r.params.get('projectId') === '7');
    http.expectOne((r) => r.url === '/api/tasks/history' && r.params.get('projectId') === '7').flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
    expect(element.querySelector('.empty-message')).toBeNull();
  });

  it('renders archived tasks and their state transitions', async () => {
    flushHistory([archived]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.history-card h3')?.textContent).toContain('Preparar reunión');
    expect(element.textContent).toContain('Terminada');
    expect(element.textContent).toContain('1 h 1 min');
    const details = element.querySelector('details') as HTMLDetailsElement;
    details.open = true;
    fixture.detectChanges();
    expect(element.textContent).toContain('ACTIVA → TERMINADA');
  });

  it('labels cancelled tasks as deleted', async () => {
    flushHistory([
      {
        ...archived,
        task: { ...archived.task, status: 'CANCELADA' },
        cancelledAt: '2026-09-01T10:00:00Z',
        events: [
          {
            eventId: 3,
            previousStatus: 'COMPLETADA',
            newStatus: 'CANCELADA',
            reason: 'Tarea eliminada',
            changedAt: '2026-09-01T10:00:00Z',
          },
          ...archived.events,
        ],
      },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.textContent).toContain('Eliminada');
  });

  it('sends the search term and shows a controlled empty result', async () => {
    flushHistory();
    await fixture.whenStable();
    fixture.detectChanges();

    const input = element.querySelector('#history-search') as HTMLInputElement;
    input.value = 'informe';
    input.dispatchEvent(new Event('input'));
    (element.querySelector('.btn-search') as HTMLButtonElement).click();
    fixture.detectChanges();

    flushHistory([], 'informe');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.textContent).toContain('No se encontraron tareas anteriores');
  });

  it('shows a useful message when history cannot be loaded', async () => {
    http.expectOne((r) => r.url === '/api/subtasks/history' && r.params.get('projectId') === '7');
    http.expectOne((r) => r.url === '/api/tasks/history' && r.params.get('projectId') === '7').flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'No se pudo cargar el historial',
    );
  });

  it('explains when the chosen project is not available anymore', async () => {
    http.expectOne((r) => r.url === '/api/subtasks/history' && r.params.get('projectId') === '7');
    http.expectOne((r) => r.url === '/api/tasks/history' && r.params.get('projectId') === '7').flush({}, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'No se encontró el proyecto',
    );
    expect(element.querySelector('.empty-message')).toBeNull();
  });

  it('cancels the pending request when the component is destroyed', () => {
    const pending = http.expectOne((r) => r.url === '/api/tasks/history' && r.params.get('projectId') === '7');
    const pendingSubtasks = http.expectOne((r) => r.url === '/api/subtasks/history' && r.params.get('projectId') === '7');
    fixture.destroy();
    expect(pending.cancelled).toBe(true);
    expect(pendingSubtasks.cancelled).toBe(true);
  });

  it('returns to the project picker and forgets everything it showed', () => {
    flushHistory([archived]);

    fixture.componentInstance.selectProject(null);

    expect(fixture.componentInstance.history).toEqual([]);
    expect(fixture.componentInstance.tasks).toEqual([]);
    expect(fixture.componentInstance.error).toBeNull();
    expect(fixture.componentInstance.activeSearch).toBe('');
    expect(fixture.componentInstance.searchDraft).toBe('');
    // Sin proyecto no se vuelve a pedir nada al backend.
    http.expectNone((r) => r.url === '/api/tasks/history');
    http.expectNone((r) => r.url === '/api/tasks');
    expect(element.textContent).toContain('Elige un proyecto');
  });

  it('shows the empty archive and the results of the search', async () => {
    flushHistory();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element.textContent).toContain('Todavía no hay tareas terminadas o eliminadas.');

    const input = element.querySelector('#history-search') as HTMLInputElement;
    input.value = 'informe';
    input.dispatchEvent(new Event('input'));
    expect(fixture.componentInstance.searchDraft).toBe('informe');
    (element.querySelector('.btn-search') as HTMLButtonElement).click();

    flushHistory([], 'informe');
    await fixture.whenStable();
    fixture.detectChanges();

    // El resumen de la búsqueda acompañado de su botón de limpiar.
    expect(element.querySelector('.search-summary')?.textContent).toContain('resultado(s) para');
    const clear = element.querySelector('.btn-clear') as HTMLButtonElement;
    expect(clear).not.toBeNull();
    clear.click();
    flushHistory();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element.querySelector('.btn-clear')).toBeNull();
    expect(element.textContent).toContain('Todavía no hay tareas terminadas');
  });

  it('ignores a search while the history is still loading', () => {
    http.expectOne((r) => r.url === '/api/subtasks/history' && r.params.get('projectId') === '7').flush([]);
    const stale = http.expectOne((r) => r.url === '/api/tasks/history' && r.params.get('projectId') === '7');
    expect(fixture.componentInstance.loading).toBe(true);
    fixture.componentInstance.searchHistory(new Event('submit'));
    fixture.componentInstance.clearSearch();
    // La petición en curso se mantiene: ni se cancela ni se lanza otra.
    expect(stale.cancelled).toBe(false);
  });

  it('explains that the titles of the tasks could not be loaded', () => {
    const archivedSubtask: TaskHistoryModel = { ...withoutDates, parentTaskId: 12 };
    http.expectOne((r) => r.url === '/api/subtasks/history' && r.params.get('projectId') === '7').flush([]);
    http.expectOne((r) => r.url === '/api/tasks/history' && r.params.get('projectId') === '7').flush([]);
    // El detalle de los títulos de las tareas principales es secundario: si falla,
    // la pantalla sigue funcionando y lo dice por consola.
    const spy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    fixture.componentInstance.loadTasks();
    http.expectOne((r) => r.url === '/api/tasks' && r.params.get('projectId') === '7').flush({}, { status: 500, statusText: 'Server Error' });

    expect(fixture.componentInstance.getParentTaskTitle(archivedSubtask)).toBe('Tarea principal no disponible');
    expect(fixture.componentInstance.tasks).toEqual([]);
    spy.mockRestore();
  });

  it('labels dates and durations that are missing or unusual', () => {
    const component = fixture.componentInstance;
    flushHistory();
    // Tarea archivada a la que el backend no dejó fechas.
    const entry = withoutDates;
    const child = { ...entry, id: -1, parentTaskId: 4 };

    expect(component.formatDate(null)).toBe('Fecha no disponible');
    expect(component.formatDate('lo que sea')).toBe('Fecha no disponible');
    expect(component.archiveDate(entry)).toBe('Fecha no disponible');
    expect(component.archiveDate({ ...entry, deletedAt: '2026-09-02T10:00:00Z' })).not.toBe('Fecha no disponible');
    expect(component.archiveLabel({ ...entry, deletedAt: null })).toBe('Terminada');
    expect(component.formatDuration()).toBe('0 min');
    expect(component.formatDuration(59)).toBe('59 s');
    expect(component.formatDuration(120)).toBe('2 min 0 s');
    expect(component.formatDuration(7255)).toBe('2 h 0 min');
    expect(component.eventLabel({ id: 1, fromStatus: null, toStatus: 'ACTIVA', changedAt: '' })).toBe(
      'Creada como ACTIVA',
    );
    // Una subtarea archivada sin su tarea en la lista tampoco rompe la pantalla.
    expect(component.isSubtask(child)).toBe(true);
    expect(component.isSubtask(entry)).toBe(false);
  });

  it('archives a subtask under the title of the task it belonged to', async () => {
    const subtask: SubtaskDto = {
      subtaskId: 21,
      taskId: 12,
      title: 'Pedir sala',
      description: 'Confirmar el salón',
      status: 'COMPLETADA',
      completedAt: '2026-09-01T09:00:00Z',
      createdAt: '2026-09-01T08:00:00Z',
      updatedAt: '2026-09-01T09:00:00Z',
      version: 1,
    };

    // La subtarea archivada llega del endpoint propio; el título de su tarea, de la lista.
    http.expectOne((r) => r.url === '/api/subtasks/history' && r.params.get('projectId') === '7').flush([subtask]);
    http.expectOne((r) => r.url === '/api/tasks/history' && r.params.get('projectId') === '7').flush([]);
    fixture.componentInstance.loadTasks();
    http
      .expectOne((r) => r.url === '/api/tasks' && r.params.get('projectId') === '7')
      .flush([{ ...archived.task, subtasks: [], assignedWorkerIds: [] }]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.history.map((item) => item.title)).toEqual(['Pedir sala']);
    expect(element.querySelector('.task-type-badge')?.textContent).toContain('SUBTAREA');
    expect(element.querySelector('.history-parent-task')?.textContent).toContain('Preparar reunión');
    // Una subtarea archivada no muestra prioridad ni categoría.
    expect(element.textContent).not.toContain('Prioridad:');
    expect(element.querySelector('details')?.textContent).toContain('Ver cambios de estado (0)');
  });
});

describe('TaskHistory sin proyecto elegido', () => {
  let fixture: ComponentFixture<TaskHistory>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [TaskHistory],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskHistory);
    // Al entrar a la pantalla todavía no hay proyecto: solo se carga la lista para elegir.
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
  });

  it('does not ask for the history until the user picks a project', () => {
    http.expectOne('/api/projects').flush([
      { projectId: 7, name: 'Proyecto del historial', description: null },
    ]);
    fixture.detectChanges();

    // Sin proyecto elegido nunca se llama al backend con un projectId inventado (el fallo
    // que rompía la pantalla al entrar: projectId 0 y un 404 mal interpretado).
    http.expectNone((r) => r.url === '/api/tasks/history');
    http.expectNone((r) => r.url === '/api/subtasks/history');
    http.expectNone((r) => r.url === '/api/tasks');
    expect(fixture.componentInstance.history).toEqual([]);
    expect(fixture.componentInstance.error).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Elige un proyecto');
  });

  it('tells the Personal that the history is consulted project by project', () => {
    // El listado de proyectos falla: no hay histórico que consultar.
    http.expectOne('/api/projects').flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.projects).toEqual([]);
    expect(fixture.componentInstance.hasProjects).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('No diriges ningún proyecto');
  });

  it('asks for the history of the project the user picks', () => {
    http.expectOne('/api/projects').flush([
      { projectId: 7, name: 'Proyecto del historial', description: null },
    ]);
    fixture.detectChanges();

    fixture.componentInstance.selectProject(7);

    http.expectOne((r) => r.url === '/api/tasks/history' && r.params.get('projectId') === '7').flush([]);
    http.expectOne((r) => r.url === '/api/subtasks/history' && r.params.get('projectId') === '7').flush([]);
    http.expectOne((r) => r.url === '/api/tasks' && r.params.get('projectId') === '7').flush([]);
    expect(fixture.componentInstance.selectedProjectId).toBe(7);
  });
});
