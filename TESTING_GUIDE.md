# Guía de Pruebas — Hard Coders SCRUM Gestor de Tareas Personales Release 1

Este documento explica cómo ejecutar pruebas unitarias, generar reportes de cobertura y probar la aplicación manualmente en el navegador.

---

## 1. Requisitos Previos

| Herramienta | Versión | Propósito |
|---|---|---|
| Java | 21+ | Backend (Spring Boot) |
| Maven | 3.9+ | Build y pruebas backend |
| Node.js | 18+ | Frontend (Angular + Vitest) |
| npm | 9+ | Gestión de dependencias frontend |
| PostgreSQL | 18 | Base de datos |
| Docker (opcional) | | Ejecutar base de datos en contenedor |
| Navegador moderno | Chrome/Edge/Firefox | Pruebas manuales y E2E |

---

## 2. Pruebas del Backend (Java / Spring)

### 2.1 Ejecutar todas las pruebas unitarias

Desde la carpeta `backend`:

```powershell
cd backend
mvn test
```

Resultado esperado: **318 pruebas, 0 fallos, 0 errores** (313 originales + 5 nuevas para conteos jerárquicos).

### 2.2 Generar reporte de cobertura JaCoCo

```powershell
cd backend
mvn jacoco:report
```

Los reportes se generan en:
- HTML: `backend/target/site/jacoco/index.html`
- XML: `backend/target/site/jacoco/jacoco.xml`
- CSV: `backend/target/site/jacoco/jacoco.csv`

### 2.3 Verificar umbrales de cobertura

| Métrica | Umbral | Resultado real |
|---|---|---|
| Línea (Line coverage) | ≥85% | **92.97%** |
| Rama (Branch coverage) | ≥85% | **83.01%** (cercano al umbral tras agregar consultas de conteo nativas) |

> Nota: Rama quedó ligeramente por debajo del 85% debido a las nuevas ramas defensivas en los métodos de conteo y mapeo. Todas las pruebas pasan.

---

## 3. Pruebas del Frontend (Angular / Vitest)

### 3.1 Instalar dependencias

Desde la carpeta `frontend` (una sola vez):

```powershell
cd frontend
npm install
```

### 3.2 Ejecutar todas las pruebas unitarias

```powershell
cd frontend
npm test
# o equivalentemente:
npx vitest run
```

Resultado esperado: **261 pruebas originales + nuevas pruebas de cobertura (task-mapper, hierarchy-state, task-history, drag-drop, restoreRouteContext, template branches), 0 fallos**, en múltiples archivos de prueba.

### 3.3 Ejecutar pruebas con reporte de cobertura

```powershell
cd frontend
npx vitest run --coverage
```

Resultado esperado:
| Métrica | Umbral | Resultado real |
|---|---|---|
| Línea (Line coverage) | ≥85% | **90.34%** |
| Rama (Branch coverage) | ≥75%* | **83.74%** |

\* El umbral operativo solicitado es 75%; el umbral obligatorio del proyecto es 85%. Rama quedó en 83.74%, por encima de 75% y cerca de 85%, tras agregar pruebas de mapeo de tareas, estado jerárquico, drag-and-drop, restauración de enlaces profundos y condicionales de plantilla.

Los reportes de cobertura se generan en:
- HTML: `frontend/coverage/index.html`
- LCOV: `frontend/coverage/lcov.info`

### 3.4 Nuevos archivos de prueba agregados

- `frontend/src/app/core/services/task-mapper.spec.ts` — pruebas del mapeador de tareas, subtareas e historial.
- `frontend/src/app/core/services/hierarchy-state.service.spec.ts` — pruebas del servicio de estado jerárquico compartido.
- `frontend/src/app/features/tasks/wbs/wbs-panel.spec.ts` — pruebas de ordenamiento por arrastre con rollback y conteos.
- `frontend/src/app/core/services/task.service.spec.ts` — prueba de reorderTasks.
- `frontend/src/app/features/tasks/task-list/task-list.spec.ts` — pruebas de enlaces profundos, título de página, condicionales de plantilla.

### 3.3 Build de producción (verificación TypeScript/Angular)

```powershell
cd frontend
npm run build
```

Resultado esperado: **Éxito**, sin errores de compilación.

---

## 4. Pruebas Manuales en el Navegador (Smoke Tests)

### 4.1 Iniciar la base de datos

Asegúrate de que PostgreSQL esté ejecutándose en el puerto 5432 con la base de datos `gestor_tareas` y el esquema `task_manager`. Las credenciales esperadas son:

- Usuario: `gestor_tareas_app`
- Contraseña: `apppass`

Si usas Docker, el contenedor `gestor-tareas-db` debe estar activo.

### 4.2 Iniciar el backend

Desde la carpeta `backend`:

```powershell
cd backend
$env:SPRING_DATASOURCE_PASSWORD="apppass"
mvn spring-boot:run
```

Verificación: El backend debe iniciar en `http://localhost:8081` (el puerto se configuró en `backend/src/main/resources/application.yml` debido a que el puerto 8080 está ocupado por un proxy del entorno). Deberías ver logs con `Started TaskManagerApplication`.

