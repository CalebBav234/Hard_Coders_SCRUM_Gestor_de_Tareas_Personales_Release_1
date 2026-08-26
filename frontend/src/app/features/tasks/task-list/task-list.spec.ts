import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TaskList } from './task-list';
import { TaskService } from '../../../core/services/task.service';

describe('TaskList', () => {
  let component: TaskList;
  let fixture: ComponentFixture<TaskList>;
  const taskService = {
    listTasks: () => of([]),
    activate: () => of({} as unknown as import('../../../core/models/task').Task),
    complete: () => of({} as unknown as import('../../../core/models/task').Task)
  } as unknown as TaskService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskList],
      providers: [{ provide: TaskService, useValue: taskService }]
    }).compileComponents();

    fixture = TestBed.createComponent(TaskList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
