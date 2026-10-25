package com.lollomarta.streamhub.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.integrations.ContentRequest
import com.lollomarta.streamhub.integrations.WatchOption
import com.lollomarta.streamhub.ui.appViewModel
import com.lollomarta.streamhub.ui.components.AttributionFooter
import com.lollomarta.streamhub.ui.components.ButtonStyle
import com.lollomarta.streamhub.ui.components.ErrorState
import com.lollomarta.streamhub.ui.components.MediaRow
import com.lollomarta.streamhub.ui.components.RemoteImage
import com.lollomarta.streamhub.ui.components.ScrimBottom
import com.lollomarta.streamhub.ui.components.ScrimLeft
import com.lollomarta.streamhub.ui.components.SkeletonRow
import com.lollomarta.streamhub.ui.components.StreamHubWordmark
import com.lollomarta.streamhub.ui.components.TopDest
import com.lollomarta.streamhub.ui.components.TopLevelScaffold
import com.lollomarta.streamhub.ui.components.TvButton
import com.lollomarta.streamhub.ui.components.formatRating
import com.lollomarta.streamhub.ui.launch.CinematicLaunchHost
import com.lollomarta.streamhub.ui.launch.rememberServiceLauncher
import com.lollomarta.streamhub.ui.theme.SH

@Composable
fun HomeScreen(
    onOpen: (MediaItem) -> Unit,
    onNavigate: (TopDest) -> Unit,
    onConfigureApi: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val launcher = rememberServiceLauncher()
    val listState = rememberLazyListState()

    val open: (MediaItem) -> Unit = { item ->
        vm.pendingRestoreKey = item.key
        onOpen(item)
    }

    TopLevelScaffold(TopDest.HOME, onNavigate) {
        when {
            state.loading && state.sections.isEmpty() -> HomeSkeleton()
            state.error != null && state.sections.isEmpty() ->
                ErrorState(state.error!!, onRetry = vm::load, onConfigure = onConfigureApi)
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                state.featured?.let { featured ->
                    item(key = "hero") {
                        Hero(
                            item = featured,
                            option = state.featuredOption,
                            requestInitialFocus = !vm.initialFocusDone && vm.pendingRestoreKey == null,
                            onInitialFocusDone = { vm.initialFocusDone = true },
                            onWatch = { option ->
                                option.service?.let {
                                    launcher.launch(it.id, ContentRequest(featured.type, featured.id, featured.title), featured.posterUrl)
                                }
                            },
                            onDetails = { open(featured) },
                        )
                    }
                }
                items(state.sections, key = { it.id }) { section ->
                    MediaRow(
                        title = section.title,
                        items = section.items,
                        onClick = open,
                        restoreKey = vm.pendingRestoreKey,
                        onFocusRestored = { vm.pendingRestoreKey = null },
                    )
                }
                if (favorites.isNotEmpty()) {
                    item(key = "my-list") {
                        MediaRow(
                            "La tua lista", favorites, onClick = open,
                            restoreKey = vm.pendingRestoreKey, onFocusRestored = { vm.pendingRestoreKey = null },
                        )
                    }
                }
                item(key = "attribution") { AttributionFooter() }
            }
        }
        CinematicLaunchHost(launcher)
    }
}

@Composable
private fun Hero(
    item: MediaItem,
    option: WatchOption?,
    requestInitialFocus: Boolean,
    onInitialFocusDone: () -> Unit,
    onWatch: (WatchOption) -> Unit,
    onDetails: () -> Unit,
) {
    val watchRequester = remember { FocusRequester() }
    val detailsRequester = remember { FocusRequester() }
    var detailsFocused by remember { mutableStateOf(false) }
    val canWatch = option?.service != null
    Box(Modifier.fillMaxWidth().height(390.dp)) {
        RemoteImage(item.backdropUrl, null, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(ScrimLeft))
        Box(Modifier.fillMaxSize().background(ScrimBottom))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 22.dp).widthIn(max = 560.dp)) {
            StreamHubWordmark(symbolSize = 30.dp)
            Spacer(Modifier.height(18.dp))
            Text(item.title, style = MaterialTheme.typography.displaySmall, color = SH.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(
                listOfNotNull(
                    item.year,
                    if (item.type == MediaType.TV) "Serie TV" else "Film",
                    formatRating(item.rating)?.let { "★ $it" },
                    option?.let { "${it.providerName} · ${it.bestType.label}" },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.labelLarge, color = SH.Primary,
            )
            Spacer(Modifier.height(10.dp))
            Text(item.overview, style = MaterialTheme.typography.bodyLarge, color = SH.TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // "Guarda" compare solo con una destinazione verificata; "Dettagli" mantiene sempre
                // la stessa identità, così il focus non si perde quando i dati arrivano.
                if (canWatch && option != null) {
                    TvButton(
                        "Guarda su ${option.providerName}", { onWatch(option) },
                        Modifier.focusRequester(watchRequester), icon = Icons.Filled.PlayArrow, style = ButtonStyle.PRIMARY,
                    )
                }
                TvButton(
                    "Dettagli", onDetails,
                    Modifier.focusRequester(detailsRequester).onFocusChanged { detailsFocused = it.isFocused },
                    icon = Icons.Filled.Info,
                    style = if (canWatch) ButtonStyle.SECONDARY else ButtonStyle.PRIMARY,
                )
            }
        }
    }
    if (requestInitialFocus) {
        LaunchedEffect(Unit) {
            runCatching { (if (canWatch) watchRequester else detailsRequester).requestFocus() }
            onInitialFocusDone()
        }
    }
    LaunchedEffect(canWatch) {
        if (canWatch && detailsFocused) runCatching { watchRequester.requestFocus() }
    }
}

@Composable
private fun HomeSkeleton() {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(300.dp).padding(32.dp)) { StreamHubWordmark() }
        SkeletonRow()
        SkeletonRow()
    }
}
