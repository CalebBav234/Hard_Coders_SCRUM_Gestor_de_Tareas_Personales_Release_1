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
} from '../../../core/models/task';

import { TaskService } from '../../../core/services/task.service';

@Component({
  selector: 'app-task-editor',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './task-editor.html',
  styleUrl: './task-editor.css',
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

  loadingCategories = false;
  saving = false;

  error: string | null = null;
  categoryError: string | null = null;

  ngOnInit(): void {
    this.form.setValue({
      title: this.task.title,
      description: this.task.description ?? '',
      priority: this.task.priority,
      categoryName: this.task.categoryName ?? '',
    });

    this.loadCategories();
  }

  loadCategories(): void {
    this.loadingCategories = true;
    this.categoryError = null;

    this.taskService.listCategories().subscribe({
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
            (this.task.categoryName ?? null) !== categoryName;

          if (!categoryChanged) {
            this.saving = false;
            this.saved.emit(updatedTask);

            this.changeDetector.markForCheck();
            return;
          }

          this.taskService
            .changeCategory(updatedTask, categoryName)
            .subscribe({
              next: () => {
                const finalTask: Task = {
                  ...updatedTask,
                  categoryName: categoryName,
                };

                this.saving = false;
                this.saved.emit(finalTask);

                this.changeDetector.markForCheck();
              },

              error: (err) => {
                this.saving = false;
                this.form.enable();

                this.error =
                  err.error?.message ??
                  'La tarea se guardó, pero no se pudo actualizar la categoría.';

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