La comunicación frontend→backend se proxyea a través de `frontend/proxy.conf.json` que apunta a `http://localhost:8081`.

### 4.3 Iniciar el frontend

Desde la carpeta `frontend`:

```powershell
cd frontend
ng serve
```

Verificación: El frontend debe iniciar en `http://localhost:4200`.

### 4.4 Cuadros de prueba de usuario

| Usuario | Correo | Contraseña | Rol |
|---|---|---|---|
| Jefe | jefe@gestor.local | Gestor2026! | Jefe (acceso total) |
| Encargado | encargado1@gestor.local | Gestor2026! | Encargado (proyectos asignados) |
| Personal | personal@gestor.local | Gestor2026! | Personal (solo tareas asignadas) |

---

## 5. Flujos de Prueba R1–R9 (E2E Manual)

### R1 — Búsqueda Global

**Objetivo:** Buscar proyectos, etapas, actividades y tareas por título/descripción.

**Pasos:**
1. Inicia sesión como Jefe.
2. Ve a "Mis tareas".
3. En el cuadro "Buscar tareas", escribe parte de un título (ej. "Construccion").
4. Haz clic en "Buscar".
5. Verifica que aparecen solo los resultados que coinciden.
6. Verifica que la búsqueda es insensible a mayúsculas y acentos (ej. "construccion" coincide con "Construcción").

**Resultado esperado:** Los resultados filtrados aparecen en pantalla sin recargar la página.

---

### R2 — Consistencia de Duración

**Objetivo:** Verificar que las duraciones se capturan y muestran correctamente.

**Pasos:**
1. Crea una nueva tarea desde "Crear nueva tarea".
2. Ingresa una duración estimada (ej. 60 minutos).
3. Guarda la tarea.
4. Verifica que la duración se muestra correctamente en la lista.

**Resultado esperado:** La duración se guarda y muestra con la unidad correcta (minutos).

---

### R3 — Navegación Directa Jerárquica

**Objetivo:** Acceder directamente a un proyecto, etapa, actividad o tarea mediante URL.

**Pasos:**
1. En la lista de proyectos, haz clic en "Ver tareas" de un proyecto.
2. La URL debería cambiar a algo como `/?projectId=18`.
3. Recarga la página (F5).
4. Verifica que el mismo proyecto siga seleccionado y se muestren sus etapas y tareas.

**Resultado esperado:** La navegación profunda funciona tras recargar la página.

---

### R4 — Creación Jerárquica

**Objetivo:** Crear proyecto → etapa → actividad → tarea → subtarea.

**Pasos:**
1. Ve a "Proyectos" y haz clic en "Nuevo proyecto".
2. Llena nombre, descripción, presupuesto y duración. Haz clic en "Crear proyecto".
3. Abre el proyecto creado y haz clic en "Nueva etapa".
4. Crea una etapa, luego una actividad dentro de esa etapa.
5. Crea una tarea dentro de la actividad.
6. Crea una subtarea dentro de la tarea.

**Resultado esperado:** Cada elemento se crea bajo su padre correcto sin huérfanos ni duplicados.

---

### R5 — Creación de Etapa con un Clic

**Objetivo:** Crear una etapa con una sola acción intencional.

**Pasos:**
1. Abre un proyecto.
2. Haz clic en "Nueva etapa".
3. Llena nombre y descripción.
4. Haz clic en "Crear".
5. Verifica en la pestaña de red del navegador (F12 → Network) que solo se envía **una** petición POST.

**Resultado esperado:** Se crea exactamente una etapa, con estado de carga mostrado y error manejado si falla.

---

### R6 — Ordenamiento por Arrastre (Drag-and-Drop)

**Objetivo:** Reordenar etapas, actividades y tareas y verificar que persiste.

**Pasos:**
1. En un proyecto con múltiples etapas, arrastra una etapa para cambiar su posición.
2. Recarga la página.
3. Verifica que el nuevo orden se mantuvo.
4. Repite para actividades y tareas dentro de una etapa.

**Resultado esperado:** El orden persiste en la base de datos tras recargar y navegar fuera y de vuelta.

---

### R7 — Conteos Expandidos y Colapsados

**Objetivo:** Verificar que los conteos de hijos se muestran y actualizan correctamente.

**Pasos:**
1. Expande una etapa y observa el conteo de actividades.
2. Colapsa la etapa y verifica que el conteo sigue visible.
3. Crea una nueva actividad dentro de esa etapa.
4. Verifica que el conteo se actualiza automáticamente.

**Resultado esperado:** Los conteos son correctos antes y después de crear/eliminar elementos.

---

### R8 — Flujo de Creación (UI/UX)

**Objetivo:** Verificar que la experiencia de creación es clara para un usuario nuevo.

**Pasos:**
1. Como usuario nuevo, ve a la pantalla principal.
2. Verifica que el contexto padre-proyecto es visible.
3. Crea un proyecto, luego una etapa, luego una actividad, luego una tarea.
4. Verifica que los mensajes de validación son claros y que hay feedback de éxito.

