set ApplPath=C:/Projects/IT/3D_Projects/Courier/code/build
set XalanPath=C:/JavaLibs/xalan-j_2_5_2/bin/xml-apis.jar;C:/JavaLibs/xalan-j_2_5_2/bin/xercesImpl.jar;C:/JavaLibs/xalan-j_2_5_2/bin/xalan.jar
set SybasePath=C:/sybase/jConnect-5_5/classes/jconn2.jar
set MsPath="C:/Program Files/Microsoft SQL Server 2000 Driver for JDBC/lib/msbase.jar";"C:/Program Files/Microsoft SQL Server 2000 Driver for JDBC/lib/mssqlserver.jar";"C:/Program Files/Microsoft SQL Server 2000 Driver for JDBC/lib/msutil.jar"
set MailPath=C:/JavaLibs/javax.mail-1.5.6.jar;C:/JavaLibs/jaf-1.0.2/activation.jar;C:/JavaLibs/commons-lang-2.0/commons-lang-2.0.jar

set Java=C:\j2sdk1.4.2\bin\java.exe

%Java% -classpath %ApplPath%;%XalanPath%;%SybasePath%;%MsPath%;%MailPath% ru.rd.courier.Application config-dexDeal.xml 