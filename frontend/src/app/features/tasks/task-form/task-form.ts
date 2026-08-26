import { Component, EventEmitter, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TaskService } from '../../../core/services/task.service';

@Component({
  selector: 'app-task-form',
  imports: [FormsModule],
  templateUrl: './task-form.html',
  styleUrl: './task-form.css'
})
export class TaskForm {
  private readonly taskService = inject(TaskService);

  @Output() taskCreated = new EventEmitter<void>();

  title = '';
  submitted = false;
  loading = false;
  error: string | null = null;

  crearTarea(): void {
    this.submitted = true;
    this.error = null;

    if (this.title.trim() === '') {
      return;
    }

    this.loading = true;
    this.taskService.createTask(this.title.trim()).subscribe({
      next: () => {
        this.title = '';
        this.submitted = false;
        this.loading = false;
        this.taskCreated.emit();
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message ?? 'No se pudo crear la tarea.';
      }
    });
  }
}
