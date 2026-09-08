import { Routes } from '@angular/router';

import { TaskHome } from './features/tasks/task-home/task-home';
import { TaskHistory } from './features/tasks/task-history/task-history';

export const routes: Routes = [
  {
    path: '',
    component: TaskHome,
  },
  {
    path: 'history',
    component: TaskHistory,
  },
  {
    path: '**',
    redirectTo: '',
  },
];