@echo off
setlocal
chcp 65001

call supersonic-common.bat %*
call %sbinDir%/../conf/supersonic-env.bat %*

set "command=%~1"
set "service=%~2"
if "%service%"=="" (
   set "service=%standalone_service%"
)
set "model_name=%service%"
IF "%service%"=="pyllm" (
  set "llmProxy=PythonLLMProxy"
  set "model_name=%standalone_service%"
)

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


: start
   if "%service%"=="%pyllm_service%" (
         call :runPythonService
         call :runJavaService
         goto :EOF
   )
   call :runJavaService
   goto :EOF


: stop
   call :stopPythonService
   call :stopJavaService
   goto :EOF


: reloadExamples
   set "pythonRunDir=%baseDir%\pyllm"
   cd "%pythonRunDir%\sql"
   start  %python_path% examples_reload_run.py
   goto :EOF


: runJavaService
   echo 'java service starting, see logs in logs/'
   set "libDir=%baseDir%\lib"
   set "confDir=%baseDir%\conf"
   set "webDir=%baseDir%\webapp"
   set "logDir=%baseDir%\logs"
   set "classpath=%baseDir%;%webDir%;%libDir%\*;%confDir%"
   set "java-command=-Dfile.encoding=UTF-8 -Duser.language=Zh -Duser.region=CN -Duser.timezone=GMT+08 -Xms1024m -Xmx2048m -cp %CLASSPATH% %MAIN_CLASS%"
   if not exist %logDir% mkdir %logDir%
   start /B java %java-command% >nul 2>&1
   timeout /t 10 >nul
   echo 'java service started'
   goto :EOF


: runPythonService
   echo 'python service starting, see logs in pyllm\pyllm.log'
   set "pythonRunDir=%baseDir%\pyllm"
   start /B %python_path% %pythonRunDir%\supersonic_pyllm.py  > %pythonRunDir%\pyllm.log 2>&1
   timeout /t 10 >nul
   echo 'python service started'
   goto :EOF


: stopPythonService
   for /f "tokens=2" %%i in ('tasklist ^| findstr /i "python"') do (
           taskkill /PID %%i /F
           echo "python service (PID = %%i) is killed."
   )
   goto :EOF


: stopJavaService
   for /f "tokens=2" %%i in ('tasklist ^| findstr /i "java"') do (
            taskkill /PID %%i /F
            echo "java service (PID = %%i) is killed."
   )
   goto :EOF

endlocal