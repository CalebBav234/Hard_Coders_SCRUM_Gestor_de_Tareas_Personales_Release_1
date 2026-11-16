# ETAPA 2 — Guía de pruebas y verificación (EC3 / Formularios y funcionalidades complementarias)

**Alcance:** A6/EC3-01 — operación CREATE de `app_user` (endpoint seguro, caso de uso, validaciones, persistencia, formulario accesible solo a Jefe) y verificación de flujos complementarios CREATE (`task_assignment`, etapa/actividad inline WBS). Sin instalación/configuración nueva de Playwright en el repositorio ni suite E2E formal de Etapa 5.  
**Ejecución navegador:** herramientas Playwright MCP preinstaladas en Kilo Code (`kilo-playwright_browser_*`) sobre navegador real.  
**ID ejecución:** `EC3-E2-20261115-2302`  
**Fecha/hora (local):** 2026-11-15 23:02–23:53 (aprox., -04:00)  
**Rama:** `Refactor-E2`  
**HEAD de partida:** `10988a31d81f7596947c93d1e3abc3ddb42028db` (cierre Fase 1)  
**Working tree:** no limpio (cambios/artefactos de Etapa 1/2 y logs locales presentes); **sin commit, push ni merge**.

> **Criterio:** evidencia real observada (código, compilación, tests automatizados, API, PostgreSQL, navegador). No se inventan resultados. **No se documentan secretos** (contraseñas, tokens JWT, cookies, hashes completos). Cuentas seed/dev y datos ficticios usados solo en entorno aislado.

---

## 1. Gate de entrada y contexto

| Criterio | Evidencia | Estado |
|---|---|---|
| Rama `Refactor-E2` / HEAD / working tree inspeccionados | `git branch --show-current` → `Refactor-E2`; `git rev-parse HEAD` → `10988a31…`; WT sucio registrado. | OK |
| Etapa 1 aprobada y revisada | `docs/EC3/ETAPA_1_GUIA_PRUEBAS.md` en **PASS** (EC3-04/02/03/11/12/13, R2, US3.1). Contratos heredados preservados. | PASS (entrada) |
| Estado real inspeccionado | `app_user`/`UserController`/`UserCommandService`/`UserWriter`/`PasswordHasher`, auth JWT + BCrypt (`SecurityConfiguration`, `AuthController`), guards/nav, `task_assignment` + `reason` (V006), WBS inline (`wbs-panel`) revisados. | OK |
| Contrato heredado | Sin cambios en V005/V006 ni migraciones nuevas; `task_assignment.reason` (normalización/default) intacto; sin `jakarta.*` añadido a application/domain por A6; US3.1 sin regresión funcional en alcance verificado. | OK |

**Entorno:** Windows/PowerShell, Docker 28.4.0, PostgreSQL 18 `gestor-tareas-db` (`gestor_tareas`, `task_manager`, V001–V006), Maven 3.9.16 + JDK 21, Node v24.15.0/npm 11.12.1, Angular 22.1.3. Backend `http://localhost:8081` (`ddl-auto: validate`, proxy `/api` desde frontend `http://localhost:4200`). Servicios reiniciados con build actualizado; `docker compose down -v` **no** ejecutado.

---

## 2. Alcance implementado (A6 / EC3-01 — CREATE `app_user`)

### 2.1 Backend (hexagonal)
- **`POST /api/users`** (`adapter/in/web/user/UserController`): autenticado + Jefe (`@PreAuthorize` / `ProjectAccessPolicy`), `201 Created`, `Location /api/users/{id}`.
- **Web DTOs:** `UserRequests.CreateUserRequest` valida email/nombres/contraseña (límites reales, no vacíos). `UserResponses.UserResponse` expone solo `userId,email,givenNames,familyNames,fullName,chief,active` (sin `password`/hash/token).
- **Application/domain:** `ManageUsersUseCase`/`CreateUserCommand`, `UserCommandService` (autorización de negocio, normaliza email trim+lowercase, valida/recorta nombres y política de contraseña, unicidad pre-check + carrera BD → duplicado, fuerza `isChief=false,isActive=true`, `PasswordHasher`).
- **Out:** `SpringSecurityPasswordHasher` (BCrypt/`PasswordEncoder` real compatible con login), `UserWriterAdapter`/`AppUserEntity` persistencia; wiring en `UseCaseConfiguration`.
- **Reglas:** no se aceptan campos privilegiados; sin CRUD adicional.

