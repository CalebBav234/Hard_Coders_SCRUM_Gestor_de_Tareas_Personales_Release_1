import { ChangeDetectorRef, Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { Task } from '../../../core/models/task';
import { TaskService } from '../../../core/services/task.service';
import { TaskEditor } from '../task-editor/task-editor';

@Component({
  selector: 'app-task-list',
  imports: [TaskEditor],
  templateUrl: './task-list.html',
  styleUrl: './task-list.css',
})
export class TaskList implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  tasks: Task[] = [];
  loading = false;
  error: string | null = null;
  feedback: string | null = null;
  editingTask: Task | null = null;
  confirmingDelete: Task | null = null;
  busyTaskId: number | null = null;
  searchDraft = '';
  activeSearch = '';

  @Output() readonly historyChanged = new EventEmitter<void>();

  get actionsDisabled(): boolean {
    return (
      this.loading ||
      this.busyTaskId !== null ||
      this.editingTask !== null ||
      this.confirmingDelete !== null
    );
  }

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    if (this.actionsDisabled) return;
    this.loading = true;
    this.error = null;
    this.taskService.listTasks(this.activeSearch).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudo cargar la lista de tareas.';
        this.changeDetector.markForCheck();
      },
    });
  }

  searchTasks(event: Event): void {
    event.preventDefault();
    if (this.actionsDisabled) return;
    this.activeSearch = this.searchDraft.trim();
    this.loadTasks();
  }

  updateSearchDraft(event: Event): void {
    this.searchDraft = (event.target as HTMLInputElement).value;
  }

  clearSearch(): void {
    if (this.actionsDisabled) return;
    this.searchDraft = '';
    this.activeSearch = '';
    this.loadTasks();
  }

  showCreatedTask(task: Task): void {
    if (this.activeSearch) {
      this.searchDraft = '';
      this.activeSearch = '';
      this.loadTasks();
    }
    this.tasks = [task, ...this.tasks.filter((currentTask) => currentTask.id !== task.id)];
    this.feedback = `Tarea "${task.title}" creada.`;
    this.error = null;
    this.changeDetector.markForCheck();
  }

  activate(task: Task): void {
    if (this.actionsDisabled) return;
    this.busyTaskId = task.id;
    this.clearFeedback();
    this.taskService.activate(task).subscribe({
      next: () => {
        this.busyTaskId = null;
        this.feedback = `Tarea "${task.title}" activada.`;
        this.changeDetector.markForCheck();
        this.historyChanged.emit();
        this.loadTasks();
      },
      error: (err) => {
        this.busyTaskId = null;
        this.handleError(err);
      },
    });
  }

  complete(task: Task): void {
    if (this.actionsDisabled) return;
    this.busyTaskId = task.id;
    this.clearFeedback();
    this.taskService.complete(task).subscribe({
      next: () => {
        this.busyTaskId = null;
        this.feedback = `Tarea "${task.title}" completada.`;
        this.changeDetector.markForCheck();
        this.historyChanged.emit();
        this.loadTasks();
      },
      error: (err) => {
        this.busyTaskId = null;
        this.handleError(err);
      },
    });
  }

  reopen(task: Task): void {
    if (this.actionsDisabled) return;
    this.busyTaskId = task.id;
    this.clearFeedback();
    this.taskService.reopen(task).subscribe({
      next: () => {
        this.busyTaskId = null;
        this.feedback = `Tarea "${task.title}" reabierta.`;
        this.changeDetector.markForCheck();
        this.historyChanged.emit();
        this.loadTasks();
      },
      error: (err) => {
        this.busyTaskId = null;
        this.handleError(err);
      },
    });
  }

  pause(task: Task): void {
    if (this.actionsDisabled) return;
    this.busyTaskId = task.id;
    this.clearFeedback();
    this.taskService.pause(task).subscribe({
      next: () => {
        this.busyTaskId = null;
        this.feedback = `Tarea "${task.title}" pausada.`;
        this.changeDetector.markForCheck();
        this.loadTasks();
      },
      error: (err) => {
        this.busyTaskId = null;
        this.handleError(err);
      },
    });
  }

  saveCategory(task: Task, event: Event): void {
    const input = event.target as HTMLInputElement;
    const name = input.value.trim();
    if (name === (task.categoryName ?? '')) {
      return;
    }
    this.changeCategory(task, name === '' ? null : name, input);
  }

  changeCategory(task: Task, categoryName: string | null, input: HTMLInputElement | null): void {
    if (this.actionsDisabled) return;
    const previous = task.categoryName ?? '';
    this.busyTaskId = task.id;
    this.clearFeedback();
    this.taskService.changeCategory(task, categoryName).subscribe({
      next: (updated) => {
        this.busyTaskId = null;
        this.tasks = this.tasks.map((current) => (current.id === task.id ? updated : current));
        this.feedback =
          categoryName == null
            ? `Categoría eliminada de "${task.title}".`
            : `Categoría de "${task.title}" actualizada.`;
        this.error = null;
        this.changeDetector.markForCheck();
        this.historyChanged.emit();
      },
      error: (err) => {
        this.busyTaskId = null;
        if (input) {
          input.value = previous;
        }
        this.handleError(err);
        this.changeDetector.markForCheck();
      },
    });
  }

  edit(task: Task): void {
    if (this.actionsDisabled) return;
    this.clearFeedback();
    this.editingTask = task;
  }

  cancelEdit(): void {
    this.editingTask = null;
    this.feedback = 'Edición cancelada. No se guardaron cambios.';
  }

  showUpdatedTask(task: Task): void {
    this.tasks = this.tasks.map((current) => (current.id === task.id ? task : current));
    this.editingTask = null;
    this.feedback = `Tarea "${task.title}" actualizada.`;
    this.error = null;
    this.changeDetector.markForCheck();
    this.historyChanged.emit();
  }

  requestDelete(task: Task): void {
    if (this.actionsDisabled) return;
    this.clearFeedback();
    this.confirmingDelete = task;
  }

  cancelDelete(): void {
    if (this.busyTaskId !== null) return;
    this.confirmingDelete = null;
    this.feedback = 'Eliminación cancelada. La tarea se conserva.';
  }

  confirmDelete(): void {
    const task = this.confirmingDelete;
    if (!task || this.busyTaskId !== null) return;
    this.busyTaskId = task.id;
    this.error = null;
    this.taskService.deleteTask(task).subscribe({
      next: () => {
        this.tasks = this.tasks.filter((current) => current.id !== task.id);
        this.confirmingDelete = null;
        this.busyTaskId = null;
        this.feedback = `Tarea "${task.title}" eliminada.`;
        this.changeDetector.markForCheck();
        this.historyChanged.emit();
      },
      error: (err) => {
        this.busyTaskId = null;
        this.confirmingDelete = null;
        this.handleError(err);
      },
    });
  }

  formatDuration(seconds?: number): string {
    if (seconds == null || seconds <= 0) {
      return '0m';
    }
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    if (minutes > 0) {
      return `${minutes}m ${secs}s`;
    }
    return `${secs}s`;
  }

  clearFeedback(): void {
    this.feedback = null;
    this.error = null;
  }

  private handleError(err: unknown): void {
    const response = err as { status?: number; error?: { message?: string; error?: string } };
    this.error =
      response?.status === 412
        ? 'La tarea cambió. Actualiza la lista y vuelve a intentarlo.'
        : response?.status === 404
          ? response?.error?.error === 'CATEGORY_NOT_FOUND'
            ? 'La categoría seleccionada ya no está disponible. Actualiza la lista.'
            : 'La tarea ya no está disponible. Actualiza la lista.'
          : (response?.error?.message ?? 'No se pudo completar la operación.');
    this.changeDetector.markForCheck();
  }
}
