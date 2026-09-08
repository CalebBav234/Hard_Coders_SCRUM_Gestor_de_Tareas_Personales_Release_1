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
  templateUrl: './task-home.html',
  styleUrl: './task-home.css',
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