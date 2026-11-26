import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { UserList } from './user-list';
import { UserResponse } from '../../../core/models/user';

const USERS: UserResponse[] = [
  { userId: 1, email: 'ana@gestor.local', givenNames: 'Ana', familyNames: 'Pérez', fullName: 'Ana Pérez', chief: false, active: true },
  { userId: 2, email: 'luis@gestor.local', givenNames: 'Luis', familyNames: 'Gómez', fullName: 'Luis Gómez', chief: false, active: false },
];

function setup() {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [UserList],
    providers: [provideHttpClient(), provideHttpClientTesting()],
  });
  const http = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(UserList);
  const component = fixture.componentInstance;
  const router = TestBed.inject(Router);
  const navigateSpy = vi.spyOn(router, 'navigate');
  return { http, fixture, component, navigateSpy };
}

describe('UserList (administración de usuarios)', () => {
  it('muestra estado de carga al iniciar', () => {
    const { fixture, component } = setup();
    fixture.detectChanges();
    expect(component.loading).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Cargando usuarios');
  });

  it('renderiza el listado con estado activo/inactivo y sin secretos', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    const request = http.expectOne('/api/users');
    request.flush(USERS);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Ana');
    expect(text).toContain('Pérez');
    expect(text).toContain('Activo');
    expect(text).toContain('Luis');
    expect(text).toContain('Gómez');
    expect(text).toContain('Inactivo');
    expect(text).not.toContain('password');
    expect(text).not.toContain('token');
    expect(component.users.length).toBe(2);
  });

  it('muestra estado vacío sin error', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users').flush([]);
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('No hay usuarios registrados');
    expect(component.error).toBeNull();
  });

  it('muestra error cuando falla la carga', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users').flush({ code: 'X', message: 'Falló' }, { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    expect(component.error).toBeTruthy();
    expect(component.loading).toBe(false);
  });

  it('navega al detalle al ver un usuario', () => {
    const { fixture, component, http, navigateSpy } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users').flush(USERS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('[data-testid="user-list-view-1"]') as HTMLElement)?.click();
    expect(navigateSpy).toHaveBeenCalledWith(['/users', 1]);
  });

  it('navega a editar un usuario', () => {
    const { fixture, component, http, navigateSpy } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users').flush(USERS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('[data-testid="user-list-edit-1"]') as HTMLElement)?.click();
    expect(navigateSpy).toHaveBeenCalledWith(['/users', 1, 'edit']);
  });

  it('desactiva un usuario tras confirmar y actualiza el listado', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users').flush(USERS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('[data-testid="user-list-deactivate-1"]') as HTMLElement)?.click();
    fixture.detectChanges();
    const dialog = el.querySelector('dialog');
    expect(dialog).not.toBeNull();
    expect(el.textContent ?? '').toContain('¿Desactivar a Ana Pérez?');
    const btnConfirm = el.querySelector('[data-testid="user-deactivate-confirm"]') as HTMLElement;
    btnConfirm.click();
    const deactivateReq = http.expectOne('/api/users/1');
    expect(deactivateReq.request.method).toBe('DELETE');
    const updated = { ...USERS[0], active: false };
    deactivateReq.flush(updated);
    fixture.detectChanges();
    expect(component.users.find((u) => u.userId === 1)?.active).toBe(false);
    expect(component.success).toContain('desactivado');
    const el2 = fixture.nativeElement as HTMLElement;
    expect(el2.querySelector('[data-testid="user-list-deactivate-1"]')?.getAttribute('disabled')).not.toBeNull();
  });

  it('no desactiva un usuario ya inactivo', () => {
    const { fixture, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users').flush(USERS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const btns = Array.from(el.querySelectorAll('[data-testid^="user-list-deactivate-"]'));
    const btnInactive = btns.find((b) => (b.textContent ?? '').includes('Ya inactivo'));
    expect(btnInactive).not.toBeNull();
    expect(btnInactive!.getAttribute('disabled')).not.toBeNull();
    http.expectNone('/api/users/2');
  });

  it('navega a crear usuario al hacer clic en Nuevo usuario', () => {
    const { fixture, http, navigateSpy } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users').flush(USERS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const btnNuevo = Array.from(el.querySelectorAll('button')).find((b) => (b.textContent ?? '').includes('Nuevo usuario'));
    expect(btnNuevo).not.toBeNull();
    btnNuevo!.click();
    expect(navigateSpy).toHaveBeenCalledWith(['/users/new']);
  });

  it('cierra el diálogo al cancelar la desactivación', () => {
    const { fixture, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users').flush(USERS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('[data-testid="user-list-deactivate-1"]') as HTMLElement)?.click();
    fixture.detectChanges();
    expect(el.querySelector('dialog')).not.toBeNull();
    const btnCancel = el.querySelector('dialog .gt-btn-small') as HTMLElement;
    btnCancel.click();
    fixture.detectChanges();
    expect(el.querySelector('dialog')).toBeNull();
    http.expectNone('/api/users/1');
  });

  it('muestra error si falla la desactivación y el usuario sigue activo', () => {
    const { fixture, component, http } = setup();
    fixture.detectChanges();
    http.expectOne('/api/users').flush(USERS);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('[data-testid="user-list-deactivate-1"]') as HTMLElement)?.click();
    fixture.detectChanges();
    (el.querySelector('[data-testid="user-deactivate-confirm"]') as HTMLElement)?.click();
    const deactivateReq = http.expectOne('/api/users/1');
    deactivateReq.flush({ code: 'X', message: 'No se pudo desactivar' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(component.error).toContain('No se pudo desactivar');
    expect(component.deactivating).toBe(false);
    expect(component.users.find((u) => u.userId === 1)?.active).toBe(true);
  });
});
