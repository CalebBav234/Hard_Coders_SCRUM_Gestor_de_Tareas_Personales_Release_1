# Plan de pruebas de validación — EC3 (Etapas 1–6)

**Proyecto:** Gestor de Tareas Personales — Hard Coders
**Rama:** `Refactor-E2-testing` (base `ecd5f97` + correcciones FIX-01..09)
**Propósito:** validar a mano, y con las suites automatizadas, el estado del sistema antes de cerrar el informe EC3 (ANEXO 1), dejando evidencia trazable por persona.
**Documentos relacionados:**
- `GUIA_MAESTRA_EJECUCION_Y_PRUEBAS_EC3.md` (§9 catálogos, §16 divergencias, correcciones y limitaciones);
- `ETAPA_3_DISENO_PRUEBAS.md` (diseño de casos);
- `DIVISION_TRABAJO_EC3.md` (reparto).

> **Sin secretos.** Este documento no contiene contraseñas ni tokens. Los usuarios seed (`jefe`, `encargado1`, `encargado2`, `personal`, `inactivo`, en el dominio `@gestor.local`) y su contraseña de desarrollo están en `database/docker/seed-dev-users.sql`. No pegues la contraseña, el token ni los hashes en capturas ni en registros.

---

## 0. Objetivo y cómo registrar

### 0.1 Estados
| Estado | Uso |
|---|---|
| `PASS` | El observado coincide con el esperado. |
| `FAIL` | El observado difiere del esperado. Describe la diferencia y adjunta captura. |
| `BLOCKED` | No se pudo ejecutar por entorno o datos. Indica la causa. |

### 0.2 Plantilla de fila
Copia esta tabla en tu archivo de registro (`docs/EC3/evidencias/validacion_<persona>/REGISTRO.md`):

```md
| ID | Pasos (resumen) | Esperado | Observado | Estado | Evidencia | Fecha / HEAD |
|---|---|---|---|---|---|---|
| USR-05 | Crear usuario con contraseña de 73 "x" | 400 + mensaje "72 bytes" | ... | PASS | validacion_ana/USR-05.png | 2026-11-20 / ecd5f97+ |
```

### 0.3 Evidencias
- Las capturas van en `docs/EC3/evidencias/validacion_<persona>/<ID>.png` (por ejemplo `validacion_ana/ST-PROY-08.png`).
- Para los casos de API, guarda la salida de `curl` en `<ID>.txt`, con el token y la contraseña recortados.
- Obtén el HEAD con `git rev-parse --short HEAD`. Si tienes cambios sin commit, añade `+`.

---

## 1. Preparación del entorno

| Requisito | Verificación |
|---|---|
| JDK 21 | `java -version` → 21.x |
| Node ≥ 22.22.3 (exigido por Angular 22) | `node -v` |
| Docker con `gestor-tareas-db` healthy | `docker ps` → `(healthy)` |
| BD de pruebas `gestor_tareas_test` (solo para la IT) | consulta de abajo |
| Dependencias del frontend | `frontend/node_modules/.bin/ng` existe |
| Chromium de Playwright | `npx playwright install chromium` (solo si falta) |

**Bash (Linux/macOS)**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64      # ajustar a tu ruta
export PATH="$JAVA_HOME/bin:$PATH"
nvm use 22.22.3                                          # o la versión ≥22.22.3 instalada
docker compose up -d db && docker ps --filter name=gestor-tareas-db
docker compose exec db psql -U postgres -tAc "SELECT datname FROM pg_database WHERE datname LIKE 'gestor_tareas%';"
cd frontend && npm ci && cd ..
# Terminal 1 — backend (puerto 8081)
cd backend && mvn spring-boot:run
# Terminal 2 — frontend (http://localhost:4200, proxy /api → 8081)
cd frontend && npm start
```

**PowerShell (Windows)**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"          # ajustar a tu ruta
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
nvm use 22.22.3
docker compose up -d db; docker ps --filter name=gestor-tareas-db
docker compose exec db psql -U postgres -tAc "SELECT datname FROM pg_database WHERE datname LIKE 'gestor_tareas%';"
cd frontend; npm ci; cd ..
# Terminal 1
cd backend; mvn spring-boot:run
# Terminal 2
cd frontend; npm start
```

