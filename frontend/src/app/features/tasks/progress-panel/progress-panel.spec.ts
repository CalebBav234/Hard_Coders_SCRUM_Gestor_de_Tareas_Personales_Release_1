import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProjectProgressDto } from '../../../core/models/api';
import { ProgressPanel } from './progress-panel';

describe('ProgressPanel — US3.3 (progreso en cascada)', () => {
  let fixture: ComponentFixture<ProgressPanel>;
  let http: HttpTestingController;
  let element: HTMLElement;

  const tree: ProjectProgressDto = {
    projectId: 42,
    projectName: 'Edificio A',
    projectStatus: 'EN_EJECUCION',
    budgetBob: 15000,
    allocatedBudgetBob: 250,
    estimatedDurationMinutes: 600,
    root: {
      id: null,
      name: 'Edificio A',
      completed: 1,
      total: 3,
      leaf: false,
      ratio: 1 / 3,
      children: [
        {
          id: 4,
          name: 'Construcción',
          completed: 1,
          total: 3,
          leaf: false,
          ratio: 1 / 3,
          children: [
            {
              id: 6,
              name: 'Cimentación',
              completed: 1,
              total: 2,
              leaf: false,
              ratio: 0.5,
              children: [
                { id: 8, name: 'Excavar', completed: 1, total: 1, leaf: true, ratio: 1, children: [] },
                { id: 9, name: 'Encofrar', completed: 0, total: 1, leaf: true, ratio: 0, children: [] },
              ],
            },
            {
              id: 7,
              name: 'Acabados',
              completed: 0,
              total: 1,
              leaf: false,
              ratio: 0,
              children: [{ id: 10, name: 'Pintar', completed: 0, total: 1, leaf: true, ratio: 0, children: [] }],
            },
          ],
        },
      ],
    },
  };

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ProgressPanel],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ProgressPanel);
    element = fixture.nativeElement;
    fixture.componentInstance.projectId = 42;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
  });

  it('renders the whole cascade tree with the percentage of every node', () => {
    http.expectOne('/api/projects/42/progress').flush(tree);
    fixture.detectChanges();

    for (const name of ['Edificio A', 'Construcción', 'Cimentación', 'Excavar', 'Acabados', 'Pintar']) {
      expect(element.textContent).toContain(name);
    }
    expect(element.textContent).toContain('33 %');
    expect(element.textContent).toContain('50 %');
    expect(element.textContent).toContain('100 %');
  });

  it('shows the leaf counters without double counting tasks that have subtasks', () => {
    http.expectOne('/api/projects/42/progress').flush(tree);
    fixture.detectChanges();

    const leaves = Array.from(element.querySelectorAll('.progress-node.leaf')).map((n) =>
      n.querySelector('.progress-node-ratio')?.textContent?.trim(),
    );
    expect(leaves).toEqual(['100 %', '0 %', '0 %']);
    // La raíz agrupa 3 unidades hoja (2 de Cimentación + 1 de Acabados), nunca las repite.
    expect(element.querySelector('.progress-node .progress-node-count')?.textContent).toContain('1 de 3');
  });

  it('publishes the loaded tree so the screen can show the project ratio', () => {
    const emitted: ProjectProgressDto[] = [];
    fixture.componentInstance.loaded.subscribe((value) => emitted.push(value));
    http.expectOne('/api/projects/42/progress').flush(tree);
    fixture.detectChanges();

    expect(emitted.length).toBe(1);
    expect(emitted[0].projectId).toBe(42);
    expect(fixture.componentInstance.percent(1 / 3)).toBe(33);
  });

  it('reloads the tree when the chosen project changes', () => {
    http.expectOne('/api/projects/42/progress').flush(tree);
    fixture.detectChanges();

    // El panel no se recrea al elegir otro proyecto: su ngOnChanges debe recalcular el árbol.
    fixture.componentInstance.projectId = 7;
    fixture.componentInstance.ngOnChanges({
      projectId: { previousValue: 42, currentValue: 7, firstChange: false, isFirstChange: () => false },
    });

    const second = http.expectOne('/api/projects/7/progress');
    expect(second.request.method).toBe('GET');
    second.flush({ ...tree, projectId: 7, projectName: 'Otro edificio' });
    fixture.detectChanges();

    expect(fixture.componentInstance.progress?.projectName).toBe('Otro edificio');
    expect(fixture.componentInstance.progress?.projectId).toBe(7);
  });

  it('reports a friendly message when the progress cannot be loaded', () => {
    http.expectOne('/api/projects/42/progress').flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toContain('No se pudo cargar');
    expect(element.textContent).toContain('No se pudo cargar');
  });
});
