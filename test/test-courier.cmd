@ECHO OFF
SET JAVA_HOME=C:\Program Files\Java\jdk1.6.0_14
REM SET COURIER_DEFAULT_CONFIG=%~dp0conf-def.xml
SET CourierLibs=%~dp0..\libs\main
SET CourierAxis2Distr=%~dp0..\project\WebServiceSource\distr
CALL %CourierAxis2Distr%\wssource.cmd
CALL %~dp0distr\courier.cmd %~dp0conf-mock.xml
