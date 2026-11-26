import { test, expect } from '@playwright/test';
import { uiLogin, apiLogin, API_URL, uniqueId } from '../fixtures/helpers';

/**
 * Flujo E2E de CRUD de app_user (Etapa 2 / EC4).
 * Ejecuta contra la base de datos de prueba aislada {@code gestor_tareas_test} (nunca gestor_tareas).
 * El Jefe de pruebas y un usuario Personal de pruebas se bootstreas en gestor_tareas_test; sus
 * credenciales se proveen por variable de entorno para no escribir secretos en el reporte.
 */
const CHIEF_EMAIL = process.env.E2E_CHIEF_EMAIL ?? 'jefe.e2e.test@gestor.local';
const CHIEF_PASSWORD = process.env.E2E_CHIEF_PASSWORD ?? 'E2EChiefPass123!';

async function crearUsuarioUI(page: any, email: string, givenNames: string, familyNames: string, password = 'ClaveSegura123') {
  await page.goto('/users/new');
  await expect(page.getByRole('heading', { name: 'Crear usuario' })).toBeVisible();
  await page.getByLabel('Correo').fill(email);
  await page.getByLabel('Nombres').fill(givenNames);
  await page.getByLabel('Apellidos').fill(familyNames);
  await page.getByLabel('Contraseña').fill(password);
  const [resp] = await Promise.all([
    page.waitForResponse(r => r.url().includes('/api/users') && r.request().method() === 'POST'),
    page.getByTestId('user-create-submit').click(),
  ]);
  return resp;
}

async function listUsers(request: any, token: string) {
  const res = await request.get(`${API_URL}/users`, { headers: { Authorization: `Bearer ${token}` } });
  expect(res.ok()).toBeTruthy();
  return res.json();
}

