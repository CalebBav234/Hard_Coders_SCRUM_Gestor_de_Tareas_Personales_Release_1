# Registro de sesión — Revisión EC3: correcciones + plan de validación

**Fecha:** 2026-11-18
**Rama:** `Refactor-E2-testing` (HEAD `ecd5f97`, cambios **sin commit**)
**Entorno:** Linux, JDK 21 (`/usr/lib/jvm/java-21-openjdk-amd64`), Node 22.22.3 (nvm), Docker `gestor-tareas-db` (PostgreSQL 18)
**Objetivo:** validar las Etapas 1–6 antes de cerrar el informe EC3 (ANEXO 1):
- corregir bugs de bajo riesgo detectados en la auditoría;
- volver a correr las suites;
- entregar un plan de pruebas manual (`PLAN_PRUEBAS_VALIDACION_EC3.md`).

> La sesión se interrumpió una vez (apagado por batería). Al retomarla se revisó el árbol de trabajo: las Partes A, B y C estaban aplicadas completas, sin ediciones a medias, así que **no fue necesario `git restore`**. Se completó lo pendiente.

---

## 1. Correcciones de código

### 1.1 Backend (`backend/src/main/java/com/hardcoders/taskmanager/`)
| ID | Archivo | Cambio | Test |
|---|---|---|---|
| FIX-01 (DIV-E4-01) | `application/usecase/UserCommandService.java` | `requirePassword` rechaza contraseñas de más de 72 bytes UTF-8 con `DomainValidationException` → **400** (antes 500, por BCrypt). Se corrigió el comentario sobre `PASSWORD_MAX`. | `UserCommandServiceTest.rejectsPasswordsOverBcryptByteLimitBeforeHashing`. En `PostgresIntegrationTest.passwordBoundaries`, la aserción `isIn(400,500)` pasó a `400`. |
| FIX-02 | `application/usecase/TaskCommandService.java` | Nuevas constantes `CHIEF_OVERRIDE_PREFIX` y `STATUS_REASON_MAX = 500`. Si el motivo con prefijo de override del Jefe supera 500 → **400** (antes error de BD). Motivo útil máximo: 473 caracteres. | `TaskCommandServiceTest.chiefOverrideReasonMustFitStatusHistoryColumn` |
| FIX-03 | `adapter/in/web/task/TaskController.java` | `searchTasks` mapeado con `params = {"q", "!projectId"}`: `GET /api/tasks?projectId=X&q=Y` ahora filtra por proyecto en lugar de hacer búsqueda global. | Nuevo `adapter/in/web/task/TaskSearchRoutingTest.java` (2 casos) |
| FIX-04 | `application/usecase/TaskCommandService.java` | `cancel()` llama a `task.verifyVersion(command.version())` → **412** con versión vieja. El frontend ya enviaba `?version=`. | `TaskCommandServiceTest.cancelRejectsStaleVersion` |

### 1.2 Frontend (`frontend/src/app/`)
| ID | Archivo | Cambio | Test |
|---|---|---|---|
| FIX-05 | `features/projects/project-list/project-list.ts` | Se eliminó el bloque duplicado del botón «✎ Editar datos». | `features/projects/projects.spec.ts` (exactamente 1 botón) |
| FIX-06 | `features/tasks/task-form/task-form.ts` | Título: `maxlength="160"`, mensaje de error y bloqueo de envío si supera 160. | Specs existentes |
| FIX-06 | `features/tasks/task-editor/task-editor.ts` | Título: `maxlength="160"` (el validador 160 ya existía). Descripción: 4000 → **2000** (`maxlength`, `Validators.maxLength` y mensaje). | Specs existentes |
| FIX-07 | `features/tasks/task-home/task-home.ts` | Se quitaron 2 `console.log` de depuración. | — |
| FIX-08 | `features/users/user-form/user-form.ts` | Validador exportado `maxUtf8Bytes(72)` con mensaje específico de 72 bytes. El botón «Crear usuario» ya no se deshabilita si el formulario es inválido: `submit()` hace `markAllAsTouched()` y sale. | Nuevo `features/users/user-form/user-form.spec.ts` (4 casos) |
| FIX-09 | `frontend/e2e/tasks/main-flow.spec.ts` | Se reemplazó el predicado inválido `r.url().includes(/stages//activities)` por una regex sobre `/api/stages/{id}/activities`. | Playwright |
| — | `frontend/e2e/users/user-create.spec.ts` | Test «contraseña corta bloquea envío» adaptado a FIX-08: en vez de exigir el botón deshabilitado, verifica que no se envía POST y que el error sigue visible. | Playwright |

