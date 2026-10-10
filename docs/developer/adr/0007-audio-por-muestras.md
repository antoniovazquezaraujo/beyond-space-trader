# ADR 0007 — Audio por muestras: `sounds/`, mezclador y ambiente por pantalla

- **Estado**: aceptado
- **Fecha**: 2026-10-09
- **Referencias**: ADR 0005 (`0005-puertos-del-modelo.md`), ADR 0006
  (`0006-sonido-sintetizado.md`, sustituido), `org/gts/bst/ports/SoundService.java`,
  `org/gts/bst/audio/` (`SampledSound`, `SampleLibrary`, `SampleMixer`),
  `docs/developer/sounds.md` y `tools/convert-sounds.sh`

## Contexto

El ADR 0006 apostó por **sintetizar** los efectos en código para no distribuir
ficheros de audio, y se llegó a implementar un beeper de 1 bit. El autor lo oyó
y lo descartó: quiere **sonidos reales**, descargados de bancos como Freesound,
con la misma idea que los samples de LeTrain. La fase A es el motor de muestras,
los samples empaquetados y el ambiente por pantalla; la fase B traerá los motores
de las naves, los cañones por tipo y la música.

Se mantienen del ADR 0006 las decisiones que no dependen del timbre: el puerto
neutro, la reproducción asíncrona que nunca bloquea, el fallback a silencio sin
dispositivo y el mute del jugador. Lo que cambia es de dónde sale el sonido.

## Decisión

1. **Los samples viajan dentro del jar**, como resources del módulo
   (`BeyondSpaceTraderJava/src/main/resources/sounds/<clave>-<n>.wav`); el
   jugador no gestiona ninguna carpeta. El cargador los lee del classpath
   (`/sounds/...`) y **sondea las variantes** `-1`, `-2`... hasta la primera que
   falta (tope 9), porque dentro de un jar no se pueden listar carpetas. Cada
   fichero se convierte a **16 bits, 44,1 kHz, mono** con
   `AudioSystem.getAudioInputStream(formatoCanónico, ...)`; lo que no se puede
   convertir se ignora con un aviso en consola. Las claves son rutas
   (`combat/hit`, `ambient/trade`) y, cuando tienen varias variantes, se elige
   una **al azar en cada reproducción**. Clave ausente = silencio, nunca un
   error.
2. **El puerto crece con `default`s** para no romper `NONE` ni los fakes: además
   de `play(SoundEffect)` y `playRivalLaser`, `playWeapon(WeaponType)`,
   `engine(ShipType, boolean)`, `engineStop(boolean)`, `ambience(AmbienceKey)` y
   `music(MusicTheme)`, con los enums nuevos `AmbienceKey` y `MusicTheme` en
   `org.gts.bst.ports`.
3. **El mezclador** (`SampleMixer`, sin hilo ni dispositivo) suma voces en float:
   one-shots, **1 ambiente**, **1 música** (con crossfade) y **hasta 2 motores**
   (jugador y rival), cada una con ganancia y fades (4 ms de entrada, 12 ms de
   salida en los one-shots; 0,25 s en los bucles). Los canales (efectos,
   ambiente, música, motores) tienen volumen propio con valores por defecto
   sensatos y mutables. `SampledSound` pone el hilo demonio, la `SourceDataLine`
   y la conversión a PCM; `create(BooleanSupplier, LineOpener)` sigue devolviendo
   `NONE` sin dispositivo y los tests inyectan la línea.
4. **Ambiente por pantalla**: el front-end pide una `AmbienceKey` al abrir cada
   panel, al volver a navegación, en la portada (`TITLE`) y en el menú (`MENU`);
   pedir la misma clave no hace nada, y el cambio hace crossfade. El motor de
   muestras mapea las claves de la fase A (`SoundEffect` → `ui/...`, `alerts/...`,
   `combat/...`, `travel/...`) y el cableado existente no cambia.
5. **Fase B: motores, cañones y música.** La escena de encuentros engancha el
   ciclo de vida de los bucles: el motor de cada nave es **constante** mientras
   está en escena y arranca con su animación de entrada (~0,3 s de fade); una
   nave parada, rendida o inutilizada pero presente lo mantiene; al destruirse
   se funde en ~0,15 s con la explosión; al marcharse (huir, escapar, cruzar y
   salir) se funde en ~0,4 s mientras se aleja; al cerrar la escena, motores y
   música hacen un fade rápido (~0,12 s). El sample del motor cae del tipo a la
   talla (`ships/tiny|small|medium|large|huge|gargantuan`) y de ahí a
   `ships/default`; si no hay ninguno, silencio. El disparo suena con el **arma
   más potente a bordo** de quien dispara (Morgan's > Military > Beam > Pulse >
   Quantum > Photon). La música se **re-evalúa en cada parte** según la tensión
   (piratas, policía y cazas de misión → `TENSE`; comerciantes y encuentros
   raros → `CALM`), con crossfade cuando cambia y `NONE` al cerrar; huir con
   éxito usa `travel/escape` (el salto del mapa conserva `travel/warp`).
6. **Autoría y empaquetado**: `tools/convert-sounds.sh` prepara una descarga
   (recorta silencios, aplica fades, normaliza y convierte con `ffmpeg`) y la
   escribe en los resources del módulo; el jar la lleva tal cual, igual que los
   zips de release y el snap (que conserva el plug `audio-playback` y
   `libasound2`). Las claves y la licencia de cada WAV se anotan en
   `docs/developer/sounds.md`.
7. **`SynthesizedSound` se elimina** con sus tests; el patrón de fábrica y el
   seam de `LineOpener` se mantienen en el motor de muestras.

## Consecuencias

- El sonido es **contenido de autoría**: el autor convierte los WAV con
  `tools/convert-sounds.sh` y viajan en el jar; el jugador no instala ni gestiona
  ficheros, y cambiar un sonido pide recompilar el paquete.
- Todo es opcional: sin ficheros el juego arranca en silencio y ningún test
  abre un dispositivo (el `LineOpener` inyectable cubre el fallback y el mute).
- La fase B (motores por nave, cañones por la mejor arma a bordo y música por
  tensión) queda cableada en la escena de encuentros; los volúmenes por canal
  siguen pendientes de exponerse en el panel de opciones.
- El jar crece con los WAV del autor; el paquete `jlink` no cambia (`javax.sound`
  ya viajaba en `java.desktop`) y el snap tampoco (el jar lo lleva dentro).

## Alternativas descartadas

- **Seguir sintetizando en código (ADR 0006).** Se implementó el beeper de 1 bit
  y el autor lo descartó al oírlo: el chiptune sintético no era el sonido que
  quería.
- **La carpeta externa `sounds/` junto a los lanzadores.** Se implementó primero
  (resolución de rutas, copia en el paquete y en el snap, `sounds/raw/` ignorada)
  y el autor la descartó: los sonidos deben viajar en el jar y el jugador no
  tiene que gestionar ficheros.
- **Cargar todo el catálogo al arranque.** Con muchos samples castiga el
  arranque; el resolutor carga cada clave la primera vez que suena.
- **Un motor externo (OpenAL, LWJGL).** `javax.sound` basta para PCM y el
  proyecto no gana una dependencia nueva.
- **Volúmenes solo por sonido.** Con loops conviviendo con efectos hace falta
  mezclar por canal; los volúmenes por canal ya están en el mezclador.
