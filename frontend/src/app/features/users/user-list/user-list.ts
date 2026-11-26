import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserResponse } from '../../../core/models/user';
import { UserService } from '../../../core/services/user.service';
import { apiErrorMessage } from '../../../core/services/project.service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule],
  template: `<section class="user-list-page" aria-label="Administración de usuarios">
  <header class="user-list-header">
    <div>
      <span class="gt-eyebrow">ADMINISTRACIÓN DE USUARIOS</span>
      <h2>Usuarios</h2>
      <p>Lista de usuarios registrados. Solo un Jefe activo puede crear, editar o desactivar usuarios.</p>
    </div>
    <button type="button" class="gt-btn gt-btn-primary" (click)="goCreate()" [disabled]="loading">
      ＋ Nuevo usuario
    </button>
  </header>

  @if (success) {
    <p class="gt-alert gt-alert-success" role="status" data-testid="user-list-success">{{ success }}</p>
  }
  @if (error) {
    <p class="gt-alert gt-alert-error" role="alert" data-testid="user-list-error">{{ error }}</p>
  }

  @if (loading) {
    <p class="user-list-empty" role="status" aria-label="Cargando">Cargando usuarios…</p>
  } @else if (users.length === 0 && !error) {
    <p class="user-list-empty">No hay usuarios registrados.</p>
  }

  <table class="user-table" aria-label="Listado de usuarios">
    <thead>
      <tr>
        <th scope="col">Nombres</th>
        <th scope="col">Apellidos</th>
        <th scope="col">Correo</th>
        <th scope="col">Estado</th>
        <th scope="col" class="user-table-actions">Acciones</th>
      </tr>
    </thead>
    <tbody>
      @for (u of users; track u.userId) {
        <tr>
          <td>{{ u.givenNames }}</td>
          <td>{{ u.familyNames }}</td>
          <td>{{ u.email }}</td>
          <td>
            <span class="user-status" [class.user-status-active]="u.active" [class.user-status-inactive]="!u.active"
                  [attr.aria-label]="u.active ? 'Activo' : 'Inactivo'">
              {{ u.active ? 'Activo' : 'Inactivo' }}
            </span>
          </td>
          <td class="user-table-actions">
            <button type="button" class="gt-btn gt-btn-small" (click)="goDetail(u.userId)"
                    [disabled]="loading" [attr.data-testid]="'user-list-view-' + u.userId">Ver</button>
            <button type="button" class="gt-btn gt-btn-small" (click)="goEdit(u.userId)"
                    [disabled]="loading" [attr.data-testid]="'user-list-edit-' + u.userId">Editar</button>
            <button type="button" class="gt-btn gt-btn-small gt-btn-danger" (click)="requestDeactivate(u)"
                    [disabled]="loading || deactivatingId === u.userId || !u.active"
                    [attr.data-testid]="'user-list-deactivate-' + u.userId">
              {{ deactivatingId === u.userId ? 'Desactivando…' : (u.active ? 'Desactivar' : 'Ya inactivo') }}
            </button>
          </td>
        </tr>
      }
    </tbody>
  </table>

  @if (confirmTarget) {
    <dialog class="gt-modal" open aria-modal="true" aria-labelledby="user-deactivate-title">
      <header class="gt-modal-header">
        <div>
          <span class="gt-eyebrow">DESACTIVAR USUARIO</span>
          <h2 id="user-deactivate-title">¿Desactivar a {{ confirmTarget.givenNames }} {{ confirmTarget.familyNames }}?</h2>
          <p>{{ confirmTarget.email }}</p>
        </div>
        <button type="button" class="gt-modal-close" aria-label="Cerrar" (click)="cancelDeactivate()">×</button>
      </header>
      <p class="gt-modal-body">Esta acción desactiva al usuario para que no pueda iniciar sesión ni realizar operaciones. Su identidad, sus relaciones y su historial se conservan; no se elimina ningún dato.</p>
      <div class="gt-modal-actions">
        <button type="button" class="gt-btn gt-btn-small" (click)="cancelDeactivate()">Cancelar</button>
        <button type="button" class="gt-btn gt-btn-danger" (click)="confirmDeactivate()" [disabled]="deactivating"
                data-testid="user-deactivate-confirm">
          Desactivar usuario
        </button>
      </div>
    </dialog>
    <div class="gt-modal-backdrop" (click)="cancelDeactivate()"></div>
  }
</section>
`,
  styles: [`.user-list-page { max-width: 1100px; margin: 0 auto; padding-top: 28px; }
.user-list-header { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 20px; }
.user-list-header h2 { margin: 0; font-size: 24px; font-weight: 650; }
.user-list-header p, .user-list-empty { margin: 6px 0 0; color: var(--text-muted); font-size: 13px; }
.user-list-empty { text-align: center; padding: 30px 0; color: var(--text-muted); }
.user-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
.user-table th { text-align: left; font-size: 12px; color: var(--text-muted); padding: 10px 12px; border-bottom: 2px solid var(--martinica); }
.user-table td { padding: 12px; border-bottom: 1px solid var(--martinica); font-size: 14px; overflow-wrap: anywhere; }
.user-table-actions { white-space: nowrap; }
.user-status { display: inline-flex; align-items: center; gap: 6px; padding: 3px 9px; border-radius: 999px; font-size: 11px; font-weight: 650; }
.user-status-active { background: rgba(99, 167, 124, 0.18); color: #2f7d4f; }
.user-status-inactive { background: rgba(185, 103, 103, 0.18); color: #a33a3a; }
.gt-btn-danger { background: #b34040; border-color: #b34040; color: #fff; }
.gt-btn-danger:hover { background: #a33434; }
.gt-btn-danger[disabled] { background: #c4bdbd; border-color: #c4bdbd; color: #e8e0e0; cursor: not-allowed; }
.gt-modal-body { margin: 14px 0 0; font-size: 13px; color: var(--text-secondary); }
.gt-modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 18px; }
.gt-modal-close { background: transparent; border: none; font-size: 24px; line-height: 1; color: var(--text-muted); padding: 0 4px; cursor: pointer; }
.gt-modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,0.35); z-index: 10; }
.gt-modal { position: fixed; top: 12vh; left: 50%; transform: translateX(-50%); width: min(560px, 92vw); background: var(--arcilla-ebano); border: 1px solid var(--martinica); border-radius: 10px; padding: 0; z-index: 11; box-shadow: 0 8px 24px rgba(0,0,0,0.35); }
.gt-modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--martinica); }
.gt-modal-header h2 { margin: 0; font-size: 18px; font-weight: 650; }
.gt-modal-header p { margin: 6px 0 0; font-size: 13px; color: var(--text-secondary); }
.gt-modal-header .gt-eyebrow { font-size: 11px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }
`],
})
export class UserList implements OnInit {
  private readonly usersService = inject(UserService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly router = inject(Router);

  users: UserResponse[] = [];
  loading = false;
  error: string | null = null;
  success: string | null = null;
  confirmTarget: UserResponse | null = null;
  deactivatingId: number | null = null;
  deactivating = false;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.usersService.list().subscribe({
      next: (users) => {
        this.users = users;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.loading = false;
        this.error = apiErrorMessage(error, 'No se pudieron cargar los usuarios.');
        this.changeDetector.markForCheck();
      },
    });
  }

