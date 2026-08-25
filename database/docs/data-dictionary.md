# Diccionario de datos

## Criterios transversales

- Claves primarias `bigint identity`, compatibles con JPA `GenerationType.IDENTITY`.
- Fechas con `timestamptz`; la base trabaja en UTC y la interfaz convierte a la zona del usuario.
- Estados persistidos: `INACTIVA`, `ACTIVA`, `TERMINADA`.
- Prioridades persistidas: `ALTA`, `MEDIA`, `BAJA`.
- `deleted_at` implementa borrado lógico. La cuenta de la aplicación no puede ejecutar `DELETE` físico sobre tareas, categorías o etiquetas.
- `version` se reserva para `@Version` y bloqueo optimista en JPA.

## `task_manager.tasks`

| Columna | Tipo | Nulo | Regla |
|---|---|---:|---|
| `id` | `bigint` | No | Identidad y clave primaria. |
| `title` | `varchar(160)` | No | Texto no vacío. |
| `description` | `text` | Sí | Máximo 4000 caracteres. |
| `status` | `varchar(16)` | No | `INACTIVA`, `ACTIVA` o `TERMINADA`. |
| `priority` | `varchar(8)` | No | `ALTA`, `MEDIA` o `BAJA`; valor inicial `MEDIA`. |
| `category_id` | `bigint` | Sí | Categoría; al borrarla físicamente queda `NULL`. |
| `parent_task_id` | `bigint` | Sí | Autorelación para subtareas; un trigger evita ciclos. |
| `activated_at` | `timestamptz` | Sí | Inicio del segmento actual; solo existe en estado `ACTIVA`. |
| `completed_at` | `timestamptz` | Sí | Última finalización; solo existe en estado `TERMINADA`. |
| `total_active_seconds` | `bigint` | No | Acumulado de segmentos ya cerrados, nunca negativo. |
| `created_at` | `timestamptz` | No | Creación. |
| `updated_at` | `timestamptz` | No | Actualización automática por trigger. |
| `deleted_at` | `timestamptz` | Sí | Borrado lógico. |
| `version` | `bigint` | No | Bloqueo optimista desde JPA. |

## Organización

### `task_manager.categories`

Catálogo opcional con nombre único sin distinguir mayúsculas/minúsculas. Incluye auditoría básica y borrado lógico.

### `task_manager.tags`

Catálogo de etiquetas con nombre único sin distinguir mayúsculas/minúsculas. La relación N:M vive en `task_tags`.

### `task_manager.task_tags`

Tabla puente con clave compuesta `(task_id, tag_id)` y fecha de asignación.

## Historial y tiempo

### `task_manager.task_status_history`

El trigger de `tasks` registra la creación y cada cambio real de estado. `change_reason` queda disponible para que el servicio añada contexto en una evolución posterior.

### `task_manager.task_time_entries`

Cada activación abre un segmento (`ended_at IS NULL`) y cada pausa o finalización lo cierra. Un índice único parcial impide más de un segmento abierto por tarea.

## Relaciones

### `task_manager.task_relations`

Relaciona dos tareas diferentes como `RELACIONADA`, `BLOQUEA` o `DEPENDE_DE`. No admite duplicar la misma relación orientada.

## Vistas

- `v_tasks`: tareas no eliminadas, con categoría, etiquetas y tiempo activo efectivo.
- `v_pending_tasks`: `INACTIVA` + `ACTIVA`; pendientes no es un cuarto estado.
- `v_task_history`: transiciones históricas, incluso si la tarea tiene borrado lógico.
