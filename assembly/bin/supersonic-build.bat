@echo off
setlocal enabledelayedexpansion
chcp 65001

set "sbinDir=%~dp0"
call %sbinDir%/supersonic-common.bat %*

set "service=%~1"

cd %projectDir%
if "%service%"=="" (
    set service=%standalone_service%
)

call mvn help:evaluate -Dexpression=project.version > temp.txt
for /f "delims=" %%i in (temp.txt) do (
    set line=%%i
    if not "!line:~0,1!"=="[" (
        set MVN_VERSION=!line!
    )
)
del temp.txt
cd %baseDir%


if "%service%"=="webapp" (
   call :buildWebapp
   tar xvf supersonic-webapp.tar.gz
   move /y supersonic-webapp webapp
   move /y webapp %projectDir%\launchers\%STANDALONE_SERVICE%\target\classes
   goto :EOF
) else (
   call :buildJavaService
   call :buildWebapp
   call :packageRelease
   goto :EOF
)


:buildJavaService
   set "model_name=%service%"
   echo "starting building supersonic-%model_name% service"
   call mvn -f %projectDir% clean package -DskipTests -Dspotless.skip=true
   IF ERRORLEVEL 1 (
      ECHO Failed to build backend Java modules.
      EXIT /B 1
   )
   copy /y %projectDir%\launchers\%model_name%\target\*.tar.gz %buildDir%\
   echo "finished building supersonic-%model_name% service"
   goto :EOF


:buildWebapp
   echo "starting building supersonic webapp"
   cd %projectDir%\webapp
   call start-fe-prod.bat
   copy /y supersonic-webapp.tar.gz %buildDir%\
   rem check build result
   IF ERRORLEVEL 1 (
        ECHO Failed to build frontend webapp.
        EXIT /B 1
   )
   echo "finished building supersonic webapp"
   goto :EOF


:packageRelease
   set "model_name=%service%"
   set "release_dir=supersonic-%model_name%-%MVN_VERSION%"
   set "service_name=launchers-%model_name%-%MVN_VERSION%"
   echo "starting packaging supersonic release"
   cd %buildDir%
   if exist %release_dir% rmdir /s /q %release_dir%
   if exist %release_dir%.zip del %release_dir%.zip
   mkdir %release_dir%
   rem package webapp
   tar xvf supersonic-webapp.tar.gz
   move /y supersonic-webapp webapp
   echo {"env": ""} > webapp\supersonic.config.json
   move /y webapp %release_dir%
   rem package java service
   tar xvf %service_name%-bin.tar.gz
   for /d %%D in ("%service_name%\*") do (
       move "%%D" "%release_dir%"
   )
   rem generate zip file
   powershell Compress-Archive -Path %release_dir% -DestinationPath %release_dir%.zip
   del %service_name%-bin.tar.gz
   del supersonic-webapp.tar.gz
   rmdir /s /q %service_name%
   echo "finished packaging supersonic release"
   goto :EOF

endlocal