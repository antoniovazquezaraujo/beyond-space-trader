# ADR 0003 — El encuentro como escena: el juego arbitra, la escena cuenta

- **Estado**: propuesto
- **Fecha**: 2026-09-30
- **Referencias**: [ADR 0002](0002-letras-como-datos.md), `docs/ships.md`,
  `spacetrader/Encounter.java` (las reglas), `LanternaEncounterView` (la vista
  actual)

## Contexto

Hoy el encuentro es un formulario: un texto, una lista de acciones y sus teclas.
Toda la lógica ya está en `Encounter`: las rondas (una por segundo, con
ataque/huida continuos), el acierto o el fallo (habilidades, huida, sistema de
puntería), el daño (escudos uno a uno y después el casco), los disruptores que
inutilizan los sistemas, la destrucción, el éxito de la huida, y los comercios y
trueques.

Lo que falta es **verlo**. El autor quiere acción, no contestar preguntas. Con
el ADR 0002 las naves ya son rejillas de celdas con colores (`ShipPicture`,
`mirrored`), y la TUI ya anima a ~9-15 fotogramas por segundo (el campo de
estrellas) sin gastar CPU.

## Decisión

**El juego sigue siendo el árbitro y la escena cuenta lo que pasa.** No se
cambian las reglas: se juega lo mismo que el clásico, con otra interfaz.

1. **La escena**: el encuentro pasa a ser una pantalla completa **con el cielo
   estrellado detrás**, y **se elimina el formulario** (no se esconde debajo).
   Las dos naves enfrentadas, la tuya a la izquierda y la suya a la derecha
   (espejada), cada una con su casco y su escudo encima, el registro de texto
   abajo (los textos de siempre) y la barra de acciones con sus teclas.

   ```
   ╔════════════════════════════════════════════════════════════════════╗
   ║ Vapor   casco ██████░░  escudo ███░░░      Pirata  █████░░░  ██░░░░║
   ║                                                                    ║
   ║       ┌∙──∙───■■■─╖⎚.__                                            ║
   ║       [  \⣿⣷   274≡≡≡▒>                            '..`-.          ║
   ║       ╘■■─■■■/-∙☢▒▒▒▄▄▄▀▀\                       .-`'.⎚ ════╡      ║
   ║                ·──→        ←──·                                    ║
   ║                                                                    ║
   ║ El pirata abre fuego y tu escudo aguanta.                          ║
   ║ [A] Atacar · [F] Huir · [B] Sobornar · [R] Rendirse …              ║
   ╚════════════════════════════════════════════════════════════════════╝
   ```

2. **El parte de ronda**: el juego resuelve cada ronda como hoy (una por
   segundo) y la escena recibe un parte: quién disparó, acierto o fallo, el
   daño, si fue al escudo o al casco, y los cambios de estado (inutilizado,
   destruido, huida). La animación **se deriva del parte**, no lo decide: al
   atacar salen los disparos hacia la otra nave (una andanada, un disparo por
   arma) y el parte dice el resultado de la ronda; el daño total es el del
   juego y la escena solo reparte qué proyectiles pasan y cuáles estallan.
   - **Fallo**: los proyectiles pasan de largo y la nave los esquiva (una
     maniobra corta).
   - **Acierto al escudo**: destello del escudo, sin chispas; la barra baja.
   - **Acierto al casco**: el proyectil muere en la tinta y salen chispas, con
     el número de daño.
   - **Inutilizado**: humo y chispas sobre los sistemas.
   - **Destruido**: explosión y la nave queda a la deriva.
   - **Huida**: persecución animada; se gana cuando el juego lo dice (la
     distancia crece hasta perderse de vista).
3. **Las acciones son las del clásico**, con sus teclas, y cada una tiene su
   escena: las de diálogo (comerciar, sobornar, hablar, beber, abordar)
   **pausan** la escena y usan los diálogos actuales; el **registro policial**
   (si lo aceptamos) acerca las naves y pasa un **escáner** por la nuestra; el
   **transvase de mercancía** tiende una **pasarela** entre las dos naves, con
   algo que se mueve por ella (puede ser intermitente); **abordar** acerca las
   naves hasta tocarse.
4. **El automático** (ataque o huida continuos) se muestra con un rótulo; no
   cambia nada de lógica.
5. **Se maneja la nave, decide el juego**: el jugador mueve su nave (subir y
   bajar; girar hacia atrás es emprender la huida) y esas maniobras se ven, pero
   el desenlace lo sigue diciendo el juego: sus veredictos se traducen en cómo
   reacciona la otra nave.
   - **Nos ignora**: no se mueve; podemos cruzar por el hueco que deje.
   - **No nos ignora**: copia la maniobra, más despacio o más rápido según lo
     que diga el juego; despacio, se puede esquivar y colar.
   - **Huimos** (flecha hacia atrás): la otra nave puede seguirnos o no, y
     podemos perderla de vista o no; lo decide el juego.
6. **Encaje**: si las naves no caben en el ancho de su mitad (miden hasta 60
   celdas), primero se recortan los márgenes vacíos del dibujo y, si aún no
   caben, se dibuja una celda de cada dos.
7. **Fuera de alcance por ahora**: el daño de piezas concretas, las reglas
   propias de esquiva (el veredicto sigue siendo del juego), el escaneo
   voluntario (el de la policía sí se ve, porque lo pide el juego), apuntar y
   los minijuegos de abordaje.

## Consecuencias

- La vista de diálogo del encuentro se sustituye por la escena: se reescribe
  `LanternaEncounterView` y sus tests; los textos (`Strings.Encounter…`) se
  reaprovechan como registro.
- El modelo de escena (posiciones, proyectiles, humo y partes) es **lógica
  pura**: se prueba sin GUI, como el resto de `view/`.
- `Encounter`, `EncounterPresenter` y sus tests **no se tocan**: las reglas no
  cambian.
- **Pendiente**: los hitos y el repaso del autor — **M1** la escena quieta
  (estrellas, naves, barras, registro y acciones), **M2** las rondas animadas
  (andanada, impacto, humo, destrucción), **M3** el manejo de la nave
  (esquivar y colar, huir con persecución) y **M4** las escenas de las demás
  acciones (escáner policial, pasarela de mercancía, abordaje).

## Alternativas descartadas

- **Daño de piezas concretas** (que un tiro rompa el motor o un arma): necesita
  reglas nuevas, desequilibra el clásico y el autor lo ha aparcado.
- **Movimiento manual con consecuencias** (esquivar de verdad): lo mismo.
- **Sprites por tipo** (como los antiguos): el arte del autor ya dice la
  silueta y la carga; no hacen falta imágenes por tipo.
- **Seguir con el formulario**: mata la acción que pide el autor.
