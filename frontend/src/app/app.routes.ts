import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/auth/auth.guard';
import { Login } from './features/auth/login/login';
import { ProjectList } from './features/projects/project-list/project-list';
import { TaskHome } from './features/tasks/task-home/task-home';
import { TaskHistory } from './features/tasks/task-history/task-history';

export const routes: Routes = [
  {
    path: 'login',
    component: Login,
    canActivate: [guestGuard],
  },
  {
    path: '',
    component: TaskHome,
    canActivate: [authGuard],
  },
  {
    path: 'history',
    component: TaskHistory,
    canActivate: [authGuard],
  },
  {
    path: 'projects',
    component: ProjectList,
    canActivate: [authGuard],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
