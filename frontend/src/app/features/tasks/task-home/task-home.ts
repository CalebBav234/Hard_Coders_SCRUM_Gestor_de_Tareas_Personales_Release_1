import { Component } from '@angular/core';

import { Task } from '../../../core/models/task';

import { TaskEditor } from '../task-editor/task-editor';
import { TaskForm } from '../task-form/task-form';
import { TaskList } from '../task-list/task-list';

@Component({
  selector: 'app-task-home',
  standalone: true,
  imports: [
    TaskForm,
    TaskList,
    TaskEditor
  ],
  template: `<div class="task-home">

  <!-- LISTA DE TAREAS Y JERARQUÍA -->
  <section class="tasks-section">
    <app-task-list
      #taskList
      (editRequested)="openEditor($event)"
      (subtaskRequested)="openSubtaskForm($event)"
    >
      <!-- PROYECTAMOS EL FORMULARIO DE CREACIÓN AL INTERIOR DEL TASK-LIST -->
      <section class="create-section" task-form>
        <app-task-form
          [activityId]="taskList.selectedActivityId"
          [activityName]="taskList.selectedActivityName"
          (taskCreated)="onTaskCreated(taskList, $event)"
        ></app-task-form>
      </section>

    </app-task-list>
  </section>
  <!-- =========================
       MODAL DE EDICIÓN
       ========================= -->

  @if (editingTask) {

    <div
      class="modal-backdrop"
      (click)="closeEditor()"
    ></div>

    <dialog
      class="editor-modal"
      open
      aria-modal="true"
      aria-labelledby="editor-modal-title"
    >

      <header class="editor-modal-header">

        <div class="editor-modal-heading">

          <span class="editor-eyebrow">
            EDITAR TAREA
          </span>

          <h2 id="editor-modal-title">
            {{ editingTask.title }}
          </h2>

          <p>
            Modifica los datos de la tarea sin perder su estado ni su progreso.
          </p>

        </div>

        <button
          type="button"
          class="modal-close"
          aria-label="Cerrar editor"
          (click)="closeEditor()"
        >
          ×
        </button>

      </header>

      <div class="editor-modal-content">

        <app-task-editor
          [task]="editingTask"
          (saved)="onTaskSaved(taskList, $event)"
          (cancelled)="closeEditor()"
        ></app-task-editor>

      </div>

    </dialog>

  }


  <!-- =========================
       MODAL PARA CREAR SUBTAREA
       ========================= -->

  @if (creatingSubtaskFor) {

    <div
      class="modal-backdrop"
      (click)="closeSubtaskForm()"
    ></div>

    <dialog
      class="editor-modal"
      open
      aria-modal="true"
      aria-labelledby="subtask-modal-title"
    >

      <header class="editor-modal-header">

        <div class="editor-modal-heading">

          <span class="editor-eyebrow">
            NUEVA SUBTAREA
          </span>

          <h2 id="subtask-modal-title">
            Crear subtarea
          </h2>

          <p>
            Se agregará como subtarea de:
            <strong>{{ creatingSubtaskFor.title }}</strong>
          </p>

        </div>

        <button
          type="button"
          class="modal-close"
          aria-label="Cerrar formulario"
          (click)="closeSubtaskForm()"
        >
          ×
        </button>

      </header>

      <div class="editor-modal-content">

        <app-task-form
        [parentTaskId]="creatingSubtaskFor.id"
        (taskCreated)="onSubtaskCreated(taskList)"
        (cancelled)="closeSubtaskForm()"

        > </app-task-form>


      </div>

    </dialog>

  }

</div>
`,
  styles: [`:host {
  display: block;
  width: 100%;
}

.task-home {
  position: relative;
  width: 100%;
  max-width: 1480px;
  margin: 0 auto;
}

/* =========================
   CREAR TAREA
   ========================= */

.create-section {
  width: calc(100% - 8px);
  margin: 40px 4px 58px;
  border: 1px solid var(--cometa);
  border-radius: 10px;
  background: var(--arcilla-ebano);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

/* =========================
   LISTA DE TAREAS
   ========================= */

.tasks-section {
  width: 100%;
}

/* =========================
   FONDO DEL MODAL
   ========================= */

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1100;

  background: rgba(18, 18, 38, 0.78);

  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);

  animation: modalBackdropIn 0.16s ease-out;
}

/* =========================
   MODAL
   ========================= */

.editor-modal {
  position: fixed;
  top: 50%;
  left: 50%;
  z-index: 1101;

  display: flex;
  flex-direction: column;

  width: min(720px, calc(100vw - 32px));
  max-height: min(860px, calc(100vh - 32px));

  transform: translate(-50%, -50%);
  overflow: hidden;

  border: 1px solid var(--cometa);
  border-radius: 12px;

  background: var(--arcilla-ebano);

  box-shadow:
    0 24px 70px rgba(0, 0, 0, 0.45),
    0 0 0 1px rgba(104, 104, 166, 0.05);

  animation: modalIn 0.18s ease-out;
}

/* =========================
   CABECERA
   ========================= */

.editor-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;

  flex-shrink: 0;

  padding: 20px 22px;

  border-bottom: 1px solid var(--martinica);

  background: var(--arcilla-ebano);
}

.editor-modal-heading {
  min-width: 0;
}

.editor-eyebrow {
  display: block;

  margin-bottom: 6px;

  color: var(--scampi);

  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.editor-modal-heading h2 {
  margin: 0;

  overflow: hidden;

  color: var(--text-primary);

  font-size: 18px;
  font-weight: 650;
  line-height: 1.3;

  text-overflow: ellipsis;
}

.editor-modal-heading p {
  margin: 6px 0 0;

  color: var(--text-muted);

  font-size: 11px;
  line-height: 1.5;
}

.editor-modal-heading strong {
  color: var(--text-secondary);
  font-weight: 600;
}

/* =========================
   BOTÓN CERRAR
   ========================= */

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;

  width: 32px;
  height: 32px;

  flex-shrink: 0;

  border: 1px solid var(--martinica);
  border-radius: 7px;

  background: transparent;

  color: var(--text-muted);

  font-family: inherit;
  font-size: 22px;
  font-weight: 300;
  line-height: 1;

  cursor: pointer;

  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease;
}

.modal-close:hover {
  border-color: var(--cometa);
  background: var(--martinica);
  color: var(--text-primary);
}

.modal-close:active {
  background: var(--cometa);
}

/* =========================
   CONTENIDO
   ========================= */

.editor-modal-content {
  min-height: 0;

  overflow-y: auto;

  padding: 22px;

  background: var(--arcilla-ebano);
}

/* =========================
   ANIMACIONES
   ========================= */

@keyframes modalBackdropIn {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: translate(-50%, -48%) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

/* =========================
   RESPONSIVE
   ========================= */

@media (max-width: 900px) {
  .create-section {
    margin-top: 32px;
  }
}

@media (max-width: 650px) {
  .create-section {
    width: 100%;
    margin: 28px 0 45px;
  }

  .editor-modal {
    width: calc(100vw - 20px);
    max-height: calc(100vh - 20px);
    border-radius: 10px;
  }

  .editor-modal-header {
    padding: 17px 18px;
  }

  .editor-modal-content {
    padding: 18px;
  }

  .editor-modal-heading h2 {
    font-size: 16px;
  }
}`],
})
export class TaskHome {

  editingTask: Task | null = null;

  creatingSubtaskFor: Task | null = null;

  onTaskCreated(taskList: TaskList, task: Task): void {
    taskList.showCreatedTask(task);
  }

  openEditor(task: Task): void {
    console.log('TASK HOME RECIBIÓ EDITAR', task);

    this.creatingSubtaskFor = null;
    this.editingTask = task;
  }

  openSubtaskForm(task: Task): void {
    console.log('TASK HOME RECIBIÓ SUBTAREA', task);

    this.editingTask = null;
    this.creatingSubtaskFor = task;
  }

  closeEditor(): void {
    this.editingTask = null;
  }

  closeSubtaskForm(): void {
    this.creatingSubtaskFor = null;
  }

  onTaskSaved(taskList: TaskList, task: Task): void {
    taskList.updateTaskInList(task);
    this.editingTask = null;
    }

  onSubtaskCreated(taskList: TaskList): void {
    this.creatingSubtaskFor = null;

    // Actualizar inmediatamente la lista y los contadores.
    taskList.loadTasks();
    }


}