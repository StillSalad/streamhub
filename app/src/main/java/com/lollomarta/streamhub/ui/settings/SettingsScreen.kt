package com.lollomarta.streamhub.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lollomarta.streamhub.BuildConfig
import com.lollomarta.streamhub.domain.model.UserSettings
import com.lollomarta.streamhub.integrations.Platform
import com.lollomarta.streamhub.ui.appContainer
import com.lollomarta.streamhub.ui.components.AttributionFooter
import com.lollomarta.streamhub.ui.components.ButtonStyle
import com.lollomarta.streamhub.ui.components.FilterChip
import com.lollomarta.streamhub.ui.components.StreamHubWordmark
import com.lollomarta.streamhub.ui.components.TopDest
import com.lollomarta.streamhub.ui.components.TopLevelScaffold
import com.lollomarta.streamhub.ui.components.TvButton
import com.lollomarta.streamhub.ui.theme.SH
import kotlinx.coroutines.launch

private val REGIONS = listOf("IT" to "Italia", "CH" to "Svizzera", "SM" to "San Marino", "FR" to "Francia", "DE" to "Germania", "ES" to "Spagna", "GB" to "Regno Unito", "US" to "Stati Uniti")

@Composable
fun SettingsScreen(onNavigate: (TopDest) -> Unit, onConfigureApi: () -> Unit) {
    val container = appContainer()
    val settings by remember { container.preferences.settings }.collectAsStateWithLifecycle(initialValue = UserSettings())
    val hasUserKey by remember { container.preferences.hasUserApiKey }.collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    var recentCleared by remember { mutableStateOf(false) }
    val firstRequester = remember { FocusRequester() }

    TopLevelScaffold(TopDest.SETTINGS, onNavigate) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 32.dp, top = 28.dp, bottom = 24.dp)) {
            item { Text("Impostazioni", style = MaterialTheme.typography.headlineSmall, color = SH.TextPrimary) }
            item {
                SettingCard("Area geografica", "Determina la disponibilità dei servizi mostrata. Predefinita: Italia.") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)) {
                        items(REGIONS) { (code, name) ->
                            FilterChip(
                                "$name ($code)", settings.region == code,
                                { scope.launch { container.preferences.setRegion(code) } },
                                if (code == REGIONS.first().first) Modifier.focusRequester(firstRequester) else Modifier,
                            )
                        }
                    }
                }
            }
            item {
                SettingCard("Animazioni", "Riduce zoom e transizioni, utile su dispositivi lenti.") {
                    TvButton(
                        if (settings.reduceMotion) "Animazioni ridotte: attive" else "Animazioni ridotte: disattivate",
                        { scope.launch { container.preferences.setReduceMotion(!settings.reduceMotion) } },
                        icon = if (settings.reduceMotion) Icons.Filled.Check else null,
                    )
                }
            }
            item {
                val keyStatus = when {
                    hasUserKey -> "Configurata su questo dispositivo (cifrata localmente)."
                    BuildConfig.TMDB_API_KEY.isNotBlank() -> "Incorporata nella build (solo per uso personale: non è un segreto)."
                    else -> "Non configurata: film e serie non possono essere caricati."
                }
                SettingCard("Chiave API TMDb", keyStatus) {
                    TvButton("Configura chiave", onConfigureApi, icon = Icons.Filled.Settings)
                }
            }
            item {
                SettingCard("Dati locali", "Lista, servizi, filtri e ricerche restano solo su questo dispositivo.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvButton(if (recentCleared) "Cronologia cancellata" else "Cancella ricerche recenti", {
                            scope.launch { container.preferences.clearRecentSearches(); recentCleared = true }
                        })
                        TvButton(
                            if (confirmReset) "Premi di nuovo per confermare" else "Ripristina tutto",
                            {
                                if (confirmReset) scope.launch { container.preferences.resetAll(); confirmReset = false }
                                else confirmReset = true
                            },
                            style = if (confirmReset) ButtonStyle.PRIMARY else ButtonStyle.SUBTLE,
                        )
                    }
                }
            }
            item {
                SettingCard("Informazioni", null) {
                    StreamHubWordmark(symbolSize = 32.dp)
                    Spacer(Modifier.height(8.dp))
                    val platform = if (container.deviceEnvironment.platform == Platform.FIRE_TV) "Fire TV" else "Android TV / Google TV"
                    Text("Versione ${BuildConfig.VERSION_NAME} · Dispositivo: $platform · Creato da LolloMarta", style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary)
                    Text(
                        "STREAMHUB non riproduce né scarica contenuti: apre le app ufficiali. Nessuna pubblicità, nessun tracciamento.",
                        style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary,
                    )
                    AttributionFooter(Modifier.padding(start = 0.dp))
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { firstRequester.requestFocus() } }
}

@Composable
private fun SettingCard(title: String, subtitle: String?, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(16.dp))
            .background(SH.Surface.copy(alpha = 0.9f)).padding(18.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = SH.TextPrimary)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = SH.TextSecondary, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)) }
        content()
    }
}
