package com.lollomarta.streamhub.ui.details

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.domain.model.CastMember
import com.lollomarta.streamhub.domain.model.MediaDetails
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.SeasonInfo
import com.lollomarta.streamhub.integrations.ContentRequest
import com.lollomarta.streamhub.integrations.StreamingServices
import com.lollomarta.streamhub.integrations.WatchOption
import com.lollomarta.streamhub.ui.appViewModel
import com.lollomarta.streamhub.ui.components.AttributionFooter
import com.lollomarta.streamhub.ui.components.ButtonStyle
import com.lollomarta.streamhub.ui.components.ErrorState
import com.lollomarta.streamhub.ui.components.OfferBadge
import com.lollomarta.streamhub.ui.components.RemoteImage
import com.lollomarta.streamhub.ui.components.ScrimBottom
import com.lollomarta.streamhub.ui.components.ScrimLeft
import com.lollomarta.streamhub.ui.components.SectionTitle
import com.lollomarta.streamhub.ui.components.SkeletonRow
import com.lollomarta.streamhub.ui.components.TvButton
import com.lollomarta.streamhub.ui.components.TvFocusBox
import com.lollomarta.streamhub.ui.components.formatRating
import com.lollomarta.streamhub.ui.launch.CinematicLaunchHost
import com.lollomarta.streamhub.ui.launch.rememberServiceLauncher
import com.lollomarta.streamhub.ui.theme.SH

