import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import {
  Task,
  TaskPriority,
  isSubtask,
} from '../../../core/models/task';

import { TaskService } from '../../../core/services/task.service';

@Component({
  selector: 'app-task-editor',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `<form
  class="task-editor-form"
  [formGroup]="form"
  (ngSubmit)="save()"
  novalidate
>

  <!-- TÍTULO -->

  <div class="editor-field">
    <label for="task-title">
      Título
    </label>

    <input
      id="task-title"
      type="text"
      formControlName="title"
      placeholder="Escribe el título de la tarea"
      autocomplete="off"
    />

    @if (
      form.controls.title.invalid &&
      form.controls.title.touched
    ) {
      <small class="field-error">
        El título es obligatorio y debe tener un máximo de 160 caracteres.
      </small>
    }
  </div>


  <!-- DESCRIPCIÓN -->

  <div class="editor-field">
    <label for="task-description">
      Descripción
    </label>

    <textarea
      id="task-description"
      formControlName="description"
      rows="5"
      placeholder="Describe la tarea..."
    ></textarea>

    @if (
      form.controls.description.invalid &&
      form.controls.description.touched
    ) {
      <small class="field-error">
        La descripción no puede superar los 4000 caracteres.
      </small>
    }
  </div>


  <!-- Las subtareas (V4) no tienen prioridad ni categoría propias. -->
  @if (!isSubtask) {
  <!-- PRIORIDAD + CATEGORÍA -->

  <div class="editor-fields-row">

    <!-- PRIORIDAD -->

    <div class="editor-field">
      <label for="task-priority">
        Prioridad
      </label>

      <select
        id="task-priority"
        formControlName="priority"
      >
        <option value="ALTA">
          Alta
        </option>

        <option value="MEDIA">
          Media
        </option>

        <option value="BAJA">
          Baja
        </option>
      </select>
    </div>


    <!-- CATEGORÍA -->

    <div class="editor-field">
      <label for="task-category">
        Categoría
      </label>

      <select
        id="task-category"
        formControlName="categoryName"
      >

        <option value="">
          Sin categoría
        </option>

        @for (category of categories; track category.id) {
          <option [value]="category.name">
            {{ category.name }}
          </option>
        }

      </select>

      @if (loadingCategories) {
        <small class="field-hint">
          Cargando categorías...
        </small>
      }

      @if (categoryError) {
        <small class="field-error">
          {{ categoryError }}
        </small>
      }
    </div>

  </div>
  }


  <!-- ERROR GENERAL -->

  @if (error) {
    <div class="editor-error">
      {{ error }}
    </div>
  }


  <!-- ACCIONES -->

  <div class="editor-actions">

    <button
      type="button"
      class="btn-editor-cancel"
      (click)="cancel()"
      [disabled]="saving"
    >
      Cancelar
    </button>

    <button
      type="submit"
      class="btn-editor-save"
      [disabled]="saving || form.invalid"
    >
      @if (saving) {
        Guardando...
      } @else {
        Guardar cambios
      }
    </button>

  </div>

</form>`,
  styles: [`:host {
  display: block;
  width: 100%;
}


/* =========================================================
   FORMULARIO
   ========================================================= */

.task-editor-form {
  width: 100%;
  max-width: 760px;

  margin: 0 auto;
  padding: 4px 6px 6px;

  box-sizing: border-box;
}


/* =========================================================
   CAMPO
   ========================================================= */

.editor-field {
  width: 100%;
  margin-bottom: 18px;
}

.editor-field label {
  display: block;

  margin: 0 0 7px;

  color: var(--text-secondary);

  font-size: 11px;
  line-height: 1.4;
  font-weight: 650;
}


/* =========================================================
   INPUTS / TEXTAREA / SELECT
   ========================================================= */

.editor-field input,
.editor-field textarea,
.editor-field select {
  width: 100%;

  box-sizing: border-box;

  border: 1px solid var(--martinica);
  border-radius: 7px;

  background: #18182f;
  color: var(--text-primary);

  font-family: inherit;
  font-size: 12px;

  outline: none;

  transition:
    border-color 0.15s ease,
    background-color 0.15s ease,
    box-shadow 0.15s ease;
}


/* INPUT */

.editor-field input,
.editor-field select {
  min-height: 40px;

  padding: 0 11px;
}


/* TEXTAREA */

.editor-field textarea {
  min-height: 110px;

  padding: 10px 11px;

  resize: vertical;

  line-height: 1.5;
}


/* PLACEHOLDER */

.editor-field input::placeholder,
.editor-field textarea::placeholder {
  color: var(--text-placeholder);
}


/* HOVER */

.editor-field input:hover,
.editor-field textarea:hover,
.editor-field select:hover {
  border-color: var(--cometa);
}


/* FOCUS */

.editor-field input:focus,
.editor-field textarea:focus,
.editor-field select:focus {
  border-color: var(--scampi);

  background: #1b1b35;

  box-shadow:
    0 0 0 2px rgba(104, 104, 166, 0.12);
}


/* DISABLED */

.editor-field input:disabled,
.editor-field textarea:disabled,
.editor-field select:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}


/* =========================================================
   FILA PRIORIDAD / CATEGORÍA
   ========================================================= */

.editor-fields-row {
  display: grid;

  grid-template-columns:
    repeat(2, minmax(0, 1fr));

  gap: 14px;

  width: 100%;
}


/* =========================================================
   AYUDA
   ========================================================= */

.field-hint {
  display: block;

  margin-top: 6px;

  color: var(--text-muted);

  font-size: 10px;
  line-height: 1.4;
}


/* =========================================================
   ERRORES
   ========================================================= */

.field-error {
  display: block;

  margin-top: 6px;

  color: #e49a9a;

  font-size: 10px;
  line-height: 1.4;
}

.editor-error {
  margin: 4px 0 18px;

  padding: 10px 12px;

  border: 1px solid #98505e;
  border-radius: 7px;

  background: #4a252d;

  color: #ffffff;

  font-size: 11px;
  line-height: 1.5;
}


/* =========================================================
   ACCIONES
   ========================================================= */

.editor-actions {
  display: flex;

  justify-content: flex-end;
  align-items: center;

  gap: 10px;

  margin-top: 24px;
  padding: 16px 4px 4px;

  border-top: 1px solid var(--martinica);
}


/* =========================================================
   BOTONES
   ========================================================= */

.editor-actions button {
  min-height: 40px;

  padding: 0 18px;

  border-radius: 7px;

  font-family: inherit;

  font-size: 11px;
  font-weight: 650;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    transform 0.1s ease,
    opacity 0.15s ease;
}


/* CANCELAR */

.btn-editor-cancel {
  border: 1px solid var(--martinica);

  background: transparent;

  color: var(--text-secondary);
}

.btn-editor-cancel:hover:not(:disabled) {
  background: var(--martinica);

  border-color: var(--cometa);

  color: var(--text-primary);
}


/* GUARDAR */

.btn-editor-save {
  border: 1px solid var(--scampi);

  background: var(--scampi);

  color: #ffffff;
}

.btn-editor-save:hover:not(:disabled) {
  background: var(--cometa);

  border-color: var(--cometa);

  transform: translateY(-1px);
}

.btn-editor-save:active:not(:disabled) {
  transform: translateY(0);
}


/* DISABLED */

.editor-actions button:disabled {
  opacity: 0.45;

  cursor: not-allowed;

  transform: none;
}


/* =========================================================
   RESPONSIVE
   ========================================================= */

@media (max-width: 650px) {

  .task-editor-form {
    max-width: none;
  }

  .editor-fields-row {
    grid-template-columns: 1fr;

    gap: 0;
  }

  .editor-actions {
    display: grid;

    grid-template-columns:
      1fr 1fr;

    gap: 8px;
  }

  .editor-actions button {
    width: 100%;
  }

}`],
})
export class TaskEditor implements OnInit {