test.describe('CRUD de usuarios (app_user) — flujo completo de Jefe', () => {
  let chiefToken: string;

  test.beforeAll(async ({ request }) => {
    const res = await request.post(`${API_URL}/auth/login`, { data: { email: CHIEF_EMAIL, password: CHIEF_PASSWORD } });
    expect(res.ok(), `login jefe: ${res.status()}`).toBeTruthy();
    chiefToken = (await res.json()).token as string;
  });

  test('crear dos usuarios válidos y distinctos; el listado los muestra sin secretos', async ({ page, request }) => {
    await uiLogin(page, CHIEF_EMAIL, CHIEF_PASSWORD);
    await page.goto('/users');
    await expect(page.getByRole('heading', { name: /Usuarios/ })).toBeVisible();

    const emailA = uniqueId('usr').toLowerCase() + '@test.local';
    const respA = await crearUsuarioUI(page, emailA, 'Ana', 'Pérez');
    expect(respA.status()).toBe(201);
    const bodyA = await respA.json();
    expect(bodyA.password).toBeUndefined();
    expect(bodyA.passwordHash).toBeUndefined();
    expect(bodyA.chief).toBe(false);
    expect(bodyA.active).toBe(true);
    await expect(page.getByTestId('user-create-success')).toBeVisible();

    const emailB = uniqueId('usr').toLowerCase() + '@test.local';
    await crearUsuarioUI(page, emailB, 'Luis', 'Gómez');
    expect((await respA.json()).userId).toBeDefined();

    await page.goto('/users');
    await expect(page.locator('table')).toBeVisible();
    const users = await listUsers(request, chiefToken);
    const emails = users.map((u: any) => u.email);
    expect(emails).toContain(emailA);
    expect(emails).toContain(emailB);
    for (const u of users) {
      expect(u).not.toHaveProperty('passwordHash');
      expect(u).not.toHaveProperty('password');
      expect(u).not.toHaveProperty('token');
    }
  });

  test('detalle y edición de nombres y apellidos con persistencia tras recarga', async ({ page, request }) => {
    const emailA = uniqueId('usr').toLowerCase() + '@test.local';
    await uiLogin(page, CHIEF_EMAIL, CHIEF_PASSWORD);
    await crearUsuarioUI(page, emailA, 'Ana', 'Pérez');
    await page.goto('/users');
    await expect(page.locator('table')).toBeVisible();
    const row = page.locator('tr').filter({ hasText: emailA }).first();
    await row.getByRole('button', { name: 'Ver' }).click();
    await expect(page.getByRole('heading', { name: 'Ana Pérez' })).toBeVisible();

    await page.getByRole('button', { name: /✎ Editar/ }).click();
    await expect(page.getByRole('heading', { name: /Editar/ })).toBeVisible();
    await page.getByLabel('Nombres').fill('María');
    await page.getByLabel('Apellidos').fill('López');
    await page.getByTestId('user-edit-submit').click();
    await expect(page.getByTestId('user-edit-success')).toBeVisible();
    await expect(page.getByText('María López')).toBeVisible();

    await page.reload();
    await expect(page.getByText('María López')).toBeVisible();
    const users = await listUsers(request, chiefToken);
    const u = users.find((x: any) => x.email === emailA);
    expect(u.givenNames).toBe('María');
    expect(u.familyNames).toBe('López');
    expect(u.active).toBe(true);
  });

  test('editar email válido único se persiste normalizado', async ({ page, request }) => {
    const emailA = uniqueId('usr').toLowerCase() + '@test.local';
    const nuevoEmail = uniqueId('nuevo').toLowerCase() + '@test.local';
    await uiLogin(page, CHIEF_EMAIL, CHIEF_PASSWORD);
    await crearUsuarioUI(page, emailA, 'Ana', 'Pérez');
    await page.goto('/users');
    await expect(page.locator('table')).toBeVisible();
    const row = page.locator('tr').filter({ hasText: emailA }).first();
    await row.getByRole('button', { name: 'Ver' }).click();
    await page.getByRole('button', { name: /✎ Editar/ }).click();
    await expect(page.getByRole('heading', { name: /Editar/ })).toBeVisible();
    await page.getByLabel('Correo').fill(nuevoEmail);
    await page.getByTestId('user-edit-submit').click();
    await expect(page.getByTestId('user-edit-success')).toBeVisible();
    await page.reload();
    await expect(page.getByText(nuevoEmail)).toBeVisible();
    const users = await listUsers(request, chiefToken);
    const u = users.find((x: any) => x.email === nuevoEmail);
    expect(u).toBeDefined();
    expect(u.givenNames).toBe('Ana');
    expect(u.familyNames).toBe('Pérez');
    expect(u.active).toBe(true);
  });

  test('email inválido en alta y edición no crea ni modifica', async ({ page, request }) => {
    const emailA = uniqueId('usr').toLowerCase() + '@test.local';
    await uiLogin(page, CHIEF_EMAIL, CHIEF_PASSWORD);
    await crearUsuarioUI(page, emailA, 'Ana', 'Pérez');
    const preCount = (await listUsers(request, chiefToken)).length;

    // Alta con email inválido: ninguna petición POST al backend
    await page.goto('/users/new');
    await page.getByLabel('Correo').fill('invalido');
    await page.getByLabel('Nombres').fill('X');
    await page.getByLabel('Apellidos').fill('Y');
    await page.getByLabel('Contraseña').fill('ClaveSegura123');
    const postRequests: any[] = [];
    page.on('request', r => { if (r.url().includes('/api/users') && r.method() === 'POST') postRequests.push(r); });
    await page.getByTestId('user-create-submit').click();
    await expect(page.getByText(/correo válido/i)).toBeVisible();
    expect(postRequests.length).toBe(0);
    expect((await listUsers(request, chiefToken)).length).toBe(preCount);

    // Edición con email inválido: valores previos conservados, ninguna petición PUT
    await page.goto('/users');
    await expect(page.locator('table')).toBeVisible();
    const row = page.locator('tr').filter({ hasText: emailA }).first();
    await row.getByRole('button', { name: 'Ver' }).click();
    await page.getByRole('button', { name: /✎ Editar/ }).click();
    await page.getByLabel('Correo').fill('también-invalido');
    const putRequests: any[] = [];
    page.on('request', r => { if (r.url().includes(`/api/users/`) && r.method() === 'PUT') putRequests.push(r); });
    await page.getByTestId('user-edit-submit').click();
    await expect(page.getByText(/correo válido/i)).toBeVisible();
    expect(putRequests.length).toBe(0);
    const users = await listUsers(request, chiefToken);
    const u = users.find((x: any) => x.email === emailA);
    expect(u.givenNames).toBe('Ana');
    expect(u.familyNames).toBe('Pérez');
  });

  test('PUT ordinario no permite mass assignment de privilegios ni estado', async ({ request }) => {
    const emailA = uniqueId('usr').toLowerCase() + '@test.local';
    const createResp = await request.post(`${API_URL}/users`, {
      headers: { Authorization: `Bearer ${chiefToken}` },
      data: { email: emailA, givenNames: 'Ana', familyNames: 'Pérez', password: 'ClaveSegura123' },
    });
    expect(createResp.status()).toBe(201);
    const created = await createResp.json();
    const userId = created.userId as number;

    const resp = await request.put(`${API_URL}/users/${userId}`, {
      headers: { Authorization: `Bearer ${chiefToken}` },
      data: {
        givenNames: 'María',
        isChief: true,
        chief: true,
        isActive: false,
        active: false,
        password_hash: '$2a$10$injected',
        password: 'injected',
      },
    });
    expect(resp.status()).toBe(200);
    const body = await resp.json();
    expect(body.chief).toBe(false);
    expect(body.active).toBe(true);
    expect(body.email).toBe(emailA);
    const db = await request.get(`${API_URL}/users/${userId}`, { headers: { Authorization: `Bearer ${chiefToken}` } });
    const dbBody = await db.json();
    expect(dbBody.chief).toBe(false);
    expect(dbBody.active).toBe(true);
  });

  test('desactivar usuario no referenciado y verificar estado inactivo', async ({ page, request }) => {
    const emailB = uniqueId('usr').toLowerCase() + '@test.local';
    await uiLogin(page, CHIEF_EMAIL, CHIEF_PASSWORD);
    await crearUsuarioUI(page, emailB, 'Luis', 'Gómez');
    await page.goto('/users');
    await expect(page.locator('table')).toBeVisible();
    const row = page.locator('tr').filter({ hasText: emailB }).first();
    await row.getByRole('button', { name: 'Desactivar' }).click();
    const dialog = page.locator('dialog[open]');
    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText('¿Desactivar a Luis Gómez?');
    await dialog.getByTestId('user-deactivate-confirm').click();
    await expect(page.getByTestId('user-list-success')).toContainText('desactivado');
    await page.reload();
    const rowB = page.locator('tr').filter({ hasText: emailB }).first();
    await expect(rowB.locator('.user-status-inactive')).toBeVisible();
    const users = await listUsers(request, chiefToken);
    const ub = users.find((x: any) => x.email === emailB);
    expect(ub.active).toBe(false);
  });

  test('usuario desactivado no puede iniciar sesión (contexto separado, sin registrar contraseña)', async ({ page, request }) => {
    const emailInactivo = uniqueId('usr').toLowerCase() + '@test.local';
    const password = 'ClaveSegura123';
    await request.post(`${API_URL}/users`, {
      headers: { Authorization: `Bearer ${chiefToken}` },
      data: { email: emailInactivo, givenNames: 'Inactivo', familyNames: 'Usuario', password },
    });
    const users = await listUsers(request, chiefToken);
    const u = users.find((x: any) => x.email === emailInactivo);
    await request.delete(`${API_URL}/users/${u.userId}`, { headers: { Authorization: `Bearer ${chiefToken}` } });

    await page.goto('/login');
    await page.getByLabel('Correo').fill(emailInactivo);
    await page.getByLabel('Contraseña').fill(password);
    await page.getByRole('button', { name: /Ingresar/i }).click();
    await expect(page).toHaveURL(/\/login/);
    await expect(page.locator('.gt-alert-error, .gt-field-error').first()).toBeVisible();
  });

  test('usuario no Jefe no accede a administración (UI y backend 403)', async ({ page, request }) => {
    const emailPersonal = uniqueId('usr').toLowerCase() + '@test.local';
    const password = 'ClaveSegura123';
    await request.post(`${API_URL}/users`, {
      headers: { Authorization: `Bearer ${chiefToken}` },
      data: { email: emailPersonal, givenNames: 'Personal', familyNames: 'Prueba', password, isChief: false },
    });
    await uiLogin(page, emailPersonal, password);
    await page.goto('/users');
    await expect(page).not.toHaveURL(/\/users/);
    const personalToken = await apiLogin(request, emailPersonal, password);
    const res = await request.get(`${API_URL}/users`, { headers: { Authorization: `Bearer ${personalToken}` } });
    expect(res.status()).toBe(403);
  });
});
