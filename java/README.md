# Port Java

Este es el port principal de escritorio para `Geometry Jump`.

## Compilar

```powershell
javac -d out src\geometryjump\GeometryJump.java
```

## Ejecutar

```powershell
java -cp out geometryjump.GeometryJump
```

## Compilacion rapida en Windows

Haz doble clic en `Compilar JAR.bat` para compilar y crear `Geometry Jump Java Edition\GeometryJump.jar`.

Haz doble clic en `Preparar JAR.bat` para compilarlo sin abrir el JAR automaticamente.

Si ya existe el JAR, puedes abrirlo con `Geometry Jump Java Edition\Abrir Geometry Jump.bat`.

Estos scripts tambien intentan reutilizar el Java incluido con Minecraft Launcher si Java no esta en `PATH`.

## Incluye

- menu
- seleccion de nivel
- nivel demo jugable
- editor
- comunidad local

## Notas

- Esta version usa solo Java Swing.
- El guardado y la publicacion de niveles son locales a la sesion actual.
- La maquina actual no tenia Java instalado globalmente cuando se creo este port, asi que se trabajo usando el runtime de Minecraft.