@Composable
fun DetailsScreen(type: MediaType, id: Int, onConfigureApi: () -> Unit) {
    val vm = appViewModel(key = "${type.apiPath}-$id") { DetailsViewModel(it, type, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val launcher = rememberServiceLauncher()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.refreshInstallState() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Box(Modifier.fillMaxSize().background(SH.Background)) {
        val details = state.details
        when {
            details != null -> {
                val watch: (WatchOption) -> Unit = { option ->
                    option.service?.let { svc ->
                        launcher.launch(svc.id, ContentRequest(type, id, details.item.title), details.item.posterUrl)
                    }
                }
                DetailsContent(
                    details = details,
                    options = state.options,
                    primary = state.primary,
                    isFavorite = state.isFavorite,
                    onWatch = watch,
                    onTrailer = {
                        launcher.launch(
                            StreamingServices.YOUTUBE.id,
                            ContentRequest(type, id, details.item.title, youtubeKey = details.trailerYoutubeKey),
                            details.item.posterUrl,
                        )
                    },
                    onToggleFavorite = vm::toggleFavorite,
                )
            }
            state.error != null -> ErrorState(state.error!!, onRetry = vm::reload, onConfigure = onConfigureApi)
            else -> Column(Modifier.padding(top = 120.dp)) { SkeletonRow(count = 1); SkeletonRow() }
        }
        CinematicLaunchHost(launcher)
    }
}

@Composable
private fun DetailsContent(
    details: MediaDetails,
    options: List<WatchOption>,
    primary: WatchOption?,
    isFavorite: Boolean,
    onWatch: (WatchOption) -> Unit,
    onTrailer: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val item = details.item
    val mainRequester = remember { FocusRequester() }
    Box(Modifier.fillMaxSize()) {
        RemoteImage(item.backdropUrl, null, Modifier.fillMaxSize().alpha(0.45f))
        Box(Modifier.fillMaxSize().background(ScrimLeft))
        Box(Modifier.fillMaxSize().background(ScrimBottom))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 48.dp, end = 32.dp, top = 40.dp, bottom = 24.dp)) {
            item(key = "header") {
                Row {
                    RemoteImage(item.posterUrl, item.title, Modifier.width(210.dp).height(315.dp).clip(RoundedCornerShape(16.dp)))
                    Spacer(Modifier.width(36.dp))
                    Column(Modifier.widthIn(max = 640.dp)) {
                        Text(item.title, style = MaterialTheme.typography.headlineMedium, color = SH.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        MetaLine(details)
                        details.tagline?.let {
                            Spacer(Modifier.height(10.dp))
                            Text(it, style = MaterialTheme.typography.bodyLarge, color = SH.Primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            item.overview.ifBlank { "Trama non disponibile in italiano." },
                            style = MaterialTheme.typography.bodyLarge, color = SH.TextSecondary,
                            maxLines = 6, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (primary?.service != null) {
                                TvButton(
                                    "GUARDA SU ${primary.providerName.uppercase()}", { onWatch(primary) },
                                    Modifier.focusRequester(mainRequester), icon = Icons.Filled.PlayArrow, style = ButtonStyle.PRIMARY,
                                )
                            }
                            TvButton(
                                if (isFavorite) "Nella tua lista" else "Aggiungi alla lista", onToggleFavorite,
                                if (primary?.service == null) Modifier.focusRequester(mainRequester) else Modifier,
                                icon = if (isFavorite) Icons.Filled.Check else Icons.Filled.Add,
                            )
                            if (details.trailerYoutubeKey != null) {
                                TvButton("Trailer", onTrailer, icon = Icons.Filled.PlayArrow, style = ButtonStyle.SUBTLE)
                            }
                        }
                        primary?.let {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OfferBadge(it.bestType)
                                if (it.installed == false) {
                                    Spacer(Modifier.width(10.dp))
                                    Text("App non rilevata su questo dispositivo", style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
            item(key = "options") { WatchOptionsSection(options, onWatch, details) }
            if (details.seasons.isNotEmpty()) item(key = "seasons") { SeasonsRow(details.seasons, details.numberOfSeasons) }
            if (details.cast.isNotEmpty()) item(key = "cast") { CastRow(details.cast) }
            item(key = "attribution") { AttributionFooter(Modifier.padding(start = 0.dp)) }
        }
    }
    LaunchedEffect(item.key) { runCatching { mainRequester.requestFocus() } }
}

@Composable
private fun MetaLine(details: MediaDetails) {
    val item = details.item
    Row(verticalAlignment = Alignment.CenterVertically) {
        formatRating(item.rating)?.let {
            Icon(Icons.Filled.Star, null, tint = SH.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(it, style = MaterialTheme.typography.labelLarge, color = SH.TextPrimary)
            Spacer(Modifier.width(14.dp))
        }
        val parts = listOfNotNull(
            item.year,
            details.runtimeMinutes?.let { m -> if (item.type == MediaType.TV) "$m min/ep." else "${m / 60}h ${m % 60}min".removePrefix("0h ") },
            details.numberOfSeasons?.let { n -> if (n == 1) "1 stagione" else "$n stagioni" },
            details.genres.take(3).joinToString(", ") { it.name }.ifBlank { null },
        )
        Text(parts.joinToString("  ·  "), style = MaterialTheme.typography.labelLarge, color = SH.TextSecondary)
    }
}

@Composable
private fun WatchOptionsSection(options: List<WatchOption>, onWatch: (WatchOption) -> Unit, details: MediaDetails) {
    Column(Modifier.padding(top = 28.dp)) {
        SectionTitle("Dove guardarlo (${details.availability.region})")
        if (options.isEmpty()) {
            // Schermata per contenuti non disponibili: nessuna disponibilità inventata.
            Text(
                "Nessuna disponibilità streaming nota in questa area. I dati possono essere incompleti o cambiare nel tempo.",
                style = MaterialTheme.typography.bodyLarge, color = SH.TextSecondary, modifier = Modifier.padding(top = 8.dp),
            )
            return@Column
        }
        LazyRow(contentPadding = PaddingValues(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(options) { option ->
                OptionCard(option, onClick = { onWatch(option) })
            }
        }
    }
}

@Composable
private fun OptionCard(option: WatchOption, onClick: () -> Unit) {
    TvFocusBox(onClick = onClick, enabled = option.canLaunch, modifier = Modifier.width(230.dp)) { focused ->
        Column(Modifier.fillMaxWidth().background(if (focused) SH.SurfaceElevated else SH.Surface).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (option.logoUrl != null) {
                    // Logo fornito da TMDb tramite l'endpoint ufficiale dei provider.
                    RemoteImage(option.logoUrl, option.providerName, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)), placeholder = R.drawable.empty_search)
                } else {
                    val color = option.service?.brandColor?.let { Color(it) } ?: SH.Secondary
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color), contentAlignment = Alignment.Center) {
                        Text(option.providerName.take(1), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(option.providerName, style = MaterialTheme.typography.titleMedium, color = SH.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                option.types.sortedBy { it.rank }.take(2).forEach { OfferBadge(it) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    !option.canLaunch -> "Nessuna integrazione disponibile"
                    option.installed == true -> "App installata · premi OK"
                    else -> "App non rilevata"
                },
                style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary,
            )
        }
    }
}

@Composable
private fun SeasonsRow(seasons: List<SeasonInfo>, total: Int?) {
    Column(Modifier.padding(top = 18.dp)) {
        SectionTitle(if (total != null) "Stagioni ($total)" else "Stagioni")
        LazyRow(contentPadding = PaddingValues(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(seasons) { s ->
                TvFocusBox(onClick = {}, modifier = Modifier.width(170.dp)) { focused ->
                    Column(Modifier.fillMaxWidth().background(if (focused) SH.SurfaceElevated else SH.Surface).padding(14.dp)) {
                        Text(s.name, style = MaterialTheme.typography.labelLarge, color = SH.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(s.episodeCount?.let { "$it episodi" }, s.airYear).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CastRow(cast: List<CastMember>) {
    Column(Modifier.padding(top = 10.dp)) {
        SectionTitle("Cast principale")
        LazyRow(contentPadding = PaddingValues(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(cast) { member ->
                TvFocusBox(onClick = {}, modifier = Modifier.width(120.dp), shape = RoundedCornerShape(14.dp)) { _ ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        RemoteImage(member.profileUrl, member.name, Modifier.size(84.dp).clip(CircleShape))
                        Spacer(Modifier.height(8.dp))
                        Text(member.name, style = MaterialTheme.typography.labelMedium, color = SH.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        member.character?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
