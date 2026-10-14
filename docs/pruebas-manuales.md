# Pruebas manuales de la aplicacion

Lista completa de comprobaciones que se hacen usando la aplicacion en el navegador.
Cada prueba indica los pasos y el resultado que se debe ver. Si todo pasa, la aplicacion
funciona de punta a punta: acceso, proyectos, estructura (etapas y actividades), tareas,
subtareas, asignacion de Personal, ejecucion, progreso e historial.

Como usar este documento: recorre las pruebas en orden dentro de cada seccion y anota
OK o Falla en la linea "Resultado" de cada una. Al final hay una tabla resumen para
dejar constancia de la ejecucion.

---

## 0. Preparacion

Antes de empezar, los tres servicios deben estar arriba.

### P0.1 Levantar los servicios

1. Levantar la base de datos: docker start gestor-tareas-db
2. En una terminal, arrancar el backend (carpeta backend):
   $env:SPRING_DATASOURCE_PASSWORD='apppass'
   mvn clean spring-boot:run
3. En otra terminal, arrancar el frontend (carpeta frontend): npm start
4. Abrir en el navegador: http://localhost:4200

Resultado: aparece la pantalla de inicio de sesion. El backend responde en
http://localhost:8080 y el frontend en http://localhost:4200.

### P0.2 Usuarios de prueba

La aplicacion tiene cinco usuarios locales, todos con la contrasena Gestor2026!

- jefe@gestor.local       Rol Jefe: ve y gestiona todos los proyectos
- encargado1@gestor.local Rol Encargado: gestiona los proyectos que le deleguen
- encargado2@gestor.local Rol Encargado: para las pruebas de aislamiento
- personal@gestor.local   Rol Personal: solo ve las tareas que le asignan
- inactivo@gestor.local   Usuario desactivado: no puede entrar

Recomendacion: crear un proyecto nuevo con el Jefe y delegarselo a
encargado1@gestor.local para seguir las pruebas con datos propios.

---

## 1. Sesion y acceso

### P1.1 Inicio de sesion correcto

1. Entrar a http://localhost:4200 (redirige a la pantalla de login).
2. Escribir jefe@gestor.local y la contrasena Gestor2026!
3. Pulsar el boton de entrar.

Resultado: entra a la pantalla "Mis tareas". En la cabecera se ve el nombre del
usuario, la palabra Jefe y el menu con Mis tareas, Proyectos e Historial.

### P1.2 Contrasena incorrecta

1. Salir de la sesion.
2. Intentar entrar con jefe@gestor.local y una contrasena equivocada.

Resultado: no entra. Se muestra un mensaje de credenciales invalidas y se permanece
en la pantalla de login.

### P1.3 Usuario inactivo

1. Intentar entrar con inactivo@gestor.local y la contrasena Gestor2026!

Resultado: el inicio de sesion es rechazado. Aparece el mensaje "Tu usuario esta inactivo.
Contacta al administrador." y no se emite token: la pantalla sigue en el login. Este caso es
distinto del de P1.2 a proposito, para que la persona sepa que su cuenta sigue existiendo
pero esta deshabilitada.

### P1.4 Cerrar sesion

1. Con sesion abierta, pulsar el boton Salir de la cabecera.

Resultado: vuelve a la pantalla de login. Si se recarga la pagina o se escribe
http://localhost:4200/historial a mano, la aplicacion redirige al login.

### P1.5 Rol del Personal en la cabecera

1. Entrar con personal@gestor.local

Resultado: entra a "Mis tareas". La cabecera muestra el nombre y la palabra Usuario
(no Jefe).

---

## 2. Proyectos (Epica 1)

Todas las pruebas de esta seccion se hacen con el Jefe salvo que se indique otro rol.

### P2.1 Crear proyecto

1. Ir al menu Proyectos.
2. Pulsar el boton Nuevo proyecto (signo mas).
3. Completar nombre, descripcion, elegir Encargado (encargado1@gestor.local),
   presupuesto 12000 y duracion 480 minutos.
4. Guardar.

Resultado: el proyecto aparece en la lista con estado Planificado, presupuesto
Bs 12.000,00, revision del plan 1 y el Encargado elegido. El proyecto queda listo
para construirle la estructura.

### P2.2 Editar datos del proyecto