> Si `gestor_tareas_test` no aparece, créala en tu Docker **local**. El script de inicialización se reutiliza cambiando solo el nombre de la base; los roles ya existentes se omiten:
> ```bash
> sed 's/\bgestor_tareas\b/gestor_tareas_test/g' database/docker/init-dev-db.sql \
>   | docker compose exec -T db psql -U postgres -v ON_ERROR_STOP=1 -q
> docker compose exec -T db psql -U postgres -d gestor_tareas_test -tAc \
>   "SELECT string_agg(version::text, ',' ORDER BY version) FROM public.database_schema_migrations;"   # → 1,2,3,4,5,6
> ```
> En PowerShell: `(Get-Content database/docker/init-dev-db.sql) -replace '\bgestor_tareas\b','gestor_tareas_test' | docker compose exec -T db psql -U postgres -v ON_ERROR_STOP=1 -q`.
> No hace falta el seed de usuarios: la IT crea los suyos. Nunca uses `docker compose down -v`.

**Token para los casos de API.** Se usa en ST-PROY, ST-TAR, ASG y LIM. Escribe la contraseña de forma interactiva para que no quede en el historial:
```bash
read -rs PW; API=http://localhost:8081/api
TOKEN=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"jefe@gestor.local\",\"password\":\"$PW\"}" | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d.get("token") or d.get("accessToken"))')
H="Authorization: Bearer $TOKEN"
```
Para otro rol, cambia el correo y guarda el token en otra variable (`TOKEN_PERSONAL`, …).

---

## 2. Suites automatizadas

| ID | Comando (desde la raíz) | Resultado de referencia (2026-11-18, este HEAD) | Qué capturar |
|---|---|---|---|
| AUTO-01 | `cd backend && mvn verify` | **381 tests, 0 fallos/errores/omitidos, BUILD SUCCESS** (376 de E6 + 5 nuevos de FIX; incluye las 25 IT). JaCoCo: INSTRUCTION 92.58 % (13279/14343), **BRANCH 85.09 % (622/731)**, **LINE 94.97 % (2721/2865)**, «All coverage checks have been met» | Final de consola (`Tests run`, `BUILD …`) + `backend/target/site/jacoco/index.html` |
| AUTO-02 | `cd backend && mvn -Dtest=PostgresIntegrationTest test` | **25/25 PASS** con `gestor_tareas_test` | Línea `Tests run: 25` |
| AUTO-03 | `cd frontend && npx ng test --watch=false --coverage` | **20 archivos / 307 tests PASS**. Stmts 92.47 %, Branch 88.30 %, Funcs 86.62 %, Lines 93.51 % | Resumen + `frontend/coverage/index.html` |
| AUTO-04 | `cd frontend && npx ng build` | Build OK. Solo avisos de presupuesto de bundle (no son errores) | Línea `Application bundle generation complete` |
| AUTO-05 | `cd frontend && npx playwright test --project=chromium --reporter=line,html` (backend y frontend levantados) | **14/14 PASS** | Resumen + `frontend/playwright-report/index.html` |
| AUTO-06 | `bash docs/EC3/scripts/e2e_fase1.sh` | Script API+BD de Fase 1 (crea datos en `gestor_tareas`). **No cuenta como Playwright** | Salida del script |

> **Nota AUTO-01:**
> - El gate JaCoCo **depende de la IT**. Sin `gestor_tareas_test`, la IT falla al cargar el contexto y, sin ella, BRANCH baja a 84 %: 356/356 tests pasan, pero no se llega al 85 %.
> - Medición con la IT: 85.09 %.
> - Si la BD de pruebas no existe, créala como se indica en §1. Nunca bajes el umbral.
>
> **Conteo:** las 25 IT **ya forman parte** del total del backend. No las sumes aparte en el informe.
>
> **Playwright:** el test "Jefe crea usuario válido… puede iniciar sesión" falló una vez por una carrera tras «Salir» (formulario de login vacío) y pasó en las 3 repeticiones siguientes. Si vuelve a fallar, regístralo como `FAIL` con la traza `test-results/**/trace.zip`.

---

## 3. Casos manuales por bloque

Todo se hace en `http://localhost:4200` salvo que el caso diga «API». Los textos entre «» son las etiquetas reales de la UI.

