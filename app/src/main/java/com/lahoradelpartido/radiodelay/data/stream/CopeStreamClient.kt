package com.lahoradelpartido.radiodelay.data.stream

import com.lahoradelpartido.radiodelay.domain.model.ConnectionStatus
import com.lahoradelpartido.radiodelay.domain.model.Emission
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

/** Descarga una única señal MP3 de COPE y alimenta el buffer local. */
class CopeStreamClient(
    private val buffer: Mp3CircularBuffer,
    private val client: OkHttpClient = defaultClient(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableStatus = MutableStateFlow(ConnectionStatus.IDLE)
    private val mutableError = MutableStateFlow<String?>(null)

    val status: StateFlow<ConnectionStatus> = mutableStatus.asStateFlow()
    val error: StateFlow<String?> = mutableError.asStateFlow()

    @Volatile private var streamJob: Job? = null
    @Volatile private var activeCall: Call? = null
    @Volatile private var sourceUrl: String = Emission.NATIONAL.streamUrl

    // Cada conexión lleva su número de generación. Cancelar una llamada no
    // detiene al instante el hilo que está leyendo el socket, así que sin esta
    // marca podrían colarse en el buffer frames de la señal anterior justo
    // después de vaciarlo.
    @Volatile private var generation = 0

    /** Señal que se está descargando, para no reiniciar si no ha cambiado. */
    val currentSource: String get() = sourceUrl

    // Leer la lista `.m3u` es una descarga corta y necesita un tiempo máximo de
    // lectura, al contrario que el propio audio, que nunca termina.
    private val playlistClient = client.newBuilder()
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Conecta con [url]. Si ya se estaba escuchando otra señal, cierra la
     * anterior y descarta el histórico: el audio guardado pertenece a una
     * emisión distinta y mezclarlo produciría un salto sin sentido.
     */
    @Synchronized
    fun start(url: String = sourceUrl) {
        if (url == sourceUrl && streamJob?.isActive == true) return
        if (url != sourceUrl) {
            stopInternal(clearBuffer = true)
            sourceUrl = url
        }
        if (streamJob?.isActive == true) return
        val myGeneration = generation
        streamJob = scope.launch { downloadLoop(myGeneration) }
    }

    @Synchronized
    fun stop(clearBuffer: Boolean = false) = stopInternal(clearBuffer)

    private fun stopInternal(clearBuffer: Boolean) {
        generation += 1
        activeCall?.cancel()
        activeCall = null
        streamJob?.cancel()
        streamJob = null
        mutableStatus.value = ConnectionStatus.IDLE
        if (clearBuffer) buffer.clear()
    }

    private suspend fun downloadLoop(myGeneration: Int) {
        var attempt = 0
        while (currentCoroutineContext().isActive && generation == myGeneration) {
            mutableStatus.value = if (attempt == 0) {
                ConnectionStatus.CONNECTING
            } else {
                ConnectionStatus.RECONNECTING
            }

            try {
                downloadSingleConnection(resolveStreamUrl(sourceUrl), myGeneration)
                attempt = 0
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (!currentCoroutineContext().isActive) return
                mutableError.value = exception.message ?: "No se pudo leer la emisión"
                mutableStatus.value = ConnectionStatus.ERROR
                attempt += 1
                delay(retryDelayMillis(attempt))
            }
        }
    }

    /**
     * COPE publica cada señal como una lista `.m3u` de un solo elemento que
     * apunta al nodo de reparto asignado en ese momento. Resolverla en cada
     * intento evita quedarse enganchado a un nodo caído tras una reconexión.
     */
    private fun resolveStreamUrl(source: String): String {
        if (!source.endsWith(".m3u", ignoreCase = true)) return source

        val request = Request.Builder()
            .url(source)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://www.cope.es/")
            .build()

        playlistClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("La lista de reproducción respondió con HTTP ${response.code}")
            }
            val playlist = response.body.string()
            return playlist.lineSequence()
                .map(String::trim)
                .firstOrNull { it.startsWith("http", ignoreCase = true) }
                ?: throw IOException("La lista de reproducción no contiene ninguna señal")
        }
    }

    private fun downloadSingleConnection(streamUrl: String, myGeneration: Int) {
        val parser = Mp3FrameParser { frame ->
            if (generation == myGeneration) buffer.append(frame)
        }
        val request = Request.Builder()
            .url(streamUrl)
            .header("Accept", "*/*")
            .header("Origin", "https://www.cope.es")
            .header("Referer", "https://www.cope.es/")
            .header("User-Agent", USER_AGENT)
            .build()

        val call = client.newCall(request)
        activeCall = call
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("El servidor respondió con HTTP ${response.code}")
                }

                val body = response.body
                mutableError.value = null
                mutableStatus.value = ConnectionStatus.CONNECTED

                body.byteStream().use { input ->
                    val chunk = ByteArray(NETWORK_CHUNK_SIZE)
                    while (generation == myGeneration && streamJob?.isActive == true) {
                        val count = input.read(chunk)
                        if (count < 0) throw IOException("La emisión cerró la conexión")
                        if (count > 0) parser.consume(chunk, count)
                    }
                }
            }
        } finally {
            if (activeCall === call) activeCall = null
        }
    }

    private fun retryDelayMillis(attempt: Int): Long =
        (1_000L shl (attempt - 1).coerceIn(0, 4)).coerceAtMost(15_000L)

    companion object {
        private const val USER_AGENT = "LaHoraDelPartido/2.0 Android"
        private const val NETWORK_CHUNK_SIZE = 32 * 1024

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
