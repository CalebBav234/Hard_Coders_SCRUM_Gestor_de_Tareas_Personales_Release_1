# ETAPA 3 — Guía de revisión y validación del diseño de pruebas (EC3)

- **Documento de diseño asociado:** `docs/EC3/ETAPA_3_DISENO_PRUEBAS.md`
- **Rama/HEAD (contexto de diseño):** `Refactor-E2` @ `10988a31d81f7596947c93d1e3abc3ddb42028db`
- **Gate de entrada:** Etapa 1 y Etapa 2 en **PASS** (`docs/EC3/ETAPA_1_GUIA_PRUEBAS.md`, `docs/EC3/ETAPA_2_GUIA_PRUEBAS.md`)
- **Alcance de esta guía:** revisión analítica del **diseño** (EP, BVA3, transiciones, tabla de decisión, CREATE complementarios, trazabilidad). Originalmente no afirmaba ejecución (E4/E5/E6 pendientes). **Actualización E4 (2026-11-16, `docs/EC3/ETAPA_4_GUIA_PRUEBAS.md`, PASS):** ejecución real e integración PostgreSQL aislada completadas para DT-ASG-R1..R10, CA-ASG-01..12, CA-USR-01..10 y MIG/compat; consolidación backend 337/337 y frontend 297/297 (Vitest). Las discrepancias observadas deben reflejarse en el diseño (Etapa 3). E5/E6 siguen pendientes.

## 1. Requisitos y archivos fuente

### 1.1 Requisitos/documentos EC3
| Fuente | Uso en revisión |
|---|---|
| `docs/EC3/ANALISIS_EC3.md` | Objetivos, riesgos y alcance funcional de EC3. |
| `docs/EC3/DIVISION_TRABAJO_EC3.md` | Distribución por etapas, entregables y dependencias E3→E4→E5→E6. |
| `docs/EC3/ETAPA_1_GUIA_PRUEBAS.md` | Contratos heredados vigentes (duración tarea en horas 0.25→min, budget separado, `reason` asignación, subtarea sin plan V4, US3.1). |
| `docs/EC3/ETAPA_2_GUIA_PRUEBAS.md` | Alcance A6 `app_user` CREATE, autorización Jefe-only, BROWSER-01..14 PASS y estado de flujos complementarios. |
| `docs/EC3/ETAPA_3_DISENO_PRUEBAS.md` | Matrices EP/BVA3, estados, decisión, CREATE, divergencias y trazabilidad a validar. |

### 1.2 Código y contratos del sistema (HEAD actual)
| Capa | Archivos a cotejar | Qué verificar |
|---|---|---|
| DB/migraciones | `database/migrations/V001__initial_schema.sql`, `V004__project_management_model.sql`, `V005__add_sort_order.sql`, `V006__task_assignment_reason.sql` | Tipos, longitudes, checks, unicidad, FKs, defaults (`numeric(15,2)`, `varchar(160/2000/254/100/500)`, `task_assignment.reason`). |
| Dominio | `domain/model/{Project,ProjectStatus,Stage,Activity,Task,TaskPriority,Subtask,WorkStatus,TaskAssignment,WorkText,PlanRevision,UserAccount}.java` | Estados/transiciones, normalización `reason`, creación/edición/replan, límites de texto/plan. |
| Políticas | `domain/policy/{ProjectAccessPolicy,WbsHierarchyPolicy,AssignmentPolicy,BudgetPolicy,CompletionPolicy}.java` | Permisos (Jefe/Encargado), proyecto abierto, elegibilidad/inactividad/self, presupuesto/duración, jerarquía. |
| Servicios | `application/usecase/{ProjectCommandService,TaskCommandService,HierarchyCommandService,SubtaskCommandService,AssignmentCommandService,UserCommandService}.java` | Guardas, orquestación, duplicados, versión/concurrencia, reglas de negocio aplicadas. |
| Web/errores | `adapter/in/web/**/{*Controller,*Requests,*Responses}.java`, `adapter/in/web/error/GlobalExceptionHandler.java` | Endpoints, Bean Validation, payloads, mapeo HTTP (400/401/403/404/409/412). |
| Frontend | `features/projects/project-form/project-form.{ts,html}`, `features/tasks/wbs/wbs-panel.ts`, `features/tasks/task-form/task-form.ts`, `features/tasks/task-list/task-list.ts`, `features/tasks/assignment-dialog/assignment-dialog.{ts,html}`, `features/users/user-form/user-form.ts` | Campos reales, validaciones UI, `step/maxlength/pattern`, handlers (`validateHours`/`validateBudget`), estados de carga/error, controles inline vs modal. |

