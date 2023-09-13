@echo off
setlocal

set "sbinDir=%~dp0"
set "baseDir=%~dp0.."
set "runtimeDir=%baseDir%\..\runtime"
set "buildDir=%baseDir%\build"

set "command=%~1"
set "module=%~2"

set "APP_NAME=standalone-service"
set "MAIN_CLASS=com.tencent.supersonic.StandaloneLauncher"

set "python_path=python"
set "pip_path=pip3.9"
set "llm_host=127.0.0.1"
set "llm_port=9092"
set "start_name=api_service"


if "%module%"=="" (
   set "module=standalone"
) else if "%module%"=="semantic" (
   set "APP_NAME=semantic-service"
   set "MAIN_CLASS=com.tencent.supersonic.SemanticLauncher"
) else if "%module%"=="chat" (
   set "APP_NAME=chat-service"
   set "MAIN_CLASS=com.tencent.supersonic.ChatLauncher"
)

if "%command%"=="" (
   set "command=restart"
)

set "libDir=%runtimeDir%\supersonic-%module%\lib"
set "confDir=%runtimeDir%\supersonic-%module%\conf"
set "webDir=%runtimeDir%\supersonic-%module%\webapp"
set "CLASSPATH=%confDir%;%webDir%;%libDir%\*"
set "java-command=-Dfile.encoding=UTF-8 -Duser.language=Zh -Duser.region=CN -Duser.timezone=GMT+08 -Xms1024m -Xmx2048m -cp %CLASSPATH% %MAIN_CLASS%"


if "%command%"=="stop" (
    call:STOP
    goto :EOF
)

if "%command%"=="restart" (
     call:STOP
)

::1. clear file
rd /s /q "%runtimeDir%"
mkdir "%runtimeDir%"
set "llm_path=%runtimeDir%\supersonic-standalone\llm"

if "%module%"=="llmparser" (
   tar -zxvf "%buildDir%\supersonic.tar.gz" -C "%runtimeDir%"
   for /d %%f in ("%runtimeDir%\launchers-standalone-*") do (
       move "%%f" "%runtimeDir%\supersonic-standalone"
   )
   cd "%runtimeDir%"
   "%pip_path%" install -r "%llm_path%\requirements.txt"
   "%python_path%" -c "import langchain,fastapi,chromadb,tiktoken,uvicorn" >nul 2>&1
   cd "%runtimeDir%\supersonic-standalone\llm\llm"
   start "" /B uvicorn %start_name%:app --port %llm_port% --host %llm_host%  > "%runtimeDir%\supersonic-standalone\llm\llm.log" 2>&1
   echo "llm service started, see logs/error with logs/error command"
   goto :EOF
)

tar -zxvf "%buildDir%\supersonic.tar.gz" -C "%runtimeDir%"
for /d %%f in ("%runtimeDir%\launchers-%module%-*") do (
    move "%%f" "%runtimeDir%\supersonic-%module%"
)

if not exist "%runtimeDir%\supersonic-%module%\logs" mkdir "%runtimeDir%\supersonic-%module%\logs"

tar -zxvf "%buildDir%\supersonic-webapp.tar.gz" -C "%buildDir%"
if not exist "%runtimeDir%\supersonic-%module%\webapp" mkdir "%runtimeDir%\supersonic-%module%\webapp"
xcopy /s /e /h /y "%buildDir%\supersonic-webapp\*" "%runtimeDir%\supersonic-%module%\webapp"
if not exist "%runtimeDir%\supersonic-%module%\conf\webapp" mkdir "%runtimeDir%\supersonic-%module%\conf\webapp"
xcopy /s /e /h /y "%runtimeDir%\supersonic-%module%\webapp\*" "%runtimeDir%\supersonic-%module%\conf\webapp"
rd /s /q "%buildDir%\supersonic-webapp"

::3. start service
::start standalone service
if "%command%"=="start" (
    call:START
    goto :EOF
)

if "%command%"=="restart" (
    call:START
    goto :EOF
)

:START
    if "%module%"=="standalone" (
          cd "%runtimeDir%"
          "%pip_path%" install -r "%llm_path%\requirements.txt"
          "%python_path%" -c "import langchain,fastapi,chromadb,tiktoken,uvicorn" >nul 2>&1
          cd "%runtimeDir%\supersonic-standalone\llm\llm"
          start "" /B uvicorn %start_name%:app --port %llm_port% --host %llm_host%  > "%runtimeDir%\supersonic-standalone\llm\llm.log" 2>&1
          echo "llm service started, see logs/error with logs/error command"
    )
    start "supersonic" /B java %java-command%>"%runtimeDir%\supersonic-%module%\logs\info-%module%.log" 2>&1
    echo "%module% service started, see logs/error with logs/error command"
    goto :EOF


:STOP
    for /f "tokens=2" %%i in ('tasklist ^| findstr /i "python"') do (
                     taskkill /PID %%i /F
                     echo "llm Process (PID = %%i) is killed."
    )
    for /f "tokens=2" %%i in ('tasklist ^| findstr /i "java"') do (
                taskkill /PID %%i /F
                echo "%module% Process (PID = %%i) is killed."
    )
    goto :EOF