import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Task, TaskPriority, TaskRelationResponse, RelationType } from '../../../core/models/task';
import { TaskService } from '../../../core/services/task.service';

@Component({
  selector: 'app-task-editor',
  imports: [ReactiveFormsModule],
  templateUrl: './task-editor.html',
  styleUrl: './task-editor.css',
})
export class TaskEditor implements OnInit {
  @Input({ required: true }) task!: Task;
  @Output() saved = new EventEmitter<Task>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() navigateToTask = new EventEmitter<number>();

  private readonly taskService = inject(TaskService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly formBuilder = inject(FormBuilder);

  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(160), Validators.pattern(/\S/)]],
    description: ['', Validators.maxLength(4000)],
    priority: [
      'MEDIA' as TaskPriority,
      [Validators.required, Validators.pattern(/^(ALTA|MEDIA|BAJA)$/)],
    ],
  });

  readonly relationForm = this.formBuilder.nonNullable.group({
    targetTaskId: [0, [Validators.required, Validators.min(1)]],
    relationType: ['RELACIONADA' as RelationType, Validators.required]
  });

  saving = false;
  error: string | null = null;

  relations: TaskRelationResponse[] = [];
  loadingRelations = false;
  relationError: string | null = null;
  addingRelation = false;

  ngOnInit(): void {
    this.form.setValue({
      title: this.task.title,
      description: this.task.description ?? '',
      priority: this.task.priority,
    });
    this.loadRelations();
  }


  loadRelations(): void {
    this.loadingRelations = true;
    this.taskService.getRelations(this.task.id).subscribe({
      next: (relations) => {
        this.relations = relations;
        this.loadingRelations = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.loadingRelations = false;
        this.relationError = 'No se pudieron cargar las relaciones.';
        this.changeDetector.markForCheck();
      }
    });
  }

  addRelation(): void {
    if (this.relationForm.invalid || this.addingRelation) return;

    this.addingRelation = true;
    this.relationError = null;
    const values = this.relationForm.getRawValue();

    this.taskService.addRelation(this.task.id, {
      targetTaskId: values.targetTaskId,
      relationType: values.relationType
    }).subscribe({
      next: (newRelation) => {
        this.relations = [...this.relations, newRelation];
        this.relationForm.reset({ targetTaskId: 0, relationType: 'RELACIONADA' });
        this.addingRelation = false;
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.addingRelation = false;
        this.relationError = err.error?.message || 'Error al añadir relación. Verifica el ID.';
        this.changeDetector.markForCheck();
      }
    });
  }

  removeRelation(relationId: number): void {
    this.relationError = null;
    this.taskService.removeRelation(this.task.id, relationId).subscribe({
      next: () => {
        this.relations = this.relations.filter(r => r.id !== relationId);
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.relationError = 'Error al eliminar la relación.';
        this.changeDetector.markForCheck();
      }
    });
  }

  navigate(targetTaskId: number): void {
    this.navigateToTask.emit(targetTaskId);
  }


  save(): void {
    if (this.saving) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const values = this.form.getRawValue();
    this.saving = true;
    this.error = null;
    this.form.disable();
    this.taskService
      .updateTask(this.task.id, {
        ...values,
        title: values.title.trim(),
        description: values.description || null,
        version: this.task.version,
      })
      .subscribe({
        next: (task) => {
          this.saving = false;
          this.saved.emit(task);
          this.changeDetector.markForCheck();
        },
        error: (err) => {
          this.saving = false;
          this.form.enable();
          if (err.status === 412) {
            this.error =
              'La tarea cambió mientras la editabas. Conserva tu texto, cancela y actualiza la lista antes de volver a editar.';
          } else if (err.status === 404) {
            this.error = 'Esta tarea ya no está disponible. Cancela y actualiza la lista.';
          } else {
            this.error =
              err.error?.message ?? 'No se pudieron guardar los cambios. Inténtalo de nuevo.';
          }
          this.changeDetector.markForCheck();
        },
      });
  }

  cancel(): void {
    if (!this.saving) this.cancelled.emit();
  }
}