## 2. Revisión de EP (Particiones de equivalencia)

### 2.1 Criterios de calidad
- [ ] **No vacías:** cada partición tiene al menos un representante ejecutable o justificable.
- [ ] **No solapadas:** un valor representativo pertenece a una sola partición por campo/condición.
- [ ] **Comportamiento homogéneo:** todos los valores de la partición producen el mismo resultado esperado (misma clase de aceptación/rechazo).
- [ ] **Cobertura:** para cada campo relevante existen particiones válidas e inválidas (o exclusión explícita justificada, ej. subtarea sin campos plan).
- [ ] **Independencia:** no mezclar múltiples causas de fallo en un caso sin necesidad; usar *Each Choice* y añadir combinaciones de riesgo solo cuando aporten cobertura.

### 2.2 Cómo revisar por formulario
1. Abrir la sección de contrato del formulario en `ETAPA_3_DISENO_PRUEBAS.md` (tabla 2.2) y cotejar con UI/DTO/dominio/DB fuente.
2. Verificar que IDs `EP-*` cubren: obligatorios/opcionales, tipo/formato, longitud, numéricos, enums/defaults, unicidad/FK, permisos/contexto.
3. Confirmar resultado esperado por capa: **UI** (mensaje/bloqueo/estado), **API** (HTTP/código cuando aplique), **BD** (persiste/no persiste, valor normalizado).
4. Marcar divergencias entre capas (ej. `DIV-01` motivo proyecto UI 300 vs backend 500) como hallazgo documental, no como error inventado del sistema.
5. Comprobar CREATE complementarios: `task_assignment` (`EP-ASG-*`, `CA-ASG-*`) y `app_user` (`EP-USR-*`, `CA-USR-*`) incluyen caminos válidos, inválidos y seguridad (401/403, no privilegios).

### 2.3 Checklist EP
- [ ] Proyecto: nombre, descripción, encargado, presupuesto, duración (min), motivo — válidas/inválidas.
- [ ] Etapa/Actividad inline: nombre, descripción, contexto/padre.
- [ ] Tarea: título, prioridad, actividad padre, presupuesto Bs, duración horas→min.
- [ ] Subtarea: título, descripción, tarea padre; exclusión explícita de presupuesto/duración/prioridad.
- [ ] Asignación: trabajador existente/activo, actor autorizado, autoasignación, duplicidad, proyecto abierto, `reason`.
- [ ] Usuario: email, nombres, apellidos, contraseña, rol Jefe/privilegios.
- [ ] Sin solapes ni particiones vacías; origen de regla trazado.

## 3. Revisión de BVA3 (n-1, n, n+1)

### 3.1 Criterios
- [ ] **Tres valores por frontera aplicable:** inmediatamente inferior, límite exacto, inmediatamente superior.
- [ ] **Unidad correcta:** entero (+/-1), decimal monetario (+/-0.01 o unidad representable coherente con `DECIMAL(15,2)`), duración tarea UI 0.25 h / 15 min, texto en caracteres.
- [ ] **Ambas fronteras cuando existan:** mínimo y máximo documentados por separado.
- [ ] **Capa que impone la frontera:** HTML/Angular, DTO, dominio, DB (y discrepancias anotadas).
- [ ] **Sin invención:** si no hay mínimo de negocio ordenable, usar EP; subtarea sin campos plan no lleva BVA de esos campos.

