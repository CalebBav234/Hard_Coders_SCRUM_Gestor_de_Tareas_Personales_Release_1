import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

import { Project } from '../models/project';
import { StageDto, ActivityDetailDto } from '../models/api';
import { Task } from '../models/task';

/**
 * Estado jerárquico compartido y reactivo (Proyecto → Etapa → Actividad → Tarea).
 *
 * Sirve como única fuente de verdad entre componentes para que una mutación exitosa
 * (crear, editar, eliminar, reordenar) se refleje inmediatamente en todas las vistas
 * que la consumen, sin necesidad de recargar la página.
 *
 * Cada colección es un BehaviorSubject: los nuevos suscriptores reciben el último
 * valor al suscribirse y los componentes reaccionan a cada emisión.
 */
@Injectable({
  providedIn: 'root',
})
export class HierarchyStateService {
  private readonly projectsSubject = new BehaviorSubject<Project[]>([]);
  private readonly selectedProjectSubject = new BehaviorSubject<number | null>(null);
  private readonly stagesSubject = new BehaviorSubject<StageDto[]>([]);
  private readonly activitiesSubject = new BehaviorSubject<ActivityDetailDto[]>([]);
  private readonly tasksSubject = new BehaviorSubject<Task[]>([]);

  readonly projects$ = this.projectsSubject.asObservable();
  readonly selectedProject$ = this.selectedProjectSubject.asObservable();
  readonly stages$ = this.stagesSubject.asObservable();
  readonly activities$ = this.activitiesSubject.asObservable();
  readonly tasks$ = this.tasksSubject.asObservable();

  /** Proyectos disponibles (resultado de listar proyectos). */
  setProjects(projects: Project[]): void {
    this.projectsSubject.next([...projects]);
  }

  /** Proyecto actualmente seleccionado. */
  selectProject(projectId: number | null): void {
    this.selectedProjectSubject.next(projectId);
    if (projectId === null) {
      this.stagesSubject.next([]);
      this.activitiesSubject.next([]);
      this.tasksSubject.next([]);
    }
  }

  /** Etapas del proyecto seleccionado. */
  setStages(stages: StageDto[]): void {
    this.stagesSubject.next([...stages]);
  }

  /** Añade una etapa recién creada al estado compartido. */
  addStage(stage: StageDto): void {
    const current = this.stagesSubject.value;
    if (!current.some((s) => s.stageId === stage.stageId)) {
      this.stagesSubject.next([...current, stage]);
    }
  }

  /** Actualiza una etapa existente en el estado compartido. */
  updateStage(updated: StageDto): void {
    this.stagesSubject.next(
      this.stagesSubject.value.map((s) => (s.stageId === updated.stageId ? updated : s)),
    );
  }

  /** Elimina una etapa del estado compartido. */
  removeStage(stageId: number): void {
    this.stagesSubject.next(
      this.stagesSubject.value.filter((s) => s.stageId !== stageId),
    );
  }

  /** Actividades de la etapa seleccionada. */
  setActivities(activities: ActivityDetailDto[]): void {
    this.activitiesSubject.next([...activities]);
  }

  /** Añade una actividad recién creada al estado compartido. */
  addActivity(activity: ActivityDetailDto): void {
    const current = this.activitiesSubject.value;
    if (!current.some((a) => a.activityId === activity.activityId)) {
      this.activitiesSubject.next([...current, activity]);
    }
  }

  /** Actualiza una actividad existente en el estado compartido. */
  updateActivity(updated: ActivityDetailDto): void {
    this.activitiesSubject.next(
      this.activitiesSubject.value.map((a) =>
        a.activityId === updated.activityId ? updated : a,
      ),
    );
  }

  /** Tareas del proyecto seleccionado. */
  setTasks(tasks: Task[]): void {
    this.tasksSubject.next([...tasks]);
  }

  /** Añade una tarea recién creada al estado compartido. */
  addTask(task: Task): void {
    const current = this.tasksSubject.value;
    if (!current.some((t) => t.id === task.id)) {
      this.tasksSubject.next([task, ...current]);
    }
  }

  /** Actualiza una tarea existente en el estado compartido. */
  updateTask(updated: Task): void {
    this.tasksSubject.next(
      this.tasksSubject.value.map((t) => (t.id === updated.id ? updated : t)),
    );
  }

  /** Elimina una tarea del estado compartido. */
  removeTask(taskId: number): void {
    this.tasksSubject.next(
      this.tasksSubject.value.filter((t) => t.id !== taskId),
    );
  }
}
