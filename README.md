# Navegador para tablets viejas (Galaxy Tab 3 Lite SM-T110)

Un navegador mínimo pensado para tablets con **Android 4.2.2 "Jelly Bean"** (como la Samsung
Galaxy Tab 3 Lite SM-T110), con accesos directos a Google, YouTube y CloudMoon. Versión 0.1.0.

## Aviso honesto antes de instalarlo

Esta app usa el **WebView del propio sistema operativo** del tablet para mostrar páginas web — no
trae su propio motor de navegador aparte. En Android 4.2.2 ese WebView es de 2012-2013: no tiene
WebRTC, le faltan varias funciones modernas de JavaScript, y la propia tablet trae un procesador de
un solo núcleo con 1 GB de RAM.

Eso quiere decir que:

- **Sitios sencillos** (páginas de texto, formularios, noticias, Wikipedia, etc.) deberían
  funcionar razonablemente bien.
- **YouTube actual** y el **cliente web de CloudMoon** (`web.cloudmoonapp.com`) muy probablemente
  **no funcionen bien o no carguen del todo**, porque ambos dependen de JavaScript y video que ese
  WebView viejo no soporta. Esto no es un error de esta app: es un límite del sistema operativo del
  tablet, y ninguna app puede arreglarlo por sí sola. La única forma real de cambiar esto sería
  ponerle al tablet una versión de Android más nueva (una ROM no oficial, si existe una compatible
  con el SM-T110), lo cual tiene su propio riesgo de dejar el tablet sin funcionar si algo sale mal.
- Se manda un identificador de navegador moderno (como si fuera Chrome actual) para que los sitios
  no bloqueen la carga de entrada con un aviso de "actualiza tu navegador" — pero eso no le da al
  WebView capacidades que no tiene, solo evita que algunos sitios corten el paso antes de intentar
  cargar.

## Qué trae

- Barra de dirección con búsqueda (si no parece una dirección, busca en Google).
- Botones de atrás, adelante, recargar y "casa".
- Accesos directos a Google, YouTube (versión móvil) y CloudMoon.
- Soporte de video en pantalla completa dentro de la página (necesario para que un reproductor de
  video se vea en toda la pantalla, cuando el sitio sí logra cargar el video).
- Se dejan pasar errores de certificado HTTPS causados por la fecha/hora o los certificados raíz
  desactualizados del propio tablet (algo común en dispositivos tan viejos), para no bloquear sitios
  por eso.

## Cómo compilar el APK

El repositorio compila el APK automáticamente en GitHub Actions y lo publica como Release en cada
cambio a `main`; no hace falta una PC para conseguirlo. También se puede compilar a mano con
Android Studio o `gradle assembleDebug` (Gradle 8.9+, `ANDROID_HOME` apuntando al SDK).

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Cópialo al tablet (por USB, por
correo, o descargándolo directo desde el Release de GitHub en el propio tablet) e instálalo
permitiendo "instalar apps de orígenes desconocidos".

Requiere Android 4.2 (API 17) o superior.
