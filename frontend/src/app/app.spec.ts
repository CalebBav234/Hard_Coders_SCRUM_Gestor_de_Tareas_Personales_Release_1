import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { App } from './app';
import { SESSION_STORAGE_KEY } from './core/auth/auth.service';

describe('App', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  afterEach(() => sessionStorage.clear());

  async function create() {
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

  it('logout clears the session and navigates to login', async () => {
    sessionStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({
        token: 't',
        expiresAt: new Date(Date.now() + 60_000).toISOString(),
        user: { userId: 1, email: 'j@x.com', givenNames: 'Julia', familyNames: 'Mendoza', fullName: 'Julia Mendoza', chief: true, active: true },
      }),
    );
    const router = TestBed.inject(Router);
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('.session-logout') as HTMLButtonElement | null;
    expect(button).not.toBeNull();
    button!.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
    expect(navSpy).toHaveBeenCalledWith(['/login']);
  });
});
