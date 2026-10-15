package com.example.musicplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val SearchHeight = 52.dp

/**
 * A small glass search button that grows into a full-width search field when tapped.
 * The width, the text and the close button all follow one spring, so it feels like
 * a single liquid motion. Place it in a Box and align it to the top end.
 *
 * [restingEndPadding] leaves room for something beside the button while it is small.
 */
@Composable
fun ExpandableSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    restingEndPadding: Dp = 0.dp
) {
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 300f),
        label = "searchProgress"
    )
    val clearAlpha by animateFloatAsState(
        targetValue = if (expanded && query.isNotEmpty()) 1f else 0f,
        animationSpec = tween(180),
        label = "searchClear"
    )
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(140)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // field not attached yet, the user can tap it
            }
        } else {
            focusManager.clearFocus()
        }
    }

    val close = {
        onQueryChange("")
        onExpandedChange(false)
    }

    BackHandler(enabled = expanded) { close() }

    BoxWithConstraints(modifier.fillMaxWidth().height(SearchHeight)) {
        val p = progress.coerceIn(0f, 1.08f)
        val shown = progress.coerceIn(0f, 1f)
        val cancelSlot = 76.dp
        val width = SearchHeight + (maxWidth - cancelSlot - SearchHeight) * p
        val endPadding = restingEndPadding * (1f - shown) + cancelSlot * shown
        // the pill swells for a moment while it opens, like a drop of liquid
        val swell = sin(PI.toFloat() * shown)

        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = endPadding)
                .width(width)
                .height(SearchHeight)
                .graphicsLayer {
                    scaleY = 1f + 0.10f * swell
                    scaleX = 1f + 0.015f * swell
                }
                .glass(RoundedCornerShape(SearchHeight / 2), tint = R4.Cyan.copy(alpha = 0.10f * shown))
                .then(
                    if (expanded) Modifier else Modifier.bouncyClick(0.9f) { onExpandedChange(true) }
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Search",
                tint = if (expanded) R4.Cyan else R4.Ink,
                modifier = Modifier.size(24.dp)
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = expanded,
                singleLine = true,
                textStyle = TextStyle(color = R4.Ink, fontSize = 16.sp),
                cursorBrush = SolidColor(R4.Cyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
                    .graphicsLayer {
                        alpha = ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        translationX = (1f - progress.coerceIn(0f, 1f)) * 24f
                    }
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(placeholder, color = R4.InkDim, fontSize = 16.sp, maxLines = 1)
                        }
                        innerTextField()
                    }
                }
            )
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Clear",
                tint = R4.InkDim,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        alpha = clearAlpha
                        scaleX = 0.5f + 0.5f * clearAlpha
                        scaleY = 0.5f + 0.5f * clearAlpha
                    }
                    .then(
                        if (expanded && query.isNotEmpty()) {
                            Modifier.bouncyClick(0.8f) { onQueryChange("") }
                        } else {
                            Modifier
                        }
                    )
            )
        }

        Text(
            "Cancel",
            color = R4.Cyan,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(cancelSlot)
                .graphicsLayer {
                    alpha = ((progress - 0.45f) / 0.55f).coerceIn(0f, 1f)
                    translationX = (1f - progress.coerceIn(0f, 1f)) * 60f
                }
                .then(if (expanded) Modifier.bouncyClick(0.9f) { close() } else Modifier)
                .padding(start = 12.dp),
        )
    }
}