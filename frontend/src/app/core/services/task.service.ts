import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { CategorySummary, Task, TaskPriority, UpdateTaskRequest } from '../models/task';

@Injectable({
  providedIn: 'root',
})
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tasks';

  listTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(this.baseUrl).pipe(catchError(this.handleError));
  }

  createTask(title: string, priority: TaskPriority, categoryName: string): Observable<Task> {
    return this.http
      .post<Task>(this.baseUrl, { title, priority, categoryName })
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
    return this.http
      .get<CategorySummary[]>('/api/categories')
      .pipe(catchError(this.handleError));
  }

  private handleError(error: unknown): Observable<never> {
    return throwError(() => error);
  }
}
