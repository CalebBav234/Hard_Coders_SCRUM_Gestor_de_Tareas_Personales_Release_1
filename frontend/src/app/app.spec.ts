import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { SESSION_STORAGE_KEY } from './core/auth/auth.service';

describe('App', () => {
  beforeEach(async () => {
    sessionStorage.clear();
  });

  afterEach(() => sessionStorage.clear());

  async function create() {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('hides the navigation until the user signs in', async () => {
    const element = await create();
    expect(element.querySelector('h1')?.textContent).toContain('Gestor de Tareas');
    expect(element.querySelector('nav')).toBeNull();
  });

  it('shows the sections and the signed-in user', async () => {
    sessionStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({
        token: 't',
        expiresAt: new Date(Date.now() + 60_000).toISOString(),
        user: { userId: 1, email: 'j@x.com', givenNames: 'Julia', familyNames: 'Mendoza', fullName: 'Julia Mendoza', chief: true, active: true },
      }),
    );
    const element = await create();
    expect(element.querySelector('nav')?.textContent).toContain('Proyectos');
    expect(element.querySelector('nav')?.textContent).toContain('Historial');
    expect(element.querySelector('.session-user')?.textContent).toContain('Julia Mendoza');
  });
});
