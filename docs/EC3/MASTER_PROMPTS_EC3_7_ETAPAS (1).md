# EC3 — Paquete de 7 Master Prompts para Kilo Code

**Proyecto:** Gestor de Tareas Personales — Hard Coders  
**Repositorio:** [CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1](https://github.com/CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1/tree/Refactor-E2)  
**Rama objetivo:** `Refactor-E2`  
**Entorno de Kilo Code:** Code Mode · Provider `Glyph Cluster` · Free · Reasoning effort `Default`  
**Idioma de trabajo y documentación:** Español  
**Finalidad:** ejecutar la EC3 de forma controlada en siete etapas, con una guía de pruebas y una puerta de aceptación independiente al final de cada etapa.

---

## Cómo utilizar este paquete

1. Abre el repositorio en Kilo Code y verifica que el workspace corresponde a `Refactor-E2`.
2. Configura Kilo Code con **Code Mode / Glyph Cluster / Free / Reasoning effort Default**.
3. Copia y ejecuta **un solo prompt a la vez**, empezando por la Etapa 1.
4. Revisa la guía de pruebas que genere esa etapa, sus resultados reales y el checklist de salida.
5. No ejecutes el prompt de la siguiente etapa hasta que la actual tenga estado **PASS**. Si el resultado es `FAIL` o `BLOCKED`, resuelve el problema dentro del alcance de la etapa actual o deja documentado el bloqueo; no declares la etapa terminada.
6. Conserva las guías de pruebas y evidencias generadas. La Etapa 7 las consolidará en la documentación final.

### Orden y distribución del trabajo

| Etapa | Fase del plan EC3 | Responsable de la división | Objetivo principal | Dependencia para avanzar |
|---|---|---|---|---|
| 1 | Preparación y correcciones funcionales | Persona 1 | **Verificar la Fase 1 ya implementada** en el commit de cierre y re-ejecutar las comprobaciones | Debe aprobarse primero |
| 2 | Formularios y funcionalidades complementarias | Persona 4 | Completar CREATE de `app_user` y asegurar los flujos complementarios | Etapa 1 aprobada |
| 3 | Pruebas de diseño | Persona 4 | Matrices EP, BVA3, estados/transiciones y tabla de decisión | Etapa 1 aprobada; usar requisitos funcionales actualizados de Etapa 2 |
| 4 | Pruebas unitarias y de integración | Persona 5 | Probar reglas, casos de uso, controladores y persistencia | Etapas 1–3 aprobadas |
| 5 | Automatización E2E | Persona 3 | Configurar Playwright y automatizar flujos de usuario | Etapas 1–3 aprobadas; `app_user` de Etapa 2 listo |
| 6 | Cobertura y regresión | Persona 5 | Llegar al umbral de cobertura y validar R1–R9 | Etapas 4 y 5 aprobadas |
| 7 | Documentación y evidencias | Persona 2 | Consolidar guías, resultados, evidencias e informe académico | Etapa 6 aprobada |

La división original permite que las etapas 2, 3, 4 y 5 se trabajen en paralelo una vez aprobada la Etapa 1. Para el flujo de trabajo solicitado —verificar una etapa antes de pasar a la siguiente— estos prompts se presentan en orden secuencial. Si el equipo decide paralelizar las etapas 2–5, debe mantener ramas/áreas de trabajo aisladas, coordinar los contratos compartidos y volver a ejecutar las pruebas de integración después de combinar cambios. La Etapa 6 depende de las Etapas 4 y 5; la Etapa 7 depende de todo el trabajo anterior.

## Referencias de contexto del proyecto

Se deben utilizar como fuentes de planificación:

- `docs/EC3/ANALISIS_EC3.md` — análisis técnico y plan de implementación de la EC3. Si el archivo local difiere del informe compartido, comparar versiones y documentar diferencias antes de actuar.
- `DIVISION_TRABAJO_EC3.md` o su ubicación real en `docs/EC3/` — asignación de las siete fases y responsables. Confirmar su ruta real en el repositorio.
- `TESTING_GUIDE.md` — guía previa del proyecto. Tratar cifras históricas o resultados esperados como referencias, no como resultados de ejecución actual.
- `docs/pruebas-manuales.md` y los documentos de regresión existentes — escenarios R1–R9 y comportamiento manual esperado.
- Código, DTOs, controladores, reglas de dominio, migraciones, pruebas y configuración actuales — fuente de verdad para contratos y comportamiento real.

### Observaciones verificadas que los prompts deben respetar

- **Fase 1 ya tiene commit de cierre en la rama**: `10988a31d81f7596947c93d1e3abc3ddb42028db` (`Fase 1 Terminada con exito...`, 2026-11-15). El documento de evidencia es `docs/EC3/FASE1_CORRECCIONES_Y_ENTORNO.md`. Cada prompt debe volver a comprobar el HEAD; no asumir que el commit sigue siendo el último.
- **V006 ya existe**: `database/migrations/V006__task_assignment_reason.sql`. V005 continúa reservado para `add_sort_order`; no crear otra migración duplicada ni modificar V005/V006 salvo que una auditoría detecte un defecto real y se justifique una migración nueva aditiva.
- **EC3-04 ya está corregido según el reporte de Fase 1**: el formulario de tarea usa `estimatedHours` en horas (paso previsto de 0.25 h = 15 minutos), convierte a minutos enteros y envía `estimatedDurationMinutes`; existe un campo independiente `allocatedBudgetBob` en Bs con límites de hasta 13 dígitos enteros y 2 decimales. Las subtareas no tienen estos campos en el modelo V4. Verifica esta semántica en el código antes de diseñar casos nuevos.
- **EC3-02/03 ya están corregidos según el reporte**: `TaskAssignment` tiene `reason`, `REASON_MAX = 500`, `DEFAULT_REASON` y `normalizeReason()` en dominio; `AssignmentCommandService` persiste/retorna el motivo; entidad y adaptador JPA hacen write/read; `AssignWorkerRequest` ya valida `@Size(max=500)`. `HexagonalArchitectureTest` prohíbe dependencias `jakarta.*` en application/domain: no añadas Bean Validation a `AssignWorkerCommand` ni rompas la arquitectura. Mantén la regla de dominio y la validación HTTP en el DTO web.
- **EC3-11 ya está corregido según el reporte**: `database/docker/init-dev-db.sql` aplica y registra V005 y V006. Verifica arranque con `ddl-auto: validate`; una BD nueva debe pasar V001–V006. Nunca uses `docker compose down -v` sin confirmar que el volumen pertenece exclusivamente a pruebas y que no contiene datos que el equipo necesite.
- **EC3-12/13 ya están corregidos según el reporte**: `@angular/platform-browser-dynamic` quedó fijado exactamente en 22.1.3 y el lockfile sincronizado; dependencias `@angular/cdk`/`platform-browser-dynamic` instaladas en el entorno de prueba y errores TS de specs corregidos. No actualices paquetes sin necesidad.
- **R2 ya está corregido según el reporte**: la lista muestra la duración como horas/minutos. La regresión US3.1 también fue detectada y corregida: `TaskService.listAssignedTasks` debe seguir omitiendo los campos `allocatedBudgetBob` y `estimatedDurationMinutes` en la vista del Personal. No reintroduzcas información que esa vista no debe exponer.
- Entorno reportado: JDK 21, Maven 3.9.9, Node 22.22.3, npm 10.7.0, PostgreSQL 18, puertos DB 5432/backend 8081/frontend 4200. Angular 22 requiere Node `^22.22.3 || ^24.15.0 || ^26.0.0` y TypeScript `>=6.0.0 <6.1.0`; usa Node 22.22.3 para reproducir el entorno validado y verifica siempre `https://angular.dev/reference/versions`.
- El reporte Fase 1 informa 328/328 tests backend, 295/295 frontend y 21/21 pruebas API+BD; son referencias del 2026-11-15, no resultados que se puedan copiar como una nueva ejecución. Deben re-ejecutarse y etiquetarse con fecha/HEAD.
- Línea base de cobertura reportada al cierre de Fase 1: backend líneas 93.00%, ramas 83.21%; frontend líneas 92.89%, ramas 87.77%, sentencias 92.02%, funciones 84.20%. La cobertura de ramas backend y funciones frontend siguen bajo el umbral configurado de 85%; el reporte atribuye el cierre de estas brechas a Etapa 6. **No declares PASS de cobertura global en Etapa 1 ni bajes los thresholds.**
- Frontend: para ejecutar tests Angular utiliza `npx ng test --watch=false` desde `frontend` (o el script `npm test` si es equivalente); el reporte advierte que `npx vitest run` directo no carga el entorno Angular. No uses ese comando aislado como sustituto.
- Las etapas y actividades ya tienen formularios inline dentro de `wbs-panel`; no crear formularios o modales duplicados solo por conveniencia de testing.

---

# PROMPT MAESTRO 1 — ETAPA 1: VERIFICACIÓN DEL CIERRE DE PREPARACIÓN Y CORRECCIONES FUNCIONALES

> **Estado de partida:** la Fase 1 se reportó como terminada en el commit `10988a31d81f7596947c93d1e3abc3ddb42028db`. Este prompt no debe repetir la implementación. Su tarea principal es **verificar que esa fase está realmente completa y funciona en el HEAD actual**. Si algo no puede verificarse, no inventar un PASS.

Copia desde la siguiente línea:

---

## MASTER PROMPT — EC3 / ETAPA 1 (VERIFICACIÓN)

### 1. Rol y configuración

Actúa como un equipo senior compuesto por arquitecto de software, ingeniero Angular/TypeScript, ingeniero Java/Spring Boot, especialista PostgreSQL/Flyway y QA engineer.

Herramienta: Kilo Code · Modo: Code Mode · Provider: Glyph Cluster · Plan: Free · Reasoning effort: Default · Idioma: Español.

Repositorio: `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`  
Rama obligatoria: `Refactor-E2`  
Proyecto: Gestor de Tareas Personales — Hard Coders.

### 2. Misión: auditoría de aceptación, no reimplementación

La Fase 1 fue reportada como completa en el commit `10988a31d81f7596947c93d1e3abc3ddb42028db`, y se documentó en `docs/EC3/FASE1_CORRECCIONES_Y_ENTORNO.md`.

Debes verificar independientemente los siguientes resultados:

| ID | Cambio declarado | Evidencia que debes comprobar |
|---|---|---|
| EC3-04 | Formulario de tarea separa duración estimada y presupuesto | `estimatedHours` → minutos enteros en `estimatedDurationMinutes`; `allocatedBudgetBob` independiente; persistencia y lectura |
| EC3-02 | `task_assignment.reason` persiste | V006 + dominio + entidad JPA + adaptador write/read + consulta/listado |
| EC3-03 | Límite 500 de `reason` | Contrato HTTP (`AssignWorkerRequest`) y regla de dominio (`TaskAssignment.normalizeReason`) |
| EC3-11 | Bootstrap DB ejecuta V005 y V006 | `init-dev-db.sql`, tabla de tracking y backend con `ddl-auto: validate` |
| EC3-12 | Dependencias Angular alineadas | `package.json`, `package-lock.json`, Angular packages y Node correcto |
| EC3-13 | Tests frontend compilan sin errores TS propios | `tsc -p tsconfig.spec.json` y `ng test` |
| R2 | Duración se muestra en la lista | `task-list` presenta horas/minutos correctamente |
| US3.1 | Mis tareas no expone datos de planificación/finanzas restringidos | `TaskService.listAssignedTasks` y tests; no reintroducir campos filtrados |

**No vuelvas a implementar estos puntos si ya están correctos.** Si detectas un defecto, documenta evidencia y clasifícalo como `FAIL`; no modifiques código en esta etapa de verificación. Solicita autorización explícita antes de abrir una corrección adicional.

### 3. Inspección de rama y fuentes

Antes de ejecutar nada:

1. Ejecuta `git status --short --branch`, `git branch --show-current` y `git rev-parse HEAD`.
2. Confirma `Refactor-E2`. Si no estás en esa rama, detente sin cambiar de rama.
3. Comprueba si el HEAD actual es el commit de cierre indicado o un descendiente; si es otro commit, documenta el SHA y revisa el diff pertinente.
4. Comprueba que el working tree está limpio o registra exactamente los cambios locales; no los sobrescribas.
5. Lee `docs/EC3/FASE1_CORRECCIONES_Y_ENTORNO.md`, `docs/EC3/ANALISIS_EC3.md`, `docs/EC3/DIVISION_TRABAJO_EC3.md` y la guía `TESTING_GUIDE.md`.
6. Contrasta el informe contra el código y los tests actuales. El código y los reportes recién generados prevalecen sobre afirmaciones históricas.

### 4. Verificación EC3-04 — duración y presupuesto

Verifica:

- El formulario presenta duración estimada en **horas**, con atributo HTML `step="0.25"` (15 minutos).
- `estimatedHours` se convierte a minutos enteros y se envía al argumento/campo `estimatedDurationMinutes`.
- **Riesgo de validación que debe comprobarse explícitamente:** el control de crear es `button type="button"` y `crearTarea()` utiliza `validateHours()` manual. En el HEAD inspeccionado, `validateHours()` comprueba null, finitud, no-negatividad y máximo en minutos, pero no comprueba explícitamente que el valor sea múltiplo de 0.25. No supongas que el atributo HTML `step="0.25"` bloquea por sí solo el POST. Prueba `0.1` horas (y otro valor no múltiplo, por ejemplo `0.3`) desde el botón real. Si la solicitud se envía, documenta el hallazgo como discrepancia de la regla de incrementos y marca el gate de Fase 1 como FAIL para ese criterio; no modifiques código en esta etapa de verificación.
- El presupuesto es un campo separado `allocatedBudgetBob` expresado en Bs, no se deriva de la duración.
- El presupuesto respeta `>= 0`, hasta 13 dígitos enteros y 2 decimales, coherente con el contrato backend/`DECIMAL(15,2)`.
- Una entrada inválida de duración o presupuesto muestra error y no envía POST.
- Las subtareas no reciben presupuesto/duración, porque el modelo V4 no define esos campos para subtareas.
- `task-mapper.ts` mapea los campos correctos y `task-list` muestra la duración persistida.
- `TaskService.listAssignedTasks` sigue respetando US3.1: no debe exponer a Personal campos de presupuesto/duración que hayan sido retirados de esa vista. Comprueba el contrato real y sus tests.

Revisa los tests de `task-form`, `task-mapper`, `task-list` y `task.service`. No cambies el contrato por preferencia personal.

### 5. Verificación EC3-02/03 — `task_assignment.reason`

Comprueba:

1. Existe `database/migrations/V006__task_assignment_reason.sql`; V005 sigue siendo `add_sort_order`.
2. V006 añade `reason varchar(500) NULL` y `ck_task_assignment_reason`, que admite NULL histórico y rechaza motivos compuestos solo por espacios.
3. `TaskAssignment` contiene `reason`, `REASON_MAX = 500`, `DEFAULT_REASON` y `normalizeReason()`.
4. El dominio normaliza nulo/vacío a valor por defecto, recorta espacios y rechaza >500 con `DomainValidationException`.
5. `AssignWorkerRequest` conserva `@Size(max = 500)` en el adaptador web.
6. **No debe aparecer `jakarta.*` en application/domain** por añadir validación: `HexagonalArchitectureTest` lo prohíbe. No trasladar `@Size` a `AssignWorkerCommand`.
7. `AssignmentCommandService` persiste motivo normalizado; `AssignmentPersistenceAdapter` mapea el valor en `save` y `toDomain`; listado por tarea recupera el valor guardado.
8. Asignaciones históricas con `reason = NULL` se leen sin fallar.

### 6. Verificación EC3-11 — bootstrap de DB

- Inspecciona `database/docker/init-dev-db.sql` y comprueba que ejecuta/registra V001–V006, en particular V005 y V006.
- Comprueba tracking de migraciones y checksums según la estrategia del repositorio.
- Ejecuta el bootstrap solo en una base/volumen desechable y dedicado. **Nunca ejecutes `docker compose down -v` si no has demostrado que el volumen es exclusivamente de pruebas y que puede eliminarse.** No borres el volumen de desarrollo del equipo.
- Comprueba backend Spring Boot con `ddl-auto: validate` contra una BD que haya aplicado todas las migraciones.

### 7. Verificación EC3-12/13 — entorno Angular

Entorno reportado al cierre:

| Componente | Versión reportada |
|---|---|
| JDK | 21 |
| Maven | 3.9.9 |
| Node.js | 22.22.3 |
| npm | 10.7.0 |
| PostgreSQL | 18 |
| Angular | 22.x; `@angular/platform-browser-dynamic` fijado a 22.1.3 |
| Frontend | puerto 4200 |
| Backend | puerto 8081 |
| PostgreSQL | puerto 5432 |

Comprueba compatibilidad de Angular 22 en `https://angular.dev/reference/versions`. Node 22.22.3 es la versión reproducida en el informe; no fuerces Node 18.

Comprueba la sincronía de `package.json` y `package-lock.json`, incluidas versiones de Angular que deban coincidir exactamente. No actualices dependencias si ya están correctas.

Para tests Angular utiliza `npx ng test --watch=false` (o el script equivalente verificado). **No utilices `npx vitest run` directamente como sustituto**, porque el informe indica que no carga el entorno Angular/TestBed del proyecto.

Ejecuta `tsc -p tsconfig.spec.json` con el comando real y registra cualquier error propio del proyecto. Distingue warnings/deprecations de errores que bloquean build/test.

### 8. Ejecución segura de verificación

Utiliza el entorno aislado descrito por el proyecto. No reutilices una BD compartida ni de producción.

Ejecuta y registra, cuando el entorno esté disponible:

- Backend: `mvn test` desde `backend`.
- Backend: `mvn verify`, pero distingue el fallo de `jacoco:check` debido al umbral global de ramas todavía no alcanzado; no atribuyas ese gate a Fase 1 porque su cierre global corresponde a Etapa 6.
- Frontend: `npx ng test --watch=false` desde `frontend`.
- Frontend: `npx ng build` o `npm run build`.
- TypeScript de pruebas: `npx tsc -p tsconfig.spec.json --noEmit` si coincide con la configuración real; adapta flags solo tras inspeccionar `tsconfig`.
- E2E API+BD: `bash docs/EC3/scripts/e2e_fase1.sh`, únicamente con backend y BD de pruebas dedicados.
- Smoke test de arranque de backend con V001–V006 y `ddl-auto: validate`.

El script `e2e_fase1.sh` crea datos; no lo ejecutes sobre BD compartida. No muestres credenciales de seed, JWT ni secretos en la guía, logs o salida final. Usa variables de entorno/archivos locales ignorados por Git cuando se requiera autenticación.

### 9. Métricas históricas de Fase 1 — solo referencia

El reporte `FASE1_CORRECCIONES_Y_ENTORNO.md` del 2026-11-15 registra:

| Indicador | Backend | Frontend |
|---|---:|---:|
| Tests ejecutados/aprobados/fallidos/omitidos | 328/328/0/0 | 295/295/0/0 |
| Líneas | 93.00% | 92.89% |
| Ramas | 83.21% | 87.77% |
| Sentencias | — | 92.02% |
| Funciones | — | 84.20% |
| E2E API+BD de Fase 1 | 21/21 PASS | — |

Estas cifras **no son resultados de tu ejecución actual**. Repite las pruebas y compara. La cobertura de ramas backend (83.21%) y funciones frontend (84.20%) no cumple el umbral configurado de 85%; se lleva como deuda a Etapa 6. No marques la cobertura global como PASS en esta etapa y no reduzcas ningún threshold.

### 10. Guía de pruebas de Etapa 1

Asegúrate de que exista:

`docs/EC3/ETAPA_1_GUIA_PRUEBAS.md`

Si ya existe, revísala y actualízala con resultados actuales. Si no existe, crea este archivo como complemento del reporte `FASE1_CORRECCIONES_Y_ENTORNO.md`.

Debe incluir:
- Alcance y tabla de requisitos EC3-04, EC3-02/03, EC3-11/12/13 y R2/US3.1.
- Rama, HEAD, fecha y versión del entorno.
- Pasos y comandos exactos ejecutados, precondiciones y códigos de salida.
- Casos de duración (incluida conversión 0.25 h = 15 min), presupuesto separado, subtareas y vista Personal.
- Casos `reason`: explícito, default, vacío, 499, 500, 501 y lectura/listado/NULL histórico.
- Bootstrap V001–V006 y arranque backend.
- Métricas actuales separadas de métricas históricas.
- Hallazgos y limitaciones.
- Checklist PASS/FAIL/BLOCKED.

No copies contraseñas ni tokens del seed ni del script E2E a la guía.

### 11. Gate de salida — Etapa 1

- [ ] Se verificó rama `Refactor-E2`, HEAD y estado del working tree.
- [ ] A1 duration-hours → integer minutes está demostrado por tests/payload/BD.
- [ ] Se verificó que el incremento de 0.25 h se aplica realmente (0.1/0.3 h inválido no debe generar POST si el paso es requisito contractual; atributo HTML aislado no basta).
- [ ] `allocatedBudgetBob` independiente y con validaciones reales.
- [ ] Subtareas sin campos que no soporta el modelo V4.
- [ ] Vista de Personal no reexpone campos retirados por US3.1.
- [ ] V006 persist/read de `reason` demostrado; V005 intacto.
- [ ] `reason` 500/normalization/default/domain validation verificados.
- [ ] No se añadió Jakarta Validation a application/domain ni se rompió HexagonalArchitectureTest.
- [ ] V005/V006 aplicadas por bootstrap y backend arranca con schema validation.
- [ ] `ng test`, build y TypeScript specs ejecutados con Node compatible.
- [ ] E2E API+BD 21/21 re-ejecutados o el resultado actual está explicado con evidencia.
- [ ] No se declaró PASS de cobertura global: las brechas backend branches/frontend functions quedan para Etapa 6.
- [ ] `ETAPA_1_GUIA_PRUEBAS.md` está completo, reproducible y sin secretos.

**Estado final:** PASS solo si las funcionalidades de Fase 1 y pruebas aplicables se verifican. FAIL si hay comportamiento defectuoso; BLOCKED si no hay entorno seguro para verificarlo. El incumplimiento de cobertura global ya asignado a Etapa 6 no convierte por sí solo la Fase 1 en FAIL.

No modifiques código ni migraciones en esta etapa de verificación. Si detectas una discrepancia, detente, documenta archivo/línea/evidencia/impacto y pide autorización antes de corregir. No empieces Etapa 2. No hagas commit, push ni merge.

---

# PROMPT MAESTRO 2 — ETAPA 2: FORMULARIOS Y FUNCIONALIDADES COMPLEMENTARIAS

> **Objetivo específico:** completar el flujo de creación de `app_user` de forma segura y verificar que los dos CREATE complementarios (`app_user` y `task_assignment`) son funcionales. Evaluar el flujo de etapa/actividad existente sin duplicarlo.

Copia desde la siguiente línea:

---

## MASTER PROMPT — EC3 / ETAPA 2

### 1. Rol y configuración

Actúa como arquitecto de software, desarrollador senior Angular/Spring Boot, especialista de seguridad de aplicaciones y QA engineer. Trabaja en Code Mode / Glyph Cluster / Free / Reasoning effort Default. Documentación y comunicación en español.

Repositorio: `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`; rama `Refactor-E2`.

### 2. Gate de entrada

Antes de cambiar código:

1. Verifica rama, HEAD y `git status --short --branch`.
2. Lee `docs/EC3/FASE1_CORRECCIONES_Y_ENTORNO.md`, `docs/EC3/ETAPA_1_GUIA_PRUEBAS.md` (si ya fue generada), análisis EC3 y división de fases.
3. Verifica que la Etapa 1 fue marcada PASS y revisa sus pruebas. Si no hay evidencia PASS, no continúes; informa qué criterio está pendiente.
4. Inspecciona nuevamente el estado real de `app_user`, `UserDirectory`, `UserDirectoryAdapter`, `AppUserEntity`, autenticación, políticas, navegación y componentes de usuarios. No supongas que la función todavía falta si la rama ya cambió.
5. Verifica el endpoint y el formulario de asignación actual. La operación CREATE de `task_assignment` ya existe en el análisis (`POST /api/tasks/{taskId}/assignments`); no crear un segundo flujo equivalente.

### 3. Alcance de esta etapa

#### Contrato heredado de Etapa 1 que debes preservar

La Fase 1 ya dejó implementado y probado, según `docs/EC3/FASE1_CORRECCIONES_Y_ENTORNO.md`:

- Duración de tarea introducida en horas (`estimatedHours`), con atributo de input `step=0.25`, convertida a minutos enteros para `estimatedDurationMinutes`. Verifica el enforcement real del paso; el `validateHours()` inspeccionado no parecía comprobar múltiplos de 0.25 y el botón es `type="button"`, por lo que se debe probar `0.1`/`0.3` y no asumir bloqueo nativo.
- Presupuesto separado (`allocatedBudgetBob`) en Bs, `>=0`, hasta 13 enteros y 2 decimales. Las subtareas no exponen esos campos en el modelo V4.
- `task_assignment.reason` persiste por V006 y se normaliza en `TaskAssignment` (500 chars, default y trim). `AssignWorkerRequest` ya aplica `@Size(max=500)`.
- La arquitectura prohíbe dependencias `jakarta.*` en application/domain; no añadas anotaciones Bean Validation a `AssignWorkerCommand` ni rompas `HexagonalArchitectureTest`.
- `init-dev-db.sql` aplica V001–V006. No modifiques V005/V006 ni dupliques el motivo.
- Preserva la corrección de US3.1 en `TaskService.listAssignedTasks`; no reintroduzcas los campos eliminados para la vista Personal. Nunca escribas credenciales seed o tokens en docs/tests/logs.

#### Actividades de esta etapa

- **A6 / EC3-01:** desarrollar la operación CREATE de `app_user`, endpoint seguro `POST /api/users`, caso de uso/servicio, validaciones, persistencia y formulario de creación accesible solo para Jefe.
- **A7:** evaluar el flujo de creación de etapa y actividad en `wbs-panel` y confirmar que sus controles inline son utilizables y comprobables. El análisis ya encontró formularios inline existentes; no crear modales duplicados por defecto.
- Especificar y verificar los dos flujos CREATE complementarios: `app_user` y `task_assignment`.

No implementes todavía Playwright ni todas las matrices de diseño EP/BVA3; corresponden a Etapas 3 y 5. Puedes crear pruebas unitarias/de integración estrictamente necesarias para esta funcionalidad y documentarlas.

### 4. Diseño funcional de CREATE `app_user`

Inspecciona el modelo actual antes de decidir campos. El análisis indica que `app_user` contiene al menos email, `password_hash`, nombres/apellidos, `is_chief` e `is_active`, y que `UserDirectory` actualmente es de lectura. Verifica los tipos y restricciones reales en migraciones y entidades.

Implementa un flujo mínimo y completo, no un CRUD general:

**Backend**

1. Endpoint `POST /api/users` protegido; únicamente un Jefe autenticado y activo puede crear usuarios.
2. La autorización debe comprobarse en backend (controlador/seguridad y caso de uso/regla de dominio según la arquitectura). Ocultar el botón en frontend no es control de acceso.
3. Define DTO de entrada y respuesta. Nunca incluir `password`, `password_hash`, tokens ni secretos en la respuesta.
4. Normaliza email según las reglas existentes (trim/lowercase si así lo establece el esquema) y valida formato y unicidad antes de guardar. Maneja también la carrera de unicidad en base de datos sin devolver información sensible.
5. Valida nombres/apellidos y contraseña de acuerdo con el contrato existente y políticas de seguridad. No inventes longitudes: deriva los límites del esquema y la política del proyecto.
6. Usa el `PasswordEncoder` real de Spring Security ya empleado por el login. Inspecciona cómo se almacenan los hashes actuales y prueba que el login pueda autenticar al usuario recién creado. No almacenes contraseña en texto plano, no implementes hash manual ni devuelvas el hash.
7. No cambies a ciegas el algoritmo de hashing ni rompas contraseñas existentes. OWASP recomienda Argon2id para sistemas nuevos; en sistemas existentes se debe preservar compatibilidad con el encoder configurado y planificar cualquier migración. Si se usa BCrypt, verificar work factor e input limit conforme a la política vigente. Revisa la documentación de OWASP y Spring Security antes de cambiar la estrategia.
8. No permitas que una petición de usuario común se autoasigne privilegios de Jefe. Respeta el modelo real: `is_chief` es una propiedad privilegiada; encargado y personal se determinan por las relaciones/reglas existentes. Por defecto, un nuevo usuario debe ser no-Jefe (`is_chief=false`) salvo que el requisito confirmado indique otro flujo autorizado explícito.
9. Define un caso de uso/puerto/adapter según la arquitectura hexagonal existente, en lugar de poner reglas de negocio en el controlador o componente Angular. Respeta `HexagonalArchitectureTest`: `jakarta.*`/Bean Validation permanece en el adapter web; application/domain no deben depender de Jakarta. Las reglas de negocio propias del dominio se validan allí.
10. Devuelve respuesta apropiada (por ejemplo, 201) y maneja duplicidad, validación y permisos con el esquema de errores ya utilizado.
11. No añadas UPDATE/DELETE de usuarios a menos que un requisito explícito lo exija. La EC3 solicita probar CREATE.

**Frontend**

1. Crea una pantalla, diálogo o formulario de creación dentro de la estructura de funcionalidades existente, de acuerdo con los patrones de navegación y UI actuales.
2. Solo Jefe debe poder acceder. Usa guards y control visual existentes, pero conserva la autorización de backend como control definitivo.
3. Incluye labels accesibles, mensajes por campo, estado loading, prevención de doble envío, mensaje de éxito y manejo de errores del servidor.
4. No muestres contraseñas en consola ni guardes la contraseña después de crear el usuario. No incluyas hash en modelos frontend.
5. Refresca la lista/vista afectada de forma reactiva, sin forzar recarga completa cuando la arquitectura existente permita actualizar el estado. Preserva el contrato de US3.1 para la vista Personal: el flujo `listAssignedTasks` no debe reintroducir el presupuesto ni otros campos que el servicio haya retirado para esa vista.
6. Evita duplicar formularios o servicios existentes; reutiliza patrones y componentes adecuados.

### 5. Verificación de `task_assignment` y etapa/actividad

1. Comprueba la operación de asignación ya existente y la persistencia de `reason` implementada en Etapa 1. Verifica motivo explícito, valor por defecto, lectura y compatibilidad de filas anteriores.
2. Comprueba que una asignación necesita tarea existente, actor autorizado, proyecto abierto, trabajador existente y activo, no autoasignación, ausencia de duplicidad y relaciones válidas.
3. Comprueba el flujo inline de creación de etapa y actividad en `wbs-panel`. Asegura que el formulario tiene labels/estados/mensajes observables y permite probar entradas válidas e inválidas. No agregues modales si no existe un motivo de UX o requisito verificable.
4. No cambies los contratos de API sin actualizar clientes, DTOs y tests de manera compatible.

### 6. Pruebas de la etapa

Crea pruebas unitarias y de integración para:

- Jefe crea usuario válido.
- Encargado y Personal reciben 403 al intentar crear usuarios.
- Email inválido, vacío, duplicado, mayúsculas/espacios según normalización.
- Nombre/apellido inválidos y límites reales.
- Contraseña no válida según política; comprobar que no se almacena ni retorna en claro.
- Contraseña se codifica con el encoder real y el nuevo usuario puede autenticarse por el flujo existente.
- La respuesta no contiene password/hash.
- No se puede crear un usuario privilegiado por tampering de campos.
- Asignación complementaria válida y sus restricciones principales.
- Etapa y actividad se crean mediante sus controles inline actuales y reflejan respuesta/errores.

Usa base PostgreSQL aislada para pruebas que dependan de constraints y migraciones. No uses mocks como sustituto de las pruebas que deben comprobar persistencia real.

### 7. Testing guide obligatorio

Crea:

`docs/EC3/ETAPA_2_GUIA_PRUEBAS.md`

Incluye:

- Alcance y criterios de aceptación de la Etapa 2.
- Rama/commit probados y estado de Etapa 1.
- Descripción del flujo CREATE de `app_user` (actor, formulario, endpoint, caso de uso, persistencia).
- Reglas de autorización y hashing verificadas.
- Datos de prueba y roles usados, sin contraseñas reales ni secretos.
- Pasos reproducibles desde UI y API.
- Tests unitarios/integración, comandos exactos y resultados observados.
- Casos de rechazo y códigos/mensajes reales.
- Prueba de login con la cuenta creada.
- Evidencia de persistencia de ambos CREATE complementarios.
- Validación del flujo inline de etapa/actividad.
- Conteo real de ejecutados, aprobados, fallidos y omitidos.
- Defectos, limitaciones, resultados BLOCKED y checklist de salida.

### 8. Gate de salida — Etapa 2

- [ ] El caso de uso CREATE `app_user` existe y sigue la arquitectura del sistema.
- [ ] Endpoint protegido y autorización Jefe probada en backend.
- [ ] Formulario accesible solo por el flujo Jefe y usable desde la interfaz.
- [ ] Validación de email, nombres, contraseña, duplicidad y campos privilegiados probada.
- [ ] PasswordEncoder compatible con el login actual; nunca se guarda ni expone texto plano/hash.
- [ ] Nuevo usuario persistido y capaz de autenticarse.
- [ ] `task_assignment` CREATE probado y `reason` leído desde persistencia real.
- [ ] Formularios inline de etapa y actividad evaluados; no se duplicaron sin justificación.
- [ ] Pruebas de la etapa pasan y no hay cambios fuera del alcance.
- [ ] `docs/EC3/ETAPA_2_GUIA_PRUEBAS.md` permite reproducir la verificación.

Declara `PASS`, `FAIL` o `BLOCKED` según evidencia real. No avances a la Etapa 3 ni configures Playwright. No hagas commit/push/merge. Detente al terminar.

---

# PROMPT MAESTRO 3 — ETAPA 3: DISEÑO DE PRUEBAS EP, BVA3, ESTADOS Y DECISIONES

> **Objetivo específico:** producir el diseño de pruebas trazable y revisable. Esta etapa es de diseño documental: no debe implementar funcionalidades ni empezar la automatización.

Copia desde la siguiente línea:

---

## MASTER PROMPT — EC3 / ETAPA 3

### 1. Rol y configuración

Actúa como Test Analyst / QA Architect con experiencia en ISTQB CTFL, pruebas de caja negra y análisis de reglas de negocio. Trabaja en Kilo Code Code Mode / Glyph Cluster / Free / Reasoning effort Default. Español para los documentos.

Repositorio `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`; rama `Refactor-E2`.

### 2. Gate de entrada

1. Comprueba rama, HEAD y working tree.
2. Lee `docs/EC3/ANALISIS_EC3.md`, `DIVISION_TRABAJO_EC3.md`, `docs/EC3/ETAPA_1_GUIA_PRUEBAS.md` y `docs/EC3/ETAPA_2_GUIA_PRUEBAS.md`.
3. Verifica que Etapas 1 y 2 tienen estado PASS. Si no, detente y documenta la dependencia.
4. Revalida formularios, campos, validaciones y contratos en el código actual; no copies matrices previas sin cotejarlas con el HEAD actual.

### 3. Alcance y restricción

Diseñar y documentar:

- Partición de equivalencia (EP).
- Análisis de valores límite de tres valores (BVA3).
- Transición de estados de proyecto.
- Tabla de decisión del flujo de asignación.
- Casos complementarios CREATE para `app_user` y `task_assignment`.
- Trazabilidad de cada caso a requisito, formulario, regla y resultado esperado.

No modifiques lógica de aplicación, endpoints, migraciones ni UI. No escribas todavía suites de ejecución automatizadas: la implementación de tests backend/integración corresponde a Etapa 4 y la automatización E2E a Etapa 5. El entregable principal de esta fase es el diseño de pruebas revisado.

### 4. Base de diseño y formularios a cubrir

Considera el proceso principal confirmado:

`Proyecto → Etapa → Actividad → Tarea → Subtarea`

Incluye los formularios/controles reales:

- Proyecto (crear y editar cuando corresponda al alcance EC3).
- Etapa (form inline de WBS).
- Actividad (form inline de WBS).
- Tarea.
- Subtarea.
- Asignación de Personal (`task_assignment`).
- Creación de usuario (`app_user`) incorporada en Etapa 2.

No confundas “formulario” con “modal”: etapa y actividad ya pueden exponerse inline dentro de `wbs-panel`.

Para cada formulario extrae del código:

- Campos obligatorios/opcionales.
- Tipos y formatos.
- Longitudes mínimas/máximas.
- Límites numéricos y precisión.
- Valores por defecto/enumeraciones.
- Reglas de unicidad y referencias foráneas.
- Validaciones en HTML/Angular, DTO/Bean Validation, dominio y SQL.
- Mensajes y códigos HTTP reales.

Marca divergencias entre frontend, backend y base de datos. No inventes límites ni des por válido un caso sin evidencia contractual.

### 5. Diseño EP — Particiones de equivalencia

Para cada campo relevante, identifica particiones no vacías, no solapadas y con comportamiento esperado común. Incluye particiones válidas e inválidas.

Matriz requerida:

| ID | Formulario | Campo | Partición | Válida/Inválida | Dato representativo | Precondición | Resultado UI | Resultado API/BD | Origen de la regla |
|---|---|---|---|---|---|---|---|---|---|

Incluye al menos:

- Proyecto: nombre, descripción, encargado, presupuesto, duración (la unidad real del proyecto; verificar si sigue en minutos).
- Etapa/Actividad: nombre, descripción y padre/contexto.
- Tarea: título, prioridad, actividad padre, presupuesto (`allocatedBudgetBob`) y duración `estimatedHours` en horas, convertida a `estimatedDurationMinutes` enteros.
- Subtarea: título, descripción y tarea padre; no incluir presupuesto ni duración mientras el modelo V4 no los soporte.
- Asignación: trabajador existente/activo, actor autorizado, autoasignación, duplicidad, proyecto abierto, `reason`.
- Usuario: email válido/incorrecto/duplicado, nombres, contraseña y control privilegiado.

Elige representantes que realmente separen resultados. No mezcles varias condiciones en un mismo caso si luego no es posible saber qué regla falló. Para múltiples campos, utiliza Each Choice como base y añade combinaciones de riesgo cuando sea necesario.

### 6. Diseño BVA3 — Tres valores por cada frontera

BVA3 requiere probar el valor inmediatamente inferior, el valor exacto del límite y el inmediatamente superior de cada frontera ordenada. Documenta las unidades.

| ID | Formulario | Campo | Tipo/límite | Valor inferior | Valor límite | Valor superior | Resultado esperado por valor | Justificación |
|---|---|---|---|---|---|---|---|---|

Considera límites reales para:

- Nombre/título y descripción, según contratos actuales (no reutilices límites históricos sin comprobar).
- Motivo de asignación `reason`: 499 / 500 / 501 caracteres; recordar que null/vacío se normaliza y que valores solo espacios están rechazados por dominio/constraint.
- Presupuesto de tarea `allocatedBudgetBob`: `-0.01 / 0.00 / 0.01`, máximo de 13 dígitos enteros, precisión de 2 decimales y valor con 3 decimales.
- Duración de tarea: campo UI en horas con `step=0.25` (15 min). Diseña EP para valores múltiplos válidos y valores no múltiplos; incluye `0.1`/`0.3` como candidatos inválidos solo si el paso es una regla contractual confirmada. Comprueba que el handler realmente los rechaza y no depende solo de la validez nativa del input: el control de creación es `type="button"`. Diseña BVA3 para el mínimo `-0.25 / 0 / 0.25`, y para el máximo confirmado por `MAX_DURATION_MINUTES`, con unidad horaria/minutos convertidos. La conversión esperada es `estimatedHours × 60` a minutos enteros.
- Las subtareas no tienen duración/presupuesto, por tanto no diseñes BVA para campos que no existen.
- Duración de proyecto/etapas permanece en la unidad que determine su contrato; no mezclarla con horas de tarea.
- Cualquier límite de email/contraseña que el contrato final de `app_user` defina.

Reglas:

1. Para enteros, documenta n−1, n, n+1; para decimales, usa la unidad mínima representable coherente con `DECIMAL(15,2)` o la regla real (por ejemplo, 0.01), sin simplificar precisión.
2. Para campos opcionales sin mínimo de negocio, no inventes un límite mínimo. Usa particiones EP si BVA no aplica.
3. Si un campo tiene dos fronteras, documenta cada una.
4. Comprueba si una frontera es impuesta por HTML, DTO, dominio o DB; si no coinciden, identifica la discrepancia.
5. No uses el contrato anterior en días para tareas: Etapa 1 lo sustituyó por horas (paso 0.25 h) y conversión a minutos enteros. Mantén separados duración en minutos y presupuesto en Bs. Proyecto/etapas pueden conservar sus propias unidades: verifica cada contrato.

### 7. Diseño de transición de estados

Utiliza el ciclo de vida de proyecto verificado en el enum, servicio, controlador y catálogo SQL. El informe analiza cinco estados como candidato:

- `PLANIFICADO`
- `EN_EJECUCION`
- `EN_PAUSA`
- `FINALIZADO`
- `CANCELADO`

Confirma el grafo actual; no codifiques una transición basada solo en el informe.

Entrega:

1. Diagrama Mermaid `stateDiagram-v2`.
2. Tabla de todas las transiciones válidas e inválidas relevantes.
3. Secuencias de eventos para llegar a los estados.
4. Guardas y permisos (Jefe, Encargado, conflicto de versión si aplica).
5. Resultado observable y side effects esperados.
6. Cobertura de estados y cobertura de transiciones por separado.

Matriz:

| ID | Estado inicial | Evento/comando | Guarda/actor | Estado final esperado | Válida | Resultado HTTP/dominio | Evidencia persistida |
|---|---|---|---|---|---|---|---|

Incluye transiciones terminales desde `FINALIZADO` y `CANCELADO`, transiciones no permitidas desde estados abiertos, y conflicto por versión si el contrato lo expone. Añade el ciclo de tarea como una matriz complementaria si aporta cobertura al flujo principal, especialmente `PENDIENTE → EN_PROGRESO → COMPLETADA → EN_PROGRESO` y las restricciones de cancelación con subtareas abiertas.

### 8. Tabla de decisión del flujo de asignación

Modela el flujo real `POST /api/tasks/{taskId}/assignments`.

Analiza condiciones relevantes como:

- La tarea existe.
- El actor tiene permiso para asignar (Jefe/Encargado conforme a la regla real).
- El proyecto está abierto.
- El trabajador existe y está activo.
- El actor no se autoasigna.
- No existe una asignación duplicada.
- Las relaciones tarea/proyecto/usuario son válidas.

Primero define condiciones y acciones. Después enumera combinaciones factibles. No generes combinaciones imposibles sin marcar N/A y explicar por qué. Usa “—” solo cuando una condición sea irrelevante para la acción en esa regla.

| Condición/acción | R1 | R2 | R3 | R4 | R5 | R6 | ... |
|---|---|---|---|---|---|---|---|

Cada regla debe tener ID de prueba, datos, precondiciones, pasos, outcome, respuesta HTTP y expectativa de persistencia/no persistencia. Incluye el camino feliz y al menos las reglas para proyecto cerrado, trabajador inactivo, actor no autorizado, autoasignación y duplicidad, si son factibles en el contrato real.

Calcula la cobertura de reglas factibles: columnas ejercitadas / columnas factibles. No digas que se alcanzó 100% de cobertura si quedan reglas sin caso.

### 9. Casos CREATE de las dos tablas complementarias

Crea conjuntos de casos diferenciados:

- `task_assignment`: crear asignación, guardar `reason`, valor por defecto, rechazo de trabajador inactivo/autoasignación/duplicidad/proyecto cerrado/no autorizado y lectura posterior.
- `app_user`: crear usuario válido, email normalizado, duplicidad, validación de nombres/contraseña, rechazo por rol no Jefe, y protección frente a asignación de `is_chief` no autorizada.

### 10. Consolidación y trazabilidad

Genera una matriz maestra que relacione cada requisito EC3 con pruebas, tipo, fase ejecutora y evidencia futura.

| Requisito | Caso(s) | Técnica | Formulario/flujo | Ejecución prevista | Etapa responsable |
|---|---|---|---|---|---|

Asigna:

- Casos de lógica de dominio y aplicación a Etapa 4.
- Casos de UI, navegación y persistencia visible a Etapa 5.
- Regresión consolidada y umbrales a Etapa 6.

### 11. Entregables documentales de Etapa 3

Crea:

- `docs/EC3/ETAPA_3_DISENO_PRUEBAS.md` — matrices completas, diagramas, justificación y trazabilidad.
- `docs/EC3/ETAPA_3_GUIA_PRUEBAS.md` — guía para revisar y validar el diseño (no afirmes que los casos se ejecutaron en esta etapa).

La guía debe indicar:

- Requisitos y archivos del sistema utilizados como fuente.
- Cómo revisar que cada partición es válida/no válida y no solapada.
- Cómo comprobar BVA3 (n−1, n, n+1) y cobertura de cada frontera.
- Cómo revisar transiciones y cobertura de estados/transiciones.
- Cómo verificar viabilidad y cobertura de todas las reglas de la tabla de decisión.
- Cómo enlazar IDs con futuros tests unitarios, integración y Playwright.
- Checklist PASS/FAIL y discrepancias pendientes.

### 12. Gate de salida — Etapa 3

- [ ] Todas las formas principales existentes están inventariadas y diseñadas: proyecto, etapa, actividad, tarea, subtarea.
- [ ] EP incluye particiones válidas e inválidas de todos los campos relevantes y dos CREATE complementarios.
- [ ] BVA3 tiene tres valores por frontera aplicable, unidades claras y resultados esperados.
- [ ] Todas las reglas/limites se rastrean a código, contrato o DB.
- [ ] El diagrama y la matriz de estados cubren los estados alcanzables y transiciones relevantes.
- [ ] La tabla de decisión distingue combinaciones factibles e inviables y tiene al menos tres reglas distintas, preferiblemente las cinco reglas negativas citadas más camino feliz.
- [ ] Cada caso tiene ID único, precondiciones, pasos, resultado esperado y nivel de prueba.
- [ ] La matriz de trazabilidad asigna cada caso a Etapa 4, 5 o 6.
- [ ] `ETAPA_3_DISENO_PRUEBAS.md` y `ETAPA_3_GUIA_PRUEBAS.md` son completos y revisables.
- [ ] No se modificó código de aplicación ni se afirmó ejecución de pruebas que no ocurrió.

Detente tras generar y validar la documentación. No comiences Etapa 4 ni Etapa 5. Sin commits, pushes ni merges.

---

# PROMPT MAESTRO 4 — ETAPA 4: PRUEBAS UNITARIAS Y DE INTEGRACIÓN

> **Objetivo específico:** implementar y ejecutar pruebas unitarias y de integración que comprueben reglas de negocio, validaciones, persistencia y errores del backend, trazadas al diseño de Etapa 3.

Copia desde la siguiente línea:

---

## MASTER PROMPT — EC3 / ETAPA 4

### 1. Rol y configuración

Actúa como Principal QA Automation Engineer especializado en Java 21, Spring Boot, arquitectura hexagonal, JUnit 5, Spring Boot Test, PostgreSQL y pruebas de integración. Trabaja en Kilo Code Code Mode / Glyph Cluster / Free / Reasoning effort Default. Documentación en español.

Repositorio: `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`; rama: `Refactor-E2`.

### 2. Gate de entrada

1. Verifica rama, HEAD y estado del árbol de trabajo.
2. Lee el análisis, la división de fases y las guías de Etapas 1–3.
3. Comprueba que las Etapas 1, 2 y 3 están PASS.
4. Lee `docs/EC3/ETAPA_3_DISENO_PRUEBAS.md`; úsalo como base de trazabilidad.
5. Inspecciona los tests actuales antes de crear archivos para evitar duplicados y seguir sus convenciones.
6. Identifica la versión real de JUnit/Spring Boot, dependencias, configuración DB de tests y cobertura actual. No añadas Testcontainers u otra dependencia si el proyecto ya dispone de una estrategia adecuada.

Si faltan evidencias/gates previos, no inventes aceptación: detente como BLOCKED.

### 3. Alcance

Implementa pruebas unitarias y de integración, priorizando el backend y los flujos de dominio:

- `AssignmentCommandService` y `AssignmentPolicy`.
- `TaskCommandService` y reglas de presupuesto/duración/estado.
- `ProjectCommandService`, estados, permisos y versionado.
- Persistencia de `TaskAssignment` y `reason` (incluida migración y lectura histórica).
- Creación de usuarios (`app_user`) de Etapa 2, autorización, validación y hashing.
- Controladores/DTOs para códigos HTTP, Bean Validation y contrato de respuesta.
- Integridad relacional y restricciones PostgreSQL que sean esenciales para los requisitos.

No configures ni implementes todavía Playwright. No hagas el trabajo global de cobertura de Etapa 6; genera las mediciones disponibles para detectar línea base, pero no rebajes thresholds ni conviertas el objetivo de 85% en una excusa para tests artificiales.

### 4. Estrategia unit tests

Organiza las pruebas por unidad de responsabilidad:

**Dominio/policies**
- Cada política de asignación: actor, trabajador, proyecto, duplicidad y self-assignment.
- `ProjectStatus.allowedTargets()` y `Project.changeStatus()` con transiciones válidas e inválidas.
- Validaciones de presupuesto, duración, nombre, motivo y versionado.
- Fábricas `Project.create`, `Task.create`, `restore` y reglas de estados relevantes si el diseño las incluye.

**Servicios de aplicación**
- Caso feliz y caminos de error para `ProjectCommandService`, `TaskCommandService`, `AssignmentCommandService` y el servicio de creación de usuario.
- Verificar interacciones con puertos: repositorios, catálogo, actor actual, directorio y encoder, usando doubles/mocks solo donde aporten aislamiento válido.
- Asegurar que un rechazo no realiza persistencia ni side effects indebidos.
- Verificar que el motivo de asignación devuelto es el motivo del agregado/registro persistido, no una variable temporal.

**Validación**
- Probar límites definidos por la Etapa 3: nombre, descripción, presupuesto, duración, `reason` y datos de usuario.
- Probar null/blank, valores fuera de rango, enumeraciones inválidas y asociaciones inexistentes.
- Evitar duplicar exactamente pruebas que ya cubren la misma rama con el mismo nivel; amplía los huecos identificados.

### 5. Estrategia de integración

El entorno validado en Fase 1 es JDK 21, Maven 3.9.9, Node 22.22.3/npm 10.7.0 y PostgreSQL 18. Usa ese baseline, confirmándolo en el sistema donde se ejecuta. El bootstrap `database/docker/init-dev-db.sql` aplica V001–V006.

Usa la estrategia PostgreSQL real del repositorio para comprobar:

- Migraciones ordenadas aplicadas sobre una DB de prueba vacía.
- Upgrade desde esquema anterior con registros existentes.
- `task_assignment.reason` write/read y filas históricas NULL.
- Clave primaria compuesta y rechazo de duplicados.
- FK y constraints de usuario/tarea/proyecto.
- Crear usuario, email normalizado/único y hash guardado; login/PasswordEncoder compatible.
- Controladores: autenticación, autorización, validación HTTP, status codes y shape de DTO.
- Respuesta no contiene password/hash.
- Errores previstos 400/403/404/409/412 según mapeo real del exception handler.

Usa un entorno aislado y reproducible. Preferir PostgreSQL real para probar triggers, `CHECK`, constraints y SQL específico. Si el entorno usa Testcontainers, reutiliza la configuración existente. Si usa Docker Compose, configura una DB exclusiva de tests. No sustituyas PostgreSQL por H2 para validar comportamiento PostgreSQL-específico. Nunca ejecutes tests destructivos contra la BD de desarrollo compartida, staging o producción.

### 6. Integración con el diseño de Etapa 3

- Implementa los casos de EP/BVA3 cuyo nivel apropiado sea unidad o integración.
- Implementa estados/transiciones de proyecto y tarea a nivel integración para demostrar transacción, persistencia e historial.
- Implementa reglas de la tabla de decisión que dependen de actor, proyecto, usuario y asignaciones persistidas.
- Usa los IDs de caso de `ETAPA_3_DISENO_PRUEBAS.md` en nombres, display names o comentarios de tests para mantener trazabilidad.
- Si al implementar detectas una expectativa incorrecta del diseño, no cambies silenciosamente el test: verifica contrato/código, documenta el conflicto y actualiza el documento de diseño con motivo antes de cambiar la expectativa.

### 7. Calidad de los tests

- Arrange / Act / Assert o patrón consistente del repo.
- Nombres que expliquen condición y resultado.
- Tests deterministas e independientes.
- No depender del orden de ejecución.
- No usar sleeps ni reloj real cuando un `Clock` inyectable exista.
- Fixture builders legibles; evitar enormes setups duplicados.
- No sobreusar mocks en pruebas de integración.
- Comprobar resultado observable y persistencia, no llamadas internas irrelevantes.
- Mantener compatibilidad con arquitectura hexagonal y convenciones del equipo.

### 8. Ejecución y resultados

Ejecuta suites unitarias y de integración separadamente si el proyecto permite distinguirlas; luego ejecuta `mvn test` y la suite backend completa. Como referencia histórica, al cierre de Fase 1 el backend registraba 328/328 tests; el conteo cambiará con los nuevos tests y debe medirse de nuevo. Obtén métricas reales desde reportes generados por la herramienta. Si `mvn verify` falla exclusivamente en `jacoco:check` por cobertura de ramas aún inferior al 85%, registra esa brecha como pendiente de Etapa 6; no confundas el quality gate global con una falla de las pruebas unitarias/integración de Etapa 4. Cualquier fallo de test o build ajeno al threshold sí debe resolverse antes de PASS.

No inventes cantidad de tests, cobertura ni estado. Informa pruebas ejecutadas, aprobadas, fallidas, omitidas y error details relevantes. No sumes dos veces la misma prueba incluida en una suite global y una suite por tags.

No manipules JaCoCo, no excluyas clases de negocio ni reduzcas thresholds. La meta mínima de 85% línea y 85% rama se evaluará de manera global en Etapa 6.

### 9. Testing guide obligatorio

Crea:

`docs/EC3/ETAPA_4_GUIA_PRUEBAS.md`

Incluye:

- Alcance, dependencias y IDs de diseño cubiertos.
- Precondiciones de JDK/Maven/PostgreSQL.
- Cómo inicializar la DB exclusiva de test.
- Comandos exactos para unit tests, integración y suite completa.
- Separación entre tests unitarios y tests de integración.
- Datos y fixtures usados; cómo se limpian.
- Migraciones y restricciones verificadas.
- Matriz ID de caso → clase/método de test → resultado.
- Conteos reales y resumen de fallos corregidos.
- Ubicación de Surefire/Failsafe/JaCoCo u otros reportes reales.
- Cómo reproducir un fallo específico.
- Qué se reserva para Playwright (Etapa 5) y cobertura/regresión global (Etapa 6).
- Checklist PASS/FAIL.

### 10. Gate de salida — Etapa 4

- [ ] Las pruebas se trazan a los casos de Etapa 3.
- [ ] Reglas críticas de asignación, usuario, proyecto y tarea están probadas.
- [ ] Persistencia real de `reason`, compatibilidad histórica y constraints están verificadas con PostgreSQL de prueba.
- [ ] Autorización y validación de CREATE `app_user` están probadas.
- [ ] Códigos HTTP y errores corresponden al exception handler real.
- [ ] Las suites unitarias e integración de la etapa pasan.
- [ ] Los tests son independientes, deterministas y no contaminan datos.
- [ ] No se redujeron umbrales ni se excluyó código significativo.
- [ ] La guía `ETAPA_4_GUIA_PRUEBAS.md` contiene comandos y resultados reales.
- [ ] No quedaron cambios fuera del alcance.

Declara `PASS`, `FAIL` o `BLOCKED` con evidencia. No avances a Etapa 5. No hagas commit/push/merge.

---

# PROMPT MAESTRO 5 — ETAPA 5: AUTOMATIZACIÓN E2E CON PLAYWRIGHT

> **Objetivo específico:** dejar Playwright configurado y automatizar los flujos visibles para el usuario, incluida la creación de los formularios principales y los dos CREATE complementarios.

Copia desde la siguiente línea:

---

## MASTER PROMPT — EC3 / ETAPA 5

### 1. Rol y configuración

Actúa como Senior SDET / QA Automation Architect experto en Playwright + TypeScript + Angular, automatización de aplicaciones SPA, autenticación JWT y pruebas de integración visual/persistencia. Trabaja en Kilo Code Code Mode / Glyph Cluster / Free / Reasoning effort Default. Español para documentación.

Repositorio: `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`; rama `Refactor-E2`.

### 2. Gate de entrada

1. Verifica rama/HEAD/working tree.
2. Lee análisis, división, diseño EP/BVA3 y guías de Etapas 1–4.
3. Comprueba PASS de Etapas 1–3 y disponibilidad de los contratos de Etapa 2 y pruebas backend de Etapa 4.
4. Inspecciona `frontend/package.json`, Angular 22, lockfile, scripts, entorno de backend, proxy/base URL, seed de usuarios y base aislada.
5. Comprueba que Playwright realmente no esté ya instalado. Si ya existe, audita y reutiliza la configuración; no crees una segunda configuración. El reporte Fase 1 indicaba que no estaba instalado, pero vuelve a verificar el HEAD actual.
6. Verifica compatibilidad Node usando [Angular Version compatibility](https://angular.dev/reference/versions). No uses Node 18 por la guía anterior si no cumple Angular 22.

### 3. Alcance

- Instalar/configurar `@playwright/test` si falta, con versión compatible y lockfile actualizado.
- Crear `playwright.config.ts` apropiado para el frontend Angular y backend Spring Boot existentes.
- Configurar webServer/baseURL, fixtures de autenticación, gestión de datos y reporte.
- Automatizar formularios principales: proyecto, etapa, actividad, tarea, subtarea y asignación.
- Automatizar CREATE de `task_assignment` y `app_user`.
- Automatizar escenarios positivos y negativos de negocio.
- Verificar interfaz, API y persistencia observable.

No hagas la cobertura global JaCoCo/Vitest ni la regresión completa R1–R9 que corresponde a Etapa 6. Sin embargo, incluye los escenarios E2E del proceso principal y los casos EC3 priorizados en la matriz de Etapa 3.

### 4. Configuración Playwright

1. Usa `@playwright/test`, scripts npm explícitos y configuración TypeScript compatible con el proyecto.
2. Configura `baseURL` de frontend conforme al entorno local real (el informe registra Angular 4200 y backend 8081; verifica de nuevo).
3. Establece `webServer` para frontend y backend solo si el entorno se puede arrancar de manera segura y determinista. Si backend/DB deben levantarse manualmente, documenta el procedimiento y no ocultes errores de startup.
4. Usa Chromium como browser inicial si el alcance académico no exige más navegadores; amplía solo si hay razón y recursos disponibles.
5. Crea fixtures de autenticación para Jefe, Encargado y Personal utilizando el endpoint/login real. No almacenes contraseñas, tokens o storageState sensibles en Git.
6. Usa usuarios seed/dev exclusivamente en DB de prueba aislada. No modifiques usuarios reales ni reutilices cuentas personales.
7. Aísla cada test: un browser context por prueba, datos únicos por escenario y cleanup confiable. No dependas del orden de ejecución.
8. Configura retries solo para diagnóstico/CI; los reintentos no deben ocultar flakiness. Conserva trace en fallo o retry, screenshots en fallo y reporte HTML.
9. Usa Page Objects solo para flujos repetidos y complejos; evita una capa excesiva que dificulte entender los tests.
10. Define scripts como `test:e2e`, `test:e2e:ui` y `test:e2e:report` si se alinean con los scripts existentes.

### 5. Buenas prácticas obligatorias de automatización

Sigue la guía oficial de Playwright:

- Prioriza locators por rol, label, placeholder y texto visible (`getByRole`, `getByLabel`, `getByText`) sobre selectores CSS frágiles o clases internas.
- Aprovecha auto-wait y assertions reintentables. No agregues `waitForTimeout` como sincronización normal.
- Prueba lo que ve y hace el usuario; no acoples E2E a nombres de métodos internos o detalles de implementación.
- Cada test debe poder ejecutarse solo y no depender del estado dejado por otro.
- Usa expectativas explícitas y espera una respuesta observable de UI/red cuando corresponda.
- Adjunta trace/screenshot para poder diagnosticar fallos.

### 6. Flujos E2E que debes automatizar

Identifica/usa los IDs concretos de `docs/EC3/ETAPA_3_DISENO_PRUEBAS.md`.

**Proceso principal**

1. Login como Jefe.
2. Crear proyecto válido.
3. Abrir WBS y crear etapa usando el formulario inline existente.
4. Crear actividad dentro de la etapa.
5. Crear tarea dentro de la actividad con prioridad, duración en horas según el paso permitido de 0.25 h y presupuesto separado en Bs.
6. Verificar que la duración se convierte a `estimatedDurationMinutes`, se muestra en horas/minutos y no se asigna al presupuesto; probar también que los valores inválidos no generan POST.
7. Crear subtarea dentro de la tarea.
8. Asignar un trabajador activo y verificar la asignación después de recargar/volver a consultar.
9. Probar `reason` explícito, null/vacío (valor por defecto), 499/500/501 y espacios, confirmando el valor persistido y recuperado tras nueva consulta.
10. Login como Personal y comprobar que la vista “Mis tareas” sigue sin exponer los campos que US3.1 excluye; no aceptar regresiones que reintroduzcan información de presupuesto.

**CREATE `app_user`**

1. Login como Jefe y acceder al formulario.
2. Crear usuario con datos únicos.
3. Verificar feedback visible y presencia del registro en la vista/lista disponible.
4. Cerrar sesión y comprobar que la cuenta creada puede autenticarse mediante el endpoint real.
5. Verificar que la respuesta no expone password/hash (aserción de API solo si es parte del test de contrato, no inspección de secretos).
6. Intentar acceder/crear como Encargado y Personal y comprobar el rechazo correspondiente.
7. Probar email duplicado, formato inválido, campos requeridos y longitud límite.
8. Manipular el payload para intentar privilegios no autorizados y verificar el rechazo o normalización segura.

**CREATE `task_assignment`**

1. Asignación válida.
2. Trabajador inactivo.
3. Autoasignación.
4. Asignación duplicada.
5. Proyecto cerrado.
6. Actor no autorizado.
7. Persistencia del `reason` y recuperación después de una nueva consulta.

**Escenarios negativos de formularios**

- Nombre/título vacío o solo espacios.
- Nombre/título superando el máximo real.
- Descripción fuera de límite.
- Duración negativa/cero/límite y conversión aprobada.
- Actividad/etapa/tarea padre inexistente o contexto inválido, cuando el flujo UI pueda representarlo.
- Error del backend mostrado correctamente sin dejar UI en estado loading.

### 7. Comprobación de red y persistencia observable

- Para la creación de etapa (escenario R5 reservado a Etapa 6), incluye una prueba o helper que permita contar peticiones `POST` y confirmar que un único click exitoso produce una sola creación. No uses mocks del endpoint para dar por probado el backend.
- Comprueba que el UI se actualiza sin recargar si ese es el contrato funcional.
- Para persistencia, vuelve a abrir/recargar la vista o consulta la representación de usuario y confirma el registro real; no te bases exclusivamente en un toast.
- Si una aserción necesita inspeccionar respuesta HTTP, vincúlala a la acción de usuario y valida también el efecto visible.

### 8. Estructura y mantenimiento

Organiza bajo `frontend/e2e/` o la ruta E2E ya existente. Agrupa por dominio/flujo:

- `auth/`
- `projects/`
- `hierarchy/`
- `tasks/`
- `assignments/`
- `users/`

Usa fixtures y utilidades pequeñas para login y datos. No guardes credenciales en texto claro. Configura `.gitignore` para reportes/storageState si es necesario, sin borrar reglas existentes.

La ejecución API+BD de Fase 1 mediante `docs/EC3/scripts/e2e_fase1.sh` (21/21 reportada) no es Playwright y no se debe sumar como tests E2E Playwright de esta etapa.

### 9. Testing guide obligatorio

Crea:

`docs/EC3/ETAPA_5_GUIA_PRUEBAS.md`

Incluye:

- Requisitos de Node/Angular, Java, Maven, PostgreSQL y browsers.
- Instalación de Playwright y browsers.
- Cómo arrancar DB aislada, backend y frontend.
- Variables de entorno requeridas (nombres sin valores secretos).
- Cómo ejecutar todos los E2E, un spec, un test por ID, UI mode y reporte HTML.
- Configuración de autenticación y fixtures por rol.
- Cómo generar/limpiar datos de prueba sin tocar datos reales.
- Tabla ID de caso → archivo/spec → rol → escenario → resultado esperado.
- Reporte real con tests ejecutados/aprobados/fallidos/omitidos.
- Ubicación de traces/screenshots/reportes.
- Diagnóstico de problemas comunes.
- Limitaciones y casos no automatizados.
- Checklist PASS/FAIL.

### 10. Gate de salida — Etapa 5

- [ ] Playwright instalado/configurado o configuración existente verificada.
- [ ] Comandos reproducibles para correr E2E y generar reporte.
- [ ] Tests usan locators accesibles y auto-wait; no dependen de sleeps fijos.
- [ ] Tests aislados, repetibles y no dependen del orden.
- [ ] Flujos principales cubiertos desde navegador.
- [ ] `app_user CREATE` probado desde UI y login posterior.
- [ ] `task_assignment CREATE` probado desde UI y con recuperación del motivo persistido.
- [ ] Escenarios negativos y roles no autorizados verificados.
- [ ] Persistencia y estado visible comprobados.
- [ ] Capturas/traces/reportes disponibles para fallos.
- [ ] Guía `ETAPA_5_GUIA_PRUEBAS.md` contiene resultados reales.

Si los E2E fallan, corrige la causa dentro del alcance de la Etapa 5 y repite la suite. Si una falla es de backend ya fuera del alcance, documenta un blocker con evidencia y no lo ocultes mediante mocks. No avances a Etapa 6. Sin commits/pushes/merges.

---

# PROMPT MAESTRO 6 — ETAPA 6: COBERTURA MÍNIMA Y REGRESIÓN R1–R9

> **Objetivo específico:** verificar las suites completas, alcanzar ≥85% de líneas y ramas en frontend y backend y cerrar la regresión funcional R1–R9 con resultados reproducibles.

Copia desde la siguiente línea:

---

## MASTER PROMPT — EC3 / ETAPA 6

### 1. Rol y configuración

Actúa como QA Lead, especialista en quality gates, JaCoCo, Vitest, Playwright, pruebas de regresión Angular/Spring Boot y análisis de cobertura. Trabaja en Code Mode / Glyph Cluster / Free / Reasoning effort Default. Español en reportes.

Repositorio: `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`; rama `Refactor-E2`.

### 2. Gate de entrada

1. Verifica rama, commit y working tree.
2. Lee análisis, división y guías de Etapas 1–5.
3. Comprueba PASS de Etapas 1, 2, 3, 4 y 5. Si alguna está BLOCKED/FAIL, detente y registra dependencias.
4. Inspecciona los umbrales configurados actualmente en `backend/pom.xml` y `frontend/vitest.config.ts`. El snapshot ya indicaba JaCoCo con 85% líneas/ramas en backend y Vitest con 85% líneas/ramas/funciones/sentencias en frontend; confirma en HEAD.
5. Verifica que los reportes de test no estén obsoletos: limpia solo directorios de build/reporte generados y seguros, nunca fuentes ni bases de datos.

### 3. Línea base de Fase 1 y objetivos cuantitativos

El reporte verificado de Fase 1 (2026-11-15) registra estos valores, que son baseline y no métricas finales de Etapa 6:

| Métrica | Backend | Frontend |
|---|---:|---:|
| Tests ejecutados/aprobados/fallidos/omitidos | 328/328/0/0 | 295/295/0/0 (18 archivos) |
| Líneas | 93.00% | 92.89% |
| Ramas | 83.21% | 87.77% |
| Sentencias | — | 92.02% |
| Funciones | — | 84.20% |
| E2E API+BD fase 1 | 21/21 PASS | — |

**Brechas de partida a investigar con reportes frescos:** backend branches 83.21% (<85%) y frontend functions 84.20% (<85%). Frontend lines/branches superaban el objetivo, pero `task-list.ts` aparecía con functions 73.5% y branches 77%. El backend tenía líneas 93%, ramas 83.21%. No presumas que el estado no ha cambiado.

Debes alcanzar al menos:

| Métrica | Frontend | Backend |
|---|---:|---:|
| Cobertura de líneas | ≥85% | ≥85% |
| Cobertura de ramas | ≥85% | ≥85% |

Mantén todos los thresholds actuales. Si Vitest ya exige también funciones y sentencias ≥85%, conserva esos umbrales y cúmplelos. No reduzcas el gate para obtener un build verde.

### 4. Medición inicial y estrategia

1. Ejecuta la suite frontend, la suite backend y las E2E después de preparar el entorno de prueba aislado.
2. Genera reportes HTML/XML/LCOV según las herramientas actuales.
3. Identifica archivos/clases/líneas/ramas no cubiertas y priorízalas por riesgo funcional y proximidad a cambios de EC3.
4. Distingue código realmente ejecutable del generado/boilerplate según la configuración oficial. No agregues exclusiones arbitrarias.
5. No uses mocks que eviten ejecutar ramas de negocio importantes.
6. Añade pruebas significativas que verifiquen comportamientos, resultados y errores reales; no repitas tests triviales ni fuerces ejecución sin assertions.
7. Repite medición hasta alcanzar los umbrales o hasta identificar un bloqueo real; informa siempre el resultado exacto.

### 5. Comandos y reportes

Inspecciona y usa los comandos soportados por los scripts reales:

- Backend: ejecutar tests completos y `mvn verify` (o el ciclo Maven equivalente que ejecute JaCoCo report/check); genera reporte JaCoCo.
- Frontend: ejecutar suite Angular con `npx ng test --watch=false` y generar cobertura usando las opciones reales del Angular CLI/builder. No usar `npx vitest run` standalone si no carga TestBed. Obtener reportes compatibles (text/html/lcov o JSON) y validar las métricas con un chequeo explícito.
- Build: `npm run build` y Maven package/verify según convenga.
- E2E: ejecutar el suite Playwright completo contra entorno de prueba aislado; generar HTML report.

No supongas que `npm test` equivale a `npx vitest run`. El estado Fase 1 documenta el comando Angular `npx ng test --watch=false`; confirma `package.json`, Angular CLI, configuración de test y la forma real de generar/validar coverage. Evita el Vitest standalone si no carga `TestBed`.

### 6. Regresión R1–R9

Ejecuta y, si hace falta, completa automatización de estos nueve escenarios:

| ID | Escenario | Verificación de salida |
|---|---|---|
| R1 | Búsqueda global, insensible a mayúsculas/acentos según contrato | Resultados pertinentes y estado vacío correcto, sin recarga innecesaria |
| R2 | Consistencia de unidades de duración | Horas de entrada (paso 0.25 h = 15 min) → minutos enteros persistidos/mostrados; presupuesto independiente y no contaminado; no múltiplos rechazados si el step es contractual |
| R3 | Navegación directa y restauración de contexto jerárquico | Deep link y recarga recuperan proyecto/etapa/actividad seleccionados |
| R4 | Creación jerárquica completa | Proyecto/etapa/actividad/tarea/subtarea coherentes, sin registros huérfanos |
| R5 | Creación de etapa de un solo click | Exactamente una solicitud de creación y una sola etapa persistida |
| R6 | Drag-and-drop y persistencia | Orden actualizado después de mover, recargar y volver a consultar |
| R7 | Contadores de hijos | Contadores correctos después de expandir/contraer y agregar/eliminar elementos según UI |
| R8 | Claridad/consistencia de UI | Labels, errores por campo, feedback de éxito, loading y accesibilidad básica comprobados objetivamente |
| R9 | Métodos de fábrica del backend | Tests unitarios de `Project.create`, `Task.create`, restauración y reglas del dominio |

Usa Playwright para R1–R8 cuando el escenario sea visible en interfaz. R9 corresponde a pruebas unitarias backend, no a una prueba E2E artificial.

Para R8, evita juicios vagos como “la interfaz es bonita”: define checks verificables (controles con nombre accesible, validaciones comprensibles, no se duplica acción, loading/deshabilitado, mensajes en contexto, navegación consistente, errores visibles y ausencia de recarga inesperada).

### 7. Corrección de fallos encontrados

- Si el fallo está en funcionalidad o tests de etapas anteriores, identifica la causa y coordina una corrección en el archivo/fase responsable sin esconderla.
- Puedes realizar correcciones puntuales necesarias para completar cobertura/regresión, pero registra cada cambio, su justificación y el test de confirmación.
- No rediseñes arquitectura ni abras alcance sin necesidad.
- Si necesitas nuevos cambios de producción, añade prueba de regresión antes/después y actualiza la guía de la etapa correspondiente.

### 8. Métricas y reporte fiel

Registra por separado:

| Suite | Ejecutados | Aprobados | Fallidos | Omitidos |
|---|---:|---:|---:|---:|
| Frontend unit/component | | | | |
| Backend unit | | | | |
| Backend integration | | | | |
| Playwright E2E | | | | |

Evita contar dos veces un mismo test repetido en un reporte de suite y un reporte global. Indica cuándo un total es suma de suites disjuntas.

Registra líneas/ramas/funciones/sentencias de frontend y líneas/ramas de backend. Incluye rutas a reportes y commit/HEAD exacto. No copies resultados históricos de `TESTING_GUIDE.md` como si fueran mediciones nuevas.

### 9. Testing guide obligatorio

Crea:

`docs/EC3/ETAPA_6_GUIA_PRUEBAS.md`

La guía debe incluir:

- Estado de precondiciones Etapas 1–5.
- Versiones/servicios del entorno probado.
- Comandos exactos para backend, frontend, cobertura, build y Playwright.
- Cómo limpiar/reconstruir reportes sin tocar datos ni fuentes.
- Resultados de cobertura por proyecto y métrica.
- Tabla de tests por suite (ejecutados/pasados/fallidos/omitidos).
- Matriz R1–R9 con evidencia y resultado real.
- Reportes HTML/XML/LCOV y Playwright.
- Capturas/trace de escenarios relevantes o fallidos.
- Defectos encontrados, causas, correcciones y re-test.
- Limitaciones y casos no ejecutados.
- Checklist de salida PASS/FAIL.

### 10. Gate de salida — Etapa 6

- [ ] Suite backend completa pasa.
- [ ] Suite frontend completa pasa.
- [ ] Playwright E2E completo pasa.
- [ ] Cobertura backend ≥85% líneas y ≥85% ramas. La rama parte de 83.21% según Fase 1, pero debe re-medirse.
- [ ] Cobertura frontend ≥85% líneas y ≥85% ramas. Funciones/sentencias deben cumplir cualquier threshold vigente; Fase 1 reportó funciones 84.20%, por lo que no se puede asumir PASS.
- [ ] El proceso de verificación realmente falla si cualquier umbral obligatorio no se cumple; un `ng test` exit code 0 no sustituye el chequeo si los thresholds de `vitest.config.ts` no se aplican al builder Angular.
- [ ] No se rebajaron thresholds ni se introdujeron exclusiones artificiales.
- [ ] R1–R9 verificados con evidencia reproducible.
- [ ] R2 confirma unidades correctas, presupuesto independiente y enforcement del step 0.25 h si es requisito contractual.
- [ ] R5 confirma una sola petición/registro.
- [ ] R6 confirma persistencia de orden.
- [ ] R7 confirma contadores correctos sin recarga innecesaria.
- [ ] Las métricas reales están registradas sin doble conteo.
- [ ] `docs/EC3/ETAPA_6_GUIA_PRUEBAS.md` permite reproducir todos los resultados.

Si la cobertura está por debajo de 85%, la etapa es `FAIL`, aunque los tests pasen. Si no puede medirse por infraestructura, es `BLOCKED`. No marques `PASS` de forma parcial. Detente al terminar; no empieces Etapa 7. No hagas commit/push/merge.

---

# PROMPT MAESTRO 7 — ETAPA 7: DOCUMENTACIÓN, EVIDENCIAS E INFORME FINAL

> **Objetivo específico:** consolidar todo lo ejecutado, generar la guía final de pruebas y preparar el informe académico final EC3 en PDF con métricas y evidencias reales.

Copia desde la siguiente línea:

---

## MASTER PROMPT — EC3 / ETAPA 7

### 1. Rol y configuración

Actúa como Technical Writer, QA Lead y responsable de documentación académica/técnica. Debes convertir el trabajo verificado de las seis etapas previas en entregables claros, reproducibles y fieles a la evidencia. Kilo Code Code Mode / Glyph Cluster / Free / Reasoning effort Default. Español.

Repositorio: `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`; rama `Refactor-E2`.

### 2. Gate de entrada

1. Comprueba rama, HEAD y estado del working tree.
2. Lee `docs/EC3/ANALISIS_EC3.md`, `docs/EC3/DIVISION_TRABAJO_EC3.md`, `docs/EC3/FASE1_CORRECCIONES_Y_ENTORNO.md` y las guías `ETAPA_1` a `ETAPA_6`. Contrasta el reporte de cierre de Fase 1 con el HEAD actual.
3. Comprueba que las seis etapas anteriores están PASS.
4. Abre reportes reales de JaCoCo, Vitest y Playwright; contrasta sus valores con lo escrito en las guías.
5. Inspecciona capturas, traces, reportes de ejecución y evidencia del comportamiento del sistema.
6. Si faltan datos de portada o identidad académica (integrantes, docente, grupo/sección), no los inventes; usa campos claramente marcados `PENDIENTE DE COMPLETAR`.
7. Si un gate anterior no está PASS, detente; no redactes un informe que afirme cumplimiento.

### 3. Entregables obligatorios

Produce como mínimo:

1. `docs/EC3/GUIA_PRUEBAS_EC3.md` — guía final consolidada para reproducir todas las pruebas.
2. `docs/EC3/INFORME_FINAL_EC3.md` — fuente editable del informe académico.
3. `docs/EC3/INFORME_FINAL_EC3.pdf` — versión PDF lista para entregar.
4. Evidencias organizadas en `docs/EC3/evidencias/` o una ruta ya existente coherente, con nombres/captions que permitan relacionarlas con los casos.

Actualiza o amplía las guías por etapa solo si una corrección documental es imprescindible; no alteres los resultados ya observados ni borres reportes históricos que sean parte de la evidencia. No generes cifras ficticias ni anexes secretos, tokens o contraseñas. El informe de Fase 1 incluye datos de acceso seed de desarrollo; redacta esos valores y referencia únicamente los nombres de variables, perfiles y scripts necesarios para reproducir el entorno.

### 4. Guía consolidada de pruebas

`GUIA_PRUEBAS_EC3.md` debe incluir:

- Objetivo, alcance, rama/commit y mapa de etapas.
- Requisitos de entorno exactos (Java, Maven, Node, npm, PostgreSQL, Playwright/browsers).
- Configuración de base de datos exclusivamente de pruebas.
- Preparación de variables de entorno sin valores sensibles.
- Cómo ejecutar backend unit tests.
- Cómo ejecutar backend integration tests.
- Cómo ejecutar el frontend mediante el test runner Angular confirmado (`npx ng test --watch=false` según la Fase 1), cómo producir la cobertura real y cómo verificar thresholds; no documentes `npx vitest run` standalone como comando válido si no carga TestBed.
- Cómo generar reportes de cobertura.
- Cómo arrancar frontend/backend y base de pruebas.
- Cómo ejecutar Playwright completo, un spec, un test y HTML report.
- Cómo inspeccionar traces y capturas.
- Cómo limpiar datos de test sin afectar entornos compartidos.
- Cómo interpretar resultados de EP, BVA3, estados/transiciones y tabla de decisión.
- Cómo ejecutar R1–R9.
- Matriz caso → comando/suite → evidencia → resultado.
- Fallos conocidos o limitaciones honestas.
- Procedimiento de reproducción de los resultados finales.

### 5. Informe académico final en PDF

El PDF debe ser profesional, consistente, legible y estar estructurado de forma que responda a la asignación EC3.

Incluye como mínimo:

1. **Portada:** nombre del proyecto, integrantes, docente, asignatura, grupo y fecha. Si algún dato no está disponible, déjalo claramente como pendiente.
2. **Introducción:** contexto, problema y objetivo de aplicar pruebas dinámicas al sistema.
3. **Alcance y arquitectura:** stack real, arquitectura hexagonal backend, Angular standalone, PostgreSQL y flujo general.
4. **Modelo relacional y diseño inicial:** entidades/tablas relevantes, relaciones y diagrama actualizado del modelo real después de migraciones.
5. **Proceso principal:** Proyecto → Etapa → Actividad → Tarea → Subtarea; formularios y dependencias.
6. **Cambios funcionales efectuados:** documenta con precisión los cambios de Fase 1: duración de tarea introducida en horas (0.25 h = 15 min) y convertida a minutos, presupuesto independiente en Bs, motivo de asignación persistido por V006, normalización/validación de 500 caracteres, bootstrap de V005/V006, dependencias Angular/TypeScript corregidas, R2 en la lista y preservación de US3.1 para Personal. Añade CREATE de usuario solo si Etapa 2 lo implementó realmente. No describas propuestas como cambios realizados.
7. **Diseño de pruebas EP:** particiones, justificación, muestras y resultados.
8. **Diseño de pruebas BVA3:** fronteras, valores inferior/límite/superior y resultados.
9. **Transición de estados:** diagrama, matriz de transiciones, casos ejecutados y cobertura.
10. **Tabla de decisión:** condiciones, reglas factibles, casos ejecutados y resultados.
11. **Automatización:** unit/integration/E2E, configuración Playwright, datos de prueba y estrategia de aislamiento.
12. **Resultados cuantitativos:** total de casos ejecutados, aprobados, fallidos y omitidos; cobertura de líneas/ramas frontend y backend; métricas Playwright. Separa resultados históricos de Fase 1 (328 backend, 295 frontend, 21 API+BD) de resultados finales posteriores; nunca los sumes dos veces ni presentes como nueva ejecución.
13. **Regresión R1–R9:** estado de cada escenario y evidencia.
14. **Evidencias:** capturas, reportes y trazas con títulos/leyendas y referencias a IDs de casos.
15. **Análisis e interpretación:** qué defectos detectó cada técnica, qué se corrigió y qué límites permanecen.
16. **Conclusiones y lecciones aprendidas.**
17. **Anexos:** matrices completas, comandos, estructura de pruebas, repositorio/branch y enlaces a documentación/reportes.

No afirmes que el sistema está “100% libre de errores”. La conclusión debe limitarse a las pruebas, entornos y versiones efectivamente verificadas.

### 6. Evidencias y métricas

- Usa capturas de Playwright o del navegador real para respaldar los escenarios funcionales.
- Usa reportes generados por JaCoCo y el builder de test Angular que realmente se ejecutó para respaldar cobertura. Si la salida Angular CLI no aplica automáticamente los thresholds configurados en `vitest.config.ts`, añade evidencia de un chequeo explícito reproducible.
- Usa reportes de ejecución para respaldar conteos.
- Si una evidencia no existe, indícala como no disponible; no simules una captura o salida.
- Asegura que las capturas no muestren contraseñas, tokens, valores de configuración, credenciales seed, datos personales reales o información sensible. No copies secretos de `seed-dev-users.sql`, scripts E2E ni del entorno local a Markdown/PDF.
- En cada evidencia incluye ID de caso, fecha/commit, contexto y resultado.
- No infles el total contando dos veces un test que aparece en una suite particular y una suite agregada.

### 7. Revisión editorial y técnica

Antes de exportar:

1. Comprueba que todos los valores numéricos coinciden con los reportes.
2. Comprueba que nombres de entidades, endpoints, campos y migraciones coinciden con HEAD.
3. Verifica que Mermaid/diagramas renderizan correctamente o incorpora una imagen exportada legible.
4. Revisa enlaces internos, pies de figura, tablas partidas y encabezados huérfanos.
5. Comprueba legibilidad del PDF: fuentes incrustadas, tablas dentro de página, márgenes, numeración y portada.
6. Verifica que el PDF se abre y que el número de páginas y texto se leen correctamente.
7. Mantén el Markdown fuente completo y consistente con el PDF.

### 8. Testing guide obligatorio de la etapa

Crea:

`docs/EC3/ETAPA_7_GUIA_PRUEBAS.md`

Esta guía es el checklist de control de calidad de los entregables documentales. Incluye:

- Verificación de los seis gates anteriores.
- Lista de reportes fuente contrastados.
- Validación de conteos y porcentajes.
- Validación de capturas y rutas de evidencia.
- Checklist de cada capítulo del informe PDF.
- Instrucciones para regenerar el PDF desde el Markdown fuente.
- Comprobación de apertura/legibilidad del PDF.
- Verificación de consistencia entre PDF, Markdown y guías por etapa.
- Campos académicos pendientes.
- Resultado final PASS/FAIL/BLOCKED.

Además, `GUIA_PRUEBAS_EC3.md` debe quedar como la guía operativa integral para un evaluador que quiera volver a ejecutar el trabajo.

### 9. Gate final — Etapa 7

- [ ] Etapas 1–6 están PASS y sus pruebas/evidencias pueden consultarse.
- [ ] `GUIA_PRUEBAS_EC3.md` contiene requisitos, comandos y pasos reproducibles.
- [ ] `INFORME_FINAL_EC3.md` contiene todos los capítulos y datos correctos.
- [ ] `INFORME_FINAL_EC3.pdf` existe, abre correctamente y tiene formato profesional.
- [ ] Métricas y conteos coinciden exactamente con reportes reales.
- [ ] Cobertura frontend/backend está representada con los porcentajes auténticos.
- [ ] EP, BVA3, transición de estados y tabla de decisión están justificadas y trazadas.
- [ ] Se presentan resultados CREATE de `app_user` y `task_assignment`.
- [ ] R1–R9 están reportados individualmente y respaldados por evidencia.
- [ ] Evidencias están organizadas y no exponen credenciales ni datos sensibles.
- [ ] Las limitaciones, fallos pendientes y datos académicos faltantes se declaran claramente.
- [ ] `ETAPA_7_GUIA_PRUEBAS.md` permite auditar la entrega documental.

Si algún requisito de la entrega no tiene evidencia, no lo marques como completado. Emite `FAIL` o `BLOCKED` según corresponda.

No realices commit, push, merge ni publicación externa sin autorización. Detente después de verificar los entregables y entrega un resumen final con rutas, métricas auténticas y estado.

---

# Estándares y fuentes técnicas utilizadas para diseñar los prompts

Las siguientes fuentes son documentación oficial o estándares de referencia; los prompts aplican sus prácticas al contexto concreto del repositorio. Esto **no significa que los autores de esas fuentes hayan revisado individualmente estos prompts**.

1. **ISTQB — Certified Tester Foundation Level Syllabus v4.0.1**: técnicas de partición de equivalencia, BVA de tres valores, tablas de decisión, transiciones de estado, cobertura y planificación de pruebas.  
   https://istqb.org/wp-content/uploads/2024/11/ISTQB_CTFL_Syllabus_v4.0.1.pdf
2. **Playwright — Best Practices**: probar comportamiento visible para el usuario, usar locators accesibles, auto-wait, assertions reintentables, traces y evitar acoplar tests a detalles internos.  
   https://playwright.dev/docs/best-practices
3. **Playwright — Browser Contexts / Isolation**: contexto independiente por test, aislamiento de cookies/storage y prevención de contaminación entre pruebas.  
   https://playwright.dev/docs/browser-contexts
4. **Vitest — Coverage**: proveedores V8/Istanbul, reportes y thresholds configurables; confirmar la integración real de Angular CLI y no asumir que los thresholds Vitest se aplican cuando `ng test` ejecuta el builder Angular.  
   https://vitest.dev/guide/coverage
5. **JaCoCo — Maven Check Goal**: reglas de cobertura que pueden hacer fallar el build cuando los umbrales no se cumplen.  
   https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html
6. **Angular — Version Compatibility**: rangos compatibles de Node.js, TypeScript y RxJS para cada versión Angular.  
   https://angular.dev/reference/versions
7. **OWASP — Password Storage Cheat Sheet**: algoritmos adaptativos para almacenamiento de contraseñas; Argon2id como recomendación para nuevos sistemas y requisitos de compatibilidad cuando se mantiene BCrypt.  
   https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
8. **Spring Security — Password Storage**: uso de `PasswordEncoder` y `DelegatingPasswordEncoder` para compatibilidad/actualización de formatos de hash.  
   https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html

---

## Línea base de Fase 1 (2026-11-15)

El commit `10988a31d81f7596947c93d1e3abc3ddb42028db` cierra la primera implementación según `docs/EC3/FASE1_CORRECCIONES_Y_ENTORNO.md`. La línea base reportada es: backend 328/328 tests, líneas 93.00%, ramas 83.21%; frontend 295/295 tests, líneas 92.89%, ramas 87.77%, sentencias 92.02%, funciones 84.20%; E2E API+BD de fase 1: 21/21. Estos valores deben verificarse y nunca copiarse como resultado final de otras etapas. Las brechas de cobertura se trasladan a Etapa 6.

La ejecución frontend de Fase 1 se hizo mediante `npx ng test --watch=false`; el reporte advierte que ejecutar `npx vitest run` directamente no carga el entorno Angular. Node 22.22.3, Java 21, Maven 3.9.9, npm 10.7.0 y PostgreSQL 18 son las versiones reportadas. No incluir credenciales de desarrollo reproducidas en el informe de Fase 1 en nuevos prompts, guías, capturas ni logs.

## Regla global de cierre

El resultado de cada etapa debe ser verificable. **Una guía de pruebas no es una lista de comandos vacía:** debe registrar qué se ejecutó, con qué datos y entorno, qué resultado se esperaba, qué se observó, cuántos tests aprobaron/fallaron/omitieron y dónde está la evidencia. La etapa solo pasa cuando sus criterios de salida están respaldados por esa evidencia.
