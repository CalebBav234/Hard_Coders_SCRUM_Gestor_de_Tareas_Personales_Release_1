@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21\jdk-21.0.12.1+1
set PATH=%JAVA_HOME%\bin;%PATH%
set SPRING_DATASOURCE_PASSWORD=apppass
cd /d C:\Users\caleb\Downloads\Hard_Coders_SCRUM_Gestor_de_Tareas_Personales_Release_1-1\backend
mvn spring-boot:run > ..\backend-etapa2.log 2>&1
