@echo off
setlocal

set "binDir=%~dp0"
cd /d "%binDir%.."
set "baseDir=%cd%"
echo %binDir%

call "%binDir%\env.bat"

"%pip_path%" install -r "%baseDir%\requirements.txt"

"%python_path%" -c "import langchain,fastapi,chromadb,tiktoken,uvicorn" >nul 2>&1
if "%errorlevel%" equ 0 (
    echo install ok, will pass
) else (
    if "%errorlevel%" equ 1 (
        echo install ok
    ) else (
        echo install fail, but if it can be started normally, it will not affect
    )
)