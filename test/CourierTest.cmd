SET CourierCode=%~dp0..\project\classes
SET CourierLibs=%~dp0..\libs\main
@%windir%\system32\cscript /NOLOGO %~dp0courier.js --system "%~dp0..\project\test-src\ru\rd\courier\distr" --conf %1