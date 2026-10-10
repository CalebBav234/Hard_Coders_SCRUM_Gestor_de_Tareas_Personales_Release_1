import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, forkJoin, map, throwError } from 'rxjs';
import {
  ActivityDetailDto,
  AssignedTaskDto,
  CreateSubtaskBody,
  CreateTaskBody,
  SubtaskDto,
  TaskDto,
  TaskHistoryDto,
  TransitionBody,
  UpdateSubtaskBody,
  UpdateTaskBody,
  UpdateTaskPlanBody,
} from '../models/api';
import {
  CategorySummary as CategoryView,
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
import { UserSummary } from '../models/project';

type Lifecycle = 'start' | 'pause' | 'complete' | 'reopen';

/**
 * Fachada entre la interfaz y la API V4. Todas las lecturas y escrituras van ligadas a un
 * proyecto explícito o a una actividad concreta: ya no se deduce el "espacio de trabajo" del
 * primer proyecto porque el backend rechaza cualquier cruce de proyectos con 404.
 */
@Injectable({
  providedIn: 'root',
})
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly tasksUrl = '/api/tasks';
  private readonly subtasksUrl = '/api/subtasks';
  private readonly activitiesUrl = '/api/activities';

  /** Tareas visibles del proyecto (Jefe o Encargado de ese proyecto). */
  listTasks(projectId: number, query = ''): Observable<Task[]> {
    return this.http
      .get<TaskDto[]>(this.tasksUrl, { params: { projectId, ...this.searchParams(query) } })
      .pipe(map((tasks) => flattenTasks(tasks)), catchError(this.handleError));
  }

  /** US3.1: "Mis tareas"; el Personal solo ve lo que tiene asignado. */
  listAssignedTasks(query = ''): Observable<Task[]> {
    return this.http
      .get<AssignedTaskDto[]>(`${this.tasksUrl}/assigned`, { params: this.searchParams(query) })
      .pipe(
        map((tasks) =>
          tasks.map((assigned) => {
            const taskDto: TaskDto = {
              ...assigned,
              allocatedBudgetBob: 0,
              estimatedDurationMinutes: 0,
            };
            return taskToView(taskDto);
          }),
        ),
        catchError(this.handleError),
      );
  }

  getTask(id: number): Observable<Task> {
    return this.http
      .get<TaskDto>(`${this.tasksUrl}/${id}`)
      .pipe(map((task) => taskToView(task)), catchError(this.handleError));
  }

  /** Historial unificado de un proyecto: tareas y subtareas terminadas o canceladas. */
  listHistory(projectId: number, query = ''): Observable<TaskHistory[]> {
    const params = { projectId, ...this.searchParams(query) };
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

  /** Con {@code parentTaskId} crea una subtarea de esa tarea; si no, una tarea en la actividad. */
  createTask(
    activityId: number,
    title: string,
    priority: TaskPriority,
    allocatedBudgetBob: number,
    estimatedDurationMinutes: number,
    parentTaskId?: number | null,
  ): Observable<Task> {
    if (parentTaskId != null) {
      const body: CreateSubtaskBody = { title, description: null };
      return this.http
        .post<SubtaskDto>(`${this.tasksUrl}/${parentTaskId}/subtasks`, body)
        .pipe(map((subtask) => subtaskToView(subtask, priority)), catchError(this.handleError));
    }
    const body: CreateTaskBody = { activityId, title, priority, allocatedBudgetBob, estimatedDurationMinutes };
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

  /** US2.2: presupuesto y duración de la tarea, con motivo obligatorio. */
  updatePlan(taskId: number, budgetBob: number, minutes: number, reason: string, version: number): Observable<Task> {
    const body: UpdateTaskPlanBody = { allocatedBudgetBob: budgetBob, estimatedDurationMinutes: minutes, reason, version };
    return this.http
      .put<TaskDto>(`${this.tasksUrl}/${taskId}/plan`, body)
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

  activate(task: Task, reason?: string): Observable<Task> {
    return this.lifecycle(task, 'start', reason);
  }

  pause(task: Task, reason?: string): Observable<Task> {
    return this.lifecycle(task, 'pause', reason);
  }

  complete(task: Task, reason?: string): Observable<Task> {
    return this.lifecycle(task, 'complete', reason);
  }

  reopen(task: Task, reason?: string): Observable<Task> {
    return this.lifecycle(task, 'reopen', reason);
  }

  /** US2.2: mover la tarea a otra actividad del mismo proyecto. */
  changeActivity(task: Task, activityId: number): Observable<Task> {
    return this.http
      .put<TaskDto>(`${this.tasksUrl}/${this.entityId(task)}/activity`, {
        activityId,
        version: task.version,
      })
      .pipe(map((updated) => taskToView(updated)), catchError(this.handleError));
  }

  /** Las actividades ("categorías") de la etapa seleccionada; oculta "Sin categoría". */
  listCategories(stageId: number): Observable<CategoryView[]> {
    return this.http.get<ActivityDetailDto[]>(`${this.activitiesUrl}`, { params: { stageId } }).pipe(
      map((activities) =>
        activities
          .filter((activity) => activity.name.toLowerCase() !== 'sin categoría')
          .map((activity) => ({ id: activity.activityId, name: activity.name })),
      ),
      catchError(this.handleError),
    );
  }

  private lifecycle(task: Task, action: Lifecycle, reason?: string): Observable<Task> {
    const url = `${this.baseFor(task)}/${this.entityId(task)}/${action}`;
    const body: TransitionBody = { version: task.version };
    if (reason && action === 'complete') {
      body.reason = reason;
    }
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

  private searchParams(query: string): Record<string, string> {
    const q = query.trim();
    return q ? { q } : {};
  }

  private handleError(error: unknown): Observable<never> {
    return throwError(() => error);
  }
}
