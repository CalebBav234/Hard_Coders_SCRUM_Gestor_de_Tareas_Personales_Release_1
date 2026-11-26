import { test, expect } from '@playwright/test';
import { uiLogin, USERS } from '../fixtures/helpers';

test.describe('Autenticación y roles', () => {
  test('login válido (Jefe) navega a la aplicación y muestra menú Usuarios', async ({ page }) => {
    await uiLogin(page, USERS.jefe.email);
    await expect(page.getByRole('link', { name: 'Usuarios' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Mis tareas' })).toBeVisible();
  });

  test('login válido (Encargado) navega sin menú Usuarios', async ({ page }) => {
    await uiLogin(page, USERS.encargado.email);
    await expect(page).not.toHaveURL(/\/login/);
    await expect(page.getByRole('link', { name: 'Usuarios' })).toHaveCount(0);
  });

  test('credenciales incorrectas muestran error 401', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Correo').fill(USERS.jefe.email);
    await page.getByLabel('Contraseña').fill('ClaveIncorrecta!');
    await page.getByRole('button', { name: /Ingresar/i }).click();
    await expect(page.getByRole('alert')).toContainText(/incorrectos/i);
    await expect(page).toHaveURL(/\/login/);
  });

  test('usuario inactivo autenticado correctamente pero deshabilitado muestra mensaje de inactividad', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Correo').fill(USERS.inactivo.email);
    await page.getByLabel('Contraseña').fill('Gestor2026!');
    await page.getByRole('button', { name: /Ingresar/i }).click();
    await expect(page.getByRole('alert')).toContainText(/inactivo/i);
  });

  test('ruta /users/new protegida para Encargado (chiefGuard)', async ({ page }) => {
    await uiLogin(page, USERS.encargado.email);
    await page.goto('/users/new');
    await expect(page).not.toHaveURL(/\/users\/new/);
  });

  test('ruta /users/new accesible para Jefe', async ({ page }) => {
    await uiLogin(page, USERS.jefe.email);
    await page.goto('/users/new');
    await expect(page.getByRole('heading', { name: 'Crear usuario' })).toBeVisible();
  });
});
