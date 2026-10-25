package com.lollomarta.streamhub.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.dp
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.ui.theme.LocalReduceMotion
import com.lollomarta.streamhub.ui.theme.SH
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val appear = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { appear.animateTo(1f, tween(if (reduce) 100 else 600, easing = FastOutSlowInEasing)) }
        progress.animateTo(1f, tween(if (reduce) 300 else 1100))
        onDone()
    }
    Box(Modifier.fillMaxSize().background(SH.Background), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.streamhub_background), null, Modifier.fillMaxSize().alpha(0.55f), contentScale = ContentScale.Crop)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = appear.value
                val s = 0.9f + 0.1f * appear.value
                scaleX = s; scaleY = s
            },
        ) {
            Image(painterResource(R.drawable.streamhub_symbol), "STREAMHUB", Modifier.size(150.dp))
            Spacer(Modifier.height(18.dp))
            Row {
                Text("STREAM", style = MaterialTheme.typography.displaySmall, color = SH.TextPrimary)
                Text("HUB", style = MaterialTheme.typography.displaySmall, color = SH.Primary)
            }
            Text("TUTTI I TUOI CONTENUTI, UN'UNICA APP", style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary)
            Spacer(Modifier.height(28.dp))
            Box(Modifier.width(160.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(SH.SurfaceElevated)) {
                Box(
                    Modifier.fillMaxWidth(progress.value).height(4.dp)
                        .background(Brush.horizontalGradient(listOf(SH.Primary, SH.Secondary))),
                )
            }
        }
    }
}
