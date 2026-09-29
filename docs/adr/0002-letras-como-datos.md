# ADR 0002 — Las letras como datos: fuselajes, piezas y naves

- **Estado**: propuesto
- **Fecha**: 2026-09-29
- **Sustituye en parte al**: [ADR 0001](0001-diseno-de-naves.md) — las letras ya
  no viven en el dibujo del chasis, y hay **dos editores separados**.
- **Referencias**: `docs/ships.md`

## Contexto

El 0001 metía las letras de sitio **dentro del dibujo** del chasis. Eso choca con
el dibujo: borrar una letra podía borrar un carácter del casco, y editar sitios
obligaba a parchear el fichero del fuselaje celda a celda. Además, el autor
quiere colorear el casco por zonas y no mezclar esa tarea con la de montar naves.

## Decisión

1. **Tres ficheros, tres papeles**
   - `ships/chassis.txt`: los **fuselajes**. El dibujo (de solo lectura para el
     editor) y la **lista de letras de color** con sus posiciones y tamaños.
   - `ships/pieces.txt`: las **piezas**. Cada una con su **nombre**, su **letra**
     (el sitio que rellena) y su **glifo** (el dibujo).
   - `ships/ships.txt`: las **naves**. Cada nave con su **nombre**, su **tipo**
     (el de la ficha del juego, para el presupuesto) y su **fuselaje**, más los
     **grupos de letras** (letra, posición, cantidad y color).
2. **Dos editores separados: naves y fuselajes.** No se mezclan: son modos
   distintos, con su lista de letras y su previsualización cada uno.
3. **Editor de fuselajes**: el dibujo **no se toca** (siempre se ve debajo). Se
   le añaden **letras de color**, que son **libres**: el usuario define las
   combinaciones que necesite (color, fondo y parpadeo) y les asigna la letra que
   prefiera. Se pintan sobre el casco y se guardan en el fichero de fuselajes
   como **key + tamaño + posición**. En la otra mitad se ve el casco coloreado.
4. **Editor de naves**: el fuselaje se muestra **tal cual, con sus colores ya
   puestos**, y **no se toca**. Sobre él se teclean las **letras de las piezas**
   (los sitios: `C` cabina, `M` motores, `D` depósito, `B` medidor de bodegas,
   `R` rol, `A` arma, `E` escudo, `G` artilugio, `P` cápsula), con **cursor
   visible**; espacio borra. Se guardan en el fichero de naves como **grupos**
   (letra, posición, cantidad y color). En la otra mitad se ve la nave con las
   **piezas reales**. Al cambiar de nave se cargan sus letras con sus colores.
5. **Los dos alfabetos no se mezclan**: las letras de color del fuselaje viven en
   el fichero de fuselajes y las letras de las piezas en el de naves; cada editor
   tiene su propia lista. Así ninguna letra de un editor interfiere con el otro.
6. **Los sitios** (lo que el juego rellena) son los de las piezas, con sus
   mínimos y máximos por tipo (`docs/ships.md`); la validación de siempre
   (`puesto/máximo` y avisos) cuenta los grupos de la nave.
7. **Las piezas** llevan `key=` en `pieces.txt`; se acabó la convención por
   nombre. Su estilo (color, fondo, parpadeo) es el que ya soporta el formato.
8. **Las naves** se guardan con secciones `[nombre]` y sus claves: `key=`, `type=`,
   `chasis=` y los grupos (`group=A x=5 y=2 n=3`), con el color del grupo.
9. **Guardados**: el editor de naves guarda **todas las naves modificadas**; el
   de fuselajes, las letras de color de los fuselajes tocados.
10. **Reparto de pantalla** en los dos editores: mitad izquierda la edición,
    mitad derecha la previsualización; las listas (piezas, fuselajes, naves,
    letras) en una franja o panel.

## Consecuencias

- El dibujo del casco **nunca** se corrompe desde ningún editor.
- Cada tarea tiene su herramienta: colorear el casco no se mezcla con montar la
  nave, y ninguna de las dos toca el dibujo.
- Las letras pasan a ser datos fáciles de contar, mover y recolorear; el juego
  podrá leerlas para componer.
- El formato crece: listas de letras con estilo, zonas y grupos por nave.
- **Pendiente**: el renderizador del juego (sustituir `ShipSprites`), las piezas
  de escudos y artilugios, y decidir si las letras se pueden **arrastrar** (mover
  un grupo ya puesto) además de teclear y borrar.

## Alternativas descartadas

- **Las letras en el dibujo del chasis** (ADR 0001): obliga a parchear el
  fichero y roza el dibujo.
- **Un solo editor para todo**: mezclaba colorear el casco con montar la nave.
- **Colores fijos por letra en el código**: el autor quiere combinaciones
  libres.
- **Guardar solo la nave actual**: se perderían los cambios de las demás.
