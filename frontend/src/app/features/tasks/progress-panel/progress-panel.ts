import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectProgressDto, ProgressNodeDto } from '../../../core/models/api';
import { WbsService } from '../../../core/services/wbs.service';

/**
 * US3.3 — progreso en cascada del proyecto: Proyecto → Etapa → Actividad → Tarea → Subtarea.
 * El backend calcula cada porcentaje sobre trabajo hoja, así que una tarea con subtareas no se
 * cuenta dos veces; un proyecto vacío queda en 0 % y el 100 % nunca cierra el proyecto solo.
 */
@Component({
  selector: 'app-progress-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './progress-panel.html',
  styleUrl: './progress-panel.css',
})
export class ProgressPanel implements OnInit, OnChanges {
  private readonly wbs = inject(WbsService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  @Input({ required: true }) projectId!: number;
  /** Publica el árbol recién calculado (la pantalla muestra el porcentaje del proyecto). */
  @Output() loaded = new EventEmitter<ProjectProgressDto>();

  progress: ProjectProgressDto | null = null;
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    this.load();
  }

  /**
   * El panel se reutiliza cuando el usuario elige otro proyecto (el @if solo comprueba que
   * haya alguno elegido): sin esto el árbol y el `loaded` seguirían mostrando el anterior.
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['projectId'] && !changes['projectId'].firstChange) {
      this.load();
    }
  }

  private load(): void {
    this.loading = true;
    this.error = null;
    this.wbs.progress(this.projectId).subscribe({
      next: (progress) => {
        this.progress = progress;
        this.loading = false;
        this.loaded.emit(progress);
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudo cargar el progreso del proyecto.';
        this.changeDetector.markForCheck();
      },
    });
  }

  /** Recarga el árbol (lo llama la pantalla tras completar tareas o cambiar la estructura). */
  reload(): void {
    this.load();
  }

  percent(ratio: number): number {
    return Math.round((ratio ?? 0) * 100);
  }

  /** Nombre del nivel para lectores de pantalla: Proyecto, Etapa, Actividad, Tarea, Subtarea. */
  levelOf(node: ProgressNodeDto): string {
    if (node.leaf) {
      return 'trabajo';
    }
    return 'grupo';
  }
}
