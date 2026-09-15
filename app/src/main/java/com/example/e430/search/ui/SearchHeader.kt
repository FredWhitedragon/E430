package com.example.e430.search.ui

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.e430.search.model.RatingFilter
import com.example.e430.search.model.SearchDateRange
import com.example.e430.search.model.SearchFilterQuery
import com.example.e430.search.model.SearchSort
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
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
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState())
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
                        SearchFilters(
                            query = query,
                            onQueryChange = onQueryChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilters(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val filters = remember(query) { SearchFilterQuery.parse(query) }
    FilterHeading(stringResource(R.string.search_filters))
    FilterHeading(stringResource(R.string.rating))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        RatingFilter.entries.forEach { rating ->
            val label = when (rating) {
                RatingFilter.Safe -> stringResource(R.string.rating_safe)
                RatingFilter.Questionable -> stringResource(R.string.rating_questionable)
                RatingFilter.Explicit -> stringResource(R.string.rating_explicit)
            }
            FilterChip(
                selected = filters.rating == rating,
                onClick = {
                    val selection = rating.takeUnless { filters.rating == rating }
                    onQueryChange(
                        SearchFilterQuery.withRating(
                            query,
                            selection,
                            filters.ratingExcluded,
                        ),
                    )
                },
                label = { Text(label) },
            )
        }
    }
    ToggleRow(
        label = stringResource(R.string.exclude_rating),
        checked = filters.ratingExcluded,
        enabled = filters.rating != null,
        onCheckedChange = { excluded ->
            onQueryChange(SearchFilterQuery.withRating(query, filters.rating, excluded))
        },
    )

    FilterHeading(stringResource(R.string.sort_by))
    SortRow(
        options = listOf(
            SearchSort.Date to stringResource(R.string.date),
            SearchSort.Favorites to stringResource(R.string.favorite_count),
        ),
        selected = filters.sort,
        onSelect = { sort ->
            onQueryChange(
                SearchFilterQuery.withSort(
                    query,
                    sort.takeUnless { filters.sort == sort },
                    filters.ascending,
                ),
            )
        },
    )
    SortRow(
        options = listOf(
            SearchSort.Score to stringResource(R.string.score),
            SearchSort.Comments to stringResource(R.string.comment_count),
        ),
        selected = filters.sort,
        onSelect = { sort ->
            onQueryChange(
                SearchFilterQuery.withSort(
                    query,
                    sort.takeUnless { filters.sort == sort },
                    filters.ascending,
                ),
            )
        },
    )
    ToggleRow(
        label = stringResource(R.string.ascending),
        checked = filters.ascending,
        enabled = filters.sort != null,
        onCheckedChange = { ascending ->
            onQueryChange(SearchFilterQuery.withSort(query, filters.sort, ascending))
        },
    )

    FilterHeading(stringResource(R.string.time_period))
    val today = LocalDate.now().toString()
    val endDate = filters.dateRange.to.ifBlank { today }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        DatePickerButton(
            value = filters.dateRange.from,
            label = stringResource(
                R.string.date_from_value,
                filters.dateRange.from.ifBlank { stringResource(R.string.not_set) },
            ),
            onDateSelected = { from ->
                onQueryChange(
                    SearchFilterQuery.withDateRange(
                        query,
                        SearchDateRange(from = from, to = endDate),
                    ),
                )
            },
            modifier = Modifier.weight(1f),
        )
        DatePickerButton(
            value = endDate,
            label = stringResource(R.string.date_to_value, endDate),
            enabled = filters.dateRange.from.isNotBlank(),
            onDateSelected = { to ->
                onQueryChange(
                    SearchFilterQuery.withDateRange(
                        query,
                        SearchDateRange(from = filters.dateRange.from, to = to),
                    ),
                )
            },
            modifier = Modifier.weight(1f),
        )
    }
    Text(
        text = stringResource(R.string.date_filter_hint),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
    )
    OutlinedButton(
        onClick = {
            onQueryChange(SearchFilterQuery.withDateRange(query, SearchDateRange()))
        },
        enabled = filters.dateRange.from.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.clear_time_period))
    }
}

@Composable
private fun FilterHeading(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun SortRow(
    options: List<Pair<SearchSort, String>>,
    selected: SearchSort?,
    onSelect: (SearchSort) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { (sort, label) ->
            FilterChip(
                selected = selected == sort,
                onClick = { onSelect(sort) },
                label = { Text(label) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun DatePickerButton(
    value: String,
    label: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val initialDate = runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onDateSelected(LocalDate.of(year, month + 1, day).toString())
                },
                initialDate.year,
                initialDate.monthValue - 1,
                initialDate.dayOfMonth,
            ).show()
        },
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(label, maxLines = 1)
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
