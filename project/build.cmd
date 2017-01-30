@ECHO OFF

if "[%ANT_HOME%]" == "[]" SET ANT_HOME=C:\AppDistr\courier\JavaLibs\apache-ant-1.9.7

if "[%BCEL_HOME%]" == "[]" SET BCEL_HOME=C:\AppDistr\courier\JavaLibs\bcel-6.0

if "[%JAVA_HOME%]" == "[]" SET JAVA_HOME=C:\Program Files\Java\jdk1.8.0_102

SET CLASSPATH=%ANT_HOME%\lib\ant-apache-bcel.jar

CALL %ANT_HOME%\bin\ant -lib %BCEL_HOME% -buildfile courier.xml %1