# AUDITORIA INICIAL DEL WORKSPACE

## Tabla de diagnostico

| Elemento | Estado real | Evidencia | Observacion |
|---|---|---|---|
| Lenguaje principal | Java 21 (backend) + TypeScript 6.0.2 / Angular 22 (frontend) | backend/pom.xml:21; frontend/package.json:14-19,29; frontend/tsconfig.json:14 | Backend Spring Boot 4.1, frontend Angular 22 |
| Framework backend | Spring Boot 4.1.0 | backend/pom.xml:10 | Parent POM spring-boot-starter-parent 4.1.0 |
| Framework frontend | Angular 22.1.0 | frontend/package.json:14-19 | Standalone components, sin ngModule |
| Version Java | 21+ (requisito) | backend/pom.xml:21 | NO VERIFICADA en ejecucion (java no en PATH) |
| Version Node.js | >= 24.15 (requisito README) | README.md:95 | NO VERIFICADA en ejecucion (node no en PATH) |
| Build system backend | Maven 3.9+ | backend/pom.xml; README:91 | NO VERIFICADA en ejecucion (mvn no en PATH) |
| Build system frontend | Angular CLI 22 + Vitest 4 | frontend/package.json:24-25,30; frontend/vitest.config.ts | ng build / ng test |
| Package manager | npm 10.7.0 | frontend/package.json:12; package-lock.json existe | node_modules frontend ya instalado |
| Base de datos | PostgreSQL 18 (Docker) o 18 local | docker-compose.yml:3; README:7,93 | Contenedor gestor-tareas-db no activo ahora |
| Tests backend | Si - Mockito (sin BD) | backend/src/test/.../TaskServiceTest.java (587 lineas) | 3 archivos: TaskServiceTest, TaskHistoryIntegrationTest, TaskEditingTest |
| Tests frontend | Si - Vitest + jsdom | frontend/src/app/app.spec.ts, task-list.spec.ts, task-form.spec.ts, task-history.spec.ts, task.service.spec.ts, app-integration.spec.ts | 6 archivos de tests |
| SonarQube existente | NO | No sonar-project.properties, no config en pom.xml, no archivo en repositorio | NO VERIFICADO en workspace |
| Scanner existente | NO | No sonar-scanner, no sonar-maven-plugin en pom.xml | NO VERIFICADO en workspace |
| Docker compose | Si - solo servicio db | docker-compose.yml (20 lineas, 1 service) | PostgreSQL 18, sin SonarQube |
| Git | Si - develop activo, working tree clean | git status: On branch develop, up to date, nothing to commit | Ultimo commit fd3dbc0; ramas: main, develop, feature/US-17-19 |
| Documentacion | Si - README.md, docs/, database/README.md, frontend/README.md | README.md (223 lineas); docs/us-06-us-07.md; docs/us-12-us-13.md; database/README.md (137 lineas) | Excelente documentacion de dominio |
| Estructura | backend/, frontend/, database/, docs/, .kilo/ | Listing directorios raiz | Proyecto bifurcado backend+frontend+BD |
| Archivos generados | backend/target NO existe; frontend/dist SI; frontend/node_modules SI; node_modules raiz SI | backend/target: NO; frontend/dist: SI | Backend no compilado recientemente; frontend ya construido |

## Lenguajes detectados (reales)
- Java 21: backend/src/main/java/**/*.java (30+ archivos)
- TypeScript 6.0.2: frontend/src/**/*.ts (20+ archivos)
- SQL: database/migrations/*.sql, database/docker/init-dev-db.sql
- HTML: frontend/src/**/*.html
- CSS: frontend/src/**/*.css
- YAML: backend/src/main/resources/application.yml, docker-compose.yml
- JSON: package.json, angular.json, tsconfig.json, proxy.conf.json

## Dependencias reales detectadas
Backend (pom.xml):
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- postgresql (runtime)
- spring-boot-starter-test (test)

Frontend (package.json):
- @angular/common, compiler, core, forms, platform-browser, router (^22.1.0)
- rxjs ~7.8.0, tslib ^2.3.0
- Dev: @angular/build ^22.1.5, @angular/cli ^22.1.5, @angular/compiler-cli ^22.1.0, typescript ~6.0.2, vitest ^4.0.8, jsdom ^28.0.0, prettier ^3.8.1

Raiz (package.json):
- vitest ^4.1.1 (devDependency)

## Estado del proyecto
- Branch actual: develop
- Commit actual: fd3dbc0
- Working tree: clean (nothing to commit)
- Proyecto compilable: NO VERIFICADO (Maven no disponible en PATH)
- Proyecto ejecutable: NO VERIFICADO
- Tests existentes: Si, pero no ejecutados en esta sesion

## Configuración SonarQube existente
- sonar-project.properties: NO EXISTE
- sonar-maven-plugin en pom.xml: NO EXISTE
- sonar-project.properties en raiz: NO EXISTE
- Variable de entorno SONAR_TOKEN: NO CONFIGURADA
- Servidor SonarQube en ejecucion: NO VERIFICADO