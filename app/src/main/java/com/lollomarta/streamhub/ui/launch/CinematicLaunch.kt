package com.lollomarta.streamhub.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.integrations.AndroidTargetStarter
import com.lollomarta.streamhub.integrations.ContentRequest
import com.lollomarta.streamhub.integrations.LaunchOutcome
import com.lollomarta.streamhub.integrations.LaunchTarget
import com.lollomarta.streamhub.integrations.StreamingService
import com.lollomarta.streamhub.integrations.StreamingServices
import com.lollomarta.streamhub.ui.appContainer
import com.lollomarta.streamhub.ui.components.ButtonStyle
import com.lollomarta.streamhub.ui.components.RemoteImage
import com.lollomarta.streamhub.ui.components.TvButton
import com.lollomarta.streamhub.ui.theme.LocalReduceMotion
import com.lollomarta.streamhub.ui.theme.SH
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface LaunchPhase {
    data object Idle : LaunchPhase
    data class Animating(val serviceId: String, val request: ContentRequest?, val posterUrl: String?) : LaunchPhase
    data class Launched(val serviceName: String) : LaunchPhase
    data class NotInstalled(val service: StreamingService, val store: LaunchTarget.Store?) : LaunchPhase
    data class Failed(val message: String) : LaunchPhase
}

@Stable
class ServiceLauncherState {
    var phase: LaunchPhase by mutableStateOf(LaunchPhase.Idle)
        internal set

    /** Avvia la transizione e poi il servizio. Ignora pressioni ripetute durante l'animazione. */
    fun launch(serviceId: String, request: ContentRequest?, posterUrl: String?) {
        if (phase is LaunchPhase.Animating) return
        phase = LaunchPhase.Animating(serviceId, request, posterUrl)
    }

    fun dismiss() { phase = LaunchPhase.Idle }
}

@Composable
fun rememberServiceLauncher(): ServiceLauncherState = remember { ServiceLauncherState() }

/**
 * Transizione cinematografica: la locandina si ingrandisce e l'interfaccia si oscura,
 * poi Android porta in primo piano l'app ufficiale. Al ritorno, lo stato torna normale.
 * Non simuliamo mai una riproduzione dentro STREAMHUB.
 */
@Composable
fun CinematicLaunchHost(state: ServiceLauncherState) {
    val context = LocalContext.current
    val container = appContainer()
    val reduceMotion = LocalReduceMotion.current
    val overlay = remember { Animatable(0f) }
    val posterScale = remember { Animatable(0.92f) }
    var lastPoster by remember { mutableStateOf<String?>(null) }
    var lastServiceName by remember { mutableStateOf("") }
    val phase = state.phase

    LaunchedEffect(phase) {
        when (phase) {
            is LaunchPhase.Animating -> {
                lastPoster = phase.posterUrl
                lastServiceName = StreamingServices.byId(phase.serviceId)?.displayName.orEmpty()
                val duration = if (reduceMotion) 120 else 480
                coroutineScope {
                    launch { overlay.animateTo(0.95f, tween(duration)) }
                    launch { posterScale.animateTo(1.12f, tween(duration + 80, easing = FastOutSlowInEasing)) }
                }
                val outcome = container.launchManager.launch(
                    phase.serviceId, phase.request, AndroidTargetStarter(context, container.deviceEnvironment),
                )
                state.phase = when (outcome) {
                    is LaunchOutcome.Launched -> LaunchPhase.Launched(outcome.service.displayName)
                    is LaunchOutcome.NotInstalled -> LaunchPhase.NotInstalled(outcome.service, outcome.store)
                    is LaunchOutcome.Failed -> LaunchPhase.Failed(outcome.reason)
                }
            }
            is LaunchPhase.Launched -> {
                // Nessuna schermata di caricamento permanente: se restiamo visibili, chiudiamo l'overlay.
                delay(2500)
                if (state.phase is LaunchPhase.Launched) state.phase = LaunchPhase.Idle
            }
            else -> {
                overlay.animateTo(0f, tween(if (reduceMotion) 80 else 260))
                posterScale.snapTo(0.92f)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                container.deviceEnvironment.invalidate()
                if (state.phase is LaunchPhase.Launched) state.phase = LaunchPhase.Idle
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (overlay.value > 0.01f) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = overlay.value)), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    scaleX = posterScale.value; scaleY = posterScale.value; alpha = overlay.value
                },
            ) {
                if (lastPoster != null) {
                    RemoteImage(lastPoster, null, Modifier.width(200.dp).height(300.dp).clip(RoundedCornerShape(16.dp)))
                } else {
                    Image(painterResource(R.drawable.streamhub_symbol), null, Modifier.size(160.dp))
                }
                Spacer(Modifier.height(18.dp))
                if (lastServiceName.isNotEmpty()) {
                    Text("Apertura di $lastServiceName…", style = MaterialTheme.typography.titleMedium, color = SH.TextPrimary)
                }
            }
        }
    }

    when (phase) {
        is LaunchPhase.NotInstalled -> NotInstalledDialog(phase, onStore = {
            phase.store?.let { AndroidTargetStarter(context, container.deviceEnvironment).start(it) }
            state.dismiss()
        }, onClose = state::dismiss)
        is LaunchPhase.Failed -> InfoDialog("Impossibile aprire il servizio", phase.message, onClose = state::dismiss)
        else -> Unit
    }
}

/** Schermata per servizi non disponibili sul dispositivo. */
@Composable
private fun NotInstalledDialog(phase: LaunchPhase.NotInstalled, onStore: () -> Unit, onClose: () -> Unit) {
    val name = phase.service.displayName
    InfoDialog(
        title = "$name non è installata",
        message = if (phase.store != null) "Per guardare questo contenuto installa l'app ufficiale di $name dallo store del dispositivo."
        else "Non abbiamo trovato l'app di $name su questo dispositivo. Cercala nello store del dispositivo, se disponibile per il tuo modello.",
        primaryLabel = if (phase.store != null) "Apri lo store" else null,
        onPrimary = onStore,
        onClose = onClose,
    )
}

@Composable
fun InfoDialog(
    title: String,
    message: String,
    onClose: () -> Unit,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
) {
    val requester = remember { FocusRequester() }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(20.dp)).background(SH.SurfaceElevated).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = SH.TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = SH.TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (primaryLabel != null) {
                    TvButton(primaryLabel, onPrimary, Modifier.focusRequester(requester), style = ButtonStyle.PRIMARY)
                    TvButton("Chiudi", onClose)
                } else {
                    TvButton("Chiudi", onClose, Modifier.focusRequester(requester), style = ButtonStyle.PRIMARY)
                }
            }
        }
        LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }
    }
}
