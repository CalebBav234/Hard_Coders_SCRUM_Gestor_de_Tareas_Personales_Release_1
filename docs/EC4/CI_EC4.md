# CI EC4 — Documentación del Pipeline (Etapa 5)

**Repositorio:** `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`  
**Archivo:** `.github/workflows/ec4-quality.yml`  
**Rama objetivo:** `Refactor-E2`  
**Estado:** Workflow definido y validado sintácticamente localmente. No se ha disparado en GitHub (no hay integración de repositorio remoto en esta sesión). No es un run real de GitHub Actions.

---

## 1. Arquitectura del pipeline

El pipeline es puramente de **Integración Continua (CI)** y **quality gates**. No hay CD, ni despliegue, ni publicación de imágenes.

```
push / pull_request a Refactor-E2  ──►  [backend]  ─┐
                                                     ├─► [quality-gate] (falla si algún job falla)
pull_request a Refactor-E2 ───────────────────────►  [frontend] ─┤
workflow_dispatch ─────────────────────────────────►  [e2e]    ─┘
```

### Jobs

| Job | Runs-on | Needs | Propósito |
|-----|---------|-------|-----------|
| `backend` | ubuntu-latest | – | Java 21 + Maven. Service container PostgreSQL 18. Crea `gestor_tareas_test`, aplica V001–V006, ejecuta `mvn -B verify` (unit + integración + Cucumber EC4 + JaCoCo check). |
| `frontend` | ubuntu-latest | – | Node 24 + npm. `npm ci`, Vitest con cobertura (umbral 85%), build Angular de producción. |
| `e2e` | ubuntu-latest | backend, frontend | Service container PostgreSQL 18 + backend (puerto 8081) + frontend (puerto 4200). Playwright Chromium contra `gestor_tareas_test`. |
| `quality-gate` | ubuntu-latest | backend, frontend, e2e | Consolidación: falla si cualquier job requerido falló o fue cancelado. No usa `always()` para enmascarar fallos. |

### Eventos y concurrencia

- `push` a `Refactor-E2`
- `pull_request` con destino `Refactor-E2`
- `workflow_dispatch`
- `concurrency: ec4-quality-${{ github.ref }}` con `cancel-in-progress: false` (evita ejecución redundante sin descartar evidencia de commits antiguos).

No se incluye `main` (no es parte del flujo real de la rama de trabajo). No se usa `pull_request_target`.

---

## 2. Permisos

- Nivel de workflow: `permissions: contents: read` (mínimo por defecto).
- Los jobs que suben artifacts (`backend`, `frontend`, `e2e`) elevan a `contents: write` implícito mediante `actions/upload-artifact` (necesario y documentado; solo para subir archivos de diagnóstico, nunca secrets).
- `quality-gate` no escribe artifacts.

No se usa `write-all` ni se elevan permisos de `GITHUB_TOKEN` para checkout/cache/artifacts sin justificación.

---

## 3. Actions externas y SHA completos verificados

Cada acción de terceros está fijada a su SHA de commit completo (inmutable), verificado contra el repositorio oficial de GitHub Actions:

