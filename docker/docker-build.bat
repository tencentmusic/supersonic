@echo off
setlocal

REM Function to execute the build script
:execute_build_script
echo Executing build script: assembly\bin\supersonic-build.bat
call assembly\bin\supersonic-build.bat
if %errorlevel% neq 0 (
    echo Build script failed. Exiting.
    exit /b 1
)
goto :eof

REM Function to build the Docker image
:build_docker_image
set "version=%1"
echo Building Docker image: supersonic:%version%
docker build --no-cache --build-arg SUPERSONIC_VERSION=%version% -t supersonicbi/supersonic:%version% -f docker\Dockerfile .
if %errorlevel% neq 0 (
    echo Docker build failed. Exiting.
    exit /b 1
)
echo Docker image supersonic:%version% built successfully.
goto :eof

REM Main script execution
set "VERSION=%1"
if "%VERSION%"=="" (
    echo Usage: %0 ^<version^>
    exit /b 1
)

call :execute_build_script
call :build_docker_image %VERSION%

endlocal