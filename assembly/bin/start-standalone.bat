@echo off
setlocal

set "sbinDir=%~dp0"
set "baseDir=%~dp0.."
set "runtimeDir=%baseDir%\..\runtime"
set "buildDir=%baseDir%\build"

cd /d %baseDir%

::1. clear file
rd /s /q "%runtimeDir%"
mkdir "%runtimeDir%"


::2. package lib
:: Assuming you have tar utility installed in Windows
tar -zxvf "%buildDir%\supersonic.tar.gz" -C "%runtimeDir%"

for /d %%f in ("%runtimeDir%\launchers-standalone-*") do (
    move "%%f" "%runtimeDir%\supersonic-standalone"
)

tar -zxvf "%buildDir%\supersonic-webapp.tar.gz" -C "%buildDir%"

if not exist "%runtimeDir%\supersonic-standalone\webapp" mkdir "%runtimeDir%\supersonic-standalone\webapp"

xcopy /s /e /h /y "%buildDir%\supersonic-webapp\*" "%runtimeDir%\supersonic-standalone\webapp"

if not exist "%runtimeDir%\supersonic-standalone\conf\webapp" mkdir "%runtimeDir%\supersonic-standalone\conf\webapp"
xcopy /s /e /h /y "%runtimeDir%\supersonic-standalone\webapp\*" "%runtimeDir%\supersonic-standalone\conf\webapp"

rd /s /q "%buildDir%\supersonic-webapp"


::3. start service
::start standalone service
call "%runtimeDir%\supersonic-standalone\bin\service.bat" restart
call "%runtimeDir%\supersonic-standalone\llm\bin\service.bat" restart
