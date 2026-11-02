import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  OnInit,
  Output,
  ViewChild,
  inject,
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DragDropModule, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';

import { TaskService } from '../../../core/services/task.service';
import { WbsService } from '../../../core/services/wbs.service';
import { ProjectService } from '../../../core/services/project.service';
import { HierarchyStateService } from '../../../core/services/hierarchy-state.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Task } from '../../../core/models/task';
import { Project } from '../../../core/models/project';
import { ProjectProgressDto, StageDto } from '../../../core/models/api';

import { TaskForm } from '../task-form/task-form';
import { TaskEditor } from '../task-editor/task-editor';
import { WbsPanel } from '../wbs/wbs-panel';
import { AssignmentDialog } from '../assignment-dialog/assignment-dialog';
import { ProgressPanel } from '../progress-panel/progress-panel';

import { ActivatedRoute, ParamMap } from '@angular/router';
import { combineLatest } from 'rxjs';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DragDropModule,
    TaskForm,
    TaskEditor,
    WbsPanel,
    AssignmentDialog,
    ProgressPanel
  ],
  template: `<div class="task-list-page">

  <!-- =========================================================
       1. JERARQUÍA (PROYECTO, ETAPA, ACTIVIDAD) - MOVIDO ARRIBA
       ========================================================= -->
  @if (!usesPersonalScope) {
    <section class="workspace-bar">
      <label for="workspace-project">Proyecto</label>
      <select
        id="workspace-project"
        [(ngModel)]="selectedProjectId"
        (ngModelChange)="selectProject($event)"
      >
        <option [ngValue]="null">— Selecciona un proyecto —</option>
        @for (project of projects; track project.projectId) {
          <option [ngValue]="project.projectId">{{ project.name }}</option>
        }
      </select>

      @if (selectedStageId !== null || stages.length > 0) {
        <label for="workspace-stage">Etapa</label>
        <select
          id="workspace-stage"
          [(ngModel)]="selectedStageId"
          (ngModelChange)="selectStage($event)"
        >
          @for (stage of stages; track stage.stageId) {
            <option [ngValue]="stage.stageId">{{ stage.name }}</option>
          }
        </select>

        @if (categories.length > 0) {
          <label for="workspace-activity">Actividad</label>
          <select
            id="workspace-activity"
            [(ngModel)]="selectedActivityId"
            (ngModelChange)="selectActivity($event)"
          >
            <option [ngValue]="null">— Selecciona —</option>
            @for (category of categories; track category.id) {
              <option [ngValue]="category.id">{{ category.name }}</option>
            }
          </select>
        }
      }

      @if (canManageHierarchy) {
        <button type="button" class="btn-wbs-toggle" [class.active]="wbsPanelOpen" (click)="toggleWbsPanel()">
          ▤ Estructura
        </button>
        }

      @if (selectedProjectId !== null) {
        <span class="ratio">{{ progressRatio }} % completado</span>
      }
    </section>

    <!-- Constructor de la WBS y Progreso -->
    @if (wbsPanelOpen && selectedProjectId !== null) {
      <app-wbs-panel
        [projectId]="selectedProjectId"
        [canManage]="canManageHierarchy"
        (structureChanged)="loadCategories(); loadTasks()"
      ></app-wbs-panel>
    }

    @if (selectedProjectId !== null) {
      <app-progress-panel
        [projectId]="selectedProjectId"
        (loaded)="onProgressLoaded($event)"
      ></app-progress-panel>
    }
  }

  <!-- =========================================================
       2. FORMULARIO DE CREACIÓN (PROYECTADO DESDE TASK-HOME)
       ========================================================= -->
  <ng-content select="[task-form]"></ng-content>


  <!-- =========================================================
       3. ENCABEZADO CONTEXTUAL (proyecto → etapa → actividad)
       ========================================================= -->
  <header class="page-header">
    <div class="page-header-left">
      <nav class="breadcrumb" aria-label="Ruta actual">
        <ol>
          <li><a routerLink="/" (click)="selectProject(null)">Proyectos</a><span class="breadcrumb-sep">›</span></li>
          @if (selectedProjectId !== null) {
            <li><a routerLink="/" (click)="selectProject(selectedProjectId)">{{ selectedProject?.name }}</a><span class="breadcrumb-sep">›</span></li>
            @if (selectedStageId !== null) {
              <li><a routerLink="/" (click)="selectStage(selectedStageId)">{{ stageName(selectedStageId) }}</a><span class="breadcrumb-sep">›</span></li>
              @if (selectedActivityId !== null) {
                <li><span class="breadcrumb-current">{{ activityName(selectedActivityId) }}</span></li>
              } @else {
                <li><span class="breadcrumb-current">Tareas</span></li>
              }
            } @else {
              <li><span class="breadcrumb-current">Etapas</span></li>
            }
          }
        </ol>
      </nav>
      <div>
        <h1 class="page-title">{{ pageTitle() }}</h1>
        <p class="page-subtitle">{{ pageSubtitle() }}</p>
      </div>
    </div>

    <div class="page-header-actions">
      @if (completedTasks.length > 0) {
        <button type="button" class="btn-completed" (click)="openCompletedAside()">
          ✓ Completadas <span class="completed-count">{{ completedTasks.length }}</span>
        </button>
      }
      <button type="button" class="btn-refresh" (click)="loadTasks()" [disabled]="loading">
        ↻ Actualizar
      </button>
      @if (selectedProjectId !== null && selectedActivityId !== null && canCreateTask) {
        <button type="button" class="gt-btn gt-btn-primary" (click)="openTaskForm()">
          ＋ Nueva tarea
        </button>
      } @else if (selectedProjectId !== null && selectedActivityId === null && categories.length > 0 && canCreateTask) {
        <button type="button" class="gt-btn gt-btn-primary" (click)="selectActivity(categories[0].id)">
          ＋ Nueva tarea
        </button>
      } @else if (selectedProjectId !== null && stages.length > 0 && selectedStageId === null && canManageHierarchy) {
        <button type="button" class="gt-btn gt-btn-primary" (click)="toggleWbsPanel()">
          ＋ Crear etapa
        </button>
      } @else if (selectedProjectId === null && projects.length > 0 && isChief) {
        <button type="button" class="gt-btn gt-btn-primary" (click)="openTaskForm()">
          ＋ Nuevo proyecto
        </button>
      } @else if (selectedProjectId === null && projects.length === 0 && isChief) {
        <button type="button" class="gt-btn gt-btn-primary" (click)="openTaskForm()">
          ＋ Crear primer proyecto
        </button>
      }
    </div>
  </header>

  <!-- =========================================================
       4. BUSCADOR Y LISTA
       ========================================================= -->
  <section class="search-container">
    <form class="search-form" (ngSubmit)="searchTasks()">
      <label for="task-search">Buscar tareas</label>
      <input id="task-search" type="text" class="search-input" [(ngModel)]="searchTerm" name="searchTerm" placeholder="Buscar tareas..."/>
      <button type="submit" class="btn-search" [disabled]="loading">Buscar</button>
    </form>
  </section>

  <div class="swipe-hint">
    <span class="swipe-hint-delete">← Desliza para eliminar</span>
    <span class="swipe-hint-complete">Desliza para completar →</span>
  </div>


@if (successMessage) {

    <div class="alert alert-success">
      {{ successMessage }}
    </div>

  }


  @if (errorMessage) {

    <div class="alert alert-error">
      {{ errorMessage }}
    </div>

  }



  @if (!usesPersonalScope && selectedProjectId === null) {

    <div class="empty-state">

      <div class="empty-state-icon">
        📁
      </div>

      <h3>
        Selecciona un proyecto
      </h3>

      <p>
        Elige el proyecto sobre el que quieres trabajar. Las tareas
        siempre pertenecen a una etapa y una actividad de ese proyecto.
      </p>

    </div>

  } @else if (loading) {

    <div class="empty-state">

      <div class="empty-state-icon">
        ⏳
      </div>

      <h3>
        Cargando tareas...
      </h3>

      <p>
        Espera un momento.
      </p>

    </div>

  }


  <!-- =========================================================
       LISTA DE TAREAS PRINCIPALES
       ========================================================= -->

  @else if (parentTasks.length > 0) {

    <section class="tasks" cdkDropList (cdkDropListDropped)="onDropTasks($event)" [cdkDropListData]="parentTasks">

      @for (task of parentTasks; track task.id) {

        <div class="swipe-container" cdkDrag [cdkDragData]="task">

          <!-- ACCIÓN ELIMINAR -->

          <div
            class="swipe-action swipe-delete"
            [class.revealed]="getSwipeOffset(task.id) < -20"
          >

            <span class="swipe-icon">
              🗑
            </span>

            <strong>
              ELIMINAR
            </strong>

          </div>


          <!-- ACCIÓN COMPLETAR -->

          <div
            class="swipe-action swipe-complete"
            [class.revealed]="getSwipeOffset(task.id) > 20"
          >

            <span class="swipe-icon">
              ✓
            </span>

            <strong>
              COMPLETAR
            </strong>

          </div>


          <!-- TARJETA PRINCIPAL -->

          <article
            class="task-card"
            [ngClass]="getTaskColorClass(task.id)"
            [id]="'task-card-' + task.id"
            [class.is-swiping]="isSwiping(task.id)"
            [style.transform]="
              'translate3d(' +
              getSwipeOffset(task.id) +
              'px, 0, 0)'
            "
            (pointerdown)="startSwipe($event, task)"
            (pointermove)="moveSwipe($event, task)"
            (pointerup)="endSwipe($event, task)"
            (pointercancel)="cancelSwipe(task.id)"
          >

            <!-- CABECERA -->

            <div class="task-header">

              <span class="wbs-drag-handle" cdkDragHandle title="Arrastre para reordenar">⠿</span>

              <h2 class="task-title">
                {{ task.title }}
              </h2>

              <span
                class="task-status"
                [class.status-activa]="task.status === 'ACTIVA'"
                [class.status-inactiva]="task.status === 'INACTIVA'"
                [class.status-terminada]="task.status === 'TERMINADA'"
              >
                {{ task.status }}
              </span>

            </div>


            <!-- DESCRIPCIÓN -->

            @if (task.description) {

              <p class="task-description">
                {{ task.description }}
              </p>

            }


            <!-- INFORMACIÓN -->

            <div class="task-meta">

              <span
                class="meta-item priority"
                [class.priority-alta]="task.priority === 'ALTA'"
                [class.priority-media]="task.priority === 'MEDIA'"
                [class.priority-baja]="task.priority === 'BAJA'"
              >
                Prioridad:
                {{ task.priority }}
              </span>

            </div>


            <!-- TIEMPO ACTIVO -->

            @if (task.totalActiveSeconds !== undefined) {

              <div class="active-time">

                <span>
                  Tiempo activo
                </span>

                <strong>
                  {{ formatActiveTime(getActiveSeconds(task)) }}
                </strong>

              </div>

            }


            <!-- SUBTAREAS -->

            @if (getSubtasks(task.id).length > 0) {

              <button
                type="button"
                class="subtasks-button"
                (click)="openSubtasksModal(task)"
                [disabled]="actionsDisabled(task)"
              >

                <span>
                  ▣ Subtareas
                </span>

                <span class="subtasks-count">
                  {{ getSubtasks(task.id).length }}
                </span>

              </button>

            }


            <!-- BOTONES DE ACCIÓN -->

            <div class="task-actions">

              <!-- US2.3 — asignar Personal a la tarea -->

              @if (canManageHierarchy) {

                <button
                  type="button"
                  (click)="openAssignment(task)"
                  [disabled]="actionsDisabled(task)"
                >
                  👥 Asignar

                  @if (assignedCount(task) > 0) {
                    <span class="assigned-count">
                      {{ assignedCount(task) }}
                    </span>
                  }
                </button>

              }

              @if (task.status === 'INACTIVA') {

                <button
                  type="button"
                  (click)="activate(task)"
                  [disabled]="actionsDisabled(task)"
                >
                  ▶ Activar
                </button>

              }

              @if (task.status === 'ACTIVA') {

                <button
                  type="button"
                  (click)="pause(task)"
                  [disabled]="actionsDisabled(task)"
                >
                  ⏸ Pausar
                </button>

                <button
                  type="button"
                  (click)="complete(task)"
                  [disabled]="actionsDisabled(task)"
                >
                  ✓ Completar
                </button>

              }

              @if (task.status === 'TERMINADA') {

                <button
                  type="button"
                  class="btn-reopen"
                  (click)="reopen(task)"
                  [disabled]="actionsDisabled(task)"
                >
                  ↻ Reabrir tarea
                </button>

              }


              <!-- CREAR SUBTAREA -->

              <button
                type="button"
                (click)="createSubtask(task)"
                [disabled]="actionsDisabled(task)"
              >
                ＋ Subtarea
              </button>


              <!-- EDITAR TAREA PRINCIPAL -->

              <button
                type="button"
                (click)="editTask(task)"
                [disabled]="actionsDisabled(task)"
              >
                ✎ Editar
              </button>


              <!-- ELIMINAR -->

              <button
                type="button"
                class="btn-delete"
                (click)="requestDelete(task)"
                [disabled]="actionsDisabled(task)"
              >
                🗑 Eliminar
              </button>

            </div>


            <!-- CONFIRMACIÓN DE ELIMINACIÓN -->

            @if (confirmingDelete === task.id) {

              <section class="delete-confirmation">

                <div class="delete-warning-icon">
                  ⚠
                </div>

                <div class="delete-warning-content">

                  <h4>
                    ¿Eliminar "{{ task.title }}"?
                  </h4>

                  <p>
                    La tarea dejará de aparecer
                    en la lista.
                    Su historial se conservará.
                  </p>

                </div>

                <div class="delete-confirmation-actions">

                  <button
                    type="button"
                    class="btn-cancel-delete"
                    (click)="cancelDelete()"
                  >
                    Cancelar
                  </button>

                  <button
                    type="button"
                    class="btn-confirm-delete"
                    (click)="deleteTask(task)"
                    [disabled]="busyTaskId === task.id"
                  >
                    Eliminar
                  </button>

                </div>

              </section>

            }

          </article>

        </div>

      }

    </section>

  }


  <!-- =========================================================
       SIN TAREAS
       ========================================================= -->

  @else {

    <div class="empty-state">

      <div class="empty-state-icon">
        📝
      </div>

      <h3>
        @if (usesPersonalScope) {
          No tienes tareas asignadas
        } @else {
          No hay tareas
        }
      </h3>

      <p>
        @if (usesPersonalScope) {
          Cuando tu Encargado te asigne una tarea aparecerá aquí.
        } @else {
          Crea una tarea para comenzar a organizar tu trabajo.
        }
      </p>

    </div>

  }


  <!-- =========================================================
       US2.3 — DIÁLOGO DE ASIGNACIÓN DE PERSONAL
       ========================================================= -->

  @if (assigningTask) {

    <app-assignment-dialog
      [taskId]="assignmentTaskId(assigningTask)"
      [taskTitle]="assigningTask.title"
      [projectId]="selectedProjectId ?? 0"
      (changed)="onAssignmentsChanged()"
      (closed)="closeAssignment()"
    ></app-assignment-dialog>

  }


  <!-- =========================================================
       ASIDE DE TAREAS COMPLETADAS
       ========================================================= -->

  @if (completedAsideOpen) {

    <div
      class="completed-overlay"
      (click)="closeCompletedAside()"
    ></div>

    <aside
      class="completed-aside"
      aria-label="Tareas completadas"
    >

      <header class="completed-aside-header">

        <div>

          <h2>
            Tareas completadas
          </h2>

          <p>
            {{ completedTasks.length }}
            tarea(s) terminada(s)
          </p>

        </div>

        <button
          type="button"
          class="completed-aside-close"
          aria-label="Cerrar tareas completadas"
          (click)="closeCompletedAside()"
        >
          ×
        </button>

      </header>


      <div class="completed-aside-content">

        @for (task of completedTasks; track task.id) {

          <article class="completed-task">

            <div class="completed-task-check">
              ✓
            </div>

            <div class="completed-task-content">

              <!-- IDENTIFICADOR DEL TIPO DE TAREA -->

              @if (task.parentTaskId) {

                <span class="completed-task-type subtask-type">
                  SUBTAREA
                </span>

              } @else {

                <span class="completed-task-type main-task-type">
                  TAREA PRINCIPAL
                </span>

              }


              <!-- TÍTULO -->

              <h3>
                {{ task.title }}
              </h3>


              <!-- TAREA PRINCIPAL DE LA SUBTAREA -->

              @if (task.parentTaskId) {

                @if (getParentTask(task); as parentTask) {

                  <p class="completed-parent-task">
                    Tarea principal:
                    <strong>
                      {{ parentTask.title }}
                    </strong>
                  </p>

                }

              }


              <!-- DESCRIPCIÓN -->

              @if (task.description) {

                <p>
                  {{ task.description }}
                </p>

              }


              <!-- INFORMACIÓN -->

              <div class="completed-task-meta">

                @if (!task.parentTaskId) {
                <span
                  class="completed-priority"
                  [class.priority-alta]="task.priority === 'ALTA'"
                  [class.priority-media]="task.priority === 'MEDIA'"
                  [class.priority-baja]="task.priority === 'BAJA'"
                >
                  {{ task.priority }}
                </span>
                }

                @if (task.categoryName) {

                  <span>
                    {{ task.categoryName }}
                  </span>

                }

              </div>


              <!-- REABRIR -->

              <button
                type="button"
                class="btn-reopen"
                (click)="reopen(task)"
                [disabled]="actionsDisabled(task)"
              >
                ↻ Reabrir
              </button>

            </div>

          </article>

        } @empty {

          <div class="completed-empty">

            <div>
              ✓
            </div>

            <h3>
              No hay tareas completadas
            </h3>

            <p>
              Las tareas que completes aparecerán aquí.
            </p>

          </div>

        }

      </div>

    </aside>

  }


  <!-- =========================================================
       POPUP DE SUBTAREAS
       ========================================================= -->

  @if (subtasksModalParent) {

    <div
      class="subtasks-modal-overlay"
      (click)="
        $event.target === $event.currentTarget &&
        closeSubtasksModal()
      "
    >

      <dialog
        class="subtasks-modal"
        open
        aria-modal="true"
        [attr.aria-label]="
          'Subtareas de ' +
          subtasksModalParent.title
        "

      >

        <!-- =====================================================
             VISTA LISTA
             ===================================================== -->

        @if (subtaskModalMode === 'list') {

          <div class="subtasks-modal-header">

            <div>

              <h2>
                Subtareas
              </h2>

              <p>
                Subtareas de "{{ subtasksModalParent.title }}"
              </p>

            </div>


            <!-- BOTÓN ÚNICO DE NUEVA SUBTAREA -->

            <div class="subtasks-modal-header-actions">

              <button
                type="button"
                class="btn-new-subtask"
                (click)="openCreateSubtask()"
                [disabled]="actionsDisabled(subtasksModalParent)"
              >
                + Nueva subtarea
              </button>

              <button
                type="button"
                class="subtasks-modal-close"
                (click)="closeSubtasksModal()"
                aria-label="Cerrar"
              >
                ×
              </button>

            </div>

          </div>


          <div class="subtasks-modal-content">

            <div class="subtasks-modal-summary">

              {{ getSubtasks(subtasksModalParent.id).length }}
              subtarea(s)

            </div>


            <section class="subtasks-modal-grid">

              @for (
                subtask of getSubtasks(subtasksModalParent.id);
                track subtask.id
              ) {
                <!-- CONTENEDOR SWIPE -->

                <div class="subtask-swipe-container">

                  <!-- SWIPE ELIMINAR -->

                  <div
                    class="subtask-swipe-actions subtask-swipe-delete"
                    [class.revealed]="getSwipeOffset(subtask.id) < -20"
                  >

                    <span>
                      🗑
                    </span>

                    <strong>
                      ELIMINAR
                    </strong>

                  </div>


                  <!-- SWIPE COMPLETAR -->

                  <div
                    class="subtask-swipe-actions subtask-swipe-complete"
                    [class.revealed]="getSwipeOffset(subtask.id) > 20"
                  >

                    <span>
                      ✓
                    </span>

                    <strong>
                      COMPLETAR
                    </strong>

                  </div>


                  <!-- SUBTAREA -->

                  <article
                    class="subtask-postit"
                    [ngClass]="getTaskColorClass(subtask.id)"
                    [class.is-swiping]="isSwiping(subtask.id)"
                    [style.transform]="
                      'translate3d(' +
                      getSwipeOffset(subtask.id) +
                      'px, 0, 0)'
                    "
                    (pointerdown)="startSwipe($event, subtask)"
                    (pointermove)="moveSwipe($event, subtask)"
                    (pointerup)="endSwipe($event, subtask)"
                    (pointercancel)="cancelSwipe(subtask.id)"
                  >

                    <!-- CABECERA SUBTAREA -->

                    <header class="subtask-postit-header">

                      <h3>
                        {{ subtask.title }}
                      </h3>

                      <span
                        class="task-status"
                        [class.status-activa]="subtask.status === 'ACTIVA'"
                        [class.status-inactiva]="subtask.status === 'INACTIVA'"
                        [class.status-terminada]="subtask.status === 'TERMINADA'"
                      >
                        {{ subtask.status }}
                      </span>

                    </header>


                    <!-- DESCRIPCIÓN -->

                    @if (subtask.description) {

                      <p class="subtask-postit-description">
                        {{ subtask.description }}
                      </p>

                    }


                    <!-- INFORMACIÓN -->

                    <!-- En V4 las subtareas no tienen prioridad ni cronómetro propios. -->
                    @if (subtask.kind !== 'SUBTASK') {
                    <div class="task-meta">

                      <span
                        class="meta-item priority"
                        [class.priority-alta]="subtask.priority === 'ALTA'"
                        [class.priority-media]="subtask.priority === 'MEDIA'"
                        [class.priority-baja]="subtask.priority === 'BAJA'"
                      >
                        Prioridad:
                        {{ subtask.priority }}
                      </span>

                    </div>
                    }


                    <!-- TIEMPO ACTIVO -->

                    @if (subtask.totalActiveSeconds !== undefined) {

                      <div class="active-time">

                        <span>
                          Tiempo activo
                        </span>

                        <strong>
                          {{ formatActiveTime(getActiveSeconds(subtask)) }}
                        </strong>

                      </div>

                    }


                    <!-- EDITOR DE CATEGORÍA -->

                    @if (editingCategoryId === subtask.id) {

                      <div class="subtask-category-editor">
                        <label for="subtask-category-{{ subtask.id }}">
                          Categoría
                        </label>

                        <select
                          id="subtask-category-{{ subtask.id }}"
                          [(ngModel)]="selectedCategoryName"
                          name="subtask-category-{{ subtask.id }}"
                        >

                          <option [ngValue]="null">
                            Sin categoría
                          </option>

                          @for (category of categories; track category.id) {

                            <option [ngValue]="category.name">
                              {{ category.name }}
                            </option>

                          }

                        </select>


                        <div class="category-editor-actions">

                          <button
                            type="button"
                            (click)="saveCategory(subtask)"
                            [disabled]="busyTaskId === subtask.id"
                          >
                            Guardar
                          </button>

                          <button
                            type="button"
                            (click)="cancelCategoryEdit()"
                            [disabled]="busyTaskId === subtask.id"
                          >
                            Cancelar
                          </button>

                        </div>

                      </div>

                    }


                    <!-- ACCIONES SUBTAREA -->

                    <div class="subtask-postit-actions">

                      @if (subtask.status === 'INACTIVA') {

                        <button
                          type="button"
                          (click)="activate(subtask)"
                          [disabled]="actionsDisabled(subtask)"
                        >
                          ▶ Activar
                        </button>

                      }

                      @if (subtask.status === 'ACTIVA') {

                        <button
                          type="button"
                          (click)="pause(subtask)"
                          [disabled]="actionsDisabled(subtask)"
                        >
                          ⏸ Pausar
                        </button>

                        <button
                          type="button"
                          (click)="complete(subtask)"
                          [disabled]="actionsDisabled(subtask)"
                        >
                          ✓ Completar
                        </button>

                      }

                      @if (subtask.status === 'TERMINADA') {

                        <button
                          type="button"
                          class="btn-reopen"
                          (click)="reopen(subtask)"
                          [disabled]="actionsDisabled(subtask)"
                        >
                          ↻ Reabrir
                        </button>

                      }


                      <!-- EDITAR SUBTAREA -->

                      <button
                        type="button"
                        (click)="openEditSubtask(subtask)"
                        [disabled]="actionsDisabled(subtask)"
                      >
                        ✎ Editar
                      </button>


                      <!-- ELIMINAR -->

                      <button
                        type="button"
                        class="btn-delete"
                        (click)="requestDelete(subtask)"
                        [disabled]="actionsDisabled(subtask)"
                      >
                        🗑 Eliminar
                      </button>

                    </div>


                    <!-- CONFIRMACIÓN ELIMINAR SUBTAREA -->

                    @if (confirmingDelete === subtask.id) {

                      <section class="subtask-delete-confirm">

                        <div class="delete-warning-icon">
                          ⚠
                        </div>

                        <div class="delete-warning-content">

                          <h4>
                            ¿Eliminar "{{ subtask.title }}"?
                          </h4>

                          <p>
                            La subtarea dejará de aparecer
                            en la lista.
                            Su historial se conservará.
                          </p>

                        </div>

                        <div class="delete-confirmation-actions">

                          <button
                            type="button"
                            class="btn-cancel-delete"
                            (click)="cancelDelete()"
                          >
                            Cancelar
                          </button>

                          <button
                            type="button"
                            class="btn-confirm-delete"
                            (click)="deleteTask(subtask)"
                            [disabled]="busyTaskId === subtask.id"
                          >
                            Eliminar
                          </button>

                        </div>

                      </section>

                    }

                  </article>

                </div>

              } @empty {

                <div class="subtasks-empty-state">

                  <div>
                    ▣
                  </div>

                  <h3>
                    No hay subtareas
                  </h3>

                  <p>
                    Crea una subtarea para organizar
                    mejor esta tarea.
                  </p>

                </div>

              }

            </section>

          </div>

        }


        <!-- =====================================================
             VISTA CREAR SUBTAREA
             ===================================================== -->

        @if (subtaskModalMode === 'create') {

          <header class="subtasks-modal-header">

            <div>

              <h2>
                Nueva subtarea
              </h2>

              <p>
                {{ subtasksModalParent.title }}
              </p>

            </div>

            <button
              type="button"
              class="subtasks-modal-close"
              aria-label="Cerrar"
              (click)="closeSubtasksModal()"
            >
              ×
            </button>

          </header>


          <div class="subtask-form-view">

            <app-task-form
              [parentTaskId]="subtasksModalParent.id"
              (taskCreated)="onSubtaskCreated($event)"
              (cancelled)="backToSubtasks()"
            ></app-task-form>

          </div>

        }


        <!-- =====================================================
             VISTA EDITAR SUBTAREA
             ===================================================== -->

        @if (
          subtaskModalMode === 'edit' &&
          editingSubtask
        ) {

          <header class="subtasks-modal-header">

            <div>

              <h2>
                Editar subtarea
              </h2>

              <p>
                {{ subtasksModalParent.title }}
              </p>

            </div>

            <button
              type="button"
              class="subtasks-modal-close"
              aria-label="Cerrar"
              (click)="closeSubtasksModal()"
            >
              ×
            </button>

          </header>


          <div class="subtask-form-view">

            <app-task-editor
              [task]="editingSubtask"
              (saved)="onSubtaskSaved($event)"
              (cancelled)="backToSubtasks()"
            ></app-task-editor>

          </div>

        }

      </dialog>

    </div>

  }

</div>
`,
  styles: [`/* MIS TAREAS — estilos propios de esta pantalla.
   Las primitivas compartidas (botones, tarjetas, chips, modal, panel lateral,
   campos y avisos) viven en src/design-system.css. La hoja anterior pesaba
   22,08 kB por duplicarlas todas y rompía el presupuesto de producción. */

:host { display: block; min-height: 100vh; background: var(--espejismo); }

/* ---------------- shell de la página ---------------- */
.task-list-page { width: 100%; min-height: 100vh; padding: 40px 4px 70px; box-sizing: border-box; background: var(--espejismo); color: var(--text-primary); }
.page-header { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: flex-start; gap: 16px 32px; margin-bottom: 20px; padding: 0 4px; }
.page-title { margin: 0; color: #f4f4f8; font-size: 31px; line-height: 1.2; font-weight: 650; letter-spacing: -0.03em; }
.page-subtitle { margin: 9px 0 0; color: #aaaac2; font-size: 13px; line-height: 1.5; }
.page-header-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }

.page-header-left { display: flex; flex-direction: column; gap: 8px; min-width: 0; flex: 1; }

.breadcrumb { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin: 0; padding: 0; list-style: none; }
.breadcrumb li { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-muted); }
.breadcrumb li a { color: var(--scampi); text-decoration: underline; text-decoration-thickness: 1px; text-underline-offset: 2px; cursor: pointer; font-weight: 600; }
.breadcrumb li a:hover { color: #8a8ac9; }
.breadcrumb-sep { color: var(--text-muted); font-weight: 700; user-select: none; }
.breadcrumb-current { color: var(--text-primary); font-weight: 650; }

/* ---------------- barra de proyecto (US2.2): ya no hay proyecto por defecto ---------------- */
.workspace-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin: 0 4px 22px; padding: 12px 16px; border: 1px solid var(--martinica); border-radius: 10px; background: var(--arcilla-ebano); }
.workspace-bar label { font-size: 12px; font-weight: 600; color: var(--text-secondary); }
.workspace-bar select { min-height: 38px; min-width: 260px; padding: 0 12px; border: 1px solid var(--martinica); border-radius: 6px; background: var(--espejismo); color: var(--text-primary); font-size: 13px; }
.workspace-bar select:focus { outline: none; border-color: var(--cometa); }
.workspace-bar .ratio { margin-left: auto; font-size: 13px; font-weight: 700; color: var(--priority-media); }

/* ---------------- estructura y asignaciones (US2.2 / US2.3) ---------------- */
.btn-wbs-toggle.active { background: var(--martinica); border-color: var(--cometa); }
.assigned-count { display: inline-flex; align-items: center; justify-content: center; min-width: 20px; height: 18px; padding: 0 5px; border-radius: 999px; background: var(--scampi); color: #fff; font-size: 10px; font-weight: 700; }

/* ---------------- rejilla y buscador ---------------- */
.tasks { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; padding: 0 4px; }
.search-form { margin: 0 4px 22px; }
.search-container { display: flex; gap: 8px; }
.search-input { flex: 1; min-height: 40px; padding: 0 14px; border: 1px solid var(--martinica); border-radius: 6px; background: var(--arcilla-ebano); color: var(--text-primary); font-size: 13px; }
.search-input::placeholder { color: var(--text-placeholder); }
.search-input:hover, .search-input:focus { border-color: var(--cometa); outline: none; }

/* ---------------- subtareas ---------------- */
.subtasks-count { color: var(--text-muted); font-size: 12px; }
.subtasks-button { align-self: flex-start; }
.subtasks-modal { display: flex; flex-direction: column; width: min(1020px, 100%); max-height: calc(100vh - 24px); border: 1px solid var(--martinica); border-radius: 14px; background: var(--arcilla-ebano); overflow: hidden; }
.subtasks-modal-header { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: flex-start; gap: 12px; padding: 16px 18px; border-bottom: 1px solid var(--martinica); }
.subtasks-modal-header h2 { margin: 0; font-size: 19px; font-weight: 650; }
.subtasks-modal-header p { margin: 4px 0 0; color: var(--text-muted); font-size: 12px; }
.subtasks-modal-header-actions { display: flex; align-items: center; gap: 6px; }
.subtasks-modal-summary { margin: 0; color: var(--text-secondary); font-size: 13px; }
.subtasks-modal-content { padding: 22px 18px 20px; overflow: auto; }
.subtasks-modal-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 14px; }
.subtask-postit { display: flex; flex-direction: column; gap: 10px; min-height: 240px; padding: 16px; border: 1px solid var(--martinica); border-left: 4px solid var(--scampi); border-radius: 12px; background: var(--espejismo); }
.subtask-postit.is-swiping { transform: scale(.99); }
.subtask-postit-header { display: flex; flex-direction: column; gap: 6px; }
.subtask-postit-header h3 { margin: 0; font-size: 15px; font-weight: 650; }
.subtask-postit-description { margin: 0; color: var(--text-secondary); font-size: 12px; line-height: 1.5; }
.subtask-postit-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px; margin-top: auto; }
.subtask-category-editor { display: flex; flex-wrap: wrap; gap: 6px; }
.subtask-form-view { display: flex; flex-direction: column; gap: 8px; }

/* ---------------- responsive ---------------- */
@media (max-width: 900px) {
  .tasks { grid-template-columns: 1fr; }
  .subtasks-modal-grid { grid-template-columns: 1fr; }
  .page-header { flex-direction: column; }
}
@media (max-width: 620px) {
  .task-list-page { padding: 24px 4px 50px; }
  .page-title { font-size: 24px; }
  .subtask-postit { min-height: 220px; }
  .subtask-postit-actions { grid-template-columns: minmax(0, 1fr); }
  .workspace-bar select { min-width: 100%; }
}
`]
})
export class TaskList implements OnInit {

