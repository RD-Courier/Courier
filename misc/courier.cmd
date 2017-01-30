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
SET LIBS=%CLIBS%\activation.jar
SET LIBS=%LIBS%;%CLIBS%\commons-lang-2.1.jar
SET LIBS=%LIBS%;%CLIBS%\commons-codec-1.1.jar
SET LIBS=%LIBS%;%CLIBS%\commons-net-1.1.0.jar
SET LIBS=%LIBS%;%CLIBS%\jakarta-oro-2.0.8.jar
SET LIBS=%LIBS%;%CLIBS%\jasypt-1.5.jar
SET LIBS=%LIBS%;%CLIBS%\javadbf-0.4.0.jar
SET LIBS=%LIBS%;%CLIBS%\jconn2.jar
SET LIBS=%LIBS%;%CLIBS%\jms.jar
SET LIBS=%LIBS%;%CLIBS%\junit.jar
SET LIBS=%LIBS%;%CLIBS%\jxl.jar
SET LIBS=%LIBS%;%CLIBS%\msbase.jar
SET LIBS=%LIBS%;%CLIBS%\sqljdbc42.jar
SET LIBS=%LIBS%;%CLIBS%\mysql-connector-java-5.1.36-bin.jar
SET LIBS=%LIBS%;%CLIBS%\msutil.jar
SET LIBS=%LIBS%;%CLIBS%\xalan.jar
SET LIBS=%LIBS%;%CLIBS%\xercesImpl.jar
SET LIBS=%LIBS%;%CLIBS%\xml-apis.jar
SET LIBS=%LIBS%;%CLIBS%\serializer.jar
SET LIBS=%LIBS%;%CLIBS%\xsltc.jar
SET LIBS=%LIBS%;%CLIBS%\javax.mail-1.5.6.jar
SET LIBS=%LIBS%;%CLIBS%\trilead-ssh2-build211.jar
SET LIBS=%LIBS%;%CLIBS%\mina-core-1.1.7.jar
SET LIBS=%LIBS%;%CLIBS%\slf4j-api-1.5.2.jar
SET LIBS=%LIBS%;%CLIBS%\slf4j-jdk14-1.5.2.jar
SET LIBS=%LIBS%;%CLIBS%\ojdbc6.jar
SET LIBS=%LIBS%;%CLIBS%\orai18n.jar
SET LIBS=%LIBS%;%CLIBS%\accessors-smart-1.1.jar
SET LIBS=%LIBS%;%CLIBS%\asm-5.1.jar
SET LIBS=%LIBS%;%CLIBS%\json-path-2.2.0.jar
SET LIBS=%LIBS%;%CLIBS%\json-smart-2.2.1.jar
SET LIBS=%LIBS%;%~dp0courier.zip
REM FOR %%f IN (%CLIBS%\*.jar) DO SET LIBS=!LIBS!;%%f

SET CourierAxis2Distr=%~dp0ws-source
CALL %CourierAxis2Distr%\wssource.cmd
SET LIBS=%LIBS%;%CourierAxis2Libs%

IF DEFINED CourierExtraLibs SET LIBS=%LIBS%;%CourierExtraLibs%

IF NOT DEFINED JAVA_HOME SET JAVA_HOME=%JAVAHOME%

REM @echo on
"%JAVA_HOME%\bin\java.exe" -classpath "%LIBS%" %JavaParams% ru.rd.courier.Application %1 %~dp0
REM @echo off
