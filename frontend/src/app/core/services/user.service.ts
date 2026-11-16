import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateUserRequest, UserResponse } from '../models/user';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  create(request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>('/api/users', request);
  }
}
