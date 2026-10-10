import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Project, UserSummary } from '../../../core/models/project';
import { ProjectService, apiErrorMessage } from '../../../core/services/project.service';

/** US1.2 — el Jefe delega o reasigna el Encargado; la delegación queda en el historial del plan. */
@Component({
  selector: 'app-delegation-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './delegation-dialog.html',
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
