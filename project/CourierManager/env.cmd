SET PRJ=%~dp0project
SET LIBS=%~dp0..\..\libs\main
SET Courier=%~dp0..
SET cp=%LIBS%\junit.jar;%LIBS%\slf4j-api-1.5.2.jar;%LIBS%\slf4j-jdk14-1.5.2.jar;%LIBS%\mina-core-1.1.7.jar;%Courier%\classes;%Courier%\test-classes;%PRJ%\classes;%PRJ%\test-classes
SET JAVA="%JAVA_HOME%\bin\java.exe"
SET JAVA_CMAN="%JAVA_HOME%\bin\java-cman.exe"
