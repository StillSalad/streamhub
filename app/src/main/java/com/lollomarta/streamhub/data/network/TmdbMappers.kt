package com.lollomarta.streamhub.data.network

import com.lollomarta.streamhub.domain.model.Availability
import com.lollomarta.streamhub.domain.model.CastMember
import com.lollomarta.streamhub.domain.model.CatalogProvider
import com.lollomarta.streamhub.domain.model.Genre
import com.lollomarta.streamhub.domain.model.MediaDetails
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.OfferType
import com.lollomarta.streamhub.domain.model.ProviderOffer
import com.lollomarta.streamhub.domain.model.SeasonInfo

object TmdbImages {
    private const val BASE = "https://image.tmdb.org/t/p/"
    fun poster(path: String?) = path?.takeIf { it.isNotBlank() }?.let { "${BASE}w342$it" }
    fun backdrop(path: String?) = path?.takeIf { it.isNotBlank() }?.let { "${BASE}w1280$it" }
    fun profile(path: String?) = path?.takeIf { it.isNotBlank() }?.let { "${BASE}w185$it" }
    fun logo(path: String?) = path?.takeIf { it.isNotBlank() }?.let { "${BASE}w92$it" }
}

object TmdbMappers {
    private fun year(date: String?) = date?.takeIf { it.length >= 4 }?.take(4)

    /** Restituisce null per persone, voci senza titolo o tipo sconosciuto: niente dati inventati. */
    fun toItem(dto: MediaDto, fallbackType: MediaType? = null): MediaItem? {
        if (dto.adult || dto.id <= 0) return null
        val type = MediaType.fromApi(dto.mediaType) ?: if (dto.mediaType == null) fallbackType else null
        type ?: return null
        val title = (if (type == MediaType.MOVIE) dto.title ?: dto.name else dto.name ?: dto.title)
            ?.takeIf { it.isNotBlank() } ?: return null
        return MediaItem(
            id = dto.id,
            type = type,
            title = title,
            overview = dto.overview.orEmpty(),
            posterUrl = TmdbImages.poster(dto.posterPath),
            backdropUrl = TmdbImages.backdrop(dto.backdropPath),
            year = year(if (type == MediaType.MOVIE) dto.releaseDate else dto.firstAirDate),
            rating = dto.voteAverage?.takeIf { it > 0.0 },
            genreIds = dto.genreIds,
            popularity = dto.popularity ?: 0.0,
        )
    }

    fun toItems(page: PagedDto<MediaDto>, fallbackType: MediaType? = null): List<MediaItem> =
        page.results.mapNotNull { toItem(it, fallbackType) }.distinctBy { it.key }

    fun toAvailability(dto: WatchProvidersDto?, region: String): Availability {
        val r = dto?.results?.get(region) ?: return Availability(region)
        fun List<ProviderDto>.map(type: OfferType) = mapNotNull { p ->
            val name = p.providerName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ProviderOffer(p.providerId, name, TmdbImages.logo(p.logoPath), type, p.displayPriority ?: Int.MAX_VALUE)
        }
        val offers = r.flatrate.map(OfferType.SUBSCRIPTION) + r.free.map(OfferType.FREE) +
            r.ads.map(OfferType.ADS) + r.rent.map(OfferType.RENT) + r.buy.map(OfferType.BUY)
        return Availability(region, offers, r.link)
    }

    fun toProviders(dto: ProviderListDto): List<CatalogProvider> = dto.results.mapNotNull { p ->
        p.providerName?.let { CatalogProvider(p.providerId, it, TmdbImages.logo(p.logoPath)) }
    }

    fun pickTrailer(videos: VideosDto?): String? {
        val yt = videos?.results.orEmpty().filter { it.site.equals("YouTube", true) && !it.key.isNullOrBlank() }
        return (yt.firstOrNull { it.type == "Trailer" && it.official && it.language == "it" }
            ?: yt.firstOrNull { it.type == "Trailer" && it.language == "it" }
            ?: yt.firstOrNull { it.type == "Trailer" && it.official }
            ?: yt.firstOrNull { it.type == "Trailer" }
            ?: yt.firstOrNull { it.type == "Teaser" })?.key
    }

    fun toDetails(dto: DetailsDto, type: MediaType, region: String): MediaDetails {
        val title = (dto.title ?: dto.name)?.takeIf { it.isNotBlank() } ?: "Titolo non disponibile"
        val item = MediaItem(
            id = dto.id, type = type, title = title,
            overview = dto.overview.orEmpty(),
            posterUrl = TmdbImages.poster(dto.posterPath),
            backdropUrl = TmdbImages.backdrop(dto.backdropPath),
            year = year(if (type == MediaType.MOVIE) dto.releaseDate else dto.firstAirDate),
            rating = dto.voteAverage?.takeIf { it > 0.0 },
            genreIds = dto.genres.map { it.id },
            popularity = dto.popularity ?: 0.0,
        )
        return MediaDetails(
            item = item,
            tagline = dto.tagline?.takeIf { it.isNotBlank() },
            runtimeMinutes = dto.runtime?.takeIf { it > 0 } ?: dto.episodeRunTime.firstOrNull { it > 0 },
            genres = dto.genres.mapNotNull { g -> g.name?.let { Genre(g.id, it) } },
            cast = dto.credits?.cast.orEmpty().sortedBy { it.order }.take(15)
                .mapNotNull { c -> c.name?.let { CastMember(it, c.character?.takeIf { s -> s.isNotBlank() }, TmdbImages.profile(c.profilePath)) } },
            seasons = dto.seasons.filter { it.seasonNumber > 0 }
                .map { SeasonInfo(it.seasonNumber, it.name ?: "Stagione ${it.seasonNumber}", it.episodeCount, year(it.airDate)) },
            numberOfSeasons = dto.numberOfSeasons,
            trailerYoutubeKey = pickTrailer(dto.videos),
            availability = toAvailability(dto.watchProviders, region),
        )
    }
}
