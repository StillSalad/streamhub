package com.lollomarta.streamhub.ui.services

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.domain.model.CatalogProvider
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.UserSettings
import com.lollomarta.streamhub.integrations.InstallState
import com.lollomarta.streamhub.integrations.ServiceMatcher
import com.lollomarta.streamhub.integrations.StreamingServices
import com.lollomarta.streamhub.ui.appContainer
import com.lollomarta.streamhub.ui.components.ButtonStyle
import com.lollomarta.streamhub.ui.components.RemoteImage
import com.lollomarta.streamhub.ui.components.TopDest
import com.lollomarta.streamhub.ui.components.TopLevelScaffold
import com.lollomarta.streamhub.ui.components.TvButton
import com.lollomarta.streamhub.ui.launch.CinematicLaunchHost
import com.lollomarta.streamhub.ui.launch.rememberServiceLauncher
import com.lollomarta.streamhub.ui.theme.SH
import kotlinx.coroutines.launch

@Composable
fun ServicesScreen(onNavigate: (TopDest) -> Unit) {
    val container = appContainer()
    val settings by remember { container.preferences.settings }.collectAsStateWithLifecycle(initialValue = UserSettings())
    val scope = rememberCoroutineScope()
    val launcher = rememberServiceLauncher()
    var refresh by remember { mutableIntStateOf(0) }
    var providers by remember { androidx.compose.runtime.mutableStateOf<List<CatalogProvider>>(emptyList()) }
    val firstRequester = remember { FocusRequester() }

    LaunchedEffect(settings.region) {
        providers = runCatching {
            container.availability.regionProviders(MediaType.MOVIE, settings.region) +
                container.availability.regionProviders(MediaType.TV, settings.region)
        }.getOrDefault(emptyList())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val installStates = remember(refresh) {
        StreamingServices.ALL.associate { it.id to container.launchManager.installState(it.id) }
    }

    TopLevelScaffold(TopDest.SERVICES, onNavigate) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 32.dp, top = 28.dp, bottom = 32.dp)) {
            item {
                Text("I tuoi servizi", style = MaterialTheme.typography.headlineSmall, color = SH.TextPrimary)
                Text(
                    "Indica i servizi che possiedi: servono solo a ordinare i risultati. STREAMHUB non chiede credenziali e non verifica gli abbonamenti.",
                    style = MaterialTheme.typography.bodyMedium, color = SH.TextSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }
            items(StreamingServices.ALL, key = { it.id }) { service ->
                val selected = service.id in settings.selectedServiceIds
                val preferred = settings.preferredServiceId == service.id
                val installed = installStates[service.id] == InstallState.INSTALLED
                val catalogProvider = providers.firstOrNull { ServiceMatcher.match(it.name)?.id == service.id }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).background(SH.Surface.copy(alpha = 0.9f)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (catalogProvider?.logoUrl != null) {
                        RemoteImage(catalogProvider.logoUrl, service.displayName, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), placeholder = R.drawable.empty_search)
                    } else {
                        // Icona testuale: nessun logo di marchio generato artificialmente.
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Color(service.brandColor)), contentAlignment = Alignment.Center) {
                            Text(service.displayName.take(1), color = Color.White, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(service.displayName, style = MaterialTheme.typography.titleMedium, color = SH.TextPrimary)
                        val status = listOfNotNull(
                            if (selected) "Selezionato da te" else null,
                            if (installed) "App installata" else "App non rilevata",
                            if (catalogProvider == null) "Nessun dato nel catalogo per questa area" else null,
                        ).joinToString(" · ")
                        Text(status, style = MaterialTheme.typography.labelMedium, color = if (installed) SH.Success else SH.TextSecondary)
                        Text("Sito ufficiale: ${service.officialSite}", style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvButton(
                            "Apri", { launcher.launch(service.id, null, null) },
                            if (service == StreamingServices.ALL.first()) Modifier.focusRequester(firstRequester) else Modifier,
                            icon = Icons.Filled.PlayArrow, style = if (installed) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY,
                        )
                        TvButton(
                            if (selected) "Lo possiedo" else "Non selezionato",
                            { scope.launch { container.preferences.toggleService(service.id) } },
                            icon = if (selected) Icons.Filled.Check else null,
                        )
                        TvButton(
                            if (preferred) "Preferito" else "Imposta preferito",
                            { scope.launch { container.preferences.setPreferredService(if (preferred) null else service.id) } },
                            icon = if (preferred) Icons.Filled.Star else null,
                            style = ButtonStyle.SUBTLE,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
        CinematicLaunchHost(launcher)
    }
    LaunchedEffect(Unit) { runCatching { firstRequester.requestFocus() } }
}
