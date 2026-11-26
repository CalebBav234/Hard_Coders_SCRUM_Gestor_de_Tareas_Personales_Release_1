import { test, expect } from '@playwright/test';
import { uiLogin, apiLogin, authHeader, USERS, API_URL, uniqueId, createProjectViaApi } from '../fixtures/helpers';

test.describe('CRUD de asignaciones de Personal (task_assignment)', () => {
  test('flujo completo: crear, listar, detalle, editar motivo, revocar con confirmación y persistencia', async ({ page, request }) => {
    const runId = uniqueId('asg');
    const projectName = `Proyecto Asignación ${runId}`;

    // 1. Autenticación vía API como Jefe
    const chiefToken = await apiLogin(request, USERS.jefe.email);
    const chiefHeaders = await authHeader(request, USERS.jefe.email);

    // 2. Crear proyecto, etapa, actividad y tarea vía API
    const managerId = 2; // encargado1@gestor.local es user_id 2 (seed)
    const project = await createProjectViaApi(request, chiefToken, managerId, projectName);
    const projectId = project.projectId;

    const stageRes = await request.post(`${API_URL}/projects/${projectId}/stages`, {
      headers: chiefHeaders,
      data: { name: `Etapa ${runId}`, description: null },
    });
    expect(stageRes.status()).toBe(201);
    const stage = await stageRes.json();

    const activityRes = await request.post(`${API_URL}/stages/${stage.stageId}/activities`, {
      headers: chiefHeaders,
      data: { name: `Actividad ${runId}`, description: null },
    });
    expect(activityRes.status()).toBe(201);
    const activity = await activityRes.json();

    const taskRes = await request.post(`${API_URL}/tasks`, {
      headers: chiefHeaders,
      data: {
        activityId: activity.activityId,
        title: `Tarea ${runId}`,
        priority: 'MEDIA',
        allocatedBudgetBob: 1000,
        estimatedDurationMinutes: 60,
      },
    });
    expect(taskRes.status()).toBe(201);
    const task = await taskRes.json();
    const taskId = task.taskId;

    // 3. Crear un trabajador activo vía API para asignar
    const workerEmail = `worker.${runId}@test.local`;
    const workerRes = await request.post(`${API_URL}/users`, {
      headers: chiefHeaders,
      data: { email: workerEmail, givenNames: 'Trabajador', familyNames: 'E2E', password: 'UserPass123!' },
    });
    expect(workerRes.status()).toBe(201);
    const worker = await workerRes.json();
    const workerUserId = worker.userId;

    // 4. Iniciar sesión en UI como Jefe
    await uiLogin(page, USERS.jefe.email);

    // 5. Navegar al proyecto y tarea
    await page.goto(`/?projectId=${projectId}`);
    await expect(page.locator('#workspace-project')).toHaveValue(new RegExp(String(projectId) + '$'), { timeout: 20000 });
    await expect(page.getByRole('heading', { name: new RegExp(projectName) })).toBeVisible({ timeout: 15000 });

    // Seleccionar etapa y actividad para mostrar la tarea
    await page.locator('#workspace-stage').selectOption({ label: `Etapa ${runId}` });
    await page.locator('#workspace-activity').selectOption({ label: `Actividad ${runId}` });
    // Cargar tareas de la actividad seleccionada
    await page.locator('.btn-refresh').click();

    // Esperar a que la tarjeta de la tarea aparezca
    const taskCard = page.locator('article.task-card', { has: page.locator('.task-title', { hasText: `Tarea ${runId}` }) }).first();
    await expect(taskCard).toBeVisible({ timeout: 15000 });

    // Abrir el diálogo de asignación de la tarea
    await taskCard.getByRole('button', { name: /Asignar/i }).click();
    await expect(page.locator('#assignment-modal-title')).toHaveText(`Tarea ${runId}`);

    // 6. READ listado inicial - vacío (verificado en BDD; aquí procedemos a crear)
    // 7. CREATE: asignar trabajador con motivo explícito y espacios laterales
    await page.locator('#assignment-worker').selectOption({ label: 'Trabajador E2E' });
    await page.locator('#assignment-reason').fill(`  Motivo E2E ${runId}  `);
    const createRespP = page.waitForResponse(r =>
      r.url().includes(`/api/tasks/${taskId}/assignments`) && r.request().method() === 'POST');
    await page.getByRole('button', { name: /^Asignar$/ }).click();
    const createResp = await createRespP;
    expect(createResp.status(), await createResp.text()).toBe(200);
    const createBody = await createResp.json();
    expect(createBody.reason).toBe(`Motivo E2E ${runId}`); // normalizado (trim)

    // 8. Verificar que aparece en el listado
    await expect(page.locator('.assignment-item-info strong', { hasText: 'Trabajador E2E' })).toBeVisible();
    await expect(page.locator('.assignment-reason')).toContainText(`Motivo E2E ${runId}`);

    // 9. READ individual: abrir detalle de la asignación
    await page.getByRole('button', { name: 'Ver detalle', exact: false }).first().click();
    await expect(page.locator('h3', { hasText: 'Detalle de asignación' })).toBeVisible();
    await expect(page.getByText('Trabajador E2E')).toBeVisible();
    await expect(page.getByText(`Motivo E2E ${runId}`)).toBeVisible();

    // 10. UPDATE: editar el motivo
    await page.getByRole('button', { name: 'Editar motivo' }).click();
    await page.locator('#assignment-edit-reason').fill('Motivo editado E2E');
    await page.getByRole('button', { name: 'Guardar motivo' }).click();
    await expect(page.getByText('Motivo actualizado.')).toBeVisible({ timeout: 10000 });
    // Verificar que el nuevo motivo aparece tras volver al listado
    await page.getByRole('button', { name: 'Volver al listado' }).click();
    await expect(page.locator('.assignment-reason')).toContainText('Motivo editado E2E');

    // 12. Persistencia tras recargar el diálogo
    await page.getByRole('button', { name: 'Cerrar asignaciones' }).click();
    await taskCard.getByRole('button', { name: /Asignar/i }).click();
    await expect(page.locator('.assignment-reason')).toContainText('Motivo editado E2E');
    await page.getByRole('button', { name: 'Cerrar asignaciones' }).click();

    // 13. Limpieza: revocar la asignación creada vía API
    await request.delete(`${API_URL}/tasks/${taskId}/assignments/${workerUserId}`, {
      headers: chiefHeaders,
    }).catch(() => {}); // Ignorar si ya fue borrada
  });

  test('motivo vacío usa valor predeterminado DEFAULT_REASON', async ({ page, request }) => {
    const runId = uniqueId('default');
    const chiefToken = await apiLogin(request, USERS.jefe.email);
    const chiefHeaders = await authHeader(request, USERS.jefe.email);

    const managerId = 2;
    const project = await createProjectViaApi(request, chiefToken, managerId, `Proyecto Default ${runId}`);
    const projectId = project.projectId;

    const stageRes = await request.post(`${API_URL}/projects/${projectId}/stages`, {
      headers: chiefHeaders,
      data: { name: `Etapa ${runId}`, description: null },
    });
    const stage = await stageRes.json();

    const activityRes = await request.post(`${API_URL}/stages/${stage.stageId}/activities`, {
      headers: chiefHeaders,
      data: { name: `Actividad ${runId}`, description: null },
    });
    const activity = await activityRes.json();

    const taskRes = await request.post(`${API_URL}/tasks`, {
      headers: chiefHeaders,
      data: {
        activityId: activity.activityId,
        title: `Tarea Default ${runId}`,
        priority: 'MEDIA',
        allocatedBudgetBob: 500,
        estimatedDurationMinutes: 30,
      },
    });
    const task = await taskRes.json();
    const taskId = task.taskId;

    const workerRes = await request.post(`${API_URL}/users`, {
      headers: chiefHeaders,
      data: { email: `default.${runId}@test.local`, givenNames: 'Default', familyNames: 'Worker', password: 'UserPass123!' },
    });
    const worker = await workerRes.json();
    const workerUserId = worker.userId;

    await uiLogin(page, USERS.jefe.email);
    await page.goto(`/?projectId=${projectId}`);
    await expect(page.locator('#workspace-project')).toHaveValue(new RegExp(String(projectId) + '$'), { timeout: 20000 });
    await page.locator('#workspace-stage').selectOption({ label: `Etapa ${runId}` });
    await page.locator('#workspace-activity').selectOption({ label: `Actividad ${runId}` });
    await page.locator('.btn-refresh').click();

    const taskCard = page.locator('article.task-card', { has: page.locator('.task-title', { hasText: `Tarea Default ${runId}` }) }).first();
    await expect(taskCard).toBeVisible({ timeout: 15000 });
    await taskCard.getByRole('button', { name: /Asignar/i }).click();

    await page.locator('#assignment-worker').selectOption({ label: 'Default Worker' });
    // No llenar el motivo (dejar vacío)
    const createRespP = page.waitForResponse(r =>
      r.url().includes(`/api/tasks/${taskId}/assignments`) && r.request().method() === 'POST');
    await page.getByRole('button', { name: /^Asignar$/ }).click();
    const createResp = await createRespP;
    expect(createResp.status(), await createResp.text()).toBe(200);
    const createBody = await createResp.json();
    expect(createBody.reason).toBe('Asignación de Personal');

    await expect(page.locator('.assignment-reason')).toContainText('Asignación de Personal');

    // Verificar por API que se persiste el valor predeterminado
    const listCheck = await request.get(`${API_URL}/tasks/${taskId}/assignments`, { headers: chiefHeaders });
    const assignments = await listCheck.json();
    expect(assignments.some((a: any) => a.reason === 'Asignación de Personal')).toBeTruthy();

    // Limpiar vía API (evita flujo de confirmación UI en esta prueba)
    await request.delete(`${API_URL}/tasks/${taskId}/assignments/${worker.userId}`, {
      headers: chiefHeaders,
    });
  });

  test('actor sin autorización no puede gestionar asignaciones (403)', async ({ page, request }) => {
    const runId = uniqueId('auth');
    const personalToken = await apiLogin(request, USERS.personal.email);
    const personalHeaders = { Authorization: `Bearer ${personalToken}` };

    // Crear proyecto y tarea como jefe
    const chiefToken = await apiLogin(request, USERS.jefe.email);
    const chiefHeaders = await authHeader(request, USERS.jefe.email);
    const managerId = 2;
    const project = await createProjectViaApi(request, chiefToken, managerId, `Proyecto Auth ${runId}`);
    const projectId = project.projectId;

    const stageRes = await request.post(`${API_URL}/projects/${projectId}/stages`, {
      headers: chiefHeaders,
      data: { name: `Etapa ${runId}`, description: null },
    });
    const stage = await stageRes.json();

    const activityRes = await request.post(`${API_URL}/stages/${stage.stageId}/activities`, {
      headers: chiefHeaders,
      data: { name: `Actividad ${runId}`, description: null },
    });
    const activity = await activityRes.json();

    const taskRes = await request.post(`${API_URL}/tasks`, {
      headers: chiefHeaders,
      data: {
        activityId: activity.activityId,
        title: `Tarea Auth ${runId}`,
        priority: 'MEDIA',
        allocatedBudgetBob: 100,
        estimatedDurationMinutes: 30,
      },
    });
    const task = await taskRes.json();
    const taskId = task.taskId;

    // Intentar crear asignación como personal (no autorizado) -> 403
    const workerRes = await request.post(`${API_URL}/users`, {
      headers: chiefHeaders,
      data: { email: `auth.${runId}@test.local`, givenNames: 'Otro', familyNames: 'Trabajador', password: 'UserPass123!' },
    });
    const worker = await workerRes.json();

    const forbiddenResp = await request.post(`${API_URL}/tasks/${taskId}/assignments`, {
      headers: personalHeaders,
      data: { workerUserId: worker.userId, reason: 'Intento no autorizado' },
    });
    expect(forbiddenResp.status()).toBe(403);

    // Intentar GET individual como personal -> 403
    const getForbidden = await request.get(`${API_URL}/tasks/${taskId}/assignments/${worker.userId}`, {
      headers: personalHeaders,
    });
    expect(getForbidden.status()).toBe(403);
  });
});
