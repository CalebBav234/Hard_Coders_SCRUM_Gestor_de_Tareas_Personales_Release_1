import { test, expect } from '@playwright/test';
import { uiLogin, apiLogin, USERS, API_URL, uniqueId, createProjectViaApi, getManagerId } from '../fixtures/helpers';

test.describe('Validaciones y casos negativos priorizados EC3', () => {
  test('tarea: validación duración no múltiplo 0.25 y presupuesto >2 decimales en UI; negativos API (-1 duración, -0.01 presupuesto)', async ({ page, request }) => {
    const runId = uniqueId('val');
    const chiefToken = await apiLogin(request, USERS.jefe.email);
    const managerId = await getManagerId(request, chiefToken, USERS.encargado.email);
    const project = await createProjectViaApi(request, chiefToken, managerId, `P ${runId}`);
    const projectId = project.projectId;
    const headers = { Authorization: `Bearer ${chiefToken}` };
    const st = await request.post(`${API_URL}/projects/${projectId}/stages`, { headers, data: { name: `S ${runId}`, description: null } });
    expect(st.status(), await st.text()).toBe(201); const stage = await st.json();
    const ac = await request.post(`${API_URL}/stages/${stage.stageId}/activities`, { headers, data: { name: `A ${runId}`, description: null } });
    expect(ac.status(), await ac.text()).toBe(201); const activity = await ac.json();

    await uiLogin(page, USERS.jefe.email);
    await page.goto(`/?projectId=${projectId}`);
    await expect(page.locator('#workspace-project')).toHaveValue(new RegExp(String(projectId) + '$'), { timeout: 20000 });
    await expect(page.locator('#workspace-stage option', { hasText: `S ${runId}` })).toHaveCount(1, { timeout: 20000 });
    const stageSelP = page.waitForResponse(r => r.url().includes('/stages/' + String(stage.stageId) + '/activities') && r.request().method() === 'GET', { timeout: 15000 }).catch(() => null);
    await page.locator('#workspace-stage').selectOption({ label: `S ${runId}` });
    await expect(page.locator('#workspace-activity option', { hasText: `A ${runId}` })).toHaveCount(1, { timeout: 15000 });
    await page.locator('#workspace-activity').selectOption({ label: `A ${runId}` });
    await expect(page.locator('#category')).toContainText(`A ${runId}`);

    // Duración 0.1 (no múltiplo 0.25)
    await page.getByLabel('Título').first().fill(`Tbad ${runId}`);
    await page.getByLabel('Duración estimada (horas)').fill('0.1');
    await page.getByLabel('Presupuesto asignado (Bs)').fill('10');
    await page.getByRole('button', { name: 'Crear tarea' }).click();
    await expect(page.locator('.error-message', { hasText: /incrementos de 0\.25/i })).toBeVisible();

    // Presupuesto 3 decimales
    await page.getByLabel('Duración estimada (horas)').fill('1');
    await page.getByLabel('Presupuesto asignado (Bs)').fill('1.234');
    await page.getByRole('button', { name: 'Crear tarea' }).click();
    await expect(page.locator('.error-message', { hasText: /hasta 13 dígitos enteros y 2 decimales/i })).toBeVisible();

    // Negativos API
    const negDur = await request.post(`${API_URL}/tasks`, { headers, data: { activityId: activity.activityId, title: 'NegD', priority: 'MEDIA', allocatedBudgetBob: 0, estimatedDurationMinutes: -1 } });
    expect(negDur.status()).toBe(400);
    const negBud = await request.post(`${API_URL}/tasks`, { headers, data: { activityId: activity.activityId, title: 'NegB', priority: 'MEDIA', allocatedBudgetBob: -0.01, estimatedDurationMinutes: 0 } });
    expect(negBud.status()).toBe(400);
  });

  test('asignación: motivo default/501/500, duplicado e inactivo vía API (persistencia y errores observables)', async ({ request }) => {
    const runId = uniqueId('asg');
    const chiefToken = await apiLogin(request, USERS.jefe.email);
    const managerId = await getManagerId(request, chiefToken, USERS.encargado.email);
    const project = await createProjectViaApi(request, chiefToken, managerId, `P ${runId}`);
    const headers = { Authorization: `Bearer ${chiefToken}` };
    const st = await request.post(`${API_URL}/projects/${project.projectId}/stages`, { headers, data: { name: `S ${runId}`, description: null } });
    expect(st.status(), await st.text()).toBe(201);
    const stage = await st.json();
    const ac = await request.post(`${API_URL}/stages/${stage.stageId}/activities`, { headers, data: { name: `A ${runId}`, description: null } });
    expect(ac.status(), await ac.text()).toBe(201);
    const activity = await ac.json();
    const t = await request.post(`${API_URL}/tasks`, { headers, data: { activityId: activity.activityId, title: `T ${runId}`, priority: 'MEDIA', allocatedBudgetBob: 0, estimatedDurationMinutes: 15 } });
    expect(t.status(), await t.text()).toBe(201);
    const task = await t.json();

    const def = await request.post(`${API_URL}/tasks/${task.taskId}/assignments`, { headers, data: { workerUserId: 4, reason: null } });
    expect(def.status(), await def.text()).toBe(200);
    expect((await def.json()).reason).toBe('Asignación de Personal');

    const dup = await request.post(`${API_URL}/tasks/${task.taskId}/assignments`, { headers, data: { workerUserId: 4, reason: 'otra' } });
    expect([400, 409]).toContain(dup.status());

    const ina = await request.post(`${API_URL}/tasks/${task.taskId}/assignments`, { headers, data: { workerUserId: 5, reason: null } });
    expect([400, 404]).toContain(ina.status());

    const r501 = await request.post(`${API_URL}/tasks/${task.taskId}/assignments`, { headers, data: { workerUserId: 3, reason: 'a'.repeat(501) } });
    expect(r501.status()).toBe(400);
    const r500 = await request.post(`${API_URL}/tasks/${task.taskId}/assignments`, { headers, data: { workerUserId: 3, reason: 'a'.repeat(500) } });
    expect(r500.status(), await r500.text()).toBe(200);
    expect((await r500.json()).reason.length).toBe(500);

    const list = await request.get(`${API_URL}/tasks/${task.taskId}/assignments`, { headers });
    expect(list.ok()).toBeTruthy();
    const asgs = await list.json();
    expect(asgs.some((a: any) => a.workerUserId === 4 && a.reason === 'Asignación de Personal')).toBeTruthy();
    expect(asgs.some((a: any) => a.workerUserId === 3 && a.reason.length === 500)).toBeTruthy();
  });

  test('proyecto: crear desde UI como Jefe con datos válidos', async ({ page }) => {
    const name = `Proyecto UI ${uniqueId('pui')}`;
    await uiLogin(page, USERS.jefe.email);
    await page.getByRole('link', { name: 'Proyectos' }).click();
    await page.getByRole('button', { name: /Nuevo proyecto/i }).click();
    await page.locator('#project-name').fill(name);
    await page.locator('#project-description').fill('Desc E2E');
    await page.locator('#project-manager').selectOption({ index: 1 });
    await page.locator('#project-budget').fill('1000');
    await page.locator('#project-duration').fill('480');
    const respP = page.waitForResponse(r => r.url().includes('/api/projects') && r.request().method() === 'POST');
    await page.getByRole('button', { name: 'Crear proyecto' }).click();
    const resp = await respP;
    expect(resp.status()).toBe(201);
    await expect(page.getByRole('heading', { name })).toBeVisible({ timeout: 15000 });
  });
});

