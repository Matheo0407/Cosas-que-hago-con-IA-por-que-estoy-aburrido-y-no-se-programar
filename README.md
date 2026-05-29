# Geometry Jump

`Geometry Jump` es un proyecto base de plataformas ritmicas original, inspirado en juegos de auto-run rapidos.

**No** reproduce Geometry Dash de forma literal. Este repositorio evita copiar arte, audio, marca, menus, servidores u otros contenidos protegidos. En su lugar, ofrece una base real en varios lenguajes con los mismos objetivos generales del genero:

- jugabilidad de cubo con auto-run
- fisicas de salto con gravedad, pads y orbs
- menus y seleccion de nivel
- editor de niveles local
- navegador y publicacion local de "comunidad"
- estructura de nivel compartida

## Versiones incluidas

- `html/Geometry Jump HTML Edition/`: version de navegador con menu, nivel jugable, editor y comunidad local
- `java/`: version de escritorio con Swing, menu, seleccion de nivel, jugabilidad, editor y comunidad local
- `python/Geometry Jump Python Edition/`: prototipo con Tkinter usando el mismo bucle y modelo de nivel
- `C++/Geometry Jump C++ Edition/`: prototipo con raylib siguiendo el mismo flujo general
- `lua/Geometry Jump Lua Edition/`: prototipo con Love2D siguiendo el mismo flujo general

## Alcance actual

Implementado ahora:

- identidad original de `Geometry Jump`
- un nivel demo integrado: `Pulse Run`
- valores de fisicas ajustables compartidos conceptualmente entre ports
- modelo reutilizable de objetos de nivel
- herramientas de editor para bloques, spikes, pads, orbs y coins
- flujo local de guardado y publicacion en web y Java

Todavia no implementado:

- paridad exacta uno a uno con las fisicas de Geometry Dash
- assets, musica, interfaz o layouts exactos de Geometry Dash
- backend online real con cuentas, moderacion, comentarios, tablas de clasificacion o sincronizacion
- efectos de produccion completos, transiciones, particulas y progresion

## Notas de uso

La version mas facil de probar primero es:

1. Abre `html/Geometry Jump HTML Edition/index.html` en un navegador.

Runtimes sugeridos si quieres seguir construyendo:

- Java 17+ para `java/`
- Python 3.11+ para `python/`
- raylib + un compilador C++17 para `C++/`
- Love2D 11.x para `lua/`

## Scripts utiles

- En `java/` usa `Compilar JAR.bat`, `Preparar JAR.bat` y `Geometry Jump Java Edition/Abrir Geometry Jump.bat`
- En `C++/` usa `Descargar herramientas.bat`, `Comprobar herramientas.bat` y `Compilar.bat`

## Proximos pasos

Si quieres, la siguiente pasada puede centrarse en un solo port y llevarlo mucho mas lejos:

- fisicas mas parecidas
- sincronizacion real con audio
- serializacion e importacion/exportacion de niveles
- mas modos de juego
- backend y API online
- pipeline de arte y audio mas pulido

Mira `docs/hoja-de-ruta.md` para el orden sugerido de construccion.
