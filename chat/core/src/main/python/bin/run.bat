@echo off
setlocal

set "binDir=%~dp0"
cd /d "%binDir%.."
set "baseDir=%cd%"

call "%binDir%\env.bat"
echo "%binDir%"

if "%llm_host%"=="" (
    echo llm_host llm_port is not set
    exit /b 1
)

if "%python_path%"=="" (
    echo please set env value python_path, pip_path to python, pip path by export cmd
    exit /b 1
)

call "%binDir%\install.bat"
cd "%baseDir%\llm"
start "" /B uvicorn %start_name%:app --port %llm_port% --host %llm_host% > "%baseDir%\llm.log" 2>&1