1. En la tarjeta del proyecto, pulsar Editar datos.
2. Cambiar el nombre y la descripcion.
3. Guardar.

Resultado: la tarjeta muestra el nombre nuevo. El cambio se ve al recargar la lista.

### P2.3 Revision del plan (presupuesto y duracion)

1. Pulsar Editar plan.
2. Poner presupuesto 15000, duracion 600 y un motivo, por ejemplo "Ajuste de alcance".
3. Guardar.

Resultado: la tarjeta muestra el presupuesto nuevo y la revision del plan sube a 2.
Al abrir Historial del plan se ven las dos revisiones con su motivo, autor y fecha.

### P2.4 Presupuesto por debajo de lo asignado

1. Crear una tarea en una actividad del proyecto con presupuesto asignado 500
   (ver seccion 4).
2. Pulsar Editar plan y poner un presupuesto total de 100, menor que lo asignado.
3. Guardar.

Resultado: la aplicacion rechaza el cambio con un mensaje que explica que el
presupuesto no puede quedar por debajo de lo ya asignado a las tareas. La revision
del plan no aumenta.

### P2.5 Cambiar estado del proyecto

1. En la tarjeta, abrir el selector Cambiar estado.
2. Elegir En ejecucion y pulsar Aplicar.

Resultado: el estado de la tarjeta cambia a En ejecucion. Las transiciones que
ofrece el selector son solo las permitidas desde el estado actual.

### P2.6 Delegar el proyecto a otro Encargado

1. Pulsar Delegar.
2. Elegir encargado2@gestor.local y escribir un motivo.
3. Guardar.

Resultado: la tarjeta muestra como Encargado al usuario nuevo. Si se entra con
encargado1@gestor.local, el proyecto ya no aparece en su lista.

### P2.7 El Encargado solo ve sus proyectos

1. Salir y entrar con encargado1@gestor.local
2. Ir al menu Proyectos.

Resultado: solo aparecen los proyectos delegados a ese Encargado. El boton Nuevo
proyecto no se muestra porque crearlos es exclusivo del Jefe.

---

## 3. Estructura del proyecto: etapas y actividades (US2.2)

Estas pruebas se hacen en la pantalla Mis tareas con el Jefe o con el Encargado del
proyecto.

### P3.1 Sin proyecto seleccionado no hay datos

1. Entrar a Mis tareas sin elegir nada en el selector Proyecto.

Resultado: el selector muestra "Selecciona un proyecto" y la pantalla no carga
ninguna tarea. No aparece ningun dato de otros proyectos.

### P3.2 Elegir proyecto y etapa

1. Elegir el proyecto creado en P2.1 en el selector Proyecto.

Resultado: aparece la barra de progreso del proyecto y el selector Etapa. Si el
proyecto no tiene etapas, la lista de tareas aparece vacia.

### P3.3 Abrir el panel de estructura

1. Con el proyecto elegido, pulsar el boton Estructura de la cabecera.

Resultado: se abre el panel "Estructura del proyecto (WBS)". Si el proyecto no
tiene etapas, muestra un mensaje que invita a crear la primera.

### P3.4 Crear etapa

1. Pulsar Nueva etapa.
2. Escribir nombre "Construccion" y descripcion "Obra gruesa".
3. Pulsar Crear etapa.

Resultado: la etapa aparece en la lista con estado Pendiente y un mensaje de
confirmacion. El numero de version se gestiona solo; no hay que hacer nada con el.

### P3.5 Crear actividad dentro de la etapa

1. Pulsar el nombre de la etapa para desplegarla.
2. Escribir nombre "Cimentacion" y descripcion "Excavacion y hormigon".
3. Pulsar Crear actividad.

Resultado: la actividad aparece dentro de la etapa con estado Pendiente. La
actividad queda disponible como categoria al crear tareas.

### P3.6 Renombrar etapa y actividad

1. Pulsar el boton de editar (lapiz) de la etapa.
2. Cambiar el nombre y pulsar Guardar.
3. Repetir con una actividad.

Resultado: ambos nombres cambian en la lista y se mantienen al recargar la pagina.

### P3.7 Cambiar estado de etapa y actividad

1. En la etapa en estado Pendiente, pulsar Iniciar.

Resultado: la etapa pasa a En progreso y ahora ofrece Pausar, Completar y Cancelar.