  @Output() editRequested = new EventEmitter<Task>();
  @Output() subtaskRequested = new EventEmitter<Task>();

  /**
   * US3.3: panel de progreso en cascada. Se referencia para recalcular el árbol cada vez que
   * cambia el trabajo (completar, pausar, eliminar, crear actividad); el panel solo carga al
   * montarse o al cambiar de proyecto.
   */
  @ViewChild(ProgressPanel) private progressPanel?: ProgressPanel;

  /**
   * US2.2: el proyecto de trabajo lo elige el usuario y se guarda en esta misma sesión.
   * Antes se tomaba "el primero" de la lista, lo que mezclaba datos de proyectos ajenos.
   * Ahora, si no hay proyecto seleccionado, la pantalla no carga nada.
   */
  selectedProjectId: number | null = null;

  projects: Project[] = [];
  stages: StageDto[] = [];
  selectedStageId: number | null = null;

  /** US2.2: actividad elegida dentro de la etapa; toda tarea nueva se crea en ella. */
  selectedActivityId: number | null = null;

  tasks: Task[] = [];
  /** Actividades ("categorías") de la etapa seleccionada. */
  categories: Array<{ id: number; name: string }> = [];
  progressRatio = 0;

  subtasksModalParent: Task | null = null;

  subtaskModalMode: 'list' | 'create' | 'edit' = 'list';

