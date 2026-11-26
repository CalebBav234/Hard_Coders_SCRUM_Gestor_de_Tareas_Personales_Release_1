# GUIA DE TESTING COMPLETA — EC4

**Elemento de Competencia 4 — BDD + Gherkin + CRUD Full-Stack + Pruebas de Integración + Cobertura de Pruebas + CI/CD**  
**Repositorio:** `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`  
**Rama:** `Refactor-E2`  
**SHA documentada:** `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3`  
**Idioma:** Español  
**Propósito:** Guía práctica y precisa de comandos para ejecutar toda la suite de pruebas del EC4 (backend, frontend, Cucumber-BDD, cobertura y E2E).

> **Regla de integridad:** esta guía solo documenta comandos de ejecución y verificación. No modifica código de producto, configuración, dependencias, migraciones ni base de datos. No ejecuta comandos destructivos (`git reset`, `clean`, `stash`, `checkout`, `docker compose down -v`, borrado de volúmenes).

---

## 0. Prerrequisitos y verificación del entorno

Antes de ejecutar cualquier prueba, confirma la rama, el SHA y que el working tree está limpio:

```powershell
git branch --show-current
git rev-parse HEAD
git status --short --branch
```

Valores esperados en esta entrega:
- Rama: `Refactor-E2`
- SHA: `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3`
- Working tree: limpio (sin cambios sin commit, salvo los archivos `.md` de documentación creados en la Etapa 6)

### 0.1 Versiones de entorno verificadas

| Herramienta | Versión esperada | Comando de verificación |
|---|---|---|
| Java (JDK) | 21 declarado en `pom.xml`; entorno JDK 24.0.2 | `java -version` |
| Maven | 3.9.16 | `mvn -version` |
| Node | v24.15.0 | `node --version` |
| npm | 11.12.1 | `npm --version` |
| PostgreSQL | 18 (contenedor `gestor-tareas-db`) | `docker ps` o `pg_isready -h localhost -p 5432` |

### 0.2 Base de datos de integración

Todas las pruebas de integración y BDD usan la base de datos aislada **`gestor_tareas_test`** (nunca `gestor_tareas` de desarrollo ni producción). Confirma que el contenedor PostgreSQL esté corriendo en `localhost:5432` y que exista la base `gestor_tareas_test`.

---

## 1. Backend — pruebas unitarias, de integración y Cucumber-BDD

Todas las comandos de backend se ejecutan desde el directorio `backend/`.

### 1.1 Ejecutar TODAS las pruebas del backend (recomendado)

```powershell
cd backend
mvn verify
```

**Qué hace:** ejecuta Surefire (unitarias), Failsafe/integración (`PostgresIntegrationTest`), Cucumber-BDD (19 escenarios EC4) y JaCoCo con `jacoco:check` (umbrales LINE ≥ 85 %, BRANCH ≥ 85 %).

**Resultado esperado:** `BUILD SUCCESS`, `Tests run: 412, Failures: 0, Errors: 0, Skipped: 0`, `All coverage checks have been met.`

### 1.2 Ejecutar solo pruebas unitarias (Surefire)

```powershell
cd backend
mvn test
```

**Resultado esperado:** `BUILD SUCCESS`, ~343 pruebas unitarias, 0 fallos.

### 1.3 Ejecutar solo el escenario BDD completo (Cucumber-JVM — los 19 escenarios EC4)

```powershell
cd backend
mvn test -Dtest=CucumberIntegrationTest
```

**Qué ejecuta:** `backend/src/test/java/com/hardcoders/taskmanager/bdd/CucumberIntegrationTest.java`, que invoca Cucumber-JVM 7.18.0 con los tres archivos `.feature` y el tag `@PP-01 or @USR or @ASG`.

**Resultado esperado:** `19 Scenarios (19 passed), 123 Steps (123 passed)`, `CUCUMBER_EXIT_CODE=0`.

