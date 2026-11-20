# EC3 — Fase 1: Preparación y correcciones funcionales

**Responsable:** Persona 1 · **Rama:** `Refactor-E2` · **Fecha:** 2026-11-15
**Alcance:** actividades A1–A5 de `ANALISIS_EC3.md §17.2` (EC3-02, EC3-03, EC3-04 y entorno de pruebas).

---

## 1. Resumen

| ID | Hallazgo | Estado | Verificación |
|---|---|---|---|
| EC3-04 | `task-form` enviaba la duración como `allocatedBudgetBob` y `estimatedDurationMinutes = 0` | **Corregido** | Specs de Vitest + E2E API/BD |
| EC3-02 | `task_assignment` no persistía `reason` | **Corregido** (V006 + dominio + JPA) | JUnit + E2E (fila en BD) |
| EC3-03 | Límite de `reason` no alineado entre capas | **Corregido** (regla en dominio, 500) | JUnit BVA 499/500/501 + E2E |
| EC3-11 (nuevo) | `init-dev-db.sql` no aplicaba V005 → el backend no arrancaba (`ddl-auto: validate`, falta `sort_order`) | **Corregido** | Backend arranca contra la BD migrada |
| EC3-12 (nuevo) | Frontend: faltan `@angular/cdk` y `@angular/platform-browser-dynamic` en `node_modules`; el lockfile está desincronizado | **Corregido** (`platform-browser-dynamic` fijado en 22.1.3) | `ng test` y `ng build` funcionan |
| EC3-13 (nuevo) | Specs de frontend con errores de TypeScript que bloqueaban `ng test` | **Corregido** | `tsc -p tsconfig.spec.json` sin errores propios |
| R2 | La duración de la tarea no se mostraba en la lista | **Corregido** | Spec `task-list` R2 |

## 2. Cambios por capa

### Base de datos
- **Nuevo** `database/migrations/V006__task_assignment_reason.sql`: `reason varchar(500) NULL` + `ck_task_assignment_reason` (no puede ser solo espacios). Es nullable para no invalidar las asignaciones anteriores.
- `database/docker/init-dev-db.sql`: ahora aplica y registra **V005 y V006** (antes se quedaba en V004).

### Backend (arquitectura hexagonal)
- `domain/model/TaskAssignment`: campo `reason`, `REASON_MAX = 500`, `DEFAULT_REASON`, `normalizeReason()` (vacío → valor por defecto, quita espacios, >500 → `DomainValidationException`).
  - Nota: el análisis proponía poner `@Size` en `AssignWorkerCommand`, pero `HexagonalArchitectureTest` prohíbe `jakarta.*` en application/domain. Por eso la regla vive en el dominio. El contrato web (`AssignWorkerRequest`) **ya tenía** `@Size(max = 500)`.
- `AssignmentCommandService`: persiste el motivo normalizado; el listado de asignaciones devuelve el motivo (antes devolvía `null`).
- `TaskAssignmentEntity` / `AssignmentPersistenceAdapter`: mapean `reason` al guardar y al leer.

### Frontend
- `task-form` (template inline + `task-form.html`, que es una copia idéntica sin uso):
  - **Duración estimada en horas** (paso de 0.25 h = 15 min); se envía en **minutos enteros**, que es la unidad del backend y de la BD. Muestra la ayuda `≈ X h Y min de jornada`.
    - *Por qué horas:* las tareas de un trabajador se miden en horas de su jornada; los días quedan para etapas y proyecto. La BD sigue guardando minutos, así que no hace falta migrar datos.
  - **Nuevo campo de presupuesto asignado (Bs):** ≥ 0, hasta 13 enteros y 2 decimales (igual que `@Digits(13,2)` y `DECIMAL(15,2)`).
  - Los valores inválidos muestran un mensaje y **no se envía el POST**. Las subtareas no tienen estos campos (V4).
- `core/models/task.ts` + `task-mapper.ts`: la vista expone `estimatedDurationMinutes` y `allocatedBudgetBob`.
- `task-list`: muestra `Duración: 1 h 30 min` (R2).
- Specs con errores previos corregidos: rutas de import y fixture obsoleto en `hierarchy-state.service.spec.ts`; acceso a miembros privados en `task-list.spec.ts`; tipos en `task-mapper.spec.ts`.

## 3. Requisitos del entorno de pruebas (A5)

