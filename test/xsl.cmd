@echo off
set InFile=%1
set XslFile=%2
set OutFile=%3

set LIBS=%~dp0..\libs\main
rem set CourierLib=%~dp0distr\courier.zip
set CourierLib=%~dp0..\project\classes
set cp=%LIBS%\xml-apis.jar;%LIBS%\xercesImpl.jar;%LIBS%\xalan.jar;%CourierLib%

set JavaParams=-classpath "%cp%" -Duser.language=rus org.apache.xalan.xslt.Process
set StdParams=-in %InFile% -xsl %XslFile% -out %OutFile%

"%JAVA_HOME%\bin\java.exe" %JavaParams% %StdParams% %ExtraParams%