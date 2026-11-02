import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AssignmentDto } from '../../../core/models/api';
import { UserSummary } from '../../../core/models/project';
import { WbsService } from '../../../core/services/wbs.service';

@Component({
  selector: 'app-assignment-dialog',
  standalone: true,
  imports: [FormsModule],
  template: `<div class="assignment-overlay" (click)="$event.target === $event.currentTarget && close()">

  <dialog
    class="assignment-modal"
    open
    aria-modal="true"
    aria-labelledby="assignment-modal-title"
  >

    <header class="assignment-modal-header">

      <div>
        <span class="assignment-eyebrow">ASIGNAR PERSONAL</span>
        <h2 id="assignment-modal-title">{{ taskTitle }}</h2>
        <p>Elige el Personal que ejecutará esta tarea (US2.3).</p>
      </div>

      <button
        type="button"
        class="btn-assign-close"
        aria-label="Cerrar asignaciones"
        (click)="close()"
      >
        ×
      </button>

    </header>

    @if (success) {
      <div class="alert alert-success" role="status">{{ success }}</div>
    }

    @if (error) {
      <div class="alert alert-error" role="alert">{{ error }}</div>
    }

    <div class="assignment-modal-content">

      <!-- ASIGNACIONES ACTUALES -->

      <section class="assignment-section">

        <h3>Personal asignado</h3>

        @if (assignments.length === 0) {

          <p class="assignment-empty">Todavía no hay Personal asignado a esta tarea.</p>

        } @else {

          <ul class="assignment-list">

            @for (assignment of assignments; track assignment.workerUserId) {

              <li class="assignment-item">

                <div class="assignment-item-info">
                  <strong>{{ assignment.workerName }}</strong>
                  <span>Asignado por {{ assignment.assignedByName }}</span>
                </div>

                <button
                  type="button"
                  class="btn-unassign"
                  (click)="unassign(assignment)"
                  [disabled]="saving"
                >
                  Retirar
                </button>

              </li>

            }

          </ul>

        }

      </section>

      <!-- NUEVA ASIGNACIÓN -->

      <section class="assignment-section">

        <h3>Asignar nuevo Personal</h3>

        @if (loading) {

          <p class="assignment-empty">Cargando Personal...</p>

        } @else if (availableWorkers.length === 0) {

          <p class="assignment-empty">No queda Personal activo disponible para asignar.</p>

        } @else {

          <div class="assignment-form">

            <!-- NUEVO BUSCADOR -->
            <div class="form-group">
              <label for="assignment-search">Buscar personal</label>
              <input
                id="assignment-search"
                type="text"
                placeholder="Buscar por nombre o rol..."
                [(ngModel)]="searchTermWorker"
                [disabled]="saving"
                style="margin-bottom: 8px;"
              />
            </div>

            <div class="form-group">
              <label for="assignment-worker">Trabajador</label>
              <select
                id="assignment-worker"
                [(ngModel)]="selectedWorkerId"
                [disabled]="saving"
              >
                <option [ngValue]="null">— Selecciona —</option>
                <!-- CAMBIO AQUÍ: Usamos filteredAvailableWorkers -->
                @for (worker of filteredAvailableWorkers; track worker.userId) {
                  <option [value]="worker.userId">
                    {{ worker.fullName }}{{ worker.chief ? ' (Jefe)' : '' }}
                  </option>
                }
              </select>

              <!-- Mensaje por si la búsqueda no encuentra a nadie -->
              @if (searchTermWorker && filteredAvailableWorkers.length === 0) {
                <span style="color: #ff6b6b; font-size: 0.85rem; margin-top: 4px; display: block;">
                  No se encontraron coincidencias.
                </span>
              }
            </div>

            <div class="form-group">
              <label for="assignment-reason">Motivo (opcional)</label>
              <input
                id="assignment-reason"
                type="text"
                maxlength="500"
                placeholder="Ej. Experiencia en cimentación"
                [(ngModel)]="reason"
                [disabled]="saving"
              />
            </div>

            <div class="assignment-form-actions">
              <button
                type="button"
                class="btn-assign"
                (click)="assign()"
                [disabled]="saving || selectedWorkerId === null"
              >
                {{ saving ? 'Asignando...' : 'Asignar' }}
              </button>
            </div>

          </div>

        }

      </section>

    </div>

  </dialog>

</div>
`,
  styles: [`/* US2.3 — diálogo de asignación de Personal.
   Overlay y botones reutilizan src/design-system.css. */

.assignment-overlay {
  position: fixed;
  inset: 0;
  z-index: 45;
  display: grid;
  place-items: center;
  padding: 12px;
  background: rgb(6 6 16 / 62%);
}

.assignment-modal {
  display: flex;
  flex-direction: column;
  width: min(520px, 100%);
  max-height: min(86vh, 720px);
  padding: 0;
  border: 1px solid var(--martinica);
  border-radius: 12px;
  background: var(--arcilla-ebano);
  color: inherit;
  overflow: auto;
}

.assignment-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px;
  border-bottom: 1px solid var(--martinica);
}

.assignment-eyebrow {
  display: block;
  color: var(--scampi);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .14em;
}

.assignment-modal-header h2 {
  margin: 6px 0 0;
  font-size: 18px;
  font-weight: 650;
}

.assignment-modal-header p {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.assignment-modal-content {
  padding: 16px 18px 22px;
}

.assignment-section + .assignment-section {
  margin-top: 20px;
}

.assignment-section h3 {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 650;
  letter-spacing: .04em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.assignment-empty {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.assignment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.assignment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--martinica);
  border-radius: 8px;
  background: var(--espejismo);
}

.assignment-item-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.assignment-item-info strong {
  font-size: 13px;
}

.assignment-item-info span {
  color: var(--text-muted);
  font-size: 11px;
}

.assignment-form {
  display: grid;
  gap: 12px;
}

.assignment-form-actions {
  display: flex;
  justify-content: flex-end;
}
`],
})
export class AssignmentDialog implements OnInit {
  private readonly wbs = inject(WbsService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  @Input({ required: true }) taskId!: number;
  @Input({ required: true }) taskTitle!: string;
  @Input({ required: true }) projectId!: number;
  @Output() changed = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  workers: UserSummary[] = [];
  assignments: AssignmentDto[] = [];

  selectedWorkerId: number | null = null;
  reason = '';

  loading = false;
  saving = false;
  error: string | null = null;
  success: string | null = null;

  searchTermWorker = '';

  /** Trabajadores que todavía no están asignados a esta tarea. */
  get availableWorkerss(): UserSummary[] {
    const assigned = new Set(this.assignments.map((assignment) => assignment.workerUserId));
    return this.workers.filter((worker) => !assigned.has(worker.userId));
  }

  // ==========================================
  // NUEVO: FILTRO PARA EL SELECTOR
  // ==========================================
  get filteredAvailableWorkers(): UserSummary[] {
    const available = this.availableWorkers;
    if (!this.searchTermWorker.trim()) {
      return available;
    }
    const term = this.searchTermWorker.toLowerCase();
    return available.filter(worker =>
      worker.fullName.toLowerCase().includes(term) ||
      (worker.chief && 'jefe'.includes(term)) // Permite buscar escribiendo "jefe"
    );
  }

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = null;
    this.wbs.listWorkers(this.projectId).subscribe({
      next: (workers) => {
        this.workers = workers;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudo cargar el Personal del proyecto.';
        this.changeDetector.markForCheck();
      },
    });
    this.wbs.listAssignments(this.taskId).subscribe({
      next: (assignments) => {
        this.assignments = assignments;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.error = 'No se pudieron cargar las asignaciones de la tarea.';
        this.changeDetector.markForCheck();
      },
    });
  }

