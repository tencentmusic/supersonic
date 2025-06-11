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

REM fix path configuration - point to the correct release package directory
set "releaseDir=%buildDir%\supersonic-%service%-1.0.0-SNAPSHOT"
cd %releaseDir%

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
   echo 'Using release directory: %releaseDir%'
   
   REM use release package directory as base path
   set "libDir=%releaseDir%\lib"
   set "confDir=%releaseDir%\conf"
   set "webDir=%releaseDir%\webapp"
   set "logDir=%releaseDir%\logs"
   
   REM fix variable name matching problem
   set "CLASSPATH=%releaseDir%;%webDir%;%libDir%\*;%confDir%"
   set "MAIN_CLASS=%main_class%"
   
   REM add port configuration
   set "property=-Dfile.encoding=UTF-8 -Duser.language=Zh -Duser.region=CN -Duser.timezone=GMT+08 -Dspring.profiles.active=%profile% -Dserver.port=9080"
   set "java_command=%property% -Xms1024m -Xmx2048m -cp "%CLASSPATH%" %MAIN_CLASS%"
   
   if not exist %logDir% mkdir %logDir%
   
   REM check if the main jar file exists
   if not exist "%libDir%\launchers-standalone-1.0.0-SNAPSHOT.jar" (
       echo "Error: Main jar file not found in %libDir%"
       echo "Please make sure the application has been built and packaged correctly."
       goto :EOF
   )
   
   echo 'Main Class: %MAIN_CLASS%'
   echo 'Profile: %profile%'
   echo 'Starting Java service...'
   
   REM start service and save logs
   start /B java %java_command% > "%logDir%\supersonic.log" 2>&1
   timeout /t 15 >nul
   
   REM check service status
   netstat -an | findstr ":9080" >nul
   if errorlevel 1 (
       echo "Warning: Port 9080 is not listening"
       echo "Please check the log file: %logDir%\supersonic.log"
       if exist "%logDir%\supersonic.log" (
           echo "Recent log entries:"
           powershell -Command "Get-Content '%logDir%\supersonic.log' | Select-Object -Last 10"
       )
   ) else (
       echo "Service started successfully on port 9080"
       echo "You can access the application at: http://localhost:9080"
   )
   
   echo 'java service started'
   goto :EOF

:stopJavaService
   echo 'Stopping Java service...'
   for /f "tokens=2" %%i in ('tasklist ^| findstr /i "java"') do (
            taskkill /PID %%i /F
            echo "java service (PID = %%i) is killed."
   )
   goto :EOF

endlocal