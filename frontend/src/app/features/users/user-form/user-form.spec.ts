import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { maxUtf8Bytes, UserForm } from './user-form';

describe('UserForm (A6 — creación de usuario por Jefe)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<UserForm>;
  let component: UserForm;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [UserForm],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(UserForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function fill(password: string): void {
    component.form.setValue({
      email: 'nuevo@gestor.local',
      givenNames: ' Ana ',
      familyNames: ' Paz ',
      password,
    });
  }

  it('maxUtf8Bytes cuenta bytes UTF-8, no caracteres', () => {
    const validator = maxUtf8Bytes(72);
    expect(validator(new FormControl('x'.repeat(72)))).toBeNull();
    expect(validator(new FormControl('x'.repeat(73)))).toEqual({ maxBytes: { max: 72, actual: 73 } });
    // 25 × "€" = 75 bytes aunque sean solo 25 caracteres
    expect(validator(new FormControl('€'.repeat(25)))).not.toBeNull();
    expect(validator(new FormControl(null))).toBeNull();
  });

  it('bloquea contraseñas cortas y de más de 72 bytes sin llamar al backend', () => {
    fill('short');
    expect(component.form.invalid).toBe(true);
    fill('x'.repeat(73));
    expect(component.form.controls.password.hasError('maxBytes')).toBe(true);
    component.submit();
    fixture.detectChanges();
    http.expectNone('/api/users');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('72 bytes');
  });

  it('envía datos recortados y muestra el éxito sin exponer la contraseña', () => {
    fill('x'.repeat(72));
    component.submit();
    const request = http.expectOne('/api/users');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'nuevo@gestor.local',
      givenNames: 'Ana',
      familyNames: 'Paz',
      password: 'x'.repeat(72),
    });
    request.flush({ userId: 9, fullName: 'Ana Paz', email: 'nuevo@gestor.local', chief: false, active: true });
    fixture.detectChanges();
    expect(component.success).toContain('Ana Paz');
    expect(component.form.getRawValue().password).toBe('');
  });

  it('muestra el error del backend (correo duplicado)', () => {
    fill('Valida2026!');
    component.submit();
    http.expectOne('/api/users').flush(
      { code: 'VALIDATION_ERROR', message: 'El correo ya está registrado' },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();
    expect(component.error).toBeTruthy();
    expect(component.saving).toBe(false);
  });
});
