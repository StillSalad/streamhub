package com.lollomarta.streamhub.domain.repository

import com.lollomarta.streamhub.domain.model.Availability
import com.lollomarta.streamhub.domain.model.CatalogProvider
import com.lollomarta.streamhub.domain.model.Genre
import com.lollomarta.streamhub.domain.model.MediaDetails
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.SearchFilters
import com.lollomarta.streamhub.domain.model.SortOption
import com.lollomarta.streamhub.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    suspend fun trending(type: MediaType? = null, daily: Boolean = false): List<MediaItem>
    suspend fun popularOnStreaming(type: MediaType, region: String): List<MediaItem>
    suspend fun nowPlaying(region: String): List<MediaItem>
    suspend fun topRated(type: MediaType): List<MediaItem>
    suspend fun recommendations(item: MediaItem): List<MediaItem>
    suspend fun search(query: String, page: Int = 1): List<MediaItem>
    suspend fun discover(
        type: MediaType,
        region: String,
        genreId: Int? = null,
        providerIds: List<Int> = emptyList(),
        sort: SortOption = SortOption.POPULARITY,
        page: Int = 1,
    ): List<MediaItem>
    suspend fun genres(type: MediaType): List<Genre>
    suspend fun details(type: MediaType, id: Int, region: String): MediaDetails
    suspend fun validateKey(): Boolean
}

interface ProviderAvailabilityRepository {
    suspend fun availability(type: MediaType, id: Int, region: String): Availability
    suspend fun regionProviders(type: MediaType, region: String): List<CatalogProvider>
}

interface PreferencesRepository {
    val settings: Flow<UserSettings>
    val favorites: Flow<List<MediaItem>>
    val recentSearches: Flow<List<String>>
    val savedFilters: Flow<SearchFilters>
    val hasUserApiKey: Flow<Boolean>

    suspend fun toggleFavorite(item: MediaItem)
    suspend fun setRegion(region: String)
    suspend fun setReduceMotion(enabled: Boolean)
    suspend fun toggleService(serviceId: String)
    suspend fun setPreferredService(serviceId: String?)
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()
    suspend fun saveFilters(filters: SearchFilters)
    suspend fun apiKey(): String?
    suspend fun setApiKey(key: String?)
    suspend fun resetAll()
}