### 2.2 Frontend
- **Ruta/guard:** `/users/new` con `authGuard + chiefGuard` (redirige a `/` si no Jefe). Nav “Usuarios” solo para Jefe.
- **Formulario:** `features/users/user-form/user-form.ts` reactivo, labels accesibles, validaciones, `saving`/anti doble envío, mensajes éxito/error (`apiErrorMessage`), reseteo tras éxito sin retener contraseña.
- **Servicio/modelo:** `user.service.ts` (`POST /api/users`), `user.ts` sin campos sensibles.

---

## 3. Ejecución automatizada (real, esta sesión)

| Categoría | Ejecutados | Aprobados | Fallidos | Skipped |
|---|---:|---:|---:|---:|
| Backend compile (`mvn -DskipTests compile`, JDK 21) | 1 | 1 | 0 | 0 |
| Backend nuevas A6 (`UserCommandServiceTest,UserControllerTest`) | 8 | 8 | 0 | 0 |
| Backend suite completa (`mvn test`) | 337 | 337 | 0 | 0 |
| Frontend `tsc` (app + spec) | 2 checks | 2 | 0 | 0 |
| Frontend `ng test --watch=false` | 297 | 297 | 0 | 0 |
| API funcionales A6 + complementarios (PostgreSQL real) | 22 | 22 | 0 | 0 |
| Verificaciones BD/persistencia | 10 | 10 | 0 | 0 |
| Navegador BROWSER-01..14 (Playwright MCP) | 14 | 14 | 0 | 0 |

**Notas:** suite backend total 337/337 incluye A6 y estado actual del árbol (0 fallos/errores). Frontend 18 archivos, 297/297. Cobertura/`mvn verify` jacoco gate fuera de Etapa 2.

---

## 4. Verificación API y persistencia (complementaria, PostgreSQL real)

| Verificación | Resultado |
|---|---|
| `POST /api/users` sin auth / Encargado / Personal | **401 / 403 / 403** |
| `POST /api/users` Jefe (válido) | **201**, campos públicos correctos, `chief=false,active=true`, sin `password`/hash |
| Normalización/trim y login del usuario creado | Email normalizado/recortado, nombres recortados, login `/api/auth/login` **OK** (activo, no jefe) |
| Duplicado, email inválido, contraseña corta, nombres vacíos/largo, email largo | **400** (rechazos), sin creación indebida |
| Hash almacenado (BCrypt) | Persistido hasheado en BD (no expuesto) |
| `task_assignment` motivo explícito (trim) | Persistido/leído `Motivo explicito B08 EC3-E2-20261115-2302` (UI+API+BD) |
| `task_assignment` motivo `null` (default) | Persistido/leído `Asignación de Personal` (UI+API+BD) |
| Asignaciones inválidas (B10) | 10A inactivo **400**, 10B inexistente **400**, 10C duplicada **400** (1ª ok), 10D autoasignación **403**, 10E proyecto cerrado **409** (revertido a PLANIFICADO), 10F no autorizado **403**, 10G tarea inexistente **404**; sin registros indebidos |
| Etapa/actividad inline (WBS) | `Etapa Browser 11` (stage 4) y `Actividad Browser 13` (activity 3, stage 4) creadas y persistidas; rechazos backend whitespace/>160 **400** |

**Estado final datos:** proyecto 1 `PLANIFICADO` (v=1794800980290507), tarea 1 sin asignaciones; usuarios de prueba `b02-ec3-e2-20261115-2302@test.local` y `b03dup-ec3-e2-20261115-2302@test.local` creados (`is_chief=false,is_active=true`).

---

## 5. Pruebas manuales en navegador (BROWSER-01..BROWSER-14) — todas PASS

**Herramienta:** Playwright MCP preinstalado en Kilo Code (sin instalar/configurar Playwright en proyecto). Evidencias en `docs/EC3/evidencias/etapa_2/browser/`.

