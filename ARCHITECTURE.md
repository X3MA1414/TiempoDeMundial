# Arquitectura

El proyecto sigue **Clean Architecture** con dependencias orientadas hacia el dominio:

```text
presentation  ──> domain <── data/controller
                    ^  ^
                    │  └── data/catalog
                    │
              playback/data-stream
```

## Capas

- `domain/model`: objetos inmutables sin dependencias de Android (`Delay`, `Emission`).
- `domain/gateway`: puertos que describen lo que necesita la aplicación
  (`RadioController`, `EmissionCatalog`).
- `domain/usecase`: una acción de negocio por clase.
- `data/controller`: adaptador Android que convierte los casos de uso en comandos del servicio.
- `data/catalog`: consulta de la parrilla de COPE (`CopeEmissionCatalog`) y su
  interpretación pura (`CopeScheduleParser`).
- `data/preferences`: persistencia del retraso y de la emisión elegida.
- `data/stream`: conexión, análisis de frames MP3 y buffer circular.
- `playback`: coordinación de Media3 y servicio en segundo plano.
- `presentation`: Compose, tema y ViewModel.
- `di/AppContainer`: composition root con inyección manual y explícita.

## SOLID

- **S**: cada clase tiene una responsabilidad concreta: descargar, separar
  frames, almacenar, reproducir, presentar, persistir o leer la parrilla.
  `CopeScheduleParser` se separó de `CopeEmissionCatalog` precisamente para que
  interpretar el horario no dependa de la red.
- **O**: otra emisora puede incorporarse sustituyendo la implementación del
  cliente de stream y del catálogo, sin modificar los casos de uso.
- **L**: las implementaciones respetan contratos pequeños (`RadioController`,
  `DelayPreferences`, `EmissionPreferences`, `EmissionCatalog`).
- **I**: no existen interfaces generales con operaciones que sus consumidores no usan.
- **D**: dominio y ViewModel dependen de abstracciones/casos de uso, no del
  servicio Android ni de OkHttp.

## Flujo del audio

```text
COPE .m3u -> resolución de nodo -> OkHttp -> Mp3FrameParser -> Mp3CircularBuffer
                                                                     |
                                                                     v
                                                          BufferedMp3DataSource
                                                                     |
                                                                     v
                                                     Media3 / ExoPlayer / audio
```

El cliente de red se mantiene conectado una sola vez por señal. Al cambiar el
retraso, el motor crea un cursor nuevo sobre el buffer local y reconstruye
únicamente la fuente local de Media3. Nunca espera en silencio el número de
segundos seleccionado.

## Flujo del cambio de emisión

```text
RadioViewModel -> SelectEmissionUseCase -> AndroidRadioController
                                                |
                                    guarda en preferencias
                                                |
                                    intent ACTION_SET_EMISSION
                                                v
                       RadioPlaybackService -> DelayedPlaybackEngine
                                                |
                        CopeStreamClient.start(nueva URL) + buffer.clear()
```

El histórico se descarta al cambiar de señal porque pertenece a la emisión
anterior; el retraso elegido queda pendiente y se reaplica automáticamente en
cuanto la nueva señal ha acumulado audio suficiente.
