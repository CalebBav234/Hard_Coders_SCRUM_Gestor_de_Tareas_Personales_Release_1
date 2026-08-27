import { Component } from '@angular/core';
import { Task } from './core/models/task';
import { TaskForm } from './features/tasks/task-form/task-form';
import { TaskList } from './features/tasks/task-list/task-list';

@Component({
  selector: 'app-root',
  imports: [TaskForm, TaskList],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  onTaskCreated(taskList: TaskList, task: Task): void {
    taskList.showCreatedTask(task);
  }
}
