@echo off
setlocal

set "binDir=%~dp0"
set "baseDir=%~dp0.."
set "confDir=%baseDir%\conf"
call "%baseDir%\bin\env.bat"

set "command=%~1"


if "%command%"=="start" (
    for /f "tokens=2" %%i in ('tasklist ^| findstr /i "java"') do (
        echo "Process (PID = %%i) is running."
        goto :EOF
    )
    echo "Starting %APP_NAME%"
    "%baseDir%\bin\run.bat" %MAIN_CLASS%
    echo "Process started, see logs/error with logs/error command"
    goto :EOF
)


if "%command%"=="stop" (
    for /f "tokens=2" %%i in ('tasklist ^| findstr /i "java"') do (
        taskkill /PID %%i /F
        echo "Process (PID = %%i) is killed."
        goto :EOF
    )
    echo "Process is not running."
    goto :EOF
)


if "%command%"=="restart" (
    for /f "tokens=2" %%i in ('tasklist ^| findstr /i "java"') do (
            taskkill /PID %%i /F
            echo "Process (PID = %%i) is killed."
        )
    "%baseDir%\bin\run.bat" %MAIN_CLASS%
    echo "%APP_NAME% started, see logs/error with logs/error command"
)