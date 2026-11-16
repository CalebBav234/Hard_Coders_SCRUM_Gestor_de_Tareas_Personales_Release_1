import { test, expect } from '@playwright/test';
import { uiLogin, apiLogin, USERS, API_URL, uniqueId } from '../fixtures/helpers';

test.describe('Gestión de usuarios (A6 - app_user CREATE)', () => {
  test('Jefe crea usuario válido desde UI y el nuevo usuario puede iniciar sesión', async ({ page, request }) => {
    const email = uniqueId('e2e-usr').toLowerCase() + '@test.local';
    const password = 'ClaveSegura123';

    await uiLogin(page, USERS.jefe.email);
    await page.goto('/users/new');
    await expect(page.getByRole('heading', { name: 'Crear usuario' })).toBeVisible();

    await page.getByLabel('Correo').fill(email);
    await page.getByLabel('Nombres').fill('Usuario E2E');
    await page.getByLabel('Apellidos').fill('Prueba Cinco');
    await page.getByLabel('Contraseña').fill(password);

    const createRespPromise = page.waitForResponse(r => r.url().includes('/api/users') && r.request().method() === 'POST');
    await page.getByTestId('user-create-submit').click();
    const createResp = await createRespPromise;
    expect(createResp.status()).toBe(201);
    const body = await createResp.json();
    expect(body.email).toBe(email);
    expect(body.password).toBeUndefined();
    expect(body.passwordHash).toBeUndefined();

    await expect(page.getByTestId('user-create-success')).toBeVisible();

    // Verificar que puede iniciar sesión con las credenciales creadas
    await page.getByRole('button', { name: /Salir/i }).click();
    await expect(page).toHaveURL(/\/login/);
    await page.getByLabel('Correo').fill(email);
    await page.getByLabel('Contraseña').fill(password);
    await page.getByRole('button', { name: /Ingresar/i }).click();
    await expect(page).not.toHaveURL(/\/login/);
  });

  test('email duplicado muestra error y no crea usuario adicional', async ({ page, request }) => {
    const email = uniqueId('e2e-dup').toLowerCase() + '@test.local';
    const token = await apiLogin(request, USERS.jefe.email);
    const pre = await request.post(`${API_URL}/users`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { email, givenNames: 'Dup', familyNames: 'User', password: 'ClaveSegura123' },
    });
    expect(pre.status()).toBe(201);

    await uiLogin(page, USERS.jefe.email);
    await page.goto('/users/new');
    await page.getByLabel('Correo').fill(email);
    await page.getByLabel('Nombres').fill('Otro');
    await page.getByLabel('Apellidos').fill('Intento');
    await page.getByLabel('Contraseña').fill('ClaveSegura123');
    const respPromise = page.waitForResponse(r => r.url().includes('/api/users') && r.request().method() === 'POST');
    await page.getByTestId('user-create-submit').click();
    const resp = await respPromise;
    expect([400, 409]).toContain(resp.status());
    await expect(page.getByTestId('user-create-error')).toBeVisible();
  });

  test('validación UI: contraseña corta bloquea envío', async ({ page }) => {
    await uiLogin(page, USERS.jefe.email);
    await page.goto('/users/new');
    await page.getByLabel('Correo').fill('shortpwd@test.local');
    await page.getByLabel('Nombres').fill('Nom');
    await page.getByLabel('Apellidos').fill('Ape');
    await page.getByLabel('Contraseña').fill('short');
    await page.getByLabel('Contraseña').blur();
    await expect(page.getByText(/entre 8 y 200/i)).toBeVisible();
    await expect(page.getByTestId('user-create-submit')).toBeDisabled();
  });

  test('Personal no puede crear usuarios vía API (403) y no accede a /users/new', async ({ page, request }) => {
    await uiLogin(page, USERS.personal.email);
    await page.goto('/users/new');
    await expect(page).not.toHaveURL(/\/users\/new/);

    const token = await apiLogin(request, USERS.personal.email);
    const res = await request.post(`${API_URL}/users`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { email: uniqueId('nope') + '@test.local', givenNames: 'X', familyNames: 'Y', password: 'ClaveSegura123' },
    });
    expect(res.status()).toBe(403);
  });
});
