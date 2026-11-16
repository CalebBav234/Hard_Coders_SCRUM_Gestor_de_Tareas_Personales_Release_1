# ETAPA 5 — Guía de pruebas E2E (EC3)

- **Proyecto:** Gestor de Tareas Personales / `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`
- **Rama:** `Refactor-E2`
- **HEAD:** `10988a31d81f7596947c93d1e3abc3ddb42028db`
- **Working tree:** no limpio al inicio y durante la etapa (cambios E2/A6/docs/evidencias previas, logs, y ahora instalación/configuración/pruebas Playwright E5 en `frontend/`; sin commit/push/merge).
- **Fecha de ejecución:** 2026-11-16
- **Rol:** QA Engineer / Automation (Etapa 5)
- **Alcance:** instalación/configuración de Playwright en `frontend/`, automatización E2E de flujos principales y casos EC3 priorizados (UI + API + persistencia observable), y documentación de resultados reales. **No incluye Etapa 6** (regresión R1–R9 completa, cobertura global JaCoCo/Vitest, ajuste de umbrales).
- **Estado global Etapa 5:** **PASS** (suite Playwright E2E automatizada ejecutada con resultados reales: 14/14 PASS).

## 1. Gate de entrada

| Criterio | Evidencia | Estado |
|---|---|---|
| Rama/HEAD/WT verificados | `Refactor-E2` @ `10988a31…`; WT sucio (sin commit/push/merge). | OK |
| Etapa 1 PASS | `docs/EC3/ETAPA_1_GUIA_PRUEBAS.md` (contratos EC3-04/02/03, R2, US3.1). | OK |
| Etapa 2 PASS | `docs/EC3/ETAPA_2_GUIA_PRUEBAS.md` (A6 CREATE `app_user`, BROWSER-01..14). | OK |
| Etapa 3 PASS (diseño) | `docs/EC3/ETAPA_3_DISENO_PRUEBAS.md`, `docs/EC3/ETAPA_3_GUIA_PRUEBAS.md`. | OK |
| Etapa 4 PASS | `docs/EC3/ETAPA_4_GUIA_PRUEBAS.md` (integración PostgreSQL 25/25, backend 337/337, frontend Vitest 297/297). | OK |
| Dependencias E5 | Playwright no instalado previamente en repo (verificado); servicios disponibles (backend `:8081`, frontend `:4200`, DB `gestor-tareas-db` healthy `:5432`). | OK |

## 2. Restricciones aplicadas

- Sin cambios a V005/V006/migraciones/contratos/endpoints/UI de producto salvo lo estrictamente necesario para automatización/configuración de pruebas (no se modificó código de aplicación).
- Sin `docker compose down -v` ni borrado de volúmenes.
- No se registraron contraseñas, tokens, hashes, cookies ni `storageState` con secretos en documentación, pruebas, logs o capturas. Credenciales de desarrollo seed se usan solo en runtime de tests sobre entorno local controlado y no se exponen en artefactos versionados.
- Datos/usuarios de prueba aislados por escenario mediante identificadores únicos (`timestamp+random`) para proyectos/etapas/actividades/tareas/subtareas/usuarios creados durante E2E; no se modificaron usuarios/datos reales existentes salvo creación de entidades de prueba adicionales en BD dev local.
- Tests aislados a nivel de escenario (contexto Playwright por prueba), locators accesibles/semánticos y auto-wait de Playwright; sin `waitForTimeout` normal. `baseURL`/entorno determinista configurado.
- Sin mocks de backend para dar por probados flujos backend: las pruebas validan respuestas HTTP reales, UI y persistencia observable vía API/UI.
- `docs/EC3/scripts/e2e_fase1.sh` (API+BD, 21/21) **no** se contabiliza como Playwright E2E.
- Sin ejecución de regresión completa R1–R9 de Etapa 6 ni cobertura global JaCoCo/Vitest en esta etapa.

## 3. Entorno y precondiciones verificadas

