# Videos Kids — reproductor de videos sin Internet (Galaxy Tab 3 Lite SM-T110)

Una app estilo "YouTube Kids" pero **100% sin conexión**: reproduce los videos que ya tienes
guardados en la tablet, con una cuadrícula grande y de colores, categorías por carpeta, buscador,
y "siguiente video" automático con recomendaciones — pensada para una tablet vieja con **Android
4.2.2 "Jelly Bean"** (como la Samsung Galaxy Tab 3 Lite SM-T110). Versión 1.0.0.

No usa el nombre, el logo ni los colores de la app real de YouTube Kids — es un reproductor
propio, inspirado en cómo se usa, pero hecho desde cero para esta tablet y sin conexión a
Internet.

## Cómo poner tus videos

1. Conecta la tablet a una computadora por USB (o usa una memoria/tarjeta que luego pongas en la
   tablet).
2. Copia tus videos a la carpeta **`VideosKids`** en el almacenamiento de la tablet (la app la crea
   sola la primera vez que se abre, si no existe).
3. Si quieres que aparezcan separados por pestañas (como "Caricaturas", "Música", etc.), crea
   subcarpetas dentro de `VideosKids` y pon cada video en la subcarpeta que corresponda. Los videos
   sueltos, directo en `VideosKids`, aparecen en la pestaña "Todos".
4. Abre la app (o vuelve a la pantalla principal si ya estaba abierta): busca los videos solos y
   los muestra en la cuadrícula.

Formatos que funcionan mejor: **MP4** (H.264). Otros formatos (MKV, WEBM, 3GP, AVI, M4V) también se
detectan, pero si el tablet no trae el códec de video necesario, puede que no se reproduzcan bien;
MP4 es la apuesta más segura en un dispositivo tan viejo.

## Qué trae

- Cuadrícula grande con tarjetas de colores y miniatura de cada video, fácil de tocar para un niño.
- Pestañas de categoría automáticas según las subcarpetas que uses dentro de `VideosKids`.
- Buscador (ícono de lupa) para encontrar un video por su nombre.
- Al terminar un video, se muestra una pantalla de "siguiente video" con otros videos recomendados
  (elegidos al azar de la biblioteca) y una cuenta regresiva: si no se toca nada, pasa solo al
  siguiente; si se toca otro video de la lista, se reproduce ese en su lugar — igual que el
  autoplay de YouTube Kids.
- Todo funciona **sin Internet**: la app nunca se conecta a la red, solo lee archivos que ya están
  en la tablet.

## Por qué no es exactamente la app real de YouTube Kids

YouTube Kids es una app y una marca de Google — no se puede usar su nombre, su logo ni copiar su
diseño exacto. Esta app se parece en cómo se usa (cuadrícula de colores, categorías, autoplay con
recomendaciones), pero es un reproductor local hecho desde cero, con su propio nombre e ícono, y
sin ninguna conexión con el catálogo real de YouTube: solo reproduce los videos que tú mismo copies
a la tablet.

## Cómo compilar el APK

El repositorio compila el APK automáticamente en GitHub Actions y lo publica como Release en cada
cambio a `main`; no hace falta una PC para conseguirlo. También se puede compilar a mano con
Android Studio o `gradle assembleDebug` (Gradle 8.9+, `ANDROID_HOME` apuntando al SDK).

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Cópialo al tablet (por USB, por
correo, o descargándolo directo desde el Release de GitHub en el propio tablet) e instálalo
permitiendo "instalar apps de orígenes desconocidos".

Requiere Android 4.2 (API 17) o superior.