Los 19 escenarios son:
- `PP-01` — `proceso-principal.feature`
- `USR-INS-01`, `USR-INS-02`, `USR-INS-03`, `USR-UPD-01`, `USR-UPD-02`, `USR-UPD-03`, `USR-DEL-01`, `USR-SEL-01`, `USR-SEL-02` — `app-user-crud.feature`
- `ASG-INS-01`, `ASG-INS-02`, `ASG-INS-03`, `ASG-UPD-01`, `ASG-UPD-02`, `ASG-UPD-03`, `ASG-DEL-01`, `ASG-SEL-01`, `ASG-SEL-02` — `task-assignment-crud.feature`

### 1.4 Ejecutar solo pruebas de integración contra PostgreSQL

```powershell
cd backend
mvn test -Dtest=PostgresIntegrationTest
```

**Resultado esperado:** 44 pruebas de integración, 0 fallos, contra `gestor_tareas_test`.

### 1.5 Generar reporte de cobertura JaCoCo

```powershell
cd backend
mvn jacoco:report
```

**Reportes generados (no versionados, están en `.gitignore`):**
- `backend/target/site/jacoco/index.html`
- `backend/target/site/jacoco/jacoco.csv`
- `backend/target/site/jacoco/jacoco.xml`

**Umbrales verificados en esta entrega:** LINE 95,14 %, BRANCH 85,26 % (ambos ≥ 85 %).

### 1.6 Ejecutar una clase de prueba individual (ejemplo)

```powershell
cd backend
mvn test -Dtest=UserCommandServiceTest
```

---

## 2. Frontend — pruebas Vitest con cobertura y build

Todas las comandos de frontend se ejecutan desde el directorio `frontend/`.

### 2.1 Ejecutar pruebas Vitest con cobertura (recomendado)

```powershell
cd frontend
npx ng test --watch=false --coverage
```

**Qué hace:** Vitest 4.1.12 con proveedor de cobertura v8 (`@vitest/coverage-v8`). Umbrales en `frontend/vitest.config.ts`: Statements/Branches/Functions/Lines ≥ 85 %.

**Resultado esperado:** `Test Files  23 passed (23)`, `Tests  349 passed (349)`, cobertura por encima del 85 % en las 4 métricas.

**Valores verificados en esta entrega:**
- Statements: 92,95 %
- Branches: 88,53 %
- Functions: 87,37 %
- Lines: 94,05 %

### 2.2 Ejecutar pruebas Vitest sin cobertura (más rápido)

```powershell
cd frontend
npx ng test --watch=false
```

### 2.3 Build de producción de Angular

```powershell
cd frontend
npm run build
```

**Resultado esperado:** `SUCCESS` (solo warnings de presupuesto de bundle, no errores). Salida: `frontend/dist/gestor-tareas`.

---

## 3. Pruebas E2E — Playwright (Chromium)

Las pruebas E2E requieren que el **backend** y el **frontend** estén levantados localmente contra `gestor_tareas_test`. No ejecutar sin ambos servicios arriba.

### 3.1 Levantar el backend para E2E

```powershell
cd backend
mvn -B -q -DskipTests package
java -jar target\gestor-tareas-backend-0.0.1-SNAPSHOT.jar --server.port=8081
```

> El backend debe apuntar a `gestor_tareas_test` (configurado en el entorno de prueba). Puerto: 8081.

### 3.2 Levantar el frontend para E2E

```powershell
cd frontend
npx ng serve --port 4200 --proxy-config proxy.conf.json
```

> El proxy `/api` apunta a `http://localhost:8081`. Puerto: 4200.

### 3.3 Ejecutar pruebas E2E Playwright

Desde una tercera terminal, con el backend y frontend ya levantados:

```powershell
cd frontend
$env:E2E_BASE_URL="http://localhost:4200"
$env:E2E_API_URL="http://localhost:8081/api"
$env:E2E_CHIEF_EMAIL="jefe@gestor.local"
$env:E2E_CHIEF_PASSWORD="Gestor2026!"
npx playwright test --output <directorio temporal>
```

**Resultado esperado:** `25 passed (25)`.

Desglose por spec:
- `e2e/auth/auth.spec.ts` — 6 PASS
- `e2e/tasks/main-flow.spec.ts` (proceso principal) — 1 PASS
- `e2e/tasks/task-assignment.spec.ts` (asignaciones) — 3 PASS
- `e2e/tasks/validations.spec.ts` — 3 PASS
- `e2e/users/user-create.spec.ts` — 4 PASS
- `e2e/users/user-crud.spec.ts` (usuarios) — 8 PASS

