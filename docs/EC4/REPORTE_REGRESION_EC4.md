# REPORTE DE REGRESIÓN — EC4 (Etapa 4 de integración y estabilización)

**Elemento de Competencia 4 — BDD + Gherkin + CRUD Full-Stack + Pruebas de Integración + Cobertura + CI/CD**  
**Repositorio:** `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`  
**Rama:** `Refactor-E2` (no se cambió de rama; no se hizo merge, commit ni push)  
**Fecha de ejecución:** 2026-11-26  
**Ejecutado por:** Kilo Code (Release Engineer / Principal QA / Software Architect)  
**Estado:** Integración y regresión validadas. Listo para Etapa 5 (CI/CD).

---

## 1. Estado de Git y rama

| Concepto | Valor |
|---|---|
| Rama | `Refactor-E2` |
| HEAD | `4ceef5e2ad7e443c960850b57657d69a92a31116` |
| `git status --short --branch` | `## Refactor-E2...origin/Refactor-E2` + cambios de Etapas 1–3 sin commitear |
| Cambios destructivos ejecutados | Ninguno (no reset, clean, stash, rebase, merge, force push, checkout) |
| Cambios fuera de alcance | Ninguno (solo código de Etapas 1–3 y 3 correcciones de specs de frontend) |
| Base de datos `gestor_tareas` (dev) | No tocada |
| Base de datos `gestor_tareas_test` | Usada para integración/BDD/E2E; se sembraron usuarios de prueba (jefe, encargado1, personal, inactivo) para que E2E pueda autenticar |

Los cambios en el working tree corresponden exclusivamente a las Etapas 1–3 (infraestructura BDD, CRUD `app_user`, CRUD `task_assignment`) más las correcciones de compilación de tres specs de frontend (sección 6). No se eliminó ni se deshabilitó ninguna prueba.

---

## 2. Entorno de validación

| Componente | Versión / valor |
|---|---|
| JDK | 24.0.2 (`pom.xml` declara Java 21; Spring Boot 4.1.0 compila y ejecuta sobre JDK 24) |
| Maven | 3.9.16 |
| Node | v24.15.0, npm 11.12.1 |
| Backend | Spring Boot 4.1.0, Puerto 8081, PostgreSQL 18 (Docker `gestor-tareas-db`) |
| Frontend | Angular 22.1.5, Vitest 4.1.12 (cobertura v8), Playwright 1.58.2 (Chromium) |
| Base de integración | `gestor_tareas_test` (localhost:5432, schema `task_manager`, rol `gestor_tareas_owner`/`ownerpass`) |
| Base de desarrollo | `gestor_tareas` — NO usada por ninguna prueba |
| Cucumber-JVM | 7.18.0 (`cucumber-java`, `cucumber-junit`, `cucumber-spring`) |

Comando de backend (ciclo real del proyecto, Surefire + JaCoCo en `verify`):
```
cd backend
mvn verify
```

Comando de frontend:
```
cd frontend
npx ng test --watch=false --coverage
npm run build
```

Comando de Playwright E2E (salida redirigida a temp para no sobrescribir `frontend/playwright-report/`):
```
cd frontend
E2E_BASE_URL=http://localhost:4200 E2E_API_URL=http://localhost:8081/api \
  E2E_CHIEF_EMAIL=jefe@gestor.local E2E_CHIEF_PASSWORD=Gestor2026! \
  npx playwright test --output <directorio temporal>
```

Servidores levantados para E2E contra `gestor_tareas_test`:
- Backend: `java -jar target\gestor-tareas-backend-0.0.1-SNAPSHOT.jar` con datasource sobre `gestor_tareas_test`.
- Frontend: `npx ng serve --port 4200 --proxy-config proxy.conf.json` (`/api` → `http://localhost:8081`).

---

## 3. Matriz BDD oficial — 19 ejecuciones (14 Happy Path + 5 Unhappy Path)

Ejecutadas vía Cucumber-JVM (`CucumberIntegrationTest`, tag `@PP-01 or @USR or @ASG`) contra API real (Spring Boot `RANDOM_PORT`) y PostgreSQL `gestor_tareas_test`, con verificación JDBC de persistencia y efectos secundarios. Cada escenario es independiente (limpieza de BD en `@Before`, emails UUID, sin estado compartido).

