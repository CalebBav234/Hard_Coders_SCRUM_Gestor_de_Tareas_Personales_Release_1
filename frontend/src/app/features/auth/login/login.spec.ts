import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;
  let element: HTMLElement;
  let router: Router;

  function fill(email: string, password: string): void {
    const component = fixture.componentInstance;
    component.form.setValue({ email, password });
    component.submit();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [Login, ReactiveFormsModule],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(Login);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    sessionStorage.clear();
    http.verify();
  });

  it('shows the generic message when the credentials are wrong (P1.2)', () => {
    fill('jefe@gestor.local', 'claveMala');

    http
      .expectOne('/api/auth/login')
      .flush(
        { error: 'INVALID_CREDENTIALS', message: 'Correo o contraseña incorrectos.' },
        { status: 401, statusText: 'Unauthorized' },
      );
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('Correo o contraseña incorrectos.');
    expect(element.textContent).toContain('Correo o contraseña incorrectos.');
  });

  it('explains that the account is inactive instead of blaming the password (P1.3)', () => {
    fill('inactivo@gestor.local', 'Gestor2026!');

    http
      .expectOne('/api/auth/login')
      .flush(
        { error: 'USER_INACTIVE', message: 'Tu usuario está inactivo. Contacta al administrador.' },
        { status: 403, statusText: 'Forbidden' },
      );
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('Tu usuario está inactivo. Contacta al administrador.');
    expect(fixture.componentInstance.submitting).toBe(false);
  });

  it('keeps a clear message when the backend is unreachable', () => {
    fill('jefe@gestor.local', 'Gestor2026!');

    http.expectOne('/api/auth/login').error(new ProgressEvent('network'), { status: 0, statusText: 'Unknown Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toContain('No se pudo iniciar sesión');
  });

  it('enters and navigates to the returnUrl when the login succeeds (P1.1)', () => {
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fill('jefe@gestor.local', 'Gestor2026!');

    http.expectOne('/api/auth/login').flush({
      token: 'jwt-de-prueba',
      expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      user: { userId: 1, email: 'jefe@gestor.local', givenNames: 'Julia', familyNames: 'Mendoza', fullName: 'Julia Mendoza', chief: true, active: true },
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBeNull();
    expect(navigate).toHaveBeenCalled();
  });

  it('navigates to home when there is no returnUrl', () => {
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fill('jefe@gestor.local', 'Gestor2026!');

    http.expectOne('/api/auth/login').flush({
      token: 'jwt-de-prueba',
      expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      user: { userId: 1, email: 'jefe@gestor.local', givenNames: 'Julia', familyNames: 'Mendoza', fullName: 'Julia Mendoza', chief: true, active: true },
    });
    fixture.detectChanges();

    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('shows default message when 403 error has no message', () => {
    fill('inactivo@gestor.local', 'Gestor2026!');

    http
      .expectOne('/api/auth/login')
      .flush(
        { error: 'USER_INACTIVE', message: null },
        { status: 403, statusText: 'Forbidden' },
      );
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toBe('Tu usuario está inactivo. Contacta al administrador.');
  });

  it('ignores a second submit while already submitting', () => {
    const component = fixture.componentInstance;
    component.form.setValue({ email: 'jefe@gestor.local', password: 'Gestor2026!' });
    component.submitting = true;
    component.submit();
    fixture.detectChanges();

    expect(component.submitting).toBe(true);
    http.expectNone('/api/auth/login');
  });

  it('does not submit when the form is invalid', () => {
    const component = fixture.componentInstance;
    component.form.setValue({ email: '', password: '' });
    component.submit();
    fixture.detectChanges();

    expect(component.submitting).toBe(false);
    http.expectNone('/api/auth/login');
  });
});
