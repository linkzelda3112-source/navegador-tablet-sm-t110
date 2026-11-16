# Pulso Estelar - subir a GitHub y sacar el APK (solo 2 archivos)

Necesitas subir solo DOS cosas:

1. `proyecto.zip`  -> el juego completo (se sube sin descomprimir).
2. `build-apk.yml` -> la receta que descomprime el zip y compila el APK.

## Pasos

1. En **github.com** crea tu cuenta y toca **+ > New repository**. Nombre: `pulso-estelar`. Crea el repositorio.
2. Toca **Add file > Upload files**, elige `proyecto.zip` y toca **Commit changes**.
   (No lo descomprimas: la receta lo hace sola.)
3. Toca **Add file > Create new file**.
   - En el nombre escribe exactamente: `.github/workflows/build-apk.yml`
   - Abre `build-apk.yml`, copia TODO su contenido y pegalo ahi.
   - Toca **Commit changes**.
4. Abre la pestana **Actions**. Veras "Construir APK de Pulso Estelar" trabajando (5 a 10 minutos).
   Si no empieza, entra a esa receta y toca **Run workflow**.
5. Al terminar con una palomita verde, abre **Releases** (a la derecha en la pagina principal del repositorio)
   y descarga **PulsoEstelar.apk**. Tambien esta en Actions > el run > **Artifacts**.
6. Instalalo en tu Android (permite "instalar apps de origen desconocido").

Si algo se pone rojo en Actions, abre el paso que fallo y copia el error: con eso se arregla.

## Despues

- Si cambias el juego, sube un `proyecto.zip` nuevo (reemplaza al anterior) y se compila otro APK solo.
- El APK es de prueba (debug): se instala sin problema; para Google Play se necesita una version firmada.
- La app queda en horizontal y pantalla completa.
