@echo off
setlocal
chcp 65001

set "sbinDir=%~dp0"
call %sbinDir%/supersonic-common.bat %*
call %sbinDir%/supersonic-env.bat %*

set "command=%~1"
set "service=%~2"
set "profile=%~3"

if "%service%"=="" (
   set "service=%standalone_service%"
)

if "%profile%"=="" (
   set "profile=%S2_DB_TYPE%"
)

set "model_name=%service%"

cd %baseDir%

if "%command%"=="restart" (
   call :stop
   call :start
   goto :EOF
) else if "%command%"=="start" (
   call :start
   goto :EOF
) else if "%command%"=="stop" (
   call :stop
   goto :EOF
) else if "%command%"=="reload" (
   call :reloadExamples
   goto :EOF
) else (
   echo "Use command {start|stop|restart} to run."
   goto :EOF
)

:start
   call :runJavaService
   goto :EOF

:stop
   call :stopJavaService
   goto :EOF

:runJavaService
   echo 'java service starting, see logs in logs/'
   set "libDir=%baseDir%\lib"
   set "confDir=%baseDir%\conf"
   set "webDir=%baseDir%\webapp"
   set "logDir=%baseDir%\logs"
   set "classpath=%baseDir%;%webDir%;%libDir%\*;%confDir%"
   set "property=-Dfile.encoding=UTF-8 -Duser.language=Zh -Duser.region=CN -Duser.timezone=GMT+08 -Dspring.profiles.active=%profile%"
   set "java-command=%property% -Xms1024m -Xmx1024m -cp %CLASSPATH% %MAIN_CLASS%"
   if not exist %logDir% mkdir %logDir%
   start /B java %java-command% >nul 2>&1
   timeout /t 10 >nul
   echo 'java service started'
   goto :EOF

:stopJavaService
   for /f "tokens=2" %%i in ('tasklist ^| findstr /i "java"') do (
            taskkill /PID %%i /F
            echo "java service (PID = %%i) is killed."
   )
   goto :EOF

endlocal