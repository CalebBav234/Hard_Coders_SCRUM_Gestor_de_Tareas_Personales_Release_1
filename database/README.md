# Base de datos - PostgreSQL 18

Esta carpeta implementa la fundación de datos del manual operativo y añade las restricciones necesarias para que el backend no dependa solo de validaciones de interfaz.

## Qué incluye

- Base sugerida: `gestor_tareas`.
- Esquema: `task_manager`.
- Rol propietario de migraciones: `gestor_tareas_owner`.
- Rol del backend con permisos limitados: `gestor_tareas_app`.
- Siete tablas, cuatro vistas, índices, restricciones y triggers.
- Migraciones ordenadas y protegidas por versión + checksum.
- Borrado lógico para US-07 e historial unificado de terminadas/eliminadas para US-12.
- Búsqueda por título o descripción para US-13 mediante consultas parametrizadas.

El diagrama está en [diagram/erd.mmd](diagram/erd.mmd) y el detalle de columnas en [docs/data-dictionary.md](docs/data-dictionary.md).

## 1. Requisitos

En Windows:

1. PostgreSQL 18 instalado y el servicio iniciado.
2. `psql.exe` disponible en `PATH` o en `C:\Program Files\PostgreSQL\18\bin\psql.exe`.
3. La contraseña del usuario administrador `postgres` definida durante la instalación.
4. PowerShell 5.1 o posterior.

No se necesita Java, Maven, Angular ni Docker para completar esta fase.

## 2. Crear la base local

Abre PowerShell en la raíz del repositorio y ejecuta:

```powershell
.\database\scripts\setup-local.ps1
```

El script pedirá tres contraseñas sin mostrarlas:

1. La contraseña existente de `postgres`.
2. Una contraseña nueva para `gestor_tareas_owner`.
3. Una contraseña nueva para `gestor_tareas_app`.

Usa contraseñas locales distintas y compártelas con el equipo por un canal seguro, nunca por Git, Trello, capturas o mensajes públicos.

El script puede repetirse: actualiza las contraseñas de los roles, conserva la base y omite las migraciones ya aplicadas si sus checksums coinciden.

Si PostgreSQL escucha en otro puerto:

```powershell
.\database\scripts\setup-local.ps1 -Port 5433
```

## 3. Verificar

Con la contraseña de `gestor_tareas_app`:

```powershell
.\database\scripts\verify-local.ps1
```

El resultado correcto debe mostrar:

- PostgreSQL mayor 18.
- Zona horaria `UTC`.
- Migraciones V1, V2 y V3.
- Permisos de lectura, inserción y actualización sobre `tasks`.
- `can_hard_delete_tasks = false`.

Esta verificación comprueba la instalación y los permisos; no reemplaza las pruebas funcionales de historias de usuario, que pertenecen a una fase posterior.

## 4. Conectar pgAdmin 4

En el panel izquierdo:

1. Clic derecho en **Servers** → **Register** → **Server**.
2. En **General**, nombre: `Gestor Tareas Local`.
3. En **Connection**:
   - Host name/address: `localhost`
   - Port: `5432`
   - Maintenance database: `gestor_tareas`
   - Username: `gestor_tareas_app`
   - Password: la contraseña local del rol de aplicación
4. Guarda la conexión.
5. Navega a **Databases → gestor_tareas → Schemas → task_manager → Tables**.

Usa `gestor_tareas_owner` solamente para nuevas migraciones. Para consultas normales y para el backend usa `gestor_tareas_app`.

## 5. Conectar Spring Boot cuando exista el backend

Copia las variables de `.env.example` a tu entorno local. Nunca confirmes `.env`.

La configuración de referencia está en [examples/application.yml](examples/application.yml). El backend deberá incluir el driver oficial de PostgreSQL y mantener:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

`validate` obliga a que las entidades coincidan con el esquema y evita que Hibernate cambie tablas por su cuenta.

## 6. Añadir una migración

1. No edites una migración ya compartida o aplicada.
2. Crea el siguiente archivo, por ejemplo `V003__descripcion_breve.sql`.
3. Ejecuta:

```powershell
.\database\scripts\migrate.ps1
```

4. Ejecuta la verificación.
5. Incluye el SQL, el diccionario y el diagrama actualizados en el mismo Pull Request.

El runner crea `public.database_schema_migrations` y guarda versión, descripción, checksum, fecha y usuario. Si un archivo aplicado cambia, la ejecución se detiene para impedir divergencias entre integrantes.

## 7. Reglas para el backend

- Crear una tarea como `INACTIVA`, prioridad inicial `MEDIA`, `activated_at = NULL` y `completed_at = NULL`.
- Al activar: cambiar a `ACTIVA`, definir `activated_at` y abrir un `task_time_entry`.
- Al pausar: sumar el segmento a `total_active_seconds`, cerrar el registro de tiempo, limpiar `activated_at` y cambiar a `INACTIVA` dentro de una misma transacción.
- Al completar: cerrar el segmento, acumular tiempo, limpiar `activated_at`, definir `completed_at` y cambiar a `TERMINADA` en una misma transacción.
- Al reabrir: limpiar `completed_at`, definir `activated_at`, abrir segmento y cambiar a `ACTIVA`.
- Para eliminar en US-07: actualizar `deleted_at`; no emitir `DELETE FROM task_manager.tasks`.
- Usar `version` con `@Version` para evitar sobrescribir cambios concurrentes.
- Mostrar tareas normales desde `v_tasks`; pendientes desde `v_pending_tasks`.
- `v_task_archive` permite consultar terminadas y eliminadas desde pgAdmin. Para mantener compatibilidad con bases V001/V002, el backend obtiene ese mismo archivo desde `tasks` y `categories`; sus transiciones provienen de `v_task_history`.
- Aplicar la búsqueda en PostgreSQL sobre título y descripción, sin concatenar SQL recibido del usuario.

## 8. Pasos para cada integrante

1. Obtener la rama `develop` actualizada.
2. Instalar PostgreSQL 18 o acordar acceso a una instancia de desarrollo.
3. Ejecutar `setup-local.ps1` en su propia máquina.
4. Configurar sus variables locales; no reutilizar contraseñas personales.
5. Ejecutar `verify-local.ps1`.
6. Informar solo el mensaje de éxito o el error, nunca las contraseñas.

Cada integrante mantiene su propia base local. No se comparte la carpeta física de datos de PostgreSQL ni se suben respaldos al repositorio.
