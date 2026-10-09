import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskHistoryDto } from '../../../core/models/api';
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

  function flushHistory(tasks: TaskHistoryDto[] = [], query = ''): void {
    http
      .expectOne((r) => r.url === '/api/tasks/history' && (r.params.get('q') ?? '') === query)
      .flush(tasks);
    http
      .expectOne((r) => r.url === '/api/subtasks/history' && (r.params.get('q') ?? '') === query)
      .flush([]);
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskHistory],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskHistory);
    element = fixture.nativeElement;
    fixture.detectChanges();
    // Títulos de las tareas principales (para mostrar a qué tarea pertenece una subtarea).
    http.expectOne('/api/tasks').flush([]);
  });

  afterEach(() => http.verify());

  it('replaces an in-flight load when a task changes instead of losing the refresh', async () => {
    const stale = http.expectOne('/api/tasks/history');
    http.expectOne('/api/subtasks/history');
    fixture.componentInstance.loadHistory();
    expect(stale.cancelled).toBe(true);
    flushHistory([archived]);
    await fixture.whenStable();
    expect(fixture.componentInstance.history.map((item) => item.title)).toEqual(['Preparar reunión']);
  });

  it('does not present a load error as an empty archive', async () => {
    http.expectOne('/api/subtasks/history');
    http.expectOne('/api/tasks/history').flush({}, { status: 500, statusText: 'Server Error' });
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
    http.expectOne('/api/subtasks/history');
    http.expectOne('/api/tasks/history').flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'No se pudo cargar el historial',
    );
  });

  it('explains when the backend does not expose the history endpoint', async () => {
    http.expectOne('/api/subtasks/history');
    http.expectOne('/api/tasks/history').flush({}, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Actualiza y reinicia el backend',
    );
    expect(element.querySelector('.empty-message')).toBeNull();
  });

  it('cancels the pending request when the component is destroyed', () => {
    const pending = http.expectOne('/api/tasks/history');
    const pendingSubtasks = http.expectOne('/api/subtasks/history');
    fixture.destroy();
    expect(pending.cancelled).toBe(true);
    expect(pendingSubtasks.cancelled).toBe(true);
  });
});
