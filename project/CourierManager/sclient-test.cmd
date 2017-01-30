@ECHO OFF

call env.cmd

%JAVA% -classpath %cp% ru.rd.courier.stat.StatProcessingTest
