@echo off
setlocal EnableExtensions

cd /d "%~dp0"

set "NO_PAUSA=0"
if /I "%~1"=="--no-pause" set "NO_PAUSA=1"
set "CODIGO_SALIDA=0"

set "MSYS2_DIR=C:\msys64"
set "UCRT_BIN=%MSYS2_DIR%\ucrt64\bin"
set "UCRT_INCLUDE=%MSYS2_DIR%\ucrt64\include"
set "UCRT_LIB=%MSYS2_DIR%\ucrt64\lib"
set "RUNTIME_LOCAL=tools\msys2-ucrt64-runtime"
set "FUENTE=Geometry Jump C++ Edition\main.cpp"
set "SALIDA_DIR=Geometry Jump C++ Edition\inst"
set "SALIDA_EXE=%SALIDA_DIR%\GeometryJump.exe"

echo [Geometry Jump C++] Verificando herramientas...
echo MSYS2: "%MSYS2_DIR%"
echo g++ esperado en: "%UCRT_BIN%\g++.exe"
echo raylib.h esperado en: "%UCRT_INCLUDE%\raylib.h"
echo libreria de raylib esperada en: "%UCRT_LIB%"
echo.

if not exist "%UCRT_BIN%\g++.exe" goto :faltan_herramientas
if not exist "%UCRT_INCLUDE%\raylib.h" goto :faltan_herramientas
if not exist "%UCRT_LIB%\libraylib.a" if not exist "%UCRT_LIB%\libraylib.dll.a" goto :faltan_herramientas
goto :compilar

:faltan_herramientas
echo Faltan herramientas o librerias.
echo Se intentara ejecutar "Descargar herramientas.bat" para completarlas.
echo.
call "Descargar herramientas.bat" --no-pause
if errorlevel 1 (
  echo.
  echo No se pudieron preparar las herramientas necesarias.
  set "CODIGO_SALIDA=1"
  goto :salir
)

if not exist "%UCRT_BIN%\g++.exe" (
  echo g++ sigue faltando despues de la instalacion.
  set "CODIGO_SALIDA=1"
  goto :salir
)
if not exist "%UCRT_INCLUDE%\raylib.h" (
  echo raylib.h sigue faltando despues de la instalacion.
  set "CODIGO_SALIDA=1"
  goto :salir
)
if not exist "%UCRT_LIB%\libraylib.a" if not exist "%UCRT_LIB%\libraylib.dll.a" (
  echo La libreria de raylib sigue faltando despues de la instalacion.
  set "CODIGO_SALIDA=1"
  goto :salir
)

:compilar
if not exist "%FUENTE%" (
  echo No se encontro el archivo fuente "%FUENTE%".
  set "CODIGO_SALIDA=1"
  goto :salir
)

if not exist "%SALIDA_DIR%" mkdir "%SALIDA_DIR%"

set "PATH=%UCRT_BIN%;%PATH%"
set "RAYLIB_ENLACE="
if exist "%UCRT_LIB%\libraylib.a" (
  set "RAYLIB_ENLACE=%UCRT_LIB%\libraylib.a"
) else if exist "%UCRT_LIB%\libraylib.dll.a" (
  set "RAYLIB_ENLACE=%UCRT_LIB%\libraylib.dll.a"
)

echo [Geometry Jump C++] Compilando...
"%UCRT_BIN%\g++.exe" "%FUENTE%" -o "%SALIDA_EXE%" ^
 -I"%UCRT_INCLUDE%" ^
 "%RAYLIB_ENLACE%" ^
 -L"%UCRT_LIB%" ^
 -lglfw3 -lopengl32 -lgdi32 -lwinmm
if errorlevel 1 (
  echo.
  echo La compilacion fallo.
  set "CODIGO_SALIDA=1"
  goto :salir
)

echo [Geometry Jump C++] Copiando DLL necesarias...
for %%F in (libraylib.dll glfw3.dll libwinpthread-1.dll libgcc_s_seh-1.dll libstdc++-6.dll) do (
  if exist "%RUNTIME_LOCAL%\%%F" copy /Y "%RUNTIME_LOCAL%\%%F" "%SALIDA_DIR%\%%F" >nul
)

echo.
echo Compilacion completada:
echo "%cd%\%SALIDA_EXE%"
set "CODIGO_SALIDA=0"

:salir
if "%NO_PAUSA%"=="0" pause
exit /b %CODIGO_SALIDA%
