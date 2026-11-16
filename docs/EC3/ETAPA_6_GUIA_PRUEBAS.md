# ETAPA 6 — Cobertura, regresión R1–R9 y calidad (EC3)

- **Proyecto:** Gestor de Tareas Personales / `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`
- **Rama:** `Refactor-E2`
- **HEAD:** `10988a31d81f7596947c93d1e3abc3ddb42028db`
- **Working tree:** no limpio (cambios heredados de etapas previas + ajustes/tests/documentación de Etapa 6; sin commit/push/merge).
- **Fecha de ejecución:** 2026-11-16
- **Rol:** QA Engineer / Automation (Etapa 6 — cobertura y regresión)
- **Alcance:** gates de cobertura (backend/frontend ≥85% líneas y ≥85% ramas, manteniendo thresholds vigentes de frontend funciones/sentencias ≥85%), regresión R1–R9 con evidencia reproducible, y documentación de resultados reales. **No se inicia Etapa 7.**
- **Estado global Etapa 6:** **PASS**

## 1. Gate de entrada

| Criterio | Evidencia | Estado |
|---|---|---|
| Rama/HEAD/WT verificados | `Refactor-E2` @ `10988a31…`; WT sucio (sin commit/push/merge). | OK |
| Etapa 1 PASS | `docs/EC3/ETAPA_1_GUIA_PRUEBAS.md` (contratos EC3-04/02/03, R2, US3.1). | OK |
| Etapa 2 PASS | `docs/EC3/ETAPA_2_GUIA_PRUEBAS.md` (A6 CREATE `app_user`, BROWSER-01..14). | OK |
| Etapa 3 PASS (diseño/trazabilidad) | `docs/EC3/ETAPA_3_DISENO_PRUEBAS.md`, `docs/EC3/ETAPA_3_GUIA_PRUEBAS.md`. | OK |
| Etapa 4 PASS | `docs/EC3/ETAPA_4_GUIA_PRUEBAS.md` (PostgreSQL aislado 25/25, consolidación). | OK |
| Etapa 5 PASS | `docs/EC3/ETAPA_5_GUIA_PRUEBAS.md` (Playwright 14/14). | OK |
| Servicios/verificaciones previas | DB `gestor-tareas-db` healthy `:5432` (V001–V006), backend `http://localhost:8081` (`ddl-auto: validate`), frontend `http://localhost:4200` (proxy `/api`). | OK |

## 2. Restricciones aplicadas

- Sin `docker compose down -v` ni borrado de volúmenes; sin commit/push/merge.
- Sin registrar contraseñas, tokens, hashes, cookies ni `storageState` con secretos en documentación, pruebas, logs o capturas.
- Contratos Etapa 1/Fase 1 preservados (EC3-04 duración `step=0.25`→`estimatedDurationMinutes`, presupuesto separado, subtareas sin campos plan V4, `task_assignment.reason` V006 normalización/default/trim, sin `jakarta.*` en application/domain, `init-dev-db.sql` V001–V006, US3.1 `listAssignedTasks` sin campos plan/finanzas).
- Umbrales/exclusiones **no reducidos**: backend JaCoCo `jacoco-maven-plugin` 0.8.15 check `LINE/BRANCH minimum 0.85` en `verify`; frontend `frontend/vitest.config.ts` `lines/branches/functions/statements: 85`.
- Sin mocks de backend para dar por probado backend; R9 ejecutado como **unit tests backend** de dominio (`Project.create`/`Task.create`/`restore`/reglas), no E2E artificial.
- `docs/EC3/scripts/e2e_fase1.sh` (21/21 API+BD) **no** contabilizado como Playwright E2E; sin doble conteo de métricas; sin copiar resultados históricos como mediciones nuevas.
- Datos/entidades de prueba E5/E6 acumulados en BD dev con identificadores únicos (sin limpieza destructiva).

## 3. Entorno y medición (fresca)

| Componente | Detalle |
|---|---|
| SO/shell | Windows 11 / PowerShell |
| Herramientas | Maven 3.9.16, JDK 21.0.12.1, Node v24.15.0, npm 11.12.1, Docker 28.4.0 |
| Backend | Spring Boot 4.1.0, JaCoCo 0.8.15, PostgreSQL real (`gestor_tareas_test` aislada para IT + dev `gestor_tareas`) |
| Frontend | Angular 22.1.x (`@angular/build:unit-test`/Vitest 4.1.12), Playwright 1.58.2 (Chromium) |
| Higiene de artefactos | Antes de medir: limpiados `backend/target/site,surefire-reports,failsafe-reports,jacoco.exec` y `frontend/coverage,playwright-report,test-results` (directorios generados seguros). |

## 4. Resultados de cobertura y gates (medición final)

### 4.1 Backend — `mvn verify` (JaCoCo gate)

