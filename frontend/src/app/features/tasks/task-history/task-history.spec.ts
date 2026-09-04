import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskHistory as TaskHistoryModel } from '../../../core/models/task';
import { TaskHistory } from './task-history';

describe('TaskHistory', () => {
  let fixture: ComponentFixture<TaskHistory>;
  let http: HttpTestingController;
  let element: HTMLElement;

  const archived: TaskHistoryModel = {
    id: 12,
    title: 'Preparar reunión',
    description: 'Agenda del equipo',
    status: 'TERMINADA',
    priority: 'ALTA',
    categoryName: 'Trabajo',
    completedAt: '2026-08-31T18:00:00Z',
    effectiveActiveSeconds: 3661,
    version: 3,
    events: [
      {
        id: 2,
        fromStatus: 'ACTIVA',
        toStatus: 'TERMINADA',
        changedAt: '2026-08-31T18:00:00Z',
      },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskHistory],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskHistory);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('replaces an in-flight load when a task changes instead of losing the refresh', async () => {
    const stale = http.expectOne('/api/tasks/history');
    fixture.componentInstance.loadHistory();
    expect(stale.cancelled).toBe(true);
    http.expectOne('/api/tasks/history').flush([archived]);
    await fixture.whenStable();
    expect(fixture.componentInstance.history).toEqual([archived]);
  });

  it('does not present a load error as an empty archive', async () => {
    http.expectOne('/api/tasks/history').flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
    expect(element.querySelector('.empty-message')).toBeNull();
  });

  it('renders archived tasks and their state transitions', async () => {
    http.expectOne('/api/tasks/history').flush([archived]);
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

  it('sends the search term and shows a controlled empty result', async () => {
    http.expectOne('/api/tasks/history').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    const input = element.querySelector('#history-search') as HTMLInputElement;
    input.value = 'informe';
    input.dispatchEvent(new Event('input'));
    (element.querySelector('.btn-search') as HTMLButtonElement).click();
    fixture.detectChanges();

    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/tasks/history' && candidate.params.get('q') === 'informe',
    );
    request.flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.textContent).toContain('No se encontraron tareas anteriores');
  });

  it('shows a useful message when history cannot be loaded', async () => {
    http.expectOne('/api/tasks/history').flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'No se pudo cargar el historial',
    );
  });

  it('explains when the backend does not expose the history endpoint', async () => {
    http.expectOne('/api/tasks/history').flush({}, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Actualiza y reinicia el backend',
    );
    expect(element.querySelector('.empty-message')).toBeNull();
  });

  it('cancels the pending request when the component is destroyed', () => {
    const pending = http.expectOne('/api/tasks/history');
    fixture.destroy();
    expect(pending.cancelled).toBe(true);
  });
});
