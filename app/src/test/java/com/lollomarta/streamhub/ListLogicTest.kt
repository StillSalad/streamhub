package com.lollomarta.streamhub

import com.lollomarta.streamhub.domain.logic.FavoritesLogic
import com.lollomarta.streamhub.domain.logic.RecentSearchesLogic
import com.lollomarta.streamhub.domain.logic.SearchQuery
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.SearchFilters
import com.lollomarta.streamhub.domain.model.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListLogicTest {
    private val movie = MediaItem(1, MediaType.MOVIE, "A", popularity = 10.0, rating = 6.0, genreIds = listOf(18))
    private val show = MediaItem(1, MediaType.TV, "B", popularity = 50.0, rating = 9.0, genreIds = listOf(35))

    @Test fun favoritesToggleAddsAndRemoves() {
        val one = FavoritesLogic.toggle(emptyList(), movie)
        assertEquals(listOf(movie), one)
        val two = FavoritesLogic.toggle(one, show)
        assertEquals(listOf(show, movie), two) // stesso id, tipo diverso: elementi distinti
        assertEquals(listOf(show), FavoritesLogic.toggle(two, movie))
        assertTrue(FavoritesLogic.contains(two, "tv-1"))
    }

    @Test fun emptyOrShortQueriesAreRejected() {
        assertNull(SearchQuery.normalize(""))
        assertNull(SearchQuery.normalize("   "))
        assertNull(SearchQuery.normalize("a"))
        assertEquals("dune parte due", SearchQuery.normalize("  dune   parte  due "))
    }

    @Test fun recentSearchesDeduplicateAndCap() {
        var list = emptyList<String>()
        repeat(15) { list = RecentSearchesLogic.add(list, "titolo $it") }
        assertEquals(RecentSearchesLogic.MAX, list.size)
        list = RecentSearchesLogic.add(list, "TITOLO 14")
        assertEquals("TITOLO 14", list.first())
        assertEquals(1, list.count { it.equals("titolo 14", true) })
        assertEquals(list, RecentSearchesLogic.add(list, " "))
    }

    @Test fun localFiltersAndSorting() {
        val items = listOf(movie, show, movie)
        assertEquals(listOf(show, movie), SearchQuery.applyLocalFilters(items, SearchFilters()))
        assertEquals(listOf(movie), SearchQuery.applyLocalFilters(items, SearchFilters(type = MediaType.MOVIE)))
        assertEquals(listOf(show), SearchQuery.applyLocalFilters(items, SearchFilters(genreId = 35)))
        assertEquals(listOf(show, movie), SearchQuery.applyLocalFilters(items, SearchFilters(sort = SortOption.RATING)))
    }
}