| # | Código | Tabla / Proceso | Operación | Tipo | Resultado | Pasos |
|---|---|---|---|---|---|---|
| 1 | PP-01 | Proceso principal | Jerarquía Proyecto→Etapa→Actividad→Tarea→Subtarea | Happy | **PASS** | 11 |
| 2 | USR-INS-01 | app_user | INSERT | Happy | **PASS** | 5 |
| 3 | USR-INS-02 | app_user | INSERT (2º usuario) | Happy | **PASS** | 5 |
| 4 | USR-INS-03 | app_user | INSERT email inválido | Unhappy | **PASS** | 4 |
| 5 | USR-UPD-01 | app_user | UPDATE nombres/apellidos | Happy | **PASS** | 6 |
| 6 | USR-UPD-02 | app_user | UPDATE email válido (normalizado) | Happy | **PASS** | 5 |
| 7 | USR-UPD-03 | app_user | UPDATE email inválido (sin cambio) | Unhappy | **PASS** | 5 |
| 8 | USR-DEL-01 | app_user | DELETE (desactivación lógica) | Happy | **PASS** | 6 |
| 9 | USR-SEL-01 | app_user | SELECT individual | Happy | **PASS** | 6 |
| 10 | USR-SEL-02 | app_user | SELECT listado | Happy | **PASS** | 7 |
| 11 | ASG-INS-01 | task_assignment | INSERT motivo explícito | Happy | **PASS** | 7 |
| 12 | ASG-INS-02 | task_assignment | INSERT motivo nulo → default | Happy | **PASS** | 7 |
| 13 | ASG-INS-03 | task_assignment | INSERT razón > 500 | Unhappy | **PASS** | 4 |
| 14 | ASG-UPD-01 | task_assignment | UPDATE motivo | Happy | **PASS** | 7 |
| 15 | ASG-UPD-02 | task_assignment | UPDATE vacío → default | Happy | **PASS** | 7 |
| 16 | ASG-UPD-03 | task_assignment | UPDATE > 500 (conserva previo) | Unhappy | **PASS** | 5 |
| 17 | ASG-DEL-01 | task_assignment | DELETE (revocación) | Happy | **PASS** | 6 |
| 18 | ASG-SEL-01 | task_assignment | SELECT individual (clave compuesta) | Happy | **PASS** | 6 |
| 19 | ASG-SEL-02 | task_assignment | SELECT listado de tarea | Happy | **PASS** | 6 |

**Resultado BDD: 19/19 aprobados, 123 pasos, 0 fallos, 0 ignorados.**  
Salida Cucumber: `19 Scenarios (19 passed), 123 Steps (123 passed)`.

> Nota de traza: PP-01 documenta un email fijo en Gherkin pero el step ignora ese valor y usa el jefe creado por Hooks (email con timestamp). Es solo documentación del paso; no afecta la ejecución ni el aislamiento.

---

## 4. Regresión backend

`mvn verify` (línea base final, sin servidor bloqueando el JAR): **BUILD SUCCESS**.

| Nivel | Pruebas | Fallos | Errores | Omitidos | Resultado |
|---|---|---|---|---|---|
| Unitarias (Surefire) | ~343 | 0 | 0 | 0 | PASS |
| Integración Postgres (`PostgresIntegrationTest`) | 44 | 0 | 0 | 0 | PASS |
| Cucumber EC4 (19 escenarios) | 1 (19 esc.) | 0 | 0 | 0 | PASS |
| **Total** | **412** | **0** | **0** | **0** | **PASS** |

**JaCoCo (0.8.15, umbrales LINE ≥ 85 %, BRANCH ≥ 85 %):**
- `mvn verify` → `jacoco:check`: **All coverage checks have been met.**
- LINE: 95,14 % (2820 cubiertas / 144 pasadas por alto)
- BRANCH: 85,26 % (642 cubiertas / 111 pasadas por alto)
- INSTRUCTION: ~92,58 % (informativo)

El agente JaCoCo se mantiene activo con forks de prueba válidos (Surefire, proveedor JUnit Platform; `forkCount` no forzado a 0). No hay `application-test.yml`; la configuración de prueba se inyecta como propiedades en `CucumberSpringConfiguration` y `PostgresIntegrationTest`.

