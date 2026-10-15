# Informe de pruebas — Persona 4

## Asignación de personal, permisos de ejecución y manejo de errores

* **Fecha de ejecución:** 15 de octubre de 2026
* **Proyecto:** Gestor de Tareas Personales — Release 1
* **Rama revisada:** `pruebas-manuales`
* **Commit:** `b5ef7ac` (`feat: terminado`)
* **Responsable del bloque:** Persona 4

## 1. Alcance y método

Se ejecutaron pruebas manuales de la asignación de personal a tareas, las restricciones de acceso según el rol, la ejecución de tareas y el comportamiento de la aplicación ante la indisponibilidad del backend y una sesión inválida.

Las pruebas se realizaron desde la interfaz web de la aplicación, utilizando cuentas de prueba con diferentes roles: Jefe, Encargado y Personal. Se observaron los controles disponibles, los mensajes mostrados, los cambios de estado y la información accesible para cada usuario.

No se modificó el código fuente durante la ejecución de estas pruebas. Se conservaron capturas de algunas comprobaciones, pero no de todas.

Los resultados se clasifican como **Aprobada**, **Aprobada parcialmente** o **Fallida**, de acuerdo con el comportamiento observado. Cuando un criterio no se comprobó completamente, se indica expresamente.

## 2. Entorno verificado

| Componente | Dirección o ubicación | Resultado |
|---|---|---|
| Frontend Angular | `http://localhost:4200` | Disponible durante las pruebas |
| Backend Spring Boot | `http://localhost:8080` | Disponible durante las pruebas, salvo P9.2 |
| Base de datos PostgreSQL | Base local del proyecto | Utilizada por la aplicación; no se realizaron modificaciones directas durante estas pruebas |
| Rama revisada | `pruebas-manuales` | Rama indicada para la ejecución |
| Commit | `b5ef7ac` | `feat: terminado` |

Se utilizaron cuentas de prueba de los roles Jefe, Encargado y Personal. También se verificó que el usuario inactivo no apareciera entre las opciones disponibles para asignar personal.

## 3. Resumen de resultados

| Prueba | Verificación realizada | Evidencia observada | Resultado |
|---|---|---|---|
| P5.1 | Abrir diálogo de asignación | Se abre el diálogo y muestra el selector de trabajador, la lista de personal asignado y el campo de motivo opcional | **APROBADA** |
| P5.2 | Asignar personal a una tarea | La aplicación confirma la asignación y muestra al trabajador en la lista de personal asignado | **APROBADA** |
| P5.3 | Rechazar asignaciones duplicadas | El trabajador ya asignado desaparece del selector; no se comprobó la respuesta ante un intento real de duplicación | **APROBADA PARCIALMENTE** |
| P5.4 | Impedir la autoasignación del Jefe | El Jefe aparece en el selector, pero la aplicación rechaza el intento de asignarse a sí mismo | **APROBADA** |
| P5.5 | Retirar asignación y comprobar pérdida de acceso | Al retirar a un usuario de Personal, sus tareas dejan de aparecer al iniciar sesión con esa cuenta | **APROBADA** para el caso de Personal |
| P5.6 | Mostrar únicamente usuarios activos | El selector muestra al Jefe, dos Encargados y un usuario de Personal; el usuario inactivo no aparece | **APROBADA** |
| P6.1 | Restringir acceso a proyectos para Personal | La cuenta de Personal muestra únicamente sus tareas, sin una sección visible de proyectos | **APROBADA** |
| P6.2 | Mostrar solo tareas asignadas | En la cuenta de Personal solo aparecen las tareas asignadas a ese usuario | **APROBADA** |
| P6.3 | Restringir información financiera | No se observan presupuestos, costos, gastos ni importes económicos en la cuenta de Personal | **APROBADA** |
| P6.4 | Activar y completar tareas asignadas | El usuario de Personal puede activar y completar las tareas que tiene asignadas | **APROBADA** |
| P6.5 | Impedir el acceso a tareas no asignadas | Las tareas que no están asignadas al usuario no aparecen en su cuenta | **APROBADA** |
| P6.6 | Completar desde el Jefe una tarea asignada a Personal y registrar la acción | El Jefe puede completar la tarea y el historial registra el responsable y el cambio de estado; no registra el motivo | **APROBADA PARCIALMENTE** |
| P9.2 | Comportamiento con backend no disponible | Con Spring Boot detenido, la aplicación muestra «No se pudieron cargar los proyectos» | **APROBADA PARCIALMENTE** |
| P9.3 | Comportamiento ante sesión inválida | Tras eliminar el token de autenticación de Session Storage, la aplicación vuelve al inicio de sesión | **APROBADA** |

