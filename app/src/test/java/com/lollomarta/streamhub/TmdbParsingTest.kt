package com.lollomarta.streamhub

import com.lollomarta.streamhub.data.network.DetailsDto
import com.lollomarta.streamhub.data.network.MediaDto
import com.lollomarta.streamhub.data.network.PagedDto
import com.lollomarta.streamhub.data.network.TmdbJson
import com.lollomarta.streamhub.data.network.TmdbMappers
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.OfferType
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbParsingTest {
    private val multi = """
        {"page":1,"total_pages":3,"results":[
          {"id":603,"media_type":"movie","title":"Matrix","release_date":"1999-03-30","poster_path":"/p.jpg","vote_average":8.2,"genre_ids":[28,878],"popularity":80.5},
          {"id":1399,"media_type":"tv","name":"Il Trono di Spade","first_air_date":"2011-04-17","vote_average":0,"extra_field":true},
          {"id":287,"media_type":"person","name":"Brad Pitt"},
          {"id":5,"media_type":"movie","title":"","release_date":""},
          {"id":6,"media_type":"movie","title":"Senza data","release_date":null,"poster_path":null}
        ]}
    """.trimIndent()

    @Test fun parsesMultiSearchAndSkipsInvalidEntries() {
        val items = TmdbMappers.toItems(TmdbJson.decodeFromString<PagedDto<MediaDto>>(multi))
        assertEquals(listOf("movie-603", "tv-1399", "movie-6"), items.map { it.key })
        assertEquals("1999", items[0].year)
        assertEquals("https://image.tmdb.org/t/p/w342/p.jpg", items[0].posterUrl)
        assertNull(items[1].rating) // 0 = nessuna valutazione, non "0 stelle"
        assertNull(items[2].year)
        assertNull(items[2].posterUrl)
    }

    @Test fun listEndpointsUseFallbackType() {
        val json = """{"results":[{"id":1,"name":"Serie"}]}"""
        val items = TmdbMappers.toItems(TmdbJson.decodeFromString<PagedDto<MediaDto>>(json), MediaType.TV)
        assertEquals(MediaType.TV, items.single().type)
    }

    @Test fun parsesDetailsWithProvidersAndMissingData() {
        val json = """
        {"id":66732,"name":"Stranger Things","overview":"","episode_run_time":[],"number_of_seasons":4,
         "genres":[{"id":18,"name":"Dramma"}],
         "seasons":[{"season_number":0,"name":"Speciali"},{"season_number":1,"name":"Stagione 1","episode_count":8,"air_date":"2016-07-15"}],
         "credits":{"cast":[{"name":"Millie Bobby Brown","character":"Eleven","order":1},{"name":null}]},
         "videos":{"results":[{"key":"abcDEF12345","site":"YouTube","type":"Trailer","official":true,"iso_639_1":"en"}]},
         "watch/providers":{"results":{"IT":{"link":"https://www.themoviedb.org/tv/66732/watch?locale=IT",
            "flatrate":[{"provider_id":8,"provider_name":"Netflix","logo_path":"/n.jpg","display_priority":1}],
            "rent":[{"provider_id":2,"provider_name":"Apple TV","display_priority":3}],
            "buy":[{"provider_id":3,"provider_name":null}]}}}}
        """.trimIndent()
        val d = TmdbMappers.toDetails(TmdbJson.decodeFromString<DetailsDto>(json), MediaType.TV, "IT")
        assertNull(d.runtimeMinutes)
        assertEquals(1, d.seasons.size)
        assertEquals(1, d.cast.size)
        assertEquals("abcDEF12345", d.trailerYoutubeKey)
        assertEquals(listOf(OfferType.SUBSCRIPTION, OfferType.RENT), d.availability.offers.map { it.type })
        assertTrue(d.availability.link!!.contains("themoviedb"))
    }

    @Test fun otherRegionGivesEmptyAvailability() {
        val json = """{"id":1,"title":"X","watch/providers":{"results":{"US":{"flatrate":[{"provider_id":8,"provider_name":"Netflix"}]}}}}"""
        val d = TmdbMappers.toDetails(TmdbJson.decodeFromString<DetailsDto>(json), MediaType.MOVIE, "IT")
        assertTrue(d.availability.isEmpty)
    }
}
