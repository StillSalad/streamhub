package com.lollomarta.streamhub.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lollomarta.streamhub.domain.logic.FavoritesLogic
import com.lollomarta.streamhub.domain.logic.RecentSearchesLogic
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.SearchFilters
import com.lollomarta.streamhub.domain.model.SortOption
import com.lollomarta.streamhub.domain.model.UserSettings
import com.lollomarta.streamhub.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "streamhub_prefs")

@Serializable
private data class StoredItem(
    val id: Int, val type: String, val title: String,
    val posterUrl: String? = null, val backdropUrl: String? = null,
    val year: String? = null, val rating: Double? = null, val overview: String = "",
)

@Serializable
private data class StoredFilters(val type: String? = null, val genreId: Int? = null, val serviceId: String? = null, val sort: String = "POPULARITY")

/** Tutti i dati restano sul dispositivo: nessun account, nessun cloud. */
class DataStorePreferencesRepository(
    context: Context,
    private val cipher: KeystoreCipher = KeystoreCipher(),
) : PreferencesRepository {

    private val store = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val region = stringPreferencesKey("region")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val services = stringSetPreferencesKey("selected_services")
        val preferred = stringPreferencesKey("preferred_service")
        val favorites = stringPreferencesKey("favorites_json")
        val recent = stringPreferencesKey("recent_searches_json")
        val filters = stringPreferencesKey("saved_filters_json")
        val apiKey = stringPreferencesKey("tmdb_api_key_enc")
    }

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val settings: Flow<UserSettings> = data.map { p ->
        UserSettings(
            region = p[Keys.region] ?: "IT",
            reduceMotion = p[Keys.reduceMotion] ?: false,
            selectedServiceIds = p[Keys.services] ?: emptySet(),
            preferredServiceId = p[Keys.preferred],
        )
    }

    override val favorites: Flow<List<MediaItem>> = data.map { decodeItems(it[Keys.favorites]) }
    override val recentSearches: Flow<List<String>> = data.map { decodeStrings(it[Keys.recent]) }
    override val savedFilters: Flow<SearchFilters> = data.map { decodeFilters(it[Keys.filters]) }
    override val hasUserApiKey: Flow<Boolean> = data.map { !it[Keys.apiKey].isNullOrBlank() }

    override suspend fun toggleFavorite(item: MediaItem) {
        store.edit { p -> p[Keys.favorites] = encodeItems(FavoritesLogic.toggle(decodeItems(p[Keys.favorites]), item)) }
    }

    override suspend fun setRegion(region: String) { store.edit { it[Keys.region] = region } }
    override suspend fun setReduceMotion(enabled: Boolean) { store.edit { it[Keys.reduceMotion] = enabled } }

    override suspend fun toggleService(serviceId: String) {
        store.edit { p ->
            val current = p[Keys.services] ?: emptySet()
            p[Keys.services] = if (serviceId in current) current - serviceId else current + serviceId
        }
    }

    override suspend fun setPreferredService(serviceId: String?) {
        store.edit { p -> if (serviceId == null) p.remove(Keys.preferred) else p[Keys.preferred] = serviceId }
    }

    override suspend fun addRecentSearch(query: String) {
        store.edit { p -> p[Keys.recent] = encodeStrings(RecentSearchesLogic.add(decodeStrings(p[Keys.recent]), query)) }
    }

    override suspend fun clearRecentSearches() { store.edit { it.remove(Keys.recent) } }

    override suspend fun saveFilters(filters: SearchFilters) {
        val stored = StoredFilters(filters.type?.name, filters.genreId, filters.serviceId, filters.sort.name)
        store.edit { it[Keys.filters] = json.encodeToString(StoredFilters.serializer(), stored) }
    }

    override suspend fun apiKey(): String? = data.first()[Keys.apiKey]?.let { cipher.decrypt(it) }

    override suspend fun setApiKey(key: String?) {
        val clean = key?.trim().orEmpty()
        store.edit { p ->
            val enc = if (clean.isEmpty()) null else cipher.encrypt(clean)
            if (enc == null) p.remove(Keys.apiKey) else p[Keys.apiKey] = enc
        }
    }

    override suspend fun resetAll() { store.edit { it.clear() } }

    private fun decodeItems(raw: String?): List<MediaItem> = runCatching {
        if (raw.isNullOrBlank()) emptyList()
        else json.decodeFromString(ListSerializer(StoredItem.serializer()), raw).mapNotNull { s ->
            val type = runCatching { MediaType.valueOf(s.type) }.getOrNull() ?: return@mapNotNull null
            MediaItem(s.id, type, s.title, s.overview, s.posterUrl, s.backdropUrl, s.year, s.rating)
        }
    }.getOrDefault(emptyList())

    private fun encodeItems(items: List<MediaItem>): String = json.encodeToString(
        ListSerializer(StoredItem.serializer()),
        items.map { StoredItem(it.id, it.type.name, it.title, it.posterUrl, it.backdropUrl, it.year, it.rating, it.overview.take(400)) },
    )

    private fun decodeStrings(raw: String?): List<String> = runCatching {
        if (raw.isNullOrBlank()) emptyList() else json.decodeFromString(ListSerializer(String.serializer()), raw)
    }.getOrDefault(emptyList())

    private fun encodeStrings(list: List<String>) = json.encodeToString(ListSerializer(String.serializer()), list)

    private fun decodeFilters(raw: String?): SearchFilters = runCatching {
        if (raw.isNullOrBlank()) SearchFilters()
        else json.decodeFromString(StoredFilters.serializer(), raw).let { s ->
            SearchFilters(
                type = s.type?.let { runCatching { MediaType.valueOf(it) }.getOrNull() },
                genreId = s.genreId,
                serviceId = s.serviceId,
                sort = runCatching { SortOption.valueOf(s.sort) }.getOrDefault(SortOption.POPULARITY),
            )
        }
    }.getOrDefault(SearchFilters())
}
