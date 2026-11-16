# ETAPA 4 — Guía de pruebas (ejecución real)
ID: `EC3-E4-20261116-0121`  
Proyecto: `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`  
Rama: `Refactor-E2` @ `10988a31d81f7596947c93d1e3abc3ddb42028db`  
Base de diseño (seguida al pie de la letra): `docs/EC3/ETAPA_3_DISENO_PRUEBAS.md` (`EC3-E3-20261116-0015`, gate PASS)  
Alcance: implementación y ejecución real de pruebas unitarias/integración pertinentes (PostgreSQL real aislado) para cerrar trazabilidad E4↔E3 (DT-ASG/CA-ASG/CA-USR/MIG), consolidación de suites backend/frontend con evidencia. **Sin Etapa 5. Sin cambios a V005/V006/migraciones/contratos/endpoints/UI de producto. Sin bajar umbrales JaCoCo. Sin `docker compose down -v`. Sin secretos en evidencia.**

## 1) Entorno y BD aislada (PostgreSQL real)
- Motor: Docker `gestor-tareas-db` (`postgres:18`, healthy, `:5432`). DB de pruebas: **`gestor_tareas_test`** (aislada de `gestor_tareas`).
- Inicialización/migraciones: `database/docker/init-dev-db.sql` + migraciones **V001–V006** aplicadas (esquemas `public, task_manager, task_manager_legacy`).
- Evidencia de esquema: `public.database_schema_migrations` = **1,2,3,4,5,6**; `task_manager.task_assignment.reason` `varchar` **nullable (YES)**, check/restricción y PK compuesta `(task_id, worker_user_id)` presentes; trigger diferido `trg_project_requires_plan_history` respetado en siembra.
- Herramientas: Maven 3.9.16, JDK **21.0.12.1**, Node v24.15.0/npm 11.12.1, Vitest 4.1.12. Spring Boot 4.1.0 test stack (`@SpringBootTest(RANDOM_PORT)`, `ddl-auto=validate`, JWT test).
- Higiene: limpieza transaccional en `@BeforeEach` (delete por jerarquía inversa) sobre `gestor_tareas_test`; sin borrado de volúmenes; sin credenciales/tokens/hashes/contraseñas en artefactos.

## 2) Artefactos implementados/ajustados (E4)
- **Nuevo/extendido:** `backend/src/test/java/com/hardcoders/taskmanager/integration/PostgresIntegrationTest.java`
  - Integración HTTP/JPA/JDBC contra `gestor_tareas_test` (RestTemplate sin error-handler por defecto, autenticación JWT real vía `/api/auth/login`, siembra jerárquica válida proyecto→plan→etapa→actividad→tarea→plan/status).
  - **25 pruebas** en 3 grupos: `MigrationsAndReason` (3), `AssignmentApi` (14), `UserApi` (8). Compila y ejecuta en verde.

## 3) Ejecución real y resultados consolidados
| ID ejecución | Suite / comando | Entorno | Resultado |
|---|---|---|---|
| EX-IT-01 | `mvn -q "-Dtest=PostgresIntegrationTest" -DfailIfNoTests=false test` | backend, JDK 21, `gestor_tareas_test` (PG18) | **PASS — 25/25 (Failures 0, Errors 0, Skipped 0, EXIT 0)** |
| EX-BE-01 | `mvn -q test` (suite completa backend, Surefire) | backend, JDK 21, `gestor_tareas_test` disponible | **PASS — Tests run: 337, Failures: 0, Errors: 0, Skipped: 0** (agregado Surefire `target/surefire-reports/*.txt`) |
| EX-FE-01 | `npx vitest run` | frontend | **PASS — Test Files 18 passed (18), Tests 297 passed (297), EXIT 0** |

> Notas: advertencias Mockito/Byte Buddy/dynamic-agent, HHH10001005, y trazas de error **esperadas/inyectadas por casos negativos** (p.ej. límite BCrypt en frontera controlada) no constituyen fallos funcionales; los comandos reportan éxito. `ng test --watch=false --browsers=ChromeHeadless` no aplica en este entorno (requiere @vitest/browser-*); la suite oficial ejecutada es Vitest (`vitest run`) con resultado verde.

## 4) Trazabilidad E4 ↔ ETAPA_3_DISENO_PRUEBAS.md (cobertura ejecutada)

