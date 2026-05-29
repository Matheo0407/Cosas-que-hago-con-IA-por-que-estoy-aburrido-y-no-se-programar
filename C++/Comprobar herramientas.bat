@echo off
setlocal

cd /d "%~dp0"

set "MSYS2_DIR=C:\msys64"
set "MSYS2_BASH=%MSYS2_DIR%\usr\bin\bash.exe"
set "UCRT_BIN=%MSYS2_DIR%\ucrt64\bin"
set "UCRT_INCLUDE=%MSYS2_DIR%\ucrt64\include"
set "UCRT_LIB=%MSYS2_DIR%\ucrt64\lib"
set "RUNTIME_LOCAL=tools\msys2-ucrt64-runtime"

echo [Geometry Jump C++] Estado de herramientas
echo.
echo MSYS2 esperado en:
echo "%MSYS2_DIR%"
if exist "%MSYS2_BASH%" (
  echo OK: bash.exe encontrado
) else (
  echo FALTA: bash.exe no encontrado
)
echo.
echo g++ esperado en:
echo "%UCRT_BIN%\g++.exe"
if exist "%UCRT_BIN%\g++.exe" (
  echo OK: g++.exe encontrado
) else (
  echo FALTA: g++.exe no encontrado
)
echo.
echo Cabecera de raylib esperada en:
echo "%UCRT_INCLUDE%\raylib.h"
if exist "%UCRT_INCLUDE%\raylib.h" (
  echo OK: raylib.h encontrado
) else (
  echo FALTA: raylib.h no encontrado
)
echo.
echo Libreria de raylib esperada en:
echo "%UCRT_LIB%"
if exist "%UCRT_LIB%\libraylib.a" (
  echo OK: libraylib.a encontrada
) else if exist "%UCRT_LIB%\raylib.lib" (
  echo OK: raylib.lib encontrada
) else (
  echo FALTA: no se encontro ni libraylib.a ni raylib.lib
)
echo.
echo DLL de raylib esperada en:
echo "%UCRT_BIN%\libraylib.dll"
if exist "%UCRT_BIN%\libraylib.dll" (
  echo OK: libraylib.dll encontrada
) else (
  echo AVISO: libraylib.dll no se encontro en la carpeta bin de MSYS2
)
echo.
echo DLL de GLFW esperada en:
echo "%UCRT_BIN%\glfw3.dll"
if exist "%UCRT_BIN%\glfw3.dll" (
  echo OK: glfw3.dll encontrada
) else (
  echo AVISO: glfw3.dll no se encontro en la carpeta bin de MSYS2
)
echo.
echo DLL opcionales del runtime:
if exist "%UCRT_BIN%\libwinpthread-1.dll" (echo OK: libwinpthread-1.dll) else (echo AVISO: libwinpthread-1.dll falta)
if exist "%UCRT_BIN%\libgcc_s_seh-1.dll" (echo OK: libgcc_s_seh-1.dll) else (echo AVISO: libgcc_s_seh-1.dll falta)
if exist "%UCRT_BIN%\libstdc++-6.dll" (echo OK: libstdc++-6.dll) else (echo AVISO: libstdc++-6.dll falta)
echo.
echo Runtime local del proyecto:
echo "%cd%\%RUNTIME_LOCAL%"
if exist "%RUNTIME_LOCAL%\libraylib.dll" (echo OK: libraylib.dll copiada en local) else (echo AVISO: libraylib.dll no fue copiada en local)
if exist "%RUNTIME_LOCAL%\glfw3.dll" (echo OK: glfw3.dll copiada en local) else (echo AVISO: glfw3.dll no fue copiada en local)
if exist "%RUNTIME_LOCAL%\libwinpthread-1.dll" (echo OK: libwinpthread-1.dll copiada en local) else (echo AVISO: libwinpthread-1.dll no fue copiada en local)
if exist "%RUNTIME_LOCAL%\libgcc_s_seh-1.dll" (echo OK: libgcc_s_seh-1.dll copiada en local) else (echo AVISO: libgcc_s_seh-1.dll no fue copiada en local)
if exist "%RUNTIME_LOCAL%\libstdc++-6.dll" (echo OK: libstdc++-6.dll copiada en local) else (echo AVISO: libstdc++-6.dll no fue copiada en local)
echo.
echo Si falta algo, ejecuta "Descargar herramientas.bat".
pause
