@echo off

set ExtraParams=-param app-dir C:\Projects\IT\3D_Projects\Courier\test
set ExtraParams=%ExtraParams% -param sys-conf-file file:/C:\Projects\IT\3D_Projects\Courier\test\distr\sys-config.xml

set InFile=conf-mock.xml
set XslFile=distr\config.xsl
set OutFile=transform-test.xml
@rem set XslFile=..\project\test-src\ru\rd\courier\distr\config.xsl
@rem set XslFile=..\project\test-src\ru\rd\courier\distr\resolve-named-tags.xsl

xsl.cmd %InFile% %XslFile% %OutFile%
