import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { authGuard, guestGuard } from './auth.guard';
import { authInterceptor } from './auth.interceptor';
import { SESSION_STORAGE_KEY, AuthService } from './auth.service';

const USER = {
  userId: 1,
  email: 'jefe@gestor.local',
  givenNames: 'Julia',
  familyNames: 'Mendoza',
  fullName: 'Julia Mendoza',
  chief: true,
  active: true,
};

describe('Autenticación', () => {
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  function login(expiresInMs = 60_000): void {
    auth.login(' jefe@gestor.local ', 'Gestor2026!').subscribe();
    const request = http.expectOne('/api/auth/login');
    expect(request.request.body).toEqual({ email: 'jefe@gestor.local', password: 'Gestor2026!' });
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({ token: 'jwt-123', expiresAt: new Date(Date.now() + expiresInMs).toISOString(), user: USER });
  }

  it('stores the session and exposes the role', () => {
    login();
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.isChief()).toBe(true);
    expect(auth.user()?.fullName).toBe('Julia Mendoza');
    expect(JSON.parse(sessionStorage.getItem(SESSION_STORAGE_KEY)!).token).toBe('jwt-123');
  });

  it('adds the bearer token to API calls only', () => {
    login();
    const client = TestBed.inject(HttpClient);
    client.get('/api/projects').subscribe();
    expect(http.expectOne('/api/projects').request.headers.get('Authorization')).toBe('Bearer jwt-123');
    client.get('/assets/logo.svg').subscribe();
    expect(http.expectOne('/assets/logo.svg').request.headers.has('Authorization')).toBe(false);
  });

  it('logs out and returns to the login when the backend answers 401', () => {
    login();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    TestBed.inject(HttpClient).get('/api/projects').subscribe({ error: () => undefined });
    http.expectOne('/api/projects').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(auth.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login'], expect.anything());
  });

  it('treats an expired token as no session', () => {
    login(-1000);
    expect(auth.token()).toBeNull();
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('guards private pages and the login page', () => {
    const run = (guard: typeof authGuard) =>
      TestBed.runInInjectionContext(() => guard({} as never, { url: '/projects' } as never));

    const blocked = run(authGuard) as UrlTree;
    expect(blocked.toString()).toBe('/login?returnUrl=%2Fprojects');
    expect(run(guestGuard)).toBe(true);

    login();
    expect(run(authGuard)).toBe(true);
    expect((run(guestGuard) as UrlTree).toString()).toBe('/');
  });
});