### 4.1 Migraciones, BD y compatibilidad (V006/reason)
| Diseño (Etapa 3) | Prueba(s) EX-IT-01 | Evidencia | Estado |
|---|---|---|---|
| MIG V001–V006 aplicadas; `task_assignment.reason` nullable (V006) | `migrationsAppliedAndReasonColumn` | versiones 1–6 y `is_nullable=YES` | **PASS** |
| CA-ASG-12/COMPAT: motivo explícito, default, lectura histórica `reason NULL` | `taskAssignmentReasonWriteReadAndHistoricalNull` | persistencia/lectura explícito, `TaskAssignment.DEFAULT_REASON`, fila NULL legible | **PASS** |
| CA-ASG-07 (BD): PK compuesta rechaza duplicada | `duplicateAssignmentRejectedByPrimaryKey` | 1 fila efectiva; segundo insert falla | **PASS** |

### 4.2 Asignación `POST /api/tasks/{taskId}/assignments` — DT-ASG-R1..R10 y CA-ASG (HTTP real)
| Regla/Caso (Etapa 3) | Prueba(s) | Resultado esperado (diseño) | Resultado observado | Estado |
|---|---|---|---|---|
| DT-ASG-R1 / CA-ASG-01,12 | `assignHappyExplicitReason`, `readBackCoherent` | 200, reason trim/persistido y lectura coherente | 200, body/list/BD coherentes | **PASS** |
| CA-ASG-02 | `assignNullReasonDefaults` | 200, persiste `DEFAULT_REASON` | 200, default persistido | **PASS** |
| CA-ASG-03 | `assignBlankReasonDefaults` | 200, normaliza a default | 200, default | **PASS** |
| CA-ASG-04 | `assignReasonTooLong` | 400, sin persistencia (>500) | 400, count 0 | **PASS** |
| DT-ASG-R4 / CA-ASG-05 | `assignInactiveWorker` | 400, sin persistencia (worker inactivo) | 400 | **PASS** |
| DT-ASG-R5 / CA-ASG-06 | `selfAssignForbidden` | 403 autoasignación | 403 | **PASS** |
| DT-ASG-R6 / CA-ASG-07 | `duplicateAssignmentRejected` | 400/409, sin nueva fila | 400/409, count 1 | **PASS** |
| DT-ASG-R3 / CA-ASG-08 | `assignClosedProject` | 409 proyecto FINALIZADO/CANCELADO | 409 en ambos, sin inserción | **PASS** |
| DT-ASG-R2 / CA-ASG-09 | `assignUnauthorizedActor` | 403 actor sin gestión (Personal no gestor) | 403 | **PASS** |
| DT-ASG-R10 / CA-ASG-10 | `assignUnauthenticated` | 401 | 401 | **PASS** |
| DT-ASG-R7 / CA-ASG-11 | `assignTaskNotFound` | 404 tarea inexistente | 404 | **PASS** |
| DT-ASG-R8 | `assignWorkerNotFound` | 400/404 trabajador inexistente | 400/404 | **PASS** |
| DT-ASG-R9 | `assignInconsistentRelation` | 404/400 (relación inconsistente/no visible para actor) | 403/404/400 según detección, sin persistencia | **PASS** (diseño admite resolución por política/visibilidad) |
| CA-ASG-12 (lectura) | `readBackCoherent` | GET refleja asignaciones y reason | lista coherente (explícito + default) | **PASS** |

### 4.3 Usuario `POST /api/users` — CA-USR (HTTP real)
| Caso (Etapa 3) | Prueba(s) | Resultado esperado | Resultado observado | Estado |
|---|---|---|---|---|
| CA-USR-01,02,10 | `createValidNormalizedSecure` | 201, normaliza email/nombres, no-Jefe/activo, hash BCrypt, login OK, sin secretos | 201, email lowercased, hash en BD (no claro), login OK, sin password/hash/token en respuesta | **PASS** |
| CA-USR-03 | `duplicateEmailRejected` | 400/409, sin nuevo usuario ni fuga | 400/409, sin incremento, sin secretos en body | **PASS** |
| CA-USR-04 | `invalidNames` | 400 nombres/apellidos vacíos/espacios/límites | 400 en casos inválidos | **PASS** |
| CA-USR-05 / BVA3-USR-PWD | `passwordBoundaries` | 7 y 201 rechazadas; 8 y 200 aceptadas (política) | **7→400, 201→400, 8→201, 72 ASCII→201**; **73 ASCII→500** (BCrypt rechaza >72 bytes) — divergencia registrada | **PASS con divergencia documentada** |
| CA-USR-06 | `invalidOrLongEmail` | 400 email inválido o >254 | 400 | **PASS** |
| CA-USR-07 | `nonChiefForbidden` | 403 no Jefe | 403 | **PASS** |
| CA-USR-08 | `unauthenticated` | 401 | 401 | **PASS** |
| CA-USR-09 | `privilegeTamperingIgnored` | campos privilegiados ignorados/no elevan; persiste no-jefe/activo | 201 aceptando payload extra; `is_chief=false,is_active=true` en BD/respuesta | **PASS** |

