import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/auth/auth.guard';
import { chiefGuard } from './core/auth/chief.guard';
import { Login } from './features/auth/login/login';
import { ProjectList } from './features/projects/project-list/project-list';
import { TaskHome } from './features/tasks/task-home/task-home';
import { TaskHistory } from './features/tasks/task-history/task-history';
import { UserForm } from './features/users/user-form/user-form';
import { UserList } from './features/users/user-list/user-list';
import { UserDetail } from './features/users/user-detail/user-detail';
import { UserEdit } from './features/users/user-edit/user-edit';

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
    path: 'projects/:projectId',
    component: TaskHome,
    canActivate: [authGuard],
  },
  {
    path: 'projects/:projectId/stages/:stageId',
    component: TaskHome,
    canActivate: [authGuard],
  },
  {
    path: 'projects/:projectId/stages/:stageId/activities/:activityId',
    component: TaskHome,
    canActivate: [authGuard],
  },
  {
    path: 'projects/:projectId/stages/:stageId/activities/:activityId/tasks/:taskId',
    component: TaskHome,
    canActivate: [authGuard],
  },
  {
    path: 'projects/:projectId/stages/:stageId/activities/:activityId/tasks/:taskId/subtasks/:subtaskId',
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
    path: 'users/new',
    component: UserForm,
    canActivate: [authGuard, chiefGuard],
  },
  {
    path: 'users',
    component: UserList,
    canActivate: [authGuard, chiefGuard],
  },
  {
    path: 'users/:userId',
    component: UserDetail,
    canActivate: [authGuard, chiefGuard],
  },
  {
    path: 'users/:userId/edit',
    component: UserEdit,
    canActivate: [authGuard, chiefGuard],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