### 3.2 Cómo comprobar
1. Para cada fila `BVA3-*`, identificar el límite `n` y la unidad mínima (columna “Tipo/límite”).
2. Verificar aritmética: `n-1` y `n+1` calculados en la misma unidad (`-0.01/0.00/0.01`, `159/160/161`, `499/500/501`, `-0.25h/0/0.25h`, minutos máx ±1).
3. Confirmar resultado esperado por valor (Inválido/Válido/Válido o según frontera).
4. Contrastar con fuente:
   - `task-form.ts`: `validateHours` (múltiplo 0.25, `MAX_DURATION_MINUTES`), `validateBudget` (patrón 13,2), conversión `round(hours*60)`.
   - `BudgetPolicy`: `DECIMAL(15,2)` (13 enteros, 2 decimales), duración ≥0.
   - DTOs: `@Size`, `@Digits`, `@Min/@DecimalMin`, `@MaxLength` implícito por UI/DB.
   - V004/V006: `varchar`, `numeric`, `integer`, checks.
5. Validar casos especiales: `reason` asignación 499/500/501; `reason` normalizado null/vacío tratado por EP; duración tarea no en días; proyecto/etapas en su unidad real (minutos donde aplique).

### 3.3 Checklist BVA3
- [ ] Nombre/título/descripción con fronteras de longitud reales.
- [ ] Presupuesto tarea/proyecto: -0.01/0.00/0.01, máx. 13 enteros, escala 2 vs 3 decimales.
- [ ] Duración tarea: -0.25/0/0.25 h y máximo `MAX_DURATION_MINUTES` en minutos.
- [ ] `reason` asignación 499/500/501.
- [ ] Email/contraseña/nombres usuario según contrato `app_user` (considerar **límite efectivo BCrypt 72 bytes**, DIV-E4-01: 73 ASCII puede dar 500 aunque ≤200).
- [ ] Subtarea sin BVA de presupuesto/duración.
- [ ] Cobertura de cada frontera documentada (inferior y/o superior según aplique).

## 4. Revisión de transición de estados

### 4.1 Criterios
- [ ] Grafo confirmado contra `ProjectStatus.allowedTargets/canMoveTo` y controladores/servicios (no solo informe previo).
- [ ] **Cobertura de estados:** todos los estados alcanzables representados.
- [ ] **Cobertura de transiciones:** todas las válidas ejercitables identificadas y transiciones inválidas relevantes (terminales, saltos, permisos, versión).
- [ ] Guardas explícitas: actor (Jefe para cambio de estado proyecto), versión/concurrencia (`412`), proyecto abierto/cerrado donde aplique.
- [ ] Evidencia observable: estado final, HTTP/dominio, persistencia (`project_status_id`, `updated_at/version`), ausencia de cambio en rechazo.

### 4.2 Cómo revisar
1. Comparar diagrama Mermaid y matriz `ST-PROY-*` con `domain/model/ProjectStatus.java`.
2. Verificar secuencias de alcanzabilidad: `PLANIFICADO→EN_EJECUCION→(EN_PAUSA|FINALIZADO|CANCELADO)`, `EN_PAUSA→EN_EJECUCION/CANCELADO`.
3. Confirmar estados terminales `FINALIZADO/CANCELADO` sin salidas y rechazos 409 `INVALID_PROJECT_TRANSITION`.
4. Revisar casos negativos: no autorizado (403), versión desactualizada (412), payload inválido (400).
5. Revisar complemento tarea/subtarea `ST-TAR-*`: `PENDIENTE↔EN_PROGRESO→COMPLETADA→EN_PROGRESO`, cancelación con subtareas abiertas (409 `TASK_HAS_SUBTASKS`), restricciones de ejecución de subtarea.

### 4.3 Checklist estados
- [ ] 5 estados proyecto cubiertos.
- [ ] 7 transiciones válidas proyecto cubiertas.
- [ ] Inválidas relevantes (terminales, no permitidas, auth, versión, payload) cubiertas.
- [ ] Cobertura de estados y de transiciones reportadas por separado.
- [ ] Complemento tarea/subtarea incluido y coherente.

## 5. Revisión de tabla de decisión (asignación)