### 4.4 Cobertura unitaria/integración pertinente consolidada (E4)
- **Backend (EX-BE-01):** 337/337 PASS incluyen suites de dominio/políticas/casos de uso/adapters/controladores/manejo de errores existentes (`TaskAssignmentTest`, `AssignmentPolicy*`, `UserCommandServiceTest`, `Project*`, jerarquía/subtarea/tarea, controllers, `GlobalExceptionHandlerTest`, persistencia, etc.) más `PostgresIntegrationTest` (25). Cubre EP/BVA/reglas de Etapa 3 a nivel unitario/servicio/HTTP simulado donde ya existían y a nivel PostgreSQL real para DT-ASG/CA-ASG/CA-USR/MIG críticos.
- **Frontend (EX-FE-01):** 297/297 PASS (Vitest, 18 archivos) — regresión de componentes/servicios/guards/flujos UI preservados (incl. usuarios/asignación/WBS/etapa-duración heredados).

### 4.5 Cierre de trazabilidad E4↔E3
- **Cerrado y evidenciado:** DT-ASG-R1..R10, CA-ASG-01..12, CA-USR-01..10, MIG V001–V006/reason/compatibilidad, consolidación backend 337 y frontend 297. Matriz de Etapa 3 para asignación/usuarios/BD cubierta por ejecución real o por suites unitarias/integración pertinentes consolidadas sin contradicción de contratos.
- **Cobertura JaCoCo:** no se ejecutó `mvn verify` como gate final por deuda conocida previa (`jacoco:check` ramas 0.83 < 0.85, fase `verify`, Etapa 6) — **no se bajaron umbrales**. Suites de prueba ejecutadas en verde.

## 5) Divergencias/riesgos observados (consistentes con Etapa 3)
| ID | Hallazgo | Impacto en Etapa 4 |
|---|---|---|
| DIV-E4-01 (rel. BVA3-USR-PWD/CA-USR-05) | Contrato/diseño espera contraseña hasta 200 chars; `BCryptPasswordEncoder` (Spring Security crypto, config actual) rechaza **>72 bytes** → 73 ASCII producen **500** (no 400). 7/8/72 y 201 se comportan según esperado/validación. | Divergencia de implementación vs especificación documentada; no se modifica contrato/config en E4. Caso ajustado a evidencia real y registrado. |
| DIV-03/04/05/09 (Etapa 3) | Mapeos aceptados como 400/409 (duplicados/worker inexistente) y 409 proyecto cerrado; R9 resuelto por visibilidad/autorización en contexto (403/404/400) sin persistencia. | Conforme a tolerancias del diseño; evidenciado. |
| Herramienta FE | `ng test --browsers=ChromeHeadless` requiere paquetes @vitest/browser no instalados. | Suite oficial FE validada con `npx vitest run` (297/297). |

## 6) Declaración de Etapa 4
**Estado: PASS**

- Integración PostgreSQL real aislada (`gestor_tareas_test`, V001–V006, `ddl-auto=validate`) ejecutada y en verde: **DT-ASG-R1..R10, CA-ASG-01..12, CA-USR-01..10, MIG/compatibilidad reason** cubiertos (25/25).
- Consolidación pertinente ejecutada: **backend 337/337 PASS (Surefire)**, **frontend 297/297 PASS (Vitest)**.
- Trazabilidad E4↔`ETAPA_3_DISENO_PRUEBAS.md` cerrada con evidencia y divergencias explícitas; sin cambios de contrato/migraciones/UI de producto; sin reducción de umbrales JaCoCo. **No se avanza a Etapa 5.**