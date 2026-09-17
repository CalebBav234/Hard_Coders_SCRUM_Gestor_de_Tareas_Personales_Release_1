# SonarQube local y práctica de calidad

Esta integración mantiene SonarQube separado de `gestor-tareas-db`: usa sus propios contenedores, base de datos, red y volúmenes. No modifica datos ni migraciones de la aplicación y expone la interfaz solo en `127.0.0.1:9000`.

## Preparación (solo una vez)

Requisitos: Docker Desktop en ejecución, Java 21 y Maven. El escáner se ejecuta con Docker, por lo que no se instala ni se agrega un ejecutable global al `PATH`.

```powershell
Copy-Item .env.sonarqube.example .env.sonarqube
# Edita .env.sonarqube y sustituye CAMBIA_ESTA_CONTRASENA_LOCAL.
docker compose --env-file .env.sonarqube -f docker-compose.sonarqube.yml up -d
```

Espera a que el servicio responda:

```powershell
Invoke-WebRequest http://localhost:9000/api/system/status | Select-Object -Expand Content
```

El resultado esperado es `{"id":"...","status":"UP"}`. Abre <http://localhost:9000>, inicia sesión por primera vez con `admin` / `admin` y establece una contraseña nueva. Después, en **My Account > Security**, crea un token de análisis y cópialo inmediatamente: SonarQube no vuelve a mostrarlo.

El proyecto se crea automáticamente en la primera ejecución del escáner. Su clave estable es `hard-coders-gestor-tareas`; no la cambies entre A1, A2 y A3, porque perderías el historial comparativo.

## Ejecutar un análisis

Primero genera el bytecode Java y verifica las pruebas. Luego ejecuta el escáner en una terminal cuyo historial no vayas a compartir:

```powershell
cd backend
mvn clean test
cd ..
.\scripts\sonar\Invoke-SonarScan.ps1 -Token 'TOKEN_CREADO_EN_SONARQUBE'
```

El token no se escribe en ningún archivo ni se sube a Git. El resultado correcto incluye `ANALYSIS SUCCESSFUL` y `EXECUTION SUCCESS`.

## Flujo requerido por la práctica

1. Ejecuta A1 antes de hacer cambios para SonarQube y registra Overview, Issues y Activity: Issues/severidad, Security, Reliability, Maintainability, complejidad, duplicación, hotspots, esfuerzo técnico, cobertura y Quality Gate.
2. Selecciona al menos cinco issues significativos si A1 contiene cinco o más; documenta regla, impacto, decisión, cambio antes/después y resultado.
3. Refactoriza sin ocultar código mediante exclusiones, ejecuta pruebas y lanza A2 con la misma clave.
4. En **Quality Profiles**, copia `Sonar way` de Java a `Hard Coders - Java` y, como práctica, configura *Cognitive Complexity of methods should not be too high* con umbral 12. Asócialo al proyecto.
5. En **Quality Gates**, crea `Hard Coders - Quality Gate` y asócialo al proyecto. Las condiciones usadas en la práctica son: New Code Issues > 0; New Code Security Hotspots Reviewed < 100%; New/Overall Duplicated Lines (%) > 3%; Overall Maintainability Rating peor que A; Overall Reliability Rating peor que B; Overall Security Rating peor que A.
6. Ejecuta A3 y compara A1/A2/A3 desde Overview y Activity. No añadas todavía cobertura como condición de gate: el repositorio no genera ni importa un informe JaCoCo XML, por lo que 0% no demuestra ausencia de pruebas.

## Detener o reiniciar

```powershell
docker compose --env-file .env.sonarqube -f docker-compose.sonarqube.yml stop
docker compose --env-file .env.sonarqube -f docker-compose.sonarqube.yml up -d
```

No ejecutes `down -v` salvo que quieras borrar de forma irreversible SonarQube, sus usuarios, tokens, configuración, historial y análisis.

## Alcance del escáner

Se analizan `backend/src/main/java` y `frontend/src`; se reconocen pruebas Java y Angular. Se excluyen únicamente dependencias, salidas generadas y archivos de prueba de la métrica de código fuente. `.scannerwork` es salida transitoria e ignorada por Git.

El escáner corre dentro de Linux/Docker. Para que también funcione cuando el proyecto está abierto como un *worktree* Git de Windows, `sonar.scm.disabled=true` evita intentar importar *blame* con rutas de Windows no montadas en el contenedor. Esto no excluye archivos ni altera issues, reglas, métricas o el historial de los análisis; solo omite la atribución de líneas a autores.
