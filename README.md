# Lumina — reproductor de música para Android

Reproductor elegante para tus archivos de música locales (mp3, m4a, flac, ogg, opus, wav…).
Sin cuentas y sin librerías externas (solo usa internet si tú buscas una carátula en línea): todo el dibujo (íconos, listas, barra de
progreso) está hecho a mano con Canvas. Versión 1.2.0 · Android 7+ · vertical.

## Dónde busca la música

Tú decides, en la pantalla de fuentes (botón de carpeta arriba a la derecha):

- **Buscar en todo el teléfono**: usa el índice de música de Android (pide permiso de audio).
- **Elegir una carpeta**: abre el selector del sistema y solo lee esa carpeta (y sus subcarpetas).
  Puedes agregar varias. No requiere permiso de almacenamiento.

La biblioteca se guarda en caché, así que la app abre al instante y se actualiza en segundo plano.

## Qué incluye

- Pestañas **Canciones / Álbumes / Artistas**, búsqueda instantánea y "reproducir en aleatorio".
- Mini reproductor flotante y pantalla **Reproduciendo** con portada grande, fondo que toma el
  color de la portada, barra para adelantar, aleatorio y repetir (lista / una canción).
- Reproducción en segundo plano con notificación, controles en pantalla de bloqueo y auriculares,
  pausa automática al desconectar audífonos o recibir una llamada.

## Carátulas y temas

- En "Reproduciendo", el botón de imagen permite poner una **carátula propia**: una imagen del
  teléfono o una **búsqueda en línea** (API pública de iTunes, sin cuenta). Se aplica a todo el álbum.
- El botón de paleta abre **Ajustes**: 6 temas (Medianoche, Esmeralda, Rosa, Océano, Carbón, Marfil),
  3 fondos para "Reproduciendo" (color de portada, difuminado, liso) y el permiso de batería para
  que la música siga sonando en segundo plano (en Honor/Huawei, activa también "Ejecutar en segundo plano").

## Estructura

`Library` (fuentes, escaneo, caché) · `ArtCache` (portadas) · `MusicService` (reproducción) ·
`MainActivity` (pantallas) · vistas propias: `NowPlayingView`, `MiniPlayerView`, `SourcesView`,
`RowView`, `SeekView`, `TabsView`, `IconView` · `Icons` (íconos por código) · `Ui` (estilo).

Cada push a `main` compila el APK con GitHub Actions y lo publica en Releases.

## Novedades 1.2

- Pantalla de **Inicio** estilo Spotify: saludo, accesos a *Me encanta*, *Me gusta*, *Éxitos* y *Recientes*.
- **Me gusta** (pulgar) y **Me encanta** (corazón) desde “Reproduciendo”; marcas en las listas.
- **Éxitos**: con internet descarga las canciones más escuchadas de tu país (lista pública de Apple) y muestra cuáles tienes en tu biblioteca.
- Arreglo: el mini reproductor ya no se encima sobre “Reproduciendo”.
