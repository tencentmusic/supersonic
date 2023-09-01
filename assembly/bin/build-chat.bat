@echo off

setlocal

set "sbinDir=%~dp0"
set "baseDir=%~dp0.."
set "runtimeDir=%baseDir%\runtime"
set "buildDir=%baseDir%\build"

cd "%baseDir%"

rem 1. move package to build
copy "%baseDir%\..\launchers\chat\target\*.tar.gz" "%buildDir%\supersonic-chat.tar.gz"

endlocal