### 3.1 AUT — Autenticación y roles
| ID | Pasos | Esperado |
|---|---|---|
| AUT-01 | Login como `jefe`: «Correo», «Contraseña», «Ingresar» | Entra a la app. El menú muestra «Usuarios». |
| AUT-02 | Login como `encargado1` | Entra. **No** aparece «Usuarios». |
| AUT-03 | Login como `personal` | Entra. No aparece «Usuarios». Ve «Mis tareas» sin presupuesto ni plan (US3.1). |
| AUT-04 | Login con una contraseña incorrecta | Mensaje de credenciales inválidas (401). Se queda en `/login`. |
| AUT-05 | Login como `inactivo` | Mensaje de usuario inactivo. No entra. |
| AUT-06 | Como `encargado1`, abrir `/users/new` | Redirige y no muestra el formulario. |
| AUT-07 | Como `personal`, abrir `/users/new` | Redirige y no muestra el formulario. |

### 3.2 USR — Creación de usuario (Jefe, `/users/new`)
Campos: «Correo», «Nombres», «Apellidos», «Contraseña». Botón «Crear usuario».
| ID | Pasos | Esperado |
|---|---|---|
| USR-01 | Contraseña de 7 caracteres → «Crear usuario» | Error «entre 8 y 200». No se envía. El botón sigue habilitado (FIX-08). |
| USR-02 | Contraseña de 8 | 201, mensaje de éxito |
| USR-03 | Contraseña de 9 | 201 |
| USR-04 | Contraseña de 72 «x» | 201 |
| USR-05 | Contraseña de 73 «x» | La UI muestra «no puede superar 72 bytes» y no envía. Por API (curl `POST /api/users`) → **400** (FIX-01). |
| USR-06 | Contraseña de 25 «€» (75 bytes, 25 caracteres) | Error de 72 bytes en la UI. API → 400. |
| USR-07 | API: contraseña de 200 y de 201 «x» | Ambas → 400. La de 200 también, porque supera 72 bytes. 201 → 400 por longitud. |
| USR-08 | Correo de 254 caracteres válido / 255 | 254 → 201. 255 → error en la UI / 400. |
| USR-09 | Nombres de 100 / 101 | 100 OK. 101 → error. |
| USR-10 | Correo ya registrado | Error visible. API → **400** (ver LIM-07). |
| USR-11 | Login con el usuario de USR-02 | Entra como Personal, sin «Usuarios». |
| USR-12 | API como `encargado1` y como `personal`: `POST /api/users` | 403 |

### 3.3 PROY — Formulario de proyecto (Jefe, «＋ Nuevo proyecto»)
Campos: «Nombre», «Descripción», «Encargado», «Presupuesto (Bs)», «Duración estimada (minutos)», «Motivo del plan inicial (opcional)».
| ID | Valor | Esperado |
|---|---|---|
| PROY-01 | Nombre de 159 / 160 / 161 | 159 y 160 OK. 161 no se puede escribir o da error. |
| PROY-02 | Nombre con solo espacios | Error de obligatorio |
| PROY-03 | Presupuesto -0.01 / 0 / 0.01 | -0.01 → error. 0 y 0.01 OK. |
| PROY-04 | Presupuesto 1.999 | Error de 2 decimales |
| PROY-05 | Duración -1 / 0 / 1 | -1 → error. 0 y 1 OK. |
| PROY-06 | Motivo de 300 / 301 | 300 OK. 301 no se puede escribir (UI). Por API → 400 (DIV-01 cerrado). |
| PROY-07 | En la lista de proyectos, contar «✎ Editar datos» por tarjeta | **Exactamente 1** (FIX-05) |

