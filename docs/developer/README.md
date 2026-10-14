# Beyond Space Trader — Documentación de desarrollo

Bienvenido a la documentación interna de **Beyond Space Trader**. Aquí viven las
guías técnicas, el detalle de los subsistemas y el registro histórico de las
decisiones de arquitectura (ADR).

> 🌐 **Nota de idiomas:**
> La documentación técnica (`ships.md`, `encounters.md`, `ui-design.md`) está
> escrita en **inglés**, el idioma del código y de los commits. Los **ADR** y las
> guías de proceso se escriben en **español**, el idioma de trabajo del autor.
> Para la documentación pública y bilingüe, consulta el
> [manual de usuario](../user/manual.md) ([Español](../user/manual_es.md)).

---

## 🏛️ Arquitectura de un vistazo

El juego es un puerto Java del clásico **Space Trader**, con una interfaz de
terminal construida con **Lanterna** y estructurado en **MVP** (Modelo-Vista-
Presentador):

```
                    ┌──────────────────────────────────────┐
                    │        Modelo (spacetrader.*)        │
                    │  Game, Ship, Trade, UniverseGenerator │
                    │    (sin ninguna dependencia de UI)    │
                    └───────────────────┬──────────────────┘
                                        │
                    ┌───────────────────┴───────────────────┐
                    ▼                                       ▼
       ┌─────────────────────────┐             ┌─────────────────────────┐
       │   Presentadores (MVP)   │             │   Vistas / ViewModels   │
       │ org.gts.bst.presenter   │ ──────────▶ │    org.gts.bst.view     │
       └────────────┬────────────┘             └────────────┬────────────┘
                    │                                       │
                    └───────────────────┬───────────────────┘
                                        ▼
                         ┌─────────────────────────────┐
                         │  UI Lanterna (texto/ANSI)   │
                         │ org.gts.bst.lanterna.*      │
                         └─────────────────────────────┘
```

### Principios de diseño

* **MVP en las pantallas:** los presentadores orquestan el modelo y las vistas
  hablan con ellos a través de interfaces (`*View`) y modelos de vista
  (`*ViewModel`). Todo se prueba sin abrir una ventana.
* **El modelo no conoce la UI:** `spacetrader.*` es la lógica del juego (reglas,
  comercio, encuentros, universo) y depende solo de puertos neutros
  (`org.gts.bst.ports`); no conoce Lanterna ni las vistas.
* **La UI vive en `org.gts.bst.*`:** las interfaces de vista y los view models en
  `org.gts.bst.view`; el renderizado Lanterna en `org.gts.bst.lanterna`.
* **El sonido también es un puerto:** los sonidos se piden con
  `org.gts.bst.ports.SoundService` (`SoundEffect`, `AmbienceKey`, `MusicTheme`)
  y salen de los WAV empaquetados en el jar (`src/main/resources/sounds/`,
  todos opcionales), mezclados por `org.gts.bst.audio.SampledSound`. Sin
  dispositivo de audio el juego arranca en silencio (`SoundService.NONE`), y el
  jugador puede apagarlo en `F8` o con `--mute`. Ver
  [ADR 0007](adr/0007-audio-por-muestras.md) y [sounds.md](sounds.md).
* **Una sola ventana, paneles dentro:** el mapa (carta local/galáctica) es el eje
  de la pantalla y el panel contextual cambia con la actividad (comercio, banco,
  astillero, misiones, periódico, encuentro...).
* **Teclado primero:** cada acción tiene una tecla visible; el ratón es opcional.
  El diseño completo está en [ui-design.md](ui-design.md).

---

## 🧭 Navegar por la documentación

* **[Interfaz y experiencia (`ui-design.md`)](ui-design.md):** la disposición de la
  pantalla, el panel contextual, las teclas y la paleta de colores.
* **[Arte de las naves (`ships.md`)](ships.md):** el formato de `ships/` (chasis,
  piezas y naves), los glifos y colores, y los editores (compositor).
* **[Encuentros (`encounters.md`)](encounters.md):** todo lo que puede pasar en un
  encuentro, acción por acción y regla por regla.
* **[Sonido (`sounds.md`)](sounds.md):** los samples dentro del jar, sus claves y
  variantes, el formato canónico y el conversor.
* **[Decisiones de arquitectura (`adr/`)](adr/README.md):** los ADR numerados
  (`0001`…) que fijan las decisiones de diseño; no se reescriben, se sustituyen.
* **[Proceso de release (`release/Release_Process.md`)](release/Release_Process.md):**
  empaquetado con `jlink`, publicación de los zips de Linux y Windows, itch.io
  y la Snap Store, y cómo lo ejecuta el jugador.

---

## 🗺️ Estado y roadmap

El juego está en desarrollo activo: compila, se juega con la interfaz Lanterna y
el modelo está desacoplado de las vistas.

- [x] Arreglar los problemas de arranque (singleton, look & feel, rutas de recursos)
- [x] Quitar el truco de depuración del constructor de `Game`
- [x] Sustituir el build de NetBeans/Ant por Maven y eliminar JNLP/WebStart
- [x] Refactorizar hacia Model-View-Presenter
- [x] Portar la UI a Lanterna y eliminar el front-end Swing/JWinForms
- [x] Paquete autónomo con `jlink`, releases en GitHub (zips de Linux y Windows), Snap Store e itch.io
- [x] Renderizador de naves: las naves se dibujan con el arte editable de `ships/`
      en las pantallas y en la escena de los encuentros
- [x] Compositor de naves y raíles del editor (los 17 tipos con sus cascos y
      piezas, validados en CI)
- [x] Descomponer `Game` por áreas (#46): Universe (#147), precios y comercio
      (#148), noticias (#149), misiones y eventos (#150), llegada (#151),
      serialización (#152) y viaje/encuentros (#229)

---

## 🛠️ Compilar y probar

Requiere **JDK 17** y **Maven 3.9+**.

```bash
mvn clean package -DskipTests  # compila el paquete autónomo (output/BeyondSpaceTrader)
./run.sh                       # arranca lo compilado (no compila)
./run.sh --lang es             # ... y arranca en español
mvn verify                     # tests
mvn -B -ntp -Pquality verify   # lo que corre la CI: tests + SpotBugs
```

Para generar el paquete autónomo con runtime `jlink` (lo que se publica en las
Releases):

```bash
mvn clean package -DskipTests
./output/BeyondSpaceTrader/bin/beyond-space-trader.sh
```

Para las normas de estilo, ramas y Pull Requests, consulta
**[CONTRIBUTING.md](../../CONTRIBUTING.md)**.