  goCreate(): void {
    this.router.navigate(['/users/new']);
  }

  goDetail(userId: number): void {
    this.router.navigate(['/users', userId]);
  }

  goEdit(userId: number): void {
    this.router.navigate(['/users', userId, 'edit']);
  }

  requestDeactivate(u: UserResponse): void {
    if (!u.active || this.deactivatingId === u.userId) return;
    this.confirmTarget = u;
  }

  cancelDeactivate(): void {
    this.confirmTarget = null;
  }

  confirmDeactivate(): void {
    const target = this.confirmTarget;
    if (!target || this.deactivating) return;
    this.deactivating = true;
    this.error = null;
    this.usersService.deactivate(target.userId).subscribe({
      next: (deactivated) => {
        this.deactivating = false;
        this.deactivatingId = null;
        this.confirmTarget = null;
        this.success = `Usuario "${deactivated.fullName}" desactivado.`;
        this.replace(deactivated);
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.deactivating = false;
        this.error = apiErrorMessage(error, 'No se pudo desactivar el usuario.');
        this.changeDetector.markForCheck();
      },
    });
  }

  private replace(updated: UserResponse): void {
    this.users = this.users.map((u) => (u.userId === updated.userId ? updated : u));
    this.changeDetector.markForCheck();
  }
}
