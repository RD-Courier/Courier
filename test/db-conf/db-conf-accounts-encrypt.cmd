@ECHO OFF
SET JAVA_HOME=C:\Program Files\Java\jdk1.6.0_10
SET CourierLibs=%~dp0..\..\libs\main
CALL %~dp0..\distr\Encrypt.cmd %~dp0db-conf-accounts.xml %~dp0db-conf-accounts.xxx
CALL %~dp0..\distr\Encrypt.cmd %~dp0common\db-conf-accounts-common.xml %~dp0common\db-conf-accounts-common.xxx
