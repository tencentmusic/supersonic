
setlocal

set "sbinDir=%~dp0"
set "baseDir=%~dp0.."
set "buildDir=%baseDir%\build"

cd /d "%baseDir%\bin"
call build-standalone.bat

cd "%buildDir%"
tar -zxvf supersonic-webapp.tar.gz
move supersonic-webapp webapp
move webapp ..\..\launchers\standalone\target\classes

endlocal