2. Pulsar Pausar.

Resultado: la etapa queda Pendiente otra vez.

3. Repetir con una actividad: Iniciar y luego Completar.

Resultado: la actividad queda Completada y ofrece Reabrir y Cancelar. Los botones
ofrecidos coinciden siempre con las transiciones permitidas desde el estado actual.

### P3.8 Proyecto cerrado no acepta cambios de estructura

1. Como Jefe, cancelar o finalizar el proyecto desde el menu Proyectos.
2. Volver a Mis tareas, elegir ese proyecto y abrir Estructura.
3. Intentar crear una etapa.

Resultado: la aplicacion muestra un mensaje que explica que no se puede modificar
la estructura de un proyecto cerrado. La etapa no se crea.

### P3.9 Aislamiento entre Encargados

1. Entrar con encargado1@gestor.local y elegir el proyecto delegado.
2. Crear una etapa y una actividad.

Resultado: funciona con normalidad. Si otro Encargado intenta abrir ese proyecto
(no aparece en su selector), no hay forma de llegar a sus datos; si se fuerza la
direccion, el backend responde con error de no encontrado.

---

## 4. Tareas y subtareas

### P4.1 Crear tarea dentro de una actividad

1. Elegir proyecto y etapa en la barra de trabajo. Las actividades de la etapa se cargan en
   el selector Actividad y la primera queda seleccionada automaticamente (se puede cambiar).
   El formulario muestra "Se creará en la actividad" con el nombre elegido.
2. Escribir el titulo "Excavar terreno" y elegir prioridad Alta.
3. Pulsar Crear tarea.

Resultado: la tarea aparece en la lista dentro de la actividad elegida, con estado Inactiva
y la prioridad Alta. El titulo del formulario se vacia y la tarea queda lista para activarse.

### P4.2 Falta la actividad

1. Elegir un proyecto cuya etapa no tenga actividades todavia, o dejar el selector Actividad
   en "Selecciona".

Resultado: el formulario avisa que hay que elegir una etapa y su actividad en la barra de
trabajo, y no se crea la tarea. El mensaje aparece junto al formulario, no como error del
sistema.

### P4.3 Editar tarea

1. Pulsar Editar en una tarea.
2. Cambiar titulo, descripcion y prioridad. Guardar.

Resultado: la tarjeta muestra los cambios. El control de version avisa si otro
usuario cambio la tarea mientras se editaba (ver P9.1).

### P4.4 Ciclo de vida de la tarea

Con una tarea en estado Inactiva:

1. Pulsar Activar.

Resultado: la tarea pasa a Activa y se ve el tiempo acumulado corriendo.

2. Pulsar Pausar.

Resultado: la tarea queda Pausada y el tiempo se detiene.

3. Pulsar Completar (marca de verificacion).

Resultado: la tarea queda Terminada con la fecha de finalizacion. Desaparece de la
lista principal y aparece en el panel de Completadas.

4. Pulsar Reabrir tarea.

Resultado: la tarea vuelve a Inactiva y se puede activar de nuevo.

### P4.5 Subtareas

1. En una tarea, pulsar el boton Subtareas.
2. Pulsar Crear subtarea, escribir "Pedir permisos" y guardar.
3. Repetir para crear una segunda subtarea.

Resultado: el contador de subtareas de la tarjeta muestra 2. En el modal se pueden
activar, pausar, completar, reabrir, editar y eliminar igual que las tareas.

4. Editar una subtarea y cambiar su titulo.

Resultado: el titulo cambia en el modal.

5. Eliminar una subtarea: pulsar Eliminar y confirmar en el recuadro de
   confirmacion que pregunta si se elimina.

Resultado: la subtarea desaparece del modal y del contador.

### P4.6 Eliminar tarea (cancelar)

1. Pulsar Eliminar en una tarea.
2. En la confirmacion pulsar Eliminar.

Resultado: la tarea sale de la lista principal. No se borra de la base de datos:
queda como cancelada y se puede consultar en el Historial.

### P4.7 Busqueda

1. Escribir en el buscador parte del titulo de una tarea y pulsar Buscar.

Resultado: la lista muestra solo las tareas que coinciden. La busqueda la hace el
servidor sobre el proyecto elegido. Al borrar el texto y buscar de nuevo, vuelve la
lista completa.

