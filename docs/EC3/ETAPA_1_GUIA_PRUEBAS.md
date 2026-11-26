# ETAPA 1 — Guía de pruebas y verificación (EC3 / Fase 1)

**Alcance:** Auditoría de aceptación (no reimplementación) de los hallazgos/correcciones reportados para Fase 1 (EC3-04, EC3-02/03, EC3-11/12/13, R2, US3.1) sobre `Refactor-E2`.  
**Fecha de verificación:** 2026-11-15/16 (local). **Actualización post-corrección:** 2026-11-16 (incremento 0.25 h).  
**Rama verificada:** `Refactor-E2` (`git branch --show-current`)  
**HEAD verificado:** `10988a31d81f7596947c93d1e3abc3ddb42028db` (`git rev-parse HEAD`) — coincide con el commit de cierre indicado.  
**Tracking remoto:** `Refactor-E2...origin/Refactor-E2` (`git status --short --branch`).  
**Working tree:** no limpio. Cambio local preexistente: `?? docs/EC3/MASTER_PROMPTS_EC3_7_ETAPAS (1).md` (ajeno a Fase 1). Durante esta etapa se creó/actualizó `docs/EC3/ETAPA_1_GUIA_PRUEBAS.md` y se aplicó la corrección autorizada del incremento 0.25 h en frontend (`task-form.ts`, `task-form.spec.ts`). No se modificaron migraciones ni backend; sin commit/push/merge.

> **Criterio aplicado:** Código y reportes recién generados prevalecen sobre afirmaciones históricas. Se combinaron inspección estática y ejecuciones runtime (Docker activo, JDK 21 para Maven, Node/npm presentes). No se ejecutó `docker compose down -v`; no se eliminó ningún volumen.

---

## 1. Alcance y requisitos verificados

| ID | Requisito declarado | Evidencia (código/tests/ejecución) | Resultado |
|---|---|---|---|
| EC3-04 | Duración estimada en horas (`step="0.25"`), conversión a `estimatedDurationMinutes` (min enteros), `allocatedBudgetBob` independiente, validaciones, subtareas sin campos V4, persistencia/lectura/lista | `task-form.ts/.html`, `core/models/task.ts`, `task-mapper.ts`, `task-list.ts`, specs `task-form.spec.ts`, `task-mapper.spec.ts`, `task-list.spec.ts`, `task.service.spec.ts`; `ng test` **297/297**; E2E 21/21 funcionales OK (API+BD) | **PASS** |
| EC3-02 | `task_assignment.reason` persiste end-to-end (V006, dominio, entidad JPA, adaptador write/read, consulta/listado) | V006, `TaskAssignment`, `TaskAssignmentEntity`, `AssignmentPersistenceAdapter`, `AssignmentCommandService`, controller/requests/responses, tests dominio/adaptador/controller; E2E asignación/listado/BD OK | **PASS** |
| EC3-03 | Límite 500 de `reason` en contrato HTTP y dominio (`normalizeReason`) | `AssignWorkerRequest @Size(max=500)`, `TaskAssignment.REASON_MAX=500` + `normalizeReason()`, BVA 499/500/501 (tests), E2E 501→400, 500→200, longitud BD 500 | **PASS** |
| EC3-11 | Bootstrap DB ejecuta/trackea V001–V006; backend compatible con `ddl-auto: validate` | `database/docker/init-dev-db.sql` aplica/trackea V001–V006; `gestor-tareas-db` alineado a V001–V006 (V005/V006 aplicados idempotentes y tracking registrado); arranque backend `ddl-auto: validate` OK | **PASS** |
| EC3-12 | Dependencias Angular alineadas (`package.json`/`package-lock.json`, Angular packages) | `frontend/package.json`/lock; `@angular/platform-browser-dynamic` 22.1.3; `ng build` y `ng test` OK | **PASS** |
| EC3-13 | Tests frontend compilan sin errores TS propios y `ng test` | `npx tsc -p tsconfig.spec.json --noEmit` EXIT 0; `npx ng test --watch=false` 18 archivos, **297/297 PASS** | **PASS** |
| R2 | Duración se muestra en la lista | `task-list.ts` (`data-testid="task-duration"`, `formatMinutes`), spec R2; `ng test` OK | **PASS** |
| US3.1 | "Mis tareas" no expone datos de planificación/finanzas restringidos | `TaskService.listAssignedTasks` elimina `allocatedBudgetBob`/`estimatedDurationMinutes`; `AssignedTaskResponse` sin esos campos; spec `task.service.spec.ts` | **PASS** |

