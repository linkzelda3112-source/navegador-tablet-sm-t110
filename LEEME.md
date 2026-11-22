# Anime Striker ⚽

Juego de fútbol 6 vs 6 para Android con gráficos estilo anime (tinta, cel-shading, ojos grandes,
pelo en picos, líneas de velocidad y cortes de cámara tipo manga). Todo se dibuja por código:
sin imágenes, sin sonidos, sin librerías externas. Equipos y jugadores 100 % originales.

## Controles (horizontal)
- **Izquierda**: joystick flotante (toca donde quieras).
- **TIRO** (mantén para cargar potencia, suelta para disparar). Sin balón: **ENTRADA**.
- **PASE** (apunta con el joystick). Sin balón: **CAMBIO** de jugador.
- **LARGO**: pase elevado. Sin balón: **BARRIDA**.
- **CORRER**: mantén para esprintar (gasta resistencia).
- **★ ESPECIAL**: se carga con buenos pases, regates, entradas y paradas. Al llenarse, ¡tiro especial con corte de cámara!

## Modos
- **Amistoso**: eliges tu equipo y rival.
- **Copa Anime**: semifinal + final (empate = gol de oro y, si sigue, penales).
- Ajustes: dificultad, duración (1:30 / 2:00 / 3:00 por tiempo), efectos y música.

## Compilar el APK
Sube esta carpeta a un repositorio de GitHub (rama `main`). El workflow
`.github/workflows/build-apk.yml` compila y publica `AnimeStriker.apk` en *Releases*
(y como artefacto de la ejecución). Requiere Android 7.0+ (minSdk 24).

Local: `gradle assembleDebug` (JDK 17 + Android SDK).

## Pruebas sin Android
`tools/SimMain.java` simula partidos CPU vs CPU para validar la lógica; `tools/Preview*.java`
renderizan capturas con un shim de Java2D (solo para desarrollo).
