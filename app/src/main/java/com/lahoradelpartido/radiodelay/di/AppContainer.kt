package com.lahoradelpartido.radiodelay.di

import android.content.Context
import com.lahoradelpartido.radiodelay.data.catalog.CopeEmissionCatalog
import com.lahoradelpartido.radiodelay.data.controller.AndroidRadioController
import com.lahoradelpartido.radiodelay.data.controller.PlaybackStateStore
import com.lahoradelpartido.radiodelay.data.preferences.DelayPreferences
import com.lahoradelpartido.radiodelay.data.preferences.EmissionPreferences
import com.lahoradelpartido.radiodelay.data.preferences.SharedPreferencesDelayPreferences
import com.lahoradelpartido.radiodelay.data.preferences.SharedPreferencesEmissionPreferences
import com.lahoradelpartido.radiodelay.data.stream.CopeStreamClient
import com.lahoradelpartido.radiodelay.data.stream.Mp3CircularBuffer
import com.lahoradelpartido.radiodelay.domain.gateway.EmissionCatalog
import com.lahoradelpartido.radiodelay.domain.gateway.RadioController
import com.lahoradelpartido.radiodelay.domain.model.Emission
import com.lahoradelpartido.radiodelay.domain.usecase.LoadEmissionsUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.ObserveRadioStateUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.PauseRadioUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.PlayRadioUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.ReturnToLiveUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.SelectEmissionUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.SetDelayUseCase

/** Composition root: concrete dependencies are assembled only here. */
class AppContainer(context: Context) {
    val delayPreferences: DelayPreferences = SharedPreferencesDelayPreferences(context)
    val emissionPreferences: EmissionPreferences = SharedPreferencesEmissionPreferences(context)

    val stateStore = PlaybackStateStore(
        initialDelay = delayPreferences.read(),
        initialEmission = emissionPreferences.read() ?: Emission.NATIONAL,
    )

    val circularBuffer = Mp3CircularBuffer()
    val streamClient = CopeStreamClient(circularBuffer)

    private val emissionCatalog: EmissionCatalog = CopeEmissionCatalog()

    private val radioController: RadioController = AndroidRadioController(
        context = context,
        stateStore = stateStore,
        delayPreferences = delayPreferences,
        emissionPreferences = emissionPreferences,
    )

    /** Indica si el oyente ya eligió una señal alguna vez en este dispositivo. */
    val hasStoredEmission: Boolean get() = emissionPreferences.read() != null

    val observeRadioState = ObserveRadioStateUseCase(radioController)
    val playRadio = PlayRadioUseCase(radioController)
    val pauseRadio = PauseRadioUseCase(radioController)
    val setDelay = SetDelayUseCase(radioController)
    val returnToLive = ReturnToLiveUseCase(radioController)
    val loadEmissions = LoadEmissionsUseCase(emissionCatalog)
    val selectEmission = SelectEmissionUseCase(radioController)
}
