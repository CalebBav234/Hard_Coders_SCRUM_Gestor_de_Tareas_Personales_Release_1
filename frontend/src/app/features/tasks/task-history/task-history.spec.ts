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
});
