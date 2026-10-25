package com.lollomarta.streamhub.data.catalog

import com.lollomarta.streamhub.data.network.AuthDto
import com.lollomarta.streamhub.data.network.DetailsDto
import com.lollomarta.streamhub.data.network.GenreListDto
import com.lollomarta.streamhub.data.network.MediaDto
import com.lollomarta.streamhub.data.network.PagedDto
import com.lollomarta.streamhub.data.network.ProviderListDto
import com.lollomarta.streamhub.data.network.TmdbClient
import com.lollomarta.streamhub.data.network.TmdbJson
import com.lollomarta.streamhub.data.network.TmdbMappers
import com.lollomarta.streamhub.data.network.WatchProvidersDto
import com.lollomarta.streamhub.domain.AppError
import com.lollomarta.streamhub.domain.model.Availability
import com.lollomarta.streamhub.domain.model.CatalogProvider
import com.lollomarta.streamhub.domain.model.Genre
import com.lollomarta.streamhub.domain.model.MediaDetails
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.SortOption
import com.lollomarta.streamhub.domain.repository.CatalogRepository
import com.lollomarta.streamhub.domain.repository.ProviderAvailabilityRepository
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import java.util.concurrent.ConcurrentHashMap

private const val LANG = "it-IT"

private inline fun <reified T> decode(body: String): T = try {
    TmdbJson.decodeFromString<T>(body)
} catch (e: SerializationException) {
    throw AppError.Parsing(e.message ?: "json")
} catch (e: IllegalArgumentException) {
    throw AppError.Parsing(e.message ?: "json")
}

class TmdbCatalogRepository(private val client: TmdbClient) : CatalogRepository {
    private val genreCache = ConcurrentHashMap<MediaType, List<Genre>>()

    private suspend fun list(path: String, type: MediaType?, params: Map<String, String?> = emptyMap()): List<MediaItem> =
        TmdbMappers.toItems(decode<PagedDto<MediaDto>>(client.get(path, params + ("language" to LANG))), type)

    override suspend fun trending(type: MediaType?, daily: Boolean): List<MediaItem> =
        list("trending/${type?.apiPath ?: "all"}/${if (daily) "day" else "week"}", type)

    override suspend fun popularOnStreaming(type: MediaType, region: String): List<MediaItem> =
        list(
            "discover/${type.apiPath}", type,
            mapOf(
                "watch_region" to region,
                "with_watch_monetization_types" to "flatrate|free|ads",
                "sort_by" to "popularity.desc",
                "include_adult" to "false",
            ),
        )

    override suspend fun nowPlaying(region: String) = list("movie/now_playing", MediaType.MOVIE, mapOf("region" to region))

    override suspend fun topRated(type: MediaType) = list("${type.apiPath}/top_rated", type)

    override suspend fun recommendations(item: MediaItem) =
        list("${item.type.apiPath}/${item.id}/recommendations", item.type)

    override suspend fun search(query: String, page: Int) =
        list("search/multi", null, mapOf("query" to query, "page" to page.toString(), "include_adult" to "false"))

    override suspend fun discover(
        type: MediaType, region: String, genreId: Int?, providerIds: List<Int>, sort: SortOption, page: Int,
    ): List<MediaItem> {
        val params = mutableMapOf<String, String?>(
            "sort_by" to if (sort == SortOption.RATING) "vote_average.desc" else "popularity.desc",
            "page" to page.toString(),
            "include_adult" to "false",
            "watch_region" to region,
            "with_genres" to genreId?.toString(),
        )
        if (sort == SortOption.RATING) params["vote_count.gte"] = "300"
        if (providerIds.isNotEmpty()) {
            params["with_watch_providers"] = providerIds.joinToString("|")
            params["with_watch_monetization_types"] = "flatrate|free|ads"
        }
        return list("discover/${type.apiPath}", type, params)
    }

    override suspend fun genres(type: MediaType): List<Genre> = genreCache[type] ?: run {
        val dto = decode<GenreListDto>(client.get("genre/${type.apiPath}/list", mapOf("language" to LANG)))
        dto.genres.mapNotNull { g -> g.name?.let { Genre(g.id, it.replaceFirstChar(Char::uppercase)) } }
            .also { genreCache[type] = it }
    }

    override suspend fun details(type: MediaType, id: Int, region: String): MediaDetails {
        val body = client.get(
            "${type.apiPath}/$id",
            mapOf(
                "language" to LANG,
                "append_to_response" to "credits,videos,watch/providers",
                "include_video_language" to "it,en,null",
            ),
        )
        return TmdbMappers.toDetails(decode<DetailsDto>(body), type, region)
    }

    override suspend fun validateKey(): Boolean = decode<AuthDto>(client.get("authentication")).success
}

class TmdbProviderAvailabilityRepository(private val client: TmdbClient) : ProviderAvailabilityRepository {
    private val providerCache = ConcurrentHashMap<String, List<CatalogProvider>>()
    private val availabilityCache = ConcurrentHashMap<String, Availability>()

    override suspend fun availability(type: MediaType, id: Int, region: String): Availability {
        val key = "${type.apiPath}-$id-$region"
        availabilityCache[key]?.let { return it }
        val dto = decode<WatchProvidersDto>(client.get("${type.apiPath}/$id/watch/providers"))
        return TmdbMappers.toAvailability(dto, region).also { availabilityCache[key] = it }
    }

    override suspend fun regionProviders(type: MediaType, region: String): List<CatalogProvider> {
        val key = "${type.apiPath}-$region"
        providerCache[key]?.let { return it }
        val dto = decode<ProviderListDto>(
            client.get("watch/providers/${type.apiPath}", mapOf("watch_region" to region, "language" to LANG)),
        )
        return TmdbMappers.toProviders(dto).also { providerCache[key] = it }
    }
}
