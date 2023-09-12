@echo off
setlocal

set "binDir=%~dp0"
cd /d "%binDir%.."
set "baseDir=%cd%"

call "%baseDir%\bin\env.bat"

set "command=%~1"


if "%command%"=="start" (
   for /f "tokens=2" %%i in ('tasklist ^| findstr /i "Python"') do (
        taskkill /PID %%i /F
        echo "Process (PID = %%i) is running."
        goto :EOF
    )
    "%baseDir%\bin\run.bat"
    echo "llm service started, see logs/error with logs/error command"
    goto :EOF
)


if "%command%"=="stop" (
    for /f "tokens=2" %%i in ('tasklist ^| findstr /i "Python"') do (
        taskkill /PID %%i /F
        echo "Process (PID = %%i) is killed."
        goto :EOF
    )
    echo "Process is not running."
    goto :EOF
)


if "%command%"=="restart" (
    for /f "tokens=2" %%i in ('tasklist ^| findstr /i "Python"') do (
            taskkill /PID %%i /F
            echo "Process (PID = %%i) is killed."
        )
    "%baseDir%\bin\run.bat"
    echo "Process started, see logs/error with logs/error command"
    goto :EOF
)