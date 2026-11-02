import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';
import {
  PROJECT_STATUS_LABELS,
  PlanRevision,
  Project,
  ProjectStatusName,
  formatBob,
  formatMinutes,
} from '../../../core/models/project';
import { ProjectService, apiErrorMessage } from '../../../core/services/project.service';
import { DelegationDialog } from '../delegation-dialog/delegation-dialog';
import { PlanForm } from '../plan-form/plan-form';
import { ProjectForm } from '../project-form/project-form';
import { Router } from '@angular/router';

type Dialog =
  | { kind: 'create' }
  | { kind: 'details'; project: Project }
  | { kind: 'plan'; project: Project }
  | { kind: 'delegate'; project: Project };

/**
 * Épica 1 — Proyectos. El Jefe ve todos y gestiona plan, estado y delegación;
 * el Encargado ve los que dirige y solo edita nombre y descripción.
 */
@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [FormsModule, ProjectForm, PlanForm, DelegationDialog],
  template: `<section class="projects-page">
  <header class="projects-header">
    <div>
      <h2>Proyectos <span class="projects-count">{{ projects.length }}</span></h2>
      <p>{{ auth.isChief() ? 'Gestiona presupuesto, estado y encargados de todos los proyectos.' : 'Proyectos que diriges.' }}</p>
    </div>
    <div class="projects-header-actions">
      <button type="button" class="gt-btn" (click)="load()" [disabled]="loading">↻ Actualizar</button>
      @if (auth.isChief()) {
        <button type="button" class="gt-btn gt-btn-primary" (click)="open({ kind: 'create' })">＋ Nuevo proyecto</button>
      }
    </div>
  </header>

  @if (success) {
    <p class="gt-alert gt-alert-success" role="status">{{ success }}</p>
  }
  @if (error) {
    <p class="gt-alert gt-alert-error" role="alert">{{ error }}</p>
  }

  @if (loading) {
    <p class="projects-empty">Cargando proyectos…</p>
  } @else if (projects.length === 0 && !error) {
    <p class="projects-empty">
      {{ auth.isChief() ? 'Aún no hay proyectos. Crea el primero.' : 'No diriges ningún proyecto todavía.' }}
    </p>
  }

  <div class="projects-grid">
    @for (project of projects; track project.projectId) {
      <article class="project-card" [class.is-closed]="project.closed">
        <header class="project-card-header">
          <h3>{{ project.name }}</h3>
          <span class="project-status" [attr.data-status]="project.status">{{ statusLabels[project.status] }}</span>
        </header>

        @if (project.description) {
          <p class="project-description">{{ project.description }}</p>
        }

        <dl class="project-facts">
          <div><dt>Encargado</dt><dd>{{ project.manager.fullName }}</dd></div>
          <div><dt>Presupuesto</dt><dd>{{ formatBob(project.budgetBob) }}</dd></div>
          <div><dt>Asignado a tareas</dt><dd>{{ formatBob(project.allocatedBudgetBob) }}</dd></div>
          <div><dt>Duración estimada</dt><dd>{{ formatMinutes(project.estimatedDurationMinutes) }}</dd></div>
          <div><dt>Revisión del plan</dt><dd>{{ project.currentRevision }}</dd></div>
        </dl>

        <div class="project-actions">
          <button type="button" class="gt-btn gt-btn-small" (click)="goToTasks(project.projectId)">
            Ver tareas
          </button>

          @if (project.canEditDetails) {
            <button type="button" class="gt-btn gt-btn-small" (click)="open({ kind: 'details', project })">✎ Editar datos</button>
          }
          @if (project.canEditDetails) {
            <button type="button" class="gt-btn gt-btn-small" (click)="open({ kind: 'details', project })">✎ Editar datos</button>
          }
          @if (project.canEditPlan) {
            <button type="button" class="gt-btn gt-btn-small" (click)="open({ kind: 'plan', project })">Bs Editar plan</button>
          }
          @if (project.canDelegate) {
            <button type="button" class="gt-btn gt-btn-small" (click)="open({ kind: 'delegate', project })">⇄ Delegar</button>
          }
          <button type="button" class="gt-btn gt-btn-small" (click)="toggleHistory(project)"
                  [attr.aria-expanded]="history[project.projectId] !== undefined">
            {{ history[project.projectId] !== undefined ? 'Ocultar historial' : 'Historial del plan' }}
          </button>
        </div>

        @if (project.allowedStatuses.length > 0) {
          <div class="project-status-change">
            <label [for]="'status-' + project.projectId">Cambiar estado</label>
            <select [id]="'status-' + project.projectId" [(ngModel)]="statusDraft[project.projectId]"
                    [name]="'status-' + project.projectId">
              <option value="">Selecciona…</option>
              @for (status of project.allowedStatuses; track status) {
                <option [value]="status">{{ statusLabels[status] }}</option>
              }
            </select>
            <button type="button" class="gt-btn gt-btn-small" (click)="changeStatus(project)"
                    [disabled]="!statusDraft[project.projectId] || busyProjectId === project.projectId">Aplicar</button>
          </div>
        }

        @if (history[project.projectId] === 'loading') {
          <p class="gt-field-hint">Cargando historial…</p>
        } @else if (history[project.projectId] === 'error') {
          <p class="gt-alert gt-alert-error">No se pudo cargar el historial.</p>
        } @else if (revisions(project.projectId).length > 0) {
          <ol class="plan-history">
            @for (revision of revisions(project.projectId); track revision.revisionNumber) {
              <li>
                <strong>Rev. {{ revision.revisionNumber }}</strong> · {{ formatBob(revision.budgetBob) }} ·
                {{ formatMinutes(revision.estimatedDurationMinutes) }}
                <span>{{ revision.reason }}</span>
                <small>{{ revision.changedBy.fullName }} — {{ formatDate(revision.changedAt) }}</small>
              </li>
            }
          </ol>
        }
      </article>
    }
  </div>
</section>

@if (dialog) {
  <div class="gt-modal-backdrop" (click)="close()"></div>
  <dialog class="gt-modal" open aria-modal="true" aria-labelledby="project-dialog-title">
    <header class="gt-modal-header">
      <div>
        <span class="gt-eyebrow">PROYECTOS</span>
        <h2 id="project-dialog-title">
          @switch (dialog.kind) {
            @case ('create') { Nuevo proyecto }
            @case ('details') { Editar datos }
            @case ('plan') { Nueva revisión del plan }
            @case ('delegate') { Delegar proyecto }
          }
        </h2>
        @if (dialogProject) {
          <p>{{ dialogProject.name }}</p>
        }
      </div>
      <button type="button" class="gt-modal-close" aria-label="Cerrar" (click)="close()">×</button>
    </header>

    @switch (dialog.kind) {
      @case ('create') {
        <app-project-form (saved)="onSaved($event, 'Proyecto creado con su plan inicial.')" (cancelled)="close()" />
      }
      @case ('details') {
        <app-project-form [project]="dialogProject!" (saved)="onSaved($event, 'Datos del proyecto actualizados.')"
                          (cancelled)="close()" />
      }
      @case ('plan') {
        <app-plan-form [project]="dialogProject!" (saved)="onSaved($event, 'Nueva revisión del plan registrada.')"
                       (cancelled)="close()" />
      }
      @case ('delegate') {
        <app-delegation-dialog [project]="dialogProject!" (saved)="onSaved($event, 'Proyecto delegado.')"
                               (cancelled)="close()" />
      }
    }
  </dialog>
}
`,
  styles: [`.projects-page {
  max-width: 1480px;
  margin: 0 auto;
  padding-top: 28px;
}

.projects-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 20px;
}

.projects-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 650;
}

.projects-header p,
.projects-empty {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.projects-count {
  margin-left: 6px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--martinica);
  font-size: 12px;
}

.projects-header-actions,
.project-actions,
.project-status-change {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.project-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--martinica);
  border-radius: 10px;
  background: var(--arcilla-ebano);
}

.project-card.is-closed {
  opacity: 0.75;
}

.project-card-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.project-card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
  overflow-wrap: anywhere;
}

.project-status {
  flex-shrink: 0;
  height: fit-content;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--martinica);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
}

.project-status[data-status='EN_EJECUCION'] { background: rgba(99, 167, 124, 0.22); color: #C7EBD3; }
.project-status[data-status='EN_PAUSA'] { background: rgba(196, 154, 91, 0.22); color: #F0D9B4; }
.project-status[data-status='CANCELADO'] { background: rgba(185, 103, 103, 0.22); color: #F1C4C4; }

.project-description {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.project-facts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 14px;
  margin: 0;
}

.project-facts dt {
  color: var(--text-muted);
  font-size: 11px;
}

.project-facts dd {
  margin: 2px 0 0;
  font-size: 13px;
  font-weight: 600;
}

.project-status-change label {
  color: var(--text-muted);
  font-size: 12px;
}

.project-status-change select {
  min-height: 30px;
  padding: 0 8px;
  border: 1px solid var(--cometa);
  border-radius: 6px;
  background: var(--espejismo);
  color: var(--text-primary);
}

.plan-history {
  margin: 0;
  padding: 10px 0 0 18px;
  border-top: 1px solid var(--martinica);
  font-size: 12px;
}

.plan-history li {
  margin-bottom: 8px;
}

.plan-history span,
.plan-history small {
  display: block;
  color: var(--text-secondary);
}

.plan-history small {
  color: var(--text-muted);
}
`],
})
export class ProjectList implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  projects: Project[] = [];
  loading = false;
  error: string | null = null;
  success: string | null = null;
  dialog: Dialog | null = null;
  busyProjectId: number | null = null;
  statusDraft: Record<number, ProjectStatusName | ''> = {};
  history: Record<number, PlanRevision[] | 'loading' | 'error'> = {};

  readonly statusLabels = PROJECT_STATUS_LABELS;
  readonly formatBob = formatBob;
  readonly formatMinutes = formatMinutes;

  /** Proyecto sobre el que actúa el diálogo abierto (nulo al crear). */
  get dialogProject(): Project | null {
    return this.dialog && this.dialog.kind !== 'create' ? this.dialog.project : null;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.projectService.list().subscribe({
      next: (projects) => {
        this.projects = projects;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        this.loading = false;
        this.error = apiErrorMessage(error, 'No se pudieron cargar los proyectos.');
        this.changeDetector.markForCheck();
      },
    });
  }

  open(dialog: Dialog): void {
    this.success = null;
    this.dialog = dialog;
  }

  close(): void {
    this.dialog = null;
  }

  onSaved(project: Project, message: string): void {
    this.dialog = null;
    this.success = message;
    this.replace(project);
  }

  changeStatus(project: Project): void {
    const target = this.statusDraft[project.projectId];
    if (!target || this.busyProjectId !== null) {
      return;
    }
    this.busyProjectId = project.projectId;
    this.success = null;
    this.error = null;
    this.projectService.changeStatus(project, target).subscribe({
      next: (updated) => {
        this.busyProjectId = null;
        this.statusDraft[project.projectId] = '';
        this.success = `Estado actualizado a ${this.statusLabels[updated.status]}.`;
        this.replace(updated);
      },
      error: (error) => {
        this.busyProjectId = null;
        this.error = apiErrorMessage(error, 'No se pudo cambiar el estado.');
        this.changeDetector.markForCheck();
      },
    });
  }

  toggleHistory(project: Project): void {
    const id = project.projectId;
    if (this.history[id] && this.history[id] !== 'error') {
      delete this.history[id];
      return;
    }
    this.history[id] = 'loading';
    this.projectService.planHistory(id).subscribe({
      next: (revisions) => {
        this.history[id] = revisions;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.history[id] = 'error';
        this.changeDetector.markForCheck();
      },
    });
  }

  revisions(projectId: number): PlanRevision[] {
    const entry = this.history[projectId];
    return Array.isArray(entry) ? entry : [];
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat('es-BO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
  }

  private replace(project: Project): void {
    const exists = this.projects.some((current) => current.projectId === project.projectId);
    this.projects = exists
      ? this.projects.map((current) => (current.projectId === project.projectId ? project : current))
      : [project, ...this.projects];
    if (Array.isArray(this.history[project.projectId])) {
      delete this.history[project.projectId];
    }
    this.changeDetector.markForCheck();
  }

  goToTasks(projectId: number): void {
    this.router.navigate(['/'], { queryParams: { projectId } });
  }
}
