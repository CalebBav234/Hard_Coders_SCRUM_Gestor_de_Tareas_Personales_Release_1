import { ChangeDetectorRef, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Task, TaskPriority } from '../../../core/models/task';
import { TaskService } from '../../../core/services/task.service';
import { DurationFormatPipe } from '../../../core/pipes/duration-format.pipe';

@Component({
  selector: 'app-task-form',
  imports: [FormsModule, DurationFormatPipe],
  templateUrl: './task-form.html',
  styleUrl: './task-form.css',
})
export class TaskForm {
  private readonly taskService = inject(TaskService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  @Input() parentTaskId: number | null = null;
  @Input() activityId: number | null = null;
  @Input() activityName: string | null = null;
  @Output() taskCreated = new EventEmitter<Task>();
  @Output() cancelled = new EventEmitter<void>();

  cancelar(): void {
    this.cancelled.emit();
  }

  title = '';
  selectedPriority: TaskPriority = 'MEDIA';

  // NUEVO: Variable para capturar los días estimados
  estimatedDays: number | null = null;

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

    if (this.parentTaskId === null && this.activityId === null) {
      this.loading = false;
      this.error = 'Elige una etapa y su actividad en la barra de trabajo antes de crear la tarea.';
      this.changeDetector.markForCheck();
      return;
    }

    // Asegurarnos de mandar un número entero válido (o 0 si está vacío)
    const duration = this.estimatedDays && this.estimatedDays > 0 ? Math.floor(this.estimatedDays) : 0;

    this.taskService
      .createTask(
        this.activityId ?? 0,
        this.title.trim(),
        this.selectedPriority,
        duration, // <-- CAMBIADO: Enviamos la duración ingresada en lugar del primer 0
        0,        // (Asumo que este segundo 0 es para otra métrica, como presupuesto o horas gastadas)
        this.parentTaskId
      )
      .subscribe({
        next: (task) => {
          this.title = '';
          this.selectedPriority = 'MEDIA';
          this.estimatedDays = null; // Limpiar el input
          this.submitted = false;
          this.loading = false;

          const createdTask: Task = {
            ...task,
            parentTaskId: this.parentTaskId ?? task.parentTaskId
          };

          this.taskCreated.emit(createdTask);
          this.changeDetector.markForCheck();
        },
        error: (err) => {
          this.loading = false;
          this.error = err?.error?.message ?? 'No se pudo crear la tarea.';
          this.changeDetector.markForCheck();
        },
      });
  }
}
