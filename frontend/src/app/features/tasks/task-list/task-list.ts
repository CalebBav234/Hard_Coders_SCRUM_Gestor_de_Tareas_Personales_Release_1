import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  OnInit,
  Output
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TaskService } from '../../../core/services/task.service';
import { Task } from '../../../core/models/task';

import { TaskForm } from '../task-form/task-form';
import { TaskEditor } from '../task-editor/task-editor';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TaskForm,
    TaskEditor
  ],
  templateUrl: './task-list.html',
  styleUrl: './task-list.css'
})
export class TaskList implements OnInit {

  @Output() editRequested = new EventEmitter<Task>();
  @Output() subtaskRequested = new EventEmitter<Task>();

  tasks: Task[] = [];
  categories: any[] = [];

  subtasksModalParent: Task | null = null;

  subtaskModalMode: 'list' | 'create' | 'edit' = 'list';

  editingSubtask: Task | null = null;

  loading = false;
  successMessage = '';
  errorMessage = '';
  searchTerm = '';

  busyTaskId: number | null = null;
  pausingSubtasksForParentId: number | null = null;
  confirmingDelete: number | null = null;

  editingCategoryId: number | null = null;
  selectedCategoryName: string | null = null;

  completedAsideOpen = false;

  swipeOffsets: Record<number, number> = {};

  private swipeStartX = 0;
  private activeSwipeTaskId: number | null = null;
  private swipeMoved = false;

  private readonly swipeThreshold = 80;
  private readonly maxSwipeDistance = 125;

  private timerInterval: ReturnType<typeof setInterval> | null = null;

  currentTime = Date.now();

  constructor(
    private readonly taskService: TaskService,
    private readonly changeDetector: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadTasks();
    this.loadCategories();
    this.startTimer();
  }

  // =========================================================
// RELOJ EN TIEMPO REAL
// =========================================================

  private startTimer(): void {
    if (this.timerInterval) {
      return;
    }

    this.timerInterval = setInterval(() => {
      this.currentTime = Date.now();
      this.changeDetector.markForCheck();
    }, 1000);
  }
  // =========================================================
  // CARGAR TAREAS
  // =========================================================

  loadTasks(): void {
    this.loading = true;
    this.errorMessage = '';

    this.taskService.listTasks().subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.loading = false;
        this.busyTaskId = null;

        this.changeDetector.markForCheck();
      },

