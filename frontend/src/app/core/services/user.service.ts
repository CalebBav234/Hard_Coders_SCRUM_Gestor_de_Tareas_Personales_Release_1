import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateUserRequest, UpdateUserRequest, UserResponse } from '../models/user';

/** Cliente de /api/users. Los permisos los decide el backend con el usuario del token. */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/users';

  list(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.baseUrl);
  }

  get(userId: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${userId}`);
  }

  create(request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.baseUrl, request);
  }

  update(userId: number, request: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${userId}`, request);
  }

  deactivate(userId: number): Observable<UserResponse> {
    return this.http.delete<UserResponse>(`${this.baseUrl}/${userId}`);
  }
}
