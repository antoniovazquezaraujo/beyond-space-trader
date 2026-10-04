# ADR 0001 — Diseño de las naves: chasis, piezas y sitios

- **Estado**: sustituido en parte por el [ADR 0002](0002-letras-como-datos.md)
  (las letras ya no viven en el dibujo del chasis y el editor cambia de forma)
- **Fecha**: 2026-09-29
- **Referencias**: `docs/developer/ships.md` (el formato y las tablas), `docs/developer/ui-design.md`,
  `ships/chassis.txt`, `ships/pieces.txt`

## Contexto

El juego dibuja hoy un sprite ASCII por tipo de nave (`ShipSprites`, hasta 12×5,
sin equipo). Queremos naves más ricas y **distintas según lo que lleven montado**
(un Wasp pirata con tres láseres no debe verse igual que un mercante desarmado),
sin depender de las fuentes del jugador y sin generadores procedurales (los
experimentos se descartaron en la #179).

## Decisión

1. **El arte vive en ficheros de texto** editables a mano y recargables:
   `ships/chassis.txt` (chasis), `ships/pieces.txt` (piezas) y `ships/ships.txt`
   (el montaje/previsualización). Formato detallado en `docs/developer/ships.md`: espacio =
   vacío, punto = tinta, comentarios `;;`, claves `color`, `bgcolor`, `blink`,
   `wide`/`narrow`; colores con nombre en inglés, `#rrggbb` o índice 0-255.
2. **Un montaje por tipo de nave** (los 17 del juego). Los 10 comprables se
   dibujan con sitios; los especiales (monstruo, Mantis, Dragonfly, Scarab,
   Scorpion, botella) van con arte fijo porque el jugador nunca los equipa.
3. **La nave se compone**: chasis + piezas encima (lo de encima tapa lo de
   debajo). El **juego decide el tipo** (encuentro, astillero, nave del jugador)
   y **la carga** (sus ranuras y reglas); el arte solo aporta las tres piezas del
   sistema: chasis, sitios y piezas.
4. **Sitios**: letras reservadas dibujadas en el chasis — `C` cabina, `M`
   motores, `D` depósito, `B` medidor de bodegas, `R` rol, `A` arma, `E` escudo,
   `G` artilugio, `P` cápsula de escape. Cada tipo exige unos mínimos y máximos
   (tabla en `docs/developer/ships.md`): los máximos salen de las fichas del juego
   (ranuras), los fijos del tamaño. Un grupo de letras (`MMM`) es el hueco que
   puede ocupar la pieza.
5. **Los sitios van en huecos del dibujo**: el compositor no pisa el fuselaje.
   Escribir una letra sobre una celda dibujada se rechaza, y borrar (espacio)
   quita el sitio y **devuelve el dibujo** (hay copia del chasis original).
6. **El compositor** (`run-composer.sh`) lee, valida y previsualiza; no es el
   juego:
   - panel lateral con lo puesto/máximo de cada sitio y avisos (`ok`, `⚠` falta,
     `✗` sobra, `·` opcional);
   - `t` lista de tipos (el panel sigue al tipo elegido);
   - `e` modo sitios: se teclea la letra y se escribe en el cursor; `s` guarda el
     chasis (parcheando solo esas celdas: comentarios y claves intactos);
   - `v` previsualización: **oculta las letras** y dibuja las piezas reales por
     convención (`motor`, `cabina`, `deposito`, `capsula`, `torreta*`,
     `escudo*`, `artilugio*`, `marca *`) más el medidor de bodegas; presets:
     vacía, comerciante, pirata, policía, a tope.
7. **Los glifos**: se leen como puntos de código (Unicode completo). Hay una
   **paleta segura** y una extendida en `docs/developer/ships.md`; los glifos de
   presentación emoji quedan fuera; `wide=`/`narrow=` corrigen los que el
   terminal pinta distinto; la tira (`g`) del compositor es el juez.
8. **El medidor de bodegas** es braille: 1 punto = 1 bodega, 2 a 9 celdas según
   la capacidad real (artilugios incluidos), y lo dibuja el juego. Las bodegas
   ocultas pueden ir en otro color (pendiente).
9. **Estados** (rol, daños, escudos, camuflaje) se representan con sitios, piezas
   y color/parpadeo, no con dibujos duplicados. Las marcas de rol son `$`
   (comerciante), `☠` (pirata) y una sirena `✶` que parpadea rojo/azul.

## Consecuencias

- El trabajo de dibujo queda acotado: **un chasis por tipo** más las piezas, que
  se comparten entre todas las naves.
- La carga se ve: el mismo tipo cambia según lo que lleve montado, sin arte
  adicional.
- El arte oficial debe ceñirse a la paleta segura; el formato sigue siendo libre
  para quien quiera experimentar.
- **Pendiente**: el renderizador del juego (que sustituya `ShipSprites` por
  chasis + sitios + piezas + carga) y las piezas de escudos y artilugios (hasta
  que existan, sus sitios se quedan como letra en la previsualización).

## Alternativas descartadas

- **Generadores procedurales** de naves (probados y retirados en la #179).
- **Imágenes por tipo** (los BMP originales): no reflejan la carga y atan el arte
  a un formato gráfico.
- **Unicode libre sin control**: rompe el dibujo cuando el terminal y las tablas
  de anchura no coinciden (fichas de dominó, ⚡).
- **Que el compositor escriba el dibujo entero**: perdería comentarios, colores y
  el resto del fichero; se parchean solo las celdas de los sitios.

## Cambios que pide el autor

- En un fichero tendremos los fuselajes, en otro las piezas y en otro las naves
- Se tiene que poder seleccionar el fuselaje de una lista que mostrará los que hay en el fichero de fuselajes 
- En el fichero piezas cada una tendrá su nombre, su letra y su glifo  
- En el fichero ships tendremos las naves, con el nombre de la nave, el tipo de fuselaje y los grupos de letras, la posición cantidad y el color de cada grupo de letras 
- Se tiene que mostrar una lista de piezas con la letra y el glifo que estarán en el fichero piezas
- Se tiene que mostrar el fuselaje seleccionado en media pantalla y permitir que el usuario teclee encima las letras que desee en la posición que desee
- Se tiene que mostrar el cursor para que el usuario sepa dónde aparecerá cada letra
- Se tiene que poder seleccionar el color de la letra, el fondo y si será intermitente
- En la otra mitad de la pantalla se debe mostrar el fuselaje con las piezas reales sustituyendo a las letras
- Al seleccionar otro fuselaje, se colocarán las letras como están en ese fuselaje y con su color
- Al guardar se guardan TODOS los fuselajes modificados, es decir, las naves.
- Con espacio se eliminan letras
- El fuselaje siempre se ve debajo, no se puede editar
- De la misma forma que las naves, se deben poder editar los colores de los fuselajes: el usuario asignará a cada letra un color y podrá colocar la letra en la zona que 
desee, mostrándose en la otra mitad el mismo fuselaje coloreado en esa zona con ese color. Eso se guardará en el fichero de fuselajes, con la lista de zonas de color, su tamaño y posición

