# Sobrevive un año

Juego Android (vertical, 360×640 estilo PS1 / analog horror) basado frame por frame en el video
"Tienes que sobrevivir un año": eliges un planeta y sobrevives 365 días.

Las cinemáticas reproducen el video (mismos cortes, subtítulos y tiempos, con su audio) y cada una
desemboca en el juego de ese planeta:

| Planeta | Cinemática | Jugabilidad |
|---|---|---|
| 1 · Las islas | hamaca, remolino, pueblo que desaparece, monstruo de ojos | **Cuenta las luces** del pueblo antes de que se apaguen · **cierra los ojos** del monstruo antes de que se abran |
| 2 · Las estrellas | cama, telescopio, cielo que se acerca, gigante con agujero | **Mantén la mira** sobre el grupo de estrellas · **cierra los ojos** (mantén presionado) cuando aparece el gigante |
| 3 · Nadie tiene prisa | plaza, relojes iguales, panadero congelado, pie que no termina el paso | **Pasos lentos** al ritmo del anillo (correr te cuesta cordura) · **empuja** lo que quedó congelado |
| 4 · El más tranquilo | calle, mismo sueño, capucha con tentáculos | **No te duermas** (toca sin parar) · **corta los tentáculos** de la capucha |

Pierdes si tu cordura llega a 0. Si llegas al día 365 el planeta queda con ★.

## APK
GitHub Actions compila el APK en cada push a esta rama: descárgalo en **Releases** (`SobreviveUnAno.apk`).

## Estructura
- `index.html`, `js/engine.js` (motor WebGL estilo PS1), `js/worlds.js` (escenas 3D), `js/game.js` (flujo, cinemáticas, minijuegos)
- `audio.mp3` audio del video (se reproduce en las cinemáticas)
- Capacitor 6 → APK debug