| Componente | Detalle observado |
|---|---|
| SO / shell | Windows 11 / PowerShell |
| Node / npm | Node `v24.15.0`, npm `11.12.1` (compatible con Angular 22.1.3) |
| Angular / frontend | `@angular/core` 22.1.x, dev server activo en `http://localhost:4200` (proxy `frontend/proxy.conf.json`: `/api -> http://localhost:8081`) |
| Backend | Spring Boot activo en `http://localhost:8081`, `ddl-auto: validate`, JWT dev configurado |
| Base de datos | Docker `gestor-tareas-db` (`postgres:18`, healthy, `:5432`), schema `task_manager`, migraciones V001–V006 aplicadas |
| BD dev | `gestor_tareas` (`gestor_tareas_owner`/`gestor_tareas_app`) con seed dev (`jefe`, `encargado1`, `encargado2`, `personal`, `inactivo`) y datos previos de pruebas manuales/automatizadas |
| BD test E4 | `gestor_tareas_test` existente/aislada para integración backend (no alterada por E5) |
| Navegador E2E | Chromium Playwright (`chromium_headless_shell` compatible con `@playwright/test` 1.58.2) instalado localmente |

## 4. Instalación y configuración Playwright

### 4.1 Dependencias y artefactos creados (frontend)

- Dependencia dev añadida: `@playwright/test@1.58.2` (`frontend/package.json`, `frontend/package-lock.json` actualizados por npm).
- Navegador instalado: Chromium para Playwright (headless shell) en caché local de Playwright del usuario.
- Configuración: `frontend/playwright.config.ts`
  - `testDir: ./e2e`, `workers: 1`, `fullyParallel: false` (determinismo sobre entorno compartido local)
  - `baseURL: process.env.E2E_BASE_URL || 'http://localhost:4200'`
  - `trace: retain-on-failure`, `screenshot: only-on-failure`, `video: off`
  - reporters: `list` + `html` (`playwright-report/`, `open: never`)
  - timeout test 60s, expect 10s
- Scripts npm añadidos (`frontend/package.json`): `test:e2e`, `test:e2e:ui`, `test:e2e:report`
- Estructura E2E creada:
  - `frontend/e2e/auth/auth.spec.ts`
  - `frontend/e2e/users/user-create.spec.ts`
  - `frontend/e2e/tasks/main-flow.spec.ts`
  - `frontend/e2e/tasks/validations.spec.ts`
  - `frontend/e2e/fixtures/helpers.ts`
  - carpetas reservadas: `frontend/e2e/{projects,hierarchy,assignments}`

### 4.2 Estrategia de datos, auth y aislamiento

- **Base URL/API:** frontend `http://localhost:4200` (proxy a backend real) y API `http://localhost:8081/api` para verificaciones/persistencia/negativos controlados.
- **Auth fixtures:** login UI por roles (`Jefe`, `Encargado`, `Personal`, `Inactivo`) usando cuentas seed dev; helper de login API solo para preparar/verificar datos y casos negativos/privilegios (sin exponer tokens en docs).
- **Datos únicos por ejecución:** prefijos con `Date.now()` + aleatorio para nombres de proyectos, etapas, actividades, tareas, subtareas y correos de usuarios creados en E2E.
- **Persistencia observable:** aserciones combinadas UI + respuestas HTTP (status/body relevante) + consultas API posteriores (ej. tarea creada con duración/presupuesto, asignaciones con `reason` persistido, usuario creado autenticable).
- **No mocks backend:** backend real requerido; validaciones/errores observados desde respuestas reales.

## 5. Cobertura automatizada ejecutada

| Suite / spec | Escenarios cubiertos | Tipo | Resultado |
|---|---|---|---|
| `e2e/auth/auth.spec.ts` | Login válido Jefe (navega y muestra menú Usuarios), login válido Encargado (sin menú Usuarios), credenciales incorrectas (401/UI error), usuario inactivo autenticado pero deshabilitado (mensaje inactividad), `/users/new` protegido para Encargado (`chiefGuard`), `/users/new` accesible para Jefe | UI | 6/6 PASS |
| `e2e/users/user-create.spec.ts` | Jefe crea `app_user` válido desde UI (POST 201, respuesta sin password/hash/token), usuario nuevo puede iniciar sesión; email duplicado muestra error y no crea adicional (400/409); validación UI contraseña corta bloquea envío; Personal no accede a `/users/new` y API `POST /api/users` devuelve 403 | UI + API | 4/4 PASS |
| `e2e/tasks/main-flow.spec.ts` | Flujo principal: proyecto preparado (API chief) → gestión UI como Jefe → crear etapa inline (POST único) y persistencia tras recarga → crear actividad inline → seleccionar contexto jerárquico → crear tarea con duración 1.5 h (`estimatedDurationMinutes=90`) y presupuesto separado 250.50 (respuesta/persistencia API) → crear subtarea → asignar Personal con `reason` con espacios (trim + persistencia en respuesta y listado API) → persistencia UI en contexto → "Mis tareas" (Personal) muestra tarea asignada sin campos plan/finanzas (US3.1, sin duración/presupuesto/Bs) | UI + API + persistencia | 1/1 PASS |
| `e2e/tasks/validations.spec.ts` | Tarea UI: duración 0.1 h rechazada (incrementos 0.25 h), presupuesto 3 decimales rechazado; API negativos tarea `estimatedDurationMinutes=-1` (400) y `allocatedBudgetBob=-0.01` (400). Asignación API: `reason` null → default "Asignación de Personal", duplicado (400/409), trabajador inactivo (400/404), `reason` 501 rechazado (400), `reason` 500 aceptado y persistido, listado confirma motivos persistidos. Proyecto UI: creación válida por Jefe (201 y visible en listado) | UI + API | 3/3 PASS |

