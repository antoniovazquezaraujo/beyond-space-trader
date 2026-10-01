# AGENTS — Beyond Space Trader

Perfil del proyecto para los agentes del equipo (Dani, Alex, Bicho, Jorge).
Consulta también [CONTRIBUTING.md](CONTRIBUTING.md) y [README.md](README.md).

## Qué es

Puerto a Java de *Space Trader*, en camino hacia una TUI con **Lanterna**.
Licencia GPL v3. Repo: `github.com/antoniovazquezaraujo/beyond-space-trader`.

## Stack y versiones

- **Java 17 exacto** (`maven.compiler.release=17`; CI con Temurin 17). No usar APIs de 18+.
- **Maven multi-módulo**: padre `pom.xml` + módulo `BeyondSpaceTraderJava`.
- **Lanterna 3.1.5** para la interfaz de terminal.
- **JUnit 5 (Jupiter 5.11.4)**. **No hay Mockito**: usar aserciones planas y fakes manuales.

## Comandos

```sh
mvn package                 # compila y genera el jar
./run.sh                    # compila y arranca la TUI
mvn verify                  # tests
mvn -B -ntp -Pquality verify   # lo que ejecuta CI (tests + SpotBugs)
```

## Tests (reglas importantes)

- Perfil `quality`: **SpotBugs** con `failOnError=true` (esfuerzo Max, umbral Medium).
  Los hallazgos se corrigen o se excluyen explícitamente en `config/spotbugs-exclude.xml`.
- Surefire ejecuta los tests con **locale inglés** (`-Duser.language=en -Duser.country=US`)
  y **`java.awt.headless=true`**: los tests **no deben abrir ventanas** ni depender
  de la configuración regional de la máquina.
- Localización: las cadenas se leen del bundle inglés en tests; no asserts sobre textos
  de otros idiomas.
- Estructura: `src/test/java/spacetrader/*Test.java` (modelo/negocio),
  `org.gts.bst.presenter/*PresenterTest.java` (MVP), `org.gts.bst.view/*Test.java` (UI con
  terminales virtuales de Lanterna).

## Arquitectura

- Patrón **MVP**: `org.gts.bst.presenter` (presenters) + `org.gts.bst.view`
  (interfaces `*View` y `*ViewModel`). El motor de juego vive en `spacetrader.*`
  (puerto del original: `Game`, `Ship`, `Trade`, `UniverseGenerator`…).
- UI **Lanterna** solo en `org.gts.bst.view` / `lanterna`; el modelo no debe
  depender de la UI.
- Documentación técnica en `docs/`: `ui-design.md`, `ships.md`, `encounters.md`.

## Decisiones de arquitectura (ADRs)

En `docs/adr/`, numerados (`0001`, `0002`…) y **nunca se reescriben**: si una
decisión cambia, se añade un ADR nuevo que la sustituye y se marca el viejo como
*sustituido* en su cabecera. Ver `docs/adr/README.md`.

## Proceso (GitHub Flow)

- `main` siempre compila; **no commitear directamente** en él.
- Rama corta por cambio con prefijo: `feature/… fix/… refactor/… docs/… build/… test/…`.
- PRs **pequeños y enfocados** en un solo cambio; CI (`mvn -B verify`) en verde.
- Merge con **squash** y borrar la rama (local y remota) al integrar.
- Commits en modo imperativo, con prefijo Conventional Commits si ayuda
  (`feat:`, `fix:`, `refactor:`, `docs:`, `build:`, `test:`).
- Referenciar la issue en el cuerpo del PR (`Closes #12`).