### 5.1 Criterios
- [ ] Condiciones y acciones derivadas del flujo real `POST /api/tasks/{taskId}/assignments`.
- [ ] Combinaciones **factibles** identificadas; **inviables/N/A** marcadas con justificación (dependencias imposibles).
- [ ] Reglas con ID, datos/precondiciones, pasos, outcome, HTTP y expectativa de persistencia/no persistencia.
- [ ] Incluye camino feliz y reglas negativas clave: proyecto cerrado, trabajador inactivo, actor no autorizado, autoasignación, duplicidad (más inexistentes/no autenticado según contrato).
- [ ] Cobertura calculada: `reglas ejercitadas / reglas factibles` (diseño = especificadas; ejecución = futura).

### 5.2 Cómo verificar viabilidad y cobertura
1. Cotejar `DT-ASG-R1..R10` con `HierarchyController.assign`, `HierarchyRequests.AssignWorkerRequest`, `AssignmentCommandService`, `AssignmentPolicy`, `WbsHierarchyPolicy`, `TaskAssignment.normalizeReason`, V004/V006.
2. Revisar precedencia/cortocircuito: autenticación/autorización antes de mutación; existencia/relaciones; estado abierto; elegibilidad/duplicidad/self.
3. Validar que “—” significa irrelevante por cortocircuito, no valor asumido.
4. Confirmar factibilidad: R1–R10 ejecutables con datos controlados; combinaciones dependientes imposibles excluidas de denominador.
5. Registrar cobertura de diseño: **10/10 reglas factibles identificadas especificadas** (100% diseño). No declarar 100% ejecución.

### 5.3 Checklist decisión
- [ ] Flujo real modelado correctamente.
- [ ] ≥3 reglas distintas (camino feliz + negativas clave).
- [ ] Factibles vs inviables diferenciadas.
- [ ] HTTP y persistencia coherentes (incl. 400/409/404 cuando hay ambigüedad documentada).
- [ ] Cobertura de reglas factibles calculada y acotada al diseño.

## 6. Enlace de IDs con pruebas futuras (E4/E5/E6)

| Prefijo ID | Tipo | Implementación futura | Nivel principal |
|---|---|---|---|
| `EP-*` | Partición de equivalencia | Casos parametrizados JUnit/servicio/DTO (válido-inválido), pruebas de componente/validación Angular, E2E Playwright para flujos visibles | E4 (dominio/app/DTO/integración), E5 (UI/navegación/persistencia visible) |
| `BVA3-*` | Valores límite | Tests de frontera en dominio/servicio/controlador y formularios; datos límite en API/UI | E4, E5 |
| `ST-PROY-*`, `ST-TAR-*` | Transiciones de estado | Unit/integration de máquina de estados, guards y concurrencia; flujos UI de cambio de estado | E4, E5 |
| `DT-ASG-R*` | Reglas de decisión asignación | Tests de servicio/controlador por regla (happy/403/409/400/404/401) + integración BD; diálogo de asignación | E4, E5 |
| `CA-ASG-*`, `CA-USR-*` | CREATE complementarios | `app_user` y `task_assignment` end-to-end funcional (API+persistencia+login/lectura) y UI | E4, E5 |
| Matriz maestra R-EC3-* | Trazabilidad requisito→caso | Selección de suites de regresión y reporte de cobertura | E6 (consolidación/umbrales) |

**Sugerencia de nomenclatura futura:** `should_<ID>_<comportamiento>` (backend) y `BROWSER/E2E-<ID>` o tags por ID en Playwright para rastrear evidencia a la matriz.

## 7. Checklist PASS/FAIL (revisión del diseño Etapa 3)

