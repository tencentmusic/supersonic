@echo off
setlocal

set "sbinDir=%~dp0"
set "baseDir=%~dp0.."
set "buildDir=%baseDir%\build"


rem 1. build semantic chat service
del /q "%buildDir%\*.tar.gz" 2>NUL

call mvn -f "%baseDir%\..\pom.xml" clean package -DskipTests

rem 2. move package to build
echo f|xcopy "%baseDir%\..\launchers\standalone\target\*.tar.gz" "%buildDir%\supersonic.tar.gz"

rem 3. build webapp
cd "%baseDir%\..\webapp"
call start-fe-prod.bat
copy /y "%baseDir%\..\webapp\supersonic-webapp.tar.gz" "%buildDir%\"


cd "%buildDir%"
tar -zxvf supersonic-webapp.tar.gz
move supersonic-webapp webapp
move webapp ..\..\launchers\standalone\target\classes

endlocal