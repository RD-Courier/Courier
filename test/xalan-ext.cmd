@echo off

set XslFile=xalan-ext.xsl
set InFile=xalan-ext-conf.xml
set OutFile=xalan-ext-conf-result.xml

xsl.cmd %InFile% %XslFile% %OutFile%