| Ítem | Criterio PASS | Criterio FAIL | Estado |
|---|---|---|---|
| G3-01 Inventario | Proyecto, etapa, actividad, tarea, subtarea (y complementarios `app_user`, `task_assignment`) inventariados con campos/contratos reales | Falta formulario o campos clave sin justificar | [ ] |
| G3-02 EP | Particiones válidas/inválidas, no solapadas, representativas, con origen de regla | Solapes, huecos, representantes ambiguos o sin regla | [ ] |
| G3-03 BVA3 | 3 valores por frontera aplicable, unidades/unidad mínima correctas, resultados esperados | Fronteras faltantes, aritmética/unidad incorrecta, BVA inventado | [ ] |
| G3-04 Estados | Grafo/matriz coherente con código, estados y transiciones (válidas/inválidas) cubiertos, guardas/versiones | Grafo no confirmado, transiciones omitidas, sin cobertura separada | [ ] |
| G3-05 Decisión asignación | Condiciones/acciones reales, factibles vs inviables, reglas clave + cobertura factible calculada | Combinaciones imposibles como factibles, sin cobertura o reglas insuficientes | [ ] |
| G3-06 CREATE complementarios | `task_assignment` y `app_user` con casos válidos, inválidos, seguridad y persistencia esperada | Falta CREATE o casos críticos (duplicado, auth, normalización) | [ ] |
| G3-07 Trazabilidad | Cada caso/requisito enlazado a formulario, técnica y etapa E4/E5/E6 | IDs huérfanos o sin asignación de ejecución | [ ] |
| G3-08 Documentación | `ETAPA_3_DISENO_PRUEBAS.md` y `ETAPA_3_GUIA_PRUEBAS.md` completos, revisables, sin afirmar ejecución | Documentos incompletos o con afirmaciones de ejecución inexistente | [ ] |
| G3-09 Integridad de alcance | Sin cambios en lógica/endpoints/migraciones/UI; sin suites E4/E5 implementadas | Modificaciones de producto o ejecución no solicitada | [ ] |

**Resultado de revisión:** PASS solo si todos los ítems aplicables están en PASS. Cualquier FAIL requiere corrección documental antes de cerrar Etapa 3.

## 8. Discrepancias pendientes y riesgos a vigilar en ejecución (E4/E5)

| Ref. | Discrepancia/riesgo | Acción recomendada en ejecución |
|---|---|---|
| DIV-01 | Motivo proyecto: UI `maxlength=300` vs DTO/dominio `500` | Probar límite efectivo por canal (UI nativo vs API). |
| DIV-02 | `GET /api/users` no contrato operativo confirmado | No depender de listado; validar por create/login/BD. | **Confirmado E4** (CA-USR sin listado). |
| DIV-03 | Duplicado `app_user`: 400/409 según mapeo | Aceptar según handler real y registrar evidencia. | **Confirmado E4** (400/409). |
| DIV-04 | Duplicado `task_assignment`: 400/409 | Verificar integridad PK y mapeo. | **Confirmado E4** (400/409). |
| DIV-05 | Asignación en proyecto cerrado: 409 esperado | Confirmar mensaje/código exacto. | **Confirmado E4** (409). |
| DIV-06 | Duración tarea no múltiplo 0.25 depende de handler (submit `type="button"`) | Ejercitar 0.1/0.3 y valores frontera por UI y API. | Pendiente E5 (regresión FE 297/297 en E4). |
| DIV-E4-01 | Contraseña: política 8–200 chars vs BCrypt >72 bytes → 73 ASCII dio **500** (no 400), 7/201→400, 8/72→201 | Alinear validación/contrato o manejo de error en producto; en pruebas considerar frontera efectiva 72 bytes. | **Observado E4** (`CA-USR-05/BVA3-USR-PWD`). |
| DIV-E4-02 | DT-ASG-R9 inconsistente/no visible resuelto como 403/404/400 sin persistencia | Aceptar según política/visibilidad; detallar en E5 si aplica. | **Observado E4**. |
| DIV-E4-03 | `ng test --browsers=ChromeHeadless` requiere @vitest/browser-* | Usar `npx vitest run` para consolidación FE. | **Observado E4** (297/297). |

## 9. Conclusión de la guía

La revisión del diseño se considera **satisfactoria** cuando: (1) EP/BVA3/estados/decisión son consistentes con las fuentes del HEAD, (2) las divergencias están explícitas y no ocultas (incluidas las observadas en E4: DIV-E4-01..03, confirmaciones DIV-02..05/R9), (3) la cobertura de reglas factibles y fronteras es verificable, y (4) los IDs están listos para trazarse en E4/E5/E6. E4 cerró ejecución/integración con evidencia en `docs/EC3/ETAPA_4_GUIA_PRUEBAS.md`; esta guía sigue siendo referencia de revisión de diseño y trazabilidad para E5/E6.