| Componente | Requerido | Encontrado en la máquina | Observación |
|---|---|---|---|
| JDK | **21** (`pom.xml`) | Por defecto 17 (sdkman). Hay 21 en `/usr/lib/jvm/java-21-openjdk-amd64` | Exportar `JAVA_HOME` (ver comandos) |
| Maven | 3.9.x | 3.9.9 | No hay `mvnw` |
| Node.js | **≥ 22.22.3** (Angular CLI 22) | Por defecto 20.19.0 (nvm). Hay 22.22.3 en nvm | `nvm use 22.22.3` |
| PostgreSQL | 18 (Docker `postgres:18`) | Contenedor `gestor-tareas-db` | Migraciones V001–V006 |
| Variables | `SPRING_DATASOURCE_PASSWORD=apppass` (dev) | — | `APP_JWT_SECRET` opcional en dev |
| Puertos | 5432 BD · 8081 backend · 4200 frontend | Libres | El proxy de Angular apunta a 8081 |
| Usuarios seed | `jefe`, `encargado1`, `encargado2`, `personal`, `inactivo` `@gestor.local` / `Gestor2026!` | Presentes (ids 1–5) | `database/docker/seed-dev-users.sql` |
| Playwright (Fase 5) | `@playwright/test` + `npx playwright install chromium` | No instalado | Lo hace la Persona 3 |

### Comandos

```bash
# BD nueva (aplica V001..V006 y los usuarios seed)
docker compose down -v && docker compose up -d db

# BD existente: aplicar solo V005/V006 (son idempotentes)
docker exec gestor-tareas-db psql -v ON_ERROR_STOP=1 -U gestor_tareas_owner -d gestor_tareas \
  -f /migrations/V005__add_sort_order.sql -f /migrations/V006__task_assignment_reason.sql
# y registrarlas en public.database_schema_migrations (versiones 5 y 6, checksum sha256 del archivo)

# Backend
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=$JAVA_HOME/bin:$PATH
cd backend && mvn verify                                   # tests + JaCoCo
SPRING_DATASOURCE_PASSWORD=apppass mvn spring-boot:run     # :8081

# Frontend (Node 22)
nvm use 22.22.3
cd frontend && npm install && npx ng test --watch=false    # NO usar "npx vitest run": no carga el entorno Angular
npx ng serve                                               # :4200

# E2E de Fase 1 (API + BD reales, con el backend levantado)
bash docs/EC3/scripts/e2e_fase1.sh
```

## 4. Dependencias del frontend (EC3-12)

`frontend/package.json` declaraba `@angular/platform-browser-dynamic@^22.1.0`, pero el lockfile no lo resolvía de forma compatible: `^22.1.0` y `~22.1.3` dan 22.2.4 / 22.1.8, que piden **exactamente** su misma versión de `@angular/core`, y el lock tiene todo Angular en 22.1.3. Por eso no se podía instalar y en `node_modules` faltaban también `@angular/cdk` y `platform-browser-dynamic`.
**Solución aplicada:** `npm install --save-dev --save-exact @angular/platform-browser-dynamic@22.1.3` (con Node 22). El lockfile solo cambia en ese paquete (de 22.2.4 a 22.1.3). npm avisa que el paquete está *deprecated*; `src/test-setup.ts` lo sigue usando, y migrarlo a `@angular/platform-browser` queda como mejora opcional.

**Regresión detectada y corregida durante la ejecución:** al exponer `allocatedBudgetBob` en la vista (R2), "Mis tareas" del Personal empezó a recibir un presupuesto de relleno (0). Eso viola US3.1, que prohíbe datos financieros para el Personal. La detectó el spec existente `task.service.spec.ts`. Se corrigió en `TaskService.listAssignedTasks`, que ahora quita `allocatedBudgetBob` y `estimatedDurationMinutes` de esa vista.

## 5. Métricas reales (2026-11-15)

| Indicador | Backend | Frontend |
|---|---|---|
| Tests ejecutados / aprobados / fallidos / omitidos | **328 / 328 / 0 / 0** | **295 / 295 / 0 / 0** (18 archivos) |
| Cobertura de sentencias | — | **92.02 %** |
| Cobertura de líneas | **93.00 %** | **92.89 %** |
| Cobertura de ramas | **83.21 %** (umbral 85 %: `mvn verify` falla en `jacoco:check`) | **87.77 %** |
| Cobertura de funciones | — | **84.20 %** (umbral 85 % en `vitest.config.ts`) |
| Build | `mvn` compila | `ng build` OK (avisos previos de *budget* de tamaño) |
| E2E API + BD (`e2e_fase1.sh`) | **21 / 21 PASS** | — |

Archivos de frontend tocados en Fase 1: `task-form.ts`, `task-mapper.ts` y `task.ts` tienen cobertura completa (el reporte los omite por estar al 100 %).
`ng test` no aplica los umbrales de `vitest.config.ts` (termina con código 0 aun con funciones al 84.2 %); el archivo más bajo es `task-list.ts` (funciones 73.5 %, ramas 77 %). Le corresponde a la Fase 6.

La cobertura de ramas ya estaba por debajo del umbral antes de esta fase (83.01 % documentado, EC3-06). Subirla le corresponde a la Fase 6.
