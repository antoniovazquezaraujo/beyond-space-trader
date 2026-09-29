# ADR 0002 — Las letras como datos: fuselajes, piezas y naves

- **Estado**: propuesto
- **Fecha**: 2026-09-29
- **Sustituye en parte al**: [ADR 0001](0001-diseno-de-naves.md) — las letras ya
  no viven en el dibujo del chasis, y el editor cambia de forma.
- **Referencias**: `docs/ships.md`

## Contexto

El 0001 metía las letras de sitio **dentro del dibujo** del chasis. Eso choca con
el dibujo: borrar una letra podía borrar un carácter del casco, y editar sitios
obligaba a parchear el fichero del fuselaje celda a celda. Además, el autor
quiere colorear el casco por zonas y dar a cada letra su estilo.

## Decisión

1. **Tres ficheros, tres papeles**
   - `ships/chassis.txt`: los **fuselajes**. El dibujo (de solo lectura para el
     editor) y la **lista de letras de color** con sus posiciones y tamaños.
   - `ships/pieces.txt`: las **piezas**. Cada una con su **nombre**, su **letra**
     (el sitio que rellena) y su **glifo** (el dibujo).
   - `ships/ships.txt`: las **naves**. Cada nave con su **nombre**, su **tipo**
     (el de la ficha del juego, para el presupuesto) y su **fuselaje**, más los
     **grupos de letras** (letra, posición y cantidad).
2. **El dibujo del fuselaje no se edita**: siempre se ve debajo y el editor no lo
   toca nunca. Las letras son una capa por encima.
3. **Estilo por letra**: cada letra tiene un trío —**color, fondo y parpadeo**—.
   El usuario **añade a la lista las combinaciones que quiera** y les **asigna la
   letra que prefiera**. Las letras con significado fijo para el juego son las de
   sitio (`C M D B R A E G P`); el resto son libres (por ejemplo, las letras de
   las zonas de color del casco).
4. **Los sitios** (lo que el juego rellena) siguen siendo `C M D B R A E G P`, con
   sus mínimos y máximos por tipo (`docs/ships.md`). Ahora se declaran en la nave
   como **grupos** (letra, posición, cantidad), no en el dibujo del chasis.
5. **Las zonas de color del casco** van en el fichero de fuselajes: se pintan
   con letras igual que las piezas, y se guardan como **letra + tamaño +
   posición**. En la otra mitad se ve el casco coloreado con su estilo.
6. **Las piezas** llevan `letra=` en `pieces.txt`; se acabó la convención por
   nombre. Su estilo (color, fondo, parpadeo) es el que ya soporta el formato.
7. **Las naves** se guardan con secciones `[nombre]` y sus claves: `tipo=`,
   `fuselaje=` y los grupos (`letra=A x=5 y=2 n=3`), con el color saliendo del
   estilo de la letra (y, si se quiere, un color por grupo que lo pise).
8. **El editor, en dos mitades**: a la izquierda el fuselaje elegido con las
   letras (cursor visible: se teclea la letra y se escribe donde esté el cursor;
   espacio borra; también se pintan las zonas de color); a la derecha el mismo
   fuselaje con las **piezas reales** sustituyendo a las letras. La **lista de
   piezas** (nombre, letra y glifo) y la **lista de fuselajes** se muestran en una
   franja o panel. Al cambiar de nave se cargan sus letras con sus estilos.
9. **Al guardar se guardan todas las naves modificadas** y las zonas de color de
   los fuselajes tocados.
10. **La validación** de sitios contra la ficha del tipo se mantiene (panel de
    `puesto/máximo` y avisos), ahora contando los grupos de la nave.

## Consecuencias

- El dibujo del casco **nunca** se corrompe desde el editor.
- Las letras pasan a ser datos fáciles de contar, mover y recolorear; el juego
  podrá leerlas para componer.
- El formato crece: listas de letras con estilo y grupos por nave.
- **Pendiente**: el renderizador del juego (sustituir `ShipSprites`), las piezas
  de escudos y artilugios, y decidir si las letras se pueden **arrastrar**
  (mover un grupo ya puesto) además de teclear y borrar.

## Alternativas descartadas

- **Las letras en el dibujo del chasis** (ADR 0001): obliga a parchear el
  fichero y roza el dibujo.
- **Colores fijos por letra en el código**: el autor quiere combinaciones
  libres.
- **Guardar solo la nave actual**: se perderían los cambios de las demás.
