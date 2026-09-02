package com.lahoradelpartido.radiodelay.data.controller

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.lahoradelpartido.radiodelay.data.preferences.DelayPreferences
import com.lahoradelpartido.radiodelay.data.preferences.EmissionPreferences
import com.lahoradelpartido.radiodelay.domain.gateway.RadioController
import com.lahoradelpartido.radiodelay.domain.model.Delay
import com.lahoradelpartido.radiodelay.domain.model.Emission
import com.lahoradelpartido.radiodelay.domain.model.RadioPlaybackState
import com.lahoradelpartido.radiodelay.playback.RadioPlaybackService
import kotlinx.coroutines.flow.StateFlow

/** Android adapter for the domain controller port. */
class AndroidRadioController(
    context: Context,
    private val stateStore: PlaybackStateStore,
    private val delayPreferences: DelayPreferences,
    private val emissionPreferences: EmissionPreferences,
) : RadioController {
    private val appContext = context.applicationContext

    override val state: StateFlow<RadioPlaybackState> = stateStore.state

    override fun play() {
        val intent = Intent(appContext, RadioPlaybackService::class.java)
            .setAction(RadioPlaybackService.ACTION_PLAY)

        // Playback must survive the activity going to background or the screen locking.
        // RadioPlaybackService registers its MediaSession during onCreate, so Media3 can
        // publish the media notification and call startForeground while the player buffers.
        ContextCompat.startForegroundService(appContext, intent)
    }

    override fun pause() {
        if (!state.value.serviceActive) return
        appContext.startService(
            Intent(appContext, RadioPlaybackService::class.java)
                .setAction(RadioPlaybackService.ACTION_PAUSE),
        )
    }

    override fun setDelay(delay: Delay) {
        delayPreferences.write(delay)
        stateStore.update { it.copy(selectedDelay = delay, errorMessage = null) }

        if (!state.value.serviceActive) return
        appContext.startService(
            Intent(appContext, RadioPlaybackService::class.java)
                .setAction(RadioPlaybackService.ACTION_SET_DELAY)
                .putExtra(RadioPlaybackService.EXTRA_DELAY_SECONDS, delay.totalSeconds),
        )
    }

    override fun returnToLive() = setDelay(Delay.ZERO)

    /**
     * Cambiar de señal reinicia la descarga, así que el servicio debe estar
     * vivo para aplicarlo. Si todavía no lo está, basta con dejarlo guardado:
     * el motor lo leerá de las preferencias al arrancar.
     */
    override fun selectEmission(emission: Emission) {
        emissionPreferences.write(emission)
        stateStore.update { it.copy(selectedEmission = emission, errorMessage = null) }

        if (!state.value.serviceActive) return
        appContext.startService(
            Intent(appContext, RadioPlaybackService::class.java)
                .setAction(RadioPlaybackService.ACTION_SET_EMISSION),
        )
    }
}
