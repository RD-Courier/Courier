@ECHO OFF

call %~dp0env.cmd

%JAVA% -classpath %cp% ru.rd.courier.manager.GeneralTest