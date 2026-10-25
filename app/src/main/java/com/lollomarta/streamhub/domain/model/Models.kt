package com.lollomarta.streamhub.domain.model

enum class MediaType(val apiPath: String) {
    MOVIE("movie"), TV("tv");

    companion object {
        fun fromApi(value: String?): MediaType? = when (value) {
            "movie" -> MOVIE
            "tv" -> TV
            else -> null
        }
    }
}

data class MediaItem(
    val id: Int,
    val type: MediaType,
    val title: String,
    val overview: String = "",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: String? = null,
    val rating: Double? = null,
    val genreIds: List<Int> = emptyList(),
    val popularity: Double = 0.0,
) {
    /** Chiave stabile per liste, focus e preferiti. */
    val key: String get() = "${type.apiPath}-$id"
}

data class Genre(val id: Int, val name: String)

data class CastMember(val name: String, val character: String?, val profileUrl: String?)

data class SeasonInfo(val number: Int, val name: String, val episodeCount: Int?, val airYear: String?)

/** Tipo di offerta secondo i dati del catalogo. Non va mai confuso tra abbonamento e pagamento. */
enum class OfferType(val label: String, val rank: Int) {
    SUBSCRIPTION("In abbonamento", 0),
    FREE("Gratis", 0),
    ADS("Gratis con pubblicità", 1),
    RENT("Noleggio", 2),
    BUY("Acquisto", 3),
}

data class ProviderOffer(
    val providerId: Int,
    val providerName: String,
    val logoUrl: String?,
    val type: OfferType,
    val displayPriority: Int = Int.MAX_VALUE,
)

data class Availability(
    val region: String,
    val offers: List<ProviderOffer> = emptyList(),
    /** Pagina TMDb/JustWatch del titolo: NON è un deep link alla riproduzione. */
    val link: String? = null,
) {
    val isEmpty: Boolean get() = offers.isEmpty()
}

/** Provider disponibile in una regione secondo TMDb (/watch/providers). */
data class CatalogProvider(val id: Int, val name: String, val logoUrl: String?)

data class MediaDetails(
    val item: MediaItem,
    val tagline: String? = null,
    val runtimeMinutes: Int? = null,
    val genres: List<Genre> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val seasons: List<SeasonInfo> = emptyList(),
    val numberOfSeasons: Int? = null,
    val trailerYoutubeKey: String? = null,
    val availability: Availability,
)

enum class SortOption(val label: String) { POPULARITY("Popolarità"), RATING("Valutazione") }

data class SearchFilters(
    val type: MediaType? = null,
    val genreId: Int? = null,
    val serviceId: String? = null,
    val sort: SortOption = SortOption.POPULARITY,
)

data class UserSettings(
    val region: String = "IT",
    val reduceMotion: Boolean = false,
    val selectedServiceIds: Set<String> = emptySet(),
    val preferredServiceId: String? = null,
)
