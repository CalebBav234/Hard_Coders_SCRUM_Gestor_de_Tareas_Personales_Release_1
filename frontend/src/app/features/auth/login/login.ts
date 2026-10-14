import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
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
