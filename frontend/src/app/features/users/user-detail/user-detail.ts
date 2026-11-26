import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { UserResponse } from '../../../core/models/user';
import { UserService } from '../../../core/services/user.service';
import { apiErrorMessage } from '../../../core/services/project.service';

@Component({
  selector: 'app-user-detail',
  standalone: true,
  imports: [CommonModule],
  template: `<section class="user-detail-page" aria-label="Detalle de usuario">
  <header class="user-detail-header">
    <button type="button" class="gt-btn gt-btn-small" (click)="goBack()">← Volver a usuarios</button>
    <div>
      <span class="gt-eyebrow">DETALLE DE USUARIO</span>
      <h2>{{ user?.fullName }}</h2>
      <p>{{ user?.email }}</p>
    </div>
  </header>

  @if (loading) {
    <p class="user-detail-empty" role="status" aria-label="Cargando">Cargando usuario…</p>
  } @else if (notFound) {
    <p class="user-detail-empty">No se encontró el usuario solicitado.</p>
    <button type="button" class="gt-btn gt-btn-primary" (click)="goBack()">Volver a usuarios</button>
  } @else if (user) {
    <dl class="user-detail-facts">
      <div><dt>Nombres</dt><dd>{{ user.givenNames }}</dd></div>
      <div><dt>Apellidos</dt><dd>{{ user.familyNames }}</dd></div>
      <div><dt>Correo</dt><dd>{{ user.email }}</dd></div>
      <div><dt>Estado</dt>
        <dd><span class="user-status" [class.user-status-active]="user.active" [class.user-status-inactive]="!user.active">
          {{ user.active ? 'Activo' : 'Inactivo' }}
        </span></dd></div>
      <div><dt>Rol</dt><dd>{{ user.chief ? 'Jefe' : 'Personal' }}</dd></div>
    </dl>
    <p class="gt-field-hint">No se muestran credenciales, contraseñas ni tokens.</p>
    <div class="user-detail-actions">
      <button type="button" class="gt-btn gt-btn-small" (click)="goEdit(user.userId)" [disabled]="loading">✎ Editar</button>
      <button type="button" class="gt-btn gt-btn-small gt-btn-danger" (click)="requestDeactivate(user)"
              [disabled]="loading || !user.active || deactivatingId === user.userId"
              data-testid="user-detail-deactivate">
        {{ deactivatingId === user.userId ? 'Desactivando…' : (user.active ? 'Desactivar' : 'Ya inactivo') }}
      </button>
    </div>
  }
  @if (error) {
    <p class="gt-alert gt-alert-error" role="alert">{{ error }}</p>
  }

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
  styles: [`.user-detail-page { max-width: 700px; margin: 0 auto; padding-top: 28px; }
.user-detail-header { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 20px; }
.user-detail-header h2 { margin: 0; font-size: 24px; font-weight: 650; }
.user-detail-header p { margin: 6px 0 0; color: var(--text-secondary); font-size: 15px; word-break: break-all; }
.user-detail-empty { text-align: center; padding: 40px 0; color: var(--text-muted); }
.user-detail-facts { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 18px; margin-top: 10px; }
.user-detail-facts dt { color: var(--text-muted); font-size: 12px; }
.user-detail-facts dd { margin: 2px 0 0; font-size: 14px; font-weight: 600; }
.user-detail-actions { display: flex; gap: 10px; margin-top: 18px; flex-wrap: wrap; }
.user-status { display: inline-flex; align-items: center; gap: 6px; padding: 3px 9px; border-radius: 999px; font-size: 11px; font-weight: 650; }
.user-status-active { background: rgba(99, 167, 124, 0.18); color: #2f7d4f; }
.user-status-inactive { background: rgba(185, 103, 103, 0.18); color: #a33a3a; }
.gt-field-hint { margin-top: 10px; font-size: 12px; color: var(--text-muted); }
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
export class UserDetail implements OnInit {
  private readonly usersService = inject(UserService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  user: UserResponse | null = null;
  loading = false;
  error: string | null = null;
  notFound = false;
  confirmTarget: UserResponse | null = null;
  deactivatingId: number | null = null;
  deactivating = false;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('userId'));
    if (!Number.isFinite(id)) {
      this.notFound = true;
      return;
    }
    this.load(id);
  }

  load(userId: number): void {
    this.loading = true;
    this.error = null;
    this.notFound = false;
    this.usersService.get(userId).subscribe({
      next: (user) => {
        this.user = user;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.loading = false;
        if (error?.status === 404) {
          this.notFound = true;
        } else {
          this.error = apiErrorMessage(error, 'No se pudo cargar el usuario.');
        }
        this.changeDetector.markForCheck();
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/users']);
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
        this.user = deactivated;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.deactivating = false;
        this.error = apiErrorMessage(error, 'No se pudo desactivar el usuario.');
        this.changeDetector.markForCheck();
      },
    });
  }
}