| ID | Escenario | Rol | Resultado esperado | Resultado observado | Estado | Evidencia |
|---|---|---|---|---|---|---|
| BROWSER-01 | Acceso al formulario de creación de usuarios | Jefe | Acceso permitido | Login Jefe, nav “Usuarios” visible, `/users/new` accesible, formulario con labels/campos y botón deshabilitado sin datos válidos, sin errores de render. | **PASS** | `BROWSER-01-acceso-jefe.png` |
| BROWSER-02 | Creación exitosa de usuario | Jefe | CREATE exitoso y persistente | Alta UI de usuario único (`b02-ec3-e2-20261115-2302@test.local`): mensaje de éxito, formulario reseteado, sin exposición de contraseña/hash; login posterior del usuario creado OK; persistencia en BD. | **PASS** | `BROWSER-02-creacion-usuario.png` |
| BROWSER-03 | Validaciones del formulario de creación | Jefe | Entradas inválidas rechazadas | UI: validación de campo (correo vacío/formato) y submit deshabilitado con datos inválidos. Backend (PostgreSQL): 03A vacío 400, 03B formato 400, 03C duplicado 400 (`b03dup-ec3-e2-20261115-2302@test.local` preexistente), 03D nombre vacío 400, 03E apellido vacío 400, 03F/G longitud 400, 03H contraseña 400. Sin registros indebidos. | **PASS** | `BROWSER-03-validaciones-usuario.png` |
| BROWSER-04 | Login con usuario recién creado | Usuario creado | Login correcto y permisos correctos | Login OK (usuario no jefe/activo). Sin “Usuarios” ni acceso a `/users/new` (redirigido). `POST /api/users` con su sesión → **403**; intento bloqueado no persistido. | **PASS** | `BROWSER-04-login-usuario-creado.png` |
| BROWSER-05 | Encargado intenta crear usuarios | Encargado | 403 / acceso denegado | Nav sin “Usuarios”, `/users/new` bloqueado/redirigido. `POST /api/users` → **403**; sin persistencia (`b05-enc-*` no existe). | **PASS** | `BROWSER-05-bloqueo-encargado.png` |
| BROWSER-06 | Personal intenta crear usuarios | Personal | 403 / acceso denegado | Nav sin “Usuarios”, ruta protegida/redirigida. `POST /api/users` → **403**; sin persistencia (`b06-per-*` no existe). | **PASS** | `BROWSER-06-bloqueo-personal.png` |
| BROWSER-07 | Matriz de navegación y permisos por rol | Jefe/Encargado/Personal | Permisos coherentes | Jefe con “Usuarios” y acceso a formulario; Encargado/Personal sin opción ni acceso directo; backend mantiene 403. Vista Personal coherente con alcance asignado (US3.1 sin exposición de campos de plan/finanzas retirados en contrato verificado). | **PASS** | `BROWSER-07-jefe.png`, `BROWSER-07-encargado.png`, `BROWSER-07-personal.png` |
| BROWSER-08 | Crear asignación válida con motivo explícito | Jefe | Asignación persistida | UI: diálogo de asignación en tarea “Tarea Etapa2”, selección de trabajador y motivo explícito, envío exitoso (“Personal asignado”, contador actualizado). API/BD: asignación creada y motivo recortado persistido/leído. Recarga/consulta coherente. | **PASS** | `BROWSER-08-asignacion-motivo.png` |
| BROWSER-09 | Asignación sin motivo (default) | Jefe | DEFAULT_REASON persistido | UI: asignación enviada con motivo vacío/omitido, éxito visible. API/BD: `reason="Asignación de Personal"` persistido y leído tras operación. | **PASS** | `BROWSER-09-motivo-default.png` |
| BROWSER-10 | Rechazo de asignaciones inválidas | Varios | Operaciones inválidas rechazadas | 10A inactivo→400, 10B inexistente→400, 10C duplicada→400 (sin duplicado), 10D autoasignación→403, 10E proyecto CERRADO→409 y revertido a PLANIFICADO, 10F sin autorización→403, 10G tarea inexistente→404. Sin asignaciones inválidas persistidas. UI de asignación mantiene controles/errores coherentes con backend. | **PASS** | `BROWSER-10-asignaciones-invalidas.png` |
| BROWSER-11 | Crear etapa mediante formulario inline WBS | Jefe (autorizado) | Etapa persistida | Panel “Estructura del proyecto” (WBS), formulario inline “Nueva etapa” visible/editable, creación de **“Etapa Browser 11”** con mensaje de éxito; aparece en WBS/progreso y persistida en BD (stage 4). | **PASS** | `BROWSER-11-creacion-etapa.png` |
| BROWSER-12 | Validar formulario inline de etapa | Jefe (autorizado) | Entradas inválidas rechazadas | UI: “Crear etapa” deshabilitado con nombre vacío; formulario recuperable. Backend: nombre solo espacios → 400, nombre > límite (161) → 400. Sin etapas inválidas creadas. | **PASS** | `BROWSER-12-validacion-etapa.png` |
| BROWSER-13 | Crear actividad mediante formulario inline WBS | Jefe (autorizado) | Actividad persistida | Etapa “Etapa Browser 11” expandida, formulario inline de actividad, creación de **“Actividad Browser 13”** con notificación de éxito; listada bajo etapa correcta, reflejada en cascada y persistida en BD/API (activity 3, stage 4). | **PASS** | `BROWSER-13-creacion-actividad.png` |
| BROWSER-14 | Validar formulario inline de actividad | Jefe (autorizado) | Entradas inválidas rechazadas | UI: “Crear actividad” deshabilitado con nombre vacío en inline de etapa; estado consistente. Backend: nombre espacios → 400, nombre > límite → 400 para `POST /api/stages/{id}/activities`. Sin actividades inválidas. | **PASS** | `BROWSER-14-validacion-actividad.png` |

