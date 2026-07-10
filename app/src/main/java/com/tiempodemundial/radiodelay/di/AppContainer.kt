package com.tiempodemundial.radiodelay.di

import android.content.Context
import com.tiempodemundial.radiodelay.data.controller.AndroidRadioController
import com.tiempodemundial.radiodelay.data.controller.PlaybackStateStore
import com.tiempodemundial.radiodelay.data.preferences.DelayPreferences
import com.tiempodemundial.radiodelay.data.preferences.SharedPreferencesDelayPreferences
import com.tiempodemundial.radiodelay.data.stream.CopeStreamClient
import com.tiempodemundial.radiodelay.data.stream.Mp3CircularBuffer
import com.tiempodemundial.radiodelay.domain.gateway.RadioController
import com.tiempodemundial.radiodelay.domain.usecase.ObserveRadioStateUseCase
import com.tiempodemundial.radiodelay.domain.usecase.PauseRadioUseCase
import com.tiempodemundial.radiodelay.domain.usecase.PlayRadioUseCase
import com.tiempodemundial.radiodelay.domain.usecase.ReturnToLiveUseCase
import com.tiempodemundial.radiodelay.domain.usecase.SetDelayUseCase

/** Composition root: concrete dependencies are assembled only here. */
class AppContainer(context: Context) {
    val delayPreferences: DelayPreferences = SharedPreferencesDelayPreferences(context)
    val stateStore = PlaybackStateStore(delayPreferences.read())
    val circularBuffer = Mp3CircularBuffer()
    val streamClient = CopeStreamClient(circularBuffer)

    private val radioController: RadioController = AndroidRadioController(
        context = context,
        stateStore = stateStore,
        delayPreferences = delayPreferences,
    )

    val observeRadioState = ObserveRadioStateUseCase(radioController)
    val playRadio = PlayRadioUseCase(radioController)
    val pauseRadio = PauseRadioUseCase(radioController)
    val setDelay = SetDelayUseCase(radioController)
    val returnToLive = ReturnToLiveUseCase(radioController)
}
