@ECHO OFF

IF DEFINED CourierLibs (
  SET CLIBS=%CourierLibs%
  GOTO CheckLibs:
) 

SET CLIBS=%~dp0libs
IF EXIST "%CLIBS%" GOTO FormLibs:
SET CLIBS=%~dp0..\CourierLibs
:CheckLibs
IF EXIST "%CLIBS%" GOTO FormLibs:

ECHO CourierLibs not found
EXIT 1

:FormLibs
SET LIBS=%CLIBS%\commons-lang-2.1.jar
SET LIBS=%LIBS%;%CLIBS%\commons-codec-1.1.jar
SET LIBS=%LIBS%;%CLIBS%\jasypt-1.5.jar
SET LIBS=%LIBS%;%~dp0courier.zip

IF NOT DEFINED JAVA_HOME SET JAVA_HOME=%JAVAHOME%

REM @echo on
"%JAVA_HOME%\bin\java.exe" -classpath "%LIBS%" %JavaParams% ru.rd.courier.EncryptTool %1 %2
REM @echo off
