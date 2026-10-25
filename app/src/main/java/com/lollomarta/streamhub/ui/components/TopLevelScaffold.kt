package com.lollomarta.streamhub.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lollomarta.streamhub.R
import com.lollomarta.streamhub.ui.theme.SH

enum class TopDest(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    SEARCH("search", "Cerca", Icons.Filled.Search),
    FAVORITES("favorites", "La mia lista", Icons.Filled.Favorite),
    SERVICES("services", "Servizi", Icons.Filled.List),
    SETTINGS("settings", "Impostazioni", Icons.Filled.Settings),
}

/** Sfondo cinematografico + navigazione laterale compatta che si espande al focus. */
@Composable
fun TopLevelScaffold(current: TopDest, onNavigate: (TopDest) -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(SH.Background)) {
        Image(
            painterResource(R.drawable.streamhub_background), null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.35f),
        )
        Row(Modifier.fillMaxSize()) {
            SideNav(current, onNavigate)
            Box(Modifier.weight(1f).fillMaxHeight(), content = content)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SideNav(current: TopDest, onNavigate: (TopDest) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(if (expanded) 210.dp else 80.dp, tween(160), label = "navWidth")
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    listOf(SH.Background, SH.Background.copy(alpha = if (expanded) 0.97f else 0.75f)),
                ),
            )
            .onFocusChanged { expanded = it.hasFocus }
            .focusRestorer()
            .focusGroup()
            .padding(horizontal = 14.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(painterResource(R.drawable.streamhub_symbol), "STREAMHUB", Modifier.padding(start = 4.dp).size(44.dp))
        Spacer(Modifier.height(28.dp))
        TopDest.entries.forEach { dest ->
            NavItem(dest, selected = dest == current, expanded = expanded, onClick = { onNavigate(dest) })
        }
    }
}

@Composable
private fun NavItem(dest: TopDest, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    TvFocusBox(onClick = onClick, shape = RoundedCornerShape(12.dp), focusedScale = 1.03f, modifier = Modifier.fillMaxWidth()) { focused ->
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (focused) SH.SurfaceElevated else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                dest.icon, contentDescription = dest.label,
                tint = if (selected) SH.Primary else if (focused) SH.TextPrimary else SH.TextSecondary,
                modifier = Modifier.size(24.dp),
            )
            AnimatedVisibility(expanded, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    dest.label, style = MaterialTheme.typography.labelLarge,
                    color = if (selected) SH.Primary else SH.TextPrimary,
                    modifier = Modifier.padding(start = 14.dp), maxLines = 1,
                )
            }
        }
    }
}