---

## 2. Higiene del repositorio
- Se sacaron del índice con `git rm --cached` (**los archivos locales siguen en disco**):
  - 17 `*.log` / `*.err` de la raíz;
  - `run-backend-etapa2.cmd`, que contenía la contraseña dev de la BD y una ruta local de Windows;
  - `sq-downloads.html`;
  - `node_modules/` de la raíz (678 archivos).
- `.gitignore`: se agregaron `/*.log`, `/*.err`, `/run-*.cmd`, `/sq-downloads.html` y `/node_modules/`.
- El `package.json` raíz (solo trae `vitest`) se dejó intacto. Se reporta que parece no usarse.
- Playwright limpió `frontend/test-results/`, cuyas trazas históricas están versionadas como evidencia. Se restauraron con `git restore -- frontend/test-results`. Queda sin versionar el nuevo `frontend/test-results/.last-run.json`.

> ⚠️ **La contraseña dev sigue en el historial de git** por `run-backend-etapa2.cmd`. Hay que rotarla si esa BD se usa fuera del entorno local.

---

## 3. Documentación
| Archivo | Cambio |
|---|---|
| `docs/EC3/GUIA_MAESTRA_EJECUCION_Y_PRUEBAS_EC3.md` | §9.2: BVA3-USR-PWD-BCRYPT pasa a «400 tras FIX-01». §16: DIV-01 **Cerrado** (UI 300 = dominio 300 en `Project.java`; solo la columna de BD admite 500) y DIV-E4-01 **Corregido**. Nuevas §16.1 (FIX-01..09), §16.2 (LIM-01..08) y nota de conteo de tests. |
| `docs/EC3/BASE_DOCUMENTAL_INFORME_EC3.md` | Referencias a DIV-01 y DIV-E4-01 actualizadas en las secciones de reglas, EP/BVA3, divergencias y §10.5 pendientes. Aclaración de que las 25 IT están incluidas en el total del backend. |
| `docs/EC3/PLAN_PRUEBAS_VALIDACION_EC3.md` (**nuevo**) | Plan manual autosuficiente, secciones 0–5: registro y evidencias; entorno en Bash y PowerShell, incluida la creación de `gestor_tareas_test`; suites AUTO-01..06 con resultados de referencia; casos AUT, USR, PROY, ST-PROY-01..15, ST-TAR-01..09, TAR, WBS, ASG, R1–R9, FIX-01..09 y LIM-01..08; SQL de solo lectura validado contra la BD; reparto por persona y checklist. |
| `docs/EC3/REGISTRO_SESION_2026-11-18_REVISION_VALIDACION.md` (**nuevo**) | Este documento. |

### Limitaciones conocidas **no corregidas** (documentadas como LIM-01..08)
1. Crear una tarea no valida el tope de presupuesto.
2. Tareas y subtareas no verifican que el proyecto esté abierto.
3. El motivo del Jefe (US1.3) no es obligatorio.
4. Completar una tarea no exige subtareas cerradas.
5. `editDetails` funciona en proyectos cerrados.
6. En `task-list`, la carga no es automática, el reorden envía IDs incorrectos y hay un botón sin acción.
7. Un correo duplicado devuelve 400 (no 409).
8. Una asignación devuelve 200 (no 201).

---

