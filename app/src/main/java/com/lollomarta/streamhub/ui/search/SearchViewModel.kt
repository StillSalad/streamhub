package com.lollomarta.streamhub.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lollomarta.streamhub.AppContainer
import com.lollomarta.streamhub.domain.AppError
import com.lollomarta.streamhub.domain.logic.SearchQuery
import com.lollomarta.streamhub.domain.model.Genre
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.SearchFilters
import com.lollomarta.streamhub.domain.toAppError
import com.lollomarta.streamhub.integrations.ServiceMatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class SearchViewModel(private val c: AppContainer) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val error: AppError? = null,
        val results: List<MediaItem> = emptyList(),
        val isQuery: Boolean = false,
        val endReached: Boolean = false,
        val page: Int = 1,
        val notice: String? = null,
    )

    val query = MutableStateFlow("")
    val filters = MutableStateFlow(SearchFilters())
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()
    val recent = c.preferences.recentSearches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedServices = c.preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var searchJob: Job? = null
    var pendingRestoreKey: String? = null

    @OptIn(FlowPreview::class)
    private fun observe() = viewModelScope.launch {
        combine(query.debounce(450), filters) { q, f -> q to f }
            .distinctUntilChanged()
            .collect { (q, f) -> startSearch(q, f, page = 1) }
    }

    init {
        viewModelScope.launch {
            filters.value = c.preferences.savedFilters.first()
            observe()
            loadGenres()
        }
    }

    private suspend fun loadGenres() {
        _genres.value = runCatching {
            (c.catalog.genres(MediaType.MOVIE) + c.catalog.genres(MediaType.TV)).distinctBy { it.id }.sortedBy { it.name }
        }.getOrDefault(emptyList())
    }

    fun setQuery(q: String) { query.value = q }
    fun useRecent(q: String) { query.value = q }

    fun updateFilters(transform: (SearchFilters) -> SearchFilters) {
        filters.update(transform)
        viewModelScope.launch { c.preferences.saveFilters(filters.value) }
    }

    fun submit() {
        SearchQuery.normalize(query.value)?.let { q -> viewModelScope.launch { c.preferences.addRecentSearch(q) } }
    }

    fun clearRecent() { viewModelScope.launch { c.preferences.clearRecentSearches() } }

    fun retry() = startSearch(query.value, filters.value, 1)

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || s.endReached || s.error != null) return
        startSearch(query.value, filters.value, s.page + 1)
    }

    private fun startSearch(rawQuery: String, f: SearchFilters, page: Int) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { if (page == 1) it.copy(loading = true, error = null, notice = null) else it.copy(loadingMore = true) }
            try {
                val (items, notice) = fetch(rawQuery, f, page)
                _state.update { old ->
                    val merged = if (page == 1) items else (old.results + items).distinctBy { it.key }
                    UiState(
                        results = merged,
                        isQuery = SearchQuery.normalize(rawQuery) != null,
                        endReached = items.isEmpty() || page >= MAX_PAGES,
                        page = page,
                        notice = notice,
                    )
                }
                if (page == 1 && items.isNotEmpty()) SearchQuery.normalize(rawQuery)?.let { c.preferences.addRecentSearch(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadingMore = false, error = if (page == 1) e.toAppError() else null, endReached = page > 1) }
            }
        }
    }

    private suspend fun fetch(rawQuery: String, f: SearchFilters, page: Int): Pair<List<MediaItem>, String?> {
        val region = c.preferences.settings.first().region
        val q = SearchQuery.normalize(rawQuery)
        if (q == null) {
            // Nessuna query: esplorazione con filtri (discover).
            val type = SearchQuery.effectiveDiscoverType(f)
            val providerIds = f.serviceId?.let { serviceProviderIds(it, type, region) }
            if (providerIds != null && providerIds.isEmpty()) {
                return emptyList<MediaItem>() to "Nessun dato del catalogo per questo servizio nella tua area."
            }
            val items = c.catalog.discover(type, region, f.genreId, providerIds.orEmpty(), f.sort, page)
            val notice = if (f.type == null && page == 1) "Esplora: film. Scegli \"Serie\" per le serie TV." else null
            return items to notice
        }
        var items = SearchQuery.applyLocalFilters(c.catalog.search(q, page), f)
        if (f.serviceId != null) items = filterByService(items, f.serviceId, region)
        return items to null
    }

    private suspend fun serviceProviderIds(serviceId: String, type: MediaType, region: String): List<Int> =
        c.availability.regionProviders(type, region).filter { ServiceMatcher.match(it.name)?.id == serviceId }.map { it.id }

    /** Verifica la disponibilità solo sui primi risultati, con concorrenza limitata (rispetto dei limiti API). */
    private suspend fun filterByService(items: List<MediaItem>, serviceId: String, region: String): List<MediaItem> {
        val semaphore = Semaphore(4)
        val candidates = items.take(20)
        val keep = coroutineScope {
            candidates.map { item ->
                async {
                    semaphore.withPermit {
                        runCatching { c.availability.availability(item.type, item.id, region) }.getOrNull()
                            ?.offers?.any { ServiceMatcher.match(it.providerName)?.id == serviceId } == true
                    }
                }
            }.awaitAll()
        }
        return candidates.filterIndexed { i, _ -> keep[i] }
    }

    private companion object { const val MAX_PAGES = 10 }
}
