import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AssignmentDto } from '../../../core/models/api';
import { UserSummary } from '../../../core/models/project';
import { WbsService } from '../../../core/services/wbs.service';

/**
 * US2.3 — asignación de Personal a una tarea. El Encargado del proyecto (o el Jefe) elige un
 * trabajador activo; el backend rechaza usuario inactivo, duplicado y auto-asignación.
 */
@Component({
  selector: 'app-assignment-dialog',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './assignment-dialog.html',
  styleUrl: './assignment-dialog.css',
})
export class AssignmentDialog implements OnInit {
  private readonly wbs = inject(WbsService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  @Input({ required: true }) taskId!: number;
  @Input({ required: true }) taskTitle!: string;
  @Input({ required: true }) projectId!: number;
  /** Avisa de que las asignaciones cambiaron (la lista debe recargar el contador). */
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