| Action | Versión etiqueta | SHA completo verificado | Origen |
|--------|------------------|-------------------------|--------|
| `actions/checkout` | v7.0.1 | `3d3c42e5aac5ba805825da76410c181273ba90b1` | api.github.com/repos/actions/checkout/commits/v7.0.1 |
| `actions/setup-java` | v6.3.0 | `d1374f8ac74474200d16ef8ecee22c7ae7a51880` | api.github.com/repos/actions/setup-java/commits/v6.3.0 |
| `actions/setup-node` | v7.1.0 | `949feb2413d6458794dcd2491c4babbbce0c15c1` | api.github.com/repos/actions/setup-node/commits/v7.1.0 |
| `actions/upload-artifact` | v7.0.2 | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` | api.github.com/repos/actions/upload-artifact/commits/v7.0.2 |

Todos los commits están firmados (verificación GPG válida) por `web-flow` (GitHub). No se usan etiquetas mutables (`@v4`, `@main`, `@master`).

---

## 4. Detalles por job

### 4.1 Backend

**Stack:** Java 21 (Eclipse Temurin vía `setup-java`), Maven (cache habilitado vía `setup-java cache: 'maven'`), PostgreSQL 18 como service container.

**Service container PostgreSQL:**
- Imagen: `postgres:18`
- `POSTGRES_HOST_AUTH_METHOD: trust` (conexiones sin password para administración local del runner)
- Health check: `pg_isready -U postgres` (interval 10s, timeout 5s, retries 5)
- Puerto mapeado: `5432:5432`

**Preparación de base de datos de prueba (aislada, nunca `gestor_tareas`/producción):**
1. Se crea el rol `gestor_tareas_owner` con password `ownerpass` (valor de entorno de prueba, ya versionado en `database/docker/init-dev-db.sql`; no es un secreto de producción).
2. Se crea la base de datos `gestor_tareas_test` owned by `gestor_tareas_owner`.
3. Se aplican las migraciones oficiales del repositorio en orden: `V001` → `V006` (`database/migrations/`), conectando como `gestor_tareas_owner` sobre el schema `task_manager`.
4. No se ejecuta `init-dev-db.sql` a ciegas (crea la base de desarrollo `gestor_tareas` y necesita el data dir vacío del contenedor).

**Ejecución:** `cd backend && mvn -B verify`
- Surefire: pruebas unitarias.
- Failsafe / SpringBootTest: `PostgresIntegrationTest` (integración real contra `gestor_tareas_test`).
- Cucumber-JVM 7.18.0: los 19 escenarios EC4 (`CucumberIntegrationTest`, `@PP-01 or @USR or @ASG`), HTTP real contra `localhost:5432/gestor_tareas_test`.
- JaCoCo 0.8.15: `prepare-agent`, `report`, `check` en fase `verify`. Umbrales `BUNDLE`: LINE ≥ 0.85, BRANCH ≥ 0.85. El job falla si JaCoCo check falla (no se desactiva instrumentación, no `forkCount=0`).

**Propiedades inyectadas (no secretos, valores de entorno de prueba):**
- `SPRING_DATASOURCE_PASSWORD=ownerpass`
- `APP_JWT_SECRET=test-secret-for-etapa4-integration-0123456789`

**Artifacts (retención 7 días):**
- `backend-surefire-reports`: `backend/target/surefire-reports/`
- `backend-jacoco-report`: `backend/target/site/jacoco/` (HTML, XML, CSV)

---

### 4.2 Frontend

**Stack:** Node 24 (coherente con el entorno de validación local de Etapa 4), npm (cache vía `setup-node cache: 'npm'`, `cache-dependency-path: frontend/package-lock.json`).

**Ejecución:**
1. `cd frontend && npm ci` (lockfile enforcado, nunca `npm install` en CI con lockfile válido).
2. `cd frontend && npx ng test --watch=false --coverage` — Vitest 4 con cobertura v8. Umbrales en `vitest.config.ts`: Statements/Branches/Functions/Lines ≥ 85%. El job falla si cualquier métrica baja de 85%.
3. `cd frontend && npm run build` — Angular 22 producción.

**Artifacts (retención 7 días):**
- `frontend-coverage`: `frontend/coverage/`
- `frontend-build-logs`: `frontend/dist/`

No se actualizan Angular, TypeScript, Vitest ni el lockfile para adaptar el pipeline (no hay incompatibilidad demostrada).

---

### 4.3 E2E (Playwright)

**Viabilidad:** El stack se levanta dentro del runner de forma aislada (PostgreSQL service container + backend jar + Angular `ng serve`). Por tanto, el job se implementa.

**Stack:** Java 21 + Node 24 + PostgreSQL 18 (service container) + Playwright Chromium.

**Pasos:**
1. `npm ci` en frontend.
2. Instalar `postgresql-client` + `curl`.
3. Crear rol + base `gestor_tareas_test` (igual que backend).
4. Aplicar migraciones V001–V006.
5. **Semeo de usuarios de prueba** en `gestor_tareas_test` (jefe, encargado1, personal, inactivo) con los hashes BCrypt de `seed-dev-users.sql` (contraseña de prueba `Gestor2026!`, solo entorno local/CI). Esto permite que `user-crud.spec.ts` autentique al Jefe vía `E2E_CHIEF_EMAIL` / `E2E_CHIEF_PASSWORD`.
6. `mvn -B -q -DskipTests package` en backend (obtiene el jar ejecutable; los tests ya corrieron en el job `backend`).
7. `npx playwright install chromium --with-deps` (navegador + dependencias de sistema).
8. Levantar backend en background: `java -jar target/gestor-tareas-backend-0.0.1-SNAPSHOT.jar --server.port=8081` con datasource sobre `gestor_tareas_test`.
9. Levantar frontend en background: `npx ng serve --port 4200 --proxy-config proxy.conf.json` (`/api` → `http://localhost:8081`).
10. **Readiness real (sin `sleep` fijo):** `curl --retry-connrefused --retry 40 --retry-delay 2` contra `http://localhost:8081/api/users` y `http://localhost:4200/`.
11. Ejecutar `npx playwright test` con variables:
    - `E2E_BASE_URL=http://localhost:4200`
    - `E2E_API_URL=http://localhost:8081/api`
    - `E2E_CHIEF_EMAIL=jefe@gestor.local`
    - `E2E_CHIEF_PASSWORD=Gestor2026!`
