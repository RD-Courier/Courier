@ECHO OFF

call %~dp0env.cmd

%JAVA% -classpath %cp% -Xmx1M -DCourierManagerDebugLog=_MemoryLogs -DCourierManagerDebugInterval=15s ru.rd.courier.manager.ManagerStarter config-example.xml