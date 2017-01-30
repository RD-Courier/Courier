@ECHO OFF

SET JAVA_HOME=C:\Program Files\Java\jdk1.5.0_07\jre

SET OpenJmsLibs=C:\Projects\Murex\OpenJMS\lib
SET CourierExtraLibs=%OpenJmsLibs%\commons-codec-1.3.jar
SET CourierExtraLibs=%CourierExtraLibs%;%OpenJmsLibs%\commons-logging-1.0.4.jar
SET CourierExtraLibs=%CourierExtraLibs%;%OpenJmsLibs%\concurrent-1.3.4.jar
SET CourierExtraLibs=%CourierExtraLibs%;%OpenJmsLibs%\jndi-1.2.1.jar
SET CourierExtraLibs=%CourierExtraLibs%;%OpenJmsLibs%\openjms-0.7.7-beta-1.jar
SET CourierExtraLibs=%CourierExtraLibs%;%OpenJmsLibs%\openjms-common-0.7.7-beta-1.jar
SET CourierExtraLibs=%CourierExtraLibs%;%OpenJmsLibs%\openjms-net-0.7.7-beta-1.jar
SET CourierExtraLibs=%CourierExtraLibs%;%OpenJmsLibs%\spice-jndikit-1.2.jar
CALL %~dp0test-courier.cmd TestJms.xml
