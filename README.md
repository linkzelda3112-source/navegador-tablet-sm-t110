# KI ARENA — juego de combate por cartas para Android

Juego de peleas por turnos y cartas **3 contra 3**, inspirado en el estilo de los "legends" de
anime de combate. Personajes, movimientos y arte son **originales** (dibujados por código con
Canvas), sin imágenes ni librerías externas. Versión 2.0.0 · Android 7+ · se juega en horizontal.

## Cómo se juega

1. **Elige 3 guerreros** (el primero sale a pelear). Empiezas con 4; los otros 4 se desbloquean
   ganando combates de la **Campaña** (5 combates, cada vez más difíciles). También hay
   **Combate libre** contra un equipo al azar.
2. Cada turno ves **4 cartas** de tu guerrero activo. Tocas una y tu rival elige la suya a la vez:
   - **Golpe** (cuesta 0, siempre disponible), **Combo**, **Onda de Ki**, **Guardia**,
     un **especial** propio (curar, subir ataque, cargar ki, o un golpe/rayo fuerte) y el
     **Definitivo** (cuesta 6 de Ki, daño enorme con animación de rayo).
   - Tocando uno de los dos compañeros de la izquierda **cambias de guerrero** (gastas el turno).
3. **Ki**: empiezas con 3 y ganas +2 por turno (+1 extra si te guardas). Máximo 8.
4. **Ventaja de tipo**: FUERZA (rojo) > TÉCNICA (verde) > KI (azul) > FUERZA. Ventaja = +25% de
   daño, desventaja = −20%.
5. Va primero el más veloz (los golpes tienen prioridad; los definitivos van más lentos).
   Hay golpes críticos (12%). Si tu guerrero cae, entra el siguiente solo. Gana quien deje sin
   vida a los 3 rivales.

El progreso (campaña y desbloqueos) se guarda en el teléfono. El botón "X" de arriba sale del
combate (toca dos veces).

## Estructura

- `Battle.java`, `Roster.java`, `Fighter.java`, `Card.java` — reglas y datos, Java puro (sin Android).
- `GameView.java` — pantallas (menú, selección, combate), animaciones y toques.
- `FighterArt.java` — dibujo de los personajes por código. `Sfx.java` — sonidos sintetizados.
- `Save.java` — guardado. `MainActivity.java` — pantalla completa.

Cada push a `main` compila el APK con GitHub Actions y lo publica en Releases.
