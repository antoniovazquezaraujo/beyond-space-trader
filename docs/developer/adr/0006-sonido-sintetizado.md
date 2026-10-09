# ADR 0006 — Efectos de sonido sintetizados: el puerto `SoundService`

- **Estado**: aceptado
- **Fecha**: 2026-10-09
- **Referencias**: ADR 0005 (`0005-puertos-del-modelo.md`),
  `org/gts/bst/ports/SoundService.java`, `org/gts/bst/ports/SoundEffect.java`,
  `org/gts/bst/audio/SynthesizedSound.java`, `spacetrader/GameOptions.java`,
  `LanternaApp`, `LanternaMainWindow`, `LanternaAlertDialogHost` y
  `EncounterSceneComponent`

## Contexto

El juego no suena: es lo último que le queda al clásico por llegar. La primera
fase son **efectos**, no música, y el autor quiere que suenen a los pitidos
cortos del original, no a una banda sonora. El paquete se publica como zip con
`jlink`, snap estricto e itch.io, así que meter ficheros de audio traería peso,
licencias y rutas nuevas que empaquetar y mantener. Además los tests corren
*headless* (y en CI no hay tarjeta de sonido): nada de lo que se pruebe puede
abrir una línea de audio.

## Decisión

1. **El puerto, en `org.gts.bst.ports` (ADR 0005).** `SoundEffect` es el enum de
   lo que ha pasado (no de cómo suena): `MENU_MOVE`, `MENU_SELECT`, `ALERT`,
   `WARNING`, `LASER`, `HIT`, `EXPLOSION` y `WARP`. `SoundService` tiene
   `void play(SoundEffect)` y la constante `NONE`, un servicio mudo para los
   tests y para el arranque silencioso. El método `playRivalLaser(boolean easy)`
   es un `default` que cae en `LASER`: da al rival fácil un tono propio sin
   ampliar el enum.
2. **El sintetizador, en `org.gts.bst.audio`.** `SynthesizedSound` genera el PCM
   con `javax.sound.sampled` (16 bits, mono, 44,1 kHz) con el timbre del *beeper*
   del ZX Spectrum: la onda se tritura a **1 bit** (cada muestra en el nivel
   máximo, positivo o negativo, con amplitud maestra alta ~0,85 y recorte suave
   para no saturar), las notas son de pulso (duty ajustable), los barridos son
   **arpegios por escalones** (sin suavizar) y el ruido se **conmuta a golpes**
   para golpes y explosiones. Los bordes llevan una rampa de uno o dos
   milisegundos (el chasquido de DC) y el resto es duro; todos los efectos por
   debajo de 700 ms. Cada efecto se renderiza **una vez en el arranque**; un
   hilo demonio con cola limitada los reproduce en segundo plano (si la cola se
   llena, el efecto se descarta: nunca se bloquea la partida).
   `create(BooleanSupplier)` devuelve `SoundService.NONE` cuando no hay
   dispositivo (`LineUnavailableException` o `IllegalArgumentException`).
   `pcm(SoundEffect)` genera las muestras sin abrir línea: es la puerta que usan
   los tests.
3. **El silencio.** La opción `_sound` de `GameOptions` (por defecto activada;
   los saves y ajustes viejos sin la clave también) y los argumentos `--mute` y
   `--no-sound`, que inyectan `SoundService.NONE`. El `BooleanSupplier` del
   sintetizador consulta la opción en cada `play`, así que apagarla en el panel
   de opciones (`F8`) silencia el juego al momento.
4. **El cableado.** La vista y los presentadores piden los efectos; el modelo no
   cambia:
   - menú (`F10`) y listas con selección (opciones, comercio, naves, equipo,
     personal, misiones): `MENU_MOVE` al moverse, `MENU_SELECT` al activar;
   - diálogos: `ALERT` con un solo botón, `WARNING` cuando es una pregunta. Los
     avisos silenciosos que van al registro no suenan;
   - combate: `LASER` cuando nace un disparo (el del rival con tono grave si su
     piloto es flojo), `HIT` en cada impacto, `EXPLOSION` cuando una nave queda
     destruida;
   - huida y salto: `WARP`.
5. **Las pruebas.** `pcm` se comprueba sin dispositivo (cada efecto suena, es de
   1 bit, dura lo razonable y no satura), `SoundService.NONE` es un no-op y el
   cableado se prueba con un `SoundService` grabador. Ningún test abre una línea
   de audio.

## Consecuencias

- **No viaja ningún binario de audio**: no hay recursos nuevos que empaquetar y
  el tono del original se imita desde código.
- El runtime `jlink` ya lleva `java.desktop` (donde vive `javax.sound`); el snap
  estricto añade el plug `audio-playback` y `libasound2` a los `stage-packages`.
- El modelo sigue sin conocer la UI: el puerto es neutro y solo lo usan la
  aplicación y las vistas. Los tests del modelo no tocan el sonido.
- La **música** (ficheros o secuenciador, con su opción propia) no entra aquí:
  merece su propia fase y, si toca el puerto, su propio ADR.

## Alternativas descartadas

- **Ficheros WAV/OGG con los efectos.** Peso, licencias y rutas nuevas en zip,
  snap e itch.io; el sintetizador da el tono de 8 bits sin nada que distribuir.
- **`javax.sound.midi` para los efectos.** Es maquinaria de música: más pesado
  y con peor control de la envolvente para pitidos de milisegundos.
- **Reproducir en el hilo de la interfaz.** La línea escribe al ritmo del
  dispositivo: bloquearía la partida en cada efecto.
- **Una dependencia de audio (LWJGL/OpenAL).** Innecesaria para generar y
  escribir PCM, y engorda el paquete.
- **La música ahora.** El autor la quiere como fase aparte; este ADR solo fija
  los efectos.
