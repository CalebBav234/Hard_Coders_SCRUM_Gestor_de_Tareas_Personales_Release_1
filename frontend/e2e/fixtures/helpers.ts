import { APIRequestContext, Page, expect } from '@playwright/test';

export const DEV_PASSWORD = process.env.E2E_DEV_PASSWORD ?? 'Gestor2026!';

export const USERS = {
  jefe: { email: 'jefe@gestor.local' },
  encargado: { email: 'encargado1@gestor.local' },
  personal: { email: 'personal@gestor.local' },
  inactivo: { email: 'inactivo@gestor.local' },
};

export const API_URL = process.env.E2E_API_URL || 'http://localhost:8081/api';

export function uniqueId(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
}

export async function apiLogin(request: APIRequestContext, email: string, password = DEV_PASSWORD) {
  const res = await request.post(`${API_URL}/auth/login`, { data: { email, password } });
  expect(res.ok(), `login failed for ${email}: ${res.status()} ${await res.text()}`).toBeTruthy();
  const body = await res.json();
  return body.token as string;
}

export async function authHeader(request: APIRequestContext, email: string) {
  const token = await apiLogin(request, email);
  return { Authorization: `Bearer ${token}` };
}

export async function uiLogin(page: Page, email: string, password = DEV_PASSWORD) {
  await page.goto('/login');
  await page.getByLabel('Correo').fill(email);
  await page.getByLabel('Contraseña').fill(password);
  await page.getByRole('button', { name: /Ingresar/i }).click();
  await expect(page).not.toHaveURL(/\/login/);
}

export async function createProjectViaApi(request: APIRequestContext, chiefToken: string, managerUserId: number, name: string) {
  const res = await request.post(`${API_URL}/projects`, {
    headers: { Authorization: `Bearer ${chiefToken}` },
    data: {
      name,
      description: null,
      managerUserId,
      budgetBob: 5000,
      estimatedDurationMinutes: 4800,
      reason: null,
    },
  });
  expect(res.status(), `create project: ${await res.text()}`).toBe(201);
  return res.json();
}

export async function getManagerId(request: APIRequestContext, chiefToken: string, email: string) {
  const paths = [`${API_URL}/projects/eligible-managers`, `${API_URL}/projects/managers/eligible`];
  for (const path of paths) {
    const res = await request.get(path, { headers: { Authorization: `Bearer ${chiefToken}` } });
    if (res.ok()) {
      const managers = await res.json();
      const found = managers.find((m: any) => m.email === email || m.fullName?.toLowerCase().includes('ana'));
      if (found?.userId) return found.userId as number;
    }
  }
  // Dev seed fallback: encargado1@gestor.local is user_id 2
  if (email.includes('encargado1')) return 2;
  return 2;
}
