import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Project, formatBob, formatMinutes } from '../../../core/models/project';
import { ProjectService, apiErrorMessage } from '../../../core/services/project.service';

/** US1.1 — el Jefe registra una nueva revisión del plan (presupuesto y duración) con su motivo. */
@Component({
  selector: 'app-plan-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './plan-form.html',
})
export class PlanForm implements OnInit {
  @Input({ required: true }) project!: Project;
  @Output() saved = new EventEmitter<Project>();
  @Output() cancelled = new EventEmitter<void>();

  private readonly projects = inject(ProjectService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  readonly form = inject(FormBuilder).nonNullable.group({
    budgetBob: [0, [Validators.required, Validators.min(0), Validators.pattern(/^\d{1,13}(\.\d{1,2})?$/)]],
    estimatedDurationMinutes: [0, [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]],
    reason: ['', [Validators.required, Validators.maxLength(300), Validators.pattern(/\S/)]],
  });

  saving = false;
  error: string | null = null;
  readonly formatBob = formatBob;
  readonly formatMinutes = formatMinutes;

  ngOnInit(): void {
    this.form.patchValue({
      budgetBob: Number(this.project.budgetBob),
      estimatedDurationMinutes: this.project.estimatedDurationMinutes,
    });
  }

  /** Aviso anticipado de AC-US1.1-04; el backend lo vuelve a validar. */
  get belowAllocations(): boolean {
    return Number(this.form.controls.budgetBob.value) < Number(this.project.allocatedBudgetBob);
  }

  save(): void {
    if (this.saving) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid || this.belowAllocations) {
      return;
    }
    const value = this.form.getRawValue();
    this.saving = true;
    this.error = null;
    this.projects
      .replan(this.project, Number(value.budgetBob), Number(value.estimatedDurationMinutes), value.reason.trim())
      .subscribe({
        next: (project) => {
          this.saving = false;
          this.saved.emit(project);
        },
        error: (error) => {
          this.saving = false;
          this.error = apiErrorMessage(error, 'No se pudo registrar el nuevo plan.');
          this.changeDetector.markForCheck();
        },
      });
  }
}
