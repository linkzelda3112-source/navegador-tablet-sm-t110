# Valle Radial

FPS de acción con estética de manga y **gravedad radial**, para Android (OpenGL ES 2.0, Java, sin librerías).
Inspirado en el video de referencia: el mundo es el interior de un tubo gigante, así que las paredes se convierten en piso.

## Qué tiene
- Valle otoñal amarillo con camino que sube por las paredes, casas de techo rojo, árboles, rocas y arbustos con contorno de tinta.
- Gravedad radial: camina hacia un lado y la cámara rota; la pared pasa a ser piso. También puedes cruzar el tubo por el aire.
- Mano derecha con pistola amarilla dibujada a tinta; mano izquierda con **kunai y cuerda** (gancho): lánzalo a una casa, roca, árbol o pared y mantén el botón para jalarte.
- Manchas de tinta enemigas (3 disparos). Barra de vida rosa segmentada en diagonal, abajo a la izquierda.
- Sonidos sintetizados, sin archivos de audio.

## Controles (horizontal)
- Lado izquierdo: stick flotante para moverte.
- Lado derecho: arrastra para mirar.
- FUEGO: pistola. GANCHO: toca para lanzar, mantén para jalar, suelta para soltarte. SALTO.

## Compilar
Cada push a `main` o `valle-radial` compila en GitHub Actions y publica `ValleRadial.apk` en Releases (etiqueta `valle-N`).
Antes de compilar corre `tools/SimTest.java`, una prueba de la física en una JVM normal.

Local: `gradle assembleRelease` (JDK 17 + Android SDK 34).

## Notas
Los modelos, texturas y manos son originales y se generan por código; no se usa ningún recurso del juego original.
