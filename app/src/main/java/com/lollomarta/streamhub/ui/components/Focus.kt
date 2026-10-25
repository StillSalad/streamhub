package com.lollomarta.streamhub.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lollomarta.streamhub.ui.theme.LocalReduceMotion
import com.lollomarta.streamhub.ui.theme.SH

/**
 * Contenitore focalizzabile con il telecomando: scala leggermente, bordo ciano e glow discreto.
 * OK/Invio/centro D-pad attivano onClick (gestito da Modifier.clickable). Nessuna dipendenza da hover.
 */
@Composable
fun TvFocusBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    focusedScale: Float = 1.06f,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (focused && !reduceMotion) focusedScale else 1f,
        animationSpec = tween(140),
        label = "focusScale",
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (focused) Modifier.shadow(18.dp, shape, ambientColor = SH.Primary, spotColor = SH.Primary) else Modifier)
            .clip(shape)
            .then(if (focused) Modifier.border(3.dp, SH.Primary, shape) else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) { content(focused) }
}

enum class ButtonStyle { PRIMARY, SECONDARY, SUBTLE }

@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: ButtonStyle = ButtonStyle.SECONDARY,
) {
    TvFocusBox(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp), focusedScale = 1.05f) { focused ->
        val bg = when {
            style == ButtonStyle.PRIMARY -> if (focused) SH.Primary else SH.Primary.copy(alpha = 0.85f)
            focused -> SH.TextPrimary
            style == ButtonStyle.SUBTLE -> Color.Transparent
            else -> SH.SurfaceElevated
        }
        val fg = if (style == ButtonStyle.PRIMARY || focused) SH.Background else SH.TextPrimary
        Row(
            modifier = Modifier
                .background(bg)
                .then(if (!focused && style == ButtonStyle.SECONDARY) Modifier.border(1.dp, SH.Primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp)) else Modifier)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TvFocusBox(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(50), focusedScale = 1.08f) { focused ->
        Box(
            Modifier
                .background(
                    when {
                        focused -> SH.TextPrimary
                        selected -> SH.Primary.copy(alpha = 0.22f)
                        else -> SH.Surface
                    },
                )
                .then(if (selected && !focused) Modifier.border(1.5.dp, SH.Primary, RoundedCornerShape(50)) else Modifier)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                label,
                color = if (focused) SH.Background else if (selected) SH.Primary else SH.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleMedium, color = SH.TextPrimary,
        modifier = modifier.padding(bottom = 2.dp).height(26.dp),
    )
}
