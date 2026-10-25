package com.lollomarta.streamhub.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lollomarta.streamhub.AppContainer
import com.lollomarta.streamhub.domain.AppError
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.UserSettings
import com.lollomarta.streamhub.domain.toAppError
import com.lollomarta.streamhub.integrations.InstallState
import com.lollomarta.streamhub.integrations.ServiceMatcher
import com.lollomarta.streamhub.integrations.WatchOption
import com.lollomarta.streamhub.integrations.WatchOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class HomeViewModel(private val c: AppContainer) : ViewModel() {

    data class Section(val id: String, val title: String, val items: List<MediaItem>)

    data class UiState(
        val loading: Boolean = true,
        val error: AppError? = null,
        val featured: MediaItem? = null,
        val featuredOption: WatchOption? = null,
        val sections: List<Section> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val favorites: StateFlow<List<MediaItem>> =
        c.preferences.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Elemento da rimettere a fuoco quando si torna dalla scheda (selezione preservata). */
    var pendingRestoreKey: String? = null
    var initialFocusDone = false

    private var loadJob: Job? = null

    init {
        // Ricarica quando cambiano regione, servizi o chiave API.
        viewModelScope.launch {
            combine(c.preferences.settings, c.preferences.hasUserApiKey) { s, key -> Triple(s.region, s.selectedServiceIds, key) }
                .distinctUntilChanged()
                .collect { load() }
        }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val settings = c.preferences.settings.first()
            val region = settings.region
            supervisorScope {
                val trending = async { runCatching { c.catalog.trending() } }
                val popular = async { runCatching { c.catalog.popularOnStreaming(MediaType.MOVIE, region) } }
                val moment = async { runCatching { c.catalog.trending(MediaType.MOVIE, daily = true) } }
                val tv = async { runCatching { c.catalog.popularOnStreaming(MediaType.TV, region) } }
                val news = async { runCatching { c.catalog.nowPlaying(region) } }
                val mine = async { runCatching { yourServices(settings) } }
                val discovery = async {
                    runCatching { (c.catalog.topRated(MediaType.MOVIE).take(10) + c.catalog.topRated(MediaType.TV).take(10)).shuffled() }
                }

                val trendingRes = trending.await()
                val featured = trendingRes.getOrNull()?.firstOrNull { it.backdropUrl != null }
                val seed = favorites.value.firstOrNull() ?: featured
                val recommended = seed?.let { runCatching { c.catalog.recommendations(it) } } ?: Result.success(emptyList())

                val results = listOf(
                    "featured" to ("In evidenza" to trendingRes),
                    "popular" to ("Popolari in Italia" to popular.await()),
                    "moment" to ("Film del momento" to moment.await()),
                    "tv" to ("Serie TV popolari" to tv.await()),
                    "news" to ("Novità" to news.await()),
                    "mine" to ("Disponibili sui tuoi servizi" to mine.await()),
                    "recommended" to ("Consigliati" to recommended),
                    "discovery" to ("Continua la scoperta" to discovery.await()),
                )
                val sections = results.mapNotNull { (id, pair) ->
                    val items = pair.second.getOrNull().orEmpty()
                    if (items.isEmpty()) null else Section(id, if (id == "popular" && region != "IT") "Popolari ($region)" else pair.first, items)
                }
                val firstError = results.firstNotNullOfOrNull { it.second.second.exceptionOrNull() }?.toAppError()
                _state.update {
                    UiState(
                        loading = false,
                        error = if (sections.isEmpty()) firstError ?: AppError.Unknown("Nessun dato") else null,
                        featured = featured,
                        sections = sections,
                    )
                }
                featured?.let { loadFeaturedOption(it, settings) }
            }
        }
    }

    private suspend fun yourServices(s: UserSettings): List<MediaItem> {
        if (s.selectedServiceIds.isEmpty()) return emptyList()
        val ids = c.availability.regionProviders(MediaType.MOVIE, s.region)
            .filter { ServiceMatcher.match(it.name)?.id in s.selectedServiceIds }
            .map { it.id }
        if (ids.isEmpty()) return emptyList()
        return c.catalog.discover(MediaType.MOVIE, s.region, providerIds = ids)
    }

    /** "Guarda" nell'area hero solo se esiste una destinazione verificata dai dati. */
    private suspend fun loadFeaturedOption(item: MediaItem, s: UserSettings) {
        val availability = runCatching { c.availability.availability(item.type, item.id, s.region) }.getOrNull() ?: return
        val option = withContext(Dispatchers.Default) {
            WatchOptions.primary(
                WatchOptions.build(availability, s.selectedServiceIds, s.preferredServiceId, { svc ->
                    c.launchManager.installState(svc.id) == InstallState.INSTALLED
                }),
            )
        }
        _state.update { if (it.featured?.key == item.key) it.copy(featuredOption = option) else it }
    }
}
