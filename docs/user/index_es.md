
[🇬🇧 Read in English](index.md)

<div style="display: flex; justify-content: center; width: 100%; overflow: hidden; margin-top: 20px; margin-bottom: 20px;">
<pre style="font-size: 1.2em; line-height: 1.2; font-weight: bold; background: transparent; border: none; overflow: hidden; padding: 0; text-align: left;">
 ___ ___ _____
| _ ) __|_   _|
| _ \__ \ | |
|___/___/ |_|
    Beyond
 Space Trader
</pre>
</div>

<p align="center">
  <a href="https://github.com/antoniovazquezaraujo/beyond-space-trader/releases/latest">
    <img src="https://img.shields.io/badge/🎮_Descargar_Última_Versión-0078D4?style=for-the-badge&logo=github&logoColor=white" alt="Descargar Última Versión">
  </a>
  <a href="https://antoniovazquezaraujo.github.io/beyond-space-trader/">
    <img src="https://img.shields.io/badge/📖_Leer_la_Documentación-2EA043?style=for-the-badge&logo=markdown&logoColor=white" alt="Leer la Documentación">
  </a>
</p>

**Beyond Space Trader** es un remake en Java del clásico **Space Trader** de Palm
OS, jugado íntegramente en una terminal. Comercia entre sistemas estelares, mejora
tu nave, acepta misiones, esquiva a piratas y policías, y trata de jubilarte como
un comandante rico.

Toda la interfaz se maneja con el teclado y se dibuja con
[Lanterna](https://github.com/mabe02/lanterna): una sola ventana, paneles a la
derecha y la carta estelar siempre en el centro.

## ✨ Características

- **El juego clásico de Space Trader:** compra barato, vende caro, mejora tu nave
  y sobrevive al viaje, con las reglas del juego original.
- **Interfaz de terminal:** las cartas local y galáctica, los paneles y los
  encuentros se dibujan como texto, con una paleta de unos pocos colores.
- **Encuentros animados:** el combate es una escena con las dos naves frente a
  frente; tú pilotas, esquivas y disparas, y el juego sigue arbitrando cada ronda.
- **Misiones y eventos especiales:** artefactos alienígenas, la Dragonfly, el
  Scarab, la Princesa, la luna en venta en Utopia...
- **Inglés y español:** el juego detecta el idioma del sistema y acepta
  `--lang es`; la documentación también es bilingüe.

## 🚀 Inicio rápido

1. Descarga el zip de tu sistema desde la
   [última release](https://github.com/antoniovazquezaraujo/beyond-space-trader/releases/latest):
   `BeyondSpaceTrader-Linux.zip` o `BeyondSpaceTrader-Windows.zip`.
2. Descomprímelo donde quieras. **No necesitas tener Java instalado**: el paquete
   incluye su propio runtime.
3. Lanza el juego:
   - **Linux/macOS:** `./bin/beyond-space-trader.sh`
   - **Windows:** `bin\beyond-space-trader.bat`

```bash
# ... y para jugar en español:
./bin/beyond-space-trader.sh --lang es
```

Las instrucciones completas, los controles y la solución de problemas están en el
**[Manual de Usuario](manual_es.md)** ([English](manual.md)).

## 📖 Documentación

- [Manual de Usuario](manual_es.md) — instalación, cómo jugar, controles, misiones y consejos.
- [Chuleta rápida](cheatsheet_es.md) — teclas y paneles de un vistazo.
- [Documentación de desarrollo](../developer/README.md) — la arquitectura y las
  guías técnicas (*en inglés*, con los ADR en español).

## ⚖️ Licencia

Beyond Space Trader se distribuye bajo la **GNU General Public License v3.0 o
posterior**; consulta el fichero `LICENSE` para el texto completo y `NOTICE` para
la cadena de procedencia. El juego original, **Space Trader** (Palm OS), fue creado
por Pieter Spronck con ilustraciones de Alexander Lawrence; este proyecto continúa
el port *SpaceTrader for Java* de Aviv Eyal y colaboradores.