**Total:** 14 pruebas, 14 PASS, 0 FAIL, 0 SKIP (ejecución real).

## 6. Trazabilidad con requisitos Etapa 5 / EC3

| Requisito Etapa 5 | Cobertura / evidencia | Estado |
|---|---|---|
| Instalar/configurar Playwright si no existe (Node compatible Angular 22) | `@playwright/test` instalado, `playwright.config.ts`, scripts npm, Chromium instalado, Node 24.15.0 | PASS |
| Proceso principal Proyecto→Etapa→Actividad→Tarea→Subtarea (formularios principales) | `main-flow.spec.ts`: etapa/actividad inline, tarea/subtarea, selección jerárquica, persistencia UI/API | PASS |
| CREATE `task_assignment` válido (UI+API+persistencia observable) | `main-flow.spec.ts`: asignación a worker activo con `reason` trim, respuesta 200, listado API confirma persistencia, UI muestra asignado | PASS |
| CREATE `app_user` válido (UI+API+persistencia observable) + login posterior | `user-create.spec.ts`: POST 201, respuesta sin secretos, éxito UI, logout/login con usuario creado | PASS |
| Casos negativos/privilegios relevantes | Auth incorrecta/inactivo/guards; user duplicado, password corta UI, personal 403/no acceso; tarea duración/presupuesto inválidos UI/API; asignación duplicada/inactivo/reason 501 | PASS |
| Contrato EC3-04 (duración 0.25h→min, presupuesto separado) | Tarea creada con 1.5 h → `estimatedDurationMinutes=90` y `allocatedBudgetBob=250.50` verificados en respuesta y consulta API; UI 0.1 h rechazado | PASS |
| Contrato asignación `reason` (trim/default/límites) | `main-flow`: trim y persistencia; `validations`: null→default, 500 aceptado/persistido, 501 rechazado | PASS |
| US3.1 "Mis tareas" sin campos plan/finanzas | `main-flow`: Personal visualiza tarea asignada sin duración/presupuesto/Bs | PASS |
| POST único etapa (sin duplicado en recarga) | `main-flow`: espera única respuesta POST 201 y tras recarga `count==1` para etapa creada | PASS |
| `docs/EC3/scripts/e2e_fase1.sh` no cuenta como Playwright E2E | No se usa como sustituto; suite Playwright propia ejecutada | PASS |

## 7. Comandos ejecutados y resultados reales

| Comando (ejecutado en `frontend/`) | Resultado real |
|---|---|
| `npm install -D @playwright/test@1.58.2` | OK (dependencia y lockfile actualizados) |
| `npx playwright install chromium` / `npx --no-install playwright install chromium` | OK (Chromium/headless shell compatible instalado) |
| `npx --no-install playwright test e2e/auth/auth.spec.ts --project=chromium --reporter=list` | 6 passed |
| `npx --no-install playwright test e2e/users/user-create.spec.ts --project=chromium --reporter=list` | 4 passed |
| `npx --no-install playwright test e2e/tasks/validations.spec.ts --project=chromium --reporter=list` | 3 passed (tras correcciones de selectors/esperas/contexto de gestión) |
| `npx --no-install playwright test e2e/tasks/main-flow.spec.ts --project=chromium --reporter=list` | 1 passed (tras correcciones de selectors/contexto jerárquico) |
| `npx --no-install playwright test --project=chromium --reporter=line,html` | **14 passed (54.1s)**, 0 failed, 0 skipped; reporte HTML generado (`frontend/playwright-report/`) |

