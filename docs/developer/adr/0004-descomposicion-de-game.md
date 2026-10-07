# ADR 0004 — Descomposición de Game: fachada y componentes

- **Estado**: aceptado
- **Fecha**: 2026-10-07
- **Referencias**: issues #46 y #147–#152, `spacetrader/Game.java`,
  `spacetrader/Newspaper.java` (la primera área extraída),
  `spacetrader/QuestStates.java`

## Contexto

`Game` es el *god object* del modelo: 1.680 líneas que mezclan la generación
del universo, los precios, las noticias, las misiones, el viaje y la
serialización. Cualquier cambio en un área obliga a tocar el mismo fichero y a
recorrer todo lo demás; los issues #46 y #147–#152 trocean ese trabajo.

La dificultad no es solo el tamaño: los presentadores y las vistas llaman a la
API pública de `Game` (`NewsEvents()`, `PriceCargoBuy()`, `WarpSystem()`…), y la
serialización guarda y carga un hash con claves fijas (`_newsEvents`,
`_paidForNewspaper`…) que las partidas antiguas ya usan. El troceo no puede
romper ninguna de las dos cosas.

## Decisión

**`Game` pasa a ser una fachada que delega; cada área se extrae a una clase del
paquete `spacetrader` que posee su estado y su lógica.**

1. **Un componente por área.** Cada clase extraída guarda su propio estado y
   resuelve sus reglas. `Game` conserva los métodos públicos como delegados, así
   que los que llaman no cambian ni una línea.
2. **Sin referencia de vuelta a `Game`.** Lo que un componente necesita de otra
   área se pasa como **parámetro explícito** (el comandante, el sistema, el
   universo) o como una **interfaz de solo lectura**. Nunca un `Game` dentro del
   componente: eso solo movería el acoplamiento. Ejemplo: los rumores de llegada
   preguntan por las misiones y la tripulación a través de `QuestStates`, que
   `Game` implementa provisionalmente delegando en sus getters; cuando las
   misiones se extraigan (#150), las implementará ese componente.
3. **La serialización no cambia.** `Game` sigue escribiendo y leyendo las mismas
   claves con el mismo formato (`Serializer`/`Deserialize`); el componente solo
   expone su estado. Las partidas guardadas antiguas deben seguir cargando.
4. **Un PR por área, con tests.** El troceo va de menor a mayor acoplamiento, y
   cada extracción lleva sus tests unitarios del componente; los tests actuales
   hacen de red de seguridad (comportamiento idéntico, sin cambios de
   expectativas). Orden propuesto: **noticias → precios/comercio → universo →
   viaje/llegada → misiones/eventos → serialización**. El orden no es rígido: se
   ajusta si una dependencia lo pide.

La primera extracción es **noticias** (#149): `Newspaper` posee `_newsEvents` y
`_paidForNewspaper`, el texto y la cabecera del periódico y el switch de rumores
de llegada. `GameArrival()` sigue llamando a los rumores, ahora al componente.

## Consecuencias

- La API pública de `Game` queda **intacta**: los presentadores, las vistas y
  los tests existentes no cambian.
- El comportamiento es **idéntico**: no se aprovecha para arreglar reglas ni
  textos (cualquier cambio de juego sería otro PR).
- La serialización conserva las claves y el formato, así que las partidas
  guardadas antiguas siguen cargando.
- Cada componente tiene tests unitarios propios, sin depender de la fachada, y
  `Game` se queda con lo que le corresponde: la coordinación y las ventanas.
- `Game` no encoge de golpe: mientras un área no esté extraída, la fachada sigue
  delegando en sí misma. El acoplamiento baja área a área, y el diff de cada PR
  es revisable.

## Alternativas descartadas

- **Dejar todo en `Game` y añadir métodos**: el problema no es la falta de API,
  es que un área no se puede probar ni cambiar sin las demás.
- **Pasar `Game` a cada componente**: cómodo a corto plazo, pero mueve el god
  object dentro del componente y mantiene el acoplamiento circular.
- **Cambiar la serialización a un formato por componente**: rompe las partidas
  guardadas y mezcla el troceo con una migración de datos; se decidirá, si
  acaso, en su propio ADR.
- **Reescribir `Game` de una vez**: PR gigante, imposible de revisar y sin red
  de seguridad por áreas.
