package com.lollomarta.streamhub.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.ui.appContainer
import com.lollomarta.streamhub.ui.components.MessageState
import com.lollomarta.streamhub.ui.components.PosterCard
import com.lollomarta.streamhub.ui.components.TopDest
import com.lollomarta.streamhub.ui.components.TopLevelScaffold
import com.lollomarta.streamhub.ui.theme.SH

private object FavoritesFocus { var pendingRestoreKey: String? = null }

@Composable
fun FavoritesScreen(onOpen: (MediaItem) -> Unit, onNavigate: (TopDest) -> Unit) {
    val container = appContainer()
    val favorites by remember { container.preferences.favorites }.collectAsStateWithLifecycle<List<MediaItem>?>(initialValue = null)

    var restoreDone by remember { mutableStateOf(false) }

    TopLevelScaffold(TopDest.FAVORITES, onNavigate) {
        val list = favorites
        when {
            list == null -> Unit
            list.isEmpty() -> MessageState(
                title = "La tua lista è vuota",
                message = "Apri la scheda di un film o di una serie e scegli \"Aggiungi alla lista\".",
                image = R.drawable.empty_search,
                primaryLabel = "Vai alla ricerca",
                onPrimary = { onNavigate(TopDest.SEARCH) },
            )
            else -> Column(Modifier.fillMaxSize().padding(start = 24.dp, top = 28.dp, end = 24.dp)) {
                Text("La mia lista", style = MaterialTheme.typography.headlineSmall, color = SH.TextPrimary)
                Text("${list.size} titoli salvati sul dispositivo", style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(vertical = 18.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(list, key = { it.key }) { item ->
                        val target = FavoritesFocus.pendingRestoreKey?.takeIf { k -> list.any { it.key == k } } ?: list.first().key
                        val restore = !restoreDone && item.key == target
                        PosterCard(
                            item,
                            onClick = { FavoritesFocus.pendingRestoreKey = item.key; onOpen(item) },
                            requestFocusOnStart = restore,
                            onFocusRestored = { restoreDone = true; FavoritesFocus.pendingRestoreKey = null },
                        )
                    }
                }
            }
        }
    }
}