      error: (error) => {
        console.error('Error al cargar tareas:', error);

        this.errorMessage =
          'No se pudieron cargar las tareas.';

        this.loading = false;
        this.busyTaskId = null;

        this.changeDetector.markForCheck();
      }
    });
  }

  // =========================================================
  // CARGAR CATEGORÍAS
  // =========================================================

  loadCategories(): void {
    this.taskService.listCategories().subscribe({
      next: (categories) => {
        this.categories = categories;

        this.changeDetector.markForCheck();
      },

      error: (error) => {
        console.error(
          'Error al cargar categorías:',
          error
        );
      }
    });
  }

  // =========================================================
  // TAREAS PRINCIPALES
  // =========================================================

  get parentTasks(): Task[] {
    return this.tasks.filter(
      (task) =>
        !task.parentTaskId &&
        task.status !== 'TERMINADA'
    );
  }

  // =========================================================
  // TAREAS COMPLETADAS
  // =========================================================

  get completedTasks(): Task[] {
    return this.tasks
      .filter(
        (task) => task.status === 'TERMINADA'
      )
      .sort((a, b) => {
        const dateA = a.completedAt
          ? new Date(a.completedAt).getTime()
          : 0;

        const dateB = b.completedAt
          ? new Date(b.completedAt).getTime()
          : 0;

        return dateB - dateA;
      });
  }

  getParentTask(task: Task): Task | undefined {
    if (!task.parentTaskId) {
      return undefined;
    }

    return this.tasks.find(
      (parentTask) =>
        parentTask.id === task.parentTaskId
    );
  }
  // =========================================================
  // SUBTAREAS NO TERMINADAS
  // =========================================================

  getSubtasks(parentId: number): Task[] {
    return this.tasks.filter(
      (task) =>
        task.parentTaskId === parentId &&
        task.status !== 'TERMINADA'
    );
  }

  // =========================================================
  // TODAS LAS SUBTAREAS
  // =========================================================

  getAllSubtasks(parentId: number): Task[] {
    return this.tasks.filter(
      (task) =>
        task.parentTaskId === parentId
    );
  }

  // =========================================================
  // ASIDE DE TAREAS COMPLETADAS
  // =========================================================

  openCompletedAside(): void {
    this.completedAsideOpen = true;
    this.changeDetector.markForCheck();
  }

  closeCompletedAside(): void {
    this.completedAsideOpen = false;
    this.changeDetector.markForCheck();
  }

  // =========================================================
  // POPUP DE SUBTAREAS
  // =========================================================

  openSubtasksModal(task: Task): void {
    if (this.actionsDisabled(task)) {
      return;
    }

    this.subtasksModalParent = task;
    this.subtaskModalMode = 'list';
    this.editingSubtask = null;

    this.changeDetector.markForCheck();
  }

  closeSubtasksModal(): void {
    this.subtasksModalParent = null;
    this.subtaskModalMode = 'list';
    this.editingSubtask = null;

    this.changeDetector.markForCheck();
  }

  // =========================================================
  // CREAR SUBTAREA DENTRO DEL POPUP
  // =========================================================

  openCreateSubtask(): void {
    if (!this.subtasksModalParent) {
      return;
    }

    this.subtaskModalMode = 'create';
    this.editingSubtask = null;

    this.changeDetector.markForCheck();
  }

  // =========================================================
  // EDITAR SUBTAREA DENTRO DEL POPUP
  // =========================================================

  openEditSubtask(task: Task): void {
    if (this.actionsDisabled(task)) {
      return;
    }

    this.editingSubtask = task;
    this.subtaskModalMode = 'edit';

    this.changeDetector.markForCheck();
  }

  // =========================================================
  // VOLVER A LA LISTA DE SUBTAREAS
  // =========================================================

  backToSubtasks(): void {
    this.subtaskModalMode = 'list';
    this.editingSubtask = null;

    this.changeDetector.markForCheck();
  }

  // =========================================================
  // SUBTAREA CREADA
  // =========================================================

  onSubtaskCreated(task: Task): void {
    this.successMessage =
    'Subtarea creada correctamente.';

    this.subtaskModalMode = 'list';
    this.editingSubtask = null;

    // Recargar las tareas para obtener inmediatamente
    // la subtarea recién creada y actualizar el contador.
    this.loadTasks();
    }


  // =========================================================
  // SUBTAREA EDITADA
  // =========================================================

  onSubtaskSaved(updatedTask: Task): void {
    this.tasks = this.tasks.map(
      (currentTask) =>
        currentTask.id === updatedTask.id
          ? updatedTask
          : currentTask
    );

    this.successMessage =
      'Subtarea actualizada correctamente.';

    this.subtaskModalMode = 'list';
    this.editingSubtask = null;

    this.changeDetector.markForCheck();
  }

  // =========================================================
  // COLOR DE LAS TARJETAS
  // =========================================================

  getTaskColorClass(taskId: number): string {
    const colors = [
      'postit-blue',
      'postit-green',
      'postit-purple',
      'postit-burgundy',
      'postit-brown',
      'postit-slate',
      'postit-teal',
      'postit-indigo'
    ];

    const index =
      Math.abs(taskId * 17 + 11) %
      colors.length;

    return colors[index];
  }

  // =========================================================
  // BUSCAR
  // =========================================================

  searchTasks(): void {
    const term =
      this.searchTerm.trim();

    if (!term) {
      this.loadTasks();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.taskService.listTasks(term).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.loading = false;

        this.changeDetector.markForCheck();
      },

      error: (error) => {
        console.error(
          'Error buscando tareas:',
          error
        );

        this.errorMessage =
          'No se pudieron buscar las tareas.';

        this.loading = false;

        this.changeDetector.markForCheck();
      }
    });
  }

  // =========================================================
// SWIPE
// =========================================================

