import { Component } from '@angular/core';
import { TaskForm } from './features/tasks/task-form/task-form';
import { TaskList } from './features/tasks/task-list/task-list';

@Component({
  selector: 'app-root',
  imports: [TaskForm, TaskList],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  onTaskCreated(taskList: TaskList): void {
    taskList.loadTasks();
  }
}
