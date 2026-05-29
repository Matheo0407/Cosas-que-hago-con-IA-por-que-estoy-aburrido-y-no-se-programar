# Port en C++

Este port usa `raylib` y sigue el mismo flujo base:

- menu
- seleccion de nivel
- jugabilidad principal con auto-run
- editor simple
- comunidad local

Ejemplo de compilacion despues de instalar raylib:

```bash
g++ -std=c++17 "Geometry Jump C++ Edition/main.cpp" -o "Geometry Jump C++ Edition/inst/GeometryJump.exe" -lraylib -lglfw3 -lopengl32 -lgdi32 -lwinmm
```

## Scripts de Windows

Usa `Descargar herramientas.bat` para instalar:

- MSYS2
- g++
- raylib
- copias locales de DLL dentro de `C++\tools\msys2-ucrt64-runtime`

Usa `Comprobar herramientas.bat` para revisar lo instalado.

Luego usa `Compilar.bat` para compilar:

- `Geometry Jump C++ Edition\inst\GeometryJump.exe`