  editingSubtask: Task | null = null;

  loading = false;
  successMessage = '';
  errorMessage = '';
  searchTerm = '';

  busyTaskId: number | null = null;
  pausingSubtasksForParentId: number | null = null;
  confirmingDelete: number | null = null;
  editingCategoryId: number | null = null;
  selectedCategoryName: string | null = null;

  completedAsideOpen = false;

  /** US2.2: panel de estructura (etapas y actividades) abierto/cerrado. */
  wbsPanelOpen = false;

  /** US2.3: tarea cuyo diálogo de asignación de Personal está abierto. */
  assigningTask: Task | null = null;

  swipeOffsets: Record<number, number> = {};

  private swipeStartX = 0;
  private activeSwipeTaskId: number | null = null;
  private swipeMoved = false;

  private readonly swipeThreshold = 80;
  private readonly maxSwipeDistance = 125;

  private timerInterval: ReturnType<typeof setInterval> | null = null;

  currentTime = Date.now();

  private readonly taskService = inject(TaskService);
  private readonly wbsService = inject(WbsService);
  private readonly projectService = inject(ProjectService);
  private readonly hierarchyState = inject(HierarchyStateService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  /** Mensaje de éxito/feedback mostrado tras una acción. */
  success = '';

  /** Mensaje de error mostrado tras una acción fallida. */
  error: string | null = null;

  ngOnInit(): void {
    // 1. Escuchar parámetros de ruta (enlaces profundos) y de consulta para restaurar el
    //    contexto del jerarquía: proyecto → etapa → actividad.
    combineLatest([this.route.paramMap, this.route.queryParamMap]).subscribe(([paramMap, queryMap]) => {
      const projectId = paramMap.get('projectId') ?? queryMap.get('projectId');
      const stageId = paramMap.get('stageId') ?? queryMap.get('stageId');
      const activityId = paramMap.get('activityId') ?? queryMap.get('activityId');

      const pid = projectId ? Number(projectId) : null;
      const sid = stageId ? Number(stageId) : null;
      const aid = activityId ? Number(activityId) : null;

      // Anotar la etapa y actividad pendientes de restaurar.
      this.pendingStageId = sid;
      this.pendingActivityId = aid;

      this.restoreRouteContext(pid, sid);
    });

    this.loadProjects();
    this.startTimer();

    // Sincronización reactiva: las etapas y actividades que el panel WBS (u otro
    // componente) publique en el estado compartido se reflejan aquí inmediatamente.
    this.hierarchyState.stages$.subscribe((stages) => {
      this.stages = stages;
      this.changeDetector.markForCheck();
    });
    this.hierarchyState.activities$.subscribe((activities) => {
      this.categories = activities.map((activity) => ({
        id: activity.activityId,
        name: activity.name,
      }));
      // Mantener la actividad seleccionada si aún existe.
      if (
        this.selectedActivityId !== null &&
        !this.categories.some((category) => category.id === this.selectedActivityId)
      ) {
        this.selectedActivityId = this.categories.length > 0 ? this.categories[0].id : null;
      }
      this.changeDetector.markForCheck();
    });
  }

  loadProjects(): void {
    if (this.loadingProjects) {
      return;
    }
    this.loadingProjects = true;
    this.projectService.list().subscribe({
      next: (projects) => {
        this.projects = projects;
        this.projectsLoaded = true;
        this.loadingProjects = false;

        // Sin sesión previa no se preselecciona ningún proyecto: es una decisión del usuario.
        if (this.selectedProjectId === null && this.previousProjectId !== null) {
          const exists = this.projects.some((p) => p.projectId === this.previousProjectId);
          if (exists) {
            // CORRECCIÓN: Antes solo se asignaba la variable, ahora sí carga sus etapas y tareas
            this.selectProject(this.previousProjectId);
          }
        }
        // US3.1: el Personal no tiene proyectos que seleccionar; ve solo lo que le asignaron.
        else if (this.usesPersonalScope && this.selectedProjectId === null && !this.loadedAssignedOnce) {
          this.loadAssignedTasks();
        }

        this.changeDetector.markForCheck();
      },
        error: () => {
          this.loadingProjects = false;
          this.errorMessage = 'No se pudieron cargar los proyectos.';
          this.changeDetector.markForCheck();
        },
    });
  }

  /** Restaura el contexto de un enlace profundo: proyecto → etapa (y actividad pendiente). */
  private restoreRouteContext(pid: number | null, sid: number | null): void {
    if (pid !== null && pid !== this.selectedProjectId) {
      this.previousProjectId = pid;
      if (this.projectsLoaded) {
        this.selectProject(pid);
      }
    } else if (
      pid !== null &&
      pid === this.selectedProjectId &&
      sid !== null &&
      this.stages.length > 0
    ) {
      // Proyecto ya seleccionado: restaurar la etapa directamente si está cargada.
      if (this.stages.some((stage) => stage.stageId === sid)) {
        this.selectStage(sid);
        this.pendingStageId = null;
      }
    }
  }


  /** Último proyecto elegido por el usuario en esta sesión (nunca el primero de la lista). */
  private previousProjectId: number | null = null;

  /** Etapa pendiente de restaurar desde un enlace profundo (se activa tras cargar las etapas). */
  private pendingStageId: number | null = null;

  /** Actividad pendiente de restaurar desde un enlace profundo (se activa tras cargar las actividades). */
  private pendingActivityId: number | null = null;

  /** US3.1: la vista del Personal ya se cargó al menos una vez (no se repite en cada reload). */
  private loadedAssignedOnce = false;

  /** Los proyectos ya se consultaron: distingue "Personal sin proyectos" de "aún cargando". */
  private projectsLoaded = false;

  /** Evita consultar la lista de proyectos más de una vez mientras la primera está en vuelo. */
  private loadingProjects = false;

  selectProject(projectId: number | null): void {
    this.selectedProjectId = projectId;
    this.previousProjectId = projectId;
    this.selectedStageId = null;
    this.stages = [];
    this.categories = [];
    this.selectedActivityId = null;
    this.tasks = [];
    this.wbsPanelOpen = false;
    this.assigningTask = null;

    if (projectId === null) {
      this.changeDetector.markForCheck();
      return;
    }
    this.loadedAssignedOnce = true;
    this.loadStages(projectId);
  }

  loadStages(projectId: number): void {
    this.wbsService.listStages(projectId).subscribe({
      next: (stages) => {
        this.stages = stages;
        this.hierarchyState.setStages(stages);
        this.hierarchyState.selectProject(projectId);
        // Continuar la restauración del enlace profundo con la etapa solicitada.
        if (this.pendingStageId !== null && stages.some((stage) => stage.stageId === this.pendingStageId)) {
          const stageToRestore = this.pendingStageId;
          this.pendingStageId = null;
          this.selectStage(stageToRestore);
        }
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.stages = [];
        this.hierarchyState.setStages([]);
        this.changeDetector.markForCheck();
      },
    });
  }

  selectStage(stageId: number): void {
    this.selectedStageId = stageId;
    // La actividad depende de la etapa: se vuelve a elegir al cambiar de etapa.
    this.selectedActivityId = null;
    this.loadCategories();
    this.changeDetector.markForCheck();
  }

  /** US2.2: la actividad donde se crearán las tareas nuevas. */
  selectActivity(activityId: number | null): void {
    this.selectedActivityId = activityId;
    this.changeDetector.markForCheck();
  }

  /** Reordena tareas de una actividad (arrastrar y soltar) con actualización optimista y rollback. */
  onDropTasks(event: CdkDragDrop<Task[]>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    const activityId = this.selectedActivityId;
    if (activityId == null) {
      return;
    }
    const previous = [...this.tasks];
    moveItemInArray(this.tasks, event.previousIndex, event.currentIndex);
    const newOrder = this.tasks.map((task) => task.id);
    this.changeDetector.markForCheck();

    this.taskService.reorderTasks(activityId, newOrder).subscribe({
      next: () => {
        this.success = 'Orden de tareas actualizado.';
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.tasks = previous;
        this.error = 'No se pudo guardar el orden de las tareas.';
        this.changeDetector.markForCheck();
      },
    });
  }

  /** Nombre de la actividad elegida (el formulario de crear tarea lo muestra). */
  get selectedActivityName(): string | null {
    if (this.selectedActivityId === null) {
      return null;
    }
    return this.categories.find((category) => category.id === this.selectedActivityId)?.name ?? null;
  }

  /** Proyecto elegido (para los permisos que devuelve el backend). */
  get selectedProject(): Project | null {
    if (this.selectedProjectId === null) {
      return null;
    }
    return this.projects.find((project) => project.projectId === this.selectedProjectId) ?? null;
  }

  /** US2.2/US2.3: el backend autoriza siempre; esto solo muestra u oculta acciones. */
  get canManageHierarchy(): boolean {
    return this.selectedProject?.canManageHierarchy === true;
  }

  /** Puede el usuario actual crear tareas en la actividad seleccionada. */
  get canCreateTask(): boolean {
    return this.selectedProjectId !== null && (this.usesPersonalScope || this.canManageHierarchy);
  }

  /** El rol Jefe gestiona todos los proyectos (defensa ante sesiones sin AuthService en pruebas). */
  get isChief(): boolean {
    return this.auth?.isChief() ?? false;
  }

  stageName(stageId: number): string {
    return this.stages.find((stage) => stage.stageId === stageId)?.name ?? '';
  }

  activityName(activityId: number): string {
    return this.categories.find((category) => category.id === activityId)?.name ?? '';
  }

  pageTitle(): string {
    if (this.usesPersonalScope) {
      return 'Tareas asignadas';
    }
    if (this.selectedActivityId !== null) {
      return `Tareas de ${this.activityName(this.selectedActivityId)}`;
    }
    if (this.selectedStageId !== null) {
      return `Tareas de ${this.stageName(this.selectedStageId)}`;
    }
    if (this.selectedProjectId !== null) {
      return `Tareas de ${this.selectedProject?.name}`;
    }
    return 'Mis tareas';
  }

  pageSubtitle(): string {
    if (this.usesPersonalScope) {
      return 'Estas son las tareas que tu Encargado te asignó.';
    }
    if (this.selectedActivityId !== null) {
      return 'Organiza, activa y completa las tareas de esta actividad.';
    }
    if (this.selectedStageId !== null) {
      return 'Elige una actividad para crear y gestionar sus tareas.';
    }
    if (this.selectedProjectId !== null) {
      return 'Elige una etapa y una actividad para crear tareas.';
    }
    return 'Selecciona un proyecto para comenzar.';
  }

  /** El formulario de creación se proyecta siempre dentro del listado; esta acción solo lo marca para renderizar. */
  openTaskForm(): void {
    this.changeDetector.markForCheck();
  }

  /** US2.2: abre o cierra el panel de etapas y actividades. */
  toggleWbsPanel(): void {
    this.wbsPanelOpen = !this.wbsPanelOpen;
    this.changeDetector.markForCheck();
  }

  /** US3.3: el panel de progreso publica el árbol recién calculado. */
  onProgressLoaded(progress: ProjectProgressDto): void {
    this.progressRatio = Math.round(progress.root.ratio * 100);
    this.changeDetector.markForCheck();
  }

  /** US2.3: abre el diálogo de asignación de Personal para una tarea. */
  openAssignment(task: Task): void {
    if (this.actionsDisabled(task)) {
      return;
    }
    this.assigningTask = task;
    this.changeDetector.markForCheck();
  }

  closeAssignment(): void {
    this.assigningTask = null;
    this.changeDetector.markForCheck();
  }

  /** Tras asignar o retirar Personal se recarga la lista para actualizar el contador. */
  onAssignmentsChanged(): void {
    this.successMessage = 'Asignación de Personal actualizada.';
    this.loadTasks();
    this.changeDetector.markForCheck();
  }

  /** US2.3: cuántas personas tiene asignadas la tarea (badge de la tarjeta). */
  assignedCount(task: Task): number {
    return task.assignedWorkerIds?.length ?? 0;
  }

  /** Id real de la tarea (las subtareas usan su id en negativo en la vista). */
  assignmentTaskId(task: Task): number {
    return task.entityId ?? Math.abs(task.id);
  }

  // =========================================================
  // RELOJ EN TIEMPO REAL
  // =========================================================

  /** Etapa activa para los formularios de creación. */
  get activeStageId(): number | null {
    return this.selectedStageId;
  }

  /** El Personal solo ve "Mis tareas": no tiene proyectos que seleccionar. */
  get usesPersonalScope(): boolean {
    return this.projectsLoaded && this.projects.length === 0;
  }

  private startTimer(): void {
    if (this.timerInterval) {
      return;
    }

    this.timerInterval = setInterval(() => {
      this.currentTime = Date.now();
      this.changeDetector.markForCheck();
    }, 1000);
  }

  loadTasks(): void {
    if (this.usesPersonalScope) {
      this.loadAssignedTasks();
      return;
    }

    if (this.selectedProjectId === null) {
      this.tasks = [];
      this.changeDetector.markForCheck();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.taskService.listTasks(this.selectedProjectId, this.searchTerm).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.loading = false;
        this.busyTaskId = null;

        // US3.3: el trabajo cambió (o se acaba de cargar), el progreso debe reflejarlo.
        this.progressPanel?.reload();
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        console.error('Error al cargar tareas:', error);

        this.errorMessage =
          'No se pudieron cargar las tareas.';

        this.loading = false;
        this.busyTaskId = null;

        this.changeDetector.markForCheck();
      }
    });
  }

  /**
   * US3.1: "Mis tareas" del Personal. Solo llega lo que el backend tiene asignado al usuario
   * del token (GET /api/tasks/assigned), sin datos financieros.
   */
  private loadAssignedTasks(): void {
    this.loading = true;
    this.errorMessage = '';

    this.taskService.listAssignedTasks(this.searchTerm).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.loading = false;
        this.busyTaskId = null;
        this.loadedAssignedOnce = true;

        this.changeDetector.markForCheck();
      },
      error: (error) => {
        console.error('Error al cargar las tareas asignadas:', error);
        this.errorMessage = 'No se pudieron cargar tus tareas asignadas.';
        this.loading = false;
        this.busyTaskId = null;
        this.changeDetector.markForCheck();
      },
    });
  }

  loadCategories(): void {
    if (this.selectedStageId === null) {
      this.categories = [];
      this.selectedActivityId = null;
      this.changeDetector.markForCheck();
      return;
    }

    this.taskService.listCategories(this.selectedStageId).subscribe({
      next: (categories) => {
        this.categories = categories;
        this.hierarchyState.setActivities(
          categories.map((category) => ({
            activityId: category.id,
            stageId: this.selectedStageId ?? 0,
            name: category.name,
            description: null,
            status: 'PENDIENTE',
            createdAt: '',
            updatedAt: '',
            version: 0,
          })),
        );

        // Continuar la restauración del enlace profundo con la actividad solicitada.
        if (this.pendingActivityId !== null) {
          const activityToRestore = this.pendingActivityId;
          this.pendingActivityId = null;
          if (categories.some((category) => category.id === activityToRestore)) {
            this.selectedActivityId = activityToRestore;
          } else {
            this.selectedActivityId = categories.length > 0 ? categories[0].id : null;
          }
        } else if (
          this.selectedActivityId === null ||
          !categories.some((category) => category.id === this.selectedActivityId)
        ) {
          this.selectedActivityId = categories.length > 0 ? categories[0].id : null;
        }

        this.changeDetector.markForCheck();
      },

      error: (error) => {
        console.error(
          'Error al cargar las categorías:',
          error
        );

        this.categories = [];
        this.selectedActivityId = null;
        this.changeDetector.markForCheck();
      }
    });
  }

  // =========================================================
  // TAREAS PRINCIPALES
  // =========================================================

  get parentTasks(): Task[] {
    return this.tasks.filter(
      (task) =>
        !task.parentTaskId &&
        task.status !== 'TERMINADA'
    );
  }

  // =========================================================
  // TAREAS COMPLETADAS
  // =========================================================

  get completedTasks(): Task[] {
    return this.tasks
      .filter(
        (task) => task.status === 'TERMINADA'
      )
      .sort((a, b) => {
        const dateA = a.completedAt
          ? new Date(a.completedAt).getTime()
          : 0;

        const dateB = b.completedAt
          ? new Date(b.completedAt).getTime()
          : 0;

        return dateB - dateA;
      });
  }

  getParentTask(task: Task): Task | undefined {
    if (!task.parentTaskId) {
      return undefined;
    }

    return this.tasks.find(
      (parentTask) =>
        parentTask.id === task.parentTaskId
    );
  }
  // =========================================================
  // SUBTAREAS NO TERMINADAS
  // =========================================================

  getSubtasks(parentId: number): Task[] {
    return this.tasks.filter(
      (task) =>
        task.parentTaskId === parentId &&
        task.status !== 'TERMINADA'
    );
  }

  // =========================================================
  // TODAS LAS SUBTAREAS
  // =========================================================

  getAllSubtasks(parentId: number): Task[] {
    return this.tasks.filter(
      (task) =>
        task.parentTaskId === parentId
    );
  }

  // =========================================================
  // ASIDE DE TAREAS COMPLETADAS
  // =========================================================

  openCompletedAside(): void {
    this.completedAsideOpen = true;
    this.changeDetector.markForCheck();
  }

  closeCompletedAside(): void {
    this.completedAsideOpen = false;
    this.changeDetector.markForCheck();
  }

  // =========================================================
  // POPUP DE SUBTAREAS
  // =========================================================

  openSubtasksModal(task: Task): void {
    if (this.actionsDisabled(task)) {
      return;
    }

    this.subtasksModalParent = task;
    this.subtaskModalMode = 'list';
    this.editingSubtask = null;

    this.changeDetector.markForCheck();
  }

  closeSubtasksModal(): void {
    this.subtasksModalParent = null;
    this.subtaskModalMode = 'list';
    this.editingSubtask = null;

    this.changeDetector.markForCheck();
  }

  // =========================================================
  // CREAR SUBTAREA DENTRO DEL POPUP
  // =========================================================

  openCreateSubtask(): void {
    if (!this.subtasksModalParent) {
      return;
    }

    this.subtaskModalMode = 'create';
    this.editingSubtask = null;

    this.changeDetector.markForCheck();
  }

  // =========================================================
  // EDITAR SUBTAREA DENTRO DEL POPUP
  // =========================================================

  openEditSubtask(task: Task): void {
    if (this.actionsDisabled(task)) {
      return;
    }

    this.editingSubtask = task;
    this.subtaskModalMode = 'edit';

    this.changeDetector.markForCheck();
  }

  // =========================================================
  // VOLVER A LA LISTA DE SUBTAREAS
  // =========================================================

  backToSubtasks(): void {
    this.subtaskModalMode = 'list';
    this.editingSubtask = null;

    this.changeDetector.markForCheck();
  }

  // =========================================================
  // SUBTAREA CREADA
  // =========================================================

  onSubtaskCreated(task: Task): void {
    this.successMessage =
    'Subtarea creada correctamente.';

    this.subtaskModalMode = 'list';
    this.editingSubtask = null;

    // Recargar las tareas para obtener inmediatamente
    // la subtarea recién creada y actualizar el contador.
    this.loadTasks();
    }


  // =========================================================
  // SUBTAREA EDITADA
  // =========================================================

  onSubtaskSaved(updatedTask: Task): void {
    this.tasks = this.tasks.map(
      (currentTask) =>
        currentTask.id === updatedTask.id
          ? updatedTask
          : currentTask
    );

    this.successMessage =
      'Subtarea actualizada correctamente.';

    this.subtaskModalMode = 'list';
    this.editingSubtask = null;

    this.changeDetector.markForCheck();
  }

  // =========================================================
  // COLOR DE LAS TARJETAS
  // =========================================================

  getTaskColorClass(taskId: number): string {
    const colors = [
      'postit-blue',
      'postit-green',
      'postit-purple',
      'postit-burgundy',
      'postit-brown',
      'postit-slate',
      'postit-teal',
      'postit-indigo'
    ];

    const index =
      Math.abs(taskId * 17 + 11) %
      colors.length;

    return colors[index];
  }

  // =========================================================
  // BUSCAR
  // =========================================================

  searchTasks(): void {
    const term =
      this.searchTerm.trim();

    if (!term) {
      this.loadTasks();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    if (this.usesPersonalScope) {
      // US3.1: la búsqueda del Personal se hace sobre lo asignado, en el servidor.
      this.loadAssignedTasks();
      return;
    }

    if (this.selectedProjectId === null) {
      this.loading = false;
      return;
    }

    this.taskService.listTasks(this.selectedProjectId, term).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.loading = false;

        // US3.3: la búsqueda también recalcula el progreso del proyecto.
        this.progressPanel?.reload();
        this.changeDetector.markForCheck();
      },

      error: (error) => {
        console.error(
          'Error buscando tareas:',
          error
        );

        this.errorMessage =
          'No se pudieron buscar las tareas.';

        this.loading = false;

        this.changeDetector.markForCheck();
      }
    });
  }

  // =========================================================
