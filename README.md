# Lector PDF — tablet vieja (Galaxy Tab 3 Lite SM-T110)

Una app sencilla para leer archivos **PDF** que ya tienes guardados en el tablet, pensada para
**Android 4.2.2 "Jelly Bean"** (como la Samsung Galaxy Tab 3 Lite SM-T110). Versión 2.0.2.

No usa Internet para nada: solo lee los PDF que ya están en el almacenamiento del tablet.

## Cómo poner tus PDF

1. Conecta la tablet a una computadora por USB (o usa una memoria/tarjeta que luego pongas en la
   tablet).
2. Copia tus archivos PDF a la carpeta **`MisPDF`** en el almacenamiento de la tablet (la app la
   crea sola la primera vez que se abre, si no existe). También puedes organizarlos en
   subcarpetas dentro de `MisPDF` si quieres.
3. La app también encuentra solos los PDF que ya estén en la carpeta de **Descargas** del tablet
   (por ejemplo, si los bajaste con el navegador o los recibiste por correo).
4. Abre la app (o vuelve a la pantalla principal si ya estaba abierta): los PDF encontrados
   aparecen en una lista, con su nombre y tamaño.

## Qué trae

- Lista de todos los PDF encontrados, con una insignia roja "PDF", nombre y tamaño del archivo.
- Visor de PDF con zoom (pellizcar para acercar/alejar), deslizar para pasar de página, y un
  indicador de "página X de Y" arriba.
- **Recuerda dónde te quedaste**: la app guarda sola la última página que leíste de cada PDF, y la
  próxima vez que abras ese mismo archivo retoma ahí mismo.
- También se puede abrir un PDF directo desde otra app (un administrador de archivos, un correo)
  con la opción "Abrir con...".
- El renderizado de las páginas usa **PDFium** (el mismo motor que usa Chrome para mostrar PDF), a
  través de una librería de código abierto (un fork mantenido de "android-pdf-viewer", ya que la
  librería original dejó de estar disponible para descargar).

## Un aviso honesto

El tablet trae un procesador de un solo núcleo y 1 GB de RAM, así que PDF muy pesados (con muchas
imágenes de alta resolución, o cientos de páginas) pueden tardar un poco más en cargar o pasar de
página que en un celular moderno. PDF de texto normal (libros, documentos, formularios) deberían
andar razonablemente bien.

## Cómo compilar el APK

El repositorio compila el APK automáticamente en GitHub Actions y lo publica como Release en cada
cambio a `main`; no hace falta una PC para conseguirlo. También se puede compilar a mano con
Android Studio o `gradle assembleDebug` (Gradle 8.9+, `ANDROID_HOME` apuntando al SDK).

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Cópialo al tablet (por USB, por
correo, o descargándolo directo desde el Release de GitHub en el propio tablet) e instálalo
permitiendo "instalar apps de orígenes desconocidos".

Requiere Android 4.2 (API 17) o superior.
