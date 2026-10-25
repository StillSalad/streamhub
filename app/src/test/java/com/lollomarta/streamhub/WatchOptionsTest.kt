package com.lollomarta.streamhub

import com.lollomarta.streamhub.domain.model.Availability
import com.lollomarta.streamhub.domain.model.OfferType
import com.lollomarta.streamhub.domain.model.ProviderOffer
import com.lollomarta.streamhub.integrations.WatchOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchOptionsTest {
    private val availability = Availability(
        "IT",
        listOf(
            ProviderOffer(2, "Apple TV", null, OfferType.RENT, 1),
            ProviderOffer(2, "Apple TV", null, OfferType.BUY, 1),
            ProviderOffer(8, "Netflix", null, OfferType.SUBSCRIPTION, 5),
            ProviderOffer(99, "Chili", null, OfferType.RENT, 0),
        ),
    )

    @Test fun subscriptionBeatsRentAndUnknownProvidersGoLast() {
        val options = WatchOptions.build(availability, emptySet(), null, { null })
        assertEquals("Netflix", options.first().providerName)
        assertEquals("Chili", options.last().providerName)
        assertFalse(options.last().canLaunch)
    }

    @Test fun subscriptionAndRentAreNeverMerged() {
        val options = WatchOptions.build(availability, emptySet(), null, { null })
        val apple = options.first { it.providerName == "Apple TV" }
        assertEquals(setOf(OfferType.RENT, OfferType.BUY), apple.types)
        assertFalse(apple.isIncludedOrFree)
        assertTrue(options.first { it.providerName == "Netflix" }.isIncludedOrFree)
    }

    @Test fun preferredServiceComesFirst() {
        val options = WatchOptions.build(availability, emptySet(), "apple_tv", { null })
        assertEquals("apple_tv", WatchOptions.primary(options)?.service?.id)
    }

    @Test fun emptyAvailabilityHasNoPrimary() {
        assertNull(WatchOptions.primary(WatchOptions.build(Availability("IT"), emptySet(), null, { null })))
    }
}
