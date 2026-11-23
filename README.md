# Sobrevive un año

Juego Android (vertical, 360×640 estilo PS1 / analog horror) basado frame por frame en el video
"Tienes que sobrevivir un año": eliges un planeta y sobrevives 365 días.

Las cinemáticas reproducen el video (mismos cortes, subtítulos y tiempos, con su audio) y cada una
desemboca en el juego de ese planeta:

| Planeta | Cinemática | Minijuegos (4 por planeta, el año pasa rápido) |
|---|---|---|
| 1 · Las islas | hamaca, remolino, pueblo que desaparece, monstruo de ojos | contar las luces · cerrar los ojos del monstruo · remar contra el remolino · atrapar botellas |
| 2 · Las estrellas | cama, telescopio, cielo que se acerca, gigante con agujero | mantener la mira · cerrar los ojos ante el gigante · unir la constelación · estrellas fugaces |
| 3 · Nadie tiene prisa | plaza, relojes iguales, panadero congelado, pie que no termina el paso | pasos lentos · empujar lo congelado · el reloj distinto · "luz verde/roja" con la multitud que mira |
| 4 · El más tranquilo | calle, mismo sueño, capucha con tentáculos | no dormirse · cortar tentáculos · repetir la secuencia de ventanas · respirar al ritmo |

**Sonido liminal** (todo sintetizado, sin archivos): olas y gaviotas, grillos y retumbos, murmullo de multitud + pasos con eco + campana lejana, zumbido de fluorescentes + goteras + pasos en pasillos, voces lejanas ininteligibles, latido cuando la cordura es baja. Viñeta y grano de película encima.

Pierdes si tu cordura llega a 0. Si llegas al día 365 el planeta queda con ★.

## APK
GitHub Actions compila el APK en cada push a esta rama: descárgalo en **Releases** (`SobreviveUnAno.apk`).

## Estructura
- `index.html`, `js/engine.js` (motor WebGL estilo PS1), `js/worlds.js` (escenas 3D), `js/game.js` (flujo, cinemáticas, minijuegos), `js/audio.js` (sonido ambiente)
- `audio.mp3` audio del video (se reproduce en las cinemáticas)
- Capacitor 6 → APK debug
