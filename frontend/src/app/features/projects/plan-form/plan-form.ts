import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Project, formatBob, formatMinutes } from '../../../core/models/project';
import { ProjectService, apiErrorMessage } from '../../../core/services/project.service';

/** US1.1 — el Jefe registra una nueva revisión del plan (presupuesto y duración) con su motivo. */
@Component({
  selector: 'app-plan-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `<form [formGroup]="form" (ngSubmit)="save()" novalidate>
  <div class="gt-modal-body">
    <p class="gt-field-hint">
      Plan vigente (revisión {{ project.currentRevision }}): {{ formatBob(project.budgetBob) }} ·
      {{ formatMinutes(project.estimatedDurationMinutes) }}. Asignado a tareas: {{ formatBob(project.allocatedBudgetBob) }}.
    </p>

    <div class="gt-field-row">
      <div class="gt-field">
        <label for="plan-budget">Nuevo presupuesto (Bs)</label>
        <input id="plan-budget" type="number" min="0" step="0.01" formControlName="budgetBob" />
        @if (form.controls.budgetBob.touched && form.controls.budgetBob.invalid) {
          <small class="gt-field-error">Importe mayor o igual a 0, con hasta 2 decimales.</small>
        } @else if (belowAllocations) {
          <small class="gt-field-error">No puede ser menor que lo ya asignado a las tareas.</small>
        }
      </div>
      <div class="gt-field">
        <label for="plan-duration">Duración estimada (minutos)</label>
        <input id="plan-duration" type="number" min="0" step="1" formControlName="estimatedDurationMinutes" />
        <small class="gt-field-hint">≈ {{ formatMinutes(form.controls.estimatedDurationMinutes.value) }}</small>
      </div>
    </div>

    <div class="gt-field">
      <label for="plan-reason">Motivo del cambio</label>
      <textarea id="plan-reason" formControlName="reason" maxlength="300"></textarea>
      @if (form.controls.reason.touched && form.controls.reason.invalid) {
        <small class="gt-field-error">El motivo es obligatorio: queda en el historial del plan.</small>
      }
    </div>

    @if (error) {
      <p class="gt-alert gt-alert-error" role="alert">{{ error }}</p>
    }
  </div>

  <div class="gt-modal-actions">
    <button type="button" class="gt-btn" (click)="cancelled.emit()" [disabled]="saving">Cancelar</button>
    <button type="submit" class="gt-btn gt-btn-primary" [disabled]="saving">
      {{ saving ? 'Guardando…' : 'Registrar revisión' }}
    </button>
  </div>
</form>
`,
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
