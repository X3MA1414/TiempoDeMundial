package com.tiempodemundial.radiodelay.data.stream

import com.tiempodemundial.radiodelay.domain.model.ConnectionStatus
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

/** Downloads the COPE MP3 stream once and continuously feeds the local buffer. */
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

    @Synchronized
    fun start() {
        if (streamJob?.isActive == true) return
        streamJob = scope.launch { downloadLoop() }
    }

    @Synchronized
    fun stop(clearBuffer: Boolean = false) {
        activeCall?.cancel()
        activeCall = null
        streamJob?.cancel()
        streamJob = null
        mutableStatus.value = ConnectionStatus.IDLE
        if (clearBuffer) buffer.clear()
    }

    private suspend fun downloadLoop() {
        var attempt = 0
        while (currentCoroutineContext().isActive) {
            mutableStatus.value = if (attempt == 0) {
                ConnectionStatus.CONNECTING
            } else {
                ConnectionStatus.RECONNECTING
            }

            try {
                downloadSingleConnection()
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

    private fun downloadSingleConnection() {
        val parser = Mp3FrameParser(buffer::append)
        val request = Request.Builder()
            .url(STREAM_URL)
            .header("Accept", "*/*")
            .header("Origin", "https://www.cope.es")
            .header("Referer", "https://www.cope.es/")
            .header("User-Agent", "RadioDelay/1.0 Android")
            .build()

        val call = client.newCall(request)
        activeCall = call
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("El servidor respondió con HTTP ${response.code}")
                }

                val body = response.body ?: throw IOException("La emisión no contiene audio")
                mutableError.value = null
                mutableStatus.value = ConnectionStatus.CONNECTED

                body.byteStream().use { input ->
                    val chunk = ByteArray(NETWORK_CHUNK_SIZE)
                    while (streamJob?.isActive == true) {
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
        const val STREAM_URL =
            "https://flucast19-h-cloud.flumotion.com/cope/net1.mp3" +
                "?referrer_url=https%3A%2F%2Fwww.cope.es%2Fprogramas%2Ftiempo-de-juego" +
                "&player_type=mobile&domain=www.cope.es"

        private const val NETWORK_CHUNK_SIZE = 32 * 1024

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
