import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ActivityDetailDto,
  StageDto,
  WorkStatusName,
} from '../../../core/models/api';
import { WbsService } from '../../../core/services/wbs.service';

/**
 * US2.2 — constructor de la WBS: etapas y actividades del proyecto elegido.
 * El panel solo aparece cuando el actor puede gestionar la jerarquía (`canManage`),
 * pero las validaciones reales las hace el backend en cada petición.
 */
@Component({
  selector: 'app-wbs-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './wbs-panel.html',
  styleUrl: './wbs-panel.css',
})
export class WbsPanel implements OnInit {
  private readonly wbs = inject(WbsService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  @Input({ required: true }) projectId!: number;
  /** El backend sigue siendo quien autoriza; esto solo muestra u oculta acciones. */
  @Input() canManage = true;
  /** Avisa de que la estructura cambió (p. ej. actividad nueva) para recargar tareas. */
  @Output() structureChanged = new EventEmitter<void>();

  stages: StageDto[] = [];
  expandedStageId: number | null = null;
  activities: ActivityDetailDto[] = [];

  loadingStages = false;
  loadingActivities = false;
  error: string | null = null;
  success: string | null = null;

  /** Formulario de nueva etapa. */
  newStageName = '';
  newStageDescription = '';
  newStageOpen = false;

  toggleNewStage(): void {
    this.newStageOpen = !this.newStageOpen;
    if (!this.newStageOpen) {
      this.newStageName = '';
      this.newStageDescription = '';
    }
    this.changeDetector.markForCheck();
  }

  /** Formulario de nueva actividad dentro de la etapa desplegada. */
  newActivityName = '';
  newActivityDescription = '';

  /** Edición en curso (etapa o actividad). */
  editingStageId: number | null = null;
  editingStageName = '';
  editingStageDescription = '';
  editingActivityId: number | null = null;
  editingActivityName = '';
  editingActivityDescription = '';

  busyId: string | null = null;

  ngOnInit(): void {
    this.loadStages();
  }

  get activitiesByStage(): ActivityDetailDto[] {
    return this.activities;
  }

  private loadStages(): void {
    this.loadingStages = true;
    this.error = null;
    this.wbs.listStages(this.projectId).subscribe({
      next: (stages) => {
        this.stages = stages;
        this.loadingStages = false;
        if (this.expandedStageId !== null && !stages.some((stage) => stage.stageId === this.expandedStageId)) {
          this.expandedStageId = null;
          this.activities = [];
        }
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.loadingStages = false;
        this.error = 'No se pudieron cargar las etapas del proyecto.';
        this.changeDetector.markForCheck();
      },
    });
  }

  toggleStage(stage: StageDto): void {
    // Mientras haya una mutación en vuelo no se cambia de etapa: su respuesta llegaría
    // a escribir en la lista recién cargada de otra etapa.
    if (this.busyId !== null) {
      return;
    }
    if (this.expandedStageId === stage.stageId) {
      this.expandedStageId = null;
      this.activities = [];
      return;
    }
    this.expandedStageId = stage.stageId;
    this.editingStageId = null;
    this.editingActivityId = null;
    this.newActivityName = '';
    this.newActivityDescription = '';
    this.loadActivities(stage.stageId);
  }

  private loadActivities(stageId: number): void {
    this.loadingActivities = true;
    this.wbs.listActivities(stageId).subscribe({
      next: (activities) => {
        this.loadingActivities = false;
        // Respuesta tardía: el usuario ya desplegó (o cerró) otra etapa.
        if (this.expandedStageId !== stageId) {
          return;
        }
        this.activities = activities;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.loadingActivities = false;
        // Sin la lista no se puede mostrar nada: se vacía para no dejar actividades de
        // otra etapa bajo el título de la que falló.
        if (this.expandedStageId === stageId) {
          this.activities = [];
          this.error = 'No se pudieron cargar las actividades de la etapa.';
        }
        this.changeDetector.markForCheck();
      },
    });
  }

  // =========================================================
  // ETAPAS
  // =========================================================

  createStage(): void {
    const name = this.newStageName.trim();
    if (!name || this.busyId !== null) {
      return;
    }
    this.busyId = 'new-stage';
    this.error = null;
    const description = this.newStageDescription.trim() || null;
    this.wbs.createStage(this.projectId, name, description).subscribe({
      next: () => {
        this.busyId = null;
        this.success = `Etapa "${name}" creada.`;
        this.newStageName = '';
        this.newStageDescription = '';
        this.loadStages();
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.busyId = null;
        this.error = err?.error?.message ?? 'No se pudo crear la etapa.';
        this.changeDetector.markForCheck();
      },
    });
  }

  startStageEdit(stage: StageDto): void {
    this.editingStageId = stage.stageId;
    this.editingStageName = stage.name;
    this.editingStageDescription = stage.description ?? '';
  }

  cancelStageEdit(): void {
    this.editingStageId = null;
  }

  saveStage(stage: StageDto): void {
    const name = this.editingStageName.trim();
    if (!name || this.busyId !== null) {
      return;
    }
    this.busyId = `stage-${stage.stageId}`;
    this.error = null;
    const description = this.editingStageDescription.trim() || null;
    this.wbs.updateStage(stage.stageId, name, description, stage.version).subscribe({
      next: (updated) => {
        this.busyId = null;
        this.editingStageId = null;
        this.success = 'Etapa actualizada.';
        this.stages = this.stages.map((current) => (current.stageId === updated.stageId ? updated : current));
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.busyId = null;
        this.error = stageErrorMessage(err);
        this.changeDetector.markForCheck();
      },
    });
  }

  changeStageStatus(stage: StageDto, status: WorkStatusName): void {
    if (this.busyId !== null) {
      return;
    }
    this.busyId = `stage-${stage.stageId}`;
    this.error = null;
    this.wbs.changeStageStatus(stage.stageId, status, stage.version).subscribe({
      next: (updated) => {
        this.busyId = null;
        this.success = `Etapa "${updated.name}": ${statusLabel(status)}.`;
        this.stages = this.stages.map((current) => (current.stageId === updated.stageId ? updated : current));
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.busyId = null;
        this.error = stageErrorMessage(err);
        this.changeDetector.markForCheck();
      },
    });
  }

  // =========================================================
  // ACTIVIDADES
  // =========================================================

  createActivity(): void {
    const stageId = this.expandedStageId;
    const name = this.newActivityName.trim();
    if (stageId === null || !name || this.busyId !== null) {
      return;
    }
    this.busyId = `new-activity-${stageId}`;
    this.error = null;
    const description = this.newActivityDescription.trim() || null;
    this.wbs.createActivity(stageId, name, description).subscribe({
      next: () => {
        this.busyId = null;
        this.success = `Actividad "${name}" creada.`;
        this.newActivityName = '';
        this.newActivityDescription = '';
        this.loadActivities(stageId);
        // La actividad nueva está disponible como "categoría" de las tareas.
        this.structureChanged.emit();
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.busyId = null;
        this.error = err?.error?.message ?? 'No se pudo crear la actividad.';
        this.changeDetector.markForCheck();
      },
    });
  }

  startActivityEdit(activity: ActivityDetailDto): void {
    this.editingActivityId = activity.activityId;
    this.editingActivityName = activity.name;
    this.editingActivityDescription = activity.description ?? '';
  }

  cancelActivityEdit(): void {
    this.editingActivityId = null;
  }

  saveActivity(activity: ActivityDetailDto): void {
    const name = this.editingActivityName.trim();
    if (!name || this.busyId !== null) {
      return;
    }
    this.busyId = `activity-${activity.activityId}`;
    this.error = null;
    const description = this.editingActivityDescription.trim() || null;
    this.wbs.updateActivity(activity.activityId, name, description, activity.version).subscribe({
      next: (updated) => {
        this.busyId = null;
        this.editingActivityId = null;
        this.success = 'Actividad actualizada.';
        // Respuesta tardía: la actividad pertenece a una etapa que ya no está desplegada.
        if (this.expandedStageId === activity.stageId) {
          this.activities = this.activities.map((current) =>
            current.activityId === updated.activityId ? updated : current,
          );
          this.structureChanged.emit();
        }
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.busyId = null;
        this.error = stageErrorMessage(err);
        this.changeDetector.markForCheck();
      },
    });
  }

  changeActivityStatus(activity: ActivityDetailDto, status: WorkStatusName): void {
    if (this.busyId !== null) {
      return;
    }
    this.busyId = `activity-${activity.activityId}`;
    this.error = null;
    this.wbs.changeActivityStatus(activity.activityId, status, activity.version).subscribe({
      next: (updated) => {
        this.busyId = null;
        this.success = `Actividad "${updated.name}": ${statusLabel(status)}.`;
        // Respuesta tardía: la actividad pertenece a una etapa que ya no está desplegada.
        if (this.expandedStageId === activity.stageId) {
          this.activities = this.activities.map((current) =>
            current.activityId === updated.activityId ? updated : current,
          );
        }
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.busyId = null;
        this.error = stageErrorMessage(err);
        this.changeDetector.markForCheck();
      },
    });
  }

  // =========================================================
  // ESTADOS Y MENSAJES
  // =========================================================

  /** Transiciones permitidas desde el estado actual (mismas reglas que tareas y subtareas). */
  nextStatuses(current: WorkStatusName): WorkStatusName[] {
    switch (current) {
      case 'PENDIENTE':
        return ['EN_PROGRESO', 'CANCELADA'];
      case 'EN_PROGRESO':
        return ['PENDIENTE', 'COMPLETADA', 'CANCELADA'];
      case 'COMPLETADA':
        return ['EN_PROGRESO', 'CANCELADA'];
      case 'CANCELADA':
        return [];
    }
  }

  statusLabel(status: WorkStatusName): string {
    return statusLabel(status);
  }

  /** Texto del botón de transición, p. ej. EN_PROGRESO → COMPLETADA = "Completar". */
  statusActionLabel(current: WorkStatusName, target: WorkStatusName): string {
    if (target === 'CANCELADA') {
      return 'Cancelar';
    }
    if (target === 'EN_PROGRESO') {
      return current === 'COMPLETADA' ? 'Reabrir' : 'Iniciar';
    }
    if (target === 'PENDIENTE') {
      return 'Pausar';
    }
    if (target === 'COMPLETADA') {
      return 'Completar';
    }
    return statusLabel(target);
  }

  statusClass(status: WorkStatusName): string {
    return `wbs-status-${status.toLowerCase().replace('_', '-')}`;
  }

  dismissMessages(): void {
    this.success = null;
    this.error = null;
  }
}

const STATUS_LABELS: Record<WorkStatusName, string> = {
  PENDIENTE: 'Pendiente',
  EN_PROGRESO: 'En progreso',
  COMPLETADA: 'Completada',
  CANCELADA: 'Cancelada',
};

function statusLabel(status: WorkStatusName): string {
  return STATUS_LABELS[status];
}

/** 412 = choque de versión: otroUsuario cambió el registro mientras se editaba. */
function stageErrorMessage(err: { status?: number; error?: { message?: string } }): string {
  if (err?.status === 412) {
    return 'El registro cambió mientras lo editabas. Cierra el formulario, actualiza y vuelve a intentarlo.';
  }
  if (err?.status === 404) {
    return 'El registro ya no está disponible. Actualiza la lista.';
  }
  return err?.error?.message ?? 'No se pudo guardar el cambio.';
}
