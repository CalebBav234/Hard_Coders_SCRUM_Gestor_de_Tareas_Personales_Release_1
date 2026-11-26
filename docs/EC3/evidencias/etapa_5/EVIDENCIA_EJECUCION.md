# Evidencia de ejecución — Etapa 5 (EC3)

## Ejecución automatizada Playwright (real)

- Fecha: 2026-11-16
- Ubicación: `frontend/`
- Comando final: `npx --no-install playwright test --project=chromium --reporter=line,html`
- Resultado: **14 passed, 0 failed, 0 skipped** (54.1s)

## Suites ejecutadas

1. `e2e/auth/auth.spec.ts` — 6/6 PASS
   - login Jefe con menú Usuarios
   - login Encargado sin menú Usuarios
   - credenciales incorrectas (401/UI)
   - usuario inactivo (mensaje de inactividad)
   - `/users/new` bloqueada para Encargado (chiefGuard)
   - `/users/new` accesible para Jefe
2. `e2e/tasks/main-flow.spec.ts` — 1/1 PASS
   - flujo Proyecto→Etapa→Actividad→Tarea→Subtarea→Asignación
   - tarea 1.5 h → `estimatedDurationMinutes=90`, presupuesto `250.50` (UI+API+persistencia)
   - subtarea creada
   - asignación con `reason` trim/persistido (UI+API+listado)
   - POST único etapa y persistencia observable
   - "Mis tareas" (Personal) sin campos plan/finanzas
3. `e2e/tasks/validations.spec.ts` — 3/3 PASS
   - UI tarea: duración 0.1 h rechazada (step 0.25), presupuesto 3 decimales rechazado
   - API tarea: `-1` duración y `-0.01` presupuesto rechazados (400)
   - API asignación: default `reason`, duplicado, inactivo, 501 rechazado, 500 aceptado/persistido, listado verificado
   - UI proyecto: creación válida por Jefe (201)
4. `e2e/users/user-create.spec.ts` — 4/4 PASS
   - Jefe crea usuario válido (201, sin secretos en respuesta) y login posterior
   - email duplicado rechazado (400/409)
   - contraseña corta bloqueada en UI
   - Personal sin acceso `/users/new` y 403 en API

## Artefactos generados

- `frontend/playwright-report/index.html` (reporte HTML de la corrida final)
- `frontend/test-results/.last-run.json`
- Configuración y código de pruebas: `frontend/playwright.config.ts`, `frontend/e2e/**`
- Guía de etapa: `docs/EC3/ETAPA_5_GUIA_PRUEBAS.md`

## Notas

- Corrida contra backend real `:8081`, frontend `:4200` (proxy), PostgreSQL `gestor-tareas-db` `:5432`.
- Datos de prueba creados con identificadores únicos por ejecución; sin exposición de secretos.
- `docs/EC3/scripts/e2e_fase1.sh` no utilizado como sustituto de Playwright E2E.

## Capturas browser (agregadas 2026-11-16)
Ubicación: docs/EC3/evidencias/etapa_5/browser/
- E5-01-login-page.png — pantalla de login.
- E5-02-login-credenciales-incorrectas.png — credenciales incorrectas (alerta UI 401).
- E5-03-home-jefe-usuarios-menu.png — home Jefe con menú Usuarios.
- E5-04-users-new-jefe.png — /users/new accesible para Jefe.
- E5-05-encargado-sin-acceso-users.png — Encargado redirigido sin acceso a /users/new.
- E5-06-wbs-estructura-proyecto.png — panel WBS/estructura del proyecto.
- E5-07-formulario-nueva-etapa-inline.png — formulario inline nueva etapa.
- E5-08-validacion-duracion-025.png — duración 0.1 h rechazada (múltiplos 0.25 h).
- E5-09-flujo-principal-wbs-tareas.png — jerarquía WBS/progreso/tareas (flujo principal).
- E5-10-validacion-presupuesto-decimales.png — presupuesto 1.234 rechazado (>2 decimales).

Capturas tomadas contra http://localhost:4200 (backend :8081, PostgreSQL :5432) con sesión de usuarios seed dev; sin exposición de secretos.
