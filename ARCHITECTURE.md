# Arquitectura

El proyecto sigue **Clean Architecture** con dependencias orientadas hacia el dominio:

```text
presentation  ──> domain <── data/controller
                         ^
                         │
                   playback/data-stream
```

## Capas

- `domain/model`: objetos inmutables sin dependencias de Android.
- `domain/gateway`: puertos que describen lo que necesita la aplicación.
- `domain/usecase`: una acción de negocio por clase.
- `data/controller`: adaptador Android que convierte los casos de uso en comandos del servicio.
- `data/preferences`: persistencia del retraso.
- `data/stream`: conexión, análisis de frames MP3 y buffer circular.
- `playback`: coordinación de Media3 y servicio en segundo plano.
- `presentation`: Compose y ViewModel.
- `di/AppContainer`: composition root con inyección manual y explícita.

## SOLID

- **S**: cada clase tiene una responsabilidad concreta: descargar, separar frames, almacenar, reproducir, presentar o persistir.
- **O**: otra emisora puede incorporarse sustituyendo la implementación del cliente, sin modificar los casos de uso.
- **L**: las implementaciones respetan contratos pequeños (`RadioController`, `DelayPreferences`).
- **I**: no existen interfaces generales con operaciones que sus consumidores no usan.
- **D**: dominio y ViewModel dependen de abstracciones/casos de uso, no del servicio Android ni de OkHttp.

## Flujo del audio

```text
COPE MP3 -> OkHttp -> Mp3FrameParser -> Mp3CircularBuffer
                                         |
                                         v
                              BufferedMp3DataSource
                                         |
                                         v
                             Media3 / ExoPlayer / audio
```

El cliente de red se mantiene conectado una sola vez. Al cambiar el retraso, el motor crea un cursor nuevo sobre el buffer local y reconstruye únicamente la fuente local de Media3. Nunca espera en silencio el número de segundos seleccionado.