### 3.4 ST-PROY-01..15 — Estados de proyecto
- **Válidas (01–07):** en la tarjeta del proyecto, elige el estado en el select «Cambiar estado» y pulsa «Aplicar». Prepara un proyecto nuevo para cada cadena.
- **Inválidas (08–15):** la UI solo ofrece las transiciones permitidas, así que estos casos se prueban por API:
```bash
P=<projectId>; V=$(curl -s $API/projects/$P -H "$H" | python3 -c 'import sys,json;print(json.load(sys.stdin)["version"])')
curl -s -w '\nHTTP %{http_code}\n' -X PUT $API/projects/$P/status -H "$H" -H 'Content-Type: application/json' -d "{\"status\":\"EN_PAUSA\",\"version\":$V}"
```
| ID | Desde → hacia | Medio | Esperado |
|---|---|---|---|
| ST-PROY-01 | PLANIFICADO → EN_EJECUCION | UI | 200, estado actualizado |
| ST-PROY-02 | PLANIFICADO → CANCELADO | UI | 200 |
| ST-PROY-03 | EN_EJECUCION → EN_PAUSA | UI | 200 |
| ST-PROY-04 | EN_EJECUCION → FINALIZADO | UI | 200 |
| ST-PROY-05 | EN_EJECUCION → CANCELADO | UI | 200 |
| ST-PROY-06 | EN_PAUSA → EN_EJECUCION | UI | 200 |
| ST-PROY-07 | EN_PAUSA → CANCELADO | UI | 200 |
| ST-PROY-08 | FINALIZADO → EN_EJECUCION | API | 409 `INVALID_PROJECT_TRANSITION` |
| ST-PROY-09 | CANCELADO → EN_EJECUCION | API | 409 |
| ST-PROY-10 | PLANIFICADO → FINALIZADO y → EN_PAUSA | API | 409 |
| ST-PROY-11 | EN_EJECUCION → PLANIFICADO | API | 409 |
| ST-PROY-12 | EN_PAUSA → PLANIFICADO y → FINALIZADO | API | 409 |
| ST-PROY-13 | Cualquiera, con el token de `encargado1` o `personal` | API | 403 (sin token: 401) |
| ST-PROY-14 | Abre el proyecto en dos pestañas y aplica un cambio en cada una (o usa API con `version` = V-1) | UI/API | 412 `VERSION_CONFLICT` en la segunda |
| ST-PROY-15 | `"status":"FOO"` | API | 400 `VALIDATION_ERROR` |

### 3.5 ST-TAR-01..09 — Estados de tarea y subtarea
Botones de la tarjeta de tarea: «▶ Activar», «⏸ Pausar», «✓ Completar», «↻ Reabrir», «🗑 Eliminar».
| ID | Pasos | Esperado |
|---|---|---|
| ST-TAR-01 | Tarea PENDIENTE → «▶ Activar» | EN_PROGRESO |
| ST-TAR-02 | EN_PROGRESO → «⏸ Pausar» | PENDIENTE |
| ST-TAR-03 | EN_PROGRESO → «✓ Completar» | COMPLETADA |
| ST-TAR-04 | COMPLETADA → «↻ Reabrir» | EN_PROGRESO |
| ST-TAR-05 | Tarea sin subtareas abiertas → «🗑 Eliminar» | CANCELADA (204) |
| ST-TAR-06 | Tarea con una subtarea PENDIENTE → «🗑 Eliminar» | 409 `TASK_HAS_SUBTASKS`; la tarea sigue igual |
| ST-TAR-07 | Tarea CANCELADA → cualquier transición (API `POST /api/tasks/{id}/start` con `{"version":V}`) | 409 |
| ST-TAR-08 | Subtarea: «▶ Activar» → «✓ Completar» → «↻ Reabrir», por Personal asignado o por el Jefe | Cada paso OK |
| ST-TAR-09 | API con el token de un `personal` **no asignado**: `POST /api/subtasks/{id}/start` `{"version":V}` | 403 |

### 3.6 TAR — Formulario de tarea («＋ Nueva tarea»)
Campos: «Título», «Duración estimada (horas)», «Presupuesto asignado (Bs)», «Prioridad».
| ID | Valor | Esperado |
|---|---|---|
| TAR-01 | Duración 0.25 | OK. En la BD queda `estimated_duration_minutes = 15` (§4). |
| TAR-02 | Duración 0.1 | Error de múltiplo de 0.25 |
| TAR-03 | Duración 0 | Según la regla vigente (0 permitido en BVA3-TAR-DUR-MIN). Registra lo observado. |
| TAR-04 | Presupuesto 250.50 / 250.505 | 2 decimales OK. 3 decimales → error. |
| TAR-05 | Título de 160 / 161 | 160 OK. 161 no se puede escribir (FIX-06). |
| TAR-06 | En «✎ Editar», descripción de 2000 / 2001 | 2000 OK. 2001 no se puede escribir (FIX-06). |

### 3.7 WBS — Etapas y actividades inline (BROWSER-11..14)
| ID | Pasos | Esperado |
|---|---|---|
| WBS-01 | «＋ Crear etapa» → «Nombre de la etapa» → crear. Abre DevTools ▸ Network. | **Una sola** llamada `POST /api/projects/{id}/stages` (R5). La etapa aparece en la lista. |
| WBS-02 | Etapa con nombre vacío o solo espacios | Rechazada, sin POST |
| WBS-03 | Crear una actividad dentro de la etapa («Nombre de la actividad») | Un `POST /api/stages/{id}/activities`. Aparece bajo la etapa correcta. |
| WBS-04 | Actividad vacía o de longitud excesiva | Rechazada |

