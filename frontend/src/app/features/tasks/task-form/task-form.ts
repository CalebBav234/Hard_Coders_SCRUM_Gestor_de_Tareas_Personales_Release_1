import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-task-form',
  imports: [FormsModule],
  templateUrl: './task-form.html',
  styleUrl: './task-form.css'
})
export class TaskForm {

  title = '';
  submitted = false;

  crearTarea(): void {
    this.submitted = true;

    if (this.title.trim() === '') {
      return;
    }

    console.log('Tarea creada:', this.title);

    this.title = '';
    this.submitted = false;
  }
}