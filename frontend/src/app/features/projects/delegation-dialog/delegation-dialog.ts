import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Project, UserSummary } from '../../../core/models/project';
import { ProjectService, apiErrorMessage } from '../../../core/services/project.service';

/** US1.2 — el Jefe delega o reasigna el Encargado; la delegación queda en el historial del plan. */
@Component({
  selector: 'app-delegation-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `<form [formGroup]="form" (ngSubmit)="save()" novalidate>
  <div class="gt-modal-body">
    <p class="gt-field-hint">Encargado actual: <strong>{{ project.manager.fullName }}</strong></p>

    <div class="gt-field">
      <label for="delegate-manager">Nuevo Encargado</label>
      <select id="delegate-manager" formControlName="managerUserId" [attr.aria-busy]="loading">
        <option [value]="0" disabled>{{ loading ? 'Cargando usuarios…' : 'Selecciona un usuario activo' }}</option>
        @for (candidate of candidates; track candidate.userId) {
          <option [value]="candidate.userId">{{ candidate.fullName }}{{ candidate.chief ? ' (Jefe)' : '' }}</option>
        }
      </select>
      @if (form.controls.managerUserId.touched && form.controls.managerUserId.value < 1) {
        <small class="gt-field-error">Elige a quién delegar el proyecto.</small>
      }
    </div>

    <div class="gt-field">
      <label for="delegate-reason">Motivo</label>
      <textarea id="delegate-reason" formControlName="reason" maxlength="300"></textarea>
      @if (form.controls.reason.touched && form.controls.reason.invalid) {
        <small class="gt-field-error">El motivo es obligatorio: queda registrado con tu nombre.</small>
      }
    </div>

    @if (error) {
      <p class="gt-alert gt-alert-error" role="alert">{{ error }}</p>
    }
  </div>

  <div class="gt-modal-actions">
    <button type="button" class="gt-btn" (click)="cancelled.emit()" [disabled]="saving">Cancelar</button>
    <button type="submit" class="gt-btn gt-btn-primary" [disabled]="saving || loading">
      {{ saving ? 'Delegando…' : 'Delegar proyecto' }}
    </button>
  </div>
</form>
`,
})
export class DelegationDialog implements OnInit {
  @Input({ required: true }) project!: Project;
  @Output() saved = new EventEmitter<Project>();
  @Output() cancelled = new EventEmitter<void>();

  private readonly projects = inject(ProjectService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  readonly form = inject(FormBuilder).nonNullable.group({
    managerUserId: [0, Validators.min(1)],
    reason: ['', [Validators.required, Validators.maxLength(300), Validators.pattern(/\S/)]],
  });

  candidates: UserSummary[] = [];
  loading = true;
  saving = false;
  error: string | null = null;

  ngOnInit(): void {
    this.projects.eligibleManagers().subscribe({
      next: (users) => {
        this.candidates = users.filter((user) => user.userId !== this.project.manager.userId);
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.loading = false;
        this.error = apiErrorMessage(error, 'No se pudieron cargar los usuarios disponibles.');
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
    if (this.form.invalid || Number(value.managerUserId) < 1) {
      return;
    }
    this.saving = true;
    this.error = null;
    this.projects.delegate(this.project, Number(value.managerUserId), value.reason.trim()).subscribe({
      next: (project) => {
        this.saving = false;
        this.saved.emit(project);
      },
      error: (error) => {
        this.saving = false;
        this.error = apiErrorMessage(error, 'No se pudo delegar el proyecto.');
        this.changeDetector.markForCheck();
      },
    });
  }
}
