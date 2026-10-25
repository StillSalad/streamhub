package com.lollomarta.streamhub.integrations

import com.lollomarta.streamhub.domain.model.Availability
import com.lollomarta.streamhub.domain.model.OfferType

/** Un'opzione di visione mostrata nella scheda, raggruppata per servizio. */
data class WatchOption(
    val service: StreamingService?,
    val providerName: String,
    val logoUrl: String?,
    val types: Set<OfferType>,
    val displayPriority: Int,
    val userSelected: Boolean,
    val installed: Boolean?,
) {
    val canLaunch: Boolean get() = service != null
    val bestType: OfferType get() = types.minByOrNull { it.rank } ?: OfferType.BUY
    val isIncludedOrFree: Boolean get() = types.any { it == OfferType.SUBSCRIPTION || it == OfferType.FREE || it == OfferType.ADS }
}

object WatchOptions {
    fun build(
        availability: Availability,
        selectedServiceIds: Set<String>,
        preferredServiceId: String?,
        installedLookup: (StreamingService) -> Boolean?,
        matcher: (String) -> StreamingService? = ServiceMatcher::match,
    ): List<WatchOption> {
        val groups = availability.offers.groupBy { offer ->
            matcher(offer.providerName)?.id ?: "provider-${offer.providerId}"
        }
        val options = groups.map { (_, offers) ->
            val first = offers.minBy { it.displayPriority }
            val service = matcher(first.providerName)
            WatchOption(
                service = service,
                providerName = service?.displayName ?: first.providerName,
                logoUrl = first.logoUrl,
                types = offers.map { it.type }.toSet(),
                displayPriority = first.displayPriority,
                userSelected = service != null && service.id in selectedServiceIds,
                installed = service?.let(installedLookup),
            )
        }
        return options.sortedWith(
            compareBy<WatchOption>(
                { if (it.service == null) 1 else 0 },
                { if (it.service != null && it.service.id == preferredServiceId) 0 else 1 },
                { if (it.userSelected && it.isIncludedOrFree) 0 else 1 },
                { it.bestType.rank },
                { if (it.installed == true) 0 else 1 },
                { if (it.userSelected) 0 else 1 },
                { it.displayPriority },
            ),
        )
    }

    /** L'opzione per il pulsante principale GUARDA SU [SERVIZIO], se esiste una destinazione. */
    fun primary(options: List<WatchOption>): WatchOption? = options.firstOrNull { it.canLaunch }
}
