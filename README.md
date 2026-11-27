# Cráter Z

Juego 3D de supervivencia por rondas contra zombis para Android (OpenGL ES 2.0, sin librerías externas).
Mapa, personajes, armas y nombres son originales.

- Primera persona, mapa "El Cráter": campamento, cráter central, iglesia en ruinas, cementerio y taller.
- Texturas procedurales con mapas de normales, sombras de la luna, niebla, lluvia, relámpagos y partículas.
- Rondas infinitas, puertas de escombros, armas en la pared, Caja Misteriosa, ventajas y bonificaciones.
- Hub con 3 modos, ajustes de calidad gráfica, récords y ayuda.

Rama: `zombies-origins`. El APK se compila con GitHub Actions y se publica en Releases (`zombies-N`).

## v1.1 — realismo
- Zombis detallados: cara esculpida con mandíbula móvil, casco, abrigo rasgado o torso al aire, manos en garra, botas; 4 aspectos y 4 formas de andar (arrastrado, cojo, tambaleo, abalanzado); tambaleo al recibir disparos y caídas distintas al morir.
- Armas con modelo detallado (corredera, martillo, miras, cañón, guardamanos, cargador, camisa perforada…), manos con dedos, y piezas animadas (corredera/cerrojo/bombeo y cargador al recargar).
- Sonido sintetizado en capas: chasquido + estampido + golpe grave + eco del terreno, reverberación, mecánica (cargador, cerrojo, bombeo, casquillos), voces con formantes, impactos carnosos y disparo a la cabeza.

## v1.2 — inteligencia colectiva
- Mapa de rutas compartido (campo de flujo sobre una rejilla con obstáculos): todos los zombis saben cómo llegar al jugador rodeando muros, trincheras, iglesia y puertas cerradas; se recalcula varias veces por segundo (<1 ms).
- Si te ven, se reparten en posiciones distintas alrededor tuyo para rodearte en vez de apilarse; sin línea de visión siguen la ruta.