  @Input({ required: true }) task!: Task;

  @Output() saved = new EventEmitter<Task>();
  @Output() cancelled = new EventEmitter<void>();

  private readonly taskService = inject(TaskService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly formBuilder = inject(FormBuilder);

  readonly form = this.formBuilder.nonNullable.group({
    title: [
      '',
      [
        Validators.required,
        Validators.maxLength(160),
        Validators.pattern(/\S/),
      ],
    ],

    description: [
      '',
      Validators.maxLength(4000),
    ],

    priority: [
      'MEDIA' as TaskPriority,
      [
        Validators.required,
        Validators.pattern(/^(ALTA|MEDIA|BAJA)$/),
      ],
    ],

    categoryName: [''],
  });

  categories: Array<{ id: number; name: string }> = [];

  /** Etapa cuyas actividades se ofrecen como "categorías" de la tarea. */
  stageIdForCategories = 0;

  /** Id de la actividad que corresponde al nombre elegido (se resuelve al guardar). */
  private categoryActivityId(categoryName: string | null): number {
    const found = this.categories.find((category) => category.name === categoryName);
    return found ? found.id : 0;
  }

  loadingCategories = false;
  saving = false;

  error: string | null = null;
  categoryError: string | null = null;

  /** Las subtareas (V4) no tienen prioridad ni categoría propias. */
  get isSubtask(): boolean {
    return isSubtask(this.task);
  }

  ngOnInit(): void {
    this.form.setValue({
      title: this.task.title,
      description: this.task.description ?? '',
      priority: this.task.priority,
      categoryName: this.task.categoryName ?? '',
    });

    if (!this.isSubtask) {
      this.loadCategories();
    }
  }

  loadCategories(): void {
    this.loadingCategories = true;
    this.categoryError = null;

    this.taskService.listCategories(this.stageIdForCategories).subscribe({
      next: (categories) => {
        this.categories = categories;
        this.loadingCategories = false;

        this.changeDetector.markForCheck();
      },

      error: () => {
        this.loadingCategories = false;
        this.categoryError =
          'No se pudieron cargar las categorías.';

        this.changeDetector.markForCheck();
      },
    });
  }

  save(): void {
    if (this.saving) {
      return;
    }

    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    const values = this.form.getRawValue();

    const categoryName =
      values.categoryName.trim() === ''
        ? null
        : values.categoryName.trim();

    this.saving = true;
    this.error = null;

    this.form.disable();

    const {
      categoryName: _categoryName,
      ...taskValues
    } = values;

    this.taskService
      .updateTask(this.task.id, {
        ...taskValues,
        title: taskValues.title.trim(),
        description:
          taskValues.description.trim() === ''
            ? null
            : taskValues.description.trim(),
        priority: taskValues.priority,
        version: this.task.version,
      })
      .subscribe({
        next: (updatedTask) => {

          const categoryChanged =
            !this.isSubtask &&
            (this.task.categoryName ?? null) !== categoryName;

          if (!categoryChanged) {
            this.saving = false;
            this.saved.emit(updatedTask);

            this.changeDetector.markForCheck();
            return;
          }

          this.taskService
            .changeActivity(updatedTask, this.categoryActivityId(categoryName))
            .subscribe({
              next: (categoryUpdatedTask) => {
                const finalTask: Task = {
                  ...categoryUpdatedTask,
                  categoryName: categoryName,
                };

                this.saving = false;
                this.saved.emit(finalTask);

                this.changeDetector.markForCheck();
              },

              error: () => {
                // Los datos ya se guardaron; solo falló el cambio de categoría.
                this.saving = false;
                this.form.enable();
                this.error =
                  'Se guardaron los cambios, pero no se pudo actualizar la categoría.';

                this.changeDetector.markForCheck();
              },
            });
        },

        error: (err) => {
          this.saving = false;
          this.form.enable();

          if (err.status === 412) {
            this.error =
              'La tarea cambió mientras la editabas. Conserva tu texto, cancela y actualiza la lista antes de volver a editar.';
          } else if (err.status === 404) {
            this.error =
              'Esta tarea ya no está disponible. Cancela y actualiza la lista.';
          } else {
            this.error =
              err.error?.message ??
              'No se pudieron guardar los cambios. Inténtalo de nuevo.';
          }

          this.changeDetector.markForCheck();
        },
      });
  }

  cancel(): void {
    if (!this.saving) {
      this.cancelled.emit();
    }
  }
}