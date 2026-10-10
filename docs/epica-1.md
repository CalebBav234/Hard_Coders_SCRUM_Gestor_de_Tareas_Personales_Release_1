# Épica 1 — Gestión global (Jefe): US1.1 y US1.2

Implementa la Épica 1 de `.kilo/plans/hexagonal_restructure_plan.md` y, como requisito previo, la autenticación JWT (TASK-001 / DEC-003).

## Autenticación (TASK-001)

- `POST /api/auth/login` `{ email, password }` → `{ token, expiresAt, user }`. Contraseñas BCrypt en `app_user.password_hash`; un usuario inactivo no puede entrar.
- Todas las demás rutas `/api/**` exigen `Authorization: Bearer <token>`. Sin token o con un token inválido responden **401**.
- El token (HS256) identifica al usuario (`sub = user_id`). En cada petición el rol (`is_chief`) y el estado (`is_active`) se vuelven a leer de la base, así que quitar el rol o desactivar a alguien tiene efecto inmediato.
- Configuración: `APP_JWT_SECRET` (mínimo 32 caracteres; obligatorio fuera de desarrollo) y `APP_JWT_EXPIRATION_MINUTES` (por defecto 480).
- Las tareas personales ("Mis tareas") ahora quedan atribuidas al usuario que inició sesión.

### Usuarios de desarrollo

`database/docker/seed-dev-users.sql` se carga automáticamente al crear el contenedor (`docker compose down -v && docker compose up -d`). La contraseña de todos es `Gestor2026!`:

| Correo | Rol |
|---|---|
| `jefe@gestor.local` | Jefe (`is_chief`) |
| `encargado1@gestor.local` | Usuario (Ana Paz) |
| `encargado2@gestor.local` | Usuario (Luis Vaca) |
| `personal@gestor.local` | Usuario (Pedro Rojas) |
| `inactivo@gestor.local` | Inactivo (no puede entrar ni recibir proyectos) |

Para cargarlos en una base que ya existe (sin perder datos):

```powershell
docker compose exec -T db psql -U postgres -f /docker-entrypoint-initdb.d/zz-seed-dev-users.sql
```

## Roles

| Actor | Cómo se determina | Proyectos |
|---|---|---|
| Jefe | `app_user.is_chief = true` | Ve todos; crea; cambia plan, estado y Encargado |
| Encargado | `project.manager_user_id` = el usuario | Ve los que dirige; solo edita nombre y descripción |
| Personal | Cualquier otro usuario activo | No ve proyectos (Épica 3) |

Los permisos los aplican los casos de uso con las políticas del dominio (`ProjectAccessPolicy`, `DelegationPolicy`, `BudgetPolicy`). La interfaz solo oculta las acciones no permitidas usando los indicadores `canEditDetails`, `canEditPlan`, `canDelegate` y `allowedStatuses` que envía el backend.

## US1.1 — Ciclo de vida y presupuesto

| Criterio | Resultado |
|---|---|
| AC-US1.1-01 | El Jefe crea el proyecto (`PLANIFICADO`) → **201** y revisión 1 en `project_plan_history`, en la misma transacción |
| AC-US1.1-02 | Un Encargado intenta cambiar el plan → **403** `FORBIDDEN` |
| AC-US1.1-03 | Presupuesto negativo o fuera de `DECIMAL(15,2)` → **400** `VALIDATION_ERROR` |
| AC-US1.1-04 | Presupuesto menor que la suma de `allocated_budget_bob` de las tareas no canceladas → **409** `BUDGET_BELOW_ALLOCATIONS` |
| AC-US1.1-05 | Sin revisión de plan, el trigger diferido de V004 revierte toda la transacción |

- Cada cambio de presupuesto o duración agrega una revisión (`revision_number` siguiente) con motivo obligatorio y el usuario que la hizo.
- Ciclo de vida: `PLANIFICADO → EN_EJECUCION ⇄ EN_PAUSA → FINALIZADO`, y cualquier estado abierto puede pasar a `CANCELADO`. Los proyectos cerrados no admiten cambios de plan ni delegación (**409** `INVALID_PROJECT_TRANSITION`).
- Concurrencia: `version` es el `updated_at` del proyecto; una versión vieja responde **412**.

## US1.2 — Delegación

| Criterio | Resultado |
|---|---|
| AC-US1.2-01 | El Jefe delega a un usuario activo → **200** y nueva revisión en `project_plan_history` |
| AC-US1.2-02 | Un Encargado intenta delegar (incluso a sí mismo) → **403** |
| AC-US1.2-03 | Destino inactivo o inexistente → **400** |
| AC-US1.2-04 | La revisión registra "Delegación de X a Y: motivo" con `changed_by_user_id` del Jefe; el historial es de solo inserción |

V4 no tiene una tabla de delegaciones; por eso la delegación se audita como una revisión del plan (mismo presupuesto y duración), tal como pide el criterio AC-US1.2-01.

## API

| Método | URL | Quién | Cuerpo |
|---|---|---|---|
| GET | `/api/projects` | Jefe: todos · Encargado: los suyos | — |
| GET | `/api/projects/{id}` | Quien puede verlo (si no, **404**) | — |
| GET | `/api/projects/{id}/plan-history` | Quien puede verlo | — |
| GET | `/api/projects/eligible-managers` | Jefe | — |
| POST | `/api/projects` | Jefe | `{ name, description?, managerUserId, budgetBob, estimatedDurationMinutes, reason? }` |
| PUT | `/api/projects/{id}` | Jefe o su Encargado | `{ name, description?, version }` |
| PUT | `/api/projects/{id}/plan` | Jefe | `{ budgetBob, estimatedDurationMinutes, reason, version }` |
| PUT | `/api/projects/{id}/status` | Jefe | `{ status, version }` |
| POST | `/api/projects/{id}/delegate` | Jefe | `{ managerUserId, reason, version }` |

## Código

- **Dominio:** `domain/model/Project`, `ProjectStatus`, `PlanRevision`, `Actor`, `UserAccount`; `domain/policy/BudgetPolicy`, `DelegationPolicy`, `ProjectAccessPolicy`; puertos `ProjectRepository` y `UserDirectory`.
- **Aplicación:** `ManageProjectsUseCase` y `QueryProjectsUseCase`, implementados por `ProjectCommandService` y `ProjectQueryService`.
- **Adaptadores:** `adapter/in/security` (configuración JWT, login y actor actual), `adapter/in/web/project`, `adapter/out/persistence/ProjectPersistenceAdapter` y `UserDirectoryAdapter`.
- **Frontend:** `core/auth` (servicio, interceptor y guards), `features/auth/login` y `features/projects` (lista, formulario, plan, delegación e historial).

## Pruebas

- **Backend:** `BudgetPolicyTest`, `DelegationPolicyTest`, `ProjectTest`, `ProjectUseCasesTest` (criterios de aceptación con dobles en memoria) y `JwtTokenServiceTest`.
- **Frontend:** `auth.spec.ts` y `projects.spec.ts` (Vitest).

## Pendiente para otras historias

- US1.3 (excepciones del Jefe) y la Épica 2 (etapas, actividades y asignaciones) se apoyan en este actor y estas políticas.
- "Mis tareas" sigue usando como espacio de trabajo el primer proyecto (ver `docs/api-v4.md`). Cuando exista la Épica 2 (WBS), cada tarea deberá crearse dentro de su proyecto.
