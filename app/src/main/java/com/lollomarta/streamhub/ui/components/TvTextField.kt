package com.lollomarta.streamhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lollomarta.streamhub.ui.theme.SH

/**
 * Campo di testo adatto alla TV: scorrendo con il D-pad NON apre la tastiera.
 * Premendo OK si entra in modifica e compare la tastiera di sistema; "Fatto"/Indietro esce.
 */
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Search,
    secret: Boolean = false,
    imeAction: ImeAction = ImeAction.Search,
    onSubmit: () -> Unit = {},
    displayRequester: FocusRequester = remember { FocusRequester() },
) {
    var editing by remember { mutableStateOf(false) }
    var returnFocus by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(14.dp)

    if (editing) {
        val editRequester = remember { FocusRequester() }
        var hadFocus by remember { mutableStateOf(false) }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = SH.TextPrimary),
            cursorBrush = SolidColor(SH.Primary),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                imeAction = imeAction,
                keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text,
            ),
            keyboardActions = KeyboardActions(onAny = {
                keyboard?.hide()
                onSubmit()
                editing = false
                returnFocus = true
            }),
            modifier = modifier
                .focusRequester(editRequester)
                .onFocusChanged {
                    if (it.isFocused) hadFocus = true
                    else if (hadFocus) { editing = false }
                }
                .clipField(shape)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        )
        LaunchedEffect(Unit) {
            runCatching { editRequester.requestFocus() }
            keyboard?.show()
        }
    } else {
        TvFocusBox(
            onClick = { editing = true },
            modifier = modifier.focusRequester(displayRequester),
            shape = shape,
            focusedScale = 1.02f,
        ) { focused ->
            Row(
                Modifier.fillMaxWidth().background(if (focused) SH.SurfaceElevated else SH.Surface).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = if (focused) SH.Primary else SH.TextSecondary)
                Spacer(Modifier.width(12.dp))
                val shown = when {
                    value.isEmpty() -> placeholder
                    secret -> "•".repeat(value.length.coerceAtMost(24))
                    else -> value
                }
                Text(shown, color = if (value.isEmpty()) SH.TextSecondary else SH.TextPrimary, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            }
        }
        if (returnFocus) {
            LaunchedEffect(Unit) {
                runCatching { displayRequester.requestFocus() }
                returnFocus = false
            }
        }
    }
}

private fun Modifier.clipField(shape: RoundedCornerShape): Modifier =
    this.background(SH.SurfaceElevated, shape).border(2.dp, SH.Primary, shape)

