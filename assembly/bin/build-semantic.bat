@echo off

setlocal

set "sbinDir=%~dp0"
set "baseDir=%~dp0.."
set "runtimeDir=%baseDir%\runtime"
set "buildDir=%baseDir%\build"

echo '%baseDir%'
echo '%buildDir%'


cd "%baseDir%"

rem 1. build semantic service
del /q "%buildDir%\*.tar.gz"
rd /s /q dist

call mvn -f "%baseDir%\..\pom.xml" clean package -DskipTests

rem 2. move package to build
echo "%baseDir%\..\launchers\semantic\target\*.tar.gz"
echo "%buildDir%\supersonic-semantic.tar.gz"
copy "%baseDir%\..\launchers\semantic\target\*.tar.gz" "%buildDir%\supersonic-semantic.tar.gz"

rem 3. build webapp
cd "%baseDir%\..\webapp"
start-fe-prod.bat
copy /y "supersonic-webapp.tar.gz" "%buildDir%\"

endlocal
