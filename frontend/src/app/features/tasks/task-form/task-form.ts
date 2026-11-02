import { ChangeDetectorRef, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Task, TaskPriority } from '../../../core/models/task';
import { TaskService } from '../../../core/services/task.service';
import { DurationFormatPipe } from '../../../core/pipes/duration-format.pipe';

@Component({
  selector: 'app-task-form',
  imports: [FormsModule, DurationFormatPipe],
  template: `<div class="task-form">

  <div class="form-header">
    <h2>{{ parentTaskId ? 'Crear nueva subtarea' : 'Crear nueva tarea' }}</h2>
    <p>{{ parentTaskId ? 'Agrega una subtarea vinculada.' : 'Agrega una tarea pendiente a tu lista.' }}</p>
  </div>

  <div class="form-group">
    <label for="title">Título</label>
    <input
      id="title"
      type="text"
      placeholder="Ej. Estudiar para el examen"
      [(ngModel)]="title"
      [disabled]="loading"
    >
    @if (submitted && title.trim() === '') {
      <p class="error-message">
        El título es obligatorio.
      </p>
    }
  </div>

  <!-- ==========================================
       NUEVO: CAMPO DE DURACIÓN ESTIMADA
       ========================================== -->
  <div class="form-group">
    <label for="estimatedDays">Duración estimada (Días)</label>
    <input
      id="estimatedDays"
      type="number"
      min="0"
      placeholder="Ej. 7 para una semana"
      [(ngModel)]="estimatedDays"
      [disabled]="loading"
    >

    <!-- Vista previa en tiempo real usando el nuevo pipe -->
    @if (estimatedDays && estimatedDays > 0) {
      <span style="font-size: 0.85rem; color: #888; margin-top: 4px; display: block;">
        Equivale a: <strong>{{ estimatedDays | durationFormat }}</strong>
      </span>
    }
  </div>

  <!-- Las subtareas (V4) no tienen prioridad ni categoría propias. -->
  @if (!parentTaskId) {
  <div class="form-group">
    <label for="priority">Prioridad</label>
    <select id="priority" [(ngModel)]="selectedPriority" [disabled]="loading">
      <option value="ALTA">Alta</option>
      <option value="MEDIA">Media</option>
      <option value="BAJA">Baja</option>
    </select>
  </div>

  <div class="form-group">
    <label for="category">Se creará en la actividad</label>
    <p id="category" class="task-form-activity" [class.empty]="!activityName">
      @if (activityName) {
        {{ activityName }}
      } @else {
        Elige una etapa y su actividad en la barra de trabajo.
      }
    </p>
  </div>
  }

  @if (error) {
    <p class="error-message">{{ error }}</p>
  }

  <div class="form-actions">
    <button type="button" (click)="crearTarea()" [disabled]="loading">
      {{ loading ? 'Creando...' : (parentTaskId ? 'Crear subtarea' : 'Crear tarea') }}
    </button>

    @if (parentTaskId) {
      <button type="button" class="btn-cancel" (click)="cancelar()" [disabled]="loading">
        Cancelar
      </button>
    }
  </div>

</div>
`,
  styles: [`.task-form {
  width: 100%;
  padding: 30px 32px 32px;
}

/* =========================================================
   FORM HEADER
   ========================================================= */

.form-header {
  margin-bottom: 24px;
}

.form-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 21px;
  line-height: 1.25;
  font-weight: 650;
  letter-spacing: -0.3px;
}

.form-header p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
}

/* =========================================================
   FORM GROUP
   ========================================================= */

.form-group {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-bottom: 18px;
}

.form-group label {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.3;
  font-weight: 600;
  letter-spacing: 0.1px;
}

/* =========================================================
   INPUTS + SELECT
   ========================================================= */

.form-group input,
.form-group select {
  width: 100%;
  height: 44px;
  padding: 0 13px;

  border: 1px solid var(--cometa);
  border-radius: 6px;

  background: var(--espejismo);
  color: var(--text-primary);

  font-family: inherit;
  font-size: 13px;

  transition:
    border-color 0.15s ease,
    background-color 0.15s ease,
    box-shadow 0.15s ease;
}

.form-group input::placeholder {
  color: var(--text-placeholder);
}

.form-group input:hover,
.form-group select:hover {
  border-color: var(--scampi);
  background: var(--espejismo);
}

.form-group input:focus,
.form-group select:focus {
  outline: none;

  border-color: var(--scampi);
  background: var(--espejismo);

  box-shadow:
    0 0 0 1px var(--scampi),
    0 0 0 3px rgba(104, 104, 166, 0.12);
}

/* =========================================================
   SELECT
   ========================================================= */

.form-group select {
  appearance: auto;
  cursor: pointer;
}

.form-group select option {
  background: var(--arcilla-ebano);
  color: var(--text-primary);
}

.form-group select[disabled],
.form-group input[disabled] {
  opacity: 0.55;
  cursor: not-allowed;
}

/* =========================================================
   ACTIVIDAD DESTINO (US2.2)
   ========================================================= */

.task-form-activity {
  display: flex;
  align-items: center;
  min-height: 44px;
  margin: 0;
  padding: 0 13px;

  border: 1px dashed var(--cometa);
  border-radius: 6px;

  background: var(--espejismo);
  color: var(--text-primary);
  font-size: 13px;
}

.task-form-activity.empty {
  border-style: dashed;
  color: var(--text-muted);
}

/* =========================================================
   PRIORITY + CATEGORY
   ========================================================= */

.form-group:nth-of-type(2),
.form-group:nth-of-type(3) {
  width: calc(50% - 8px);
  display: inline-flex;
  vertical-align: top;
}

.form-group:nth-of-type(2) {
  margin-right: 12px;
}

/* =========================================================
   ERROR
   ========================================================= */

.error-message {
  margin: 7px 0 0;
  color: var(--priority-baja);
  font-size: 12px;
  line-height: 1.4;
}

/* =========================================================
   ACTIONS
   ========================================================= */

.form-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 24px;
}

.form-actions button {
  min-height: 40px;
  padding: 0 18px;

  border: 1px solid var(--scampi);
  border-radius: 6px;

  background: var(--scampi);
  color: #ffffff;

  font-family: inherit;
  font-size: 13px;
  font-weight: 600;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.1s ease;
}

.form-actions button:hover:not(:disabled) {
  background: var(--cometa);
  border-color: var(--scampi);

  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.18);
}

.form-actions button:focus-visible {
  outline: 2px solid rgba(104, 104, 166, 0.65);
  outline-offset: 2px;
}

.form-actions button:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: none;
}

.form-actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* =========================================================
   CANCEL BUTTON
   ========================================================= */

.form-actions .btn-cancel {
  border-color: var(--cometa);
  background: transparent;
  color: var(--text-secondary);
}

.form-actions .btn-cancel:hover:not(:disabled) {
  background: var(--martinica);
  border-color: var(--scampi);
  color: var(--text-primary);
  box-shadow: none;
}

/* =========================================================
   RESPONSIVE
   ========================================================= */

@media (max-width: 900px) {
  .task-form {
    padding: 28px 26px 30px;
  }
}

@media (max-width: 650px) {
  .task-form {
    padding: 26px 22px 28px;
  }

  .form-header h2 {
    font-size: 20px;
  }

  .form-group:nth-of-type(2),
  .form-group:nth-of-type(3) {
    width: 100%;
    display: flex;
    margin-right: 0;
  }

  .form-actions {
    flex-wrap: wrap;
  }

  .form-actions button {
    flex: 1;
  }
}`],
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
