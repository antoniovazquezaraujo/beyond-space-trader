# ADR 0007 — Audio por muestras: `sounds/`, mezclador y ambiente por pantalla

- **Estado**: aceptado
- **Fecha**: 2026-10-09
- **Referencias**: ADR 0005 (`0005-puertos-del-modelo.md`), ADR 0006
  (`0006-sonido-sintetizado.md`, sustituido), `org/gts/bst/ports/SoundService.java`,
  `org/gts/bst/audio/` (`SampledSound`, `SampleLibrary`, `SampleMixer`),
  `sounds/README.md` y `tools/convert-sounds.sh`

## Contexto

El ADR 0006 apostó por **sintetizar** los efectos en código para no distribuir
ficheros de audio, y se llegó a implementar un beeper de 1 bit. El autor lo oyó
y lo descartó: quiere **sonidos reales**, descargados de bancos como Freesound,
con la misma idea que los samples de LeTrain. La fase A es el motor de muestras,
la carpeta `sounds/` y el ambiente por pantalla; la fase B traerá los motores de
las naves, los cañones por tipo y la música.

Se mantienen del ADR 0006 las decisiones que no dependen del timbre: el puerto
neutro, la reproducción asíncrona que nunca bloquea, el fallback a silencio sin
dispositivo y el mute del jugador. Lo que cambia es de dónde sale el sonido.

## Decisión

1. **Los samples viven en `sounds/`**, en la raíz del repositorio (como `ships/`),
   y se leen **en caliente**: dejar un WAV nuevo solo pide reiniciar, nunca
   recompilar. Las rutas se resuelven con los mismos candidatos que
   `ShipArtFile.resolve` (`sounds/`, `../sounds/`, `BeyondSpaceTraderJava/sounds/`).
   El cargador convierte cada fichero a **16 bits, 44,1 kHz, mono** con
   `AudioSystem.getAudioInputStream(formatoCanónico, ...)`; lo que no se puede
   convertir se ignora con un aviso en consola. Las claves son rutas
   (`combat/hit`, `ambient/trade`) y pueden tener **variantes**
   `-<n>.wav`; el resolutor las agrupa por prefijo y elige una **al azar en cada
   reproducción**. Clave ausente = silencio, nunca un error.
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
5. **Empaquetado**: el `pom.xml` copia `sounds/` al paquete `output/`, el snap
   estricto la copia a la carpeta del jugador con `cp -n` (como `ships/`, para
   que pueda cambiar sus sonidos) y `tools/convert-sounds.sh` prepara una
   descarga (recorta silencios, aplica fades, normaliza y convierte con `ffmpeg`).
   Las descargas crudas viven en `sounds/raw/`, ignorada por git; la autoría y la
   licencia de cada WAV se anotan en `sounds/README.md`.
6. **`SynthesizedSound` se elimina** con sus tests; el patrón de fábrica y el
   seam de `LineOpener` se mantienen en el motor de muestras.

## Consecuencias

- El sonido deja de ser código y pasa a ser **contenido editable**: el autor
  puede probar, cambiar y añadir WAVs sin tocar Java.
- Todo es opcional: sin ficheros el juego arranca en silencio y ningún test
  abre un dispositivo (el `LineOpener` inyectable cubre el fallback y el mute).
- La fase B (motores por nave, cañones por tipo, música por tensión) ya tiene su
  sitio: los métodos del puerto, las claves documentadas y los canales del
  mezclador están listos, pero **no se cablean todavía**.
- El snap crece un poco (los WAV del autor), y el paquete `jlink` no cambia:
  `javax.sound` ya viajaba en `java.desktop`.

## Alternativas descartadas

- **Seguir sintetizando en código (ADR 0006).** Se implementó el beeper de 1 bit
  y el autor lo descartó al oírlo: el chiptune sintético no era el sonido que
  quería.
- **Embeber los WAV en el jar.** El jugador no podría cambiarlos y cada ajuste
  pediría recompilar.
- **Cargar todo el catálogo al arranque.** Con muchos samples castiga el
  arranque; el resolutor carga cada clave la primera vez que suena.
- **Un motor externo (OpenAL, LWJGL).** `javax.sound` basta para PCM y el
  proyecto no gana una dependencia nueva.
- **Volúmenes solo por sonido.** Con loops conviviendo con efectos hace falta
  mezclar por canal; los volúmenes por canal ya están en el mezclador.
