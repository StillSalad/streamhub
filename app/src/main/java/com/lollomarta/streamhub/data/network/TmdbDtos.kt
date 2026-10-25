package com.lollomarta.streamhub.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val TmdbJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    isLenient = true
}

@Serializable
data class PagedDto<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class MediaDto(
    val id: Int = 0,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val popularity: Double? = null,
    @SerialName("media_type") val mediaType: String? = null,
    val adult: Boolean = false,
)

@Serializable data class GenreDto(val id: Int = 0, val name: String? = null)
@Serializable data class GenreListDto(val genres: List<GenreDto> = emptyList())

@Serializable
data class CastDto(
    val name: String? = null,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = 999,
)
@Serializable data class CreditsDto(val cast: List<CastDto> = emptyList())

@Serializable
data class VideoDto(
    val key: String? = null,
    val site: String? = null,
    val type: String? = null,
    val official: Boolean = false,
    @SerialName("iso_639_1") val language: String? = null,
)
@Serializable data class VideosDto(val results: List<VideoDto> = emptyList())

@Serializable
data class SeasonDto(
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("air_date") val airDate: String? = null,
)

@Serializable
data class ProviderDto(
    @SerialName("provider_id") val providerId: Int = 0,
    @SerialName("provider_name") val providerName: String? = null,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("display_priority") val displayPriority: Int? = null,
)

@Serializable
data class RegionProvidersDto(
    val link: String? = null,
    val flatrate: List<ProviderDto> = emptyList(),
    val free: List<ProviderDto> = emptyList(),
    val ads: List<ProviderDto> = emptyList(),
    val rent: List<ProviderDto> = emptyList(),
    val buy: List<ProviderDto> = emptyList(),
)

@Serializable data class WatchProvidersDto(val results: Map<String, RegionProvidersDto> = emptyMap())
@Serializable data class ProviderListDto(val results: List<ProviderDto> = emptyList())
@Serializable data class AuthDto(val success: Boolean = false)

@Serializable
data class DetailsDto(
    val id: Int = 0,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val popularity: Double? = null,
    val runtime: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    val seasons: List<SeasonDto> = emptyList(),
    val credits: CreditsDto? = null,
    val videos: VideosDto? = null,
    @SerialName("watch/providers") val watchProviders: WatchProvidersDto? = null,
)
