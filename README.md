# Radio Delay para Android

Aplicación Android en Kotlin para escuchar la emisión MP3 de **Tiempo de Juego** con un retraso local seleccionable por minutos y segundos.

## Funciones

- Selector modal independiente de minutos y segundos.
- Incrementos de **1 minuto** y **1 segundo**.
- Retraso seleccionable entre `00:00` y `05:59`.
- Buffer circular local de siete minutos.
- Cambio inmediato al audio ya almacenado, sin un silencio equivalente al retraso elegido.
- Reproducción en segundo plano mediante `MediaSessionService`.
- Notificación y controles multimedia del sistema.
- Reconexión automática.
- Persistencia del último retraso.
- Arquitectura Clean y principios SOLID.

## Cómo abrirlo

1. Instala una versión reciente de Android Studio con JDK 17 y SDK 37.
2. Abre la carpeta `RadioDelay`.
3. Espera a que Gradle sincronice las dependencias.
4. Ejecuta la configuración `app` en un dispositivo Android 8.0 o superior.

Desde terminal:

```bash
./gradlew assembleDebug
```

El APK se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Funcionamiento del retraso

La emisión es un MP3 progresivo, por lo que el servidor no permite pedir directamente “el audio de hace 40 segundos”. La aplicación mantiene una única conexión, separa los frames MP3 y conserva un historial local. Cuando se selecciona otro retraso, Media3 recibe un cursor nuevo que comienza en el instante solicitado.

Al iniciar la app todavía no existe audio anterior. Si se eligen 40 segundos cuando solo hay 12 disponibles, la reproducción continúa y el salto se aplica automáticamente al llegar a 40 segundos almacenados.

Al reanudar después de una pausa, la fuente se reconstruye desde el directo actual para conservar el retraso seleccionado y no reproducir un fragmento que haya quedado antiguo durante la pausa.

## Fuente configurada

La URL está centralizada en:

```text
app/src/main/java/com/albertocastell/radiodelay/data/stream/CopeStreamClient.kt
```

Si COPE modifica su infraestructura, solo hay que actualizar `STREAM_URL` o sustituir el cliente de stream.

## Estructura

Consulta [ARCHITECTURE.md](ARCHITECTURE.md) para ver las capas y la aplicación concreta de SOLID.

## Pruebas

Incluye pruebas unitarias para:

- Conversión y límites de `Delay`.
- Análisis incremental de frames MP3.
- Posicionamiento inicial del lector del buffer.

```bash
./gradlew testDebugUnitTest
```

## Consideraciones

Este proyecto no está afiliado a COPE. La dirección de la emisión puede cambiar y su uso debe respetar las condiciones del proveedor y los derechos aplicables. La app no redistribuye ni guarda permanentemente el audio: conserva únicamente un buffer temporal en memoria mientras el proceso está activo.

El salto de retraso no espera el tiempo elegido, aunque puede producir una interrupción muy breve mientras Media3 reinicia el decodificador local.
