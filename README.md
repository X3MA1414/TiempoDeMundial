# La Hora del Partido

Aplicación Android en Kotlin para escuchar la radio de COPE con un retraso local
seleccionable por minutos y segundos, y para elegir **qué señal** se escucha
cuando la emisora está dando varios programas a la vez.

## Funciones

- Selector de la emisión en directo: la app lee la parrilla de COPE y ofrece las
  señales que están sonando en ese momento, marcando las deportivas.
- Retraso seleccionable entre `00:00` y `05:59`, con atajos y una rueda de
  minutos y segundos.
- Buffer circular local de siete minutos.
- Cambio inmediato al audio ya almacenado, sin un silencio equivalente al
  retraso elegido.
- Reproducción en segundo plano mediante `MediaSessionService`.
- Notificación y controles multimedia del sistema.
- Reconexión automática y resolución de la lista `.m3u` en cada intento.
- Persistencia del último retraso y de la última emisión elegida.
- Arquitectura Clean y principios SOLID.

## Cómo abrirlo

1. Instala una versión reciente de Android Studio con JDK 17 y SDK 37.
2. Abre esta carpeta.
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

La emisión es un MP3 progresivo, por lo que el servidor no permite pedir
directamente “el audio de hace 40 segundos”. La aplicación mantiene una única
conexión, separa los frames MP3 y conserva un historial local. Cuando se
selecciona otro retraso, Media3 recibe un cursor nuevo que comienza en el
instante solicitado.

Al iniciar la app todavía no existe audio anterior. Si se eligen 40 segundos
cuando solo hay 12 disponibles, la reproducción continúa y el salto se aplica
automáticamente al llegar a 40 segundos almacenados.

Al reanudar después de una pausa, la fuente se reconstruye desde el directo
actual para conservar el retraso seleccionado y no reproducir un fragmento que
haya quedado antiguo durante la pausa.

## Selección de emisión

COPE reparte su programación entre varias señales simultáneas (`net1`, `net2`,
y señales extra en jornadas con varios partidos). En momentos como *Tiempo de
Juego* la señal principal puede estar dando un programa generalista mientras el
fútbol suena en otra.

La app consulta la parrilla que alimenta al reproductor de la web:

```text
https://www.cope.es/ply/prg
```

De ese documento se toman los bloques activos en el minuto actual —incluidas las
emisiones simultáneas del campo `se`— y se construye la lista de señales
disponibles, marcando como deportivas las etiquetadas por COPE o cuyo título
corresponde a un programa de deportes. Si la consulta falla, se ofrecen las
señales conocidas para que el selector nunca quede vacío.

Al cambiar de señal se descarta el histórico acumulado, porque pertenece a la
emisión anterior, y el retraso elegido vuelve a aplicarse en cuanto hay audio
suficiente de la nueva.

La lógica de interpretación de la parrilla vive en `CopeScheduleParser`, sin red
ni dependencias de Android, y está cubierta por pruebas unitarias.

## Estructura

Consulta [ARCHITECTURE.md](ARCHITECTURE.md) para ver las capas y la aplicación
concreta de SOLID.

## Pruebas

Incluye pruebas unitarias para:

- Conversión y límites de `Delay`.
- Análisis incremental de frames MP3.
- Posicionamiento inicial del lector del buffer.
- Selección de las emisiones activas a partir de la parrilla de COPE.

```bash
./gradlew testDebugUnitTest
```

## Consideraciones

Este proyecto no está afiliado a COPE. La dirección de las emisiones puede
cambiar y su uso debe respetar las condiciones del proveedor y los derechos
aplicables. La app no redistribuye ni guarda permanentemente el audio: conserva
únicamente un buffer temporal en memoria mientras el proceso está activo.

El salto de retraso no espera el tiempo elegido, aunque puede producir una
interrupción muy breve mientras Media3 reinicia el decodificador local.
