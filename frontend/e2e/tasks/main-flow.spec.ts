import { test, expect } from '@playwright/test';
import { uiLogin, apiLogin, authHeader, USERS, API_URL, uniqueId, createProjectViaApi, getManagerId } from '../fixtures/helpers';

test.describe('Proceso principal: Proyecto -> WBS -> Tarea -> Subtarea -> Asignación', () => {
  test('flujo completo con persistencia observable (UI+API) y casos prioritarios EC3', async ({ page, request }) => {
    const runId = uniqueId('e5');
    const projectName = `Proyecto ${runId}`;
    const stageName = `Etapa ${runId}`;
    const activityName = `Actividad ${runId}`;
    const taskTitle = `Tarea ${runId}`;
    const subtaskTitle = `Subtarea ${runId}`;
    const reason = `  Refuerzo ${runId}  `;

    const chiefToken = await apiLogin(request, USERS.jefe.email);
    const managerId = await getManagerId(request, chiefToken, USERS.encargado.email);
    const project = await createProjectViaApi(request, chiefToken, managerId, projectName);
    const projectId = project.projectId;

    await uiLogin(page, USERS.jefe.email);
    await page.goto(`/?projectId=${projectId}`);
    await expect(page.locator('#workspace-project')).toHaveValue(new RegExp(String(projectId) + '$'), { timeout: 20000 });
    await expect(page.getByRole('heading', { name: new RegExp(projectName) })).toBeVisible({ timeout: 15000 });

    // Abrir estructura WBS
    await page.getByRole('button', { name: /Estructura/i }).click();
    await expect(page.getByRole('heading', { name: /Estructura del proyecto/i })).toBeVisible();

    // Crear etapa inline
    await page.getByRole('button', { name: '+ Nueva etapa' }).click();
    await page.locator('#wbs-new-stage-name').fill(stageName);
    const stageRespP = page.waitForResponse(r => r.url().includes(`/api/projects/${projectId}/stages`) && r.request().method() === 'POST');
    await page.locator('.wbs-create').getByRole('button', { name: 'Crear etapa' }).click();
    const stageResp = await stageRespP;
    expect(stageResp.status(), await stageResp.text()).toBe(201);
    const stage = await stageResp.json();
    await expect(page.locator('.wbs-stage strong', { hasText: stageName })).toBeVisible();

    // POST único etapa y persistencia tras recarga
    await page.reload();
    await expect(page.locator('#workspace-project')).toHaveValue(new RegExp(String(projectId) + '$'), { timeout: 20000 });
    await page.getByRole('button', { name: /Estructura/i }).click();
    await expect(page.locator('.wbs-stage strong', { hasText: stageName })).toHaveCount(1, { timeout: 15000 });

    // Expandir etapa y crear actividad inline
    await page.locator('.wbs-stage-toggle', { hasText: stageName }).click();
    await page.locator(`#wbs-new-activity-name-${stage.stageId}`).fill(activityName);
    const actRespP = page.waitForResponse(r => r.url().includes(`/api/stages/${stage.stageId}/activities`) && r.request().method() === 'POST');
    await page.locator('.wbs-create-activity').getByRole('button', { name: 'Crear actividad' }).click();
    const actResp = await actRespP;
    expect(actResp.status(), await actResp.text()).toBe(201);
    const activity = await actResp.json();
    await expect(page.locator('.wbs-activity-name', { hasText: activityName })).toBeVisible();

    // Seleccionar etapa y actividad
    await expect(page.locator('#workspace-stage option', { hasText: stageName })).toHaveCount(1, { timeout: 20000 });
    const stageSelP = page.waitForResponse(r => r.url().includes(/stages//activities) && r.request().method() === 'GET', { timeout: 15000 }).catch(() => null);
    await page.locator('#workspace-stage').selectOption({ label: stageName });
    await stageSelP;
    await expect(page.locator('#workspace-activity option', { hasText: activityName })).toHaveCount(1, { timeout: 20000 });
    const actSelP = page.waitForResponse(r => r.url().includes('/api/tasks') && r.request().method() === 'GET', { timeout: 15000 }).catch(() => null);
    await page.locator('#workspace-activity').selectOption({ label: activityName });
    await actSelP;
    await expect(page.locator('#category')).toContainText(activityName);

    // Crear tarea padre: 1.5h (90 min) y presupuesto separado 250.50
    await page.getByLabel('Título').first().fill(taskTitle);
    await page.getByLabel('Duración estimada (horas)').fill('1.5');
    await page.getByLabel('Presupuesto asignado (Bs)').fill('250.50');
    await page.locator('#priority').selectOption('ALTA');
    const taskRespP = page.waitForResponse(r => r.url().includes('/api/tasks') && r.request().method() === 'POST' && !r.url().includes('/assignments') && !r.url().includes('/subtasks'));
    await page.getByRole('button', { name: 'Crear tarea' }).click();
    const taskResp = await taskRespP;
    expect(taskResp.status(), await taskResp.text()).toBe(201);
    const task = await taskResp.json();
    expect(task.estimatedDurationMinutes).toBe(90);
    expect(Number(task.allocatedBudgetBob)).toBe(250.5);
    await expect(page.locator('.task-title', { hasText: taskTitle })).toBeVisible({ timeout: 15000 });

    // Persistencia observable vía API
    const chiefHeaders = await authHeader(request, USERS.jefe.email);
    const taskGet = await request.get(`${API_URL}/tasks/${task.taskId}`, { headers: chiefHeaders });
    expect(taskGet.ok()).toBeTruthy();
    const taskDb = await taskGet.json();
    expect(taskDb.estimatedDurationMinutes).toBe(90);
    expect(Number(taskDb.allocatedBudgetBob)).toBe(250.5);

    // Crear subtarea
    const taskCard = page.locator('article.task-card', { has: page.locator('.task-title', { hasText: taskTitle }) }).first();
    await taskCard.getByRole('button', { name: /Subtarea/i }).first().click();
    const subDialog = page.locator('dialog[aria-labelledby="subtask-modal-title"]');
    await expect(subDialog).toBeVisible({ timeout: 15000 });
    await subDialog.locator('#title').fill(subtaskTitle);
    const subRespP = page.waitForResponse(r => r.url().match(/\/api\/tasks\/\d+\/subtasks$/) && r.request().method() === 'POST');
    await subDialog.getByRole('button', { name: 'Crear subtarea' }).click();
    const subResp = await subRespP;
    expect(subResp.status(), await subResp.text()).toBe(201);

    // Asignar Personal con reason (trim + persistencia)
    await taskCard.getByRole('button', { name: /Asignar/i }).click();
    await expect(page.locator('#assignment-modal-title')).toHaveText(taskTitle);
    await page.locator('#assignment-worker').selectOption({ label: 'Pedro Rojas' });
    await page.locator('#assignment-reason').fill(reason);
    const asgRespP = page.waitForResponse(r => r.url().includes(`/api/tasks/${task.taskId}/assignments`) && r.request().method() === 'POST');
    await page.getByRole('button', { name: /^Asignar$/ }).click();
    const asgResp = await asgRespP;
    expect(asgResp.status(), await asgResp.text()).toBe(200);
    const asgBody = await asgResp.json();
    expect(asgBody.reason).toBe(reason.trim());
    await expect(page.locator('.assignment-item-info strong', { hasText: 'Pedro Rojas' })).toBeVisible();

    // Listado API muestra reason persistido
    const listAsg = await request.get(`${API_URL}/tasks/${task.taskId}/assignments`, { headers: chiefHeaders });
    expect(listAsg.ok()).toBeTruthy();
    const asgs = await listAsg.json();
    expect(asgs.some((a: any) => a.reason === reason.trim())).toBeTruthy();

    // Persistencia UI en contexto activo (cierre de diálogo sin perder tarea visible)
    await page.getByRole('button', { name: 'Cerrar asignaciones' }).click();
    await expect(page.locator('.task-title', { hasText: taskTitle })).toBeVisible({ timeout: 15000 });

    // "Mis tareas" (Personal): ve tarea asignada sin campos plan/finanzas (US3.1)
    await page.getByRole('button', { name: /Salir/i }).click();
    await uiLogin(page, USERS.personal.email);
    await page.goto('/');
    await expect(page.locator('.task-title', { hasText: taskTitle })).toBeVisible({ timeout: 15000 });
    const personalCard = page.locator('article.task-card', { has: page.locator('.task-title', { hasText: taskTitle }) }).first();
    await expect(personalCard.getByTestId('task-duration')).toHaveCount(0);
    await expect(personalCard).not.toContainText(/Presupuesto/i);
    await expect(personalCard).not.toContainText(/\bBs\b/);
  });
});