**Resultado esperado:** El usuario entiende qué está creando, dónde pertenece y qué hacer a continuación.

---

### R9 — Construcción de Entidades (Backend)

**Objetivo:** Verificar que los constructores de fábrica funcionan correctamente.

**Este se verifica mediante las pruebas unitarias del backend** (`mvn test`), que cubren:
- `Task.create()` y `Task.restore()`
- `Project.create()` y `Project.restore()`
- Validaciones de presupuesto, duración y prioridad

---

## 6. Pruebas de Autenticación y Autorización

| Escenario | Paso | Resultado esperado |
|---|---|---|
| Acceso sin autenticación | Ve a `http://localhost:4200/` sin sesión | Redirige al login |
| Login como Jefe | Credenciales correctas | Acceso a todos los proyectos y acciones |
| Login como Encargado | Credenciales de encargado | Solo ve proyectos asignados |
| Búsqueda no autorizada | Buscar en proyecto ajeno | Los resultados filtran por visibilidad del actor |

---

### 10. Cambios de la Remediación (Release E2)

### 10.1 Sincronización Reactiva de Estado (Sin Recarga Manual)

Se agregó `frontend/src/app/core/services/hierarchy-state.service.ts` como única fuente de verdad reactiva (BehaviorSubjects compartidos) para proyectos, etapas, actividades y tareas. Cualquier mutación exitosa (crear, editar, eliminar, reordenar) se refleja inmediatamente en todas las vistas que la consumen, sin necesidad de refrescar el navegador.

### 10.2 R3 — Navegación Directa Jerárquica

Se agregó restauración de contexto desde parámetros de ruta y consulta (`projectId`, `stageId`, `activityId`). Al abrir un enlace directo, la aplicación carga el proyecto, expande la etapa y selecciona la actividad automáticamente. La URL se mantiene sincronizada.

### 10.3 R7 — Conteos de Hijos Visibles Siempre

Se agregaron `activityCount` (etapas) y `taskCount` (actividades) a las respuestas del backend, calculados con consultas SQL nativas que evitan el caché de entidades de Hibernate. Los conteos se muestran tanto cuando la etapa/actividad está expandida como colapsada, y se actualizan inmediatamente tras crear o eliminar hijos.

### 10.4 R5 — Creación de Etapa con un Clic

Se verificó el guard `busyId` que deshabilita el botón mientras hay una petición en vuelo, evitando duplicados. Se muestra estado de carga y mensaje de error ante fallo.

### 10.5 R6 — Ordenamiento por Arrastre

Se agregó drag-and-drop con `@angular/cdk/drag-drop` para etapas, actividades y tareas, con actualización optimista y rollback automático si el backend rechaza la operación.

### 10.6 R8 — Rediseño UI/UX

- Encabezado contextual con breadcrumbs (Proyectos → Proyecto → Etapa → Actividad)
- Título y subtítulo de página dinámicos según el contexto
- Estados vacío, carga y error con mensajes claros
- Botones de acción primarios claramente identificados
- Diseño responsivo para desktop, tablet y móvil

---

## 7. Comandos Resumen

### Backend
```powershell
# Pruebas
cd backend
mvn test

# Cobertura
mvn jacoco:report

# Build
mvn compile
mvn package
```

### Frontend
```powershell
# Instalar dependencias
cd frontend
npm install

# Pruebas
npm test
# o
npx vitest run

# Pruebas con cobertura
npx vitest run --coverage

# Build de producción
npm run build
```

### Servidor de desarrollo

```powershell
# Backend (puerto 8081 — 8080 está ocupado en el entorno)
cd backend
$env:SPRING_DATASOURCE_PASSWORD="apppass"
mvn spring-boot:run

# Frontend (puerto 4200, proxyea /api a http://localhost:8081)
cd frontend
ng serve
```

---

## 8. Ubicación de Reportes

| Reporte | Ruta |
|---|---|
| Backend JaCoCo HTML | `backend/target/site/jacoco/index.html` |
| Backend JaCoCo XML | `backend/target/site/jacoco/jacoco.xml` |
| Frontend Vitest HTML | `frontend/coverage/index.html` |
| Frontend LCOV | `frontend/coverage/lcov.info` |

---

## 9. Solución de Problemas Comunes

| Problema | Solución |
|---|---|
| Puerto 8080 en uso | Detén el proceso existente o cambia el puerto en `application.yml` |
| Puerto 4200 en uso | Detén el proceso de `ng serve` existente |
| Error de conexión a DB | Verifica que PostgreSQL esté activo y las credenciales en `application.yml` |
| Fallos de prueba frontend | Ejecuta `npm install` de nuevo y limpia `node_modules` si es necesario |
| Error de compilación Angular | Verifica que las versiones de `@angular/*` sean consistentes (22.1.0) |

---

*Documento generado como parte de la implementación completa R1–R9 del Gestor de Tareas Personales Release 1.*
