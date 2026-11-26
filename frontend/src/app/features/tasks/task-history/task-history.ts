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
  template: `<div class="history-panel" [attr.aria-busy]="loading">
  <div class="history-header">
    <div>
      <h2 id="history-heading">Historial de tareas</h2>
      <p>Consulta tareas terminadas y eliminadas junto con sus cambios de estado.</p>
    </div>
    <span class="history-count">{{ history.length }} archivada(s)</span>
    <button type="button" class="btn-refresh" (click)="loadHistory()" [disabled]="loading">
      Actualizar historial
    </button>
  </div>

  <!-- El histórico es de un proyecto concreto: lo elige el usuario, nunca el "primero". -->
  <div class="history-project">
    <label for="history-project">Proyecto</label>
    <select
      id="history-project"
      [(ngModel)]="selectedProjectId"
      (ngModelChange)="selectProject($event)"
    >
      <option [ngValue]="null">— Selecciona un proyecto —</option>

      @for (project of projects; track project.projectId) {
        <option [ngValue]="project.projectId">{{ project.name }}</option>
      }
    </select>
  </div>

  <form class="history-search" role="search" (submit)="searchHistory($event)">
    <label for="history-search">Buscar en el historial por título o descripción</label>
    <div class="search-controls">
      <input
        id="history-search"
        type="search"
        maxlength="160"
        placeholder="Ej.: planificación mensual"
        [value]="searchDraft"
        (input)="updateSearchDraft($event)"
        [disabled]="loading"
      />
      <button type="submit" class="btn-search" [disabled]="loading || selectedProjectId === null">Buscar</button>
      @if (activeSearch) {
        <button type="button" class="btn-clear" (click)="clearSearch()" [disabled]="loading">
          Limpiar
        </button>
      }
    </div>
    @if (activeSearch && !loading && !error) {
      <p class="search-summary" role="status">
        {{ history.length }} resultado(s) para “{{ activeSearch }}”
      </p>
    }
  </form>

  @if (error) {
    <div class="history-feedback" role="alert">{{ error }}</div>
  }

  @if (loading) {
    <p class="loading-message">Cargando historial...</p>
  }

  <div class="history-list">
    @for (task of history; track task.id) {
      <article class="history-card">
        <header>
          <div>
            <div class="history-task-type">
              @if (isSubtask(task)) {
                <span class="task-type-badge subtask">
                  SUBTAREA
                </span>
              } @else {
                <span class="task-type-badge main-task">
                  TAREA PRINCIPAL
                </span>
              }
            </div>

            <h3>{{ task.title }}</h3>

            @if (isSubtask(task)) {
              <p class="history-parent-task">
                Tarea principal:
                <strong>
                  {{ getParentTaskTitle(task) }}
                </strong>
              </p>
            }

            @if (task.description) {
              <p class="history-description">
                {{ task.description }}
              </p>
            }
          </div>

          <span
            class="archive-badge"
            [class.deleted]="task.deletedAt"
          >
            {{ archiveLabel(task) }}
          </span>
        </header>

        <div class="history-meta">
          <span>Fecha: {{ archiveDate(task) }}</span>
          @if (!isSubtask(task)) {
            <span>Prioridad: {{ task.priority }}</span>
            <span>Categoría: {{ task.categoryName ?? 'Sin categoría' }}</span>
            <span>Tiempo activo: {{ formatDuration(task.effectiveActiveSeconds) }}</span>
          }
        </div>

        <details class="timeline">
          <summary>Ver cambios de estado ({{ task.events.length }})</summary>
          @if (task.events.length > 0) {
            <ol>
              @for (event of task.events; track event.id) {
                <li>
                  <strong>{{ eventLabel(event) }}</strong>
                  <time [attr.datetime]="event.changedAt">{{ formatDate(event.changedAt) }}</time>
                  @if (event.changeReason) {
                    <span>{{ event.changeReason }}</span>
                  }
                </li>
              }
            </ol>
          } @else {
            <p>No hay transiciones registradas para esta tarea.</p>
          }
        </details>
      </article>
    } @empty {
      @if (!loading && !error) {
        @if (selectedProjectId === null) {
          <p class="empty-message">
            @if (hasProjects) {
              Elige un proyecto para ver sus tareas terminadas y eliminadas.
            } @else {
              No diriges ningún proyecto: el historial se consulta proyecto por proyecto.
            }
          </p>
        } @else {
          <p class="empty-message">
            {{
              activeSearch
                ? 'No se encontraron tareas anteriores con esa búsqueda.'
                : 'Todavía no hay tareas terminadas o eliminadas.'
            }}
          </p>
        }
      }
    }
  </div>
</div>
`,
  styles: [`/* HISTÓRICO — solo estilos propios de esta pantalla.
   Las primitivas compartidas (botones, campos, avisos, tarjetas,
   insignias) viven en src/design-system.css. La hoja anterior
   pesaba 10,96 kB por duplicarlas y superaba el presupuesto de
   4 kB (aviso) de Angular. */

:host { display: block; width: 100%; min-height: 100vh; background: var(--espejismo); }

.history-panel { width: 100%; max-width: 1000px; margin: 0 auto; padding: 40px 30px 60px; box-sizing: border-box; }
.history-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; margin-bottom: 28px; }
.history-header > div:first-child { min-width: 0; }
.history-header h2 { margin: 0; color: #f4f4f8; font-size: 30px; line-height: 1.2; font-weight: 650; letter-spacing: -0.03em; }
.history-header p { margin: 8px 0 0; color: #aaaac2; font-size: 13px; line-height: 1.5; }
.history-count { flex-shrink: 0; align-self: center; display: inline-flex; align-items: center; min-height: 26px; padding: 0 10px; border: 1px solid var(--martinica); border-radius: 999px; background: var(--arcilla-ebano); color: #c8c8df; font-size: 11px; font-weight: 700; }
.btn-refresh { flex-shrink: 0; min-height: 38px; padding: 0 14px; font-size: 11px; }
.history-project { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; padding: 11px 14px; border: 1px solid var(--martinica); border-radius: 9px; background: var(--arcilla-ebano); }
.history-project label { flex-shrink: 0; color: #c8c8d8; font-size: 11px; font-weight: 650; }
.history-project select { flex: 1; min-width: 0; min-height: 36px; padding: 0 10px; border: 1px solid var(--martinica); border-radius: 6px; background: var(--espejismo); color: #eeeeF5; font-family: inherit; font-size: 12px; outline: none; }
.history-project select:hover { border-color: var(--cometa); }
.history-project select:focus { border-color: var(--scampi); box-shadow: 0 0 0 2px rgb(104 104 166 / 12%); }
.history-search { margin-bottom: 28px; padding: 15px; border: 1px solid var(--martinica); border-radius: 9px; background: var(--arcilla-ebano); }
.history-search label { display: block; margin-bottom: 8px; color: #c8c8d8; font-size: 11px; font-weight: 650; }
.search-controls { display: flex; gap: 8px; }
.search-controls input { flex: 1; min-width: 0; min-height: 40px; padding: 0 12px; border: 1px solid var(--martinica); border-radius: 6px; background: var(--espejismo); color: #eeeeF5; font-family: inherit; font-size: 12px; outline: none; transition: border-color .15s ease, box-shadow .15s ease; }
.search-controls input::placeholder { color: var(--text-placeholder); }
.search-controls input:hover { border-color: var(--cometa); }
.search-controls input:focus { border-color: var(--scampi); box-shadow: 0 0 0 2px rgb(104 104 166 / 12%); }
.search-summary { margin: 9px 0 0; color: #b6b6ca; font-size: 11px; }

/* ---------------- feedback ---------------- */
.history-feedback { margin-bottom: 18px; padding: 11px 13px; border: 1px solid #98505e; border-radius: 7px; background: #4a252d; color: #fff; font-size: 11px; line-height: 1.5; font-weight: 600; }

/* ---------------- rejilla y tarjetas ---------------- */
.history-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; align-items: start; }
.history-card { height: 390px; overflow: hidden; }
.history-card:hover { border-color: var(--cometa); transform: translateY(-2px); box-shadow: 0 10px 24px rgb(0 0 0 / 22%); }
.history-card h3 { font-size: 15px; line-height: 1.4; font-weight: 650; }
.history-description { color: #9999b3; font-size: 11px; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 3; overflow: hidden; }
.history-task-type { margin-bottom: 7px; }
.history-parent-task { margin: 6px 0 0; color: #aaaac2; font-size: 11px; line-height: 1.5; }
.history-parent-task strong { color: #d6d6e5; }
.history-meta { margin-top: 14px; }
.history-meta span { min-height: 23px; padding: 3px 7px; border: 1px solid rgb(255 255 255 / 8%); border-radius: 5px; background: rgb(18 18 38 / 30%); color: #aaaac0; font-size: 9px; font-weight: 600; }

/* ---------------- línea de tiempo ---------------- */
.timeline { margin-top: auto; padding-top: 11px; border-top: 1px solid rgb(255 255 255 / 8%); flex-shrink: 0; height: 167px; box-sizing: border-box; overflow: hidden; }
.timeline summary { color: #b9b9ce; font-size: 10px; font-weight: 650; cursor: pointer; user-select: none; }
.timeline summary:hover { color: #eeeeF5; }
.timeline summary:focus-visible { outline: 2px solid var(--scampi); outline-offset: 2px; }
.timeline ol { height: 125px; margin: 10px 0 0; padding-left: 19px; padding-right: 6px; box-sizing: border-box; overflow-y: auto; overflow-x: hidden; scrollbar-width: thin; scrollbar-color: var(--cometa) rgb(18 18 38 / 35%); }
.timeline ol::-webkit-scrollbar { width: 6px; }
.timeline ol::-webkit-scrollbar-track { background: rgb(18 18 38 / 35%); border-radius: 999px; }
.timeline ol::-webkit-scrollbar-thumb { background: var(--cometa); border-radius: 999px; }
.timeline ol::-webkit-scrollbar-thumb:hover { background: var(--scampi); }
.timeline li { margin: 7px 0; color: #c4c4d5; font-size: 10px; line-height: 1.4; }
.timeline li strong { color: #e2e2eb; }
.timeline time, .timeline li span { display: block; }
.timeline > p { margin: 6px 0 0; color: var(--text-muted); font-size: 11px; }

@media (max-width: 760px) {
  .history-panel { padding: 26px 14px 50px; }
  .history-list { grid-template-columns: 1fr; }
  .history-header { flex-direction: column; gap: 12px; }
}
`],
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
