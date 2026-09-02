package com.lahoradelpartido.radiodelay.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lahoradelpartido.radiodelay.di.AppContainer
import com.lahoradelpartido.radiodelay.domain.model.Delay
import com.lahoradelpartido.radiodelay.domain.model.Emission
import com.lahoradelpartido.radiodelay.domain.model.RadioPlaybackState
import com.lahoradelpartido.radiodelay.domain.usecase.LoadEmissionsUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.ObserveRadioStateUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.PauseRadioUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.PlayRadioUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.ReturnToLiveUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.SelectEmissionUseCase
import com.lahoradelpartido.radiodelay.domain.usecase.SetDelayUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la lista de señales disponibles, independiente de la reproducción. */
data class EmissionsUiState(
    val emissions: List<Emission> = emptyList(),
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
)

data class RadioUiState(
    val playback: RadioPlaybackState = RadioPlaybackState(),
    val emissions: EmissionsUiState = EmissionsUiState(),
)

class RadioViewModel(
    observeRadioState: ObserveRadioStateUseCase,
    private val playRadio: PlayRadioUseCase,
    private val pauseRadio: PauseRadioUseCase,
    private val setDelay: SetDelayUseCase,
    private val returnToLive: ReturnToLiveUseCase,
    private val loadEmissions: LoadEmissionsUseCase,
    private val selectEmission: SelectEmissionUseCase,
    private val hasStoredEmission: Boolean,
) : ViewModel() {
    private val playbackState = observeRadioState()
    private val emissionsState = MutableStateFlow(EmissionsUiState())

    // La primera carga elige automáticamente la señal deportiva, que es lo que
    // el oyente busca al abrir la app. A partir de ahí manda siempre su elección.
    private var respectStoredChoice = hasStoredEmission

    val state: StateFlow<RadioUiState> =
        combine(playbackState, emissionsState) { playback, emissions ->
            RadioUiState(playback = playback, emissions = emissions)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RadioUiState(playback = playbackState.value),
        )

    init {
        refreshEmissions()
    }

    fun refreshEmissions() {
        if (emissionsState.value.isLoading) return
        emissionsState.update { it.copy(isLoading = true, loadFailed = false) }

        viewModelScope.launch {
            val result = runCatching { loadEmissions() }
            emissionsState.update {
                it.copy(
                    emissions = result.getOrDefault(it.emissions),
                    isLoading = false,
                    loadFailed = result.isFailure,
                )
            }

            val available = result.getOrNull().orEmpty()
            if (!respectStoredChoice && available.isNotEmpty()) {
                respectStoredChoice = true
                val preferred = available.firstOrNull(Emission::isSports) ?: available.first()
                selectEmission(preferred)
            }
        }
    }

    fun togglePlayback() {
        if (playbackState.value.isPlaying) pauseRadio() else playRadio()
    }

    fun chooseDelay(delay: Delay) = setDelay(delay)
    fun goLive() = returnToLive()

    fun chooseEmission(emission: Emission) {
        respectStoredChoice = true
        selectEmission(emission)
    }

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
                loadEmissions = container.loadEmissions,
                selectEmission = container.selectEmission,
                hasStoredEmission = container.hasStoredEmission,
            ) as T
        }
    }
}