**Evidencias adicionales de apoyo:** `01-users-form-chief.png`, `02-user-created-success.png`, `03-non-chief-users-route-redirect.png`.

---

## 6. Defectos, correcciones y observaciones

- **Defectos corregidos en esta corrida:** ninguno en código fuente. No se alteraron reglas de negocio ni controles de seguridad para forzar resultados.
- **Incidencias operativas manejadas:** reinicio de frontend/backend para servir build actualizado; recarga/trigger de listado de tareas cuando la UI no reflejó datos cargados inicialmente; reversión de estado de proyecto en BD tras prueba 10-E porque la reapertura vía API devolvió conflicto de versión en ese punto (proyecto quedó **PLANIFICADO**, sin impacto en datos de prueba ajenos). Mensajes/alertas transitorios de UI no bloquearon la verificación final.
- **Limitaciones/alcance:** no se instaló Playwright en el repo ni se implementó suite E2E Etapa 5; `GET /api/users` no disponible (persistencia/resultado verificados por API/BD/login/UI). Cobertura/quality gate global no evaluados aquí.

---

## 7. Checklist de salida (Gate Etapa 2)

- [x] Gate de entrada (Etapa 1 PASS) y contexto/contratos revisados.
- [x] CREATE `app_user` operativo y alineado a arquitectura (endpoint/use case/puertos/adaptadores).
- [x] Autorización Jefe-only probada (UI+API 401/403/201), sin elevación de privilegios, usuario creado no jefe/activo.
- [x] Validaciones, unicidad, normalización y `PasswordEncoder` real (BCrypt) verificados; hash no expuesto; login del creado OK.
- [x] Formulario `/users/new` usable/accesible (loading, anti doble envío, errores, éxito, sin retener contraseña).
- [x] `task_assignment` CREATE verificado (motivo explícito y default, persistencia/lectura UI+API+BD).
- [x] Etapa/actividad inline WBS verificados (creación y validaciones/rechazos) sin duplicación de componentes.
- [x] Automatizados relevantes y API/BD/navegador ejecutados sin fallos; evidencias recopiladas sin secretos.
- [x] **BROWSER-01..BROWSER-14: 14/14 PASS.**

## 8. Estado final

**Estado: PASS.**

Todos los criterios funcionales y de seguridad de Etapa 2 (A6 CREATE `app_user`, autorización Jefe-only, no exposición de secretos, persistencia/login, y flujos complementarios `task_assignment` y etapa/actividad inline) se cumplen con evidencia real, y los **14 escenarios BROWSER-01..BROWSER-14** finalizaron en **PASS**. No se avanzó a Etapa 3, no se modificaron V005/V006, no se instaló/configuró Playwright en el proyecto y no se realizó commit/push/merge.
