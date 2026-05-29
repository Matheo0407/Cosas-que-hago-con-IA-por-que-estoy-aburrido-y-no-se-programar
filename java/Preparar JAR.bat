@echo off
setlocal

cd /d "%~dp0"
set "NO_PAUSA=0"
if /I "%~1"=="--no-pause" set "NO_PAUSA=1"

call "Compilar JAR.bat" --no-pause
if errorlevel 1 exit /b 1
echo.
echo El JAR fue creado en "Geometry Jump Java Edition\GeometryJump.jar".
if "%NO_PAUSA%"=="0" pause