startSwipe(event: PointerEvent, task: Task): void {
  if (this.actionsDisabled(task)) {
    return;
  }

  const target = event.target as HTMLElement | null;

  /*
   * Si el usuario está interactuando con un botón,
   * no iniciar ningún swipe.
   */
  if (
    target?.closest(
      'button, input, select, textarea, a'
    )
  ) {
    return;
  }

  this.activeSwipeTaskId = task.id;
  this.swipeStartX = event.clientX;
  this.swipeMoved = false;

  const element = event.currentTarget as HTMLElement;

  try {
    element.setPointerCapture(event.pointerId);
  } catch {
    // El navegador puede no permitir pointer capture.
  }
}

moveSwipe(
  event: PointerEvent,
  task: Task
): void {
  if (
    this.activeSwipeTaskId !== task.id
  ) {
    return;
  }

  const deltaX =
    event.clientX - this.swipeStartX;

  if (Math.abs(deltaX) < 5) {
    return;
  }

  this.swipeMoved = true;

  const offset = Math.max(
    -this.maxSwipeDistance,
    Math.min(
      this.maxSwipeDistance,
      deltaX
    )
  );

  this.swipeOffsets = {
    ...this.swipeOffsets,
    [task.id]: offset,
  };

  event.preventDefault();

  this.changeDetector.markForCheck();
}

endSwipe(
  event: PointerEvent,
  task: Task
): void {
  if (
    this.activeSwipeTaskId !== task.id
  ) {
    return;
  }

  const offset =
    this.getSwipeOffset(task.id);

  this.releasePointer(event);

  this.activeSwipeTaskId = null;

  if (
    offset <= -this.swipeThreshold
  ) {
    this.resetSwipe(task.id);
    this.requestDelete(task);
    return;
  }

  if (
    offset >= this.swipeThreshold
  ) {
    this.resetSwipe(task.id);

    if (task.status === 'ACTIVA') {
      this.complete(task);
    }

    return;
  }

  this.resetSwipe(task.id);
}

cancelSwipe(
  taskId: number
): void {
  if (
    this.activeSwipeTaskId === taskId
  ) {
    this.activeSwipeTaskId = null;
  }

  this.resetSwipe(taskId);
}

resetSwipe(
  taskId: number
): void {
  this.swipeOffsets = {
    ...this.swipeOffsets,
    [taskId]: 0,
  };

  this.swipeMoved = false;

  this.changeDetector.markForCheck();
}

getSwipeOffset(
  taskId: number
): number {
  return this.swipeOffsets[taskId] ?? 0;
}

isSwiping(
  taskId: number
): boolean {
  return (
    this.activeSwipeTaskId === taskId &&
    this.swipeMoved
  );
}

