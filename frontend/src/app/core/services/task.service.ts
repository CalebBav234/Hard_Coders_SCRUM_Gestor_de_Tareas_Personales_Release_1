import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { Task } from '../models/task';

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tasks';

  listTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(this.baseUrl).pipe(catchError(this.handleError));
  }

  createTask(title: string): Observable<Task> {
    return this.http.post<Task>(this.baseUrl, { title }).pipe(catchError(this.handleError));
  }

  activate(task: Task): Observable<Task> {
    return this.http.post<Task>(`${this.baseUrl}/${task.id}/activate`, { version: task.version }).pipe(catchError(this.handleError));
  }

  complete(task: Task): Observable<Task> {
    return this.http.post<Task>(`${this.baseUrl}/${task.id}/complete`, { version: task.version }).pipe(catchError(this.handleError));
  }

  private handleError(error: unknown): Observable<never> {
    return throwError(() => error);
  }
}
