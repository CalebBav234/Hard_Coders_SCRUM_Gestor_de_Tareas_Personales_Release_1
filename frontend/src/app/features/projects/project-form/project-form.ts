import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Project, UserSummary, formatMinutes } from '../../../core/models/project';
import { ProjectService, apiErrorMessage } from '../../../core/services/project.service';

/**
 * Crear proyecto (solo Jefe, US1.1) o editar nombre y descripción (Jefe o Encargado).
 * En modo edición no se muestran presupuesto ni Encargado: tienen sus propios flujos con historial.
 */
@Component({
  selector: 'app-project-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `<form [formGroup]="form" (ngSubmit)="save()" novalidate>
  <div class="gt-modal-body">
    <div class="gt-field">
      <label for="project-name">Nombre</label>
      <input id="project-name" formControlName="name" maxlength="160" />
      @if (form.controls.name.touched && form.controls.name.invalid) {
        <small class="gt-field-error">El nombre es obligatorio (máximo 160 caracteres).</small>
      }
    </div>

    <div class="gt-field">
      <label for="project-description">Descripción</label>
      <textarea id="project-description" formControlName="description" maxlength="2000"></textarea>
    </div>

    @if (!isEdit) {
      <div class="gt-field">
        <label for="project-manager">Encargado</label>
        <select id="project-manager" formControlName="managerUserId">
          <option [value]="0" disabled>Selecciona un encargado</option>
          @for (manager of managers; track manager.userId) {
            <option [value]="manager.userId">{{ manager.fullName }}{{ manager.chief ? ' (Jefe)' : '' }}</option>
          }
        </select>
        @if (form.controls.managerUserId.touched && form.controls.managerUserId.value < 1) {
          <small class="gt-field-error">Elige quién dirigirá el proyecto.</small>
        }
      </div>

      <div class="gt-field-row">
        <div class="gt-field">
          <label for="project-budget">Presupuesto (Bs)</label>
          <input id="project-budget" type="number" min="0" step="0.01" formControlName="budgetBob" />
          @if (form.controls.budgetBob.touched && form.controls.budgetBob.invalid) {
            <small class="gt-field-error">Importe mayor o igual a 0, con hasta 2 decimales.</small>
          }
        </div>
        <div class="gt-field">
          <label for="project-duration">Duración estimada (minutos)</label>
          <input id="project-duration" type="number" min="0" step="1" formControlName="estimatedDurationMinutes" />
          <small class="gt-field-hint">≈ {{ formatMinutes(form.controls.estimatedDurationMinutes.value) }}</small>
        </div>
      </div>

      <div class="gt-field">
        <label for="project-reason">Motivo del plan inicial (opcional)</label>
        <input id="project-reason" formControlName="reason" maxlength="300" placeholder="Plan inicial" />
      </div>
    }

    @if (error) {
      <p class="gt-alert gt-alert-error" role="alert">{{ error }}</p>
    }
  </div>

  <div class="gt-modal-actions">
    <button type="button" class="gt-btn" (click)="cancelled.emit()" [disabled]="saving">Cancelar</button>
    <button type="submit" class="gt-btn gt-btn-primary" [disabled]="saving">
      {{ saving ? 'Guardando…' : (isEdit ? 'Guardar cambios' : 'Crear proyecto') }}
    </button>
  </div>
</form>
`,
})
export class ProjectForm implements OnInit {
  @Input() project: Project | null = null;
  @Output() saved = new EventEmitter<Project>();
  @Output() cancelled = new EventEmitter<void>();

  private readonly projects = inject(ProjectService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(160), Validators.pattern(/\S/)]],
    description: ['', Validators.maxLength(2000)],
    managerUserId: [0, Validators.min(1)],
    budgetBob: [0, [Validators.required, Validators.min(0), Validators.pattern(/^\d{1,13}(\.\d{1,2})?$/)]],
    estimatedDurationMinutes: [0, [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]],
    reason: ['', Validators.maxLength(300)],
  });

  managers: UserSummary[] = [];
  saving = false;
  error: string | null = null;
  readonly formatMinutes = formatMinutes;

  get isEdit(): boolean {
    return this.project !== null;
  }

  ngOnInit(): void {
    if (this.project) {
      this.form.patchValue({ name: this.project.name, description: this.project.description ?? '' });
      return;
    }
    this.projects.eligibleManagers().subscribe({
      next: (managers) => {
        this.managers = managers;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.error = apiErrorMessage(error, 'No se pudieron cargar los encargados.');
        this.changeDetector.markForCheck();
      },
    });
  }

  save(): void {
    if (this.saving) {
      return;
    }
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    const createInvalid = !this.isEdit && (this.form.invalid || value.managerUserId < 1);
    const editInvalid = this.isEdit && (this.form.controls.name.invalid || this.form.controls.description.invalid);
    if (createInvalid || editInvalid) {
      return;
    }
    const description = value.description.trim() === '' ? null : value.description.trim();
    this.saving = true;
    this.error = null;
    const request = this.project
      ? this.projects.updateDetails(this.project, value.name.trim(), description)
      : this.projects.create({
          name: value.name.trim(),
          description,
          managerUserId: Number(value.managerUserId),
          budgetBob: Number(value.budgetBob),
          estimatedDurationMinutes: Number(value.estimatedDurationMinutes),
          reason: value.reason.trim() === '' ? null : value.reason.trim(),
        });
    request.subscribe({
      next: (project) => {
        this.saving = false;
        this.saved.emit(project);
      },
      error: (error) => {
        this.saving = false;
        this.error = apiErrorMessage(error, 'No se pudo guardar el proyecto.');
        this.changeDetector.markForCheck();
      },
    });
  }
}
