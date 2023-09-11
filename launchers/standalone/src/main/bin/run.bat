@echo off
setlocal

set "binDir=%~dp0"
set "baseDir=%~dp0.."
set "libDir=%baseDir%\lib"
set "confDir=%baseDir%\conf"
set "webDir=%baseDir%\webapp"

call "%baseDir%\bin\env.bat"

set "CLASSPATH=%confDir%;%webDir%;%libDir%\*"

set "LANG=zh_CN.UTF-8"

cd /d "%baseDir%"

set "command=-Dfile.encoding=UTF-8 -Duser.language=Zh -Duser.region=CN -Duser.timezone=GMT+08 -Xms1024m -Xmx2048m -cp %CLASSPATH% %MAIN_CLASS%"

if not exist "%baseDir%\logs" (
    mkdir "%baseDir%\logs"
)

if "%is_test%"=="true" (
    start "supersonic" /B java -Dspring.profiles.active=dev %command% >nul 2>"%baseDir%\logs\error.log"
) else (
    start "supersonic" /B java %command% >nul 2>"%baseDir%\logs\error.log"
)

endlocal