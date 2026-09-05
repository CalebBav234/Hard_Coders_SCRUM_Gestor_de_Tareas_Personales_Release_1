import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import {
  CategorySummary,
  CreateTaskRelationRequest,
  Task,
  TaskHistory,
  TaskPriority,
  TaskRelationResponse,
  UpdateTaskRequest,
} from '../models/task';

@Injectable({
  providedIn: 'root',
})
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tasks';

  listTasks(query = ''): Observable<Task[]> {
    const q = query.trim();
    return this.http
      .get<Task[]>(this.baseUrl, { params: q ? { q } : {} })
      .pipe(catchError(this.handleError));
  }

  listHistory(query = ''): Observable<TaskHistory[]> {
    const q = query.trim();
    return this.http
      .get<TaskHistory[]>(`${this.baseUrl}/history`, { params: q ? { q } : {} })
      .pipe(catchError(this.handleError));
  }

  createTask(title: string, priority: TaskPriority, categoryName: string, parentTaskId?: number | null): Observable<Task> {
    return this.http
      .post<Task>(this.baseUrl, { title, priority, categoryName, parentTaskId: parentTaskId ?? null })
      .pipe(catchError(this.handleError));
  }

  updateTask(id: number, request: UpdateTaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.baseUrl}/${id}`, request).pipe(catchError(this.handleError));
  }

  deleteTask(task: Task): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/${task.id}`, {
        params: { version: task.version },
      })
      .pipe(catchError(this.handleError));
  }

  activate(task: Task): Observable<Task> {
    return this.http
      .post<Task>(`${this.baseUrl}/${task.id}/activate`, { version: task.version })
      .pipe(catchError(this.handleError));
  }

  complete(task: Task): Observable<Task> {
    return this.http
      .post<Task>(`${this.baseUrl}/${task.id}/complete`, { version: task.version })
      .pipe(catchError(this.handleError));
  }

  reopen(task: Task): Observable<Task> {
    return this.http
      .post<Task>(`${this.baseUrl}/${task.id}/reopen`, { version: task.version })
      .pipe(catchError(this.handleError));
  }

  pause(task: Task): Observable<Task> {
    return this.http
      .post<Task>(`${this.baseUrl}/${task.id}/pause`, { version: task.version })
      .pipe(catchError(this.handleError));
  }

  changeCategory(task: Task, categoryName: string | null): Observable<Task> {
    return this.http
      .put<Task>(`${this.baseUrl}/${task.id}/category`, {
        categoryName,
        version: task.version,
      })
      .pipe(catchError(this.handleError));
  }

  listCategories(): Observable<CategorySummary[]> {
    return this.http.get<CategorySummary[]>('/api/categories').pipe(catchError(this.handleError));
  }

  // --- Nuevos métodos para Relaciones de Tareas (US-18) ---

  getRelations(taskId: number): Observable<TaskRelationResponse[]> {
    return this.http
      .get<TaskRelationResponse[]>(`${this.baseUrl}/${taskId}/relations`)
      .pipe(catchError(this.handleError));
  }

  addRelation(taskId: number, request: CreateTaskRelationRequest): Observable<TaskRelationResponse> {
    return this.http
      .post<TaskRelationResponse>(`${this.baseUrl}/${taskId}/relations`, request)
      .pipe(catchError(this.handleError));
  }

  removeRelation(taskId: number, relationId: number): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/${taskId}/relations/${relationId}`)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: unknown): Observable<never> {
    return throwError(() => error);
  }
}
