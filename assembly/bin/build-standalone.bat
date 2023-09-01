@echo off

setlocal

set "sbinDir=%~dp0"
set "baseDir=%~dp0.."
set "runtimeDir=%baseDir%\runtime"
set "buildDir=%baseDir%\build"

cd "%baseDir%"

rem 1. build semantic chat service
del /q "%buildDir%\*.tar.gz"
rd /s /q dist

call mvn -f "%baseDir%\..\pom.xml" clean package -DskipTests

rem 2. move package to build
copy "%baseDir%\..\launchers\standalone\target\*.tar.gz" "%buildDir%\supersonic.tar.gz"

rem 3. build webapp
cd "%baseDir%\..\webapp"
call start-fe-prod.bat
copy /y "%baseDir%\..\webapp\supersonic-webapp.tar.gz" "%buildDir%\"

endlocal
