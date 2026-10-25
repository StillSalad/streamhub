package com.lollomarta.streamhub.domain.logic

import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.SearchFilters
import com.lollomarta.streamhub.domain.model.SortOption

object FavoritesLogic {
    /** Aggiunge in cima o rimuove se già presente (confronto per chiave). */
    fun toggle(current: List<MediaItem>, item: MediaItem): List<MediaItem> =
        if (current.any { it.key == item.key }) current.filterNot { it.key == item.key }
        else listOf(item) + current

    fun contains(current: List<MediaItem>, key: String): Boolean = current.any { it.key == key }
}

object RecentSearchesLogic {
    const val MAX = 10

    fun add(current: List<String>, query: String, max: Int = MAX): List<String> {
        val q = SearchQuery.normalize(query) ?: return current
        return (listOf(q) + current.filterNot { it.equals(q, ignoreCase = true) }).take(max)
    }
}

object SearchQuery {
    const val MIN_LENGTH = 2

    /** Restituisce la query pulita o null se vuota/troppo corta. */
    fun normalize(raw: String): String? {
        val q = raw.trim().replace(Regex("\\s+"), " ")
        return if (q.length < MIN_LENGTH) null else q.take(100)
    }

    /** Filtri e ordinamento lato client per i risultati della ricerca testuale. */
    fun applyLocalFilters(items: List<MediaItem>, filters: SearchFilters): List<MediaItem> {
        val filtered = items
            .filter { filters.type == null || it.type == filters.type }
            .filter { filters.genreId == null || filters.genreId in it.genreIds }
            .distinctBy { it.key }
        return when (filters.sort) {
            SortOption.POPULARITY -> filtered.sortedByDescending { it.popularity }
            SortOption.RATING -> filtered.sortedByDescending { it.rating ?: 0.0 }
        }
    }

    fun effectiveDiscoverType(filters: SearchFilters): MediaType = filters.type ?: MediaType.MOVIE
}
