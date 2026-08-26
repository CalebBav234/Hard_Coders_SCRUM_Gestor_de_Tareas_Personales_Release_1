import { Component } from '@angular/core';
import { Task } from '../../../core/models/task';

@Component({
  selector: 'app-task-list',
  imports: [],
  templateUrl: './task-list.html',
  styleUrl: './task-list.css'
})
export class TaskList {

  tasks: Task[] = [
    {
      id: 1,
      title: 'Estudiar para el examen',
      status: 'INACTIVA',
      priority: 'MEDIA'
    },
    {
      id: 2,
      title: 'Terminar proyecto',
      status: 'ACTIVA',
      priority: 'ALTA'
    }
  ];

}