// SWIPE
// =========================================================

startSwipe(event: PointerEvent, task: Task): void {
  if (this.actionsDisabled(task)) {
    return;
  }

  const target = event.target as HTMLElement | null;

  /*
   * Si el usuario está interactuando con un botón,
   * no iniciar ningún swipe.
   */
  if (
    target?.closest(
      'button, input, select, textarea, a'
    )
  ) {
    return;
  }

  this.activeSwipeTaskId = task.id;
  this.swipeStartX = event.clientX;
  this.swipeMoved = false;

  const element = event.currentTarget as HTMLElement;

  try {
    element.setPointerCapture(event.pointerId);
  } catch {
    // El navegador puede no permitir pointer capture.
  }
}

moveSwipe(
  event: PointerEvent,
  task: Task
): void {
  if (
    this.activeSwipeTaskId !== task.id
  ) {
    return;
  }

  const deltaX =
    event.clientX - this.swipeStartX;

  if (Math.abs(deltaX) < 5) {
    return;
  }

  this.swipeMoved = true;

  const offset = Math.max(
    -this.maxSwipeDistance,
    Math.min(
      this.maxSwipeDistance,
      deltaX
    )
  );

  this.swipeOffsets = {
    ...this.swipeOffsets,
    [task.id]: offset,
  };

  event.preventDefault();

  this.changeDetector.markForCheck();
}