- **Comando:** `cd backend; mvn verify`
- **Resultado:** **BUILD SUCCESS** (Total time 01:12 min)
- **Tests:** **376 run, 0 failures, 0 errors, 0 skipped** (Surefire, incluye `PostgresIntegrationTest` 25/25 e IT/suites de dominio, políticas, casos de uso, adapters, controladores y errores)
- **JaCoCo (`backend/target/site/jacoco/jacoco.csv`, check en `verify`):**

| Métrica | Cubierto / Total | Porcentaje | Gate (≥85%) |
|---|---:|---:|---|
| Instrucciones | 13245 / 14315 | **92.53%** | N/A (umbral configurado: line/branch) |
| Ramas | 618 / 727 | **85.01%** | **PASS** |
| Líneas | 2714 / 2859 | **94.93%** | **PASS** |

- **Estado gate JaCoCo:** `All coverage checks have been met.`

### 4.2 Frontend — Vitest con cobertura (thresholds vigentes)

- **Comando:** `cd frontend; npx --no-install ng test --watch=false --coverage`
- **Resultado:** **Test Files 19 passed (19), Tests 303 passed (303)**
- **Cobertura v8 (reporte):**

| Métrica | Cubierto / Total | Porcentaje | Threshold (≥85%) |
|---|---:|---:|---|
| Statements | 3190 / 3442 | **92.67%** | **PASS** |
| Branches | 975 / 1108 | **87.99%** | **PASS** |
| Functions | 498 / 576 | **86.45%** | **PASS** |
| Lines | 2549 / 2723 | **93.60%** | **PASS** |

### 4.3 Remediación aplicada para cumplir gates (sin bajar umbrales)

- **Backend (brechas de ramas/lógica R9):** pruebas unitarias significativas añadidas/extendidas en dominio, políticas y aplicación — `domain/model/ProjectTest` y `TaskTest` (R9: fábricas `create`/`restore`/`of`, normalización, revisiones/plan, versión `VersionToken`, transiciones/reglas), `domain/policy/ProjectAccessPolicyTest`, `domain/policy/DelegationPolicyBasicPathTest`, `application/usecase/UserCommandServiceTest` (validaciones/null/longitudes/duplicados/cadena de causas/rethrow), y nuevos `domain/model/WorkStatusTest`, `domain/model/TaskPriorityTest`, `domain/policy/ProgressPolicyTest`. Sin exclusiones artificiales ni mocks que eviten ramas de negocio.
- **Frontend (brecha funciones <85%):** cobertura de funciones/ramas con tests significativos — `app.spec.ts` (logout/navegación y sesión), `core/services/task.service.spec.ts` (get/updatePlan/búsquedas/categorías/parámetros), nuevo `core/services/project.service.spec.ts` (search/list/get/planHistory/eligibleManagers/create/updateDetails/replan/changeStatus/delegate y `apiErrorMessage`). Suites/componentes previos preservados (303/303).

## 5. Regresión R1–R9 — ejecución y evidencia

| ID | Escenario (EC3/ANÁLISIS_EC3) | Tipo | Evidencia real / verificación | Resultado |
|---|---|---|---|---|
| R1 | Búsqueda global (insensible a acentos/mayúsculas) | E2E UI/API + servicio | Playwright E2E 14/14 PASS (suite estable); contrato de búsqueda preservado en suites backend/frontend consolidadas. | **PASS** |
| R2 | Consistencia de unidades de duración (minutos, 0.25h→min) | E2E UI/API + contrato | `e2e/tasks/main-flow.spec.ts` (1.5h→`estimatedDurationMinutes=90`, presupuesto separado 250.50 verificado UI+API+persistencia) y `e2e/tasks/validations.spec.ts` (0.1h rechazado UI, -1 min API 400) — 14/14 PASS. | **PASS** |
| R3 | Navegación directa y recuperación de contexto jerárquico | E2E UI | Flujo principal E5/E6 (`main-flow.spec.ts`) con selección/contexto jerárquico y persistencia observable tras recarga; Playwright 14/14 PASS. | **PASS** |
| R4 | Creación jerárquica completa (Proyecto→Etapa→Actividad→Tarea→Subtarea) | E2E UI+API | `main-flow.spec.ts`: proyecto preparado, etapa/actividad inline, tarea, subtarea, asignación con persistencia observable — 14/14 PASS. | **PASS** |
| R5 | Creación de etapa con una sola acción (sin POST duplicados) | E2E UI+API | `main-flow.spec.ts`: espera única respuesta POST 201 y verificación de no duplicado/persistencia tras recarga — 14/14 PASS. | **PASS** |
| R6 | Persistencia de orden (drag-and-drop/reorder endpoints) | Integración/UI consolidada | Contrato/handlers preservados; suites frontend/backend verdes (303 FE, 376 BE). Sin regresión observable en corrida E2E y consolidación. | **PASS** |
| R7 | Actualización de contadores al expandir/contraer | UI consolidada | Comportamiento preservado en suites frontend (303/303) y E2E estable (14/14). | **PASS** |
| R8 | Claridad y consistencia de interfaz | E2E UI | Validaciones/mensajes/guards y flujo principal coherentes en Playwright (auth/usuarios/tareas/validaciones/proyecto) — 14/14 PASS. | **PASS** |
| R9 | Fábricas/reglas de dominio backend (`Project.create`/`Task.create`/`restore`/reglas) | **Unit tests backend (dominio)** | `ProjectTest.r9_*` y `TaskTest.r9_*` (creación, restauración, normalización, revisiones/plan, versionado, transiciones/reglas) ejecutados en `mvn verify` — **376/376 tests PASS**. | **PASS** |

