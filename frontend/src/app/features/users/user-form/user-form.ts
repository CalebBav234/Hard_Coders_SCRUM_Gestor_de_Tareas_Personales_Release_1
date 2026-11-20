import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { apiErrorMessage } from '../../../core/services/project.service';

/** BCrypt solo procesa 72 bytes; el backend rechaza contraseñas más largas (400). */
export const PASSWORD_MAX_BYTES = 72;

export function maxUtf8Bytes(max: number) {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = typeof control.value === 'string' ? control.value : '';
    const bytes = new TextEncoder().encode(value).length;
    return bytes > max ? { maxBytes: { max, actual: bytes } } : null;
  };
}

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `<section class="user-page" aria-label="Crear usuario">
  <header class="user-header">
    <div>
      <span class="gt-eyebrow">ADMINISTRACIÓN</span>
      <h2>Crear usuario</h2>
      <p>Registra un nuevo usuario del sistema. La contraseña no se mostrará después de guardar.</p>
    </div>
  </header>

  @if (success) {
    <p class="gt-alert gt-alert-success" role="status" data-testid="user-create-success">{{ success }}</p>
  }
  @if (error) {
    <p class="gt-alert gt-alert-error" role="alert" data-testid="user-create-error">{{ error }}</p>
  }

  <form class="user-card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
    <div class="gt-field">
      <label for="user-email">Correo</label>
      <input id="user-email" type="email" formControlName="email" autocomplete="email" maxlength="254" />
      @if (form.controls.email.touched && form.controls.email.invalid) {
        <small class="gt-field-error">Ingresa un correo válido (máximo 254 caracteres).</small>
      }
    </div>

    <div class="gt-field-row">
      <div class="gt-field">
        <label for="user-given-names">Nombres</label>
        <input id="user-given-names" type="text" formControlName="givenNames" maxlength="100" />
        @if (form.controls.givenNames.touched && form.controls.givenNames.invalid) {
          <small class="gt-field-error">Los nombres son obligatorios (máximo 100 caracteres).</small>
        }
      </div>
      <div class="gt-field">
        <label for="user-family-names">Apellidos</label>
        <input id="user-family-names" type="text" formControlName="familyNames" maxlength="100" />
        @if (form.controls.familyNames.touched && form.controls.familyNames.invalid) {
          <small class="gt-field-error">Los apellidos son obligatorios (máximo 100 caracteres).</small>
        }
      </div>
    </div>

    <div class="gt-field">
      <label for="user-password">Contraseña</label>
      <input id="user-password" type="password" formControlName="password" autocomplete="new-password" maxlength="200" />
      @if (form.controls.password.touched && form.controls.password.hasError('maxBytes')) {
        <small class="gt-field-error">La contraseña no puede superar 72 bytes (los caracteres con tilde o símbolos cuentan más de 1).</small>
      } @else if (form.controls.password.touched && form.controls.password.invalid) {
        <small class="gt-field-error">La contraseña debe tener entre 8 y 200 caracteres.</small>
      }
    </div>

    <div class="gt-modal-actions">
      <button type="submit" class="gt-btn gt-btn-primary" [disabled]="saving" data-testid="user-create-submit">
        {{ saving ? 'Creando…' : 'Crear usuario' }}
      </button>
    </div>
  </form>
</section>
`,
  styles: [`.user-page { max-width: 760px; margin: 0 auto; padding-top: 28px; }
.user-header h2 { margin: 4px 0 0; font-size: 24px; }
.user-header p { margin: 6px 0 0; color: var(--text-muted); font-size: 13px; }
.user-card { margin-top: 18px; padding: 20px; border: 1px solid var(--martinica); border-radius: 10px; background: var(--arcilla-ebano); display: grid; gap: 14px; }
.gt-field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
@media (max-width: 650px) { .gt-field-row { grid-template-columns: 1fr; } }
`],
})
export class UserForm {
  private readonly users = inject(UserService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    givenNames: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)]],
    familyNames: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(200), maxUtf8Bytes(PASSWORD_MAX_BYTES)]],
  });

  saving = false;
  error: string | null = null;
  success: string | null = null;

  submit(): void {
    if (this.saving) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.saving = true;
    this.error = null;
    this.success = null;
    this.users.create({
      email: value.email.trim(),
      givenNames: value.givenNames.trim(),
      familyNames: value.familyNames.trim(),
      password: value.password,
    }).subscribe({
      next: (created) => {
        this.saving = false;
        this.success = `Usuario "${created.fullName}" creado correctamente.`;
        this.form.reset({ email: '', givenNames: '', familyNames: '', password: '' });
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.saving = false;
        this.error = apiErrorMessage(err, 'No se pudo crear el usuario.');
        this.changeDetector.markForCheck();
      },
    });
  }
}
