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
  template: `<section class="progress-panel" aria-label="Progreso en cascada del proyecto">

  <header class="progress-panel-header">
    <h2>Progreso en cascada (US3.3)</h2>
    @if (progress) {
      <p>{{ progress.projectName }} — {{ percent(progress.root.ratio) }} %</p>
    }
  </header>

  @if (loading) {

    <p class="progress-empty">Calculando progreso...</p>

  } @else if (error) {

    <p class="progress-empty">{{ error }}</p>

  } @else if (progress) {

    <ng-container *ngTemplateOutlet="nodeTemplate; context: { node: progress.root, depth: 0 }"></ng-container>

  }

</section>

<ng-template #nodeTemplate let-node="node" let-depth="depth">

  <div class="progress-node" [class.leaf]="node.leaf" [style.margin-left.px]="depth * 16">

    <div class="progress-node-head">
      <span class="progress-node-name" [title]="levelOf(node)">{{ node.name }}</span>
      <span class="progress-node-ratio">{{ percent(node.ratio) }} %</span>
    </div>

    <div
      class="progress-bar"
      role="progressbar"
      [attr.aria-valuenow]="percent(node.ratio)"
      aria-valuemin="0"
      aria-valuemax="100"
    >
      <span [style.width.%]="percent(node.ratio)"></span>
    </div>

    @if (!node.leaf) {
      <span class="progress-node-count">{{ node.completed }} de {{ node.total }} unidades hoja</span>
    }

  </div>

  @if (node.children.length > 0) {
    <div class="progress-children">
      @for (child of node.children; track child.id) {
        <ng-container *ngTemplateOutlet="nodeTemplate; context: { node: child, depth: depth + 1 }"></ng-container>
      }
    </div>
  }

</ng-template>
`,
  styles: [`/* US3.3 — árbol de progreso en cascada.
   La barra reutiliza la forma de .progress-bar de task-list.css pero conScope propio. */

.progress-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0 0 18px;
  padding: 16px 18px;
  border: 1px solid var(--martinica);
  border-radius: 12px;
  background: var(--arcilla-ebano);
}

.progress-panel-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
}

.progress-panel-header p {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.progress-empty {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.progress-children {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.progress-node {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--martinica);
  border-radius: 8px;
  background: var(--espejismo);
}

.progress-node.leaf {
  padding: 6px 10px;
  background: transparent;
  border-style: dashed;
}

.progress-node-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.progress-node-name {
  min-width: 0;
  color: #f0eee9;
  font-size: 13px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.progress-node-ratio {
  flex-shrink: 0;
  color: var(--priority-alta);
  font-size: 12px;
  font-weight: 700;
}

.progress-node-count {
  color: var(--text-muted);
  font-size: 10px;
}

.progress-bar {
  position: relative;
  height: 8px;
  border-radius: 999px;
  background: var(--martinica);
  overflow: hidden;
}

.progress-bar > span {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: 999px;
  background: var(--priority-alta);
}
`],
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
