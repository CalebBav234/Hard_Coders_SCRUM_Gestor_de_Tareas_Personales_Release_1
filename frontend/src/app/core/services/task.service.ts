import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, forkJoin, map, throwError } from 'rxjs';
import {
  ActivityDto,
  CreateSubtaskBody,
  CreateTaskBody,
  SubtaskDto,
  TaskDto,
  TaskHistoryDto,
  UpdateSubtaskBody,
  UpdateTaskBody,
} from '../models/api';
import {
  CategorySummary,
  Task,
  TaskHistory,
  TaskPriority,
  UpdateTaskRequest,
  isSubtask,
} from '../models/task';
import {
  archiveTime,
  flattenTasks,
  subtaskHistoryToView,
  subtaskToView,
  taskHistoryToView,
  taskToView,
} from './task-mapper';

type Lifecycle = 'start' | 'pause' | 'complete' | 'reopen';

/**
 * Fachada entre la interfaz y la API V4. Mantiene la misma interfaz pública que antes para que
 * los componentes no cambien: decide si cada operación va a /api/tasks o /api/subtasks.
 */
@Injectable({
  providedIn: 'root',
})
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly tasksUrl = '/api/tasks';
  private readonly subtasksUrl = '/api/subtasks';

  listTasks(query = ''): Observable<Task[]> {
    return this.http
      .get<TaskDto[]>(this.tasksUrl, { params: this.searchParams(query) })
      .pipe(map((tasks) => flattenTasks(tasks)), catchError(this.handleError));
  }

  getTask(id: number): Observable<Task> {
    return this.http
      .get<TaskDto>(`${this.tasksUrl}/${id}`)
      .pipe(map((task) => taskToView(task)), catchError(this.handleError));
  }

  /** Historial unificado: tareas y subtareas terminadas o eliminadas, de la más reciente a la más antigua. */
  listHistory(query = ''): Observable<TaskHistory[]> {
    const params = this.searchParams(query);
    return forkJoin([
      this.http.get<TaskHistoryDto[]>(`${this.tasksUrl}/history`, { params }),
      this.http.get<SubtaskDto[]>(`${this.subtasksUrl}/history`, { params }),
    ]).pipe(
      map(([tasks, subtasks]) =>
        [...tasks.map(taskHistoryToView), ...subtasks.map(subtaskHistoryToView)].sort(
          (a, b) => archiveTime(b) - archiveTime(a),
        ),
      ),
      catchError(this.handleError),
    );
  }

  /** Con {@code parentTaskId} crea una subtarea de esa tarea. */
  createTask(
    title: string,
    priority: TaskPriority,
    categoryName: string,
    parentTaskId?: number | null,
  ): Observable<Task> {
    if (parentTaskId != null) {
      const body: CreateSubtaskBody = { title, description: null };
      return this.http
        .post<SubtaskDto>(`${this.tasksUrl}/${parentTaskId}/subtasks`, body)
        .pipe(map((subtask) => subtaskToView(subtask, priority)), catchError(this.handleError));
    }
    const body: CreateTaskBody = { title, priority, activityName: this.categoryOrNull(categoryName) };
    return this.http
      .post<TaskDto>(this.tasksUrl, body)
      .pipe(map((task) => taskToView(task)), catchError(this.handleError));
  }

  updateTask(id: number, request: UpdateTaskRequest): Observable<Task> {
    if (isSubtask({ id })) {
      const body: UpdateSubtaskBody = {
        title: request.title,
        description: request.description,
        version: request.version,
      };
      return this.http
        .put<SubtaskDto>(`${this.subtasksUrl}/${-id}`, body)
        .pipe(map((subtask) => subtaskToView(subtask, request.priority)), catchError(this.handleError));
    }
    const body: UpdateTaskBody = { ...request };
    return this.http
      .put<TaskDto>(`${this.tasksUrl}/${id}`, body)
      .pipe(map((task) => taskToView(task)), catchError(this.handleError));
  }

  /** Eliminar = cancelar en V4; la tarea pasa al historial. */
  deleteTask(task: Task): Observable<void> {
    return this.http
      .delete<void>(`${this.baseFor(task)}/${this.entityId(task)}`, {
        params: { version: task.version },
      })
      .pipe(catchError(this.handleError));
  }

  activate(task: Task): Observable<Task> {
    return this.lifecycle(task, 'start');
  }

  pause(task: Task): Observable<Task> {
    return this.lifecycle(task, 'pause');
  }

  complete(task: Task): Observable<Task> {
    return this.lifecycle(task, 'complete');
  }

  reopen(task: Task): Observable<Task> {
    return this.lifecycle(task, 'reopen');
  }

  /** La categoría es la actividad de la tarea; las subtareas no tienen categoría propia. */
  changeCategory(task: Task, categoryName: string | null): Observable<Task> {
    return this.http
      .put<TaskDto>(`${this.tasksUrl}/${this.entityId(task)}/activity`, {
        activityName: this.categoryOrNull(categoryName),
        version: task.version,
      })
      .pipe(map((updated) => taskToView(updated)), catchError(this.handleError));
  }

  listCategories(): Observable<CategorySummary[]> {
    return this.http.get<ActivityDto[]>('/api/activities').pipe(
      map((activities) => activities.map((activity) => ({ id: activity.activityId, name: activity.name }))),
      catchError(this.handleError),
    );
  }

  private lifecycle(task: Task, action: Lifecycle): Observable<Task> {
    const url = `${this.baseFor(task)}/${this.entityId(task)}/${action}`;
    const body = { version: task.version };
    if (isSubtask(task)) {
      return this.http
        .post<SubtaskDto>(url, body)
        .pipe(map((subtask) => subtaskToView(subtask, task.priority)), catchError(this.handleError));
    }
    return this.http
      .post<TaskDto>(url, body)
      .pipe(map((updated) => taskToView(updated)), catchError(this.handleError));
  }

  private baseFor(task: Task): string {
    return isSubtask(task) ? this.subtasksUrl : this.tasksUrl;
  }

  private entityId(task: Task): number {
    return task.entityId ?? Math.abs(task.id);
  }

  private categoryOrNull(categoryName: string | null | undefined): string | null {
    const trimmed = categoryName?.trim() ?? '';
    return trimmed === '' ? null : trimmed;
  }

  private searchParams(query: string): Record<string, string> {
    const q = query.trim();
    return q ? { q } : {};
  }

  private handleError(error: unknown): Observable<never> {
    return throwError(() => error);
  }
}
