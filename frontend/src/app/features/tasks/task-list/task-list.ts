import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
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
    this.taskService.listTasks().subscribe({
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

  showCreatedTask(task: Task): void {
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
        this.loadTasks();
      },
      error: (err) => {
        this.busyTaskId = null;
        this.handleError(err);
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
    const response = err as { status?: number; error?: { message?: string } };
    this.error =
      response?.status === 412
        ? 'La tarea cambió. Actualiza la lista y vuelve a intentarlo.'
        : response?.status === 404
          ? 'La tarea ya no está disponible. Actualiza la lista.'
          : (response?.error?.message ?? 'No se pudo completar la operación.');
    this.changeDetector.markForCheck();
  }
}