### P4.8 Panel de completadas

1. Con al menos una tarea terminada, pulsar el boton Completadas de la cabecera.

Resultado: se abre un panel lateral con las tareas completadas y canceladas, con su
fecha y duracion. Se cierra con el boton de cerrar o pulsando fuera.

---

## 5. Asignacion de Personal (US2.3)

Estas pruebas se hacen con el Jefe o con el Encargado del proyecto, que son los
unicos que pueden asignar.

### P5.1 Abrir el dialogo de asignacion

1. En la tarjeta de una tarea pulsar el boton Asignar.

Resultado: se abre el dialogo "Asignar Personal" con el titulo de la tarea. Muestra
la lista de Personal asignado (vacia al principio) y el selector de Trabajador con
el Personal activo de la aplicacion.

### P5.2 Asignar Personal

1. Elegir a Pedro Rojas en el selector Trabajador.
2. Escribir un motivo, por ejemplo "Experiencia en excavacion".
3. Pulsar Asignar.

Resultado: aparece el mensaje "Personal asignado" y el trabajador figura en la lista
de Personal asignado con quien lo asigno. La tarjeta de la tarea muestra un
contador con el numero de personas asignadas.

### P5.3 Asignacion duplicada

1. Con el Personal ya asignado, abrir de nuevo el dialogo e intentar asignar a la
   misma persona.

Resultado: el dialogo rechaza la asignacion con el mensaje "Ese Personal ya esta
asignado a la tarea". El trabajador ya asignado ni siquiera aparece en el selector.

### P5.4 Asignarse a si mismo

1. Con el Jefe o el Encargado en sesion, abrir el dialogo de una tarea e intentar
   asignarse a si mismo.

Resultado: el backend rechaza la operacion y el dialogo muestra el mensaje "No puedes
asignarte trabajo a ti mismo". Uno no puede asignarse trabajo a si mismo por esta via.

### P5.5 Retirar asignacion

1. En la lista de Personal asignado pulsar Retirar.

Resultado: la persona sale de la lista, aparece el mensaje de asignacion retirada y
el contador de la tarjeta baja o desaparece. El Personal pierde el acceso a esa
tarea de inmediato.

### P5.6 El Personal sin asignacion no aparece

1. Revisar la lista de trabajadores del dialogo.

Resultado: solo aparecen usuarios activos. El usuario inactivo no se ofrece.

---

## 6. Vista y ejecucion del Personal (US3.1 y US3.2)

Estas pruebas se hacen con personal@gestor.local

### P6.1 El Personal no ve proyectos

1. Entrar con personal@gestor.local
2. Revisar la pantalla Mis tareas y el menu Proyectos.

Resultado: no aparece el selector de proyectos. En Proyectos se ve un mensaje que
indica que no dirige ningun proyecto. La pantalla muestra el titulo de tareas
asignadas.

### P6.2 Solo ve lo que le asignaron

1. Con una tarea asignada en P5.2, revisar la lista de Mis tareas del Personal.

Resultado: aparece la tarea asignada con su proyecto y actividad. No aparece
ninguna otra tarea del proyecto.

### P6.3 Sin datos financieros

1. Revisar la tarjeta de la tarea asignada como Personal.

Resultado: no se muestra presupuesto asignado ni duracion estimada. Esos campos
solo los ve el Jefe o el Encargado.

### P6.4 Ejecutar la tarea asignada

1. Pulsar Activar en la tarea asignada.

Resultado: la tarea pasa a Activa y el cronometro corre.

2. Pulsar Completar.

Resultado: la tarea queda Terminada. El Personal puede completar lo que le
asignaron.

### P6.5 El Personal no puede tocar lo no asignado

1. Como Jefe o Encargado, retirar la asignacion de la tarea terminada en P6.4.
2. Como Personal, intentar reabrirla (no deberia aparecer en su lista) o refrescar.

Resultado: el Personal ya no ve la tarea. Si la aplicacion intenta una accion sobre
ella, el backend responde con error de no permitido y la tarea sigue como estaba.

### P6.6 El Jefe puede completar tareas de otros

1. Como Jefe, abrir el proyecto de un Encargado y completar una tarea asignada al
   Personal.

Resultado: la accion se permite (es el override del Jefe) y el motivo queda
registrado en el historial de la tarea.

