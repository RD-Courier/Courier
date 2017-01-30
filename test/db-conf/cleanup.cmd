@ECHO OFF

IF NOT EXIST "%~dp0_Logs" GOTO AfterLogs:
RD /S /Q "%~dp0_Logs"
:AfterLogs

IF NOT EXIST "%~dp0_system-db.xml" GOTO AfterLogs:
DEL /F /Q "%~dp0_system-db.xml"
:AfterLogs
