package com.tiempodemundial.radiodelay.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tiempodemundial.radiodelay.di.AppContainer
import com.tiempodemundial.radiodelay.domain.model.Delay
import com.tiempodemundial.radiodelay.domain.model.RadioPlaybackState
import com.tiempodemundial.radiodelay.domain.usecase.ObserveRadioStateUseCase
import com.tiempodemundial.radiodelay.domain.usecase.PauseRadioUseCase
import com.tiempodemundial.radiodelay.domain.usecase.PlayRadioUseCase
import com.tiempodemundial.radiodelay.domain.usecase.ReturnToLiveUseCase
import com.tiempodemundial.radiodelay.domain.usecase.SetDelayUseCase
import kotlinx.coroutines.flow.StateFlow

class RadioViewModel(
    observeRadioState: ObserveRadioStateUseCase,
    private val playRadio: PlayRadioUseCase,
    private val pauseRadio: PauseRadioUseCase,
    private val setDelay: SetDelayUseCase,
    private val returnToLive: ReturnToLiveUseCase,
) : ViewModel() {
    val state: StateFlow<RadioPlaybackState> = observeRadioState()

    fun togglePlayback() {
        if (state.value.isPlaying) pauseRadio() else playRadio()
    }

    fun chooseDelay(delay: Delay) = setDelay(delay)
    fun goLive() = returnToLive()

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RadioViewModel::class.java))
            return RadioViewModel(
                observeRadioState = container.observeRadioState,
                playRadio = container.playRadio,
                pauseRadio = container.pauseRadio,
                setDelay = container.setDelay,
                returnToLive = container.returnToLive,
            ) as T
        }
    }
}
