import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DragDropModule, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import {
  ActivityDetailDto,
  StageDto,
  WorkStatusName,
} from '../../../core/models/api';
import { WbsService } from '../../../core/services/wbs.service';
import { HierarchyStateService } from '../../../core/services/hierarchy-state.service';

@Component({
  selector: 'app-wbs-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  template: `<section class="wbs-panel" aria-label="Estructura del proyecto: etapas y actividades">

  <header class="wbs-header">

    <div>
      <h2>Estructura del proyecto (WBS)</h2>
      <p>
        Etapas y actividades donde se organizan las tareas (US2.2).
      </p>
    </div>

    <div style="flex-grow: 1; margin: 0 20px;">
      <input
        type="text"
        class="search-input"
        [(ngModel)]="searchTermWbs"
        placeholder="Buscar etapa o actividad..."
        style="width: 100%; max-width: 300px; padding: 8px 12px; border-radius: 6px; background-color: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: inherit;"
      />
    </div>

    @if (canManage) {
      <button
        type="button"
        class="btn-wbs-toggle"
        (click)="toggleNewStage()"
      >
        + Nueva etapa
      </button>
    }

  </header>

  @if (success) {
    <div class="alert alert-success" role="status">
      {{ success }}
      <button type="button" class="wbs-dismiss" aria-label="Descartar" (click)="dismissMessages()">×</button>
    </div>
  }

  @if (error) {
    <div class="alert alert-error" role="alert">
      {{ error }}
      <button type="button" class="wbs-dismiss" aria-label="Descartar" (click)="dismissMessages()">×</button>
    </div>
  }

  <!-- FORMULARIO NUEVA ETAPA -->
  @if (canManage && newStageOpen) {

    <div class="wbs-create">

      <div class="form-group">
        <label for="wbs-new-stage-name">Nombre de la etapa</label>
        <input
          id="wbs-new-stage-name"
          type="text"
          maxlength="160"
          placeholder="Ej. Construcción"
          [(ngModel)]="newStageName"
          [disabled]="busyId !== null"
        />
      </div>

      <div class="form-group">
        <label for="wbs-new-stage-description">Descripción (opcional)</label>
        <input
          id="wbs-new-stage-description"
          type="text"
          maxlength="2000"
          placeholder="Ej. Obra gruesa y acabados"
          [(ngModel)]="newStageDescription"
          [disabled]="busyId !== null"
        />
      </div>

      <div class="wbs-create-actions">
        <button
          type="button"
          class="btn-wbs-save"
          (click)="createStage()"
          [disabled]="busyId !== null || newStageName.trim() === ''"
        >
          {{ busyId === 'new-stage' ? 'Creando...' : 'Crear etapa' }}
        </button>
        <button type="button" class="btn-wbs-cancel" (click)="toggleNewStage()" [disabled]="busyId !== null">
          Cancelar
        </button>
      </div>

    </div>

  }

  <!-- LISTA DE ETAPAS -->
  @if (loadingStages) {

    <div class="empty-message">Cargando etapas...</div>

  } @else if (stages.length === 0) {

    <div class="empty-message">
      <p>Este proyecto todavía no tiene etapas.</p>
      <p>Crea la primera etapa para organizar las actividades y las tareas.</p>
    </div>

  } @else {

    <ul class="wbs-stages" cdkDropList (cdkDropListDropped)="onDropStages($event)" [cdkDropListData]="filteredStages">

      @for (stage of filteredStages; track stage.stageId) {

        <li class="wbs-stage" cdkDrag [cdkDragData]="stage">

          <div class="wbs-stage-head">

            <span class="wbs-drag-handle" cdkDragHandle>⠿</span>

            <button
              type="button"
              class="wbs-stage-toggle"
              [attr.aria-expanded]="expandedStageId === stage.stageId"
              (click)="toggleStage(stage)"
            >
              <span class="wbs-chevron" [class.open]="expandedStageId === stage.stageId">▸</span>
              <strong>{{ stage.name }}</strong>
              <span class="wbs-status" [ngClass]="statusClass(stage.status)">
                {{ statusLabel(stage.status) }}
              </span>
              <span class="wbs-count">{{ (stage.activityCount ?? 0) }} actividad{{ (stage.activityCount ?? 0) !== 1 ? 'es' : '' }}</span>
            </button>

            @if (canManage) {
              <div class="wbs-stage-actions">

                @for (status of nextStatuses(stage.status); track status) {
                  <button
                    type="button"
                    class="btn-wbs-status"
                    (click)="changeStageStatus(stage, status)"
                    [disabled]="busyId !== null"
                  >
                    {{ statusActionLabel(stage.status, status) }}
                  </button>
                }

                <button type="button" class="btn-wbs-edit" (click)="startStageEdit(stage)" [disabled]="busyId !== null">
                  ✎
                </button>

              </div>
            }

          </div>

          <!-- EDICIÓN DE ETAPA -->
          @if (editingStageId === stage.stageId) {

            <div class="wbs-edit">

              <div class="form-group">
                <label for="wbs-stage-name-{{ stage.stageId }}">Nombre</label>
                <input
                  id="wbs-stage-name-{{ stage.stageId }}"
                  type="text"
                  maxlength="160"
                  [(ngModel)]="editingStageName"
                  [disabled]="busyId !== null"
                />
              </div>

              <div class="form-group">
                <label for="wbs-stage-description-{{ stage.stageId }}">Descripción</label>
                <input
                  id="wbs-stage-description-{{ stage.stageId }}"
                  type="text"
                  maxlength="2000"
                  [(ngModel)]="editingStageDescription"
                  [disabled]="busyId !== null"
                />
              </div>

              <div class="wbs-edit-actions">
                <button
                  type="button"
                  class="btn-wbs-save"
                  (click)="saveStage(stage)"
                  [disabled]="busyId !== null || editingStageName.trim() === ''"
                >
                  {{ busyId === 'stage-' + stage.stageId ? 'Guardando...' : 'Guardar' }}
                </button>
                <button type="button" class="btn-wbs-cancel" (click)="cancelStageEdit()" [disabled]="busyId !== null">
                  Cancelar
                </button>
              </div>

            </div>

          }

          <!-- ACTIVIDADES DE LA ETAPA DESPLEGADA -->
          @if (expandedStageId === stage.stageId) {

            <div class="wbs-activities">

              @if (loadingActivities) {

                <div class="empty-message">Cargando actividades...</div>

              } @else {

                @if (activities.length === 0) {

                  <div class="empty-message">
                    <p>La etapa todavía no tiene actividades.</p>
                  </div>

                } @else {

                  <ul class="wbs-activity-list" cdkDropList (cdkDropListDropped)="onDropActivities($event)" [cdkDropListData]="filteredActivities">

                    @for (activity of filteredActivities; track activity.activityId) {

                      <li class="wbs-activity" cdkDrag [cdkDragData]="activity">

                        @if (editingActivityId === activity.activityId) {

                          <div class="wbs-edit">
                            <div class="form-group">
                              <label for="wbs-activity-name-{{ activity.activityId }}">Nombre</label>
                              <input
                                id="wbs-activity-name-{{ activity.activityId }}"
                                type="text"
                                maxlength="160"
                                [(ngModel)]="editingActivityName"
                                [disabled]="busyId !== null"
                              />
                            </div>
                            <div class="form-group">
                              <label for="wbs-activity-description-{{ activity.activityId }}">Descripción</label>
                              <input
                                id="wbs-activity-description-{{ activity.activityId }}"
                                type="text"
                                maxlength="2000"
                                [(ngModel)]="editingActivityDescription"
                                [disabled]="busyId !== null"
                              />
                            </div>
                            <div class="wbs-edit-actions">
                              <button
                                type="button"
                                class="btn-wbs-save"
                                (click)="saveActivity(activity)"
                                [disabled]="busyId !== null || editingActivityName.trim() === ''"
                              >
                                Guardar
                              </button>
                              <button
                                type="button"
                                class="btn-wbs-cancel"
                                (click)="cancelActivityEdit()"
                                [disabled]="busyId !== null"
                              >
                                Cancelar
                              </button>
                            </div>
                          </div>

                        } @else {

                          <div class="wbs-activity-head">
                            <span class="wbs-drag-handle" cdkDragHandle>⠿</span>
                            <span class="wbs-activity-name">{{ activity.name }}</span>
                            <span class="wbs-status" [ngClass]="statusClass(activity.status)">
                              {{ statusLabel(activity.status) }}
                            </span>
                            <span class="wbs-count">{{ (activity.taskCount ?? 0) }} tarea{{ (activity.taskCount ?? 0) !== 1 ? 's' : '' }}</span>

                            @if (canManage) {
                              <div class="wbs-stage-actions">

                                @for (status of nextStatuses(activity.status); track status) {
                                  <button
                                    type="button"
                                    class="btn-wbs-status"
                                    (click)="changeActivityStatus(activity, status)"
                                    [disabled]="busyId !== null"
                                  >
                                    {{ statusActionLabel(activity.status, status) }}
                                  </button>
                                }

                                <button
                                  type="button"
                                  class="btn-wbs-edit"
                                  (click)="startActivityEdit(activity)"
                                  [disabled]="busyId !== null"
                                >
                                  ✎
                                </button>

                              </div>
                            }

                          </div>

                          @if (activity.description) {
                            <p class="wbs-activity-description">{{ activity.description }}</p>
                          }

                        }

                      </li>

                    }

                  </ul>

                }

                <!-- FORMULARIO NUEVA ACTIVIDAD -->
                @if (canManage) {

                  <div class="wbs-create wbs-create-activity">

                    <div class="form-group">
                      <label for="wbs-new-activity-name-{{ stage.stageId }}">Nombre de la actividad</label>
                      <input
                        id="wbs-new-activity-name-{{ stage.stageId }}"
                        type="text"
                        maxlength="160"
                        placeholder="Ej. Cimentación"
                        [(ngModel)]="newActivityName"
                        [disabled]="busyId !== null"
                      />
                    </div>

                    <div class="form-group">
                      <label for="wbs-new-activity-description-{{ stage.stageId }}">Descripción (opcional)</label>
                      <input
                        id="wbs-new-activity-description-{{ stage.stageId }}"
                        type="text"
                        maxlength="2000"
                        [(ngModel)]="newActivityDescription"
                        [disabled]="busyId !== null"
                      />
                    </div>

                    <div class="wbs-create-actions">
                      <button
                        type="button"
                        class="btn-wbs-save"
                        (click)="createActivity()"
                        [disabled]="busyId !== null || newActivityName.trim() === ''"
                      >
                        {{ busyId === 'new-activity-' + stage.stageId ? 'Creando...' : 'Crear actividad' }}
                      </button>
                    </div>

                  </div>

                }

              }

            </div>

          }

        </li>

      }

    </ul>

  }

</section>
`,
  styles: [`/* US2.2 — panel de estructura (etapas y actividades).
   Las primitivas (botones, campos, avisos) vienen de src/design-system.css. */

.wbs-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 0 0 18px;
  padding: 16px 18px;
  border: 1px solid var(--martinica);
  border-radius: 12px;
  background: var(--arcilla-ebano);
}

.wbs-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.wbs-header h2 {
  margin: 0;
  color: #f4f4f8;
  font-size: 17px;
  font-weight: 650;
}

.wbs-header p {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.wbs-dismiss {
  margin-left: 10px;
  padding: 0 4px;
  border: 0;
  background: none;
  color: inherit;
  font-size: 14px;
  cursor: pointer;
}

.wbs-create {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  align-items: end;
  padding: 14px;
  border: 1px dashed var(--martinica);
  border-radius: 10px;
  background: var(--espejismo);
}

.wbs-create-actions,
.wbs-edit-actions {
  display: flex;
  gap: 8px;
}

.wbs-edit {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  align-items: end;
  margin: 8px 0 4px;
  padding: 12px 14px;
  border: 1px solid var(--martinica);
  border-radius: 10px;
  background: var(--espejismo);
}

.wbs-stages,
.wbs-activity-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.wbs-stages {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.wbs-stage {
  border: 1px solid var(--martinica);
  border-radius: 10px;
  background: var(--espejismo);
  overflow: hidden;
}

.wbs-stage-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
}

.wbs-stage-toggle {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: 0;
  background: none;
  color: #f4f4f8;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}

.wbs-chevron {
  color: var(--scampi);
  transition: transform .15s ease;
}

.wbs-chevron.open {
  transform: rotate(90deg);
}

.wbs-count {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 600;
}

.wbs-stage-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.wbs-status {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  border: 1px solid var(--martinica);
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .06em;
}

.wbs-status-pendiente { border-color: var(--priority-media); color: var(--priority-media); }
.wbs-status-en-progreso { border-color: var(--scampi); color: var(--scampi); }
.wbs-status-completada { border-color: var(--priority-alta); color: var(--priority-alta); }
.wbs-status-cancelada { border-color: #b96767; color: #b96767; }

.wbs-activities {
  padding: 0 12px 12px;
  border-top: 1px solid var(--martinica);
}

.wbs-activity-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
}

.wbs-activity {
  padding: 10px 12px;
  border: 1px solid var(--martinica);
  border-radius: 8px;
  background: var(--arcilla-ebano);
}

.wbs-activity-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.wbs-activity-name {
  flex: 1;
  min-width: 0;
  color: #eceae4;
  font-size: 13px;
  font-weight: 600;
}

.wbs-activity-description {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 11px;
}

.wbs-create-activity {
  margin-top: 10px;
}
`],
})
export class WbsPanel implements OnInit {
  private readonly wbs = inject(WbsService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly hierarchyState = inject(HierarchyStateService);

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

  searchTermWbs = '';

  get filteredStages(): StageDto[] {
    if (!this.searchTermWbs.trim()) return this.stages;
    const term = this.searchTermWbs.toLowerCase();
    return this.stages.filter(stage => stage.name.toLowerCase().includes(term));
  }

  get filteredActivities(): ActivityDetailDto[] {
    if (!this.searchTermWbs.trim()) return this.activities;
    const term = this.searchTermWbs.toLowerCase();
    return this.activities.filter(activity => activity.name.toLowerCase().includes(term));
  }

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
      next: (created) => {
        this.busyId = null;
        this.success = `Etapa "${name}" creada.`;
        this.newStageName = '';
        this.newStageDescription = '';
        this.hierarchyState.addStage(created);
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
        this.hierarchyState.updateStage(updated);
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
        this.hierarchyState.updateStage(updated);
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
      next: (created) => {
        this.busyId = null;
        this.success = `Actividad "${name}" creada.`;
        this.newActivityName = '';
        this.newActivityDescription = '';
        this.hierarchyState.addActivity(created);
        this.loadActivities(stageId);
        // Conteo optimista: incrementar el activityCount de la etapa desplegada sin esperar
        // la respuesta del backend, para que se vea inmediatamente sin recargar.
        this.stages = this.stages.map((s) =>
          s.stageId === stageId ? { ...s, activityCount: (s.activityCount ?? 0) + 1 } : s,
        );
        // Refrescar las etapas desde el backend para corregir cualquier desajuste de conteo.
        this.loadStages();
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
        this.hierarchyState.updateActivity(updated);
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
        this.hierarchyState.updateActivity(updated);
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

  /** Reordena etapas (arrastrar y soltar) con actualización optimista y rollback ante error. */
  onDropStages(event: CdkDragDrop<StageDto[]>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    const projectId = this.projectId;
    if (projectId == null) {
      return;
    }
    const previous = [...this.filteredStages];
    moveItemInArray(this.filteredStages, event.previousIndex, event.currentIndex);
    const newOrder = this.filteredStages.map((stage) => stage.stageId);
    this.changeDetector.markForCheck();

    const previousOrder = previous.map((stage) => stage.stageId);
    this.wbs.reorderStages(projectId, newOrder).subscribe({
      next: () => {
        this.success = 'Orden de etapas actualizado.';
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.stages = previous;
        this.error = 'No se pudo guardar el orden de las etapas.';
        this.changeDetector.markForCheck();
      },
    });
  }

  /** Reordena actividades de una etapa con actualización optimista y rollback ante error. */
  onDropActivities(event: CdkDragDrop<ActivityDetailDto[]>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    const stageId = this.expandedStageId;
    if (stageId == null) {
      return;
    }
    const previous = [...this.filteredActivities];
    moveItemInArray(this.filteredActivities, event.previousIndex, event.currentIndex);
    const newOrder = this.filteredActivities.map((activity) => activity.activityId);
    this.changeDetector.markForCheck();

    const previousOrder = previous.map((activity) => activity.activityId);
    this.wbs.reorderActivities(stageId, newOrder).subscribe({
      next: () => {
        this.success = 'Orden de actividades actualizado.';
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.activities = previous;
        this.error = 'No se pudo guardar el orden de las actividades.';
        this.changeDetector.markForCheck();
      },
    });
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
