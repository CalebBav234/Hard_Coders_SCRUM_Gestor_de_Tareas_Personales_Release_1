# Evidencias EC4 — Índice de artefactos

**Elemento de Competencia 4 — BDD + Gherkin + CRUD Full-Stack + Pruebas de Integración + Cobertura de Pruebas + CI/CD**  
**Repositorio:** `CalebBav234/Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1`  
**Rama:** `Refactor-E2`  
**SHA documentada:** `8d0c9c925ad88248b57c74fd3fe1cb208e19dac3`  
**Fecha de generación del índice:** 26/11/2026  
**Idioma:** Español  
**Origen:** validación local del 26/11/2026 (no existe run real de GitHub Actions)

---

## Instrucciones de lectura

Todas las rutas son **relativas desde `docs/EC4/evidencias/`**.  
Los reportes de `backend/target/` y `frontend/coverage/` son artefactos locales de la ejecución del 26/11/2026, no versionados en Git (`.gitignore`); se citan por ruta relativa y no se copian aquí para evitar duplicar artefactos pesados.  
No existen capturas de pantalla de cobertura (ver bloqueo documentado en sección 7.4 de `BASE_DOCUMENTAL_EC4.md`).

---

## Tabla de evidencias

| # | Artefacto | Origen | Fecha de generación | Escenario / métrica que acredita | Ruta relativa (desde `docs/EC4/`) | Integridad / observación | Local o CI |
|---|---|---|---|---|---|---|---|
| E-01 | Feature: proceso principal | `backend/src/test/resources/features/ec4/proceso-principal.feature` | 25/11/2026 | PP-01 (jerarquía Proyecto→Etapa→Actividad→Tarea→Subtarea) | `../../backend/src/test/resources/features/ec4/proceso-principal.feature` | Versionado en Git, tag `@PP-01` | Local |
| E-02 | Feature: CRUD `app_user` | `backend/src/test/resources/features/ec4/app-user-crud.feature` | 26/11/2026 | USR-INS-01..03, USR-UPD-01..03, USR-DEL-01, USR-SEL-01, USR-SEL-02 | `../../backend/src/test/resources/features/ec4/app-user-crud.feature` | Versionado en Git, tag `@USR` | Local |
| E-03 | Feature: CRUD `task_assignment` | `backend/src/test/resources/features/ec4/task-assignment-crud.feature` | 26/11/2026 | ASG-INS-01..03, ASG-UPD-01..03, ASG-DEL-01, ASG-SEL-01, ASG-SEL-02 | `../../backend/src/test/resources/features/ec4/task-assignment-crud.feature` | Versionado en Git, tag `@ASG` | Local |
| E-04 | Step Definitions PP-01 | `PrincipalProcessSteps.java` | 25/11/2026 | PP-01 | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/PrincipalProcessSteps.java` | Versionado en Git | Local |
| E-05 | Step Definitions `app_user` | `UserCrudSteps.java` | 26/11/2026 | USR-INS-01..03, USR-UPD-01..03, USR-DEL-01, USR-SEL-01, USR-SEL-02 | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/UserCrudSteps.java` | Versionado en Git | Local |
| E-06 | Step Definitions `task_assignment` | `AssignmentCrudSteps.java` | 26/11/2026 | ASG-INS-01..03, ASG-UPD-01..03, ASG-DEL-01, ASG-SEL-01, ASG-SEL-02 | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/steps/AssignmentCrudSteps.java` | Versionado en Git | Local |
| E-07 | Hooks Cucumber | `Hooks.java` | 25/11/2026 | Limpieza BD por escenario, jefe autenticado, token JWT | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/hooks/Hooks.java` | Versionado en Git | Local |
| E-08 | ScenarioContext | `ScenarioContext.java` | 25/11/2026 | Estado aislado por escenario | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/context/ScenarioContext.java` | Versionado en Git | Local |
| E-09 | ApiClient | `ApiClient.java` | 25/11/2026 | Cliente HTTP real (RestTemplate) contra API Spring Boot | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/support/ApiClient.java` | Versionado en Git | Local |
| E-10 | CucumberIntegrationTest | `CucumberIntegrationTest.java` | 25/11/2026 | Punto de entrada JUnit 5, 19 escenarios, `@PP-01 or @USR or @ASG` | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/CucumberIntegrationTest.java` | Versionado en Git | Local |
| E-11 | CucumberSpringConfiguration | `CucumberSpringConfiguration.java` | 25/11/2026 | Contexto Spring `@SpringBootTest RANDOM_PORT`, `gestor_tareas_test` | `../../backend/src/test/java/com/hardcoders/taskmanager/bdd/CucumberSpringConfiguration.java` | Versionado en Git | Local |
| E-12 | Salida de consola Cucumber | Ejecución `mvn test -Dtest=CucumberIntegrationTest` | 26/11/2026 | `19 Scenarios (19 passed), 123 Steps (123 passed)` | `docs/EC4/REPORTE_REGRESION_EC4.md` (sección 3) | Texto transcrito del reporte local | Local |
| E-13 | Surefire XML (backend) | `backend/target/surefire-reports/` | 26/11/2026 | 412 pruebas backend (unitarias + integración + Cucumber), 0 fallos | `../../backend/target/surefire-reports/` | No versionado (`.gitignore`), artefacto local | Local |
| E-14 | Cucumber Surefire XML | `backend/target/surefire-reports/TEST-com.hardcoders.taskmanager.bdd.CucumberIntegrationTest.xml` | 26/11/2026 | Ejecución Cucumber 19 escenarios aprobados | `../../backend/target/surefire-reports/TEST-com.hardcoders.taskmanager.bdd.CucumberIntegrationTest.xml` | No versionado (`.gitignore`) | Local |
| E-15 | JaCoCo index HTML | `backend/target/site/jacoco/index.html` | 26/11/2026 | Cobertura backend: LINE 95,14 %, BRANCH 85,26 % | `../../backend/target/site/jacoco/index.html` | No versionado (`.gitignore`) | Local |
| E-16 | JaCoCo CSV | `backend/target/site/jacoco/jacoco.csv` | 26/11/2026 | Datos fuente de métricas JaCoCo (167 clases) | `../../backend/target/site/jacoco/jacoco.csv` | No versionado (`.gitignore`) | Local |
| E-17 | JaCoCo XML | `backend/target/site/jacoco/jacoco.xml` | 26/11/2026 | Reporte XML JaCoCo | `../../backend/target/site/jacoco/jacoco.xml` | No versionado (`.gitignore`) | Local |
| E-18 | Vitest coverage index HTML | `frontend/coverage/gestor-tareas/index.html` | 26/11/2026 | Cobertura frontend: St 92,95 %, Br 88,53 %, Fu 87,37 %, Li 94,05 % | `../../frontend/coverage/gestor-tareas/index.html` | No versionado (`.gitignore`) | Local |
| E-19 | Vitest coverage JSON | `frontend/coverage/gestor-tareas/coverage-final.json` | 26/11/2026 | Datos fuente de métricas Vitest (28 archivos) | `../../frontend/coverage/gestor-tareas/coverage-final.json` | No versionado (`.gitignore`) | Local |
| E-20 | Vitest clover XML | `frontend/coverage/gestor-tareas/clover.xml` | 26/11/2026 | Reporte cobertura XML (formato Clover) | `../../frontend/coverage/gestor-tareas/clover.xml` | No versionado (`.gitignore`) | Local |
| E-21 | Playwright report histórico | `frontend/playwright-report/index.html` | 20/11/2026 | Reporte E2E histórico preservado (no regenerado en esta etapa) | `../../frontend/playwright-report/index.html` | 565.912 bytes, sin modificar, protegido | Local |
| E-22 | Playwright E2E ejecución actual | `npx playwright test --output <directorio temporal>` | 26/11/2026 | 25 pruebas E2E, 0 fallos (auth 6, main-flow 1, task-assignment 3, validations 3, user-create 4, user-crud 8) | `docs/EC4/REPORTE_REGRESION_EC4.md` (sección 5) | Salida redirigida a temp para no sobrescribir E-21 | Local |
| E-23 | Workflow CI GitHub Actions | `.github/workflows/ec4-quality.yml` | 26/11/2026 | Pipeline CI definido, jobs backend/frontend/e2e/quality-gate, no ejecutado en GitHub | `../../.github/workflows/ec4-quality.yml` | Versionado en Git, sin run real remoto | Local (no CI) |
| E-24 | BASELINE_EC4.md | `docs/EC4/BASELINE_EC4.md` | 26/11/2026 | Línea base etapa 0 y 1, auditoría, métricas base | `BASELINE_EC4.md` | Versionado en Git | Local |
| E-25 | IMPLEMENTACION_EC4_BITACORA.md | `docs/EC4/IMPLEMENTACION_EC4_BITACORA.md` | 26/11/2026 | Decisiones y resultados por etapa (Etapas 1 a 5) | `IMPLEMENTACION_EC4_BITACORA.md` | Versionado en Git | Local |
| E-26 | REPORTE_REGRESION_EC4.md | `docs/EC4/REPORTE_REGRESION_EC4.md` | 26/11/2026 | Resultados de integración y regresión (Etapa 4) | `REPORTE_REGRESION_EC4.md` | Versionado en Git | Local |
| E-27 | CI_EC4.md | `docs/EC4/CI_EC4.md` | 26/11/2026 | Configuración del pipeline CI (Etapa 5) | `CI_EC4.md` | Versionado en Git | Local |
| E-28 | BASE_DOCUMENTAL_EC4.md | `docs/EC4/BASE_DOCUMENTAL_EC4.md` | 26/11/2026 | Documento canónico maestro (9 apartados) | `BASE_DOCUMENTAL_EC4.md` | Versionado en Git | Local |
| E-29 | INFORME_FINAL_EC4.md | `docs/EC4/INFORME_FINAL_EC4.md` | 26/11/2026 | Informe consolidado del equipo | `INFORME_FINAL_EC4.md` | Versionado en Git | Local |
| E-30 | Informe Integrante 1 | `docs/EC4/informes/INFORME_EC4_INTEGRANTE_1.md` | 26/11/2026 | Informe individual Integrante 1 | `informes/INFORME_EC4_INTEGRANTE_1.md` | Versionado en Git | Local |
| E-31 | Informe Integrante 2 | `docs/EC4/informes/INFORME_EC4_INTEGRANTE_2.md` | 26/11/2026 | Informe individual Integrante 2 | `informes/INFORME_EC4_INTEGRANTE_2.md` | Versionado en Git | Local |
| E-32 | Informe Integrante 3 | `docs/EC4/informes/INFORME_EC4_INTEGRANTE_3.md` | 26/11/2026 | Informe individual Integrante 3 | `informes/INFORME_EC4_INTEGRANTE_3.md` | Versionado en Git | Local |

---

## Evidencias no disponibles (bloqueos documentados)

| ID | Artefacto esperado | Motivo del bloqueo | Impacto |
|---|---|---|---|
| E-B-01 | `docs/EC4/evidencias/cobertura/jacoco-resumen.png` | Las herramientas de navegador bloquean el protocolo `file://`; no se levantó un servidor HTTP local para servir el reporte HTML de JaCoCo | Captura de cobertura JaCoCo no generada; se referencia E-15/E-16 en su lugar |
| E-B-02 | `docs/EC4/evidencias/cobertura/vitest-resumen.png` | Mismo motivo que E-B-01 para el reporte HTML de Vitest | Captura de cobertura Vitest no generada; se referencia E-18/E-19 en su lugar |
| E-B-03 | Run real de GitHub Actions (workflow ID, URL, conclusión) | El workflow `.github/workflows/ec4-quality.yml` está definido pero no se ha disparado en GitHub (sin integración remota en esta sesión) | CI reportado como definido, no ejecutado; no hay run ID ni URL verificable |
| E-B-04 | Validación de navegador real vía Playwright MCP | Las herramientas del servidor Playwright MCP no están disponibles/invocables en esta sesión | Validación MCP bloqueada; las pruebas E2E sí se ejecutaron con Playwright Test (E-22) |

---

## Integridad y observaciones generales

- No se incluyeron secretos, `.env`, JWT, contraseñas, tokens ni datos identificables de usuarios de prueba.
- Los ejemplos de datos usan emails anonimizados (`usr.<uuid>@gestor.local`, `worker.<uuid>@gestor.local`, `chief.ec4@<timestamp>@gestor.local`).
- No se generó ningún PDF. El entregable documental es Markdown.
- No se modificó código de producto, configuración, dependencias, pruebas ni base de datos durante esta etapa documental.
- No se realizaron commits ni pushes.
- Los reportes de `backend/target/` y `frontend/coverage/` no están versionados en Git (`.gitignore`); se citan por ruta relativa como artefactos locales de la ejecución del 26/11/2026.
- `frontend/playwright-report/index.html` (E-21) se preservó intacto (565.912 bytes, 20/11/2026) y no fue sobrescrito.

---
*Fin del índice de evidencias.*
