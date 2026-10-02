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
   abajo (los textos de siempre). **Sin botones**: no hay formulario ni lista
   que pulsar, pero la última fila de la escena muestra las acciones
   disponibles con su tecla (`[A] Atacar`, `[F] Huir`, ...), en el orden del
   enum salvo `[X] Interrumpir`, que va al final por ser un control (no una
   decisión) y es el primero que cae si el sitio no da para todo, mientras se
   decide; cuando la escena espera (un resultado que leer), esa fila es para
   `[ENTER] continue`. Se juega con la nave.

   La **cabecera del juego** (nombre, día, créditos, deuda, combustible, casco,
   escudos, carga y registro policial) **queda visible arriba** durante el
   encuentro, con los valores al día (el casco y los escudos cambian en
   combate): la escena la dibuja sobre el cielo, con su línea separadora, y
   arranca debajo, desplazando las barras, la leyenda, las naves y el registro.

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
   ║ [A] Atacar  [F] Huir  [S] Rendirse                                 ║
   ╚════════════════════════════════════════════════════════════════════╝
   ```

2. **El parte de ronda**: el juego resuelve cada ronda como hoy (una por
   segundo) y la escena recibe un parte: quién disparó, acierto o fallo, el
   daño, si fue al escudo o al casco, y los cambios de estado (inutilizado,
   destruido, huida). La animación **se deriva del parte**, no lo decide: cada
   ronda es **un disparo**, uno a uno con el mecanismo del juego (una sola
   resolución por ronda), y el daño, si acierta, es el del juego.
   - **Fallo**: el disparo pasa de largo y la nave lo esquiva (una maniobra
     corta).
   - **Acierto al escudo**: destello del escudo, sin chispas; la barra baja.
   - **Acierto al casco**: el proyectil muere en la tinta y salen chispas, con
     el número de daño.
   - **Inutilizado**: humo y chispas sobre los sistemas.
   - **Destruido**: explosión y la nave queda a la deriva.
   - **Huida**: persecución animada; se gana cuando el juego lo dice (la
     distancia crece hasta perderse de vista).
3. **Los mandos: se juega con la nave, sin botones.** No hay formularios que
   contestar (la última fila recuerda las acciones ofrecidas y su tecla):
   - `espacio` **dispara** (una ronda por disparo; repetir es el ataque
     continuo del clásico).
   - `↑` / `↓` **esquivan**: subir o bajar, y la otra nave responde según el
     veredicto del juego.
   - `←` **huye** (te alejas; el juego decide si te sigue o te pierde de
     vista); `→` **acerca** la nave a la otra. `f` huye igual, y huir gira la
     nave: se la ve encarada hacia atrás, también cuando el juego da la huida
     por buena.
   - La **barra de abajo** solo recuerda lo que se puede hacer: las acciones
     que el juego ofrece en ese momento y su tecla (`[A]`, `[F]`, `[S]`…, con
     `[X]` al final: es un control, el primero en caer si falta sitio), sin
     añadir botones ni cambiar los mandos.
   - Las decisiones del clásico se toman **volando**: acercarse es aceptar (el
     **registro policial**, con su escáner; la oferta del comerciante, con su
     **pasarela**; el encuentro con un capitán; el **abordaje** de una nave
     inutilizada, hasta tocarse); alejarse es negarse o ignorar.
   - Las rendiciones (rendirse, someterse, ceder la carga) se hacen **quedándose
     quieto**, sin disparar ni moverse, cuando el juego lo exija.
   - El comercio y el transvase de carga abren sus pantallas de siempre; no son
     botones del combate.
4. **El automático** (ataque o huida continuos) se muestra con un rótulo; no
   cambia nada de lógica.
5. **Se maneja la nave, decide el juego**: el jugador mueve su nave (subir y
   bajar; girar hacia atrás es emprender la huida) y esas maniobras se ven, pero
   el desenlace lo sigue diciendo el juego: sus veredictos se traducen en cómo
   reacciona la otra nave.
   - **Nos ignora**: cruza la escena con rapidez, se aparta con antelación a
     una banda libre (arriba o abajo, la contraria a la nuestra) y sale por el
     otro lado; podemos cruzar por el hueco que deje. Cuando termina de cruzar
     y desaparece, el encuentro **se cierra solo** (equivale a ignorar); si la
     atacamos mientras cruza, vuelve y el combate sigue. **Salvo que la nave
     no nos vea** (vamos ocultos) y quede una decisión pendiente (atacar,
     huir o rendirse): entonces la escena espera al jugador aunque la nave se
     largue.
   - **No nos ignora**: copia la maniobra, más despacio o más rápido según lo
     que diga el juego; despacio, se puede esquivar y colar.
   - **Se rinde** (una nave rendida, o la policía conminándonos a rendirnos):
     **no se va**; se queda enfrente, en su mitad, copiando nuestras
     maniobras, y la escena espera nuestra decisión (atacarla, saquearla,
     rendirnos), que es lo que ofrecen sus acciones.
   - **Nos rendimos ante un pirata**: la escena muestra el saqueo — la nave
     pirata se arrima, sale la pasarela y cruzan las cajas **si de verdad se
     llevan carga** (sin carga robada, ni pasarela) — y después la escena
     espera al jugador, como tras comerciar o una inspección policial. La
     rendición ante la Mantis (se entrega el artefacto) y la policial (el
     arresto) no son un saqueo.
   - **Huimos** (flecha hacia atrás): la otra nave puede seguirnos o no, y
     podemos perderla de vista o no; lo decide el juego. La cámara nos sigue,
     así que nuestra nave **nunca sale de pantalla**: se queda pegada al borde
     mientras huimos, y es el rival quien sale de cámara cuando lo
     despistamos. Si sigue pegándonos, se acerca un poco en cada ronda.
6. **Encaje**: si las naves no caben en el ancho de su mitad (miden hasta 60
   celdas), primero se recortan los márgenes vacíos del dibujo y, si aún no
   caben, se dibuja una celda de cada dos.
7. **Fuera de alcance por ahora**: el daño de piezas concretas, las reglas
   propias de esquiva (el veredicto sigue siendo del juego), el escaneo
   voluntario (el de la policía sí se ve, porque lo pide el juego), apuntar y
   los minijuegos de abordaje.
8. **Las reglas del clásico, sin redes**: el juego se queda como era, también
   en lo malo: sin combustible, sin dinero y sin nada que vender puedes
   quedarte atrapado, igual que en el original. Cualquier red de seguridad
   (fiar combustible, un préstamo de emergencia…) sería un ADR aparte, si el
   autor lo pide.

## Consecuencias

- La vista de diálogo del encuentro se sustituye por la escena: se reescribe
  `LanternaEncounterView` y sus tests; los textos (`Strings.Encounter…`) se
  reaprovechan como registro.
- El modelo de escena (posiciones, proyectiles, humo y partes) es **lógica
  pura**: se prueba sin GUI, como el resto de `view/`.
- `Encounter` y sus reglas no se tocan; `EncounterPresenter` solo **traduce el
  resultado del juego a la escena** (y sus tests cubren esa traducción).
- **Pendiente**: los hitos y el repaso del autor — **M1** la escena (estrellas,
  naves, barras y registro; **sin botones**), **M2** los mandos y las rondas
  animadas (espacio dispara; las flechas mueven y esquivan; el disparo, el
  impacto, el humo y la destrucción), **M3** las reacciones del rival (ignorar
  o copiar, con su retardo) y la huida con persecución, y **M4** las escenas de
  las decisiones (escáner policial, pasarela, abordaje).

## Alternativas descartadas

- **Daño de piezas concretas** (que un tiro rompa el motor o un arma): necesita
  reglas nuevas, desequilibra el clásico y el autor lo ha aparcado.
- **Movimiento manual con consecuencias** (esquivar de verdad): lo mismo.
- **Sprites por tipo** (como los antiguos): el arte del autor ya dice la
  silueta y la carga; no hacen falta imágenes por tipo.
- **Seguir con el formulario**: mata la acción que pide el autor.
