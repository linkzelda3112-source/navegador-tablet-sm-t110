# Letras Mágicas

Juego para Android pensado para una niña de 5.º de primaria a quien le cuesta **escribir bonito** y **leer rápido**.
No hay cronómetros ni castigos: los errores solo dan una pista, y todo se puede escuchar.

| Juego | Qué practica |
|---|---|
| 📖 Cuentos | Lectura guiada a su ritmo (frase enfocada, sílabas de colores, voz que resalta cada palabra) + 3 preguntas de comprensión con pista |
| 🧩 Arma la palabra | Sílabas: juntarlas en orden para formar la palabra del dibujo |
| 🔤 Deletrea | Ortografía sin escribir a mano: tocar las letras en orden |
| ✏️ Trazo mágico | Control del trazo: pintar encima de letras, sílabas y palabras con mucho margen y renglones de cuaderno |
| 🔍 Detective | Letras que se confunden (b/d, p/q), ortografía (b/v, g/j, h, ll, z…) y "oye y encuentra" |
| 🧱 Ordena la oración | Armar frases tocando palabras; puede oír lo que armó |

## Ayudas de lectura y accesibilidad

- Voz en español del propio teléfono (TextToSpeech); velocidad lenta / normal / rápida.
- Tocar una palabra del cuento la dice por sílabas y luego completa.
- Sílabas de colores, "enfocar frase", tamaño de letra (hasta muy grande), 4 colores de fondo.
- Tipografía Comic Relief (licencia OFL, incluida), espaciado amplio.
- Estrellas, niveles y pegatinas para motivar; el progreso se guarda en el teléfono.

## Obtener el APK

El flujo **Build APK** compila al hacer push a la rama `letras-magicas` (o con *Run workflow*) y publica
`LetrasMagicas.apk` en **Releases**. Compilar en local: `./gradlew assembleRelease` (JDK 17 + SDK de Android 34).

## Estructura

- `app/src/main/assets/` — el juego completo (HTML, CSS y JS sin dependencias).
  - `js/logic.js` lógica pura (sílabas, barajar, puntuación del trazo) · `js/data.js` palabras, cuentos y oraciones · `js/games.js` los 6 juegos.
- `app/src/main/java/com/letrasmagicas/app/MainActivity.java` — WebView a pantalla completa + puente a la voz de Android.
- `tests/logic.test.js` — pruebas (`node tests/logic.test.js`): separación en sílabas de las 70 palabras, contenido, trazo, etc.
- `matehub.keystore` — firma fija para que las versiones nuevas se instalen encima de las anteriores.
