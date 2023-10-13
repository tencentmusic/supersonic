@echo off
setlocal
chcp 65001
set "sbinDir=%~dp0"
set "baseDir=%~dp0.."
set "runtimeDir=%baseDir%\..\runtime"
set "buildDir=%baseDir%\build"
set "main_class=com.tencent.supersonic.StandaloneLauncher"
set "python_path=python"
set "pip_path=pip3"
set "standalone_service=standalone"
set "llmparser_service=llmparser"

set "javaRunDir=%runtimeDir%\supersonic-standalone"
set "pythonRunDir=%runtimeDir%\supersonic-standalone\llmparser"

set "command=%~1"
set "service=%~2"

if "%service%"=="" (
   set "service=%standalone_service%"
)

call :BUILD_RUNTIME

if "%command%"=="restart" (
   call :STOP
   call :START
   goto :EOF
) else if "%command%"=="start" (
   call :START
   goto :EOF
) else if "%command%"=="stop" (
  call :STOP
  goto :EOF
) else if "%command%"=="reload" (
  call :RELOAD_EXAMPLE
  goto :EOF
) else (
   echo "Use command {start|stop|restart} to run."
   goto :EOF
)

:START
    if "%service%"=="%llmparser_service%" (
         call :START_PYTHON
         goto :EOF
    )
    call :START_PYTHON
    call :START_JAVA
    goto :EOF

:STOP
    if "%service%"=="%llmparser_service%" (
        call :STOP_PYTHON
        goto :EOF
    )
    call :STOP_PYTHON
    call :STOP_JAVA
    goto :EOF

:START_PYTHON
   echo 'python service starting, see logs in llmparser/llmparser.log'
   cd "%pythonRunDir%"
   start /B %python_path% supersonic_llmparser.py  > %pythonRunDir%\llmparser.log 2>&1
   timeout /t 10 >nul
   echo 'python service started'
   goto :EOF

:START_JAVA
  echo 'java service starting, see logs in logs/'
   cd "%javaRunDir%"
   if not exist "%runtimeDir%\supersonic-standalone\logs" mkdir "%runtimeDir%\supersonic-standalone\logs"
   set "libDir=%runtimeDir%\supersonic-%service%\lib"
   set "confDir=%runtimeDir%\supersonic-%service%\conf"
   set "webDir=%runtimeDir%\supersonic-%service%\webapp"
   set "classpath=%confDir%;%webDir%;%libDir%\*"
   set "java-command=-Dfile.encoding=UTF-8 -Duser.language=Zh -Duser.region=CN -Duser.timezone=GMT+08 -Xms1024m -Xmx2048m -cp %CLASSPATH% %MAIN_CLASS%"
   start /B java %java-command% >nul 2>&1
   timeout /t 10 >nul
   echo 'java service started'
   goto :EOF

:STOP_PYTHON
   for /f "tokens=2" %%i in ('tasklist ^| findstr /i "python"') do (
           taskkill /PID %%i /F
           echo "python service (PID = %%i) is killed."
   )
   goto :EOF

:STOP_JAVA
   for /f "tokens=2" %%i in ('tasklist ^| findstr /i "java"') do (
            taskkill /PID %%i /F
            echo "java service (PID = %%i) is killed."
   )
   goto :EOF

:RELOAD_EXAMPLE
   cd "%runtimeDir%\supersonic-standalone\llmparser\sql"
   start  %python_path% examples_reload_run.py
   goto :EOF

:BUILD_RUNTIME
  rem 6. reset runtime
  if exist "%runtimeDir%" goto :EOF
  mkdir "%runtimeDir%"
  tar -zxvf "%buildDir%\supersonic-standalone.tar.gz" -C "%runtimeDir%"
  for /d %%f in ("%runtimeDir%\launchers-standalone-*") do (
      move "%%f" "%runtimeDir%\supersonic-standalone"
  )

  rem 7. copy webapp to runtime
  tar -zxvf "%buildDir%\supersonic-webapp.tar.gz" -C "%buildDir%"
  if not exist "%runtimeDir%\supersonic-standalone\webapp" mkdir "%runtimeDir%\supersonic-standalone\webapp"
  xcopy /s /e /h /y "%buildDir%\supersonic-webapp\*" "%runtimeDir%\supersonic-standalone\webapp"
  if not exist "%runtimeDir%\supersonic-standalone\conf\webapp" mkdir "%runtimeDir%\supersonic-standalone\conf\webapp"
  xcopy /s /e /h /y "%runtimeDir%\supersonic-standalone\webapp\*" "%runtimeDir%\supersonic-standalone\conf\webapp"
  rd /s /q "%buildDir%\supersonic-webapp"