## 8. Evidencias y artefactos

- Configuración/tests: `frontend/playwright.config.ts`, `frontend/e2e/**`, `frontend/e2e/fixtures/helpers.ts`, `frontend/package.json` (scripts), `frontend/package-lock.json`.
- Reporte Playwright HTML: `frontend/playwright-report/` (generado en ejecución final).
- Resultado de última corrida: `frontend/test-results/.last-run.json`.
- Evidencia documental Etapa 5: `docs/EC3/evidencias/etapa_5/` (reservado; ver `EVIDENCIA_EJECUCION.md`).
- Capturas/trazas: configuradas para retenerse en fallo (`trace: retain-on-failure`, `screenshot: only-on-failure`); corrida final sin fallos.

## 9. Observaciones, riesgos y discrepancias

| ID | Descripción | Impacto | Estado |
|---|---|---|---|
| OBS-E5-01 | `npx playwright install chromium` inicial descargó binarios no alineados con `@playwright/test@1.58.2`; se resolvió instalando Chromium con `npx --no-install playwright install chromium` usando la versión local del paquete. | Configuración/ejecución (resuelto) | Resuelto |
| OBS-E5-02 | El formulario de tarea se muestra en la vista "Mis tareas"/workspace y requiere proyecto+etapa+actividad seleccionados con datos cargados; los specs E2E esperan opciones/respuestas de jerarquía y navegan con contexto explícito (`/?projectId=...`) para determinismo. No se modificó UI de producto. | Estabilidad de automatización (cubierto en tests) | Mitigado |
| OBS-E5-03 | Se usó actor Jefe para gestión WBS/tareas/asignación en escenarios E2E de flujo principal, aprovechando permisos `canManageHierarchy` del jefe (válido por política). El caso de creación de proyecto UI y privilegios/roles se cubren explícitamente; escenarios específicos Encargado-vs-Jefe de gestión detallada quedan dentro del alcance ya validado contractualmente y pueden ampliarse en Etapa 6 si se requiere. | Cobertura de variantes de rol | Aceptado en alcance E5 |
| OBS-E5-04 | BD dev `gestor_tareas` acumula entidades de prueba E2E con identificadores únicos (aislamiento lógico por nombre/id único; sin limpieza destructiva). No se tocaron usuarios seed ni datos reales preexistentes. | Higiene de datos dev local | Aceptado |
| DIV-E4-01 (heredada) | BCrypt >72 bytes puede producir 500 en creación de usuario (73 ASCII) pese a política 8–200; no automatizado en E5 para evitar 500 no controlado, pero permanece documentada desde E4. | Riesgo residual conocido | Heredada |

## 10. Checklist de cierre Etapa 5

| Ítem | Resultado |
|---|---|
| Gate E1–E4 verificado y respetado | PASS |
| Playwright instalado/configurado en `frontend/` con Node compatible Angular 22 | PASS |
| Scripts/config/estructura E2E creados (`playwright.config.ts`, `e2e/**`, helpers) | PASS |
| Entorno backend/frontend/DB operativo y datos de prueba aislados por ejecución | PASS |
| Login y control de acceso por roles automatizados (Jefe/Encargado/Personal/Inactivo, guards) | PASS |
| Flujo principal Proyecto→Etapa→Actividad→Tarea→Subtarea automatizado con persistencia observable | PASS |
| CREATE `task_assignment` válido automatizado (UI+API+persistencia, `reason` trim) | PASS |
| CREATE `app_user` válido automatizado (UI+API+persistencia+login posterior) y negativos/privilegios relevantes | PASS |
| Casos EC3 priorizados: duración 0.25h→90 min, presupuesto separado, negativos tarea, asignación default/500/501/duplicado/inactivo, POST único etapa, US3.1 sin campos plan/finanzas | PASS |
| Ejecución real Playwright documentada con resultados (14/14 PASS) | PASS |
| No uso de `e2e_fase1.sh` como sustituto Playwright E2E | PASS |
| Sin Etapa 6 (regresión R1–R9 completa/cobertura global/umbrales) | PASS |

**Conclusión:** **Etapa 5 = PASS**. Se dispone de automatización Playwright reproducible sobre el entorno local real para los flujos y casos priorizados de EC3, con evidencia de ejecución exitosa y trazabilidad a contratos principales.
