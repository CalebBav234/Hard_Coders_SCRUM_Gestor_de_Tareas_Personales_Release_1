import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { Task,TaskHistory as TaskHistoryModel, TaskStatusHistory } from '../../../core/models/task';
import { TaskService } from '../../../core/services/task.service';
import { ProjectService } from '../../../core/services/project.service';
import { Project } from '../../../core/models/project';

@Component({
  selector: 'app-task-history',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './task-history.html',
  styleUrl: './task-history.css',
})
export class TaskHistory implements OnInit, OnDestroy {
  private readonly taskService = inject(TaskService);
  private readonly projectService = inject(ProjectService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private request?: Subscription;
  private projectsRequest?: Subscription;

  history: TaskHistoryModel[] = [];
  tasks: Task[] = [];

  /**
   * Proyecto sobre el que se muestra el histórico. Lo elige el usuario: el histórico
   * nunca se dedujo del primer proyecto.
   */
  selectedProjectId: number | null = null;

  /** Proyectos que el actor puede consultar (Jefe: todos; Encargado: los delegados). */
  projects: Project[] = [];

  loading = false;
  error: string | null = null;
  searchDraft = '';
  activeSearch = '';

  ngOnInit(): void {
    this.loadProjects();
  }

  ngOnDestroy(): void {
    this.request?.unsubscribe();
    this.projectsRequest?.unsubscribe();
  }

  /** El histórico necesita un proyecto: sin el, no se pide nada al backend. */
  private loadProjects(): void {
    this.projectsRequest?.unsubscribe();
    this.projectsRequest = this.projectService.list().subscribe({
      next: (projects) => {
        this.projects = projects;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.projects = [];
        this.changeDetector.markForCheck();
      },
    });
  }

  /** El Personal (y quien no dirija proyectos) no tiene histórico que consultar. */
  get hasProjects(): boolean {
    return this.projects.length > 0;
  }

  selectProject(projectId: number | null): void {
    this.selectedProjectId = projectId;
    this.activeSearch = '';
    this.searchDraft = '';
    this.history = [];
    this.tasks = [];
    this.error = null;
    if (projectId === null) {
      this.changeDetector.markForCheck();
      return;
    }
    this.loadHistory();
    this.loadTasks();
    this.changeDetector.markForCheck();
  }

  loadHistory(): void {
    // Sin proyecto elegido no se llama al backend: un projectId inventado solo daría 404.
    if (this.selectedProjectId === null) {
      this.history = [];
      this.error = null;
      this.changeDetector.markForCheck();
      return;
    }

    // A task may change while the previous request is still loading.
    // Cancel that stale request; never discard the newer refresh.
    this.request?.unsubscribe();
    this.loading = true;
    this.error = null;
    this.request = this.taskService.listHistory(this.selectedProjectId, this.activeSearch).subscribe({
      next: (history) => {
        this.history = history;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: (response: { status?: number }) => {
        this.loading = false;
        this.error =
          response.status === 404
            ? 'No se encontró el proyecto o ya no tienes acceso a él. Elige el proyecto de nuevo.'
            : 'No se pudo cargar el historial. Comprueba que el backend y PostgreSQL estén disponibles y vuelve a intentarlo.';
        this.changeDetector.markForCheck();
      },
    });
  }

  loadTasks(): void {
    if (this.selectedProjectId === null) {
      this.tasks = [];
      this.changeDetector.markForCheck();
      return;
    }

    this.taskService.listTasks(this.selectedProjectId).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.changeDetector.markForCheck();
      },

      error: (error) => {
        console.error(
          'Error al cargar tareas para el historial:',
          error
        );
      }
    });
  }

  searchHistory(event: Event): void {
    event.preventDefault();
    if (this.loading) return;
    this.activeSearch = this.searchDraft.trim();
    this.loadHistory();
  }

  updateSearchDraft(event: Event): void {
    this.searchDraft = (event.target as HTMLInputElement).value;
  }

  clearSearch(): void {
    if (this.loading) return;
    this.searchDraft = '';
    this.activeSearch = '';
    this.loadHistory();
  }

  isSubtask(task: TaskHistoryModel): boolean {
    return task.parentTaskId != null;
  }

  getParentTaskTitle(task: TaskHistoryModel): string {
    if (!task.parentTaskId) {
      return '';
    }

    const parentTask = this.tasks.find(
      (currentTask) =>
        currentTask.id === task.parentTaskId
    );

    return parentTask?.title ?? 'Tarea principal no disponible';
  }

  archiveLabel(task: TaskHistoryModel): string {
    return task.deletedAt ? 'Eliminada' : 'Terminada';
  }

  archiveDate(task: TaskHistoryModel): string {
    return this.formatDate(task.deletedAt ?? task.completedAt ?? task.updatedAt ?? task.createdAt);
  }

  eventLabel(event: TaskStatusHistory): string {
    return event.fromStatus
      ? `${event.fromStatus} → ${event.toStatus}`
      : `Creada como ${event.toStatus}`;
  }

  formatDate(value?: string | null): string {
    if (!value) return 'Fecha no disponible';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Fecha no disponible';
    return new Intl.DateTimeFormat('es-BO', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
  }

  formatDuration(seconds?: number): string {
    if (seconds == null || seconds <= 0) return '0 min';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    if (hours > 0) return `${hours} h ${minutes} min`;
    if (minutes > 0) return `${minutes} min ${secs} s`;
    return `${secs} s`;
  }
}
