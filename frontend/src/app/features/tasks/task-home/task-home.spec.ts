import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Project, UserSummary } from '../../../core/models/project';
import { Task } from '../../../core/models/task';
import { TaskEditor } from '../task-editor/task-editor';
import { TaskForm } from '../task-form/task-form';
import { TaskList } from '../task-list/task-list';
import { TaskHome } from './task-home';

describe('TaskHome — armado de la pantalla de tareas', () => {
  let fixture: ComponentFixture<TaskHome>;
  let http: HttpTestingController;
  let element: HTMLElement;

  const manager: UserSummary = { userId: 2, fullName: 'Ana Paz', email: 'a@x.com', chief: false, active: true };

  const project: Project = {
    projectId: 42,
    name: 'Proyecto de pruebas',
    description: null,
    status: 'EN_EJECUCION',
    closed: false,
    budgetBob: 1000,
    allocatedBudgetBob: 0,
    estimatedDurationMinutes: 60,
    manager,
    createdBy: manager,
    currentRevision: 1,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 1,
    canEditDetails: true,
    canEditPlan: true,
    canDelegate: true,
    canManageHierarchy: true,
    allowedStatuses: [],
  };

  const createdTask: Task = {
    id: 11,
    kind: 'TASK',
    entityId: 11,
    title: 'Nueva tarea',
    description: null,
    status: 'INACTIVA',
    priority: 'MEDIA',
    categoryId: null,
    categoryName: null,
    assignedWorkerIds: [],
    parentTaskId: null,
    activatedAt: null,
    completedAt: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    version: 1,
  };

  const existingTask: Task = {
    ...createdTask,
    id: 7,
    entityId: 7,
    title: 'Tarea existente',
    description: 'Descripción de la tarea',
  };

  /** Forma mínima del árbol de depuración: basta para buscar instancias hijas. */
  interface DebugNodeLike {
    componentInstance?: unknown;
    children: DebugNodeLike[];
  }

  /** Todos los componentes del tipo pedido que haya en el árbol renderizado. */
  function instancesOf<T>(kind: new (...args: never[]) => unknown): T[] {
    const found: T[] = [];
    const visit = (node: DebugNodeLike): void => {
      if (node.componentInstance instanceof kind) {
        found.push(node.componentInstance as T);
      }
      for (const child of node.children) {
        visit(child);
      }
    };
    visit(fixture.debugElement as unknown as DebugNodeLike);
    return found;
  }

  function componentIn<T>(kind: new (...args: never[]) => unknown): T {
    const found = instancesOf<T>(kind);
    expect(found.length, `instancia de ${kind.name}`).toBeGreaterThan(0);
    return found[0];
  }

  /** El formulario del modal de subtarea, no el de la sección de creación. */
  function subtaskForm(): TaskForm {
    const forms = instancesOf<TaskForm>(TaskForm);
    const modal = forms.find((form) => form.parentTaskId !== null);
    expect(modal, 'formulario del modal de subtarea').toBeTruthy();
    return modal as TaskForm;
  }

  function createForm(): TaskForm {
    const forms = instancesOf<TaskForm>(TaskForm);
    const main = forms.find((form) => form.parentTaskId === null);
    expect(main, 'formulario de creación de tareas').toBeTruthy();
    return main as TaskForm;
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

  function modal(): HTMLElement {
    const dialog = element.querySelector('dialog.editor-modal');
    expect(dialog, 'modal del editor').toBeTruthy();
    return dialog as HTMLElement;
  }

  /** Abre el modal de edición y resuelve la petición de categorías del editor. */
  function openEditor(task: Task = existingTask): void {
    fixture.componentInstance.openEditor(task);
    refresh();
    http.expectOne((request) => request.url === '/api/activities').flush([]);
    refresh();
  }

  function openSubtaskForm(task: Task = existingTask): void {
    fixture.componentInstance.openSubtaskForm(task);
    refresh();
  }

  /**
   * Marca la vista como sucia antes de refrescar: la pantalla no usa señales para estos
   * campos, así que en el entorno sin zona hay que avisar al detector de cambios.
   */
  function refresh(): void {
    fixture.componentRef.changeDetectorRef.markForCheck();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [TaskHome],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TaskHome);
    element = fixture.nativeElement;
    // La lista pide los proyectos al montarse: esta pantalla no tiene proyectos que elegir,
    // así que se comporta como "Mis tareas" del Personal y solo carga lo asignado.
    fixture.detectChanges();
    http.expectOne('/api/projects').flush([]);
    fixture.detectChanges();
    http.expectOne('/api/tasks/assigned').flush([]);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
  });

  it('renders the create form together with the task list', () => {
    expect(element.querySelector('app-task-form')).not.toBeNull();
    expect(element.querySelector('app-task-list')).not.toBeNull();
    expect(element.textContent).toContain('Crear nueva tarea');
    // Sin actividad elegida el formulario avisa de dónde se creará la tarea.
    expect(element.textContent).toContain('Elige una etapa y su actividad');
    // Sin modal abierto no se muestra ningún editor.
    expect(element.querySelector('app-task-editor')).toBeNull();
    expect(element.querySelector('dialog.editor-modal')).toBeNull();
    // Sin proyectos que elegir la pantalla es "Mis tareas".
    expect(element.textContent).toContain('No tienes tareas asignadas');
  });

  it('adds a task created by the form to the list it owns', async () => {
    const list = componentIn<TaskList>(TaskList);
    expect(list.tasks).toEqual([]);

    createForm().taskCreated.emit(createdTask);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(list.tasks.map((task) => task.title)).toEqual(['Nueva tarea']);
    expect(element.querySelector('.task-title')?.textContent).toContain('Nueva tarea');
    // La pantalla no confunde la creación con la edición: el modal sigue cerrado.
    expect(fixture.componentInstance.editingTask).toBeNull();
  });

  it('opens the editor of the task the list asks to edit', () => {
    const list = componentIn<TaskList>(TaskList);
    list.editRequested.emit(existingTask);
    fixture.detectChanges();

    expect(element.querySelector('app-task-editor')).not.toBeNull();
    expect(element.textContent).toContain('EDITAR TAREA');
    expect(element.textContent).toContain('Tarea existente');
    // El editor de la tarea pide sus categorías al montarse.
    http.expectOne((request) => request.url === '/api/activities').flush([]);
    fixture.detectChanges();
    expect(componentIn<TaskEditor>(TaskEditor).form.controls.title.value).toBe('Tarea existente');
  });

  it('substitutes the task in the list when the editor saves it', () => {
    const list = componentIn<TaskList>(TaskList);
    list.tasks = [existingTask];
    openEditor();

    const updated: Task = { ...existingTask, title: 'Tarea editada' };
    componentIn<TaskEditor>(TaskEditor).saved.emit(updated);
    fixture.detectChanges();

    expect(fixture.componentInstance.editingTask).toBeNull();
    expect(element.querySelector('app-task-editor')).toBeNull();
    expect(list.tasks.map((task) => task.title)).toEqual(['Tarea editada']);
    expect(list.successMessage).toContain('Tarea actualizada correctamente');
    expect(element.querySelector('.alert-success')?.textContent).toContain('Tarea actualizada');
  });

  it('closes the editor when the cancel button of the editor is used', () => {
    openEditor();
    componentIn<TaskEditor>(TaskEditor).cancelled.emit();
    fixture.detectChanges();
    expect(fixture.componentInstance.editingTask).toBeNull();
    expect(element.querySelector('app-task-editor')).toBeNull();
  });

  it('closes the editor with the close button of the modal', () => {
    openEditor();
    click('×', modal());
    expect(fixture.componentInstance.editingTask).toBeNull();
    expect(element.querySelector('app-task-editor')).toBeNull();
  });

  it('closes the editor when the backdrop is clicked', () => {
    openEditor();
    (element.querySelector('.modal-backdrop') as HTMLElement).click();
    fixture.detectChanges();
    expect(fixture.componentInstance.editingTask).toBeNull();
    expect(element.querySelector('app-task-editor')).toBeNull();
  });

  it('opens the subtask form of the task the list points at', () => {
    const list = componentIn<TaskList>(TaskList);
    list.subtaskRequested.emit(existingTask);
    fixture.detectChanges();

    expect(element.querySelector('app-task-editor')).toBeNull();
    expect(modal().textContent).toContain('NUEVA SUBTAREA');
    expect(modal().textContent).toContain('Tarea existente');
    // El formulario queda en modo subtarea: sin prioridad ni actividad.
    expect(modal().textContent).toContain('Crear nueva subtarea');
    expect(subtaskForm().parentTaskId).toBe(7);
  });

  it('reloads the list when a subtask is created and closes the modal', async () => {
    openSubtaskForm();
    subtaskForm().taskCreated.emit(createdTask);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.creatingSubtaskFor).toBeNull();
    const request = http.expectOne((candidate) => candidate.url === '/api/tasks/assigned');
    expect(request.request.method).toBe('GET');
    request.flush([existingTask]);
    fixture.detectChanges();
    expect(element.querySelector('.task-title')?.textContent).toContain('Tarea existente');
  });

  it('closes the subtask form when the cancel button of the form is used', () => {
    openSubtaskForm();
    subtaskForm().cancelled.emit();
    fixture.detectChanges();
    expect(fixture.componentInstance.creatingSubtaskFor).toBeNull();
    expect(element.querySelector('dialog.editor-modal')).toBeNull();
  });

  it('closes the subtask form with the close button of the modal', () => {
    openSubtaskForm();
    click('×', modal());
    expect(fixture.componentInstance.creatingSubtaskFor).toBeNull();
    expect(element.querySelector('dialog.editor-modal')).toBeNull();
  });

  it('closes the subtask form when the backdrop is clicked', () => {
    openSubtaskForm();
    (element.querySelector('.modal-backdrop') as HTMLElement).click();
    fixture.detectChanges();
    expect(fixture.componentInstance.creatingSubtaskFor).toBeNull();
    expect(element.querySelector('dialog.editor-modal')).toBeNull();
  });

  it('keeps only one modal open at a time', () => {
    const list = componentIn<TaskList>(TaskList);
    list.tasks = [existingTask];
    refresh();

    // La taratera pide la subtarea y después la edición: nunca conviven los dos modales.
    click('＋ Subtarea');
    expect(modal().textContent).toContain('NUEVA SUBTAREA');

    click('✎ Editar');
    http.expectOne((request) => request.url === '/api/activities').flush([]);
    refresh();

    expect(element.querySelector('app-task-editor')).not.toBeNull();
    expect(element.textContent).toContain('EDITAR TAREA');
    expect(element.textContent).not.toContain('NUEVA SUBTAREA');
  });
});
