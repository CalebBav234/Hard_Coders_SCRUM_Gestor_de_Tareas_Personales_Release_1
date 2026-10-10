import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateProjectRequest,
  DelegateProjectRequest,
  PlanRevision,
  Project,
  ProjectStatusName,
  ReplanProjectRequest,
  UpdateProjectDetailsRequest,
  UserSummary,
} from '../models/project';

/** Cliente de /api/projects. Los permisos los decide el backend con el usuario del token. */
@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/projects';

  list(): Observable<Project[]> {
    return this.http.get<Project[]>(this.baseUrl);
  }

  get(projectId: number): Observable<Project> {
    return this.http.get<Project>(`${this.baseUrl}/${projectId}`);
  }

  planHistory(projectId: number): Observable<PlanRevision[]> {
    return this.http.get<PlanRevision[]>(`${this.baseUrl}/${projectId}/plan-history`);
  }

  eligibleManagers(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${this.baseUrl}/eligible-managers`);
  }

  create(request: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.baseUrl, request);
  }

  updateDetails(project: Project, name: string, description: string | null): Observable<Project> {
    const body: UpdateProjectDetailsRequest = { name, description, version: project.version };
    return this.http.put<Project>(`${this.baseUrl}/${project.projectId}`, body);
  }

  replan(project: Project, budgetBob: number, estimatedDurationMinutes: number, reason: string): Observable<Project> {
    const body: ReplanProjectRequest = { budgetBob, estimatedDurationMinutes, reason, version: project.version };
    return this.http.put<Project>(`${this.baseUrl}/${project.projectId}/plan`, body);
  }

  changeStatus(project: Project, status: ProjectStatusName): Observable<Project> {
    return this.http.put<Project>(`${this.baseUrl}/${project.projectId}/status`, {
      status,
      version: project.version,
    });
  }

  delegate(project: Project, managerUserId: number, reason: string): Observable<Project> {
    const body: DelegateProjectRequest = { managerUserId, reason, version: project.version };
    return this.http.post<Project>(`${this.baseUrl}/${project.projectId}/delegate`, body);
  }
}

/** Mensaje legible a partir del error del backend ({ error, message }). */
export function apiErrorMessage(error: unknown, fallback: string): string {
  const response = error as { status?: number; error?: { message?: string } };
  if (response?.status === 412) {
    return 'El proyecto cambió mientras lo editabas. Cierra, actualiza la lista y vuelve a intentarlo.';
  }
  if (response?.status === 403) {
    return response.error?.message ?? 'No tienes permiso para esta operación.';
  }
  return response?.error?.message ?? fallback;
}
