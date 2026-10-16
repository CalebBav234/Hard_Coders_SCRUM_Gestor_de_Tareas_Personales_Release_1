import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AssignmentDto } from '../../../core/models/api';
import { UserSummary } from '../../../core/models/project';
import { AssignmentDialog } from './assignment-dialog';

describe('AssignmentDialog — US2.3 (asignación de Personal)', () => {
  let fixture: ComponentFixture<AssignmentDialog>;
  let http: HttpTestingController;
  let element: HTMLElement;

  const assignment: AssignmentDto = {
    taskId: 8,
    taskTitle: 'Levantar requisitos',
    workerUserId: 5,
    workerName: 'Ana Perez',
    assignedAt: '2026-10-02T10:00:00Z',
    assignedByUserId: 3,
    assignedByName: 'Julia Mendoza',
    reason: 'Carga de trabajo',
  };

  function buttons(scope: ParentNode = element): HTMLButtonElement[] {
    return Array.from(scope.querySelectorAll('button'));
  }

  function click(label: string, scope: ParentNode = element): void {
    const button = buttons(scope).find((b) => b.textContent?.includes(label));
    expect(button, `botón "${label}"`).toBeTruthy();
    button!.click();
    fixture.detectChanges();
  }

  /** Igual que en projects.spec.ts: valor por DOM + evento change/input. */
  function setValue(selector: string, value: string): void {
    const control = element.querySelector(selector) as HTMLInputElement | HTMLSelectElement;
    control.value = value;
    control.dispatchEvent(new Event(control.tagName === 'SELECT' ? 'change' : 'input'));
    fixture.detectChanges();
  }

  /** Asignación del trabajador del que se dispone en las pruebas. */
  function assignmentOf(overrides: Partial<AssignmentDto> = {}): AssignmentDto {
    return {
      taskId: 8,
      taskTitle: 'Levantar requisitos',
      workerUserId: 5,
      workerName: 'Ana Perez',
      assignedAt: '2026-10-02T10:00:00Z',
      assignedByUserId: 3,
      assignedByName: 'Julia Mendoza',
      reason: 'Carga de trabajo',
      ...overrides,
    };
  }

  /** Personal del proyecto con el que se alimentan las pruebas. */
  function workers(): UserSummary[] {
    return [
      { userId: 5, fullName: 'Ana Perez', email: 'a@x.com', chief: false, active: true },
      { userId: 6, fullName: 'Luis Luna', email: 'l@x.com', chief: false, active: true },
    ];
  }

  /** Las dos peticiones que el diálogo lanza al abrirse. */
  function initialLoad(assignments: AssignmentDto[] = [], available = workers()): void {
    http.expectOne('/api/projects/42/workers').flush(available);
    http.expectOne('/api/tasks/8/assignments').flush(assignments);
    fixture.detectChanges();
  }


  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [AssignmentDialog],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AssignmentDialog);
    element = fixture.nativeElement;
    fixture.componentInstance.taskId = 8;
    fixture.componentInstance.taskTitle = 'Levantar requisitos';
    fixture.componentInstance.projectId = 42;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
  });

  it('loads the workers of the project and the current assignments', () => {
    http.expectOne('/api/projects/42/workers').flush([
      { userId: 5, fullName: 'Ana Perez', email: 'a@x.com', chief: false, active: true },
      { userId: 6, fullName: 'Luis Luna', email: 'l@x.com', chief: false, active: true },
    ]);
    http.expectOne('/api/tasks/8/assignments').flush([assignment]);
    fixture.detectChanges();

    expect(element.textContent).toContain('Ana Perez');
    // El trabajador ya asignado no se ofrece de nuevo en el selector.
    const options = Array.from(element.querySelectorAll('#assignment-worker option')).map((o) =>
      o.textContent?.trim() ?? '',
    );
    expect(options.some((text) => text.includes('Luis Luna'))).toBe(true);
    expect(options.some((text) => text.includes('Ana Perez'))).toBe(false);
  });

  it('assigns the chosen worker with its reason', () => {
    http.expectOne('/api/projects/42/workers').flush([
      { userId: 6, fullName: 'Luis Luna', email: 'l@x.com', chief: false, active: true },
    ]);
    http.expectOne('/api/tasks/8/assignments').flush([]);
    fixture.detectChanges();

    setValue('#assignment-worker', '6');
    setValue('#assignment-reason', 'Experiencia');
    click('Asignar');

    const request = http.expectOne('/api/tasks/8/assignments');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ workerUserId: 6, reason: 'Experiencia' });
    request.flush({ ...assignment, workerUserId: 6, workerName: 'Luis Luna' });
    http.expectOne('/api/tasks/8/assignments').flush([{ ...assignment, workerUserId: 6, workerName: 'Luis Luna' }]);
    fixture.detectChanges();

    expect(fixture.componentInstance.success).toBe('Personal asignado.');
    expect(fixture.componentInstance.selectedWorkerId).toBeNull();
  });

  it('revokes an assignment and reloads the list', () => {
    http.expectOne('/api/projects/42/workers').flush([]);
    http.expectOne('/api/tasks/8/assignments').flush([assignment]);
    fixture.detectChanges();

    click('Retirar');

    const request = http.expectOne('/api/tasks/8/assignments/5');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne('/api/tasks/8/assignments').flush([]);
    fixture.detectChanges();

    expect(fixture.componentInstance.success).toContain('Ana Perez');
  });

  it('explains a duplicated assignment with the backend message', async () => {
    http.expectOne('/api/projects/42/workers').flush([
      { userId: 6, fullName: 'Luis Luna', email: 'l@x.com', chief: false, active: true },
    ]);
    http.expectOne('/api/tasks/8/assignments').flush([]);
    fixture.detectChanges();

    setValue('#assignment-worker', '6');
    click('Asignar');

    http
      .expectOne('/api/tasks/8/assignments')
      .flush(
        { error: 'VALIDATION_ERROR', message: 'Ese Personal ya está asignado a la tarea' },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('Ese Personal ya está asignado a la tarea');
    expect(fixture.componentInstance.saving).toBe(false);
  });

  it('refuses to assign work to oneself with the backend explanation (P5.4)', async () => {
    http.expectOne('/api/projects/42/workers').flush([
      { userId: 1, fullName: 'Julia Mendoza', email: 'j@x.com', chief: true, active: true },
    ]);
    http.expectOne('/api/tasks/8/assignments').flush([]);
    fixture.detectChanges();

    setValue('#assignment-worker', '1');
    click('Asignar');

    http
      .expectOne('/api/tasks/8/assignments')
      .flush(
        { error: 'FORBIDDEN', message: 'No puedes asignarte trabajo a ti mismo' },
        { status: 403, statusText: 'Forbidden' },
      );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('No puedes asignarte trabajo a ti mismo');
    expect(fixture.componentInstance.saving).toBe(false);
  });

  it('emits closed when dismissing the dialog', () => {
    http.expectOne('/api/projects/42/workers').flush([]);
    http.expectOne('/api/tasks/8/assignments').flush([]);
    const emitted: unknown[] = [];
    fixture.componentInstance.closed.subscribe((value) => emitted.push(value));
    click('×');

    expect(emitted.length).toBe(1);
  });

  it('leaves only the workers that are still free to assign', () => {
    const component = fixture.componentInstance;
    initialLoad([assignmentOf({ workerUserId: 6, workerName: 'Luis Luna' })]);

    expect(component.availableWorkers.map((worker) => worker.userId)).toEqual([5]);
    expect(element.textContent).toContain('Luis Luna');
    expect(element.textContent).toContain('Asignado por');
    expect(element.querySelector('#assignment-worker')).not.toBeNull();
  });

  it('warns that no active Personal is left to assign', () => {
    initialLoad([assignmentOf({ workerUserId: 5 }), assignmentOf({ workerUserId: 6 })]);

    expect(element.querySelector('#assignment-worker')).toBeNull();
    expect(element.textContent).toContain('No queda Personal activo disponible');
  });

  it('sends an empty reason as nothing at all', () => {
    const component = fixture.componentInstance;
    initialLoad();

    component.selectedWorkerId = 6;
    component.reason = '   ';
    component.assign();

    const request = http.expectOne('/api/tasks/8/assignments');
    expect(request.request.body).toEqual({ workerUserId: 6, reason: null });
    request.flush({});
    http.expectOne('/api/tasks/8/assignments').flush([]);
  });

  it('does not send anything until a worker is chosen', () => {
    const component = fixture.componentInstance;
    initialLoad();

    component.assign();
    http.expectNone('/api/tasks/8/assignments');
    expect(component.saving).toBe(false);
  });

  it('withdraws the assignment and says which worker was removed', async () => {
    const changed = vi.fn();
    const component = fixture.componentInstance;
    component.changed.subscribe(changed);
    initialLoad([assignmentOf({ workerName: 'Ana Perez' })]);
    await fixture.whenStable();
    fixture.detectChanges();

    click('Retirar');
    const request = http.expectOne('/api/tasks/8/assignments/5');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne('/api/tasks/8/assignments').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(changed).toHaveBeenCalled();
    expect(component.assignments).toEqual([]);
    expect(component.success).toBe('Asignación de Ana Perez retirada.');
    expect(element.textContent).toContain('Todavía no hay Personal asignado');
  });

  it('does not withdraw twice while the first withdrawal is in flight', () => {
    const component = fixture.componentInstance;
    initialLoad([assignmentOf()]);

    component.unassign(component.assignments[0]);
    component.unassign(component.assignments[0]);
    http.expectOne('/api/tasks/8/assignments/5');
  });

  it('explains that the Personal of the project could not be loaded', () => {
    http.expectOne('/api/projects/42/workers').flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/tasks/8/assignments').flush([]);
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('No se pudo cargar el Personal del proyecto.');
    expect(fixture.componentInstance.loading).toBe(false);
  });

  it('explains that the assignments of the task could not be loaded', () => {
    http.expectOne('/api/projects/42/workers').flush(workers());
    http.expectOne('/api/tasks/8/assignments').flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('No se pudieron cargar las asignaciones de la tarea.');
  });

  it('falls back to the explanation of each status when the backend sends none', () => {
    const component = fixture.componentInstance;
    initialLoad();

    setValue('#assignment-worker', '6');
    click('Asignar');
    http.expectOne('/api/tasks/8/assignments').flush({}, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();
    expect(component.error).toBe('No tienes permiso para asignar Personal en este proyecto.');

    setValue('#assignment-worker', '6');
    click('Asignar');
    http.expectOne('/api/tasks/8/assignments').flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(component.error).toBe('La tarea o el trabajador ya no están disponibles. Actualiza la lista.');

    setValue('#assignment-worker', '6');
    click('Asignar');
    http.expectOne('/api/tasks/8/assignments').flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(component.error).toBe('No se pudo asignar el Personal.');
    expect(component.saving).toBe(false);
  });

  it('explains why an assignment could not be withdrawn', () => {
    const component = fixture.componentInstance;
    initialLoad([assignmentOf()]);

    component.unassign(component.assignments[0]);
    http.expectOne('/api/tasks/8/assignments/5').flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(component.error).toBe('No se pudo retirar la asignación.');
    expect(component.saving).toBe(false);
  });

  it('does not close while it is still saving', () => {
    const closed = vi.fn();
    const component = fixture.componentInstance;
    component.closed.subscribe(closed);
    initialLoad();

    setValue('#assignment-worker', '6');
    click('Asignar');
    // Con una asignación en marcha la ventana no se cierra sin querer.
    component.close();
    expect(closed).not.toHaveBeenCalled();
    expect(element.textContent).toContain('Asignando...');

    http.expectOne('/api/tasks/8/assignments').flush(assignmentOf({ workerUserId: 6 }));
    http.expectOne('/api/tasks/8/assignments').flush([]);
    component.close();
    expect(closed).toHaveBeenCalled();
  });
});
