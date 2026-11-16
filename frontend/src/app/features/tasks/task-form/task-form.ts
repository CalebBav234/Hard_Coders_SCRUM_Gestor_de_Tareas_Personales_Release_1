import { ChangeDetectorRef, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Task, TaskPriority } from '../../../core/models/task';
import { TaskService } from '../../../core/services/task.service';
import { formatMinutes } from '../../../core/models/project';

/** Mismo patrón que project-form y que @Digits(integer = 13, fraction = 2) del backend. */
const BUDGET_PATTERN = /^\d{1,13}(\.\d{1,2})?$/;
/** task.estimated_duration_minutes es integer (@Max(Integer.MAX_VALUE) en el backend). */
const MAX_DURATION_MINUTES = 2_147_483_647;

@Component({
  selector: 'app-task-form',
  imports: [FormsModule],
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

  <!-- Las subtareas (V4) no tienen prioridad, presupuesto, duración ni categoría propias. -->
  @if (!parentTaskId) {
  <div class="form-group">
    <label for="estimatedHours">Duración estimada (horas)</label>
    <input
      id="estimatedHours"
      type="number"
      min="0"
      step="0.25"
      placeholder="Ej. 1.5 para una hora y media"
      [(ngModel)]="estimatedHours"
      [disabled]="loading"
    >
    @if (durationError) {
      <p class="error-message">{{ durationError }}</p>
    } @else if (estimatedMinutes() > 0) {
      <small class="gt-field-hint">≈ {{ formatMinutes(estimatedMinutes()) }} de jornada</small>
    }
  </div>

  <div class="form-group">
    <label for="allocatedBudgetBob">Presupuesto asignado (Bs)</label>
    <input
      id="allocatedBudgetBob"
      type="number"
      min="0"
      step="0.01"
      placeholder="Ej. 250.50"
      [(ngModel)]="allocatedBudgetBob"
      [disabled]="loading"
    >
    @if (budgetError) {
      <p class="error-message">{{ budgetError }}</p>
    }
  </div>

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

  /**
   * EC3-04: las tareas de un trabajador se estiman en horas de jornada (paso de 15 min) y se envían
   * al backend en minutos enteros, la unidad canónica de task.estimated_duration_minutes.
   */
  estimatedHours: number | null = null;
  /** EC3-04: presupuesto asignado a la tarea; mismas reglas que task.allocated_budget_bob DECIMAL(15,2) ≥ 0. */
  allocatedBudgetBob: number | null = null;

  submitted = false;
  loading = false;
  error: string | null = null;
  durationError: string | null = null;
  budgetError: string | null = null;

  readonly formatMinutes = formatMinutes;

  /** Minutos que se enviarán; 0 si el campo está vacío o es inválido. */
  estimatedMinutes(): number {
    const hours = Number(this.estimatedHours);
    if (this.estimatedHours === null || !Number.isFinite(hours) || hours < 0) {
      return 0;
    }
    return Math.round(hours * 60);
  }

  crearTarea(): void {
    this.submitted = true;
    this.error = null;
    this.durationError = null;
    this.budgetError = null;

    const isSubtask = this.parentTaskId !== null;
    if (!isSubtask) {
      this.durationError = this.validateHours();
      this.budgetError = this.validateBudget();
    }

    if (this.title.trim() === '' || this.durationError !== null || this.budgetError !== null) {
      this.changeDetector.markForCheck();
      return;
    }

    if (!isSubtask && this.activityId === null) {
      this.error = 'Elige una etapa y su actividad en la barra de trabajo antes de crear la tarea.';
      this.changeDetector.markForCheck();
      return;
    }

    this.loading = true;
    const budget = isSubtask || this.allocatedBudgetBob === null ? 0 : Number(this.allocatedBudgetBob);
    const minutes = isSubtask ? 0 : this.estimatedMinutes();

    this.taskService
      .createTask(this.activityId ?? 0, this.title.trim(), this.selectedPriority, budget, minutes, this.parentTaskId)
      .subscribe({
        next: (task) => {
          this.title = '';
          this.selectedPriority = 'MEDIA';
          this.estimatedHours = null;
          this.allocatedBudgetBob = null;
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

  private validateHours(): string | null {
    if (this.estimatedHours === null || (this.estimatedHours as unknown) === '') {
      return null;
    }
    const hours = Number(this.estimatedHours);
    if (!Number.isFinite(hours) || hours < 0) {
      return 'La duración debe ser un número mayor o igual a 0.';
    }
    const quarterHours = hours / 0.25;
    if (Math.abs(quarterHours - Math.round(quarterHours)) > 1e-9) {
      return 'La duraci\u00f3n debe usar incrementos de 0.25 h (15 min).';
    }
    if (Math.round(hours * 60) > MAX_DURATION_MINUTES) {
      return 'La duración estimada es demasiado grande.';
    }
    return null;
  }

  private validateBudget(): string | null {
    if (this.allocatedBudgetBob === null || (this.allocatedBudgetBob as unknown) === '') {
      return null;
    }
    const budget = Number(this.allocatedBudgetBob);
    if (!Number.isFinite(budget) || budget < 0) {
      return 'El presupuesto debe ser mayor o igual a 0.';
    }
    if (!BUDGET_PATTERN.test(String(this.allocatedBudgetBob))) {
      return 'El presupuesto admite hasta 13 dígitos enteros y 2 decimales.';
    }
    return null;
  }
}
