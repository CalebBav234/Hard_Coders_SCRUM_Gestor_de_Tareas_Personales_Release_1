import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `<section class="login-page">
  <form class="login-card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
    <header>
      <span class="gt-eyebrow">GESTOR DE TAREAS</span>
      <h2>Iniciar sesión</h2>
      <p>Usa tu correo de la organización.</p>
    </header>

    <div class="gt-field">
      <label for="login-email">Correo</label>
      <input id="login-email" type="email" formControlName="email" autocomplete="username" />
      @if (form.controls.email.touched && form.controls.email.invalid) {
        <small class="gt-field-error">Escribe un correo válido.</small>
      }
    </div>

    <div class="gt-field">
      <label for="login-password">Contraseña</label>
      <input id="login-password" type="password" formControlName="password" autocomplete="current-password" />
      @if (form.controls.password.touched && form.controls.password.invalid) {
        <small class="gt-field-error">La contraseña es obligatoria.</small>
      }
    </div>

    @if (error) {
      <p class="gt-alert gt-alert-error" role="alert">{{ error }}</p>
    }

    <button type="submit" class="gt-btn gt-btn-primary login-submit" [disabled]="submitting">
      {{ submitting ? 'Ingresando…' : 'Ingresar' }}
    </button>
  </form>
</section>
`,
  styles: [`.login-page {
  min-height: calc(100vh - 160px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
}

.login-card {
  width: min(400px, 100%);
  padding: 30px 28px;
  border: 1px solid var(--martinica);
  border-radius: 10px;
  background: var(--arcilla-ebano);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.28);
}

.login-card header {
  margin-bottom: 22px;
}

.login-card h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 650;
}

.login-card p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.login-submit {
  width: 100%;
  margin-top: 6px;
}
`],
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.maxLength(200)]],
  });

  submitting = false;
  error: string | null = null;

  submit(): void {
    if (this.submitting) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const { email, password } = this.form.getRawValue();
    this.submitting = true;
    this.error = null;
    this.auth.login(email, password).subscribe({
      next: () => {
        this.submitting = false;
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        void this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : '/');
      },
      error: (response: { status?: number; error?: { error?: string; message?: string } }) => {
        this.submitting = false;
        // El cuerpo de error del backend es { error, message }: el código viaja en "error".
        if (response.status === 403 && response.error?.error === 'USER_INACTIVE') {
          // El backend ya verificó la contraseña: la cuenta existe pero está deshabilitada.
          this.error = response.error.message ?? 'Tu usuario está inactivo. Contacta al administrador.';
        } else if (response.status === 401) {
          this.error = 'Correo o contraseña incorrectos.';
        } else {
          this.error = 'No se pudo iniciar sesión. Comprueba que el backend esté disponible.';
        }
        this.changeDetector.markForCheck();
      },
    });
  }
}
