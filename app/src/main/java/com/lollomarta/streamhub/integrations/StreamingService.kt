package com.lollomarta.streamhub.integrations

/**
 * Livello di verifica di un'informazione di integrazione.
 * Nessun package o link viene dichiarato "verificato" senza una fonte ufficiale.
 */
enum class Verification(val label: String) {
    /** Documentato pubblicamente dal fornitore. */
    OFFICIAL("Documentato dal fornitore"),
    /** Identificativo pubblico dello store, non testato da noi sul dispositivo. */
    STORE_LISTING("Da store pubblico, da verificare sul dispositivo"),
    /** Non verificato: usato solo come tentativo, con fallback. */
    UNVERIFIED("Non verificato"),
}

data class PackageCandidate(val packageName: String, val verification: Verification)

enum class Platform { ANDROID_TV, FIRE_TV }

data class StreamingService(
    val id: String,
    val displayName: String,
    /** Colore per l'icona testuale (nessun logo generato artificialmente). */
    val brandColor: Long,
    val androidTvPackages: List<PackageCandidate>,
    val fireTvPackages: List<PackageCandidate>,
    /** Etichette con cui l'app compare nel launcher, per riconoscerla se il package è diverso. */
    val launcherLabels: List<String>,
    val officialSite: String,
    /** Apertura diretta del singolo titolo: supportata solo se esiste un link ufficiale. */
    val contentLinkSupport: ContentLinkSupport = ContentLinkSupport.NONE,
    val notes: String = "",
) {
    fun candidatesFor(platform: Platform): List<PackageCandidate> = when (platform) {
        Platform.FIRE_TV -> fireTvPackages + androidTvPackages
        Platform.ANDROID_TV -> androidTvPackages + fireTvPackages
    }.distinctBy { it.packageName }
}

enum class ContentLinkSupport(val label: String) {
    NONE("Solo apertura dell'app"),
    YOUTUBE_VIDEO("Apertura diretta di un video (trailer)"),
}

/** Normalizza un nome provider TMDb o un'etichetta del launcher per il confronto. */
fun normalizeName(name: String): String =
    name.lowercase().replace("+", "plus").filter { it in 'a'..'z' || it in '0'..'9' }