---

## 2. Entorno de verificación

| Componente | Detectado / usado | Observación |
|---|---|---|
| Rama / HEAD | `Refactor-E2` / `10988a31d81f7596947c93d1e3abc3ddb42028db` | Coincide |
| OS / shell | Windows (`win32`), PowerShell | Ejecución local |
| JDK (Maven) | JDK **21.0.12.1+1** usado vía `JAVA_HOME`/`PATH` | Sistema tiene Java 24 en PATH; no usado para Maven |
| Maven | 3.9.16 | Disponible |
| Node.js / npm | Node v24.15.0, npm 11.12.1 | `@angular/core` 22.1.3 instalado; compatible; tests/build OK |
| Docker | Docker 28.4.0, daemon activo | `gestor-tareas-db` (postgres:18) **Up (healthy)**, 5432 |
| BD contenedor | `gestor-tareas-db` (bind-mounts migrations/bootstrap/seed) | Alineada a V001–V006; usuarios seed presentes |
| Backend | puerto 8081, `ddl-auto: validate` | Arranque smoke OK |

**Seguridad BD:** No se usó BD compartida conocida ni `docker compose down -v`. V005/V006 aplicadas de forma idempotente/aditiva y registradas en tracking.

---

## 3. Inspección de rama y fuentes

- `git status --short --branch` → `## Refactor-E2...origin/Refactor-E2` y cambio local ajeno preexistente.
- `git branch --show-current` → `Refactor-E2`. `git rev-parse HEAD` → `10988a31d81f7596947c93d1e3abc3ddb42028db`.
- Fuentes leídas: `docs/EC3/FASE1_CORRECCIONES_Y_ENTORNO.md`, `ANALISIS_EC3.md`, `DIVISION_TRABAJO_EC3.md`, `TESTING_GUIDE.md`, V005/V006, `database/docker/init-dev-db.sql`, código frontend/backend y specs relevantes, `docs/EC3/scripts/e2e_fase1.sh`.

---

## 4. Verificación EC3-04 — duración y presupuesto

### 4.1 Implementación (post-corrección)
- **Formulario:** `#estimatedHours` `type="number" min="0" step="0.25"`; conversión `Math.round(hours*60)` → `estimatedDurationMinutes`; presupuesto `#allocatedBudgetBob` independiente (`BUDGET_PATTERN /^\d{1,13}(\.\d{1,2})?$/`, `>= 0`).
- **Validación de incremento 0.25 h (corregida):** `validateHours()` rechaza no múltiplos de 0.25 h (`hours / 0.25` entero con tolerancia `1e-9`) con mensaje `"La duración debe usar incrementos de 0.25 h (15 min)."` y bloquea el POST en el flujo real (`button type="button"` + `crearTarea()`).
- **Subtareas:** sin campos de plan (`@if (!parentTaskId)`, envío 0 en ruta subtarea).
- **Mapper/lista:** `task-mapper.ts`/`task.ts` mapean y exponen `estimatedDurationMinutes`/`allocatedBudgetBob`; `task-list.ts` muestra duración (`data-testid="task-duration"`).
- **US3.1:** `listAssignedTasks` elimina campos de plan/finanzas de la vista; `AssignedTaskResponse` no los incluye.

### 4.2 Evidencia runtime / specs
- `npx tsc -p tsconfig.spec.json --noEmit` → EXIT 0.
- `npx ng test --watch=false` → **18 archivos, 297/297 PASS**.
  - `task-form.spec.ts`: envío 1.5 h→90 min y 250.50 Bs; BVA 0/0.25/8 h aceptados; `-0.25 h` rechazado; **`0.1 h` y `0.3 h` rechazados sin POST** (incremento 0.25 h); duración máxima y presupuesto BVA/EP rechazados sin POST; vacío→0; subtareas sin campos.
  - `task-mapper.spec.ts`: mapeo de duración/presupuesto.
  - `task-list.spec.ts`: R2 muestra `1 h 30 min` para 90 min y oculta si no hay valor.
  - `task.service.spec.ts`: US3.1 sin `allocatedBudgetBob`.
