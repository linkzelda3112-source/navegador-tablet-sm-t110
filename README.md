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

## Reglas del juego
- **Una sola vida, un solo mundo.** La primera vez eliges un planeta (con confirmación) y ya no se puede cambiar ni volver a empezar. El progreso se guarda también en **Block Store de Google**, que sobrevive a desinstalar y reinstalar (misma firma, mismo teléfono).
- **Primera persona**, sin personaje.
- **Un día del juego = un día real.** El día solo cuenta si abres el juego ese día; cada día hay una prueba (minijuego) distinta. Si cierras la app a mitad de la prueba pierdes 12 de cordura. Hora verificada por internet cuando hay conexión; si el reloj del teléfono retrocede, el día no cuenta.
- Día 365 sobrevivido = fin. Cordura 0 = fin. Ambos son permanentes.
- **⚙ MODO PRUEBA** (botón abajo a la izquierda en la pantalla de inicio y al terminar el día): ver intros, explorar los 4 mundos, cualquier día 1–365 y cualquier minijuego. No guarda nada.

## Mundo 1 · Las Islas: cámara libre y anomalías diarias
Después de la prueba del día (o si ya la hiciste), te quedas en la playa con **cámara libre**: arrastra para mirar alrededor y **toca lo que se vea raro** para registrarlo (`ANOMALÍAS n / total`, se guarda).
Todo depende del día real, así que el mundo va cambiando y lo anterior se queda:
- **Cubos con textura perdida** (magenta/negro): uno nuevo cada 12 días, más lejos y más grandes; parpadean y giran.
- **Figura negra sobre el mar** desde el día 25: cada día está más cerca de la orilla.
- **Ojos sobre el agua** desde el día 120 (uno nuevo cada 30 días, hasta 8).
- **Luna roja** a plena luz del día desde el día 200.
- Cambios de escena: las luces del pueblo se apagan de a una (día 1–90), el remolino crece, las palmeras se doblan, el muelle se alarga, la hamaca se mece sola, el mar se enrojece desde el día 180.
- **Glitch de pantalla** (cortes, franjas de color y mensajes como "FALTAN") desde el día 8, más frecuente y fuerte cada día.
- En **⚙ MODO PRUEBA → EXPLORAR EL MUNDO** (mundo 1) hay botones −30/−1/+1/+30 para saltar de día y ver cómo evoluciona.

## APK
GitHub Actions compila el APK en cada push a esta rama: descárgalo en **Releases** (`SobreviveUnAno.apk`). El APK va firmado con `debug.keystore` (fija) para poder actualizar encima sin desinstalar.

## Estructura
- `index.html`, `js/engine.js` (motor WebGL estilo PS1), `js/worlds.js` (escenas 3D), `js/game.js` (flujo, cinemáticas, minijuegos), `js/audio.js` (sonido ambiente), `js/store.js` (guardado + hora verificada), `native/` (plugin Java de Block Store)
- `audio.mp3` audio del video (se reproduce en las cinemáticas)
- Capacitor 6 → APK debug
