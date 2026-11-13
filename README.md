# Mini Motor 3D — visor/editor de escenas 3D para Android (Honor X8)

Una app sencilla para colocar y mover modelos 3D en una escena, pensada como base para programar
juegos encima más adelante — no es un editor profesional tipo Unity, es a propósito pequeña y
fácil de entender (todo el render 3D y la lectura de modelos están escritos a mano, sin librerías
externas). Versión 1.1.0.

## Cómo poner tus modelos

1. Consigue un modelo 3D en formato **.obj** (el formato de texto más simple; Blender, y
   prácticamente cualquier programa de modelado o banco de modelos gratis, lo puede exportar).
   Si tiene textura, exporta también su `.mtl` y la imagen (`.png` o `.jpg`).
2. Mete ese `.obj` (+ `.mtl` + imagen, si aplica) dentro de un archivo **.zip**. No importa si
   quedan sueltos o en una subcarpeta dentro del zip.
3. Abre la app y toca **"+ Modelo"** arriba: se abre el selector de archivos normal de Android, y
   ahí puedes elegir el `.zip` desde donde lo tengas guardado — Descargas, Google Drive, WhatsApp,
   una memoria USB conectada, etc. No hace falta copiarlo a ninguna carpeta especial ni dar
   ningún permiso de almacenamiento.

## Cómo se usa

- **Modo Cámara** (el que empieza activo): arrastra un dedo para girar la vista alrededor de la
  escena; junta o separa dos dedos para acercar/alejar.
- **Modo Objeto** (botón "Modo" arriba): toca primero el objeto en la fila de arriba para
  seleccionarlo, y arrastra un dedo sobre la pantalla para moverlo por el suelo (eje X/Z).
- Botones de abajo, con el objeto seleccionado: subir/bajar (eje Y), rotar, y agrandar/achicar.
- Puedes agregar el mismo modelo varias veces (cada uno es un objeto independiente que se puede
  mover por separado), o varios modelos distintos a la vez.

## Por qué está hecho así (sin librerías externas)

El proyecto anterior en este mismo repositorio (un lector de PDF) tuvo problemas porque la
librería externa que usaba dejó de estar disponible en los repositorios de Maven de un día para
otro. Para no repetir ese problema, este motor 3D no depende de ninguna librería de terceros: lee
los `.obj` con un parser de texto propio, y dibuja todo con OpenGL ES 2.0 (incluido en cualquier
Android desde hace más de una década) escrito directamente con sus funciones básicas.

## Requisitos

- Android 7.0 (API 24) o más nuevo — el Honor X8 cumple de sobra.
- Un procesador con OpenGL ES 2.0, que prácticamente cualquier teléfono de los últimos 10 años
  tiene (incluido el Adreno 610 del Honor X8).

## Cómo compilar el APK

El repositorio compila el APK automáticamente en GitHub Actions y lo publica como Release en cada
cambio a `main`; no hace falta una PC para conseguirlo. También se puede compilar a mano con
Android Studio o `gradle assembleDebug` (Gradle 8.9+, `ANDROID_HOME` apuntando al SDK).

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Cópialo al teléfono (por USB, por
correo, o descargándolo directo desde el Release de GitHub en el propio teléfono) e instálalo
permitiendo "instalar apps de orígenes desconocidos".
