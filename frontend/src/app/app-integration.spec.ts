import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { App } from './app';
import { Task } from './core/models/task';
import { TaskList } from './features/tasks/task-list/task-list';

describe('Task list / history integration', () => {
  it('keeps the live timer and refreshes history after complete, reopen and delete', async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(App);
    const element: HTMLElement = fixture.nativeElement;
    const active: Task = {
      id: 42,
      title: 'Tarea con historial',
      status: 'ACTIVA',
      priority: 'MEDIA',
      version: 1,
      activatedAt: new Date(Date.now() - 5000).toISOString(),
      totalActiveSeconds: 60,
    };
    fixture.detectChanges();
    http.expectOne('/api/tasks').flush([active]);
    const initialHistory = http.expectOne('/api/tasks/history');
    fixture.detectChanges();
    const list = fixture.debugElement.query(By.directive(TaskList)).componentInstance as TaskList;
    expect(list.liveTimes[42]).toBeGreaterThanOrEqual(65);
    expect(element.querySelector('.active-time')?.textContent).toContain('01:');
    expect(element.querySelector('a[href="#task-history"]')).not.toBeNull();

    // Finish the task while the initial history request is still pending.
    list.complete(active);
    const completed: Task = { ...active, status: 'TERMINADA', activatedAt: null, version: 2 };
    http.expectOne('/api/tasks/42/complete').flush(completed);
    expect(initialHistory.cancelled).toBe(true);
    http.expectOne('/api/tasks/history').flush([{ ...completed, events: [] }]);
    http.expectOne('/api/tasks').flush([completed]);
    await fixture.whenStable();
    expect(element.querySelector('.history-card h3')?.textContent).toContain(active.title);

    list.reopen(completed);
    const reopened = { ...active, version: 3 };
    http.expectOne('/api/tasks/42/reopen').flush(reopened);
    http.expectOne('/api/tasks/history').flush([]);
    http.expectOne('/api/tasks').flush([reopened]);
    await fixture.whenStable();
    expect(element.querySelector('.history-card')).toBeNull();

    list.requestDelete(reopened);
    list.confirmDelete();
    http
      .expectOne('/api/tasks/42?version=3')
      .flush(null, { status: 204, statusText: 'No Content' });
    http
      .expectOne('/api/tasks/history')
      .flush([{ ...reopened, deletedAt: new Date().toISOString(), events: [] }]);
    await fixture.whenStable();
    expect(element.querySelector('.archive-badge')?.textContent).toContain('Eliminada');
    expect(element.querySelector('.task-card')).toBeNull();
    http.verify();
    fixture.destroy();
  });
});