- E2E API+BD: tarea 90 min / 250.50 Bs en respuesta y BD; BVA -1 duración y -0.01 presupuesto rechazados (400).

**Resultado EC3-04:** PASS.

---

## 5. Verificación EC3-02/03 — `task_assignment.reason`

- V006 (`reason varchar(500) NULL` + `ck_task_assignment_reason`, NULL histórico permitido) y V005 intacto.
- Dominio `TaskAssignment`: `REASON_MAX=500`, `DEFAULT_REASON`, `normalizeReason()` (null/vacío→default, trim, >500 `DomainValidationException`).
- HTTP: `AssignWorkerRequest @Size(max=500)`. Persistencia/listado: entidad, adaptador save/read/toDomain, servicio normaliza antes de persistir.
- Sin `jakarta.*` en application/domain para esta validación (arquitectura/tests OK).
- E2E: motivo explícito (trim) persistido/listado; default persistido; 501→400; 500→200 con longitud 500 en BD; duplicidad e inactivo rechazados.

**Resultado:** PASS.

---

## 6. Verificación EC3-11 — bootstrap y schema

- `init-dev-db.sql` aplica/trackea V001–V006 con checksums.
- `gestor-tareas-db`: V001–V004 presentes inicialmente; V005/V006 aplicados idempotentes y tracking v5/v6 registrado con checksums del bootstrap. Columna `reason` presente.
- Backend `ddl-auto: validate` arrancó correctamente (smoke). Sin `docker compose down -v`.

**Resultado:** PASS.

---

## 7. Verificación EC3-12/13 — Angular

- `@angular/platform-browser-dynamic` 22.1.3; `tsc -p tsconfig.spec.json --noEmit` EXIT 0; `ng test --watch=false` 18/18, **297/297 PASS**; `ng build` EXIT 0 (warnings budget preexistentes).

**Resultado:** PASS.

---

## 8. Comandos ejecutados y resultados

| Comando | CWD / contexto | Resultado | Observaciones |
|---|---|---:|---|
| `git status/branch/rev-parse` | repo | EXIT 0 | Rama/HEAD/WT registrados |
| Inspección Docker/BD + `psql ... database_schema_migrations` | host/contenedor | OK | `gestor-tareas-db` healthy; alineación V001–V006 |
| `psql -f V005/V006` + inserts tracking v5/v6 | contenedor | EXIT 0 | Idempotente/aditivo |
| `npx tsc -p tsconfig.spec.json --noEmit` | `frontend` | EXIT 0 | Sin errores TS propios |
| `npx ng test --watch=false` (pre-fix verificación inicial) | `frontend` | EXIT 0 | 295/295 PASS |
| `npx ng build` | `frontend` | EXIT 0 | Warnings budget |
| `mvn test` (JDK 21) | `backend` | EXIT 0 | 328/328 PASS, BUILD SUCCESS |
| `mvn verify` (JDK 21) | `backend` | **EXIT 1** | Tests 328/328 PASS; falla solo `jacoco:check` branches 0.83 < 0.85 (Etapa 6) |
| Smoke `mvn -q -DskipTests spring-boot:run` (`ddl-auto: validate`) | `backend` | OK | Started en 8081 |
| E2E Fase 1 (equivalente Python del script) | repo | **21/21 funcionales OK** | Motivo default verificado en BD (`Asignación de Personal`) |
| **Corrección 0.25 h**: `validateHours()` + specs 0.1/0.3 | `frontend` | aplicado | `task-form.ts`, `task-form.spec.ts` |
| `npx tsc -p tsconfig.spec.json --noEmit` (post-fix) | `frontend` | EXIT 0 | OK |
| `npx ng test --watch=false` (post-fix) | `frontend` | EXIT 0 | **18 archivos, 297/297 PASS** (incluye 0.1/0.3 sin POST) |

---

## 9. E2E API+BD (Fase 1)

Ejecutado contra `http://localhost:8081/api` y `gestor-tareas-db` (equivalente al script por ausencia de `python3`/bash en Windows). **21/21 criterios funcionales verificados:** login, creación proyecto/etapa/actividad/tarea, EC3-04 (90 min, 250.50 Bs respuesta/BD, BVA -1/-0.01 rechazados), EC3-02 (asignación, trim, persistencia, listado, default), EC3-03 (501 rechazado, 500 aceptado, longitud 500 BD), duplicidad e inactivo rechazados. Sin credenciales/tokens/secretos en esta guía.

