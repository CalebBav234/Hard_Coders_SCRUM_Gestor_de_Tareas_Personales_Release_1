# Estructura del proyecto

Este documento describe la organización del repositorio **Hard Coders - Gestor de Tareas Personales, Release 1**. La aplicación está formada por un frontend Angular, una API Spring Boot y una base de datos PostgreSQL.

## Vista general

```text
/
├── backend/                  API REST Spring Boot y pruebas Java
├── database/                 Migraciones, scripts y modelo PostgreSQL
├── docs/                     Guías funcionales, técnicas y de pruebas
├── frontend/                 Aplicación web Angular
├── scripts/                  Automatización y herramientas auxiliares
├── .kilo/                    Planes y material de trabajo del proyecto
├── .vscode/                  Configuración del espacio de trabajo
├── docker-compose.yml        Base de datos local para desarrollo
├── docker-compose.sonarqube.yml
├── package.json              Herramientas de desarrollo del repositorio
├── .env.example              Ejemplo de variables de entorno
└── README.md                 Introducción e instrucciones de inicio
```

## Backend

El código Java está en `backend/src/main/java/com/hardcoders/taskmanager`. El backend usa Spring Boot, Java 21, JPA y PostgreSQL, y organiza el dominio siguiendo una arquitectura hexagonal:

```text
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/hardcoders/taskmanager/
    │   │   ├── adapter/in/          Adaptadores de entrada: API REST y seguridad
    │   │   ├── adapter/out/         Adaptadores de salida para persistencia
    │   │   ├── application/         Casos de uso, puertos, comandos y resultados
    │   │   ├── configuration/       Configuración de casos de uso y transacciones
    │   │   ├── domain/              Modelo, políticas, puertos y excepciones del dominio
    │   │   └── dto/                  Objetos de transferencia auxiliares
    │   └── resources/
    │       └── application.yml       Configuración de Spring y conexión a la base
    └── test/java/                    Pruebas de adaptadores, aplicación, dominio y arquitectura
```

- `adapter/in` recibe solicitudes HTTP, gestiona la autenticación JWT y traduce errores de la API.
- `application` coordina los casos de uso mediante puertos; contiene comandos y resultados para sus operaciones.
- `domain` concentra las entidades y reglas de negocio, sin depender de Spring ni de la base de datos.
- `adapter/out/persistence` implementa el acceso a PostgreSQL con entidades JPA, repositorios y adaptadores.
- Hibernate está configurado para validar el esquema (`ddl-auto: validate`), no para modificarlo automáticamente.

## Frontend

La aplicación Angular vive en `frontend/`. Sus vistas y componentes se agrupan por funcionalidad:

```text
frontend/
├── package.json
├── angular.json
└── src/
    ├── app/
    │   ├── core/                   Autenticación, modelos, pipes y servicios compartidos
    │   ├── features/
    │   │   ├── auth/               Inicio de sesión
    │   │   ├── projects/           Proyectos, planificación y delegación
    │   │   └── tasks/              Tareas, asignaciones, progreso, historial y WBS
    │   ├── app.config.ts            Configuración de la aplicación
    │   └── app.routes.ts            Rutas
    ├── index.html
    └── styles.css                   Estilos globales
```

Los componentes de cada funcionalidad mantienen juntos sus archivos TypeScript, HTML, estilos y, cuando corresponde, pruebas. `core/services` centraliza la comunicación con la API; los modelos de `core/models` describen los datos compartidos. La configuración de desarrollo de Angular incluye el proxy para enviar las solicitudes `/api` al backend.

## Base de datos

`database/` contiene la definición versionada del esquema `task_manager` y las herramientas para instalarlo y verificarlo:

```text
database/
├── migrations/             Cambios SQL versionados V001 a V004
├── docker/                 Inicialización y datos de desarrollo
├── scripts/                Migración y verificación en PowerShell/SQL
├── diagram/                Diagrama entidad-relación
├── docs/                   Diccionario de datos
├── examples/               Configuración de referencia
└── README.md               Requisitos y guía de instalación
```

Las migraciones se aplican en orden y no deben modificarse después de compartirse o aplicarse. `docker-compose.yml` levanta PostgreSQL para desarrollo; la inicialización monta los scripts de `database/docker` y las migraciones. Consulta [la guía de base de datos](../database/README.md) antes de crear una instalación local.

## Documentación y calidad

`docs/` reúne material funcional y técnico, incluidos:

- `api-v4.md`: contrato de la API.
- `migracion-v004.md`: contexto de la migración al modelo actual.
- `epica-1.md`: autenticación, proyectos, presupuesto y delegación.
- `pruebas-manuales.md` y `pruebas-unitarias/`: procedimientos e informes de pruebas.
- `modelado/`: diagramas y documentación del modelo.
- `sonarqube.md`: configuración del análisis estático.

En `scripts/sonar/` están los scripts auxiliares para SonarQube; `docker-compose.sonarqube.yml` define su entorno separado. Las configuraciones de credenciales de ejemplo están en `.env.example` y `.env.sonarqube.example`: no se deben guardar credenciales reales en Git.

## Flujo entre componentes

1. El navegador carga la aplicación Angular y sus funcionalidades.
2. Los servicios del frontend envían solicitudes HTTP a los endpoints `/api`.
3. En desarrollo, el proxy de Angular dirige esas solicitudes a Spring Boot en el puerto `8080`.
4. Los adaptadores de entrada del backend autentican y traducen la solicitud; la capa de aplicación ejecuta el caso de uso y consulta el dominio.
5. Los adaptadores de persistencia leen o escriben en PostgreSQL, cuyo esquema se administra con las migraciones de `database/`.

Para instrucciones de ejecución y pruebas, consulta el [README principal](../README.md).
