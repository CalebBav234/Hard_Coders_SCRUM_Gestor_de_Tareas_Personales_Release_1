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

  function click(label: string, scope: ParentNode = element): void {
    const button = buttons(scope).find((b) => b.textContent?.includes(label));
    expect(button, `botón "${label}"`).toBeTruthy();
    button!.click();
    fixture.detectChanges();
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
});
