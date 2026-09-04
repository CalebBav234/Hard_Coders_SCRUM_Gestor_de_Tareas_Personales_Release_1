import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { TaskHistory as TaskHistoryModel, TaskStatusHistory } from '../../../core/models/task';
import { TaskService } from '../../../core/services/task.service';

@Component({
  selector: 'app-task-history',
  templateUrl: './task-history.html',
  styleUrl: './task-history.css',
})
export class TaskHistory implements OnInit, OnDestroy {
  private readonly taskService = inject(TaskService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private request?: Subscription;

  history: TaskHistoryModel[] = [];
  loading = false;
  error: string | null = null;
  searchDraft = '';
  activeSearch = '';

  ngOnInit(): void {
    this.loadHistory();
  }

  ngOnDestroy(): void {
    this.request?.unsubscribe();
  }

  loadHistory(): void {
    // A task may change while the previous request is still loading.
    // Cancel that stale request; never discard the newer refresh.
    this.request?.unsubscribe();
    this.loading = true;
    this.error = null;
    this.request = this.taskService.listHistory(this.activeSearch).subscribe({
      next: (history) => {
        this.history = history;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: (response: { status?: number }) => {
        this.loading = false;
        this.error =
          response.status === 404
            ? 'El backend no tiene disponible el historial. Actualiza y reinicia el backend con esta rama.'
            : 'No se pudo cargar el historial. Comprueba que el backend y PostgreSQL estén disponibles y vuelve a intentarlo.';
        this.changeDetector.markForCheck();
      },
    });
  }

  searchHistory(event: Event): void {
    event.preventDefault();
    if (this.loading) return;
    this.activeSearch = this.searchDraft.trim();
    this.loadHistory();
  }

  updateSearchDraft(event: Event): void {
    this.searchDraft = (event.target as HTMLInputElement).value;
  }

  clearSearch(): void {
    if (this.loading) return;
    this.searchDraft = '';
    this.activeSearch = '';
    this.loadHistory();
  }

  archiveLabel(task: TaskHistoryModel): string {
    return task.deletedAt ? 'Eliminada' : 'Terminada';
  }

  archiveDate(task: TaskHistoryModel): string {
    return this.formatDate(task.deletedAt ?? task.completedAt ?? task.updatedAt ?? task.createdAt);
  }

  eventLabel(event: TaskStatusHistory): string {
    return event.fromStatus
      ? `${event.fromStatus} → ${event.toStatus}`
      : `Creada como ${event.toStatus}`;
  }

  formatDate(value?: string | null): string {
    if (!value) return 'Fecha no disponible';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Fecha no disponible';
    return new Intl.DateTimeFormat('es-BO', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
  }

  formatDuration(seconds?: number): string {
    if (seconds == null || seconds <= 0) return '0 min';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    if (hours > 0) return `${hours} h ${minutes} min`;
    if (minutes > 0) return `${minutes} min ${secs} s`;
    return `${secs} s`;
  }
}
