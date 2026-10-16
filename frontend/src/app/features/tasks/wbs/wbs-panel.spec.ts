import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivityDetailDto, StageDto } from '../../../core/models/api';
import { WbsPanel } from './wbs-panel';

describe('WbsPanel — US2.2 (etapas y actividades)', () => {
  let fixture: ComponentFixture<WbsPanel>;
  let http: HttpTestingController;
  let element: HTMLElement;

  const stage: StageDto = {
    stageId: 4,
    projectId: 42,
    name: 'Diseno',
    description: 'Fase de diseno',
    status: 'PENDIENTE',
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 11,
  };

  const activity: ActivityDetailDto = {
    activityId: 6,
    stageId: 4,
    name: 'Analisis',
    description: null,
    status: 'PENDIENTE',
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 21,
  };

  function buttons(scope: ParentNode = element): HTMLButtonElement[] {
    return Array.from(scope.querySelectorAll('button'));
  }

  /** Hace clic en un botón: por el texto que muestra o por selector CSS. */
  function click(label: string, scope: ParentNode = element): void {
    const bySelector = label.startsWith('.') || label.startsWith('#');
    const button = bySelector
      ? scope.querySelector<HTMLButtonElement>(label)
      : buttons(scope).find((b) => b.textContent?.includes(label));
    expect(button, `botón "${label}"`).toBeTruthy();
    button!.click();
    fixture.detectChanges();
  }

  /** Valor por DOM más el evento que Angular necesita en un campo de formulario. */
  function setValue(selector: string, value: string, scope: ParentNode = element): void {
    const control = scope.querySelector(selector) as HTMLInputElement;
    control.value = value;
    control.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  /** La actividad desplegada de la etapa de pruebas. */
  function activityItem(): HTMLElement {
    const item = element.querySelector('.wbs-activity') as HTMLElement | null;
    expect(item, 'actividad de la etapa').toBeTruthy();
    return item!;
  }

  function flushStages(stages: StageDto[] = [stage]): void {
    http.expectOne('/api/projects/42/stages').flush(stages);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [WbsPanel],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(WbsPanel);
    element = fixture.nativeElement;
    fixture.componentInstance.projectId = 42;
    fixture.detectChanges();
    flushStages();
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
  });

  it('shows the stages of the project with their status', () => {
    expect(element.textContent).toContain('Diseno');
    expect(element.textContent).toContain('Pendiente');
  });

  it('creates a stage in the selected project', () => {
    click('+ Nueva etapa');
    const input = element.querySelector('#wbs-new-stage-name') as HTMLInputElement;
    input.value = 'Construccion';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    click('Crear etapa');

    const request = http.expectOne('/api/projects/42/stages');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Construccion', description: null });
    request.flush({ ...stage, stageId: 9, name: 'Construccion' });
    flushStages([stage, { ...stage, stageId: 9, name: 'Construccion' }]);

    expect(fixture.componentInstance.success).toContain('Construccion');
  });

  it('keeps the panel usable when the server rejects a new stage', async () => {
    click('+ Nueva etapa');
    const input = element.querySelector('#wbs-new-stage-name') as HTMLInputElement;
    input.value = 'X';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    click('Crear etapa');

    http
      .expectOne('/api/projects/42/stages')
      .flush({ error: 'VALIDATION_ERROR', message: 'El nombre es obligatorio' }, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('El nombre es obligatorio');
    expect(fixture.componentInstance.busyId).toBeNull();
  });

  it('loads the activities of a stage and creates a new activity there', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();
    expect(element.textContent).toContain('Analisis');

    const input = element.querySelector('#wbs-new-activity-name-4') as HTMLInputElement;
    input.value = 'Cimentacion';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    click('Crear actividad');

    const request = http.expectOne('/api/stages/4/activities');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Cimentacion', description: null });
    request.flush({ ...activity, activityId: 8, name: 'Cimentacion' });
    http.expectOne('/api/stages/4/activities').flush([activity, { ...activity, activityId: 8, name: 'Cimentacion' }]);
    fixture.detectChanges();

    expect(fixture.componentInstance.activities.map((a) => a.name)).toEqual(['Analisis', 'Cimentacion']);
  });

  it('emits structureChanged when an activity is created', () => {
    const emitted: unknown[] = [];
    fixture.componentInstance.structureChanged.subscribe((value) => emitted.push(value));
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([]);
    fixture.detectChanges();

    const input = element.querySelector('#wbs-new-activity-name-4') as HTMLInputElement;
    input.value = 'Cimentacion';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    click('Crear actividad');
    http.expectOne('/api/stages/4/activities').flush({ ...activity, name: 'Cimentacion' });
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();

    expect(emitted.length).toBe(1);
  });

  it('edits a stage sending the displayed version', () => {
    click('✎');
    const input = element.querySelector('#wbs-stage-name-4') as HTMLInputElement;
    input.value = 'Diseno grafico';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    click('Guardar');

    const request = http.expectOne('/api/stages/4');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Diseno grafico', description: 'Fase de diseno', version: 11 });
    request.flush({ ...stage, name: 'Diseno grafico' });
    fixture.detectChanges();

    expect(fixture.componentInstance.stages[0].name).toBe('Diseno grafico');
    expect(fixture.componentInstance.editingStageId).toBeNull();
  });

  it('changes the status of a stage with the version token', () => {
    click('Iniciar');

    const request = http.expectOne('/api/stages/4/status');
    expect(request.request.body).toEqual({ status: 'EN_PROGRESO', version: 11 });
    request.flush({ ...stage, status: 'EN_PROGRESO' });
    fixture.detectChanges();

    expect(fixture.componentInstance.stages[0].status).toBe('EN_PROGRESO');
    expect(fixture.componentInstance.success).toContain('En progreso');
  });

  it('offers only valid transitions from the current status', () => {
    expect(fixture.componentInstance.nextStatuses('PENDIENTE')).toEqual(['EN_PROGRESO', 'CANCELADA']);
    expect(fixture.componentInstance.nextStatuses('EN_PROGRESO')).toEqual(['PENDIENTE', 'COMPLETADA', 'CANCELADA']);
    expect(fixture.componentInstance.nextStatuses('COMPLETADA')).toEqual(['EN_PROGRESO', 'CANCELADA']);
    expect(fixture.componentInstance.nextStatuses('CANCELADA')).toEqual([]);
  });

  it('clears the activities when the newly expanded stage fails to load', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();
    expect(element.textContent).toContain('Analisis');

    // Una segunda etapa entra en la lista y su carga falla: no se pueden quedar a la vista
    // las actividades de la etapa anterior bajo el título de la nueva.
    fixture.componentInstance.stages = [stage, { ...stage, stageId: 9, name: 'Segunda etapa' }];
    fixture.componentInstance['changeDetector'].markForCheck();
    fixture.detectChanges();
    click('Segunda etapa');
    http
      .expectOne('/api/stages/9/activities')
      .flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.activities).toEqual([]);
    expect(element.textContent).toContain('No se pudieron cargar las actividades');
    expect(element.textContent).not.toContain('Analisis');
  });

  it('ignores stage switching while a mutation is in flight', () => {
    click('+ Nueva etapa');
    const input = element.querySelector('#wbs-new-stage-name') as HTMLInputElement;
    input.value = 'Otra etapa';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    click('Crear etapa');
    expect(fixture.componentInstance.busyId).toBe('new-stage');

    // El cambio de etapa queda bloqueado hasta que termine la creación.
    click('Diseno');
    http.expectNone((r) => r.url === '/api/stages/4/activities');
    expect(fixture.componentInstance.expandedStageId).toBeNull();

    http.expectOne('/api/projects/42/stages').flush({ ...stage, stageId: 9, name: 'Otra etapa' });
    flushStages([stage, { ...stage, stageId: 9, name: 'Otra etapa' }]);
    expect(fixture.componentInstance.busyId).toBeNull();
  });

  it('hides the edit actions when the actor cannot manage the hierarchy', () => {
    // setInput es la vía oficial: un @Input asignado a mano no reaviva los bloques @if.
    fixture.componentRef.setInput('canManage', false);
    fixture.detectChanges();
    expect(element.textContent).not.toContain('+ Nueva etapa');
    expect(buttons().some((b) => b.textContent?.includes('Iniciar'))).toBe(false);
    expect(element.querySelector('.wbs-stage-head')).not.toBeNull();
  });

  it('warns while the stages load and when the project has none', () => {
    const component = fixture.componentInstance;
    component.ngOnInit();
    component['changeDetector'].markForCheck();
    fixture.detectChanges();
    expect(component.loadingStages).toBe(true);
    expect(element.textContent).toContain('Cargando etapas...');

    http.expectOne('/api/projects/42/stages').flush([]);
    fixture.detectChanges();

    expect(element.textContent).toContain('Este proyecto todavía no tiene etapas');
    expect(element.textContent).toContain('Crea la primera etapa');
  });

  it('reports that the stages of the project could not be loaded', () => {
    fixture.componentInstance.ngOnInit();
    http
      .expectOne('/api/projects/42/stages')
      .flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('No se pudieron cargar las etapas del proyecto.');
    expect(fixture.componentInstance.loadingStages).toBe(false);
  });

  it('clears the form of a new stage when it is cancelled', () => {
    click('+ Nueva etapa');
    setValue('#wbs-new-stage-name', 'Construccion');
    setValue('#wbs-new-stage-description', 'Obra gruesa');

    click('Cancelar');

    expect(fixture.componentInstance.newStageOpen).toBe(false);
    expect(fixture.componentInstance.newStageName).toBe('');
    expect(fixture.componentInstance.newStageDescription).toBe('');
    expect(element.querySelector('#wbs-new-stage-name')).toBeNull();
  });

  it('does not create a stage without a name', () => {
    click('+ Nueva etapa');
    setValue('#wbs-new-stage-name', '   ');

    fixture.componentInstance.createStage();
    http.expectNone((request) => request.method === 'POST');
    expect(fixture.componentInstance.busyId).toBeNull();
  });

  it('shows while the activities of a stage load and warns when it has none', () => {
    click('Diseno');
    expect(fixture.componentInstance.loadingActivities).toBe(true);
    expect(element.textContent).toContain('Cargando actividades...');

    http.expectOne('/api/stages/4/activities').flush([]);
    fixture.detectChanges();

    expect(element.textContent).toContain('La etapa todavía no tiene actividades');
    expect(fixture.componentInstance.loadingActivities).toBe(false);
  });

  it('collapses the expanded stage and forgets its activities', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();
    expect(element.textContent).toContain('Analisis');
    // La pestaña de la etapa desplegada muestra cuántas actividades tiene.
    expect(element.querySelector('.wbs-count')?.textContent).toContain('1');

    click('Diseno');

    expect(fixture.componentInstance.expandedStageId).toBeNull();
    expect(fixture.componentInstance.activities).toEqual([]);
    expect(element.textContent).not.toContain('Analisis');
  });

  it('ignores the activities that arrive after another stage was expanded', () => {
    click('Diseno');
    // El usuario ya pasó a otra etapa: la respuesta de la anterior no debe pintarse.
    fixture.componentInstance.expandedStageId = 9;
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();

    expect(fixture.componentInstance.activities).toEqual([]);
    expect(element.querySelector('.wbs-activity')).toBeNull();
  });

  it('keeps the empty list when the activities of another stage fail to load', () => {
    click('Diseno');
    fixture.componentInstance.expandedStageId = 9;
    http.expectOne('/api/stages/4/activities').flush(null, { status: 500, statusText: 'Server Error' });

    expect(fixture.componentInstance.activities).toEqual([]);
    expect(fixture.componentInstance.error).toBeNull();
  });

  it('drops the expanded stage when it disappears from the list', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();

    // La etapa desplegada deja de existir: se cierra y se vacían sus actividades.
    fixture.componentInstance.ngOnInit();
    http.expectOne('/api/projects/42/stages').flush([{ ...stage, stageId: 10, name: 'Otra etapa' }]);
    fixture.detectChanges();

    expect(fixture.componentInstance.expandedStageId).toBeNull();
    expect(fixture.componentInstance.activities).toEqual([]);
    expect(element.textContent).toContain('Otra etapa');
  });

  it('forgets the edit of a stage when it is cancelled', () => {
    click('✎');
    expect(fixture.componentInstance.editingStageId).toBe(4);

    click('Cancelar', element.querySelector('.wbs-edit')!);

    expect(fixture.componentInstance.editingStageId).toBeNull();
  });

  it('does not save a stage without a name', () => {
    click('✎');
    setValue('#wbs-stage-name-4', '   ');

    fixture.componentInstance.saveStage(stage);
    http.expectNone((request) => request.method === 'PUT');
    expect(fixture.componentInstance.busyId).toBeNull();
  });

  it('explains a version clash, a missing stage and any other failure when saving', () => {
    click('✎');
    click('Guardar');
    http.expectOne('/api/stages/4').flush({ error: 'VERSION' }, { status: 412, statusText: 'Precondition Failed' });
    fixture.detectChanges();
    expect(fixture.componentInstance.error).toContain('El registro cambió mientras lo editabas');
    expect(fixture.componentInstance.busyId).toBeNull();

    click('Guardar');
    http.expectOne('/api/stages/4').flush({ error: 'NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(fixture.componentInstance.error).toBe('El registro ya no está disponible. Actualiza la lista.');

    click('Guardar');
    http.expectOne('/api/stages/4').flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(fixture.componentInstance.error).toBe('No se pudo guardar el cambio.');
  });

  it('reports a rejected change of stage status', () => {
    click('Iniciar');
    http.expectOne('/api/stages/4/status').flush(null, { status: 412, statusText: 'Precondition Failed' });

    expect(fixture.componentInstance.error).toContain('El registro cambió mientras lo editabas');
    expect(fixture.componentInstance.busyId).toBeNull();
  });

  it('offers to reopen a completed stage and to cancel it', () => {
    fixture.componentInstance.stages = [{ ...stage, status: 'COMPLETADA' }];
    fixture.componentInstance['changeDetector'].markForCheck();
    fixture.detectChanges();

    // Desde COMPLETADA solo se puede reabrir o cancelar.
    const actions = Array.from(
      element.querySelectorAll('.wbs-stage-actions button'),
    ).map((button) => button.textContent?.trim());
    expect(actions).toEqual(['Reabrir', 'Cancelar', '✎']);

    click('Reabrir');
    const request = http.expectOne('/api/stages/4/status');
    expect(request.request.body).toEqual({ status: 'EN_PROGRESO', version: 11 });
    request.flush({ ...stage, status: 'EN_PROGRESO' });
    fixture.detectChanges();

    expect(fixture.componentInstance.stages[0].status).toBe('EN_PROGRESO');
    expect(fixture.componentInstance.success).toBe('Etapa "Diseno": En progreso.');
  });

  it('does not touch a stage while another mutation is in flight', () => {
    fixture.componentInstance.busyId = 'new-stage';
    fixture.componentInstance['changeDetector'].markForCheck();
    fixture.detectChanges();

    fixture.componentInstance.changeStageStatus(stage, 'EN_PROGRESO');
    fixture.componentInstance.saveStage(stage);
    http.expectNone((request) => request.method === 'PUT');
  });

  it('edits an activity and shows its description', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([
      { ...activity, description: 'Analisis funcional' },
    ]);
    fixture.detectChanges();
    expect(element.querySelector('.wbs-activity-description')?.textContent).toContain('Analisis funcional');
    click('✎', activityItem());
    expect(fixture.componentInstance.editingActivityId).toBe(6);
    setValue('#wbs-activity-name-6', 'Analisis ampliado');
    setValue('#wbs-activity-description-6', 'Analisis funcional y tecnico');

    click('Guardar', activityItem());
    const request = http.expectOne('/api/activities/6');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      name: 'Analisis ampliado',
      description: 'Analisis funcional y tecnico',
      version: 21,
    });
    request.flush({ ...activity, name: 'Analisis ampliado' });
    fixture.detectChanges();

    expect(fixture.componentInstance.activities[0].name).toBe('Analisis ampliado');
    expect(fixture.componentInstance.editingActivityId).toBeNull();
    expect(fixture.componentInstance.success).toBe('Actividad actualizada.');
  });

  it('cancels the edit of an activity', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();
    click('✎', activityItem());

    click('Cancelar', activityItem());

    expect(fixture.componentInstance.editingActivityId).toBeNull();
  });

  it('does not save an activity without a name', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();
    click('✎', activityItem());
    setValue('#wbs-activity-name-6', '  ');

    fixture.componentInstance.saveActivity(activity);
    http.expectNone((request) => request.method === 'PUT');
  });

  it('reports a rejected activity change', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();
    click('✎', activityItem());
    click('Guardar', activityItem());
    http.expectOne('/api/activities/6').flush({ error: 'VALIDATION' }, { status: 404, statusText: 'Not Found' });

    expect(fixture.componentInstance.error).toBe('El registro ya no está disponible. Actualiza la lista.');
    expect(fixture.componentInstance.busyId).toBeNull();
  });

  it('changes the status of an activity with its version', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();

    click('Iniciar', activityItem());
    const request = http.expectOne('/api/activities/6/status');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ status: 'EN_PROGRESO', version: 21 });
    request.flush({ ...activity, status: 'EN_PROGRESO' });
    fixture.detectChanges();

    expect(fixture.componentInstance.activities[0].status).toBe('EN_PROGRESO');
    expect(fixture.componentInstance.success).toBe('Actividad "Analisis": En progreso.');
  });

  it('reports a rejected activity status change', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();

    click('Iniciar', activityItem());
    http.expectOne('/api/activities/6/status').flush(null, { status: 412, statusText: 'Precondition Failed' });

    expect(fixture.componentInstance.error).toContain('El registro cambió mientras lo editabas');
    expect(fixture.componentInstance.busyId).toBeNull();
  });

  it('does not touch an activity while another mutation is in flight', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.componentInstance.busyId = 'new-activity-4';

    fixture.componentInstance.changeActivityStatus(activity, 'EN_PROGRESO');
    fixture.componentInstance.saveActivity(activity);
    http.expectNone((request) => request.method === 'PUT');
  });

  it('does not create an activity without a name or without an expanded stage', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([]);
    fixture.componentInstance.newActivityName = '   ';

    fixture.componentInstance.createActivity();
    http.expectNone((request) => request.method === 'POST');

    // Sin etapa desplegada no hay dónde crear la actividad.
    fixture.componentInstance.newActivityName = 'Cimentacion';
    fixture.componentInstance.expandedStageId = null;
    fixture.componentInstance.createActivity();
    http.expectNone((request) => request.method === 'POST');
  });

  it('reports a rejected new activity', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([]);
    fixture.detectChanges();
    setValue('#wbs-new-activity-name-4', 'Cimentacion');
    click('Crear actividad');

    http
      .expectOne('/api/stages/4/activities')
      .flush({ error: 'VALIDATION_ERROR', message: 'El nombre ya existe' }, { status: 400, statusText: 'Bad Request' });

    expect(fixture.componentInstance.error).toBe('El nombre ya existe');
    expect(fixture.componentInstance.busyId).toBeNull();
  });

  it('forgets the changes of an activity saved for another stage', () => {
    const emitted: unknown[] = [];
    fixture.componentInstance.structureChanged.subscribe((value) => emitted.push(value));
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    fixture.detectChanges();
    click('✎', activityItem());
    setValue('#wbs-activity-name-6', 'Analisis ampliado');
    click('Guardar', activityItem());

    // La etapa se cerró mientras llegaba la respuesta: la lista no se toca.
    fixture.componentInstance.expandedStageId = 9;
    http.expectOne('/api/activities/6').flush([{ ...activity, name: 'Analisis ampliado' }]);

    // Se queda lo que había: la respuesta de la etapa cerrada no se aplica.
    expect(fixture.componentInstance.activities).toEqual([activity]);
    expect(fixture.componentInstance.busyId).toBeNull();
    expect(emitted.length).toBe(0);
  });

  it('names the statuses and the buttons of every transition', () => {
    const component = fixture.componentInstance;
    // La etapa queda en progreso: desde ahí se puede pausar, completar o cancelar.
    expect(component.statusLabel('CANCELADA')).toBe('Cancelada');
    expect(component.statusClass('EN_PROGRESO')).toBe('wbs-status-en-progreso');
    expect(component.statusActionLabel('COMPLETADA', 'EN_PROGRESO')).toBe('Reabrir');
    expect(component.statusActionLabel('CANCELADA', 'EN_PROGRESO')).toBe('Iniciar');
    expect(component.statusActionLabel('EN_PROGRESO', 'PENDIENTE')).toBe('Pausar');
    expect(component.statusActionLabel('EN_PROGRESO', 'CANCELADA')).toBe('Cancelar');
    expect(component.statusActionLabel('EN_PROGRESO', 'COMPLETADA')).toBe('Completar');

    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush([activity]);
    expect(component.activitiesByStage).toEqual([activity]);
    expect(component.nextStatuses(activity.status)).toEqual(['EN_PROGRESO', 'CANCELADA']);
  });

  it('dismisses the messages of the panel', () => {
    click('Diseno');
    http.expectOne('/api/stages/4/activities').flush({ error: 'INTERNAL' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(element.querySelector('.alert-error')?.textContent).toContain('No se pudieron cargar');

    click('.wbs-dismiss', element.querySelector('.alert-error')!);

    expect(fixture.componentInstance.error).toBeNull();
    expect(element.querySelector('.alert-error')).toBeNull();
  });
});