  /** Trabajadores que todavía no están asignados a esta tarea. */
  get availableWorkers(): UserSummary[] {
    const assigned = new Set(this.assignments.map((assignment) => assignment.workerUserId));
    return this.workers.filter((worker) => !assigned.has(worker.userId));
  }

  assign(): void {
    // El <select> entrega el id como texto: se normaliza igual que en el diálogo de delegación.
    const workerUserId = Number(this.selectedWorkerId);
    if (!Number.isInteger(workerUserId) || workerUserId < 1 || this.saving) {
      return;
    }
    this.saving = true;
    this.error = null;
    this.success = null;
    this.wbs.assign(this.taskId, workerUserId, this.reason.trim() || null).subscribe({
      next: () => {
        this.saving = false;
        this.success = 'Personal asignado.';
        this.selectedWorkerId = null;
        this.reason = '';
        this.loadAssignments();
        this.changed.emit();
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.saving = false;
        this.error = assignmentError(err, 'No se pudo asignar el Personal.');
        this.changeDetector.markForCheck();
      },
    });
  }

  unassign(assignment: AssignmentDto): void {
    if (this.saving) {
      return;
    }
    this.saving = true;
    this.error = null;
    this.success = null;
    this.wbs.unassign(this.taskId, assignment.workerUserId).subscribe({
      next: () => {
        this.saving = false;
        this.success = `Asignación de ${assignment.workerName} retirada.`;
        this.loadAssignments();
        this.changed.emit();
        this.changeDetector.markForCheck();
      },
      error: (err) => {
        this.saving = false;
        this.error = assignmentError(err, 'No se pudo retirar la asignación.');
        this.changeDetector.markForCheck();
      },
    });
  }

  close(): void {
    if (!this.saving) {
      this.closed.emit();
    }
  }

  private loadAssignments(): void {
    this.wbs.listAssignments(this.taskId).subscribe({
      next: (assignments) => {
        this.assignments = assignments;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.error = 'No se pudieron cargar las asignaciones de la tarea.';
        this.changeDetector.markForCheck();
      },
    });
  }
}

/** Errores conocidos de US2.3: duplicado (400), sin permiso o autoasignación (403), no encontrado (404). */
function assignmentError(err: { status?: number; error?: { message?: string } }, fallback: string): string {
  // El backend explica cada caso con su propio mensaje; solo se usa el genérico si no llega ninguno.
  if (err?.error?.message) {
    return err.error.message;
  }
  if (err?.status === 403) {
    return 'No tienes permiso para asignar Personal en este proyecto.';
  }
  if (err?.status === 404) {
    return 'La tarea o el trabajador ya no están disponibles. Actualiza la lista.';
  }
  return fallback;
}