### 3.8 ASG — Asignación (DT-ASG-R1..R10)
En la UI: «Asignar nuevo Personal», campos «Trabajador» y «Motivo (opcional)». Por API: `POST /api/tasks/{taskId}/assignments` con `{"workerUserId":N,"reason":"..."}`.
| ID | Caso | Esperado |
|---|---|---|
| ASG-01 (R1) | Motivo explícito `"  Refuerzo  "` | 200 (ver LIM-08). En la BD queda `reason='Refuerzo'` (trim). |
| ASG-02 (R1) | Sin motivo | En la BD queda `reason='Asignación de Personal'` |
| ASG-03 | Motivo de 500 / 501 | 500 OK. 501 → 400. |
| ASG-04 (R2) | API con el token de `personal` | 403 |
| ASG-05 (R3) | Tarea de un proyecto FINALIZADO o CANCELADO | 409 |
| ASG-06 (R4) | Trabajador `inactivo` | 400 |
| ASG-07 (R5) | Autoasignación del actor | 403 |
| ASG-08 (R6) | Repetir la misma asignación | 400 o 409; no se duplica la fila |
| ASG-09 (R7/R8) | `taskId` o `workerUserId` inexistentes | 404 / 400 |
| ASG-10 (R10) | Sin token | 401 |

### 3.9 REG — Regresión R1–R9
| ID | Pasos observables | Esperado |
|---|---|---|
| R1 | Buscador global: buscar «tarea» y «TÁREA» | Mismos resultados (insensible a acentos y mayúsculas). Con un proyecto seleccionado, la búsqueda filtra solo ese proyecto (FIX-03). |
| R2 | Crear una tarea de 1.5 h y verla en la lista | Se muestra 1.5 h / 90 min. En la BD, 90. |
| R3 | Abrir directamente la URL de un proyecto o tarea y refrescar (F5) | Mantiene el contexto Proyecto › Etapa › Actividad |
| R4 | Crear Proyecto → Etapa → Actividad → Tarea → Subtarea | Todo persiste y es visible tras F5 |
| R5 | = WBS-01 | Un solo POST |
| R6 | Reordenar etapas o actividades y refrescar | El orden persiste. En task-list, ver LIM-06. |
| R7 | Expandir o contraer una etapa tras crear una actividad | Los contadores se actualizan sin recargar |
| R8 | Revisar los mensajes de validación de los formularios | Claros y coherentes con los límites |
| R9 | `mvn verify` → `ProjectTest`, `TaskTest` | PASS |

### 3.10 FIX — Regresión de las correcciones
| ID | Corrección | Cómo verificar |
|---|---|---|
| FIX-01 | Contraseña >72 bytes → 400 | USR-05 (API) |
| FIX-02 | Motivo del Jefe demasiado largo → 400 | Jefe **no asignado**: `POST /api/tasks/{id}/start` con `{"version":V,"reason":"<474 x>"}` → 400. Con 473 → 200. |
| FIX-03 | `?projectId&q` filtra | Comparar `GET /api/tasks?projectId=P&q=T` con `GET /api/tasks?q=T`: la primera devuelve solo tareas de P (menos resultados) |
| FIX-04 | Cancelar verifica la versión | `DELETE /api/tasks/{id}?version=<V-1>` → 412, la tarea no cambia. En la UI: dos pestañas, reabrir en una y «🗑 Eliminar» en la otra → error de conflicto. |
| FIX-05 | Sin botón duplicado | PROY-07 |
| FIX-06 | Límites de título y descripción | TAR-05, TAR-06 |
| FIX-07 | Sin logs de depuración | DevTools ▸ Console: al pulsar «✎ Editar» y «＋ Subtarea» no aparece `TASK HOME RECIBIÓ…` |
| FIX-08 | Formulario de usuario | USR-01 (botón habilitado y errores al enviar) y USR-05/06 (bytes) |
| FIX-09 | E2E main-flow | AUTO-05 en verde |

