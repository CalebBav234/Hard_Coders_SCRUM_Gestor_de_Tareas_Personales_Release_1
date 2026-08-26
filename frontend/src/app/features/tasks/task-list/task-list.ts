import { Component, OnInit, inject } from '@angular/core';
import { Task } from '../../../core/models/task';
import { TaskService } from '../../../core/services/task.service';

@Component({
  selector: 'app-task-list',
  imports: [],
  templateUrl: './task-list.html',
  styleUrl: './task-list.css'
})
export class TaskList implements OnInit {
  private readonly taskService = inject(TaskService);

  tasks: Task[] = [];
  loading = false;
  error: string | null = null;
  feedback: string | null = null;

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.error = null;
    this.taskService.listTasks().subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudo cargar la lista de tareas.';
      }
    });
  }

  activate(task: Task): void {
    this.taskService.activate(task).subscribe({
      next: () => {
        this.feedback = `Tarea "${task.title}" activada.`;
        this.loadTasks();
      },
      error: (err) => this.handleError(err)
    });
  }

  complete(task: Task): void {
    this.taskService.complete(task).subscribe({
      next: () => {
        this.feedback = `Tarea "${task.title}" completada.`;
        this.loadTasks();
      },
      error: (err) => this.handleError(err)
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
    const message = (err as { error?: { message?: string } })?.error?.message;
    this.error = message ?? 'No se pudo completar la operación.';
  }
}
