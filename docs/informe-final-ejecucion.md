# Informe final de ejecución — Plan de reestructuración hexagonal

## 1. Causa raíz del fallo del backend y reparación

**Síntoma:** `NoUniqueBeanDefinitionException: No qualifying bean of type 'CurrentActor' available: expected single matching bean but found 2: securityCurrentActorAdapter,localUserActorAdapter`.

**Causa raíz:** `backend/target` conservaba clases compiladas de una versión anterior del árbol de fuentes. `target/classes/.../security/LocalUserActorAdapter.class` (compilado el 10/10 a las 02:25, antes del commit `7831cd7` que eliminó `LocalUserActorAdapter.java`) seguía en disco, mientras que el adaptador vigente se había renombrado a `SecurityCurrentActorAdapter`. El escaneo de componentes encontraba dos beans `@Component` que implementan `CurrentActor` y el contexto de Spring no arrancaba.

**Reparación:** `mvn clean test` (y `mvn clean spring-boot:run` en desarrollo) elimina el artefacto obsoleto. No hizo falta reescribir código, dependencias ni configuración.

## 2. Comandos de arranque probados (PowerShell)

```powershell
# Base de datos (conserva datos; NUNCA `docker compose down -v`)
docker start gestor-tareas-db

# Seed (idempotente por ON CONFLICT; apunta a la base correcta, que `psql -U postgres` omitía)
docker cp database/docker/seed-dev-users.sql gestor-tareas-db:/tmp/seed.sql
docker exec -i gestor-tareas-db psql -U postgres -d gestor_tareas -f /tmp/seed.sql

# Backend (desde backend/)
$env:SPRING_DATASOURCE_PASSWORD='apppass'
mvn clean spring-boot:run

# Frontend (desde frontend/)
npm ci
npm start
```

### Si el puerto 8080 está ocupado

Síntoma exacto: `Web server failed to start. Port 8080 was already in use.`

Se diagnosticó con `Get-NetTCPConnection -State Listen | Where-Object LocalPort -eq 8080`, que identificó un `java.exe` residual (PID variable) ejecutando `com.hardcoders.taskmanager.TaskManagerApplication`, hijo huérfano de un `spring-boot:run` anterior cuyo padre Maven ya había terminado. Ese proceso viejo es el que ocupa el puerto; no es ningún servicio compartido que deba conservarse.

Dos salidas, ambas probadas:

```powershell
# Opción A: liberar el puerto (recomendada)
Get-NetTCPConnection -State Listen | Where-Object LocalPort -eq 8080 |
  ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }

# Opción B: override local sin tocar el puerto por defecto del proyecto
mvn clean spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=8081"
# Verificado: login OK en http://localhost:8081
```

Nota: la opción B sube el puerto solo en esa ejecución, mediante una propiedad de línea de comandos. No modifica `application.yml` ni los puertos publicados en `docker-compose.yml`, así que el contrato por defecto del proyecto (8080) sigue intacto para el resto del equipo.

**Por qué se repite el conflicto.** `mvn spring-boot:run` lanza la aplicación en un proceso `java` hijo. Si se detiene solo el padre (Maven, por ejemplo cerrando la terminal o matando el árbol desde fuera), el hijo sobrevive como huérfano y sigue escuchando en el 8080. Es la causa de los conflictos repetidos que aparecieron en esta sesión. Al cerrar, conviene limpiar también el hijo:

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object CommandLine -match 'TaskManagerApplication' |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

## 3. Instalación del frontend (`npm ci` fallaba con EPERM)

**Síntoma:** `npm error code EPERM / syscall unlink / path .../node_modules/@esbuild/win32-x64/esbuild.exe`, seguido de `Could not find the '@angular/build:dev-server' builder's node package` al ejecutar `npm start`.

**Causa:** no era un problema de permisos. Un `ng serve` seguía en marcha y el sistema operativo le impedía a `npm ci` desbloquear `esbuild.exe` para borrar `node_modules`. `npm ci` siempre borra la carpeta completa antes de instalar, así que la operación abortó a mitad y dejó la instalación incompleta (faltaban `typescript` y `.package-lock.json`, con solo 26 entradas de primer nivel). De ahí el segundo error: el builder `dev-server` había quedado dentro de un `node_modules` a medio borrar.

**Reparación:** detener primero todo proceso `node`/`esbuild` asociado al frontend, y solo entonces reinstalar.

```powershell
# 1) Cerrar lo que bloquea los archivos
Get-CimInstance Win32_Process -Filter "Name='node.exe'" |
  Where-Object CommandLine -match 'frontend' | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Get-Process esbuild -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

# 2) Reinstalar limpio (npm ci necesita el lockfile, que estaba intacto)
npm ci
```

Resultado verificado: `added 377 packages, and audited 378 packages in 20s`; `typescript`, `@angular/build` (con el builder `dev-server`), `rxjs` y `esbuild` restaurados; `npm start` levanta `ng serve` y responde `HTTP 200` en http://localhost:4200.



## 3. Resultados reales

| Comando | Resultado |
|---|---|
| `mvn clean test` | `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0` — BUILD SUCCESS |
| `npx ng test --watch=false` | `Test Files 9 passed (9)`, `Tests 52 passed (52)` |
| `npx ng build --configuration production` | `Application bundle generation complete` — 445,48 kB, **sin errores ni avisos de presupuesto CSS** |
| `gt-e2e.ps1` (17 pasos contra servicios reales) | Proyecto ? etapa ? actividad ? tarea ? subtarea ? asignación ? ejecución del Personal ? override del Jefe ? progreso, todo `200/201` |
| Proxy `/api` desde `ng serve` | `GET /api/projects` ? `200` con datos reales de PostgreSQL |

