package com.lollomarta.streamhub.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.domain.model.MediaItem
import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.domain.model.OfferType
import com.lollomarta.streamhub.ui.theme.SH
import java.util.Locale

@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    @DrawableRes placeholder: Int = R.drawable.poster_placeholder,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val painter = painterResource(placeholder)
    AsyncImage(
        model = remember(url) { ImageRequest.Builder(context).data(url).crossfade(200).build() },
        contentDescription = contentDescription,
        placeholder = painter,
        error = painter,
        fallback = painter,
        contentScale = contentScale,
        modifier = modifier,
    )
}

fun formatRating(rating: Double?): String? = rating?.let { String.format(Locale.ITALY, "%.1f", it) }

@Composable
fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 140.dp,
    requestFocusOnStart: Boolean = false,
    onFocusRestored: () -> Unit = {},
    onFocused: () -> Unit = {},
) {
    val requester = remember { FocusRequester() }
    if (requestFocusOnStart) {
        LaunchedEffect(Unit) {
            runCatching { requester.requestFocus() }
            onFocusRestored()
        }
    }
    Column(modifier.width(width)) {
        TvFocusBox(
            onClick = onClick,
            modifier = Modifier
                .focusRequester(requester)
                .width(width)
                .height(width * 1.5f),
            onFocusChanged = { if (it) onFocused() },
        ) { _ ->
            Box(Modifier.fillMaxSize().background(SH.Surface)) {
                RemoteImage(item.posterUrl, item.title, Modifier.fillMaxSize())
                if (item.posterUrl == null) {
                    Text(
                        item.title, color = SH.TextSecondary, style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp),
                    )
                }
                formatRating(item.rating)?.let { r ->
                    Row(
                        Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(8.dp))
                            .background(SH.Background.copy(alpha = 0.78f)).padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Star, null, tint = SH.Primary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(r, color = SH.TextPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.title, style = MaterialTheme.typography.labelMedium, color = SH.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOfNotNull(item.year, if (item.type == MediaType.TV) "Serie" else "Film").joinToString(" · "),
            style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary, maxLines = 1,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MediaRow(
    title: String,
    items: List<MediaItem>,
    onClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    restoreKey: String? = null,
    onFocusRestored: () -> Unit = {},
) {
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth().padding(top = 14.dp)) {
        SectionTitle(title, Modifier.padding(start = 32.dp))
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(items, key = { it.key }) { item ->
                PosterCard(
                    item = item,
                    onClick = { onClick(item) },
                    requestFocusOnStart = item.key == restoreKey,
                    onFocusRestored = onFocusRestored,
                )
            }
        }
    }
}

@Composable
fun shimmerBrush(): Brush {
    val t = rememberInfiniteTransition(label = "shimmer")
    val a by t.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha")
    return Brush.linearGradient(listOf(SH.Surface.copy(alpha = a), SH.SurfaceElevated.copy(alpha = a)))
}

@Composable
fun SkeletonRow(modifier: Modifier = Modifier, count: Int = 7) {
    val brush = shimmerBrush()
    Column(modifier.padding(start = 32.dp, top = 20.dp)) {
        Box(Modifier.width(180.dp).height(20.dp).clip(RoundedCornerShape(6.dp)).background(brush))
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(count) {
                Box(Modifier.width(140.dp).height(210.dp).clip(RoundedCornerShape(14.dp)).background(brush))
            }
        }
    }
}

@Composable
fun OfferBadge(type: OfferType, modifier: Modifier = Modifier) {
    val color = when (type) {
        OfferType.SUBSCRIPTION, OfferType.FREE, OfferType.ADS -> SH.Success
        OfferType.RENT -> SH.Rental
        OfferType.BUY -> SH.Secondary
    }
    Text(
        type.label,
        color = SH.Background,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Logo STREAMHUB: simbolo grafico + scritta resa come testo (non dentro l'immagine). */
@Composable
fun StreamHubWordmark(modifier: Modifier = Modifier, symbolSize: Dp = 40.dp, showTagline: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.streamhub_symbol), contentDescription = null, modifier = Modifier.size(symbolSize))
        Spacer(Modifier.width(12.dp))
        Column {
            Row {
                Text("STREAM", style = MaterialTheme.typography.headlineSmall, color = SH.TextPrimary)
                Text("HUB", style = MaterialTheme.typography.headlineSmall, color = SH.Primary)
            }
            if (showTagline) {
                Text("TUTTI I TUOI CONTENUTI, UN'UNICA APP", style = MaterialTheme.typography.labelMedium, color = SH.TextSecondary)
            }
        }
    }
}

val ScrimLeft = Brush.horizontalGradient(0f to SH.Background, 0.55f to SH.Background.copy(alpha = 0.6f), 1f to Color.Transparent)
val ScrimBottom = Brush.verticalGradient(0f to Color.Transparent, 0.7f to SH.Background.copy(alpha = 0.5f), 1f to SH.Background)