### 3.11 LIM — Limitaciones conocidas (dejar constancia)
Esperado: **comportamiento actual documentado** (`GUIA_MAESTRA` §16.2). Registra `PASS` si lo observado coincide con la descripción de la limitación, y `FAIL` si el sistema se comporta de otra forma.
| ID | Pasos | Comportamiento actual esperado |
|---|---|---|
| LIM-01 | Crear una tarea con presupuesto mayor al disponible del proyecto | Se acepta |
| LIM-02 | Proyecto FINALIZADO: crear una tarea o subtarea por API | Se acepta |
| LIM-03 | Jefe no asignado: «▶ Activar» sin motivo | OK; historial «Override del Jefe (US1.3): sin motivo indicado» |
| LIM-04 | «✓ Completar» una tarea con subtareas abiertas | Se completa |
| LIM-05 | «✎ Editar datos» en un proyecto FINALIZADO (o `PUT /api/projects/{id}` por API) | Se acepta |
| LIM-06 | En task-list: seleccionar una actividad, reordenar, pulsar todos los botones | No carga automáticamente, el reorden no persiste y un botón no tiene acción |
| LIM-07 | = USR-10 | 400, no 409 |
| LIM-08 | = ASG-01 | 200, no 201 |

---

## 4. Consultas SQL de verificación (solo lectura)

```bash
docker compose exec db psql -U postgres -d gestor_tareas -c "<consulta>"
```

```sql
-- Motivo de asignación (ASG-01/02/03)
SELECT task_id, worker_user_id, reason, length(reason) FROM task_manager.task_assignment ORDER BY task_id DESC LIMIT 10;

-- Duración y presupuesto de tarea (TAR-01/04, R2)
SELECT task_id, title, estimated_duration_minutes, allocated_budget_bob FROM task_manager.task ORDER BY task_id DESC LIMIT 10;

-- Historial de estado de tarea (ST-TAR, FIX-02, LIM-03)
SELECT h.task_id, ps.name AS desde, ns.name AS hacia, h.reason, h.changed_at
FROM task_manager.task_status_history h
LEFT JOIN task_manager.work_status ps ON ps.work_status_id = h.previous_status_id
JOIN task_manager.work_status ns ON ns.work_status_id = h.new_status_id
ORDER BY h.changed_at DESC LIMIT 15;

-- Estado de proyecto (ST-PROY)
SELECT p.project_id, p.name, s.name AS estado, p.updated_at FROM task_manager.project p
JOIN task_manager.project_status s ON s.project_status_id = p.project_status_id ORDER BY p.project_id DESC LIMIT 10;

-- Usuario creado sin privilegios (USR-02)
SELECT user_id, email, is_chief, is_active FROM task_manager.app_user ORDER BY user_id DESC LIMIT 5;
```
> Si el nombre de alguna columna no coincide (por ejemplo `work_status_id`; la "versión" de la API se deriva de `updated_at`), revísalo con `\d task_manager.<tabla>`. No ejecutes `UPDATE`, `DELETE` ni `DROP`.

---

## 5. Reparto sugerido y cierre

### 5.1 Reparto (alineado con `DIVISION_TRABAJO_EC3.md`)
| Persona | Fase(s) | Bloques de este plan |
|---|---|---|
| Persona 1 | F1 — Correcciones y entorno | §1 entorno, FIX-01..09, AUTO-06 |
| Persona 4 | F2+F3 — Formularios y diseño | USR, PROY, TAR, WBS, ST-PROY-01..15 |
| Persona 5 | F4+F6 — Unit/IT, cobertura, regresión | AUTO-01..04, ST-TAR-01..09, ASG, REG R1–R9, §4 SQL |
| Persona 3 | F5 — E2E | AUTO-05, AUT-01..07, LIM-01..08 |
| Persona 2 | F7 — Documentación | Consolidar los registros, actualizar `BASE_DOCUMENTAL_INFORME_EC3.md` y completar los datos de portada |

### 5.2 Checklist de cierre
- [ ] §1: entorno verificado y `gestor_tareas_test` disponible (o AUTO-01/02 marcados `BLOCKED` con la causa)
- [ ] AUTO-01..06 registrados con salida y artefactos
- [ ] Bloques AUT, USR, PROY, ST-PROY, ST-TAR, TAR, WBS, ASG, REG, FIX y LIM registrados con estado y evidencia
- [ ] Ninguna captura ni registro con contraseña, token o hash
- [ ] Cifras del informe tomadas de esta ejecución (sin contar dos veces las IT)
- [ ] `FAIL` y `BLOCKED` reportados al equipo con su causa
- [ ] Datos de portada pendientes (`BASE_DOCUMENTAL_INFORME_EC3.md` §10.5): título oficial, asignatura, grupo, docente, integrantes y fecha de entrega