## 4. Huecos del informe del equipo: resueltos

1. **"Mis tareas" usaba el primer proyecto como espacio de trabajo** — eliminados `WorkspacePersistenceAdapter`, `WorkspaceRepository` y `Workspace`. Crear una tarea ahora exige `activityId`; todas las lecturas exigen `projectId`. El selector de proyecto/etapa es obligatorio y, sin selección, la pantalla muestra un estado vacío en lugar de cargar datos ajenos.
2. **US1.3 (Jefe completa tareas de otros)** — implementado con `CompletionPolicy`: el Jefe ejecuta cualquier tarea o subtarea, y el motivo queda en `task_status_history` como `Override del Jefe (US1.3): <motivo>` (verificado en el paso 12 del E2E).
3. **`task-list.css` rompía el build de producción** — la hoja pasó de 22,08 kB a ~3 kB. Las primitivas compartidas se movieron a `src/design-system.css` (hoja global, sin presupuesto por componente) y se eliminaron las reglas duplicadas y sin usar. El build queda limpio, sin subir presupuestos arbitrariamente.
4. **Puerto 8080 ocupado al ejecutar `mvn clean spring-boot:run`** — el listener era un `java.exe` residual de un arranque anterior (`com.hardcoders.taskmanager.TaskManagerApplication`), hijo huérfano de un `spring-boot:run` cuyo padre Maven ya había terminado. Se detuvo y se verificó que el puerto queda libre. También se probó y documentó el override local `-Dspring-boot.run.jvmArguments=-Dserver.port=8081` (login OK en 8081), que no altera el puerto por defecto del proyecto. Al cerrar el backend conviene matar también el proceso `java` hijo, que es el que origina el conflicto.
5. **`npm ci` fallaba con `EPERM ... unlink esbuild.exe`** — no era un problema de permisos: un `ng serve` activo bloqueaba el binario, así que `npm ci` no pudo borrar `node_modules` y abortó dejando la instalación a medias (de ahí el error posterior de `@angular/build:dev-server`). Se detuvieron los procesos bloqueantes, se eliminó el `node_modules` corrupto y se reinstaló con `npm ci` (`added 377 packages`). `npm start` vuelve a funcionar.
6. **Cadena real UI ? proxy ? backend ? PostgreSQL verificada** — con `ng serve` y el backend en marcha, el inicio de sesión a través de `http://localhost:4200/api/auth/login` devuelve token válido y `GET http://localhost:4200/api/projects` responde `200` con los proyectos reales de PostgreSQL, igual que la llamada directa al `8080`. Confirma que el proxy de `proxy.conf.json` está bien configurado.


## 5. Correcciones adicionales encontradas y resueltas

- `getResultStream()` + `toList()` sobre consultas JPA provocaba `ClassCastException` en `findAll`, `findManagedBy` y `projectIdOf` (Hibernate 7 devuelve el stream, no la lista). Se pasó a `getResultList().stream()`.
- Los casos de uso se creaban sin envolver en transacción (`ProjectController` lanzaba `TransactionRequiredException`): se restauró `TransactionalUseCases.wrap(...)` en `UseCaseConfiguration`.

## 6. Estado por historia

| Historia | Capa | Estado |
|---|---|---|
| US1.1 plan/presupuesto | Backend + front + DB | Implementado y verificado (409 al bajar del total asignado) |
| US1.2 delegación | Backend + front + DB | Implementado y verificado (acceso anterior ? 404) |
| US1.3 override Jefe | Backend | Implementado y verificado (historial atribuido al Jefe) |
| US2.1 aislamiento | Backend + front | Verificado (404/arrays vacíos) |
| US2.2 estructura de 5 niveles | Backend + front + DB | Implementado (`stage`, `activity`, `task`) y verificado |
| US2.3 asignación de Personal | Backend + front + DB | Implementado (`task_assignment`, 403 propio, 400 duplicado) y verificado |
| US3.1 lectura asignada | Backend + front | Verificado (DTO sin campos financieros) |
| US3.2 ejecución autorizada | Backend + front | Verificado (403 si no asignado, `completed_by` correcto) |
| US3.3 progreso en cascada | Backend | Implementado (`ProgressPolicy`, solo trabajo hoja) y verificado |

## 7. URLs locales

- Frontend: http://localhost:4200
- Backend:  http://localhost:8080
- Base de datos: `gestor-tareas-db:5432`, base `gestor_tareas`, esquema `task_manager`
- Fixtures locales (solo desarrollo): `jefe@`, `encargado1@`, `encargado2@`, `personal@`, `inactivo@gestor.local` / `Gestor2026!`. El inactivo no puede autenticarse.

## 8. Documentación actualizada

- `.kilo/plans/hexagonal_restructure_plan.md` — registro de ejecución, matriz de progreso, verificación independiente del informe de Épica 1, comandos y resultados reales.
- `docs/api-v4.md` — nuevo contrato (proyecto/etapa/actividad/tarea/asignación/progreso), tabla de errores y advertencia de que ya no existe el "primer proyecto".
- `docs/epica-1.md` — resolución explícita de los tres huecos declarados y el filtrado de proyectos para el Personal.

## 9. Pendientes reales

- Informe PDF académico (entregable externo, no alojado en el repositorio).
- Despliegue remoto (fuera del alcance de esta tarea).
- La pantalla aún no expone formularios dedicados para crear etapas/actividades/asignaciones: los casos de uso y los servicios (`WbsService`) ya están listos y probados; la UI de gestión de la WBS quedó parcial (selector de proyecto/etapa y progreso sí integrados).