private releasePointer(
  event: PointerEvent
): void {
  const element =
    event.currentTarget as HTMLElement;

  try {
    if (
      element.hasPointerCapture(
        event.pointerId
      )
    ) {
      element.releasePointerCapture(
        event.pointerId
      );
    }
  } catch {}
}
  // =========================================================
  // ACTIVAR
  // =========================================================

  activate(task: Task): void {

    if (this.actionsDisabled(task)) {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService
      .activate(task)
      .subscribe({

        next: () => {

          this.successMessage =
            'Tarea activada correctamente.';

          this.busyTaskId = null;

          this.loadTasks();
        },

        error: (error) => {

          console.error(
            'Error activando tarea:',
            error
          );

          this.errorMessage =
            'No se pudo activar la tarea.';

          this.busyTaskId = null;

          this.changeDetector.markForCheck();
        }
      });
  }

  // =========================================================
  // PAUSAR
  // =========================================================

  pause(task: Task): void {
    if (this.actionsDisabled(task)) {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService.pause(task).subscribe({
      next: () => {
        const subtasks = this.getAllSubtasks(task.id);

        const activeSubtasks = subtasks.filter(
          (subtask) => subtask.status === 'ACTIVA'
        );

        // Si no hay subtareas activas, terminamos.
        if (activeSubtasks.length === 0) {
          this.successMessage =
            'Tarea pausada correctamente.';

          this.busyTaskId = null;
          this.loadTasks();
          return;
        }

        // Bloqueamos inmediatamente todo el grupo.
        this.pausingSubtasksForParentId = task.id;

        let completed = 0;
        let hasError = false;

        activeSubtasks.forEach((subtask) => {
          this.taskService.pause(subtask).subscribe({
            next: () => {
              completed++;

              if (completed === activeSubtasks.length) {
                this.pausingSubtasksForParentId = null;
                this.busyTaskId = null;

                if (hasError) {
                  this.errorMessage =
                    'La tarea se pausó, pero algunas subtareas no pudieron pausarse.';
                } else {
                  this.successMessage =
                    'Tarea y subtareas pausadas correctamente.';
                }

                this.loadTasks();
              }
            },

            error: (error) => {
              console.error(
                `Error pausando subtarea ${subtask.id}:`,
                error
              );

              hasError = true;
              completed++;

              if (completed === activeSubtasks.length) {
                this.pausingSubtasksForParentId = null;
                this.busyTaskId = null;

                this.errorMessage =
                  'La tarea se pausó, pero algunas subtareas no pudieron pausarse.';

                this.loadTasks();
              }
            }
          });
        });
      },

      error: (error) => {
        console.error('Error pausando tarea:', error);

        this.errorMessage =
          'No se pudo pausar la tarea.';

        this.busyTaskId = null;
        this.pausingSubtasksForParentId = null;

        this.changeDetector.markForCheck();
      }
    });
  }
  // =========================================================
  // COMPLETAR
  // =========================================================

  complete(task: Task): void {
    if (this.actionsDisabled(task)) {
      return;
    }

    if (task.status !== 'ACTIVA') {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService.complete(task).subscribe({
      next: () => {
        this.successMessage = 'Tarea completada correctamente.';
        this.busyTaskId = null;

        this.loadTasks();
      },
      error: (error) => {
        console.error('Error completando tarea:', error);
        this.errorMessage = 'No se pudo completar la tarea.';
        this.busyTaskId = null;
        this.changeDetector.markForCheck();
      }
    });
  }

  // =========================================================
  // REABRIR
  // =========================================================

  reopen(task: Task): void {

    if (this.actionsDisabled(task)) {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService
      .reopen(task)
      .subscribe({

        next: (updatedTask) => {

          // Actualizar la tarea dentro de la lista
          this.tasks = this.tasks.map(
            (currentTask) =>
              currentTask.id === updatedTask.id
                ? updatedTask
                : currentTask
          );

          // La tarea reabierta pasa al principio
          this.tasks = [
            updatedTask,
            ...this.tasks.filter(
              (currentTask) =>
                currentTask.id !== updatedTask.id
            )
          ];

          this.successMessage =
            'Tarea reabierta correctamente.';

          this.busyTaskId = null;

          this.changeDetector.markForCheck();
        },

        error: (error) => {

          console.error(
            'Error reabriendo tarea:',
            error
          );

          this.errorMessage =
            'No se pudo reabrir la tarea.';

          this.busyTaskId = null;

          this.changeDetector.markForCheck();
        }
      });
  }

  // =========================================================
  // ELIMINAR
  // =========================================================

  requestDelete(task: Task): void {

    if (this.actionsDisabled(task)) {
      return;
    }

    this.confirmingDelete = task.id;
    this.clearMessages();

    this.changeDetector.markForCheck();
  }

  cancelDelete(): void {

    this.confirmingDelete = null;

    this.changeDetector.markForCheck();
  }

  deleteTask(task: Task): void {

    if (
      this.busyTaskId === task.id
    ) {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService
      .deleteTask(task)
      .subscribe({

        next: () => {

          this.successMessage =
            'Tarea eliminada correctamente.';

          this.confirmingDelete = null;
          this.busyTaskId = null;

          this.loadTasks();
        },

        error: (error) => {

          console.error(
            'Error eliminando tarea:',
            error
          );

          this.errorMessage =
            'No se pudo eliminar la tarea.';

          this.busyTaskId = null;
          this.confirmingDelete = null;

          this.changeDetector.markForCheck();
        }
      });
  }

  // =========================================================
  // EDITAR TAREA PRINCIPAL
  // =========================================================

  editTask(task: Task): void {

    if (this.actionsDisabled(task)) {
      return;
    }

    this.editRequested.emit(task);
  }

  // =========================================================
  // CREAR SUBTAREA DESDE LA TARJETA PRINCIPAL
  // =========================================================

  createSubtask(task: Task): void {

    if (this.actionsDisabled(task)) {
      return;
    }

    this.subtaskRequested.emit(task);
  }

  // =========================================================
  // EDITAR CATEGORÍA
  // =========================================================

  startCategoryEdit(
    task: Task
  ): void {

    this.editingCategoryId = task.id;

    this.selectedCategoryName =
      task.categoryName ?? null;
  }

  cancelCategoryEdit(): void {

    this.editingCategoryId = null;
    this.selectedCategoryName = null;
  }

  saveCategory(
    task: Task
  ): void {

    if (
      this.busyTaskId !== null
    ) {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService
      .changeCategory(
        task,
        this.selectedCategoryName
      )
      .subscribe({

        next: () => {

          this.successMessage =
            'Categoría actualizada.';

          this.editingCategoryId = null;
          this.selectedCategoryName = null;
          this.busyTaskId = null;

          this.loadTasks();
        },

        error: (error) => {

          console.error(
            'Error actualizando categoría:',
            error
          );

          this.errorMessage =
            'No se pudo actualizar la categoría.';

          this.busyTaskId = null;

          this.changeDetector.markForCheck();
        }
      });
  }

  // =========================================================
  // DESHABILITAR ACCIONES
  // =========================================================

  actionsDisabled(task: Task): boolean {
    const parentIsInactive = this.isParentInactive(task);

    const parentIsBeingPaused =
      task.parentTaskId !== null &&
      task.parentTaskId === this.pausingSubtasksForParentId;

    const isParentBeingPaused =
      !task.parentTaskId &&
      task.id === this.pausingSubtasksForParentId;

    return (
      this.loading ||
      this.busyTaskId === task.id ||
      parentIsInactive ||
      parentIsBeingPaused ||
      isParentBeingPaused ||
      (
        this.confirmingDelete !== null &&
        this.confirmingDelete !== task.id
      ) ||
      (
        this.editingCategoryId !== null &&
        this.editingCategoryId !== task.id
      )
    );
  }

  isParentInactive(task: Task): boolean {
    if (!task.parentTaskId) {
      return false;
    }

    const parentTask = this.getParentTask(task);

    return parentTask?.status === 'INACTIVA';
  }

  // =========================================================
  // TIEMPO ACTIVO
  // =========================================================

  formatActiveTime(seconds: number): string {
    if (!seconds || seconds <= 0) {
      return '00:00:00';
    }

    const hours = Math.floor(seconds / 3600);

    const minutes = Math.floor(
      (seconds % 3600) / 60
    );

    const remainingSeconds =
      seconds % 60;

    return [
      hours.toString().padStart(2, '0'),
      minutes.toString().padStart(2, '0'),
      remainingSeconds.toString().padStart(2, '0')
    ].join(':');
  }

  getActiveSeconds(task: Task): number {
    const storedSeconds = task.totalActiveSeconds ?? 0;

    if (
      task.status !== 'ACTIVA' ||
      !task.activatedAt
    ) {
      return storedSeconds;
    }

    const activatedAt =
      new Date(task.activatedAt).getTime();

    const elapsedSeconds =
      Math.max(
        0,
        Math.floor(
          (this.currentTime - activatedAt) / 1000
        )
      );

    return storedSeconds + elapsedSeconds;
  }

  // =========================================================
  // MENSAJES
  // =========================================================

  private clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  // =========================================================
  // TAREA CREADA DESDE EL FORMULARIO PRINCIPAL
  // =========================================================

  showCreatedTask(
    task: Task
  ): void {

    const exists =
      this.tasks.some(
        (currentTask) =>
          currentTask.id === task.id
      );

    if (!exists) {
      this.tasks = [
        task,
        ...this.tasks
      ];
    }

    this.changeDetector.markForCheck();
  }

  // =========================================================
// ACTUALIZAR TAREA DESDE EL EDITOR
// =========================================================

  updateTaskInList(updatedTask: Task): void {
    this.tasks = this.tasks.map(
      (task) =>
        task.id === updatedTask.id
          ? updatedTask
          : task
    );

    this.successMessage =
      'Tarea actualizada correctamente.';

    this.changeDetector.markForCheck();
  }
}