12. Si falla, se publican HTML report, traces y screenshots (`if: always()`).

**Artifacts (retención 7 días):**
- `playwright-e2e-report`: `frontend/playwright-report/`, `frontend/playwright-traces/`, `frontend/test-results/`

**Nota de ejecución local (Etapa 4):** Los 25 tests E2E pasaron localmente contra el stack real levantado en `gestor_tareas_test`. El job de CI replica esa configuración. El workflow no se ha disparado en GitHub en esta sesión (no hay integración remota para `workflow_dispatch`).

---

### 4.4 Quality Gate

- `needs: [backend, frontend, e2e]`, `if: always()` (siempre se ejecuta).
- Un paso falla con `exit 1` si `contains(needs.*.result, 'failure')` o `contains(needs.*.result, 'cancelled')`.
- Esto **no enmascara fallos**: si `backend`, `frontend` o `e2e` falla, `quality-gate` falla y el workflow queda rojo.
- Si todos pasan, imprime mensaje de éxito.

---

## 5. Umbrales de cobertura (inmutables)

| Nivel | Métrica | Umbral | Origen |
|-------|---------|--------|--------|
| Backend (JaCoCo) | LINE | ≥ 85 % | `backend/pom.xml` jacoco:check |
| Backend (JaCoCo) | BRANCH | ≥ 85 % | `backend/pom.xml` jacoco:check |
| Frontend (Vitest) | Statements | ≥ 85 % | `frontend/vitest.config.ts` |
| Frontend (Vitest) | Branches | ≥ 85 % | `frontend/vitest.config.ts` |
| Frontend (Vitest) | Functions | ≥ 85 % | `frontend/vitest.config.ts` |
| Frontend (Vitest) | Lines | ≥ 85 % | `frontend/vitest.config.ts` |

No se reducen umbrales, no se omiten directorios, no se eliminan tests, no se usan exclusiones artificiales. Los umbrales ya están definidos en el build; el pipeline los reutiliza sin configuraciones contradictorias.

---

## 6. Artefactos publicados

| Artifact | Origen | Contenido | Retención |
|----------|--------|-----------|-----------|
| `backend-surefire-reports` | backend job | Surefire XML (unit + integración + Cucumber) | 7 días |
| `backend-jacoco-report` | backend job | JaCoCo HTML/XML/CSV | 7 días |
| `frontend-coverage` | frontend job | Vitest coverage HTML/JSON/lcov | 7 días |
| `frontend-build-logs` | frontend job | `frontend/dist/` (build Angular) | 7 días |
| `playwright-e2e-report` | e2e job | Playwright HTML report, traces, screenshots, test-results | 7 días |

No se suben `.env`, secrets, carpetas con datos personales ni contraseñas.

---

## 7. Seguridad

1. No se usa `pull_request_target`.
2. No hay secretos en logs, comandos, artifacts, outputs ni archivos versionados. Los valores de prueba (`ownerpass`, `Gestor2026!`, JWT de prueba) son de entorno local/CI, no secretos de producción.
3. No se imprimen `.env`, JWT, contraseñas, claves ni tokens.
4. Permisos mínimos: `contents: read` por defecto; `contents: write` solo donde `upload-artifact` lo requiere.
5. Acciones de terceros fijadas a SHA completos verificados (sección 3).
6. No se usa `write-all`.
7. No se elevan permisos de `GITHUB_TOKEN` sin justificación.
8. No se alteran branch protection ni secrets del repositorio.
9. No hay comandos destructivos de Docker ni Git.

---

## 8. Limitaciones

- El workflow **no se ha ejecutado en GitHub Actions** en esta sesión (no hay integración de repositorio remoto ni `workflow_dispatch` disparado). Fue validado sintácticamente localmente y sus comandos son los mismos que pasaron en la regresión de Etapa 4.
- El job `e2e` asume que los runners de GitHub Actions (`ubuntu-latest`) pueden levantar `java -jar`, `ng serve` y `playwright` conjuntamente. Esto es técnicamente viable pero requiere validación real en un push/PR a `Refactor-E2`.
- No hay CD ni destino de despliegue documentado en el plan.

---

## 9. Comandos reproducibles localmente (Etapa 4)

Backend:
```
cd backend
mvn verify
```
Frontend:
```
cd frontend
npm ci
npx ng test --watch=false --coverage
npm run build
```
E2E (requiere stack levantado contra `gestor_tareas_test`):
```
cd frontend
E2E_BASE_URL=http://localhost:4200 E2E_API_URL=http://localhost:8081/api \
  E2E_CHIEF_EMAIL=jefe@gestor.local E2E_CHIEF_PASSWORD=Gestor2026! \
  npx playwright test --output <directorio temporal>
```

---
*Fin de la documentación CI EC4.*