endSwipe(
  event: PointerEvent,
  task: Task
): void {
  if (
    this.activeSwipeTaskId !== task.id
  ) {
    return;
  }

  const offset =
    this.getSwipeOffset(task.id);

  this.releasePointer(event);

  this.activeSwipeTaskId = null;

  if (
    offset <= -this.swipeThreshold
  ) {
    this.resetSwipe(task.id);
    this.requestDelete(task);
    return;
  }

  if (
    offset >= this.swipeThreshold
  ) {
    this.resetSwipe(task.id);

    if (task.status === 'ACTIVA') {
      this.complete(task);
    }

    return;
  }

  this.resetSwipe(task.id);
}

cancelSwipe(
  taskId: number
): void {
  if (
    this.activeSwipeTaskId === taskId
  ) {
    this.activeSwipeTaskId = null;
  }

  this.resetSwipe(taskId);
}

resetSwipe(
  taskId: number
): void {
  this.swipeOffsets = {
    ...this.swipeOffsets,
    [taskId]: 0,
  };

  this.swipeMoved = false;

  this.changeDetector.markForCheck();
}

getSwipeOffset(
  taskId: number
): number {
  return this.swipeOffsets[taskId] ?? 0;
}

isSwiping(
  taskId: number
): boolean {
  return (
    this.activeSwipeTaskId === taskId &&
    this.swipeMoved
  );
}

