package com.lollomarta.streamhub.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.domain.toAppError
import com.lollomarta.streamhub.ui.appContainer
import com.lollomarta.streamhub.ui.components.ButtonStyle
import com.lollomarta.streamhub.ui.components.StreamHubWordmark
import com.lollomarta.streamhub.ui.components.TvButton
import com.lollomarta.streamhub.ui.components.TvTextField
import com.lollomarta.streamhub.ui.theme.SH
import kotlinx.coroutines.launch

/** Schermata di configurazione dell'API TMDb. */
@Composable
fun ApiSetupScreen(onDone: () -> Unit) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    val fieldRequester = remember { FocusRequester() }

    fun saveAndVerify() {
        if (busy) return
        val clean = key.trim()
        if (clean.length < 20) { status = false to "La chiave sembra troppo corta. Copiala per intero dal sito TMDb."; return }
        busy = true
        scope.launch {
            container.preferences.setApiKey(clean)
            status = try {
                if (container.catalog.validateKey()) true to "Chiave valida e salvata in modo cifrato su questo dispositivo."
                else false to "TMDb non ha confermato la chiave."
            } catch (e: Exception) {
                val err = e.toAppError()
                false to "${err.title}: ${err.userMessage}"
            }
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().background(SH.Background)) {
        Image(painterResource(R.drawable.streamhub_background), null, Modifier.fillMaxSize().alpha(0.3f), contentScale = ContentScale.Crop)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 64.dp, vertical = 36.dp),
        ) {
            StreamHubWordmark(symbolSize = 40.dp)
            Spacer(Modifier.height(24.dp))
            Text("Configura i dati del catalogo", style = MaterialTheme.typography.headlineMedium, color = SH.TextPrimary)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.widthIn(max = 760.dp)) {
                listOf(
                    "1. Da telefono o PC, crea un account gratuito su themoviedb.org.",
                    "2. Vai su Impostazioni → API e richiedi una chiave per uso personale.",
                    "3. Copia la \"Chiave API\" (v3) oppure il \"Token di accesso in lettura\" e incollalo qui.",
                    "La chiave resta su questo dispositivo, cifrata con l'Android Keystore. Nessun account STREAMHUB, nessun server.",
                ).forEach { Text(it, style = MaterialTheme.typography.bodyLarge, color = SH.TextSecondary, modifier = Modifier.padding(vertical = 2.dp)) }
                Spacer(Modifier.height(20.dp))
                TvTextField(
                    value = key, onValueChange = { key = it }, placeholder = "Incolla o digita la chiave TMDb",
                    icon = Icons.Filled.Lock, secret = true, imeAction = ImeAction.Done,
                    onSubmit = ::saveAndVerify, displayRequester = fieldRequester, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    TvButton(if (busy) "Verifica in corso…" else "Salva e verifica", ::saveAndVerify, icon = Icons.Filled.Check, style = ButtonStyle.PRIMARY)
                    TvButton("Rimuovi chiave", {
                        scope.launch { container.preferences.setApiKey(null); key = ""; status = true to "Chiave rimossa." }
                    })
                    TvButton("Indietro", onDone, style = ButtonStyle.SUBTLE)
                }
                status?.let { (ok, msg) ->
                    Spacer(Modifier.height(14.dp))
                    Text(msg, style = MaterialTheme.typography.bodyLarge, color = if (ok) SH.Success else SH.Error)
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { fieldRequester.requestFocus() } }
}