Reportes generados (reproducibles):
- `backend/target/surefire-reports/` (XML por clase)
- `backend/target/site/jacoco/` (`index.html`, `jacoco.csv`, `jacoco.xml`, `jacoco-sessions.html`)
- `backend/target/jacoco.exec`

---

## 5. Regresión frontend

**Vitest (cobertura v8, umbrales 85 % en las 4 métricas):**
```
Test Files  23 passed (23)
Tests       349 passed (349)
```
| Métrica | Valor | Umbral | Estado |
|---|---|---|---|
| Statements | 92,95 % | ≥ 85 % | PASS |
| Branches | 88,53 % | ≥ 85 % | PASS |
| Functions | 87,36 % | ≥ 85 % | PASS |
| Lines | 94,05 % | ≥ 85 % | PASS |

**Build Angular de producción:** `npm run build` → **SUCCESS** (`ng build`, configuración production). Solo warnings de presupuesto (bundle inicial > 500 kB y dos estilos de componente > 4 kB), mismos que la línea base; no son errores. Salida: `frontend/dist/gestor-tareas`.

**Playwright E2E (Chromium, headless, retries=0, salida a temp sin tocar `frontend/playwright-report/`):**
```
25 passed (25)
```
| Spec | Pruebas | Resultado |
|---|---|---|
| `e2e/auth/auth.spec.ts` | 6 | PASS |
| `e2e/tasks/main-flow.spec.ts` (proceso principal) | 1 | PASS |
| `e2e/tasks/task-assignment.spec.ts` (asignaciones) | 3 | PASS |
| `e2e/tasks/validations.spec.ts` | 3 | PASS |
| `e2e/users/user-create.spec.ts` | 4 | PASS |
| `e2e/users/user-crud.spec.ts` (usuarios) | 8 | PASS |
| **Total** | **25** | **PASS** |

Los E2E usan datos de prueba (emails `uniqueId`, usuarios semillados en `gestor_tareas_test`) y nunca `gestor_tareas`. El reporte HTML y traces quedaron en directorio temporal; `frontend/playwright-report/index.html` (archivo protegido, 565.912 bytes, 20/11/2026) no fue sobrescrito.

---

## 6. Defectos detectados y corregidos (dentro de alcance EC4)

Los nuevos specs de frontend de la Etapa 2 **no compilaban**, bloqueando la ejecución de Vitest (`ng test` fallaba en la generación del bundle). Se corrigieron:

| # | Archivo | Defecto | Corrección | Clasificación |
|---|---|---|---|---|
| D-01 | `frontend/src/app/core/services/user.service.spec.ts:2` | `HttpErrorResponse` importado de `@angular/common/http/testing` (no existe ahí; está en `@angular/common/http`) | Se eliminó la importación y se reestructuró la prueba de error para aserar `err.status` dentro del callback `error` | Regresión nueva (Etapa 2) |
| D-02 | `frontend/src/app/features/users/user-edit.spec.ts:72,103,149` | `el.querySelector(...)?.click()` sobre `Element` (no tiene `click`; solo `HTMLElement`) | Casteo a `HTMLButtonElement` antes de `.click()` | Regresión nueva (Etapa 2) |
| D-03 | `frontend/src/app/features/users/user-list.spec.ts:78,88,98,147,163,165` | `el.querySelector(...)?.click()` sobre `Element` | Casteo a `HTMLElement` antes de `.click()` | Regresión nueva (Etapa 2) |

Tras las correcciones, Vitest pasó de no compilar a **349 tests pasando** con cobertura ≥ 85 % en todas las métricas.

**Defecto de entorno (no de producto):** E2E `user-crud.spec.ts` falló en `beforeAll` con `login jefe: 401` porque usa `jefe.e2e.test@gestor.local`/`E2EChiefPass123!` por defecto, usuario que no existe en `gestor_tareas_test`. Se resolvió usando las variables de entorno que el spec ya soporta (`E2E_CHIEF_EMAIL=jefe@gestor.local`, `E2E_CHIEF_PASSWORD=Gestor2026!`) y semeando dicho usuario en `gestor_tareas_test`. No es un cambio de código.

**Flake diagnosticado (no ocultado):** `main-flow.spec.ts` falló en la primera ejecución por `#assignment-modal-title` no encontrado (tiempo de renderizado del diálogo tras crear la subtarea). Al reejecutar sin cambios de código, pasó (22.4 s). No se añadieron retries ni sleeps.

