# Evidencia de ejecución — Etapa 6 (EC3)

## Fecha y entorno
- Fecha: 2026-11-16
- Rama/HEAD: `Refactor-E2` @ `10988a31d81f7596947c93d1e3abc3ddb42028db`
- SO/shell: Windows 11 / PowerShell
- Backend: Java/JDK 21.0.12.1, Maven 3.9.16, Spring Boot 4.1.0, JaCoCo 0.8.15
- Frontend: Node v24.15.0, npm 11.12.1, Angular 22.1.x, Vitest 4.1.12, Playwright 1.58.2 (Chromium headless shell)
- Servicios: PostgreSQL `gestor-tareas-db` (`postgres:18`, healthy, `:5432`, dev DB `gestor_tareas`, test DB `gestor_tareas_test`), backend `http://localhost:8081` (`ddl-auto: validate`), frontend `http://localhost:4200` (proxy `/api -> :8081`)

## Mediciones y comandos ejecutados (reales, artefactos limpiados antes de medir)

### 1) Backend — `mvn verify` (tests + JaCoCo gate)
- Comando: `cd backend; mvn verify`
- Resultado: **BUILD SUCCESS** (`Total time: 01:12 min`)
- Tests: **Tests run: 376, Failures: 0, Errors: 0, Skipped: 0** (Surefire + integración PostgreSQL `PostgresIntegrationTest` 25/25 incluida)
- JaCoCo report/check (`backend/target/site/jacoco/jacoco.csv`): **INSTRUCTION 92.53% (13245/14315), BRANCH 85.01% (618/727), LINE 94.93% (2714/2859)** — `All coverage checks have been met.`

### 2) Frontend — Vitest + cobertura (thresholds vigentes)
- Comando: `cd frontend; npx --no-install ng test --watch=false --coverage`
- Resultado: **Test Files 19 passed (19), Tests 303 passed (303)**
- Cobertura v8: **Statements 92.67% (3190/3442), Branches 87.99% (975/1108), Functions 86.45% (498/576), Lines 93.60% (2549/2723)** — cumple thresholds `lines/branches/functions/statements >= 85`

### 3) Frontend — Playwright E2E (regresión UI/API R1–R8)
- Comando: `cd frontend; npx --no-install playwright test --project=chromium --reporter=line`
- Resultado: **14 passed (52.9s), 0 failed, 0 skipped**

## Cambios de soporte a calidad (sin bajar thresholds ni exclusiones)
- Backend (tests dominio/políticas/aplicación, R9 y ramas): `ProjectTest`, `TaskTest` (fábricas `create`/`restore`/reglas/versionado), `ProjectAccessPolicyTest`, `DelegationPolicyBasicPathTest`, `UserCommandServiceTest`, nuevos `WorkStatusTest`, `TaskPriorityTest`, `ProgressPolicyTest`.
- Frontend (tests unitarios cobertura funciones/ramas): `app.spec.ts` (logout/navegación), `core/services/task.service.spec.ts` (get/plan/búsquedas/categorías), nuevo `core/services/project.service.spec.ts` (CRUD/plan/estado/delegación/errores).
- Sin cambios a migraciones (V001–V006), contratos/endpoints/UI de producto, ni umbrales JaCoCo/Vitest.

## Notas
- Trazas de error esperadas durante integración (p.ej. validaciones/BCrypt frontera controlada en suite existente) no constituyen fallos; `mvn verify` y suites reportan verde.
- `docs/EC3/scripts/e2e_fase1.sh` no se contabiliza como Playwright E2E.
- No se ejecutó `docker compose down -v` ni se borraron volúmenes; no se registraron secretos.

## Capturas browser (agregadas 2026-11-16)
Ubicación: docs/EC3/evidencias/etapa_6/browser/
- E6-01-login-page.png .. E6-10-validacion-presupuesto-decimales.png (set representativo equivalente a E5: login, error credenciales, home Jefe/Usuarios, /users/new Jefe, Encargado sin acceso, WBS/estructura, formulario etapa inline, validación duración 0.25 h, flujo jerárquico/progreso, validación presupuesto decimales).

Evidencia visual complementaria a la regresión E6 (Playwright 14/14, Vitest 303/303, JaCoCo 85.01% ramas/94.93% líneas). Tomadas contra entorno local real; sin exposición de secretos.
