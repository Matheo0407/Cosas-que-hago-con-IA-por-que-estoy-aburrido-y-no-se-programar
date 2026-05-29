@echo off
setlocal

cd /d "%~dp0"
set "DIST_DIR=Geometry Jump Java Edition"
set "NO_PAUSA=0"
if /I "%~1"=="--no-pause" set "NO_PAUSA=1"
set "CODIGO_SALIDA=0"

set "JAVA_BIN="
for %%D in (
  "%LOCALAPPDATA%\Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime\java-runtime-gamma\windows-x64\java-runtime-gamma\bin"
  "%LOCALAPPDATA%\Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime\java-runtime-epsilon\windows-x64\java-runtime-epsilon\bin"
  "%LOCALAPPDATA%\Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime\jre-legacy\windows-x64\jre-legacy\bin"
) do (
  if exist "%%~D\javac.exe" (
    set "JAVA_BIN=%%~D"
    goto :java_encontrado
  )
)

where javac >nul 2>nul
if not errorlevel 1 (
  set "JAVA_BIN="
  goto :java_encontrado
)

echo.
echo No se encontro javac.exe. Abre Minecraft Launcher una vez o instala un JDK.
set "CODIGO_SALIDA=1"
goto :salir

:java_encontrado
echo [Geometry Jump] Compilando clases...
if not exist out mkdir out
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

if defined JAVA_BIN (
  "%JAVA_BIN%\javac.exe" -d out src\geometryjump\GeometryJump.java
) else (
  javac -d out src\geometryjump\GeometryJump.java
)
if errorlevel 1 (
  echo.
  echo La compilacion fallo. Asegurate de tener un JDK completo y no solo el runtime de Java.
  set "CODIGO_SALIDA=1"
  goto :salir
)

echo [Geometry Jump] Creando "%DIST_DIR%\GeometryJump.jar"...
if defined JAVA_BIN (
  "%JAVA_BIN%\jar.exe" --create --file "%DIST_DIR%\GeometryJump.jar" --main-class geometryjump.GeometryJump -C out .
) else (
  jar --create --file "%DIST_DIR%\GeometryJump.jar" --main-class geometryjump.GeometryJump -C out .
)
if errorlevel 1 (
  echo.
  echo No se pudo crear el archivo JAR.
  set "CODIGO_SALIDA=1"
  goto :salir
)

echo.
echo Listo: "%cd%\%DIST_DIR%\GeometryJump.jar"
echo Deberia abrirse con doble clic si Windows asocia los archivos .jar con Java.
set "CODIGO_SALIDA=0"

:salir
if "%NO_PAUSA%"=="0" pause
exit /b %CODIGO_SALIDA%
