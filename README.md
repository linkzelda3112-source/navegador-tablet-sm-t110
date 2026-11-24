# Mate Hub

Hub de minijuegos de matemáticas para Android: sumas, restas, multiplicación y división.

| Juego | Operaciones |
|---|---|
| Globos Locos | suma y resta |
| Lluvia de Meteoritos | multiplicación |
| Batalla de Monstruos | división |
| Parejas | las cuatro (memoria) |
| Gran Carrera | las cuatro (contra 3 rivales) |

Tres dificultades (Fácil, Medio, Difícil), estrellas por juego, XP y niveles. El progreso se guarda en el teléfono.

## Obtener el APK

1. Sube esta carpeta a un repo de GitHub (rama `mate-hub`).
2. Abre la pestaña **Actions**; el flujo *Build APK* compila solo (o ejecútalo con *Run workflow*).
3. Descarga `MateHub.apk` desde **Releases** o desde los *Artifacts* de la ejecución e instálalo.

Compilar en local: `./gradlew assembleRelease` (necesita JDK 17 y el SDK de Android 34).

## Estructura

- `app/src/main/assets/` — todo el juego (HTML, CSS y JS sin dependencias).
- `app/src/main/java/com/matehub/app/MainActivity.java` — WebView a pantalla completa, botón atrás y pausa al salir de la app.
- `tests/logic.test.js` — pruebas de la lógica (`node tests/logic.test.js`).
- `matehub.keystore` — firma fija para que las versiones nuevas se instalen sobre las anteriores.
