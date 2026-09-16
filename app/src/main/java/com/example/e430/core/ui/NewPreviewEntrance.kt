package com.example.e430.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Plays the entrance once for an item in the current [resultKey] result set. */
@Composable
fun NewPreviewEntrance(
    resultKey: Any,
    itemKey: Any,
    animatedKeys: MutableSet<Any>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val hasAnimated = remember(resultKey, itemKey) { itemKey in animatedKeys }
    var visible by remember(resultKey, itemKey) { mutableStateOf(hasAnimated) }

    LaunchedEffect(resultKey, itemKey) {
        if (!hasAnimated) {
            animatedKeys += itemKey
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { height -> height / 3 },
            animationSpec = tween(
                durationMillis = PREVIEW_ENTRANCE_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        ) + fadeIn(
            animationSpec = tween(durationMillis = PREVIEW_ENTRANCE_DURATION_MILLIS),
        ),
        content = { content() },
    )
}

private const val PREVIEW_ENTRANCE_DURATION_MILLIS = 240
