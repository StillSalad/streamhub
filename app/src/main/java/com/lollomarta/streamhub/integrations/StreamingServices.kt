package com.lollomarta.streamhub.integrations

import com.lollomarta.streamhub.integrations.Verification.STORE_LISTING
import com.lollomarta.streamhub.integrations.Verification.UNVERIFIED

/**
 * Catalogo iniziale dei servizi. Sono CANDIDATI, non integrazioni garantite:
 * vedi docs/INTEGRAZIONI.md per lo stato di ciascuno.
 * Per aggiungere un servizio basta aggiungere una voce qui e una regola in [ServiceMatcher].
 */
object StreamingServices {
    private fun p(name: String, v: Verification = STORE_LISTING) = PackageCandidate(name, v)

    val NETFLIX = StreamingService(
        id = "netflix", displayName = "Netflix", brandColor = 0xFFE50914,
        androidTvPackages = listOf(p("com.netflix.ninja")),
        fireTvPackages = listOf(p("com.netflix.ninja")),
        launcherLabels = listOf("Netflix"), officialSite = "netflix.com",
    )
    val PRIME_VIDEO = StreamingService(
        id = "prime_video", displayName = "Prime Video", brandColor = 0xFF1A98FF,
        androidTvPackages = listOf(p("com.amazon.amazonvideo.livingroom")),
        fireTvPackages = listOf(p("com.amazon.firebat", UNVERIFIED), p("com.amazon.avod.thirdpartyclient", UNVERIFIED)),
        launcherLabels = listOf("Prime Video"), officialSite = "primevideo.com",
        notes = "Su Fire TV Prime Video è integrato nel sistema: il package può variare.",
    )
    val DISNEY_PLUS = StreamingService(
        id = "disney_plus", displayName = "Disney+", brandColor = 0xFF113CCF,
        androidTvPackages = listOf(p("com.disney.disneyplus")),
        fireTvPackages = listOf(p("com.disney.disneyplus")),
        launcherLabels = listOf("Disney+", "Disney Plus"), officialSite = "disneyplus.com",
    )
    val APPLE_TV = StreamingService(
        id = "apple_tv", displayName = "Apple TV", brandColor = 0xFF2B2B2B,
        androidTvPackages = listOf(p("com.apple.atve.androidtv.appletv")),
        fireTvPackages = listOf(p("com.apple.atve.amazon.appletv")),
        launcherLabels = listOf("Apple TV"), officialSite = "tv.apple.com",
    )
    val PARAMOUNT_PLUS = StreamingService(
        id = "paramount_plus", displayName = "Paramount+", brandColor = 0xFF0064FF,
        androidTvPackages = listOf(p("com.cbs.ott", UNVERIFIED)),
        fireTvPackages = listOf(p("com.cbs.ott", UNVERIFIED)),
        launcherLabels = listOf("Paramount+", "Paramount Plus"), officialSite = "paramountplus.com",
        notes = "Il package internazionale può differire da quello USA: riconoscimento anche tramite etichetta.",
    )
    val NOW = StreamingService(
        id = "now", displayName = "NOW", brandColor = 0xFF00A88F,
        androidTvPackages = emptyList(), fireTvPackages = emptyList(),
        launcherLabels = listOf("NOW", "NOW TV"), officialSite = "nowtv.it",
        notes = "Package non verificato: l'app viene cercata solo tramite etichetta nel launcher.",
    )
    val DAZN = StreamingService(
        id = "dazn", displayName = "DAZN", brandColor = 0xFF1B1B1B,
        androidTvPackages = listOf(p("com.dazn")),
        fireTvPackages = listOf(p("com.dazn")),
        launcherLabels = listOf("DAZN"), officialSite = "dazn.com",
        notes = "Servizio prevalentemente sportivo: raramente presente nei dati film/serie di TMDb.",
    )
    val RAIPLAY = StreamingService(
        id = "raiplay", displayName = "RaiPlay", brandColor = 0xFF0A6CD6,
        androidTvPackages = emptyList(), fireTvPackages = emptyList(),
        launcherLabels = listOf("RaiPlay"), officialSite = "raiplay.it",
        notes = "Package non verificato: riconoscimento tramite etichetta nel launcher.",
    )
    val MEDIASET_INFINITY = StreamingService(
        id = "mediaset_infinity", displayName = "Mediaset Infinity", brandColor = 0xFF6B2BD9,
        androidTvPackages = emptyList(), fireTvPackages = emptyList(),
        launcherLabels = listOf("Mediaset Infinity", "Infinity"), officialSite = "mediasetinfinity.it",
        notes = "Package non verificato: riconoscimento tramite etichetta nel launcher.",
    )
    val YOUTUBE = StreamingService(
        id = "youtube", displayName = "YouTube", brandColor = 0xFFFF0000,
        androidTvPackages = listOf(p("com.google.android.youtube.tv"), p("com.google.android.youtube")),
        fireTvPackages = listOf(p("com.amazon.firetv.youtube")),
        launcherLabels = listOf("YouTube"), officialSite = "youtube.com",
        contentLinkSupport = ContentLinkSupport.YOUTUBE_VIDEO,
        notes = "I link https://www.youtube.com/watch?v=ID sono URL pubblici standard; usati per i trailer forniti da TMDb.",
    )

    val ALL: List<StreamingService> = listOf(
        NETFLIX, PRIME_VIDEO, DISNEY_PLUS, APPLE_TV, PARAMOUNT_PLUS,
        NOW, DAZN, RAIPLAY, MEDIASET_INFINITY, YOUTUBE,
    )

    fun byId(id: String?): StreamingService? = ALL.firstOrNull { it.id == id }
}

/** Associa i nomi dei provider TMDb (es. "Netflix basic with Ads") ai servizi. */
object ServiceMatcher {
    fun match(providerName: String): StreamingService? {
        val n = normalizeName(providerName)
        return when {
            n.startsWith("netflix") -> StreamingServices.NETFLIX
            n.startsWith("amazonprime") || n.startsWith("primevideo") || n == "amazonvideo" -> StreamingServices.PRIME_VIDEO
            n.startsWith("disney") -> StreamingServices.DISNEY_PLUS
            n.startsWith("appletv") -> StreamingServices.APPLE_TV
            n.startsWith("paramount") -> StreamingServices.PARAMOUNT_PLUS
            n == "now" || n.startsWith("nowtv") -> StreamingServices.NOW
            n.startsWith("dazn") -> StreamingServices.DAZN
            n.startsWith("raiplay") -> StreamingServices.RAIPLAY
            n.startsWith("mediasetinfinity") || n == "infinity" || n.startsWith("infinityplus") -> StreamingServices.MEDIASET_INFINITY
            n.startsWith("youtube") -> StreamingServices.YOUTUBE
            else -> null
        }
    }

    /** Un'etichetta del launcher corrisponde al servizio? Confronto esatto normalizzato. */
    fun labelMatches(service: StreamingService, appLabel: String): Boolean {
        val n = normalizeName(appLabel)
        return service.launcherLabels.any { normalizeName(it) == n }
    }
}
