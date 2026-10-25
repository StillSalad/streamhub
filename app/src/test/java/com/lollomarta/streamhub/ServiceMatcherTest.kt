package com.lollomarta.streamhub

import com.lollomarta.streamhub.integrations.ServiceMatcher
import com.lollomarta.streamhub.integrations.StreamingServices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceMatcherTest {
    @Test fun matchesTmdbProviderNames() {
        assertEquals(StreamingServices.NETFLIX, ServiceMatcher.match("Netflix"))
        assertEquals(StreamingServices.NETFLIX, ServiceMatcher.match("Netflix Standard with Ads"))
        assertEquals(StreamingServices.PRIME_VIDEO, ServiceMatcher.match("Amazon Prime Video"))
        assertEquals(StreamingServices.PRIME_VIDEO, ServiceMatcher.match("Amazon Video"))
        assertEquals(StreamingServices.DISNEY_PLUS, ServiceMatcher.match("Disney Plus"))
        assertEquals(StreamingServices.APPLE_TV, ServiceMatcher.match("Apple TV+"))
        assertEquals(StreamingServices.NOW, ServiceMatcher.match("NOW"))
        assertEquals(StreamingServices.NOW, ServiceMatcher.match("Now TV Cinema"))
        assertEquals(StreamingServices.RAIPLAY, ServiceMatcher.match("Rai Play"))
        assertEquals(StreamingServices.MEDIASET_INFINITY, ServiceMatcher.match("Mediaset Infinity"))
        assertEquals(StreamingServices.YOUTUBE, ServiceMatcher.match("YouTube Premium"))
    }

    @Test fun unknownProvidersAreNotMatched() {
        assertNull(ServiceMatcher.match("Chili"))
        assertNull(ServiceMatcher.match("Knowledge Network"))
        assertNull(ServiceMatcher.match(""))
    }

    @Test fun launcherLabelMatchIsExact() {
        assertTrue(ServiceMatcher.labelMatches(StreamingServices.NOW, "NOW"))
        assertTrue(!ServiceMatcher.labelMatches(StreamingServices.NOW, "Snowboard Now Game"))
        assertTrue(ServiceMatcher.labelMatches(StreamingServices.DISNEY_PLUS, "Disney+"))
    }

    @Test fun serviceIdsAreUnique() {
        assertEquals(StreamingServices.ALL.size, StreamingServices.ALL.map { it.id }.toSet().size)
    }
}
