package com.lollomarta.streamhub

import android.app.Application
import android.content.Context
import com.lollomarta.streamhub.data.catalog.TmdbCatalogRepository
import com.lollomarta.streamhub.data.catalog.TmdbProviderAvailabilityRepository
import com.lollomarta.streamhub.data.network.TmdbClient
import com.lollomarta.streamhub.data.preferences.DataStorePreferencesRepository
import com.lollomarta.streamhub.domain.repository.CatalogRepository
import com.lollomarta.streamhub.domain.repository.PreferencesRepository
import com.lollomarta.streamhub.domain.repository.ProviderAvailabilityRepository
import com.lollomarta.streamhub.integrations.AndroidDeviceEnvironment
import com.lollomarta.streamhub.integrations.DefaultStreamingLaunchManager
import com.lollomarta.streamhub.integrations.StreamingLaunchManager
import okhttp3.Cache
import java.io.File

/** Dependency injection manuale: semplice da leggere, nessuna libreria in più. */
class AppContainer(context: Context) {
    val preferences: PreferencesRepository = DataStorePreferencesRepository(context)

    private val http = TmdbClient.buildHttpClient(Cache(File(context.cacheDir, "tmdb_http"), 20L * 1024 * 1024))

    private val tmdb = TmdbClient(
        http = http,
        apiKeyProvider = { preferences.apiKey() ?: BuildConfig.TMDB_API_KEY.takeIf { it.isNotBlank() } },
    )

    val catalog: CatalogRepository = TmdbCatalogRepository(tmdb)
    val availability: ProviderAvailabilityRepository = TmdbProviderAvailabilityRepository(tmdb)
    val deviceEnvironment = AndroidDeviceEnvironment(context)
    val launchManager: StreamingLaunchManager = DefaultStreamingLaunchManager(deviceEnvironment)
}

class StreamHubApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
