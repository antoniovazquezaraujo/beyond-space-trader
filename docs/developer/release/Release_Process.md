# 🚀 Proceso de Release (Beyond Space Trader)

Este documento describe cómo se publican los ejecutables del juego (Linux y Windows) a partir del empaquetado con `jlink` y GitHub Actions.

---

## 1. Empaquetado con `jlink`

El módulo `BeyondSpaceTraderJava` genera, con `mvn package`, una aplicación autónoma en `output/BeyondSpaceTrader/`:

```
BeyondSpaceTrader/
├── bin/
│   ├── beyond-space-trader.sh    # lanzador Linux/macOS
│   └── beyond-space-trader.bat   # lanzador Windows
├── lib/
│   ├── app/beyond-space-trader-<versión>.jar   # jar sombreado con Lanterna
│   └── runtime/                                # JRE recortado por jlink
├── ships/                        # arte de las naves (chassis.txt, pieces.txt, ships.txt)
└── README.txt
```

- `jlink` crea el runtime solo con los módulos que necesita el juego (`java.base`, `java.desktop` y `java.logging`, calculados con `jdeps --multi-release 17 --print-module-deps` sobre el jar sombreado), así que **el jugador no necesita tener Java instalado**.
- Los lanzadores hacen `cd` a la carpeta del paquete antes de arrancar, porque el juego lee `ships/` relativo al directorio de trabajo.
- `mvn clean` borra también `output/`, y cada `mvn package` vuelve a montar el paquete completo.

Para probarlo en local:

```bash
mvn clean package -DskipTests
./output/BeyondSpaceTrader/bin/beyond-space-trader.sh
```

---

## 2. Publicación de ejecutables (Linux y Windows)

Como `jlink` solo puede crear un runtime para el sistema en el que se ejecuta, la tarea se delega a GitHub Actions (`.github/workflows/release.yml`). GitHub arranca máquinas Ubuntu y Windows en la nube, empaqueta en cada una y adjunta los dos zips a la Release.

### Pasos para sacar una nueva versión

**Paso 1: Integra los cambios de `develop` en `main`**

Crea un PR de `develop` a `main` (la rama de release, protegida) y fusiónalo tras validar que la CI (`mvn -B -ntp -Pquality verify`) pasa.

**Paso 2: Actualiza tu rama `main` local**

```bash
git checkout main
git pull origin main
```

**Paso 3: Crea la etiqueta (tag) con el número de versión**

> OJO: es obligatorio que el nombre empiece por una `v` minúscula para que el workflow se dispare.

```bash
git tag v0.1.0
```

**Paso 4: Sube la etiqueta a GitHub**

```bash
git push origin v0.1.0
```

### ¿Qué ocurre entonces?

1. GitHub Actions lanza el workflow `release` en Ubuntu y Windows en paralelo.
2. Cada máquina ejecuta `mvn -B -ntp clean package -DskipTests` (los tests ya han corrido en el PR; el empaquetado no los necesita).
3. Se comprime `output/BeyondSpaceTrader` en `BeyondSpaceTrader-Linux.zip` y `BeyondSpaceTrader-Windows.zip`.
4. `softprops/action-gh-release` crea la Release (con notas generadas automáticamente) y adjunta los dos zips.

### Ejecución manual

El workflow también se puede lanzar a mano desde la pestaña **Actions** (`workflow_dispatch`). En ese caso compila y comprime en ambos sistemas, pero **no** publica nada: el asset solo se sube cuando el disparo es un tag `v*`. Es útil para comprobar que el empaquetado sigue funcionando sin crear una Release.

---

## 3. Cómo ejecuta el jugador el juego

Solo tiene que descargar el zip de su sistema, descomprimirlo y lanzar:

- **Linux/macOS:** `./bin/beyond-space-trader.sh`
- **Windows:** `bin\beyond-space-trader.bat`

Los argumentos se pasan al juego, por ejemplo para forzar el idioma: `./bin/beyond-space-trader.sh --lang es`.
