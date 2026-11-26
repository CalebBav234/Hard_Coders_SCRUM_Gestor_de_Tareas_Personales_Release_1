import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UserService } from './user.service';

describe('UserService (clientes de /api/users)', () => {
  let http: HttpTestingController;
  let service: UserService;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [UserService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('list() llama a GET /api/users', () => {
    service.list().subscribe();
    const request = http.expectOne('/api/users');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('get(userId) llama a GET /api/users/:userId', () => {
    service.get(7).subscribe();
    const request = http.expectOne('/api/users/7');
    expect(request.request.method).toBe('GET');
    request.flush({ userId: 7, email: 'a@b.c', givenNames: 'A', familyNames: 'B', fullName: 'A B', chief: false, active: true });
  });

  it('create() llama a POST /api/users con el cuerpo de creación', () => {
    service.create({ email: 'a@b.c', givenNames: 'A', familyNames: 'B', password: 'Secreto123' }).subscribe();
    const request = http.expectOne('/api/users');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'a@b.c',
      givenNames: 'A',
      familyNames: 'B',
      password: 'Secreto123',
    });
    request.flush({ userId: 1, email: 'a@b.c', givenNames: 'A', familyNames: 'B', fullName: 'A B', chief: false, active: true });
  });

  it('update() llama a PUT /api/users/:userId con solo los campos enviados (actualización parcial)', () => {
    service.update(7, { givenNames: 'María' }).subscribe();
    const request = http.expectOne('/api/users/7');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ givenNames: 'María' });
    // No envía password ni campos de privilegio.
    expect(request.request.body).not.toHaveProperty('password');
    expect(request.request.body).not.toHaveProperty('isChief');
    expect(request.request.body).not.toHaveProperty('isActive');
    request.flush({ userId: 7, email: 'a@b.c', givenNames: 'María', familyNames: 'B', fullName: 'María B', chief: false, active: true });
  });

  it('update() envía email cuando se provee', () => {
    service.update(7, { email: 'NUEVA@GESTOR.LOCAL' }).subscribe();
    const request = http.expectOne('/api/users/7');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ email: 'NUEVA@GESTOR.LOCAL' });
    request.flush({ userId: 7, email: 'nueva@gestor.local', givenNames: 'A', familyNames: 'B', fullName: 'A B', chief: false, active: true });
  });

  it('deactivate() llama a DELETE /api/users/:userId', () => {
    service.deactivate(7).subscribe();
    const request = http.expectOne('/api/users/7');
    expect(request.request.method).toBe('DELETE');
    request.flush({ userId: 7, email: 'a@b.c', givenNames: 'A', familyNames: 'B', fullName: 'A B', chief: false, active: false });
  });

  it('errores de red/backend se propagan al llamador', () => {
    service.list().subscribe({
      error: (err) => { expect(err.status).toBe(500); },
    });
    const request = http.expectOne('/api/users');
    request.flush({ code: 'VALIDATION_ERROR', message: 'Boom' }, { status: 500, statusText: 'Server Error' });
  });
});