## 4. Evidencias detalladas

### P5.1 — Apertura del diálogo de asignación

Se accedió al diálogo de asignación de personal desde una tarea. Se observaron los siguientes elementos:

- Título «Asignar personal».
- Información de la tarea.
- Lista de personal asignado.
- Selector de trabajador.
- Campo «Motivo (opcional)».
- Botón «Asignar».

**Resultado:** el diálogo se abre correctamente y permite acceder a los controles necesarios para asignar personal.

### P5.2 — Asignación de personal

Se seleccionó un trabajador y se confirmó la operación mediante el botón «Asignar».

La aplicación mostró el mensaje «Personal asignado» y el trabajador apareció en la lista de personal asignado, con la opción «Retirar».

**Resultado:** la asignación se confirma visualmente y el trabajador aparece asociado a la tarea.

### P5.3 — Prevención de asignaciones duplicadas

Después de asignar un trabajador, este dejó de aparecer en el selector de nuevos trabajadores disponibles. Esto impide seleccionarlo nuevamente mediante el flujo normal observado en la interfaz.

No se realizó una petición directa a la API ni se verificó la respuesta del servidor ante un intento real de duplicación.

**Resultado:** aprobado parcialmente. Se comprobó el filtrado visual del trabajador ya asignado, pero queda pendiente verificar que el sistema rechace una asignación duplicada en el backend.

### P5.4 — Prevención de autoasignación

Con la cuenta del Jefe, se comprobó que el propio usuario aparecía entre las opciones del selector. Al intentar asignarse a sí mismo, la aplicación rechazó la operación y mostró un mensaje indicando que no podía realizar esa asignación.

**Resultado:** la operación fue rechazada, aunque el usuario aparezca inicialmente entre las opciones disponibles.

### P5.5 — Retiro de asignación y pérdida de acceso

Se retiró la asignación de un usuario que tenía el rol Personal. Después, se inició sesión con esa cuenta y se comprobó que sus tareas ya no aparecían.

Se observó un comportamiento diferente al considerar a un Encargado del proyecto: este podía seguir viendo y asignando tareas. Debido a que conserva funciones de gestión del proyecto, ese comportamiento debe analizarse según las reglas de autorización de su rol y no debe interpretarse automáticamente como un fallo de revocación de la asignación de Personal.

**Resultado:** aprobado para el escenario de retiro de asignación de un usuario de Personal.

### P5.6 — Exclusión de usuarios inactivos

En el selector de trabajadores se observaron cuatro usuarios: el Jefe, dos Encargados y un usuario de Personal. El usuario inactivo no apareció entre las opciones.

**Resultado:** la interfaz excluye al usuario inactivo del selector de asignación.

### P6.1 — Restricción de acceso a proyectos

Al iniciar sesión con la cuenta de Personal, se observó que la interfaz mostraba únicamente sus tareas y no una sección visible de proyectos.

**Resultado:** aprobado según el comportamiento observado en la interfaz.

### P6.2 — Visibilidad limitada a tareas asignadas

Se revisó la lista de tareas de la cuenta de Personal. Solo aparecieron las tareas asignadas a ese usuario; no se mostraron tareas correspondientes a otros trabajadores.

**Resultado:** la lista visible respeta el alcance de las asignaciones observadas.

### P6.3 — Restricción de información financiera

Se revisaron las pantallas y los detalles accesibles desde la cuenta de Personal. No se encontraron datos de presupuestos, costos, gastos ni importes económicos.

**Resultado:** aprobado para las pantallas inspeccionadas. La prueba visual no demuestra por sí sola la ausencia de acceso a esos datos mediante llamadas directas a la API.

### P6.4 — Activación y finalización de tareas

Desde la cuenta de Personal se comprobó que era posible activar una tarea asignada y marcarla como terminada.

**Resultado:** ambas operaciones estuvieron disponibles y funcionaron para las tareas asignadas que se probaron.

