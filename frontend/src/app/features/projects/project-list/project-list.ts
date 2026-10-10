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
  templateUrl: './project-list.html',
  styleUrl: './project-list.css',
})
export class ProjectList implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly changeDetector = inject(ChangeDetectorRef);
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
}
