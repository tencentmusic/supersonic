@echo off
setlocal

set "sbinDir=%~dp0"
set "baseDir=%~dp0.."
set "runtimeDir=%baseDir%\..\runtime"
set "buildDir=%baseDir%\build"

::3. start service
::start standalone service
call "%runtimeDir%\supersonic-standalone\bin\service.bat" stop
call "%runtimeDir%\supersonic-standalone\llm\bin\service.bat" stop
