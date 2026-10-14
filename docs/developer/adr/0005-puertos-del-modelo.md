# ADR 0005 — Los puertos del modelo: `org.gts.bst.ports`

- **Estado**: aceptado
- **Fecha**: 2026-10-07
- **Referencias**: issues #46 y #147–#152, ADR 0004
  (`0004-descomposicion-de-game.md`), `spacetrader/Game.java`,
  `org/gts/bst/ports/`

## Contexto

El proyecto repite dos principios: "MVP en las pantallas" y "el modelo no conoce
la UI". El segundo no era del todo cierto. `spacetrader.*` no conoce Lanterna,
pero **siete clases** (`Game`, `Commander`, `Encounter`, `Trade`, `Functions`,
`GameOptions` y `HighScores`) importaban `org.gts.bst.view` para pedir los
diálogos modales (`DialogService`), la pantalla de encuentro y los refrescos
(`GameWindow`) o el saqueo y el comercio del encuentro (`EncounterDialogHost`).
La dependencia era unidireccional y sin tipos de Lanterna, pero el modelo
dependía de un paquete llamado `view`: la frontera estaba bien pensada y mal
nombrada.

Con el troceo de `Game` (ADR 0004) la pregunta vuelve en cada extracción: si el
universo, el viaje o los encuentros se van a componentes propios, ¿qué reciben
para hablar con el jugador? ¿Una referencia a `Game`, una a `DialogService`...?

## Decisión

**Los puertos se quedan, se pasan explícitos y viven en un paquete neutro:
`org.gts.bst.ports`.**

1. **Los puertos se quedan.** El flujo del port original está lleno de diálogos
   a mitad de la lógica: la respuesta del jugador decide el siguiente paso, no
   solo el texto que se muestra después. Convertirlo en "el modelo calcula
   resultados y el presentador los muestra" es un cambio de flujo grande, y
   #46 es un troceo sin cambios de comportamiento.
2. **Se pasan explícitos.** Cada componente que salga del troceo recibe
   `DialogService`, `GameWindow` o `EncounterDialogHost` como parámetro o por
   constructor, **nunca una referencia a `Game`** (la regla 2 del ADR 0004).
3. **Paquete neutro.** `DialogService`, `DialogResult`, `GameWindow` y
   `EncounterDialogHost` se mueven de `org.gts.bst.view` a `org.gts.bst.ports`
   con `git mv`: mismos tipos, mismos nombres y mismo comportamiento. El
   paquete no nombra ninguna tecnología; los front-ends lo implementan y el
   modelo solo lo usa.
4. **Los adaptadores se quedan en `view`.** `AlertDialogHost`, `AlertButton`,
   `AlertDefinition`, `Alerts` y `LanternaDialogService` son capa de vista (la
   definición y la presentación de las alertas) y no se mueven.

## Consecuencias

- La regla "el modelo no conoce la UI" pasa a ser **literal y comprobable**:
  `spacetrader.*` ya no importa `org.gts.bst.view` para nada.
- Los componentes del troceo reciben los puertos **explícitos**, así que su
  firma dice exactamente lo que necesitan de la pantalla.
- El movimiento es mecánico: los tests actuales valen tal cual, sin tocar ni
  una expectativa.
- Un ADR futuro podrá **sustituir** este si se adopta el patrón de resultados
  (el modelo devuelve qué ha pasado y el presentador decide cómo contarlo).
  Las implementaciones sin interfaz (`DialogService.NONE` y los falsos de
  test) ya apuntan a ese mundo.

## Alternativas descartadas

- **MVP estricto ya: resultados en vez de puertos.** Es la dirección deseable,
  pero cambia el flujo de juego (encuentros, comercio, viaje) y multiplica el
  alcance de #46; merece su propio ADR y su propio PR.
- **Dejar los puertos en `org.gts.bst.view`.** El modelo seguiría importando un
  paquete de vista: el nombre seguiría mintiendo aunque el contenido no.
- **Pasar `Game` a los componentes.** Cómodo a corto plazo, pero reintroduce el
  god object dentro del componente (ya descartado en el ADR 0004).
- **Poner los puertos en `spacetrader.ports`.** Invierte la propiedad de la
  frontera: los puertos son el contrato entre modelo y presentadores, y
  `org.gts.bst.*` es el sitio de los contratos compartidos.
