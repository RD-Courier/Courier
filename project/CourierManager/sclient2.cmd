@ECHO OFF

call env.cmd

%JAVA% -classpath %cp% ru.rd.net.SocketCodecClientTest
