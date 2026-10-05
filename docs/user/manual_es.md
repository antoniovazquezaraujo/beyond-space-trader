
[🇬🇧 Read in English](manual.md)

# Beyond Space Trader — Manual de Usuario

Bienvenido al manual oficial de **Beyond Space Trader**. Aquí encontrarás todo lo
necesario para instalar el juego, volar entre las estrellas, comerciar, combatir,
terminar las misiones y jubilarte como un comandante rico.

## 1. Qué es Beyond Space Trader

Beyond Space Trader es un remake para terminal del clásico **Space Trader** (Palm
OS, 2002), construido con [Lanterna](https://github.com/mabe02/lanterna). Empiezas
con una nave pequeña y 1.000 créditos, y te abres camino como comerciante,
cazarrecompensas, pirata o como te convenga hasta llegar a la luna de Utopia.

El juego se maneja por completo con el teclado: una sola ventana, el mapa
estelar en el centro, un panel contextual a la derecha y una línea de teclas
abajo. Todos los paneles se describen en este manual, y la
[chuleta](cheatsheet_es.md) tiene las teclas de un vistazo.

## 2. Instalación y ejecución

### 2.1 Desde una release (sin Java)

1. Abre la
   [última release](https://github.com/antoniovazquezaraujo/beyond-space-trader/releases/latest)
   y descarga el paquete de tu sistema:

   | Sistema | Fichero |
   | --- | --- |
   | Linux/macOS | `BeyondSpaceTrader-Linux.zip` |
   | Windows | `BeyondSpaceTrader-Windows.zip` |

2. Descomprímelo donde quieras. El paquete lleva **su propio runtime de Java
   recortado** (`lib/runtime`), así que **no necesitas tener Java instalado**.
3. Lanza el juego desde la carpeta descomprimida:

   - **Linux/macOS:** `./bin/beyond-space-trader.sh`
   - **Windows:** `bin\beyond-space-trader.bat`

Los lanzadores cambian a la carpeta del paquete antes de arrancar, porque el
juego lee el arte de las naves (`ships/`) del directorio de trabajo. El paquete
es:

```
BeyondSpaceTrader/
├── bin/      los lanzadores
├── lib/      el jar del juego y el JRE privado
├── ships/    el arte editable de las naves (chassis, pieces y ships)
├── LICENSE, NOTICE, README.txt
```

El juego crea sus propias carpetas en el primer arranque, junto a los
lanzadores: `save/` (partidas guardadas y autoguardados), `data/` (récords y
valores por defecto) y `custom/` (plantillas de naves).

Para jugar en español, la única traducción incluida hoy, pasa el argumento
`--lang`:

```bash
./bin/beyond-space-trader.sh --lang es          # Linux/macOS
bin\beyond-space-trader.bat --lang es           # Windows
```

`--lang` también acepta región (`--lang es_ES`, `--lang es-ES`) y la forma
`--lang=es`. Por defecto se usa el idioma del sistema; los textos sin traducción
caen al inglés.

### Otras formas de instalar

- **Snap Store (Linux):** el juego también se publica como snap:

  ```bash
  sudo snap install beyond-space-trader-tui
  ```

  Los snaps se actualizan solos. El snap ejecuta el juego desde su propia
  carpeta de datos (`~/snap/beyond-space-trader-tui/current/game`), donde viven
  `save/`, `data/` y una copia editable de `ships/`.

- **itch.io:** los mismos paquetes de Linux y Windows están en
  <https://avaraujo.itch.io/beyond-space-trader-tui>.

### 2.2 Desde el código fuente

Requiere **JDK 17** y **Maven 3.9+**:

```bash
./run.sh              # compila y arranca la interfaz de terminal
./run.sh --lang es    # ... en español
mvn package           # compila el jar
mvn -Pquality verify  # tests y análisis estático
```

`run-fixed-font.sh` es un **ayudante de desarrollo**: compila el juego y lo
arranca en kitty, xfce4-terminal o alacritty con una fuente monoespaciada fija
(DejaVu Sans Mono 12), para que los mapas conserven sus proporciones sea cual sea
la fuente del escritorio. No hace falta para jugar a los paquetes publicados.

## 3. Primeros pasos

### 3.1 La pantalla de título

El juego se abre en la pantalla de título, con la portada del juego (o el logo de
texto cuando la terminal es pequeña). Cualquier tecla entra en el programa; desde
ahí:

| Tecla | Acción |
| --- | --- |
| `F2` | Partida nueva |
| `F9` | Cargar una partida guardada |
| `F3` | Récords |
| `F8` | Opciones |
| `A` | Acerca de (origen, autores, licencia) |
| `F10` | Menú |
| `Esc` | Salir |

### 3.2 Un comandante nuevo

`F2` pide cuatro cosas:

1. **Nombre** (Antonio por defecto; pulsa INTRO para dejarlo).
2. **Dificultad**: Principiante, Fácil, Normal, Difícil o Imposible. Cambia las
   probabilidades de encuentro, la fracción de daño y las condiciones de
   partida; Principiante es la única en la que huir siempre sale bien.
3. **Puntos de habilidad**: repartes **16 puntos extra** entre **Piloto**
   (esquiva e iniciativa), **Combatiente** (puntería y daño), **Comerciante**
   (precios) e **Ingeniero** (reparaciones y reducción de daño). Cada habilidad
   empieza en 1 y ninguna puede pasar de 10, así que teclea cuántos puntos extra
   recibe cada una.
4. La partida empieza: una **Gnat** con un **láser de pulso** y **1.000
   créditos**, en un sistema aleatorio de tecnología media con al menos tres
   sistemas a distancia de combustible.

Puedes abandonar la partida y empezar otra en cualquier momento con `F2` (el
juego pide confirmación si hay una partida en curso). Guarda con `F5` y carga
con `F9`; el panel que abre `F5`/`F9` es un explorador de ficheros sobre la
carpeta `save/`.

### 3.3 La pantalla

```
+--------------------------------------------------------------------------+
| Antonio · Día 12 · 12.345 cr · Deuda 0                                   |
| Combustible 14/15 · Casco 25/25 · Escudos 0/0 · Carga 3/10 · Policía: Limpio |
+-------------------------------------+------------------------------------+
|                                     | Acamar · T6 · Democracia           |
|              MAPA                   | Agua   30/ 35   compra/venta       |
|        (local / galáctico)          | Pieles 250/265  compra/venta       |
|                                     |                                    |
+-------------------------------------+------------------------------------+
| Último mensaje                                                           |
| [TAB] mapa · [C] comercio · ... · [F10] menú                             |
+--------------------------------------------------------------------------+
```

- **Cabecera (arriba):** tu nombre, el día, tu dinero, tu deuda, y el
  combustible, casco, escudos, bodegas e historial policial de tu nave. Ocupa
  una línea cuando todo cabe y dos cuando no.
- **Mapa (centro):** el mapa de corto alcance (con los nombres de los sistemas)
  o el mapa galáctico (toda la galaxia, sin nombres). `TAB` alterna entre ambos.
- **Panel (derecha):** el panel contextual: datos de navegación del sistema
  actual, la tabla de comercio, el banco, el astillero, las misiones... `Esc`
  (y `Espacio` en los paneles de solo lectura) lo cierra.
- **Pie:** los últimos mensajes y las teclas disponibles ahora mismo. El pie de
  navegación lista las teclas diarias más las acciones disponibles en el sistema
  actual.

El panel contextual usa su propio ancho y nunca tapa el mapa en una terminal
ancha; por debajo de 120 columnas, un panel de listas ocupa todo el ancho hasta
que se cierra.

### 3.4 Tamaño de la terminal

El juego necesita una terminal de **al menos 60×15** (por debajo muestra
*Ventana demasiado pequeña*). **Se recomienda 100×30 o más**, y **120×30** hace
que la escena de encuentro quepa cómodamente. Si la galaxia se ve estirada,
cambia la opción *Columnas por sector en el mapa galáctico* (`F8`) entre 1, 2 y 3
para ajustarla a tu fuente.

## 4. Volar

### 4.1 El mapa y el objetivo

El mapa es el eje del juego. Usa las **flechas** (o `h`, `j`, `k`, `l`) para
seleccionar el siguiente sistema en esa dirección; `TAB` alterna entre el mapa de
corto alcance (1:1, con nombres) y el mapa galáctico (toda la galaxia, con
marcadores y el círculo de combustible). El mapa muestra los estados de un
vistazo:

- Un **anillo braille verde** alrededor de tu sistema marca tu alcance real de
  salto (tu combustible actual); los sistemas dentro de él salen en verde.
- El sistema **seleccionado** lleva paréntesis `(•)`, el sistema **seguido**
  corchetes `[•]` (anidados `[(•)]` cuando se dan los dos) y el sistema actual se
  dibuja invertido. Un sistema con **agujero de gusano** es un punto rodeado
  `◉`, y el enlace entre un agujero y su pareja se dibuja como una L magenta.
- Los sistemas no visitados son brillantes; los visitados, apagados.

| Tecla | Acción |
| --- | --- |
| Flechas / `hjkl` | Seleccionar el siguiente sistema en esa dirección |
| `TAB` | Mapa de corto alcance / mapa galáctico |
| `INTRO` o `T` | Seguir el sistema seleccionado (`T` otra vez deja de seguirlo) |
| `/` | Buscar un sistema por nombre y seleccionarlo (pregunta cuál si hay varios) |
| `Espacio` | Viajar al sistema seleccionado (o por el agujero de gusano; ver abajo) |
| `G` | Saltar con la Singularidad Portátil |

Al seleccionar un sistema, el panel de navegación muestra sus datos (tecnología,
política, recurso, policía y piratas), su distancia y sus precios estándar.
Seguir un sistema muestra su distancia en el pie y dibuja su alcance en el mapa
mientras la opción *Mostrar el alcance hasta el sistema seguido* está activa.

### 4.2 Viajar

`Espacio` viaja al sistema seleccionado. Un viaje:

- **cuesta combustible** igual a la distancia en pársecs (el panel de navegación
  avisa `Ese sistema está fuera de alcance` cuando no llegas), y
- **ocupa un día**, durante el cual el juego resuelve el trayecto en 20 clics y
  **puede ocurrir un encuentro** (ver sección 6).

Un **agujero de gusano** es un atajo entre dos sistemas lejanos. Cuando el
cursor está en el sistema **actual** y este tiene un agujero de gusano (el panel
de navegación dice `Agujero de gusano a <sistema>`), `Espacio` cruza a su pareja:
**no gasta combustible**, pero sigue ocupando un día. Los agujeros se dibujan en
el mapa y se anuncian en las noticias.

La **Singularidad Portátil** (`G`) es una recompensa de misión de un solo uso: te
transporta **al instante a cualquier sistema seleccionado**, sin gastar
combustible ni días. Solo funciona mientras la llevas a bordo y se consume tras
el salto.

Combustible y reparaciones:

| Tecla | Acción |
| --- | --- |
| `F` | Comprar combustible (pregunta cuánto; limitado por tu dinero y el depósito) |
| `R` | Reparar el casco (pregunta cuánto; limitado por tu dinero) |
| `F8` | Opciones; *Llenar el depósito al llegar* y *Reparar el casco por completo al llegar* lo hacen automáticamente |

### 4.3 Las teclas diarias

La línea inferior de la pantalla de navegación muestra las teclas directas.
Además de las vistas en esta sección: `I` comandante, `V` nave y carga, `C`
comercio, `B` banco, `Q` misiones, `N` noticias, `P` tripulación, `S` naves en
venta, `E` equipo, `D` diseño de nave, `O` cápsula de escape, `A` acerca de,
`/` buscar, `F10` menú. Las acciones que solo tienen sentido en el sistema
actual aparecen en esa misma línea: `[Y] evento`, `[G] salto`, `[S] naves`,
`[E] equipo`, `[D] diseño`, `[O] cápsula`, `[P] tripulación`.

El **evento especial** (`Y`) se describe en la sección 7. La **cápsula de
escape** (`O`) cuesta 2.000 créditos; con una a bordo, perder un combate no es el
final.

## 5. Comercio, la nave y los paneles

### 5.1 Comercio (`C`)

El panel de comercio muestra, para cada artículo, su precio de **compra** y de
**venta** en el sistema actual, tu carga y tu dinero. Cuando hay un sistema
dentro de alcance seleccionado en el mapa, el panel también muestra sus precios
y el margen, para que puedas planear el viaje antes de despegar.

| Tecla | Acción |
| --- | --- |
| `↑` `↓` (o `n` / `p`, `j` / `k`) | Seleccionar un artículo |
| `B` | Comprar (pregunta la cantidad: tecléala y pulsa INTRO, `Esc` cancela) |
| `S` | Vender (pregunta la cantidad) |
| `Mayús+B` / `Mayús+S` | Comprar/vender el máximo sin preguntar |
| `Esc` | Cerrar el panel |

Los paneles del juego usan siempre las mismas teclas de lista: `↑`/`↓`, las
flechas, y `n`/`p` (además de `j`/`k`) para bajar y subir.

### 5.2 Banco (`B`)

Pide un **préstamo** y **devuélvelo**: la deuda crece con los intereses cada día,
así que vigila la cabecera. El banco también vende **seguro**: con él activo,
perder la nave te paga parte de su valor (se pierde si te arrestan).

| Tecla | Acción |
| --- | --- |
| `G` | Pedir un préstamo (pregunta la cantidad) |
| `P` | Devolver deuda (pregunta la cantidad) |
| `I` | Activar o desactivar el seguro |

### 5.3 Naves en venta (`S`) y equipo (`E`)

Ambos paneles necesitan que el nivel tecnológico del sistema sea lo bastante
alto; el pie solo los muestra cuando están disponibles.

**Naves en venta** lista las naves que puedes comprar con su precio y sus
prestaciones, las ranuras de casco/escudo/arma/artilugio y el valor de tasación
de tu nave actual. El tamaño de la nave fija lo que puedes instalar: una nave
mayor tiene más ranuras, más bodegas y más depósitos. `B` compra la nave
seleccionada (pide confirmación): se descuenta la tasación de tu nave, la
tripulación pasa a la nueva y el equipo instalado puede transferirse por una
tarifa cuando hay ranura para él. El equipo especial de las misiones (el láser
de Morgan, el disruptor cuántico, el escudo relámpago, el compactador de
combustible, los compartimentos ocultos) también hay que transferirlo, igual que
la cápsula de escape.

**Equipo** vende armas (láseres y disruptores), escudos y artilugios; también
recompra el equipo viejo.

| Tecla | Acción |
| --- | --- |
| `↑` `↓` (o `n`/`p`) | Seleccionar un artículo |
| `B` | Comprar |
| `S` | Vender |
| `Esc` | Cerrar |

Cada arma ocupa una ranura; los láseres dañan el casco, mientras que los
**disruptores inutilizan** al oponente (fotón y cuántico) y solo arañan el
casco. Los escudos absorben los primeros impactos; los artilugios añaden
habilidades especiales.

### 5.4 Diseño de naves (`D`)

En los sistemas con **astillero** puedes diseñar y construir una nave a medida:
elige el tamaño y una plantilla, ponle nombre y ajusta bodegas, depósitos,
resistencia del casco, ranuras de arma/escudo/artilugio y camarotes. El panel
muestra el precio de cada cambio, la tarifa, la penalización y la tasación de tu
nave actual, con el total final.

| Tecla | Acción |
| --- | --- |
| `↑` `↓` | Seleccionar un campo |
| `←` `→` | Cambiar el valor |
| `R` | Renombrar el diseño |
| `C` | Construir la nave (paga el total) |
| `V` | Guardar el diseño como plantilla |
| `Esc` | Cerrar |

### 5.5 Comandante (`I`), nave y carga (`V`)

El panel del **comandante** muestra tu nombre, habilidades, reputación e
historial policial. El panel de la **nave** muestra el dibujo de la nave, sus
prestaciones, el equipo instalado y la **bodega**, artículo a artículo. Ambos son
de solo lectura y se cierran con `Espacio` o `Esc`.

### 5.6 Tripulación (`P`), misiones (`Q`) y periódico (`N`)

- **Tripulación (`P`)**: contrata o despide **mercenarios**; cada uno tiene
  cuatro habilidades y un sueldo. La tripulación complementa tus propias
  habilidades en los combates (un segundo piloto puede esquivar, un segundo
  ingeniero puede reparar) y el sueldo se paga en cada despegue.
- **Misiones (`Q`)**: las misiones abiertas, un párrafo cada una, con su destino
  marcado bajo el texto. Usa las flechas para elegir una e `INTRO` para fijar su
  sistema como objetivo del mapa y cerrar el panel; `Espacio` también lo cierra.
- **Periódico (`N`)**: el periódico del día, con las noticias que apuntan a los
  eventos especiales y a los sistemas donde pasa algo. Desplázate con las
  flechas (o `AvPág`/`RePág`).

### 5.7 Opciones (`F8`) y menú (`F10`)

El panel de opciones es una lista que cambias con `INTRO`; `S` guarda los
valores actuales como predeterminados y `L` los recupera:

| Opción | Qué hace |
| --- | --- |
| Guardado automático antes y después de cada salto | Guarda `autosave_departure.sav` y `autosave_arrival.sav` en cada viaje |
| Llenar el depósito al llegar | Reposta hasta arriba al aterrizar (cuesta dinero) |
| Reparar el casco por completo al llegar | Repara el casco al aterrizar (cuesta dinero) |
| Pagar siempre el periódico / Mostrar el periódico al llegar | Gestión del periódico |
| Recordar los préstamos | Avisa cuando no estás pagando la deuda |
| Mostrar el alcance hasta el sistema seguido | Dibuja el alcance del sistema seguido en el mapa |
| Dejar de seguir al llegar | Quita el seguimiento al terminar el viaje |
| Reservar dinero para el coste del salto | Deja intacto el coste del despegue al comprar |
| Bodegas que dejar libres al comprar en el sistema | Evita llenar la bodega antes de un despegue |
| Ignorar siempre a los piratas / policía / mercaderes | Salta esos encuentros cuando no tienen nada que ofrecer |
| Ignorar a los mercaderes que comercian | Salta a los mercaderes que solo quieren comerciar |
| Ataque y huida continuos | Ataca o huye ronda tras ronda automáticamente (`X` lo interrumpe) |
| Seguir atacando a la nave que huye | Sigue disparando a una nave que huye |
| Intentar inutilizar a los oponentes cuando sea posible | Usa el disparo para inutilizar en vez de destruir |
| Columnas por sector en el mapa galáctico | 1, 2 o 3 columnas por sector, para ajustarlo a tu fuente |

El **menú (`F10`)** contiene las acciones del programa: récords (`F3`),
opciones (`F8`), guardar (`F5`), cargar (`F9`), partida nueva (`F2`), acerca de y
salir. `Salir` (y `Esc` en el mapa) pide confirmación cuando hay una partida
cargada, para que una salida accidental no pierda el progreso sin guardar.

## 6. Encuentros

Durante un viaje el juego hace tiradas de encuentro: **piratas**, **policía**,
**mercaderes** (incluidos los que comercian en órbita), el rarísimo **Marie
Celeste**, los **capitanes famosos** y las **botellas de tónico**, además de los
encuentros de misión. Cuando ocurre uno, la escena ocupa toda la pantalla: las
dos naves frente a frente (la tuya a la izquierda), la cabecera con los valores
en vivo del combate, el registro abajo y las acciones disponibles con su tecla
en la última fila.

### 6.1 Volar y disparar

| Tecla | Acción |
| --- | --- |
| `Espacio` | **Disparar** una ronda; el oponente responde antes de que puedas volver a disparar |
| `↑` / `↓` (o `k` / `j`) | **Esquivar** hacia arriba o abajo |
| `→` (o `l`) | **Acercarse**: aceptar la oferta (comerciar, el escáner policial, el capitán, abordar un pecio) |
| `←` (o `h`) | **Huir**: girar y alejarse; llegar al borde pide al juego la escapada |
| `INTRO` | La **acción natural** del encuentro (comerciar, someterse a un registro, conocer al capitán, beber, abordar, rendirse...) |
| `X` | **Interrumpir** las rondas automáticas de ataque/huida |

El duelo es ronda a ronda, igual que el juego clásico: tu habilidad de
**Combatiente** contra el **Piloto** del oponente, los escudos absorben primero,
el **Ingeniero** reduce el daño y el casco solo puede perder una fracción por
disparo, así que ningún combate se decide de un solo golpe. Una nave
**inutilizada** es destruida por el siguiente impacto; el casco orgánico del
**Scarab** solo recibe daño de los láseres de pulso, y el **Scorpion** no puede
destruirse (inutilízalo para rescatar a la Princesa).

### 6.2 Las acciones

La escena solo ofrece las acciones que tienen sentido en ese encuentro; la tabla
siguiente es la lista completa y sus teclas:

| Tecla | Acción | Notas |
| --- | --- | --- |
| `A` | **Atacar** | Pide confirmación antes de atacar a un mercader o a un capitán famoso; atacar a la policía arruina tu historial |
| `F` | **Huir** | En Principiante escapas ileso; si no, el oponente dispara gratis y decide el piloto |
| `S` | **Rendirse** | Los piratas te saquean, la policía te arresta, la mantis se lleva el artefacto |
| `B` | **Sobornar** | El registro policial; el precio crece con tu patrimonio y algunos agentes no aceptan sobornos |
| `U` | **Someterse** | Deja que la policía te registre: la carga ilegal se confisca con multa, la limpia mejora tu historial |
| `Y` | **Ceder** | Responde a una exigencia de rendición policial (un objeto especial ilegal significa arresto) |
| `O` | **Abordar** | El Marie Celeste: saquea su bodega (los narcóticos traen una emboscada policial) |
| `P` | **Saquear** | Un pirata o mercader inutilizado o rendido: pasa su carga a tu bodega |
| `M` | **Conocer** | Intercambia entrenamiento por equipo con un capitán famoso |
| `T` | **Comerciar** | Trata con un mercader en órbita: compra y vende a sus precios |
| `D` | **Beber** | La botella de tónico: una buena sube habilidades, una vieja estropea una |
| `I` | **Ignorar** | Deja el encuentro (solo cuando la otra nave no tiene nada que ofrecer) |
| `X` | **Interrumpir** | Detiene las rondas automáticas |

Cuando la escena espera a que leas un resultado (un registro, un saqueo, un
comercio), un bocadillo bajo el rival o una última fila te dice que pulses
`INTRO`; `Esc` también sale de la escena.

### 6.3 Desenlaces

- **Ganas:** la recompensa por los piratas (cuando tu historial está limpio), la
  reputación y los contadores de bajas, y el captador puede ofrecerte un
  contenedor de su bodega; si tienes las bodegas llenas, puedes arrojar carga.
- **Pierdes:** con una **cápsula de escape** despiertas días después en un
  puerto cercano (y sigues con una Flea tras tres días y 500 cr.); sin ella, la
  partida termina.
- **Te arrestan:** juicio, mercancía ilegal confiscada, una multa abultada (o la
  nave vendida), el seguro perdido y una Flea de segunda mano para seguir
  volando.
- **Te rindes ante los piratas:** saquean tu bodega; los compartimentos ocultos
  esconden primero un objeto especial, y si no hay nada que robar te chantajean.

Un **reactor iónico** a bordo te hace recibir más daño; tras la misión del
Scarab, tu **casco endurecido** lo reduce a la mitad.

## 7. Eventos especiales y misiones

Cuando el sistema actual tiene algo que ofrecer, el pie muestra `[Y] evento`.
`Y` abre la oferta y la aplica si confirmas (algunos eventos solo necesitan un
OK). El periódico (`N`) anuncia dónde pasan las cosas: la luna en venta, los
astilleros y las ofertas de misión.

### 7.1 Líneas de misión

| Misión | Cómo empieza | Qué hacer | Recompensa |
| --- | --- | --- | --- |
| **Artefacto alienígena** | Una oferta para entregar un artefacto | Llévaselo al profesor Berger en un sistema de alta tecnología; los alienígenas (mantis) intentarán recuperarlo | 20.000 cr |
| **Dragonfly** | El coronel Jackson te pide cazar una nave experimental robada | Sigue el rastro y destrúyela | Un escudo experimental (relámpago) para tu nave |
| **Experimento** | El aviso del Dr. Lowenstam | Llega a Daled **en diez días** | La **Singularidad Portátil** (un salto a cualquier parte, `G`) |
| **Invasión de Gemulon** | Un mensaje que entregar | Llega a Gemulon **en seis días** | Un **compactador de combustible** (+3 pársecs de alcance) |
| **Antídoto de Japori** | Una llamada de auxilio por una enfermedad | Lleva diez contenedores a Japori (10 bodegas ocupadas) | Dos puntos de habilidad aleatorios |
| **Embajador Jarek** | El embajador necesita transporte | Llévalo a Devidia | Un **ordenador de regateo** (mejores precios) |
| **Reactor** | La peligrosa misión de Henry Morgan | Lleva el reactor inestable a Nix (15 bodegas) | El **láser de Morgan** |
| **Scarab** | La nave robada del capitán Renwick | Destruye el Scarab en la salida de un agujero de gusano | Una **mejora de casco** |
| **Princesa** | El secuestro real | Sigue las pistas hasta Qonos; **inutiliza** (no destruyas) el Scorpion con disruptores y llévala a casa | Un **disruptor cuántico** |
| **Jonathan Wild** | Llevar a un fugitivo de contrabando | Llévalo a Kravat esquivando a la policía | **Historial limpio** y un mercenario gratis |
| **Escultura** | Una entrega extraña | Llévala a Endor | **Compartimentos ocultos** |
| **Monstruo espacial** | Acamar está bajo ataque | Destruye al monstruo | 15.000 cr |
| **Luna** | Las noticias lo anuncian | Compra la luna en Utopia (500.000 cr) y ve allí a reclamarla | La jubilación (ver sección 8) |

### 7.2 Eventos menores

- Tres contenedores precintados por 1.000 créditos (pueden ser robots... o agua).
- Un príncipe mercader ofrece un objeto especial por 1.000 créditos.
- Un hacker limpia tu historial policial por 5.000 créditos.
- Una máquina de aprendizaje rápido sube una habilidad aleatoria por 3.000
  créditos.
- La lotería puede pagarte 1.000 créditos al atracar.
- Un excéntrico multimillonario compra tus tribbles.
- Los encuentros rarísimos: el **Marie Celeste** (saquéalo antes de que la
  policía lo note), los capitanes famosos **Ahab**, **Conrad** y **Huie**
  (entrenamiento por equipo) y las **botellas de tónico** (buenas y viejas).

## 8. Terminar la partida

Hay tres formas de acabar:

- **Jubilarse con la luna:** cómprala en Utopia (500.000 cr, anunciada en las
  noticias) y vuelve allí a reclamarla. Obtienes la pantalla final del juego y tu
  puntuación entra en la tabla de récords (`F3`).
- **Morir en combate:** si tu nave es destruida y no tienes cápsula de escape, la
  partida termina ahí.
- **No es un final:** que te arresten te deja volando una Flea de segunda mano, y
  una cápsula de escape te deja en un puerto cercano. La partida continúa.

La puntuación final combina tu **patrimonio** (nave, dinero y deuda), la
dificultad y los días empleados —jubilarse rápido paga mejor—, y la tabla se
guarda en `data/HighScores.bin` y se ve con `F3`.

## 9. Referencia completa de teclas

### 9.1 El mapa y el programa

| Tecla | Acción |
| --- | --- |
| Flechas / `hjkl` | Mover la selección del mapa |
| `TAB` | Mapa de corto alcance / mapa galáctico |
| `INTRO` / `T` | Seguir el sistema seleccionado (otra vez: dejar de seguirlo) |
| `/` | Buscar un sistema por nombre |
| `Espacio` | Viajar al sistema seleccionado (o por el agujero de gusano del actual) |
| `G` | Salto con la Singularidad Portátil |
| `F` / `R` | Comprar combustible / reparar el casco |
| `Y` | Evento especial del sistema actual |
| `C` | Comercio |
| `B` | Banco |
| `Q` | Misiones |
| `N` | Periódico |
| `P` | Tripulación |
| `I` / `V` | Comandante / nave y carga |
| `S` / `E` / `D` | Naves en venta / equipo / diseño de nave |
| `O` | Comprar una cápsula de escape (2.000 cr) |
| `A` | Acerca de |
| `F2` / `F5` / `F9` | Partida nueva / guardar / cargar |
| `F3` / `F8` / `F10` | Récords / opciones / menú |
| `Esc` | Cerrar el panel; en el mapa, salir (pregunta antes) |
| `n` / `p` | En el mapa: periódico / tripulación; en las listas: bajar/subir |

### 9.2 Los paneles

| Panel | Teclas |
| --- | --- |
| Cualquier panel | `Esc` lo cierra (`Espacio` en los de solo lectura) |
| Listas | `↑`/`↓` (también `n`/`p`, `j`/`k`) |
| Comercio (`C`) | `B` comprar · `S` vender · `Mayús+B`/`Mayús+S` el máximo |
| Banco (`B`) | `G` préstamo · `P` devolver · `I` seguro |
| Misiones (`Q`) | `INTRO` fijar objetivo · `Espacio` cerrar |
| Tripulación (`P`) | `H` contratar/despedir |
| Naves en venta (`S`) | `B` comprar |
| Equipo (`E`) | `B` comprar · `S` vender |
| Diseño (`D`) | `←`/`→` cambiar · `R` nombre · `C` construir · `V` guardar plantilla |
| Opciones (`F8`) | `INTRO` cambiar · `S` guardar por defecto · `L` cargar por defecto |
| Periódico (`N`) | `↑`/`↓` desplazar (también `AvPág`/`RePág`) |
| Transferencia de carga (arrojar/saquear) | `1`-`9`/`0` seleccionar · `Mayús+dígito` todo |

### 9.3 Encuentros

| Tecla | Acción |
| --- | --- |
| `Espacio` | Disparar |
| `↑`/`↓` (o `k`/`j`) | Esquivar |
| `→` (o `l`) | Acercarse |
| `←` (o `h`) | Huir |
| `INTRO` | Acción natural del encuentro / continuar |
| `A` `F` `S` `B` `U` `Y` `O` `P` `M` `T` `D` `I` | Atacar, Huir, Rendirse, Sobornar, Someterse, Ceder, Abordar, Saquear, Conocer, Comerciar, Beber, Ignorar |
| `X` | Interrumpir las rondas automáticas |
| `Esc` | Salir de la escena cuando espera por ti |

## 10. Consejos y solución de problemas

- **Compra barato, vende caro:** el panel de comercio muestra el margen con los
  sistemas a tu alcance; un viaje más largo paga más, pero arriesga más
  encuentros.
- **Vigila el combustible:** el anillo braille verde del mapa es tu alcance real.
  Quedarte sin combustible lejos de un puerto es la forma clásica de atascarte.
- **Mejora casco y escudos antes que las armas.** Un láser de pulso basta una
  temporada; una bodega mayor paga todo lo demás.
- **Inutiliza, no destruyas:** una nave inutilizada intacta se puede saquear, y
  la Princesa necesita que inutilices el Scorpion.
- **El historial policial importa:** un historial limpio da mejores precios y te
  deja cobrar las recompensas por piratas, pero mercaderes y policía te tratan
  de otra manera.
- **Guarda antes de un viaje peligroso** (`F5`) o activa los autoguardados
  (`F8`).

Solución de problemas:

- **«Ventana demasiado pequeña»:** la terminal está por debajo de 60×15.
  Agrándala; 100×30 es cómodo y 120×30 encaja la escena de encuentro.
- **El mapa se ve estirado o aplastado:** ajusta *Columnas por sector en el mapa
  galáctico* (`F8`) a 1, 2 o 3; los mapas asumen una celda el doble de alta que
  de ancha.
- **Cuadros o glifos extraños:** usa una fuente monoespaciada con buena cobertura
  Unicode en la terminal; el juego cae al logo de texto cuando la portada no
  cabe.
- **El juego no encuentra `ships/`:** lánzalo con los lanzadores del paquete
  (fijan el directorio de trabajo), o ejecuta el jar desde la carpeta que
  contiene `ships/`.
- **Partidas:** en `save/`, junto a los lanzadores (los autoguardados son
  `autosave_departure.sav` y `autosave_arrival.sav`). Los récords y valores por
  defecto viven en `data/`, y los diseños propios en `custom/templates`.

## 11. Créditos y licencia

Beyond Space Trader es un remake en Java que continúa el port *SpaceTrader for
Java* de **Space Trader** (Palm OS, 2002) de **Pieter Spronck**, con
ilustraciones de **Alexander Lawrence**. El port de Windows es de **Jay French**
con **David Pierron**; el port de Java es de **Aviv Eyal** y colaboradores.
Consulta el fichero `NOTICE` para la cadena de procedencia completa.

El juego se distribuye bajo la **GNU General Public License v3.0 o posterior**;
consulta el fichero `LICENSE`.
