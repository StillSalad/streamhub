package com.lollomarta.streamhub.domain

/** Errori applicativi comprensibili. Nessuno di questi deve mandare in crash l'app. */
sealed class AppError(val title: String, val userMessage: String, val retryable: Boolean) : Exception(title) {
    data object MissingApiKey : AppError("Configurazione necessaria", "Inserisci una chiave API di TMDb nelle impostazioni per caricare film e serie.", false)
    data object InvalidApiKey : AppError("Chiave API non valida", "TMDb ha rifiutato la chiave. Controllala nelle impostazioni.", false)
    data object Network : AppError("Connessione assente", "Controlla la tua connessione e riprova.", true)
    data object Timeout : AppError("Il server non risponde", "La richiesta ha impiegato troppo tempo. Riprova.", true)
    data object RateLimited : AppError("Troppe richieste", "Il servizio dati ha limitato le richieste. Attendi qualche secondo e riprova.", true)
    data object NotFound : AppError("Contenuto non trovato", "Questo contenuto non è più disponibile nel catalogo.", false)
    data class Server(val code: Int) : AppError("Errore del servizio dati", "Il catalogo ha risposto con un errore ($code). Riprova più tardi.", code >= 500)
    data class Parsing(val detail: String) : AppError("Dati non validi", "La risposta del catalogo non è leggibile.", false)
    data class Unknown(val detail: String?) : AppError("Errore imprevisto", "Qualcosa è andato storto. Riprova.", true)
}

fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is java.net.SocketTimeoutException -> AppError.Timeout
    is java.io.IOException -> AppError.Network
    is kotlinx.serialization.SerializationException -> AppError.Parsing(message ?: "")
    is IllegalArgumentException -> AppError.Parsing(message ?: "")
    else -> AppError.Unknown(message)
}
