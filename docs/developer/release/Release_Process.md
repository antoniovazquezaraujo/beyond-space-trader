# 🚀 Proceso de Release (Beyond Space Trader)

Este documento describe cómo se publican los ejecutables del juego (Linux y Windows) a partir del empaquetado con `jlink` y GitHub Actions, y cómo se distribuyen además en **itch.io** y la **Snap Store**.

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

## 3. Publicación en itch.io

En cada tag `v*`, después de subir los zips a la Release de GitHub, el workflow `release` publica también los mismos artefactos en itch.io con [butler](https://itch.io/docs/butler/) (`.github/workflows/release.yml`):

- La máquina Linux sube `BeyondSpaceTrader-Linux.zip` al canal `linux` del proyecto `avaraujo/beyond-space-trader`.
- La máquina Windows sube `BeyondSpaceTrader-Windows.zip` al canal `windows`.
- La versión que verá el jugador es el propio nombre de la etiqueta (`github.ref_name`, por ejemplo `v0.1.0`).

### Alta (solo el dueño)

1. Crear el proyecto `beyond-space-trader` en itch.io (cuenta `avaraujo`) con los canales `linux` y `windows`.
2. Generar una API key de butler en **Account settings → API keys** y guardarla como secreto **`BUTLER_API_KEY`** en el repositorio (Settings → Secrets and variables → Actions). El workflow la pasa a butler en la variable de entorno del mismo nombre.

### Flujo

- **Tag `v*`:** publicación en ambos canales de itch.io.
- **Ejecución manual (`workflow_dispatch`):** compila y comprime, pero no publica (la condición exige `refs/tags/`).
- Sin `BUTLER_API_KEY`, el paso de butler falla y el job acaba en rojo, pero la Release de GitHub (con sus zips) ya se ha subido en el paso anterior y **no** se pierde.

---

## 4. Publicación en la Snap Store

El workflow `.github/workflows/snap.yml` construye el snap `beyond-space-trader-tui` y lo publica en la Snap Store:

- **Tag `v*`:** publica en el canal `stable`.
- **Ejecución manual (`workflow_dispatch`):** publica en el canal `edge` (ideal para probar antes de una release).
- **Pull request que toque `snap/**` o el propio workflow:** solo construye y sube el `.snap` como artefacto, sin publicar. Así el PR valida el empaquetado.
- El job corre en `ubuntu-22.04` y usa `snapcraft pack --destructive-mode`; `version: git` necesita `fetch-depth: 0` para calcular la versión a partir de los tags.

El snap:

- `base: core22`, `confinement: strict` y sin `plugs`: el juego es totalmente offline.
- Compila con `mvn -B -ntp clean package -DskipTests` y empaqueta `output/BeyondSpaceTrader/` (`lib/`, `ships/`, `LICENSE` y `NOTICE`).
- Ejecuta el juego desde `$SNAP_USER_DATA/game`, una carpeta de usuario escribible: allí viven `data/` y `save/`, y el arte de `ships/` se copia una sola vez (`cp -n`, así se respetan las ediciones del jugador).
- Pasa a Lanterna la ruta del `stty` que viaja dentro del snap (`-Dcom.googlecode.lanterna.terminal.UnixTerminal.sttyCommand="$SNAP/usr/bin/stty"`), porque bajo confinamiento estricto no puede usar `/bin/stty` del host.

### Alta (solo el dueño)

1. Con una cuenta de Ubuntu One, registrar el nombre: `snapcraft register beyond-space-trader-tui`.
2. Exportar las credenciales de publicación: `snapcraft export-login --snaps=beyond-space-trader-tui --acls=package_access,package_push,package_release snapcraft-creds` (conviene limitarlas a ese snap y reexportarlas si el login expira) y guardarlas como secreto **`SNAPCRAFT_STORE_CREDENTIALS`** en el repositorio.

### Flujo

- **Tag `v*`:** construye el snap y lo publica en `stable`.
- **Ejecución manual:** construye y publica en `edge`.
- Sin `SNAPCRAFT_STORE_CREDENTIALS`, el paso de publicación falla (el workflow solo construye en PRs, donde no hay secretos), pero el resto de publicaciones no se ve afectado.

Una vez publicado, el jugador lo instala con:

```bash
sudo snap install beyond-space-trader-tui
```

---

## 5. Cómo ejecuta el jugador el juego

Solo tiene que descargar el zip de su sistema, descomprimirlo y lanzar:

- **Linux/macOS:** `./bin/beyond-space-trader.sh`
- **Windows:** `bin\beyond-space-trader.bat`

Los argumentos se pasan al juego, por ejemplo para forzar el idioma: `./bin/beyond-space-trader.sh --lang es`.
