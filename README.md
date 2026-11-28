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

## v1.3
- **Zona de pruebas** (4.º modo del hub): todo gratis y abierto, vida y munición infinitas, zombis de todos los tipos; el botón CAMBIAR recorre las 5 armas y las máquinas de ventajas, la caja y las paredes funcionan sin coste.
- **TR-74 "Tempest" (Tormenta)** rediseñada a partir de la imagen de referencia: freno de boca ranurado con bronce, carcasa hexagonal, mira holográfica, núcleo de plasma que pulsa, rótulos "TR-74 TEMPEST / TORMENTA", cargador con franjas luminosas y arcos eléctricos animados que se intensifican al disparar.

## v1.4 — Tormenta de dos disparos
- 1.er disparo: **rayo** que paraliza (5 s) y hace daño brutal; salta a hasta 6 zombis cercanos.
- Tras el rayo, la Tormenta dispara **normal** mientras el rayo se recarga (8 s; barra "RAYO LISTO" en el HUD). El arma brilla, pulsa y echa más arcos cuando el rayo está listo.
- Zombis paralizados: tiemblan, se tiñen de azul y sueltan chispas.

## v1.5 — hambre y furia
- Los zombis tienen hambre que crece con el tiempo (más rápidos; los lentos acaban trotando), se enfurecen y rugen al verte, se vuelven más veloces si tienes poca vida, se lanzan de un salto al estar cerca y muerden más rápido cuanta más hambre tengan.

## v1.6 — Tormenta más fiel a la foto
- Cajas con aristas achaflanadas (brillos en los bordes) en lugar de bloques duros, y una textura nueva de blindaje azul acero con paneles, juntas, remaches, desgaste y parches de bronce.
