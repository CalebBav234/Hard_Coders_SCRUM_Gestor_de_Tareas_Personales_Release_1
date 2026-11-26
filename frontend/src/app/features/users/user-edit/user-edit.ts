import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UpdateUserRequest, UserResponse } from '../../../core/models/user';
import { UserService } from '../../../core/services/user.service';
import { apiErrorMessage } from '../../../core/services/project.service';

export const EMAIL_MAX = 254;
export const NAME_MAX = 100;

export function noWhitespaceOnly() {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = typeof control.value === 'string' ? control.value : '';
    return value.trim() === '' ? { blank: true } : null;
  };
}

@Component({
  selector: 'app-user-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `<section class="user-edit-page" aria-label="Editar usuario">
  <header class="user-edit-header">
    <button type="button" class="gt-btn gt-btn-small" (click)="goBack()">← Volver</button>
    <div>
      <span class="gt-eyebrow">EDICIÓN DE USUARIO</span>
      <h2>Editar {{ user?.fullName }}</h2>
      <p>{{ user?.email }}</p>
    </div>
  </header>

  @if (loading) {
    <p class="user-edit-empty" role="status" aria-label="Cargando">Cargando usuario…</p>
  } @else if (notFound) {
    <p class="user-edit-empty">No se encontró el usuario solicitado.</p>
    <button type="button" class="gt-btn gt-btn-primary" (click)="goBack()">Volver a usuarios</button>
  } @else if (user) {
    @if (success) {
      <p class="gt-alert gt-alert-success" role="status" data-testid="user-edit-success">{{ success }}</p>
    }
    @if (error) {
      <p class="gt-alert gt-alert-error" role="alert" data-testid="user-edit-error">{{ error }}</p>
    }

    <form class="user-card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <div class="gt-field">
        <label for="edit-user-email">Correo</label>
        <input id="edit-user-email" type="email" formControlName="email" autocomplete="email" maxlength="254" />
        @if (form.controls.email.touched && form.controls.email.invalid) {
          <small class="gt-field-error">Ingresa un correo válido (máximo 254 caracteres).</small>
        }
      </div>

      <div class="gt-field-row">
        <div class="gt-field">
          <label for="edit-user-given-names">Nombres</label>
          <input id="edit-user-given-names" type="text" formControlName="givenNames" maxlength="100" />
          @if (form.controls.givenNames.touched && form.controls.givenNames.invalid) {
            <small class="gt-field-error">Los nombres son obligatorios (máximo 100 caracteres).</small>
          }
        </div>
        <div class="gt-field">
          <label for="edit-user-family-names">Apellidos</label>
          <input id="edit-user-family-names" type="text" formControlName="familyNames" maxlength="100" />
          @if (form.controls.familyNames.touched && form.controls.familyNames.invalid) {
            <small class="gt-field-error">Los apellidos son obligatorios (máximo 100 caracteres).</small>
          }
        </div>
      </div>

      <p class="gt-field-hint">Solo se actualizan nombres, apellidos y correo. Los privilegios (Jefe), el estado y la contraseña no se modifican desde este formulario.</p>

      <div class="gt-modal-actions">
        <button type="button" class="gt-btn" (click)="goBack()" [disabled]="saving">Cancelar</button>
        <button type="submit" class="gt-btn gt-btn-primary" [disabled]="saving" data-testid="user-edit-submit">
          {{ saving ? 'Guardando…' : 'Guardar cambios' }}
        </button>
      </div>
    </form>
  }
</section>
`,
  styles: [`.user-edit-page { max-width: 640px; margin: 0 auto; padding-top: 28px; }
.user-edit-header { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 20px; }
.user-edit-header h2 { margin: 0; font-size: 24px; font-weight: 650; }
.user-edit-header p { margin: 6px 0 0; color: var(--text-secondary); font-size: 15px; word-break: break-all; }
.user-edit-empty { text-align: center; padding: 40px 0; color: var(--text-muted); }
.user-card { margin-top: 14px; padding: 20px; border: 1px solid var(--martinica); border-radius: 10px; background: var(--arcilla-ebano); display: grid; gap: 14px; }
.gt-field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
@media (max-width: 650px) { .gt-field-row { grid-template-columns: 1fr; } }
.gt-field-hint { margin: 0; font-size: 12px; color: var(--text-muted); }
.gt-modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 10px; }
`],
})
export class UserEdit implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly usersService = inject(UserService);

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(EMAIL_MAX)]],
    givenNames: ['', [Validators.required, Validators.maxLength(NAME_MAX), noWhitespaceOnly()]],
    familyNames: ['', [Validators.required, Validators.maxLength(NAME_MAX), noWhitespaceOnly()]],
  });

  user: UserResponse | null = null;
  loading = false;
  saving = false;
  error: string | null = null;
  success: string | null = null;
  notFound = false;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('userId'));
    if (!Number.isFinite(id)) {
      this.notFound = true;
      return;
    }
    this.load(id);
  }

  load(userId: number): void {
    this.loading = true;
    this.error = null;
    this.notFound = false;
    this.usersService.get(userId).subscribe({
      next: (user) => {
        this.user = user;
        this.form.setValue({
          email: user.email,
          givenNames: user.givenNames,
          familyNames: user.familyNames,
        });
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.loading = false;
        if (error?.status === 404) {
          this.notFound = true;
        } else {
          this.error = apiErrorMessage(error, 'No se pudo cargar el usuario.');
        }
        this.changeDetector.markForCheck();
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/users', this.user?.userId ?? '/users']);
  }

  submit(): void {
    if (this.saving || !this.user) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const request: UpdateUserRequest = {
      email: value.email.trim(),
      givenNames: value.givenNames.trim(),
      familyNames: value.familyNames.trim(),
    };
    this.saving = true;
    this.error = null;
    this.success = null;
    this.usersService.update(this.user.userId, request).subscribe({
      next: (updated) => {
        this.saving = false;
        this.user = updated;
        this.success = 'Cambios guardados correctamente.';
        this.form.setValue({ email: updated.email, givenNames: updated.givenNames, familyNames: updated.familyNames });
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.saving = false;
        this.error = apiErrorMessage(error, 'No se pudieron guardar los cambios.');
        this.changeDetector.markForCheck();
      },
    });
  }
}
