package com.lollomarta.streamhub.ui.search

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.SortOption
import com.lollomarta.streamhub.integrations.StreamingServices
import com.lollomarta.streamhub.ui.appViewModel
import com.lollomarta.streamhub.ui.components.ErrorState
import com.lollomarta.streamhub.ui.components.FilterChip
import com.lollomarta.streamhub.ui.components.MessageState
import com.lollomarta.streamhub.ui.components.PosterCard
import com.lollomarta.streamhub.ui.components.TopDest
import com.lollomarta.streamhub.ui.components.TopLevelScaffold
import com.lollomarta.streamhub.ui.components.TvButton
import com.lollomarta.streamhub.ui.components.TvTextField
import com.lollomarta.streamhub.ui.components.shimmerBrush
import com.lollomarta.streamhub.ui.theme.SH
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@Composable
fun SearchScreen(onOpen: (MediaItem) -> Unit, onNavigate: (TopDest) -> Unit, onConfigureApi: () -> Unit) {
    val vm = appViewModel { SearchViewModel(it) }
    val query by vm.query.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val genres by vm.genres.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val settings by vm.selectedServices.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fieldRequester = remember { FocusRequester() }

    // Ricerca vocale tramite il riconoscitore standard del dispositivo (nessun permesso microfono richiesto all'app).
    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Cerca un film o una serie")
        }
    }
    val voiceAvailable = remember { voiceIntent.resolveActivity(context.packageManager) != null }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
                vm.setQuery(it); vm.submit()
            }
        }
    }

    val services = remember(settings) {
        val selected = settings?.selectedServiceIds.orEmpty()
        StreamingServices.ALL.sortedBy { if (it.id in selected) 0 else 1 }
    }

    val open: (MediaItem) -> Unit = { vm.pendingRestoreKey = it.key; onOpen(it) }
    LaunchedEffect(Unit) {
        // Focus iniziale sul campo, a meno che si stia tornando da una scheda.
        if (vm.pendingRestoreKey == null) runCatching { fieldRequester.requestFocus() }
    }

    TopLevelScaffold(TopDest.SEARCH, onNavigate) {
        Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TvTextField(
                    value = query, onValueChange = vm::setQuery, placeholder = "Cerca film e serie…",
                    modifier = Modifier.weight(1f), onSubmit = vm::submit, displayRequester = fieldRequester,
                )
                if (voiceAvailable) {
                    Spacer(Modifier.width(14.dp))
                    TvButton("Ricerca vocale", {
                        runCatching { voiceLauncher.launch(voiceIntent) }
                    })
                }
            }
            Spacer(Modifier.height(14.dp))
            ChipRow {
                item { FilterChip("Tutti", filters.type == null, { vm.updateFilters { it.copy(type = null) } }) }
                item { FilterChip("Film", filters.type == MediaType.MOVIE, { vm.updateFilters { it.copy(type = MediaType.MOVIE) } }) }
                item { FilterChip("Serie", filters.type == MediaType.TV, { vm.updateFilters { it.copy(type = MediaType.TV) } }) }
                item { Spacer(Modifier.width(18.dp)) }
                items(SortOption.entries.toList()) { s ->
                    FilterChip(s.label, filters.sort == s, { vm.updateFilters { it.copy(sort = s) } })
                }
                item { Spacer(Modifier.width(18.dp)) }
                item { FilterChip("Tutti i servizi", filters.serviceId == null, { vm.updateFilters { it.copy(serviceId = null) } }) }
                items(services) { svc ->
                    FilterChip(svc.displayName, filters.serviceId == svc.id, { vm.updateFilters { it.copy(serviceId = svc.id) } })
                }
            }
            if (genres.isNotEmpty()) {
                ChipRow {
                    item { FilterChip("Tutti i generi", filters.genreId == null, { vm.updateFilters { it.copy(genreId = null) } }) }
                    items(genres) { g ->
                        FilterChip(g.name, filters.genreId == g.id, { vm.updateFilters { it.copy(genreId = g.id) } })
                    }
                }
            }
            if (query.isBlank() && recent.isNotEmpty()) {
                ChipRow {
                    item { Text("Recenti:", color = SH.TextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp)) }
                    items(recent) { r -> FilterChip(r, false, { vm.useRecent(r) }) }
                    item { FilterChip("Cancella", false, vm::clearRecent) }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading && state.results.isEmpty() -> SkeletonGrid()
                    state.error != null -> ErrorState(state.error!!, onRetry = vm::retry, onConfigure = onConfigureApi)
                    state.results.isEmpty() -> MessageState(
                        title = "Nessun risultato",
                        message = state.notice ?: "Prova con un altro nome o modifica i filtri.",
                        image = R.drawable.empty_search,
                        primaryLabel = "Azzera filtri",
                        onPrimary = { vm.updateFilters { com.lollomarta.streamhub.domain.model.SearchFilters() } },
                        requestFocus = false,
                    )
                    else -> ResultsGrid(state, open, onNearEnd = vm::loadMore, restoreKey = vm.pendingRestoreKey, onRestored = { vm.pendingRestoreKey = null })
                }
            }
        }
    }
}

@Composable
private fun ChipRow(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun ResultsGrid(
    state: SearchViewModel.UiState,
    onOpen: (MediaItem) -> Unit,
    onNearEnd: () -> Unit,
    restoreKey: String?,
    onRestored: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp, start = 6.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        state.notice?.let { notice ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(notice, style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary)
            }
        }
        itemsIndexed(state.results, key = { _, it -> it.key }) { index, item ->
            PosterCard(item, onClick = { onOpen(item) }, requestFocusOnStart = item.key == restoreKey, onFocusRestored = onRestored)
            if (index >= state.results.size - 6) {
                LaunchedEffect(state.results.size) { onNearEnd() }
            }
        }
        if (state.loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SH.Primary)
                }
            }
        }
    }
}

@Composable
private fun SkeletonGrid() {
    val brush = shimmerBrush()
    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(6) { Box(Modifier.width(140.dp).height(210.dp).clip(RoundedCornerShape(14.dp)).background(brush)) }
    }
}