---

## 10. Métricas actuales vs históricas

### Actuales
| Indicador | Backend | Frontend |
|---|---|---|
| Tests ejecutados/aprobados/fallidos/omitidos | 328 / 328 / 0 / 0 | **297 / 297 / 0 / 0** (18 archivos) |
| TypeScript specs | N/A | `tsc -p tsconfig.spec.json --noEmit` EXIT 0 |
| Build | OK (en verify antes de jacoco:check) | `ng build` EXIT 0 (warnings budget) |
| `mvn verify` | **FAIL solo `jacoco:check`** (branches 0.83 < 0.85) | N/A |
| E2E API+BD Fase 1 | 21/21 funcionales OK | N/A |
| Smoke `ddl-auto: validate` | PASS | N/A |

> No se declara PASS de cobertura global: brechas ramas backend/funciones frontend son deuda de **Etapa 6**. El fallo de `jacoco:check` no se atribuye a Fase 1.

### Históricas (referencia, 2026-11-15)
| Indicador | Backend | Frontend |
|---|---:|---:|
| Tests | 328/328/0/0 | 295/295/0/0 |
| Líneas | 93.00% | 92.89% |
| Ramas | 83.21% | 87.77% |
| Sentencias | — | 92.02% |
| Funciones | — | 84.20% |
| E2E API+BD Fase 1 | 21/21 PASS | — |

---

## 11. Hallazgos y limitaciones

| Severidad | ID | Evidencia | Impacto | Estado |
|---|---|---|---|---|
| **Resuelto** | EC3-04-STEP | `task-form.ts` `validateHours()` exige múltiplos de 0.25 h; specs 0.1/0.3 rechazados sin POST; `ng test` 297/297 PASS. | Incremento 15 min aplicado realmente en flujo de creación. | **Corregido y verificado** |
| Informativa | EC3-11-DB-STATE | `gestor-tareas-db` alineado de V001–V004 a V001–V006 (idempotente + tracking). | BD operativa consistente con bootstrap/validate. | Resuelto |
| Informativa | COV-GLOBAL | `mvn verify` falla en `jacoco:check` (branches 0.83 < 0.85). | Deuda de cobertura global. | Etapa 6 |

---

## 12. Checklist PASS / FAIL / BLOCKED (Gate Etapa 1)

- [x] Rama `Refactor-E2`, HEAD y working tree verificados.
- [x] A1 duration-hours → integer minutes demostrado por specs/payload/BD.
- [x] **Incremento 0.25 h aplicado realmente (0.1/0.3 inválidos no generan POST).**
- [x] `allocatedBudgetBob` independiente y con validaciones reales.
- [x] Subtareas sin campos no soportados por modelo V4.
- [x] Vista de Personal no reexpone campos retirados por US3.1.
- [x] V006 persist/read de `reason`; V005 intacto.
- [x] `reason` 500/normalization/default/domain validation verificados.
- [x] Sin Jakarta Validation en application/domain por `reason`; arquitectura OK.
- [x] V005/V006 aplicadas/trackeadas y backend arranca con `ddl-auto: validate`.
- [x] `ng test`, `ng build` y `tsc -p tsconfig.spec.json --noEmit` ejecutados exitosamente.
- [x] E2E API+BD Fase 1: 21/21 criterios funcionales verificados.
- [x] No se declaró PASS de cobertura global (brechas para Etapa 6).
- [x] Guía completa, reproducible y **sin secretos**.

---

## 13. Estado final

**Estado: PASS (funcionalidades de Fase 1 verificadas)**

- Se corrigió y verificó el criterio de incremento **0.25 h** (0.1/0.3 h rechazados sin POST). EC3-04, EC3-02/03, EC3-11/12/13, R2 y US3.1 verificados por inspección y ejecución.
- Resultados clave: backend `mvn test` **328/328 PASS**; frontend `ng test` **297/297 PASS**; `tsc` specs y `ng build` OK; E2E Fase 1 **21/21 funcionales OK**; smoke `ddl-auto: validate` OK.
- `mvn verify` falla únicamente en `jacoco:check` por umbral global de ramas (deuda **Etapa 6**), no atribuible a Fase 1. No se redujeron thresholds. No se inició Etapa 2.