private releasePointer(
  event: PointerEvent
): void {
  const element =
    event.currentTarget as HTMLElement;

  try {
    if (
      element.hasPointerCapture(
        event.pointerId
      )
    ) {
      element.releasePointerCapture(
        event.pointerId
      );
    }
  } catch {}
}
  // =========================================================
  // ACTIVAR
  // =========================================================

  activate(task: Task): void {

    if (this.actionsDisabled(task)) {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService
      .activate(task)
      .subscribe({

        next: () => {

          this.successMessage =
            'Tarea activada correctamente.';

          this.busyTaskId = null;

          this.loadTasks();
        },

        error: (error) => {

          console.error(
            'Error activando tarea:',
            error
          );

          this.errorMessage =
            'No se pudo activar la tarea.';

          this.busyTaskId = null;

          this.changeDetector.markForCheck();
        }
      });
  }

  // =========================================================
  // PAUSAR
  // =========================================================

  pause(task: Task): void {
    if (this.actionsDisabled(task)) {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService.pause(task).subscribe({
      next: () => {
        const subtasks = this.getAllSubtasks(task.id);

        const activeSubtasks = subtasks.filter(
          (subtask) => subtask.status === 'ACTIVA'
        );

        // Si no hay subtareas activas, terminamos.
        if (activeSubtasks.length === 0) {
          this.successMessage =
            'Tarea pausada correctamente.';

          this.busyTaskId = null;
          this.loadTasks();
          return;
        }

        // Bloqueamos inmediatamente todo el grupo.
        this.pausingSubtasksForParentId = task.id;

        let completed = 0;
        let hasError = false;

        activeSubtasks.forEach((subtask) => {
          this.taskService.pause(subtask).subscribe({
            next: () => {
              completed++;

              if (completed === activeSubtasks.length) {
                this.pausingSubtasksForParentId = null;
                this.busyTaskId = null;

                if (hasError) {
                  this.errorMessage =
                    'La tarea se pausó, pero algunas subtareas no pudieron pausarse.';
                } else {
                  this.successMessage =
                    'Tarea y subtareas pausadas correctamente.';
                }

                this.loadTasks();
              }
            },

            error: (error) => {
              console.error(
                `Error pausando subtarea ${subtask.id}:`,
                error
              );

              hasError = true;
              completed++;

              if (completed === activeSubtasks.length) {
                this.pausingSubtasksForParentId = null;
                this.busyTaskId = null;

                this.errorMessage =
                  'La tarea se pausó, pero algunas subtareas no pudieron pausarse.';

                this.loadTasks();
              }
            }
          });
        });
      },

      error: (error) => {
        console.error('Error pausando tarea:', error);

        this.errorMessage =
          'No se pudo pausar la tarea.';

        this.busyTaskId = null;
        this.pausingSubtasksForParentId = null;

        this.changeDetector.markForCheck();
      }
    });
  }
  // =========================================================
  // COMPLETAR
  // =========================================================

  complete(task: Task): void {
    if (this.actionsDisabled(task)) {
      return;
    }

    if (task.status !== 'ACTIVA') {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService.complete(task).subscribe({
      next: () => {
        this.successMessage = 'Tarea completada correctamente.';
        this.busyTaskId = null;

        this.loadTasks();
      },
      error: (error) => {
        console.error('Error completando tarea:', error);
        this.errorMessage = 'No se pudo completar la tarea.';
        this.busyTaskId = null;
        this.changeDetector.markForCheck();
      }
    });
  }

  // =========================================================
  // REABRIR
  // =========================================================

  reopen(task: Task): void {

    if (this.actionsDisabled(task)) {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService
      .reopen(task)
      .subscribe({

        next: (updatedTask) => {

          // Actualizar la tarea dentro de la lista
          this.tasks = this.tasks.map(
            (currentTask) =>
              currentTask.id === updatedTask.id
                ? updatedTask
                : currentTask
          );

          // La tarea reabierta pasa al principio
          this.tasks = [
            updatedTask,
            ...this.tasks.filter(
              (currentTask) =>
                currentTask.id !== updatedTask.id
            )
          ];

          this.successMessage =
            'Tarea reabierta correctamente.';

          this.busyTaskId = null;

          this.changeDetector.markForCheck();
        },

        error: (error) => {

          console.error(
            'Error reabriendo tarea:',
            error
          );

          this.errorMessage =
            'No se pudo reabrir la tarea.';

          this.busyTaskId = null;

          this.changeDetector.markForCheck();
        }
      });
  }

  // =========================================================
  // ELIMINAR
  // =========================================================

  requestDelete(task: Task): void {

    if (this.actionsDisabled(task)) {
      return;
    }

    this.confirmingDelete = task.id;
    this.clearMessages();

    this.changeDetector.markForCheck();
  }

  cancelDelete(): void {

    this.confirmingDelete = null;

    this.changeDetector.markForCheck();
  }

  deleteTask(task: Task): void {

    if (
      this.busyTaskId === task.id
    ) {
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService
      .deleteTask(task)
      .subscribe({

        next: () => {

          this.successMessage =
            'Tarea eliminada correctamente.';

          this.confirmingDelete = null;
          this.busyTaskId = null;

          this.loadTasks();
        },

        error: (error) => {

          console.error(
            'Error eliminando tarea:',
            error
          );

          this.errorMessage =
            'No se pudo eliminar la tarea.';

          this.busyTaskId = null;
          this.confirmingDelete = null;

          this.changeDetector.markForCheck();
        }
      });
  }

  // =========================================================
  // EDITAR TAREA PRINCIPAL
  // =========================================================

  editTask(task: Task): void {

    if (this.actionsDisabled(task)) {
      return;
    }

    this.editRequested.emit(task);
  }

  // =========================================================
  // CREAR SUBTAREA DESDE LA TARJETA PRINCIPAL
  // =========================================================

  createSubtask(task: Task): void {

    if (this.actionsDisabled(task)) {
      return;
    }

    this.subtaskRequested.emit(task);
  }

  // =========================================================
  // EDITAR CATEGORÍA
  // =========================================================

  startCategoryEdit(
    task: Task
  ): void {

    this.editingCategoryId = task.id;

    this.selectedCategoryName =
      task.categoryName ?? null;
  }

  cancelCategoryEdit(): void {

    this.editingCategoryId = null;
    this.selectedCategoryName = null;
  }

  saveCategory(
    task: Task
  ): void {

    if (
      this.busyTaskId !== null
    ) {
      return;
    }

    const activity = this.categories.find(
      (category) => category.name === this.selectedCategoryName
    );

    if (!activity) {
      this.errorMessage =
        'Selecciona una actividad válida.';

      this.changeDetector.markForCheck();
      return;
    }

    this.busyTaskId = task.id;
    this.clearMessages();

    this.taskService
      .changeActivity(
        task,
        activity.id
      )
      .subscribe({

        next: () => {

          this.successMessage =
            'Categoría actualizada.';

          this.editingCategoryId = null;
          this.selectedCategoryName = null;
          this.busyTaskId = null;

          this.loadTasks();
        },

        error: (error: unknown) => {

          console.error(
            'Error actualizando categoría:',
            error
          );

          this.errorMessage =
            'No se pudo actualizar la categoría.';

          this.busyTaskId = null;

          this.changeDetector.markForCheck();
        }
      });
  }

  // =========================================================
  // DESHABILITAR ACCIONES
  // =========================================================

  actionsDisabled(task: Task): boolean {
    const parentIsInactive = this.isParentInactive(task);

    const parentIsBeingPaused =
      task.parentTaskId !== null &&
      task.parentTaskId === this.pausingSubtasksForParentId;

    const isParentBeingPaused =
      !task.parentTaskId &&
      task.id === this.pausingSubtasksForParentId;

    return (
      this.loading ||
      this.busyTaskId === task.id ||
      parentIsInactive ||
      parentIsBeingPaused ||
      isParentBeingPaused ||
      (
        this.confirmingDelete !== null &&
        this.confirmingDelete !== task.id
      ) ||
      (
        this.editingCategoryId !== null &&
        this.editingCategoryId !== task.id
      )
    );
  }

  isParentInactive(task: Task): boolean {
    if (!task.parentTaskId) {
      return false;
    }

    const parentTask = this.getParentTask(task);

    return parentTask?.status === 'INACTIVA';
  }

  // =========================================================
  // TIEMPO ACTIVO
  // =========================================================

  formatActiveTime(seconds: number): string {
    if (!seconds || seconds <= 0) {
      return '00:00:00';
    }

    const hours = Math.floor(seconds / 3600);

    const minutes = Math.floor(
      (seconds % 3600) / 60
    );

    const remainingSeconds =
      seconds % 60;

    return [
      hours.toString().padStart(2, '0'),
      minutes.toString().padStart(2, '0'),
      remainingSeconds.toString().padStart(2, '0')
    ].join(':');
  }

  getActiveSeconds(task: Task): number {
    const storedSeconds = task.totalActiveSeconds ?? 0;

    if (
      task.status !== 'ACTIVA' ||
      !task.activatedAt
    ) {
      return storedSeconds;
    }

    const activatedAt =
      new Date(task.activatedAt).getTime();

    const elapsedSeconds =
      Math.max(
        0,
        Math.floor(
          (this.currentTime - activatedAt) / 1000
        )
      );

    return storedSeconds + elapsedSeconds;
  }

  // =========================================================
  // MENSAJES
  // =========================================================

  private clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  // =========================================================
  // TAREA CREADA DESDE EL FORMULARIO PRINCIPAL
  // =========================================================

  showCreatedTask(
    task: Task
  ): void {

    const exists =
      this.tasks.some(
        (currentTask) =>
          currentTask.id === task.id
      );

    if (!exists) {
      this.tasks = [
        task,
        ...this.tasks
      ];
    }

    this.changeDetector.markForCheck();
  }

  // =========================================================
// ACTUALIZAR TAREA DESDE EL EDITOR
// =========================================================

  updateTaskInList(updatedTask: Task): void {
    this.tasks = this.tasks.map(
      (task) =>
        task.id === updatedTask.id
          ? updatedTask
          : task
    );

    this.successMessage =
      'Tarea actualizada correctamente.';

    this.changeDetector.markForCheck();
  }
}