### 3.4 Nota sobre el reporte Playwright protegido

El archivo `frontend/playwright-report/index.html` (565.912 bytes, 20/11/2026) está protegido y **no se sobrescribe**. La salida de las pruebas E2E se redirige a un directorio temporal mediante `--output <directorio temporal>` para no tocar ese archivo.

---

## 4. Ejecución completa de extremo a extremo (todo junto)

Secuencia recomendada para validar todo el EC4:

```powershell
# 1. Verificar entorno
git branch --show-current
git rev-parse HEAD

# 2. Backend completo (unitarias + integración + Cucumber + JaCoCo)
cd backend
mvn verify

# 3. Frontend con cobertura y build
cd frontend
npx ng test --watch=false --coverage
npm run build

# 4. E2E (requiere backend y frontend levantados en terminales separadas)
# Terminal A: backend
cd backend
mvn -B -q -DskipTests package
java -jar target\gestor-tareas-backend-0.0.1-SNAPSHOT.jar --server.port=8081

# Terminal B: frontend
cd frontend
npx ng serve --port 4200 --proxy-config proxy.conf.json

# Terminal C: E2E
cd frontend
$env:E2E_BASE_URL="http://localhost:4200"
$env:E2E_API_URL="http://localhost:8081/api"
$env:E2E_CHIEF_EMAIL="jefe@gestor.local"
$env:E2E_CHIEF_PASSWORD="Gestor2026!"
npx playwright test --output <directorio temporal>
```

**Resultado global esperado:**
- Backend: 412 pruebas, 0 fallos, JaCoCo ≥ 85 %
- Frontend: 349 pruebas Vitest, 0 fallos, cobertura ≥ 85 %, build SUCCESS
- E2E: 25 pruebas Playwright, 0 fallos

---

## 5. Comandos de verificación rápida (resumen)

| Objetivo | Comando | Directorio |
|---|---|---|
| Todas las pruebas backend | `mvn verify` | `backend/` |
| Solo Cucumber (19 escenarios EC4) | `mvn test -Dtest=CucumberIntegrationTest` | `backend/` |
| Solo integración PostgreSQL | `mvn test -Dtest=PostgresIntegrationTest` | `backend/` |
| Reporte JaCoCo | `mvn jacoco:report` | `backend/` |
| Vitest con cobertura | `npx ng test --watch=false --coverage` | `frontend/` |
| Build Angular | `npm run build` | `frontend/` |
| Playwright E2E | `npx playwright test --output <temp>` | `frontend/` |

---

## 6. Rutas de reportes generados (locales, no versionados)

| Reporte | Ruta |
|---|---|
| JaCoCo HTML | `backend/target/site/jacoco/index.html` |
| JaCoCo CSV | `backend/target/site/jacoco/jacoco.csv` |
| JaCoCo XML | `backend/target/site/jacoco/jacoco.xml` |
| Surefire (por clase) | `backend/target/surefire-reports/` |
| Cucumber (Surefire XML) | `backend/target/surefire-reports/TEST-com.hardcoders.taskmanager.bdd.CucumberIntegrationTest.xml` |
| Vitest HTML | `frontend/coverage/gestor-tareas/index.html` |
| Vitest JSON | `frontend/coverage/gestor-tareas/coverage-final.json` |
| Vitest Clover | `frontend/coverage/gestor-tareas/clover.xml` |
| Playwright histórico (protegido, no regenerar) | `frontend/playwright-report/index.html` |

---

## 7. Limitaciones documentadas

- No existe run real de GitHub Actions para esta rama (el workflow `.github/workflows/ec4-quality.yml` está definido pero no se ha disparado en GitHub). Todos los resultados arriba son de **validación local**.
- Las capturas de pantalla de los reportes HTML de JaCoCo y Vitest no se generaron porque las herramientas de navegador bloquean el protocolo `file://`. Se consultan los reportes HTML directamente en las rutas de la sección 6.
- No se modificó ningún código de producto, configuración, dependencia, prueba, migración ni base de datos al generar esta guía.

---
*Fin de la GUIA DE TESTING COMPLETA — EC4.*
