package com.lollomarta.streamhub.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lollomarta.streamhub.AppContainer
import com.lollomarta.streamhub.domain.AppError
import com.lollomarta.streamhub.domain.logic.FavoritesLogic
import com.lollomarta.streamhub.domain.model.MediaDetails
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.toAppError
import com.lollomarta.streamhub.integrations.InstallState
import com.lollomarta.streamhub.integrations.WatchOption
import com.lollomarta.streamhub.integrations.WatchOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailsViewModel(private val c: AppContainer, private val type: MediaType, private val id: Int) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val error: AppError? = null,
        val details: MediaDetails? = null,
        val options: List<WatchOption> = emptyList(),
        val isFavorite: Boolean = false,
    ) {
        val primary: WatchOption? get() = WatchOptions.primary(options)
    }

    private data class Load(val loading: Boolean = true, val error: AppError? = null, val details: MediaDetails? = null)

    private val load = MutableStateFlow(Load())
    private val refreshTick = MutableStateFlow(0)

    val state: StateFlow<UiState> = combine(load, c.preferences.settings, c.preferences.favorites, refreshTick) { l, s, favs, _ ->
        val d = l.details
        UiState(
            loading = l.loading,
            error = l.error,
            details = d,
            options = d?.let {
                WatchOptions.build(it.availability, s.selectedServiceIds, s.preferredServiceId, { svc ->
                    c.launchManager.installState(svc.id) == InstallState.INSTALLED
                })
            }.orEmpty(),
            isFavorite = d != null && FavoritesLogic.contains(favs, d.item.key),
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            load.value = load.value.copy(loading = true, error = null)
            val region = c.preferences.settings.first().region
            load.value = try {
                Load(loading = false, details = c.catalog.details(type, id, region))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Load(loading = false, error = e.toAppError())
            }
        }
    }

    /** Al ritorno da un'altra app aggiorniamo lo stato di installazione senza ricaricare. */
    fun refreshInstallState() { refreshTick.value++ }

    fun toggleFavorite() {
        val item = load.value.details?.item ?: return
        viewModelScope.launch { c.preferences.toggleFavorite(item) }
    }
}
