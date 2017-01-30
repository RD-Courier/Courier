@ECHO OFF
SET JAVA_HOME=C:\Program Files\Java\jdk1.6.0_10
REM SET COURIER_DEFAULT_CONFIG=%~dp0conf-def.xml
SET CourierLibs=%~dp0..\libs\main
CALL %~dp0distr\courier.cmd %1
