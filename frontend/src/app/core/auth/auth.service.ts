import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { CurrentUser, LoginResponse, StoredSession } from './auth.models';

export const SESSION_STORAGE_KEY = 'gestor-tareas.session';

/**
 * Sesión JWT del usuario. El token se guarda en sessionStorage (se borra al cerrar la pestaña).
 * Mostrar u ocultar acciones según el rol es solo ayuda visual: el backend vuelve a autorizar todo.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = signal<StoredSession | null>(this.restore());

  readonly user = computed<CurrentUser | null>(() => this.session()?.user ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly isChief = computed(() => this.session()?.user.chief === true);

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/auth/login', { email: email.trim(), password })
      .pipe(tap((response) => this.store(response)));
  }

  logout(): void {
    this.session.set(null);
    try {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
    } catch {
      // Almacenamiento no disponible: la sesión ya se borró de memoria.
    }
  }

  /** Token vigente, o null si no hay sesión o expiró. */
  token(): string | null {
    const current = this.session();
    if (!current) {
      return null;
    }
    if (new Date(current.expiresAt).getTime() <= Date.now()) {
      this.logout();
      return null;
    }
    return current.token;
  }

  private store(response: LoginResponse): void {
    const stored: StoredSession = {
      token: response.token,
      expiresAt: response.expiresAt,
      user: response.user,
    };
    this.session.set(stored);
    try {
      sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(stored));
    } catch {
      // Sin almacenamiento la sesión dura mientras la página esté abierta.
    }
  }

  private restore(): StoredSession | null {
    try {
      const raw = sessionStorage.getItem(SESSION_STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw) as StoredSession;
      if (!parsed?.token || new Date(parsed.expiresAt).getTime() <= Date.now()) {
        sessionStorage.removeItem(SESSION_STORAGE_KEY);
        return null;
      }
      return parsed;
    } catch {
      return null;
    }
  }
}