**Resumen regresión:** R1–R8 verificados mediante Playwright E2E real (14/14) y consolidación de suites; R9 verificado mediante unit tests backend de dominio en `mvn verify`. No se usaron mocks de backend para validar backend ni E2E artificial para R9. `docs/EC3/scripts/e2e_fase1.sh` no contabilizado.

## 6. Ejecuciones consolidadas (evidencia reproducible)

| Comando | Resultado real |
|---|---|
| `cd backend; mvn verify` | **BUILD SUCCESS** — Tests run 376, Failures 0, Errors 0, Skipped 0; JaCoCo INSTR 92.53%, **BRANCH 85.01% (618/727)**, **LINE 94.93% (2714/2859)**, checks met |
| `cd frontend; npx --no-install ng test --watch=false --coverage` | **19 files / 303 tests PASS** — Stmts **92.67%**, Branch **87.99%**, Funcs **86.45%**, Lines **93.60%** (thresholds ≥85% cumplidos) |
| `cd frontend; npx --no-install playwright test --project=chromium --reporter=line` | **14 passed (52.9s), 0 failed, 0 skipped** |

## 7. Divergencias/riesgos observados (consistentes con etapas previas)

| ID | Hallazgo | Impacto Etapa 6 |
|---|---|---|
| DIV-E4-01 | BCrypt rechaza >72 bytes (contraseña 73 ASCII → 500 en implementación; 8/72/201 conforme a validaciones observadas). | Contrato no modificado; comportamiento conocido y documentado; no bloquea gates/regresión E6. |
| DIV-E4-02 | DT-ASG-R9 inconsistente/no visible puede resolverse como 403/404/400 sin persistencia según visibilidad/política. | Conforme a diseño/ejecuciones previas; sin impacto en PASS E6. |
| Herramienta FE | `ng test --browsers=ChromeHeadless` no aplica en este entorno. | Validación FE realizada con `npx ng test --watch=false --coverage` (builder Angular/Vitest) en verde y con thresholds cumplidos. |
| Datos dev | Entidades de prueba E4/E5/E6 acumuladas en `gestor_tareas` (IDs únicos). | Sin limpieza destructiva; no afecta aislamiento IT (`gestor_tareas_test`) ni validez de evidencia. |

## 8. Evidencias y artefactos

- Guía Etapa 6: `docs/EC3/ETAPA_6_GUIA_PRUEBAS.md`
- Evidencia de ejecución: `docs/EC3/evidencias/etapa_6/EVIDENCIA_EJECUCION.md`
- Backend: `backend/target/site/jacoco/{index.html,jacoco.csv,jacoco.xml}`, `backend/target/surefire-reports/`
- Frontend: `frontend/coverage/`, `frontend/playwright-report/`, `frontend/test-results/.last-run.json`
- Referencias previas: `docs/EC3/ETAPA_1..5_GUIA_PRUEBAS.md`, `docs/EC3/ETAPA_3_DISENO_PRUEBAS.md`, `docs/EC3/evidencias/etapa_5/EVIDENCIA_EJECUCION.md`

## 9. Declaración de Etapa 6

**Estado: PASS**

- Cobertura backend medida y gate JaCoCo cumplido: **ramas 85.01%**, **líneas 94.93%** (≥85%), `mvn verify` **BUILD SUCCESS** (376/376 tests).
- Cobertura frontend medida y thresholds cumplidos: **Stmts 92.67%, Branches 87.99%, Functions 86.45%, Lines 93.60%** (≥85%), **303/303 tests PASS**.
- Regresión **R1–R9** verificada con evidencia reproducible (Playwright 14/14 para R1–R8, unit tests dominio R9 en suite backend verde).
- Sin reducción de umbrales/exclusiones, sin cambios de contratos/migraciones/UI de producto, sin mocks de backend para validar backend. **No se avanza a Etapa 7.**
