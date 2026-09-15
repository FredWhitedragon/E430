package com.example.e430.search.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.e430.R
import com.example.e430.core.ui.theme.E430Blue
import com.example.e430.core.ui.theme.E430Gold
import com.example.e430.presets.model.SearchPreset
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHeader(
    query: String,
    filtersVisible: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onMenuClick: () -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    presets: List<SearchPreset>,
    onPresetSelected: (SearchPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var showingPresets by rememberSaveable { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState(query)
    val textScrollState = rememberScrollState()
    val interactionSource = remember { MutableInteractionSource() }
    val currentQuery by rememberUpdatedState(query)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val edgeSizePx = with(LocalDensity.current) { 32.dp.toPx() }
    val minimumStepPx = with(LocalDensity.current) { 4.dp.toPx() }
    val maximumStepPx = with(LocalDensity.current) { 18.dp.toPx() }
    LaunchedEffect(query) {
        if (query != textFieldState.text.toString()) {
            textFieldState.edit {
                replace(0, length, query)
                selection = TextRange(length)
            }
        }
    }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { updated ->
                if (updated != currentQuery) currentOnQueryChange(updated)
            }
    }
    LaunchedEffect(filtersVisible) {
        if (!filtersVisible) showingPresets = false
    }

    Surface(
        color = E430Blue,
        shadowElevation = 5.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        onMenuClick()
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = stringResource(R.string.menu),
                        tint = Color.White,
                    )
                }
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = E430Gold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color(0xFF17232C),
                    unfocusedTextColor = Color(0xFF17232C),
                    focusedLeadingIconColor = E430Blue,
                    unfocusedLeadingIconColor = E430Blue,
                )
                BasicTextField(
                    state = textFieldState,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    onKeyboardAction = {
                        onSearch(textFieldState.text.toString())
                        focusManager.clearFocus()
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF17232C)),
                    cursorBrush = SolidColor(E430Blue),
                    interactionSource = interactionSource,
                    scrollState = textScrollState,
                    decorator = { innerTextField ->
                        OutlinedTextFieldDefaults.DecorationBox(
                            value = textFieldState.text.toString(),
                            innerTextField = innerTextField,
                            enabled = true,
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = interactionSource,
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = null,
                                )
                            },
                            colors = fieldColors,
                            container = {
                                OutlinedTextFieldDefaults.Container(
                                    enabled = true,
                                    isError = false,
                                    interactionSource = interactionSource,
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(24.dp),
                                )
                            },
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .pointerInput(textScrollState, edgeSizePx) {
                            var pointerX = 0f
                            var hasDragged = false
                            coroutineScope {
                                val autoScrollJob = launch {
                                    while (isActive) {
                                        withFrameNanos { }
                                        if (!hasDragged) continue
                                        val edgeProgress = when {
                                            pointerX < edgeSizePx ->
                                                -((edgeSizePx - pointerX) / edgeSizePx).coerceIn(0f, 1f)
                                            pointerX > size.width - edgeSizePx ->
                                                ((pointerX - (size.width - edgeSizePx)) / edgeSizePx).coerceIn(0f, 1f)
                                            else -> 0f
                                        }
                                        if (edgeProgress != 0f) {
                                            val step = minimumStepPx +
                                                (maximumStepPx - minimumStepPx) * kotlin.math.abs(edgeProgress)
                                            textScrollState.scrollBy(
                                                step * if (edgeProgress < 0f) -1f else 1f,
                                            )
                                        }
                                    }
                                }
                                try {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Initial,
                                        )
                                        pointerX = down.position.x
                                        hasDragged = false
                                        var pressed: Boolean
                                        do {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val change = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null) {
                                                pressed = false
                                            } else {
                                                pointerX = change.position.x
                                                hasDragged = hasDragged || hypot(
                                                    change.position.x - down.position.x,
                                                    change.position.y - down.position.y,
                                                ) >= viewConfiguration.touchSlop
                                                pressed = change.pressed
                                            }
                                        } while (pressed)
                                        hasDragged = false
                                    }
                                } finally {
                                    autoScrollJob.cancelAndJoin()
                                }
                            }
                        }
                        .onFocusChanged { onSearchFocusChange(it.isFocused) },
                )
            }
            AnimatedVisibility(
                visible = filtersVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (showingPresets) {
                        SearchDropDownRow(stringResource(R.string.back)) { showingPresets = false }
                        presets.sortedBy { it.name.lowercase() }.forEach { preset ->
                            SearchDropDownRow(preset.name) {
                                onPresetSelected(preset)
                                focusManager.clearFocus()
                            }
                        }
                        if (presets.isEmpty()) {
                            Text(
                                stringResource(R.string.no_presets),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                            )
                        }
                    } else {
                        SearchDropDownRow(stringResource(R.string.import_preset)) {
                            showingPresets = true
                        }
                        Text(
                            text = stringResource(R.string.search_filters),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchDropDownRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    )
}
