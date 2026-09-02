package com.lahoradelpartido.radiodelay.playback

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.lahoradelpartido.radiodelay.data.controller.PlaybackStateStore
import com.lahoradelpartido.radiodelay.data.preferences.DelayPreferences
import com.lahoradelpartido.radiodelay.data.preferences.EmissionPreferences
import com.lahoradelpartido.radiodelay.data.stream.BufferedMp3DataSource
import com.lahoradelpartido.radiodelay.data.stream.CopeStreamClient
import com.lahoradelpartido.radiodelay.data.stream.Mp3CircularBuffer
import com.lahoradelpartido.radiodelay.domain.model.Delay
import com.lahoradelpartido.radiodelay.domain.model.Emission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import kotlin.time.Duration.Companion.milliseconds

/** Coordinates ingestion, delayed data sources and the long-lived player. */
@OptIn(UnstableApi::class)
class DelayedPlaybackEngine(
    private val player: ExoPlayer,
    private val circularBuffer: Mp3CircularBuffer,
    private val streamClient: CopeStreamClient,
    private val delayPreferences: DelayPreferences,
    private val emissionPreferences: EmissionPreferences,
    private val stateStore: PlaybackStateStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sourcePrepared = false
    private var pendingDelay: Delay? = null
    private var fadeJob: Job? = null

    private val currentEmission: Emission
        get() = emissionPreferences.read() ?: stateStore.state.value.selectedEmission

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            stateStore.update { it.copy(isPlaying = playWhenReady) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            stateStore.update {
                it.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
            }
            if (playbackState == Player.STATE_READY) fadeIn()
        }

        override fun onPlayerError(error: PlaybackException) {
            stateStore.update {
                it.copy(
                    isBuffering = false,
                    errorMessage = "Error de reproducción: ${error.errorCodeName}",
                )
            }
        }
    }

    init {
        player.addListener(playerListener)

        scope.launch {
            circularBuffer.availableDurationSeconds.collectLatest { available ->
                stateStore.update { current ->
                    current.copy(
                        availableDelaySeconds = available,
                        isWaitingForSelectedDelay = pendingDelay != null,
                    )
                }

                val waiting = pendingDelay
                if (sourcePrepared && waiting != null && available >= waiting.totalSeconds) {
                    pendingDelay = null
                    rebuildSource(waiting, player.playWhenReady)
                }
            }
        }

        scope.launch {
            streamClient.status.collectLatest { status ->
                stateStore.update { it.copy(connectionStatus = status) }
            }
        }

        scope.launch {
            streamClient.error.collectLatest { message ->
                stateStore.update { it.copy(errorMessage = message) }
            }
        }
    }

    fun play() {
        val emission = currentEmission
        streamClient.start(emission.streamUrl)

        val selected = delayPreferences.read()
        stateStore.update {
            it.copy(selectedDelay = selected, selectedEmission = emission, errorMessage = null)
        }

        // Always rebuild on resume so the delay is measured from the current live edge,
        // not from the instant at which playback was paused.
        val available = circularBuffer.availableDurationSeconds.value
        val initialDelay = Delay.ofSeconds(minOf(selected.totalSeconds, available))
        pendingDelay = selected.takeIf { it.totalSeconds > available }
        rebuildSource(initialDelay, playWhenReady = true)
    }

    fun pause() {
        player.pause()
    }

    fun setDelay(delay: Delay) {
        delayPreferences.write(delay)
        stateStore.update { current ->
            current.copy(selectedDelay = delay, errorMessage = null)
        }

        if (!sourcePrepared) {
            pendingDelay = delay.takeIf { it.totalSeconds > 0 }
            stateStore.update { it.copy(isWaitingForSelectedDelay = pendingDelay != null) }
            return
        }

        val available = circularBuffer.availableDurationSeconds.value
        if (delay.totalSeconds <= available) {
            pendingDelay = null
            rebuildSource(delay, player.playWhenReady)
        } else {
            pendingDelay = delay
            stateStore.update { it.copy(isWaitingForSelectedDelay = true) }
        }
    }

    /**
     * Cambia de señal. El histórico se descarta porque pertenece a la emisión
     * anterior, así que la escucha vuelve al directo y el retraso elegido se
     * reaplica en cuanto haya audio suficiente de la nueva señal.
     */
    fun applySelectedEmission() {
        val emission = currentEmission
        // Volver a elegir la señal que ya suena no debe tirar el histórico.
        if (emission.streamUrl == streamClient.currentSource) {
            stateStore.update { it.copy(selectedEmission = emission) }
            return
        }

        val selected = delayPreferences.read()
        streamClient.start(emission.streamUrl)
        stateStore.update {
            it.copy(
                selectedEmission = emission,
                selectedDelay = selected,
                availableDelaySeconds = 0,
                effectiveDelay = Delay.ZERO,
                errorMessage = null,
            )
        }

        pendingDelay = selected.takeIf { it.totalSeconds > 0 }
        if (sourcePrepared) rebuildSource(Delay.ZERO, player.playWhenReady)
    }

    fun release(clearBuffer: Boolean = true) {
        fadeJob?.cancel()
        player.removeListener(playerListener)
        streamClient.stop(clearBuffer = clearBuffer)
        scope.cancel()
    }

    private fun rebuildSource(delay: Delay, playWhenReady: Boolean) {
        sourcePrepared = true
        fadeJob?.cancel()
        player.volume = 0f

        val emission = stateStore.state.value.selectedEmission
        val mediaSource = ProgressiveMediaSource.Factory(
            BufferedMp3DataSource.Factory(circularBuffer, delay),
        ).createMediaSource(
            MediaItem.Builder()
                .setUri("buffered-mp3://cope/${emission.id}/${delay.totalSeconds}".toUri())
                .setMimeType(MimeTypes.AUDIO_MPEG)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(emission.title)
                        .setArtist(
                            if (delay.totalSeconds == 0) {
                                "COPE · En directo"
                            } else {
                                "COPE · ${delay.formatted} de retraso"
                            },
                        )
                        .build(),
                )
                .build(),
        )

        stateStore.update {
            it.copy(
                effectiveDelay = delay,
                isBuffering = true,
                isWaitingForSelectedDelay = pendingDelay != null,
            )
        }

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    private fun fadeIn() {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            repeat(6) { index ->
                player.volume = (index + 1).toFloat() / 6f
                delay(25.milliseconds)
            }
            player.volume = 1f
        }
    }
}