## 4. Cambios de entorno local (fuera del repositorio)
| Acción | Detalle |
|---|---|
| `npm ci` en `frontend/` | Faltaban `ng` y `@playwright/test`. |
| `npx playwright install chromium` | Chrome Headless Shell 145 (playwright build 1208) en `~/.cache/ms-playwright/`. |
| Creación de `gestor_tareas_test` | Se reutilizó `database/docker/init-dev-db.sql` cambiando solo el nombre de la base (`sed 's/\bgestor_tareas\b/gestor_tareas_test/g' … \| docker compose exec -T db psql -U postgres -v ON_ERROR_STOP=1`). Los roles ya existían y se omitieron. Migraciones aplicadas: **1,2,3,4,5,6**; 15 tablas en `task_manager`. Sin seed de usuarios (la IT crea los suyos). No se tocó `gestor_tareas` ni ningún volumen. |
| Datos de prueba en `gestor_tareas` (dev) | Playwright, el smoke curl y un usuario de prueba de 72 bytes crearon registros de prueba (proyectos, tareas y usuarios `e2e-*`). El DELETE del smoke usó una versión vieja, así que no borró nada. |

---

## 5. Resultados de verificación (medición real de esta sesión)
| Suite | Comando | Resultado |
|---|---|---|
| Backend IT aislada | `mvn -Dtest=PostgresIntegrationTest test` | **25/25 PASS**, BUILD SUCCESS |
| Backend completo | `mvn verify` (JDK 21) | **381 tests, 0 fallos, 0 errores, 0 omitidos, BUILD SUCCESS**. JaCoCo: INSTRUCTION 92.58 % (13279/14343), **BRANCH 85.09 % (622/731)**, **LINE 94.97 % (2721/2865)**, «All coverage checks have been met» |
| Backend sin IT (antes de crear la BD) | `mvn verify -Dtest='!PostgresIntegrationTest*'` | 356/356 PASS, pero BRANCH 84 % < 85 % → **el gate depende de la IT** |
| Frontend unit | `npx ng test --watch=false --coverage` | **20 archivos / 307 tests PASS**. Stmts 92.47 %, Branch 88.30 %, Funcs 86.62 %, Lines 93.51 % |
| Frontend build | `npx ng build` | OK. Solo avisos de presupuesto de bundle (583 kB inicial; CSS de `task-list` y `task-history` > 4 kB) |
| Playwright | `npx playwright test --project=chromium --reporter=line` | **14/14 PASS**. «Jefe crea usuario… puede iniciar sesión» falló 1 vez por una carrera tras «Salir» (formulario de login vacío) y pasó 3/3 en repetición: intermitente, sin relación con los cambios. |
| Smoke API (curl) | script local | Contraseña de 73 bytes → **400** ✔. `DELETE /api/tasks/{id}?version=V-1` → **412** ✔. `GET /api/tasks?projectId=11&q=T` → 1 resultado frente a 7 con `?q=T` global ✔ |

**Incidencias durante la sesión (resueltas):**
- `user-form.spec.ts` fallaba (2 tests) porque el correo de prueba tenía espacios y `Validators.email` lo rechazaba. Se corrigió el dato del test.
- La primera corrida de `mvn verify` dio 25 errores de contexto porque no existía `gestor_tareas_test`. Se resolvió creando la BD (§4).
- La primera corrida de Playwright dio 13 fallos porque faltaba Chromium. Se resolvió instalándolo (§4).

---

## 6. Pendientes
- [ ] **Commit**: no se hizo; queda a decisión del equipo.
- [ ] Rotar la contraseña dev expuesta en el historial (§2).
- [ ] Ejecutar `PLAN_PRUEBAS_VALIDACION_EC3.md` a mano, según el reparto por persona.
- [ ] Completar los datos de portada (`BASE_DOCUMENTAL_INFORME_EC3.md` §10.5).
- [ ] Decidir si se corrigen las limitaciones LIM-01..08 en otra iteración.