### P6.5 — Restricción de acceso a tareas no asignadas

Se comprobó que la cuenta de Personal no mostraba las tareas que no tenía asignadas.

**Resultado:** aprobado para la visibilidad comprobada desde la interfaz. No se realizó una prueba de acceso directo a una tarea no asignada mediante su URL o identificador.

### P6.6 — Finalización por parte del Jefe e historial

Se comprobó que el Jefe podía marcar como terminada una tarea asignada a un usuario de Personal.

También se revisó el historial de la tarea. El historial registraba quién realizó el cambio y la transición de estado, pero no se encontró un campo de motivo en la tarea ni un motivo registrado en el historial.

**Resultado:** aprobado parcialmente. La finalización por parte del Jefe y la identificación del responsable funcionan según lo observado; el registro del motivo no está disponible en el historial inspeccionado.

### P9.2 — Indisponibilidad del backend

Se detuvo temporalmente el proceso de Spring Boot y se recargó la aplicación Angular, manteniendo el frontend abierto.

La aplicación mostró el mensaje:

> No se pudieron cargar los proyectos.

Posteriormente, se volvió a iniciar el backend y se recuperó el servicio.

**Resultado:** aprobado parcialmente. La interfaz informa de que no pudo cargar los proyectos, pero el mensaje es genérico y no identifica explícitamente que el backend está fuera de servicio.

### P9.3 — Sesión inválida

Se inspeccionó Session Storage en el navegador y se identificaron las entradas relacionadas con la sesión, entre ellas `token`, `expiresAt` y `user`.

Para la prueba se eliminó la entrada `token` y se intentó volver a acceder a la aplicación. La aplicación redirigió al usuario al inicio de sesión.

Después de la prueba, se debía iniciar sesión nuevamente para restablecer la sesión normal.

**Resultado:** aprobado. La aplicación impide continuar con el acceso normal cuando falta el token de autenticación.

## 5. Incidencias y observaciones

### 5.1. Asignaciones duplicadas

No se comprobó directamente el rechazo de una asignación duplicada por parte del backend. El filtrado del trabajador ya asignado en la interfaz es una medida útil, pero no sustituye la comprobación de la regla en el servidor.

### 5.2. Motivo de finalización de tareas

El historial identifica quién realizó el cambio y el nuevo estado, pero no registra el motivo de la finalización de la tarea. Si el requisito funcional exige conservar el motivo, será necesario revisar o ampliar el mecanismo de registro correspondiente.

### 5.3. Mensaje ante la caída del backend

La aplicación muestra «No se pudieron cargar los proyectos». El mensaje permite reconocer que la operación no se completó, aunque podría mejorarse para informar de la indisponibilidad del servicio y ofrecer una opción para volver a intentarlo.

### 5.4. Alcance de las pruebas de autorización

Las comprobaciones de visibilidad se realizaron desde la interfaz web. Para certificar la autorización completa sería conveniente complementar las pruebas con solicitudes directas a la API, especialmente para tareas no asignadas, información financiera y usuarios con roles distintos.

### 5.5. Evidencias gráficas

Se conservaron capturas de algunas pruebas. Las capturas disponibles deben adjuntarse a las secciones correspondientes; no se deben presentar como evidencia capturas que no se hayan tomado.

## 6. Conclusión

Las pruebas manuales de Persona 4 permitieron comprobar el funcionamiento de la asignación y el retiro de personal, la exclusión de usuarios inactivos, las restricciones de visibilidad para Personal y las operaciones de activación y finalización de tareas.

También se verificó que el Jefe puede completar tareas asignadas a Personal y que el historial registra quién realizó el cambio de estado. Sin embargo, no se registra el motivo de la acción en el historial inspeccionado.

En las pruebas de errores, la aplicación mostró un mensaje genérico cuando el backend no estaba disponible y redirigió al inicio de sesión al eliminar el token de autenticación.

En general, el comportamiento observado fue satisfactorio en los escenarios ejecutados. Las principales observaciones pendientes son la comprobación directa de asignaciones duplicadas en el backend, el registro de motivos en el historial y la ampliación de las pruebas de autorización mediante solicitudes directas a la API.

El informe refleja los resultados realmente observados desde la interfaz web y distingue los criterios verificados de aquellos que requieren pruebas complementarias.
