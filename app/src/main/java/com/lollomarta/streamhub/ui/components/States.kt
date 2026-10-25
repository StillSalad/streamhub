package com.lollomarta.streamhub.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.domain.AppError
import com.lollomarta.streamhub.ui.theme.SH

/** Messaggio centrato con azione opzionale; il pulsante riceve subito il focus. */
@Composable
fun MessageState(
    title: String,
    message: String,
    @DrawableRes image: Int,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    requestFocus: Boolean = true,
    primaryIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Refresh,
) {
    val requester = remember { FocusRequester() }
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 520.dp)) {
            Image(painterResource(image), null, Modifier.size(96.dp))
            Spacer(Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, color = SH.TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = SH.TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (primaryLabel != null) {
                    TvButton(primaryLabel, onPrimary, Modifier.focusRequester(requester), style = ButtonStyle.PRIMARY, icon = primaryIcon)
                }
                if (secondaryLabel != null) TvButton(secondaryLabel, onSecondary, icon = Icons.Filled.Settings)
            }
        }
    }
    if (primaryLabel != null && requestFocus) LaunchedEffect(title) { runCatching { requester.requestFocus() } }
}

/** Schermata di errore recuperabile. */
@Composable
fun ErrorState(error: AppError, onRetry: () -> Unit, onConfigure: () -> Unit, modifier: Modifier = Modifier) {
    val needsConfig = error is AppError.MissingApiKey || error is AppError.InvalidApiKey
    MessageState(
        title = error.title,
        message = error.userMessage,
        image = if (error is AppError.Network || error is AppError.Timeout) R.drawable.error_network else R.drawable.empty_search,
        modifier = modifier,
        primaryLabel = if (needsConfig) "Configura" else "Riprova",
        onPrimary = if (needsConfig) onConfigure else onRetry,
        secondaryLabel = if (needsConfig) null else "Impostazioni",
        onSecondary = onConfigure,
        primaryIcon = if (needsConfig) Icons.Filled.Settings else Icons.Filled.Refresh,
    )
}

@Composable
fun AttributionFooter(modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 32.dp, vertical = 24.dp)) {
        Text(
            "Dati di film e serie: TMDb. This product uses the TMDB API but is not endorsed or certified by TMDB.",
            style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary,
        )
        Text(
            "Disponibilità streaming fornita da JustWatch.",
            style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary,
        )
    }
}
