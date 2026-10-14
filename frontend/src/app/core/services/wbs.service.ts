import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ActivityDetailDto,
  AssignmentDto,
  HierarchyStatusBody,
  ProjectProgressDto,
  StageDto,
  UpdateActivityBody,
  UpdateStageBody,
} from '../models/api';
import { UserSummary } from '../models/project';

/**
 * Jerarquía Proyecto → Etapa → Actividad (US2.2), asignación de Personal (US2.3) y
 * progreso en cascada (US3.3).
 *
 * Regla central: **no existe ningún proyecto por defecto**. La pantalla guarda el proyecto
 * elegido por el usuario (o ninguno) y toda petición lo lleva explícito; si no hay proyecto
 * seleccionado la interfaz muestra el estado "elige un proyecto" en lugar de cargar datos de
 * otro sitio.
 */
@Injectable({
  providedIn: 'root',
})
export class WbsService {
  private readonly http = inject(HttpClient);
  private readonly projectsUrl = '/api/projects';
  private readonly stagesUrl = '/api/stages';
  private readonly activitiesUrl = '/api/activities';
  private readonly tasksUrl = '/api/tasks';

  listStages(projectId: number): Observable<StageDto[]> {
    return this.http.get<StageDto[]>(`${this.projectsUrl}/${projectId}/stages`);
  }

  createStage(projectId: number, name: string, description: string | null): Observable<StageDto> {
    return this.http.post<StageDto>(`${this.projectsUrl}/${projectId}/stages`, { name, description });
  }

  /** US2.2: renombrar o describir de nuevo la etapa (requiere la versión mostrada). */
  updateStage(stageId: number, name: string, description: string | null, version: number): Observable<StageDto> {
    const body: UpdateStageBody = { name, description, version };
    return this.http.put<StageDto>(`${this.stagesUrl}/${stageId}`, body);
  }

  /** US2.2: PENDIENTE | EN_PROGRESO | COMPLETADA | CANCELADA sobre la etapa. */
  changeStageStatus(stageId: number, status: HierarchyStatusBody['status'], version: number): Observable<StageDto> {
    return this.http.put<StageDto>(`${this.stagesUrl}/${stageId}/status`, { status, version });
  }

  listActivities(stageId: number): Observable<ActivityDetailDto[]> {
    return this.http.get<ActivityDetailDto[]>(`${this.stagesUrl}/${stageId}/activities`);
  }

  createActivity(stageId: number, name: string, description: string | null): Observable<ActivityDetailDto> {
    return this.http.post<ActivityDetailDto>(`${this.stagesUrl}/${stageId}/activities`, { name, description });
  }

  /** US2.2: renombrar o describir de nuevo la actividad (requiere la versión mostrada). */
  updateActivity(activityId: number, name: string, description: string | null, version: number): Observable<ActivityDetailDto> {
    const body: UpdateActivityBody = { name, description, version };
    return this.http.put<ActivityDetailDto>(`${this.activitiesUrl}/${activityId}`, body);
  }

  /** US2.2: PENDIENTE | EN_PROGRESO | COMPLETADA | CANCELADA sobre la actividad. */
  changeActivityStatus(
    activityId: number,
    status: HierarchyStatusBody['status'],
    version: number,
  ): Observable<ActivityDetailDto> {
    return this.http.put<ActivityDetailDto>(`${this.activitiesUrl}/${activityId}/status`, { status, version });
  }

  /** Trabajadores activos que el Encargado puede asignar en el proyecto. */
  listWorkers(projectId: number): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${this.projectsUrl}/${projectId}/workers`);
  }

  listAssignments(taskId: number): Observable<AssignmentDto[]> {
    return this.http.get<AssignmentDto[]>(`${this.tasksUrl}/${taskId}/assignments`);
  }

  assign(taskId: number, workerUserId: number, reason: string | null): Observable<AssignmentDto> {
    return this.http.post<AssignmentDto>(`${this.tasksUrl}/${taskId}/assignments`, { workerUserId, reason });
  }

  unassign(taskId: number, workerUserId: number): Observable<void> {
    return this.http.delete<void>(`${this.tasksUrl}/${taskId}/assignments/${workerUserId}`);
  }

  progress(projectId: number): Observable<ProjectProgressDto> {
    return this.http.get<ProjectProgressDto>(`${this.projectsUrl}/${projectId}/progress`);
  }
}
