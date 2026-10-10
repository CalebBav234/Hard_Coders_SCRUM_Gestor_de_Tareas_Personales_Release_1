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
  templateUrl: './project-form.html',
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