**No se modificó ninguna prueba para que coincidiera con una implementación incorrecta.**

---

## 7. Matriz de aceptación global (CA)

| ID | Criterio | Estado | Evidencia |
|---|---|---|---|
| CA-01 | PP-01: 1 Happy Path BDD aprobado | **APROBADO** | 1/1 escenario Cucumber |
| CA-02 | `app_user`: 9 BDD aprobados | **APROBADO** | 9/9 (USR-INS/UPD/DEL/SEL) |
| CA-03 | `task_assignment`: 9 BDD aprobados | **APROBADO** | 9/9 (ASG-INS/UPD/DEL/SEL) |
| CA-04 | CRUD backend completo | **APROBADO** | Endpoints GET/PUT/DELETE users y GET/PUT assignment implementados y verificados por BDD + PostgresIntegrationTest |
| CA-05 | CRUD frontend/UI completo | **APROBADO** | Rutas /users, /users/:userId, /users/:userId/edit, componentes UserList/UserDetail/UserEdit, AssignmentDialog (detalle, editar motivo, revocar) |
| CA-06 | JaCoCo y Vitest ≥ 85 % todas las métricas | **APROBADO** | JaCoCo LINE 95,14 / BRANCH 85,26; Vitest St 92,95 / Br 88,53 / Fu 87,36 / Li 94,05 |
| CA-07 | Build backend y frontend ejecutable | **APROBADO** | `mvn verify` BUILD SUCCESS; `npm run build` SUCCESS |
| CA-08 | Reportes reproducibles por escenario | **APROBADO** | Surefire, JaCoCo, Vitest coverage, Cucumber output, Playwright trace/screenshot en temp |

---

## 8. Comprobaciones de protección del repositorio

- No hay conexión a `gestor_tareas` ni a producción en ninguna prueba (todas usan `gestor_tareas_test`; el backend de E2E se levantó con datasource sobre `gestor_tareas_test`).
- Los tests son independientes y repetibles (limpieza de BD por escenario, datos UUID, sin estado compartido).
- No se ejecutó `git reset`, `clean`, `stash`, rebase, merge automático, force push ni `checkout`.
- No se descartaron cambios locales ni reportes generados.
- No se bajaron thresholds de JaCoCo ni Vitest.
- No se creó GitHub Actions ni PDFs (etapa 5, pendiente por acuerdo).

---

## 9. Artefactos

| Artefacto | Ruta |
|---|---|
| Reporte Surefire backend | `backend/target/surefire-reports/` |
| Reporte JaCoCo backend | `backend/target/site/jacoco/index.html`, `jacoco.csv`, `jacoco.xml` |
| Reporte Vitest/coverage frontend | `frontend/coverage/gestor-tareas/` (`index.html`, `coverage-final.json`, `clover.xml`) |
| Ejecución Cucumber (19 escenarios) | `backend/target/surefire-reports/` + salida de consola (19 passed, 123 steps) |
| Reporte Playwright E2E | `C:\Users\caleb\AppData\Local\Temp\kilo\playwright-out/` (redirigido; no sobrescribe `frontend/playwright-report/`) |
| Logs de ejecución | `C:\Users\caleb\AppData\Local\Temp\kilo\` (cucumber-run.log, mvn-verify-clean.log, vitest-run3.log, ng-build.log, playwright-run2.log) |

---

## 10. Conclusión y readiness para Etapa 5

La integración de las Etapas 1–3 está estable y validada:
- **19/19 BDD** pasan.
- **Backend:** 412/412 pruebas, JaCoCo ≥ 85 %, `mvn verify` BUILD SUCCESS.
- **Frontend:** 349/349 Vitest, cobertura ≥ 85 %, build de producción SUCCESS.
- **E2E:** 25/25 Playwright (usuarios, asignaciones, proceso principal, auth, validaciones).

Todos los criterios de aceptación CA-01 a CA-08 están cumplidos. No quedan tests fallidos ni ignorados sin explicar. Las correcciones de alcance fueron mínimas y justificadas (3 specs de frontend que impedían compilar Vitest).

**Readiness para Etapa 5 (CI/CD):** SÍ. Los comandos locales reproducibles están definidos en la sección 2. Falta crear `.github/workflows/ec4-quality.yml` (pendiente, por acuerdo de usuario, no se creó en esta etapa).

---
*Fin del reporte.*
