import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { UserEdit } from './user-edit';
import { UserResponse } from '../../../core/models/user';

const USER: UserResponse = {
  userId: 5,
  email: 'ana@gestor.local',
  givenNames: 'Ana',
  familyNames: 'Pérez',
  fullName: 'Ana Pérez',
  chief: false,
  active: true,
};

function setup(userId = 5) {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [UserEdit],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ userId: String(userId) }) } } },
    ],
  });
  const http = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(UserEdit);
  const component = fixture.componentInstance;
  const router = TestBed.inject(Router);
  const navigateSpy = vi.spyOn(router, 'navigate');
  return { http, fixture, component, navigateSpy };
}

describe('UserEdit (edición de perfil de usuario)', () => {
  it('carga el usuario y precarga el formulario sin contraseña', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    const request = http.expectOne('/api/users/5');
    expect(request.request.method).toBe('GET');
    request.flush(USER);
    fixture.detectChanges();
    expect(component.user?.userId).toBe(5);
    expect(component.form.get('email')?.value).toBe('ana@gestor.local');
    expect(component.form.get('givenNames')?.value).toBe('Ana');
    expect(component.form.get('familyNames')?.value).toBe('Pérez');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('#edit-user-password')).toBeNull();
    // No hay campo de contraseña en el formulario de edición.
  });

  it('usuario inexistente muestra no encontrado', () => {
    const { fixture, component, http } = setup(999);
    fixture.detectChanges();
    http.expectOne('/api/users/999').flush({ code: 'NOT_FOUND', message: 'no existe' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(component.notFound).toBe(true);
    const el = fixture.nativeElement as HTMLElement;
    expect((el.textContent ?? '')).toContain('No se encontró el usuario');
  });

  it('guarda solo campos de perfil y los refleja en el formulario', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users/5').flush(USER);
    fixture.detectChanges();

    component.form.setValue({ email: 'MARIA.NUEVA@GESTOR.LOCAL', givenNames: 'María', familyNames: 'López' });
    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('[data-testid="user-edit-submit"]') as HTMLButtonElement)?.click();
    fixture.detectChanges();

    const request = http.expectOne('/api/users/5');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      email: 'MARIA.NUEVA@GESTOR.LOCAL',
      givenNames: 'María',
      familyNames: 'López',
    });
    expect(request.request.body).not.toHaveProperty('isChief');
    expect(request.request.body).not.toHaveProperty('isActive');
    expect(request.request.body).not.toHaveProperty('password');

    const updated = { ...USER, email: 'maria.nueva@gestor.local', givenNames: 'María', familyNames: 'López' };
    request.flush(updated);
    fixture.detectChanges();
    expect(component.success).toContain('guardados');
    expect(component.user?.email).toBe('maria.nueva@gestor.local');
    expect(component.user?.givenNames).toBe('María');
  });

  it('correo inválido no llama al backend y muestra error de validación', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users/5').flush(USER);
    fixture.detectChanges();

    component.form.setValue({ email: 'invalido', givenNames: 'Ana', familyNames: 'Pérez' });
    component.form.controls.email.markAsTouched();
    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('[data-testid="user-edit-submit"]') as HTMLButtonElement)?.click();
    fixture.detectChanges();

    http.expectNone('/api/users/5');
    expect(component.error).toBeNull();
    expect((el.textContent ?? '')).toContain('correo válido');
  });

  it('evita doble envío mientras se guarda', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users/5').flush(USER);
    fixture.detectChanges();

    component.form.setValue({ email: 'ana@gestor.local', givenNames: 'Ana', familyNames: 'Pérez' });
    const submit = fixture.nativeElement.querySelector('[data-testid="user-edit-submit"]') as HTMLButtonElement;
    submit.click();
    fixture.detectChanges();
    expect(component.saving).toBe(true);
    expect(submit.disabled).toBe(true);
    // Un segundo clic mientras se guarda no dispara otra petición.
    submit.click();
    fixture.detectChanges();
    // Un segundo clic mientras se guarda no dispara otra petición (expectOne falla si hay 0 o >1).
    http.expectOne('/api/users/5').flush({ ...USER, active: false });
  });

  it('cancelar navega de vuelta al detalle del usuario', () => {
    const { fixture, http, navigateSpy } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users/5').flush(USER);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const btnCancel = Array.from(el.querySelectorAll('button')).find((b) => (b.textContent ?? '').includes('Cancelar'));
    expect(btnCancel).not.toBeNull();
    btnCancel!.click();
    expect(navigateSpy).toHaveBeenCalledWith(['/users', 5]);
  });

  it('muestra error si falla la actualización y no queda guardando', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users/5').flush(USER);
    fixture.detectChanges();
    component.form.setValue({ email: 'ana@gestor.local', givenNames: 'Ana', familyNames: 'Pérez' });
    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('[data-testid="user-edit-submit"]') as HTMLButtonElement)?.click();
    const updateReq = http.expectOne('/api/users/5');
    updateReq.flush({ code: 'X', message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(component.error).toContain('Boom');
    expect(component.saving).toBe(false);
  });

  it('ignora submit mientras ya se guarda (guarda de doble envío)', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users/5').flush(USER);
    fixture.detectChanges();
    component.saving = true;
    component.submit();
    http.expectNone('/api/users/5');
  });
});
