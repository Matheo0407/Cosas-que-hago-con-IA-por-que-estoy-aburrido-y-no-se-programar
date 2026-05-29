@echo off
setlocal EnableExtensions

cd /d "%~dp0"

set "NO_PAUSA=0"
if /I "%~1"=="--no-pause" set "NO_PAUSA=1"
set "CODIGO_SALIDA=0"

set "MSYS2_DIR=C:\msys64"
set "MSYS2_BASH=%MSYS2_DIR%\usr\bin\bash.exe"
set "UCRT_BIN=%MSYS2_DIR%\ucrt64\bin"
set "UCRT_INCLUDE=%MSYS2_DIR%\ucrt64\include"
set "UCRT_LIB=%MSYS2_DIR%\ucrt64\lib"
set "RUNTIME_LOCAL=tools\msys2-ucrt64-runtime"

set "FALTA_MSYS2=0"
set "FALTA_GCC=0"
set "FALTA_RAYLIB=0"
set "YA_ACTUALIZO=0"

echo [Geometry Jump C++] Verificando herramientas...
echo.

if not exist "%MSYS2_BASH%" set "FALTA_MSYS2=1"
if not exist "%UCRT_BIN%\g++.exe" set "FALTA_GCC=1"
if not exist "%UCRT_INCLUDE%\raylib.h" set "FALTA_RAYLIB=1"
if not exist "%UCRT_LIB%\libraylib.a" if not exist "%UCRT_LIB%\libraylib.dll.a" set "FALTA_RAYLIB=1"

echo MSYS2: "%MSYS2_DIR%"
if "%FALTA_MSYS2%"=="1" (echo FALTA: MSYS2) else (echo OK: MSYS2)
if "%FALTA_GCC%"=="1" (echo FALTA: g++) else (echo OK: g++)
if "%FALTA_RAYLIB%"=="1" (echo FALTA: raylib) else (echo OK: raylib)
echo.

if "%FALTA_MSYS2%"=="1" (
  where winget >nul 2>nul
  if errorlevel 1 (
    echo No se encontro winget.
    echo Instala App Installer desde Microsoft Store o instala MSYS2 manualmente.
    set "CODIGO_SALIDA=1"
    goto :salir
  )

  echo [1/4] Instalando MSYS2 con winget...
  winget install -e --id MSYS2.MSYS2 --accept-package-agreements --accept-source-agreements
  if errorlevel 1 (
    echo La instalacion de MSYS2 fallo.
    set "CODIGO_SALIDA=1"
    goto :salir
  )

  if not exist "%MSYS2_BASH%" (
    echo MSYS2 no aparecio en "%MSYS2_DIR%" despues de la instalacion.
    set "CODIGO_SALIDA=1"
    goto :salir
  )

  echo [2/4] Actualizando el sistema base de MSYS2...
  "%MSYS2_BASH%" -lc "pacman -Syuu --noconfirm"
  if errorlevel 1 (
    echo La actualizacion grande de MSYS2 puede requerir ejecutar este script una vez mas.
    set "CODIGO_SALIDA=1"
    goto :salir
  )
  set "YA_ACTUALIZO=1"
)

if "%FALTA_GCC%"=="1" (
  if "%YA_ACTUALIZO%"=="0" (
    echo [2/4] Actualizando el indice de paquetes...
    "%MSYS2_BASH%" -lc "pacman -Sy --noconfirm"
    if errorlevel 1 (
      echo No se pudo actualizar el indice de paquetes de MSYS2.
      set "CODIGO_SALIDA=1"
      goto :salir
    )
    set "YA_ACTUALIZO=1"
  )
  echo [3/4] Instalando g++...
  "%MSYS2_BASH%" -lc "pacman -S --noconfirm --needed mingw-w64-ucrt-x86_64-gcc"
  if errorlevel 1 (
    echo La instalacion de g++ fallo.
    set "CODIGO_SALIDA=1"
    goto :salir
  )
)

if "%FALTA_RAYLIB%"=="1" (
  if "%YA_ACTUALIZO%"=="0" (
    echo [2/4] Actualizando el indice de paquetes...
    "%MSYS2_BASH%" -lc "pacman -Sy --noconfirm"
    if errorlevel 1 (
      echo No se pudo actualizar el indice de paquetes de MSYS2.
      set "CODIGO_SALIDA=1"
      goto :salir
    )
    set "YA_ACTUALIZO=1"
  )
  echo [4/4] Instalando raylib...
  "%MSYS2_BASH%" -lc "pacman -S --noconfirm --needed mingw-w64-ucrt-x86_64-raylib"
  if errorlevel 1 (
    echo La instalacion de raylib fallo.
    set "CODIGO_SALIDA=1"
    goto :salir
  )
)

if not exist "%UCRT_BIN%\g++.exe" (
  echo g++ sigue faltando en "%UCRT_BIN%\g++.exe".
  set "CODIGO_SALIDA=1"
  goto :salir
)

if not exist "%UCRT_INCLUDE%\raylib.h" (
  echo raylib.h sigue faltando en "%UCRT_INCLUDE%\raylib.h".
  set "CODIGO_SALIDA=1"
  goto :salir
)

if not exist "%UCRT_LIB%\libraylib.a" if not exist "%UCRT_LIB%\libraylib.dll.a" (
  echo La libreria de raylib sigue faltando en "%UCRT_LIB%".
  set "CODIGO_SALIDA=1"
  goto :salir
)

echo [Runtime] Sincronizando DLL locales...
if not exist "%RUNTIME_LOCAL%" mkdir "%RUNTIME_LOCAL%"
for %%F in (libraylib.dll glfw3.dll libwinpthread-1.dll libgcc_s_seh-1.dll libstdc++-6.dll) do (
  if exist "%UCRT_BIN%\%%F" copy /Y "%UCRT_BIN%\%%F" "%RUNTIME_LOCAL%\%%F" >nul
)

echo.
echo Estado final:
echo - g++: "%UCRT_BIN%\g++.exe"
echo - raylib.h: "%UCRT_INCLUDE%\raylib.h"
if exist "%UCRT_LIB%\libraylib.a" (
  echo - Enlace estatico disponible: "%UCRT_LIB%\libraylib.a"
) else (
  echo - Enlace dinamico disponible: "%UCRT_LIB%\libraylib.dll.a"
)
if exist "%RUNTIME_LOCAL%\libraylib.dll" (
  echo - DLL local de raylib: "%cd%\%RUNTIME_LOCAL%\libraylib.dll"
) else (
  echo - DLL local de raylib: no encontrada, se priorizara el enlace estatico
)
if exist "%RUNTIME_LOCAL%\glfw3.dll" (
  echo - DLL local de GLFW: "%cd%\%RUNTIME_LOCAL%\glfw3.dll"
) else (
  echo - DLL local de GLFW: no encontrada
)
echo.
echo Ya puedes ejecutar "Compilar.bat".
set "CODIGO_SALIDA=0"

:salir
if "%NO_PAUSA%"=="0" pause
exit /b %CODIGO_SALIDA%
