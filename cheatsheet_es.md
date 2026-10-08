
[🇬🇧 Read in English](cheatsheet.md)

# Beyond Space Trader — Chuleta rápida

Una página con las teclas y los paneles del juego. Los detalles están en el
[Manual de Usuario](manual_es.md).

## Ejecutar el juego

| | |
| --- | --- |
| Linux/macOS | `./bin/beyond-space-trader.sh` |
| Windows | `bin\beyond-space-trader.bat` |
| En español | añade `--lang es` |
| Desde el código | compila con `mvn clean package -DskipTests` y arranca con `./run.sh` |

## El mapa

| Tecla | Acción |
| --- | --- |
| Flechas / `hjkl` | Seleccionar el siguiente sistema en esa dirección |
| `TAB` | Mapa de corto alcance / mapa galáctico |
| `INTRO` / `T` | Seguir el sistema seleccionado (otra vez: dejar de seguirlo) |
| `/` | Buscar un sistema por nombre |
| `Espacio` | Viajar al sistema seleccionado (o por el agujero de gusano) |
| `G` | Salto con la Singularidad Portátil |
| `F` / `R` | Comprar combustible / reparar el casco |
| `Y` | Evento especial del sistema actual |
| `Esc` | Cerrar el panel; salir desde el mapa |
| `n` / `p` | Periódico / tripulación en el mapa; bajar/subir en las listas |

## Los paneles

| Panel | Teclas |
| --- | --- |
| `C` Comercio | `B` comprar · `S` vender · `Mayús+B`/`Mayús+S` el máximo |
| `B` Banco | `G` préstamo · `P` devolver · `I` seguro |
| `Q` Misiones | `INTRO` fijar objetivo · `Espacio` cerrar |
| `N` Periódico | Flechas para desplazar · `AvPág`/`RePág` |
| `P` Tripulación | `H` contratar/despedir |
| `I` Comandante | `Espacio` cerrar |
| `V` Nave y carga | `Espacio` cerrar |
| `S` Naves en venta | `B` comprar |
| `E` Equipo | `B` comprar · `S` vender |
| `D` Diseño de nave | `←`/`→` cambiar · `R` nombre · `C` construir · `V` guardar |
| `O` Cápsula de escape | 2.000 cr, pide confirmación |
| `F2` / `F5` / `F9` | Partida nueva / guardar / cargar |
| `F3` / `F8` / `F10` | Récords / opciones / menú |
| `A` Acerca de | Origen, autores y licencia |
| Cualquier panel | `Esc` lo cierra (`Espacio` en los de solo lectura) |
| Listas | `↑`/`↓` y también `n`/`p`, `j`/`k` |

Transferencia de carga (arrojar/saquear): `1`-`9`/`0` elige una ranura,
`Mayús+dígito` para todo, `Esc` para cerrar.

## Encuentros

| Tecla | Acción |
| --- | --- |
| `Espacio` | Disparar |
| `↑`/`↓` (o `k`/`j`) | Esquivar |
| `→` (o `l`) | Acercarse (aceptar la oferta) |
| `←` (o `h`) | Huir |
| `INTRO` | Acción natural del encuentro / continuar |
| `A` / `F` / `S` | Atacar / Huir / Rendirse |
| `B` / `U` / `Y` | Sobornar / Someterse / Ceder (policía) |
| `O` / `P` | Abordar (Marie Celeste) / Saquear |
| `M` / `T` / `D` / `I` | Conocer / Comerciar / Beber / Ignorar |
| `X` | Interrumpir las rondas automáticas |
| `Esc` | Salir de la escena cuando espera por ti |

## Lo básico en tres líneas

- **Mapa:** `Espacio` viaja (combustible + un día), `TAB` cambia de mapa, `G`
  salta una vez a cualquier parte.
- **Dinero:** compra barato en `C` y vende caro en `C`; el panel de comercio
  muestra el margen con los sistemas a tu alcance.
- **Final:** compra la luna en Utopia (500.000 cr) y vuelve allí para jubilarte.