---

## 7. Progreso en cascada (US3.3)

### P7.1 Arbol de progreso

1. Como Jefe o Encargado, elegir el proyecto con etapas, actividades y tareas.

Resultado: bajo la barra de progreso se ve el arbol: el proyecto, sus etapas, las
actividades de cada etapa y las tareas con subtareas. Cada nivel muestra su
porcentaje.

### P7.2 El progreso refleja los cambios

1. Completar una tarea sin subtareas.

Resultado: el porcentaje de la tarea, su actividad, su etapa y el proyecto suben.
El proyecto llega al 100 por ciento cuando todas las unidades de trabajo estan
completadas.

### P7.3 Las tareas con subtareas no se cuentan dos veces

1. En una tarea con dos subtareas, completar solo una.

Resultado: la tarea aparece como grupo (sin porcentaje propio) y su avance es el de
sus subtareas: 50 por ciento. El total de unidades del proyecto no aumenta por
tener padre e hijas.

### P7.4 Proyecto vacio

1. Crear un proyecto nuevo sin etapas ni tareas y ver su progreso.

Resultado: el progreso es 0 por ciento y no ocasiona ningun error.

### P7.5 El 100 por ciento no cierra el proyecto

1. Completar todo el trabajo de un proyecto.

Resultado: el progreso llega al 100 por ciento pero el proyecto sigue en su estado
(En ejecucion). Cerrarlo es una decision manual del Jefe desde el menu Proyectos.

---

## 8. Historial

### P8.1 Ver tareas archivadas

1. Terminar o cancelar al menos una tarea (y alguna subtarea).
2. Ir al menu Historial.
3. Elegir el proyecto en el selector Proyecto de la parte superior.

Resultado: aparecen las tareas y subtareas completadas y canceladas del proyecto elegido, con su
fecha y, para las tareas, sus cambios de estado. Al entrar sin elegir proyecto, la pantalla pide
elegir uno y no muestra ningun error.

### P8.2 Buscar en el historial

1. Escribir parte de un titulo en el buscador del historial y buscar.

Resultado: la lista se filtra por titulo o descripcion.

---

## 9. Concurrencia y errores

### P9.1 Conflicto de version

1. Abrir la misma tarea en dos pestanas del navegador.
2. En la primera pestana editar la tarea y guardar.
3. En la segunda pestana editar la misma tarea e intentar guardar.

Resultado: el segundo guardado se rechaza con un mensaje que explica que la tarea
cambio mientras se editaba. El texto escrito se conserva y basta recargar la lista
para volver a intentarlo. No se pierden datos.

### P9.2 Backend caido

1. Con el frontend abierto, detener el backend.
2. Intentar cargar tareas o crear una etapa.

Resultado: la aplicacion muestra un mensaje de error claro en lugar de quedarse
colgada. Al levantar el backend y pulsar Actualizar, todo vuelve a funcionar.

### P9.3 Sesion caducada

1. Dejar la sesion abierta hasta que expire el token (o borrar la sesion del
   navegador).
2. Intentar cualquier accion.

Resultado: la aplicacion redirige al login en lugar de mostrar errores de
programacion.

---

## 10. Resumen para firmar

Copiar esta tabla al informe de la ejecucion manual y rellenar la columna de la
derecha con OK o Falla.

| Prueba | Que comprueba | Resultado |
|---|---|---|
| P1.1 a P1.5 | Acceso, credenciales, roles y cierre de sesion | |
| P2.1 a P2.7 | Proyectos: crear, editar, plan, estados, delegar, aislamiento | |
| P3.1 a P3.9 | Etapas y actividades: crear, editar, estados, proyecto cerrado | |
| P4.1 a P4.8 | Tareas y subtareas: crear, editar, ciclo de vida, buscar, eliminar | |
| P5.1 a P5.6 | Asignacion de Personal: asignar, duplicado, retirar | |
| P6.1 a P6.6 | Personal: alcance, sin datos financieros, ejecucion, override del Jefe | |
| P7.1 a P7.5 | Progreso en cascada del proyecto | |
| P8.1 y P8.2 | Historial de terminadas y canceladas | |
| P9.1 a P9.3 | Conflictos de version, backend caido y sesion caducada | |

Notas de la ejecucion (fecha, ejecutante, incidencias):
