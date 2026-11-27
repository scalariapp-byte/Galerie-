package com.galerie.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.galerie.app.data.Media
import com.galerie.app.data.Thumb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.calculateZoom
import kotlin.math.roundToInt

// ---------------- Entrées de grille ----------------

sealed interface Entry {
    val key: Any
}

data class HeaderEntry(override val key: String, val title: String, val ids: List<Long>, val label: String) : Entry
data class MediaEntry(val media: Media, val label: String) : Entry {
    override val key: Any get() = media.id
}

enum class Grouping { NONE, DAY, MONTH }

fun buildEntries(items: List<Media>, grouping: Grouping, byAdded: Boolean): List<Entry> {
    val out = ArrayList<Entry>(items.size + 64)
    if (grouping == Grouping.NONE) {
        items.forEach { out += MediaEntry(it, Format.monthLabel(if (byAdded) it.dateAdded else it.date)) }
        return out
    }
    var i = 0
    while (i < items.size) {
        val first = items[i]
        val d0 = if (byAdded) first.dateAdded else first.date
        val k0 = if (grouping == Grouping.DAY) Format.dayKey(d0) else Format.monthKey(d0).toLong()
        var j = i
        while (j < items.size) {
            val d = if (byAdded) items[j].dateAdded else items[j].date
            val k = if (grouping == Grouping.DAY) Format.dayKey(d) else Format.monthKey(d).toLong()
            if (k != k0) break
            j++
        }
        val group = items.subList(i, j)
        val month = Format.monthLabel(d0)
        val title = if (grouping == Grouping.DAY) Format.dayLabel(d0) else month
        out += HeaderEntry("h$grouping$k0", title, group.map { it.id }, month)
        group.forEach { out += MediaEntry(it, month) }
        i = j
    }
    return out
}

// ---------------- Grille ----------------

@Composable
fun MediaGrid(
    entries: List<Entry>,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    selection: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onOpen: (Media) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(),
    focusId: Long? = null,
    trashMode: Boolean = false,
    header: (LazyGridScope.() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentEntries by rememberUpdatedState(entries)
    val currentSelection by rememberUpdatedState(selection)
    val currentColumns by rememberUpdatedState(columns)
    val onSel by rememberUpdatedState(onSelectionChange)
    val onOpenCur by rememberUpdatedState(onOpen)
    val onCols by rememberUpdatedState(onColumnsChange)
    val selectionMode = selection.isNotEmpty()
    val gap = if (columns >= 6) 2.dp else if (columns >= 5) 3.dp else 4.dp
    val corner = if (columns >= 6) 5.dp else if (columns >= 5) 7.dp else 10.dp
    var autoScroll by remember { mutableFloatStateOf(0f) }
    var dragUpdate by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Revient sur la dernière photo vue dans la visionneuse si elle est hors écran.
    LaunchedEffect(focusId) {
        val id = focusId ?: return@LaunchedEffect
        val visible = state.layoutInfo.visibleItemsInfo.any { it.key == id }
        if (!visible) {
            val idx = currentEntries.indexOfFirst { it is MediaEntry && it.media.id == id }
            if (idx >= 0) state.scrollToItem(idx + state.layoutInfo.totalItemsCount - currentEntries.size)
        }
    }

    LaunchedEffect(autoScroll) {
        while (autoScroll != 0f) {
            state.scrollBy(autoScroll)
            dragUpdate?.invoke()
            delay(16)
        }
    }

    val currentPadding by rememberUpdatedState(contentPadding)
    // Les offsets des éléments ne comptent pas la marge intérieure (en-tête en haut, marge à gauche) :
    // on convertit d'abord la position du doigt dans ce repère.
    fun keyAt(p: Offset): Any? {
        val info = state.layoutInfo
        val x = p.x - with(density) { currentPadding.calculateStartPadding(LayoutDirection.Ltr).toPx() }
        val y = p.y + info.viewportStartOffset
        return info.visibleItemsInfo.firstOrNull {
            x >= it.offset.x && x < it.offset.x + it.size.width && y >= it.offset.y && y < it.offset.y + it.size.height
        }?.key
    }

    fun indexOfKey(k: Any?): Int = if (k == null) -1 else currentEntries.indexOfFirst { it.key == k }

    Box(modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = state,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier
                .fillMaxSize()
                // Pincer pour changer le nombre de colonnes (2 à 8)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var acc = 1f
                        do {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            if (ev.changes.count { it.pressed } >= 2) {
                                acc *= ev.calculateZoom()
                                if (acc > 1.35f) { onCols((currentColumns - 1).coerceAtLeast(2)); acc = 1f }
                                else if (acc < 0.74f) { onCols((currentColumns + 1).coerceAtMost(8)); acc = 1f }
                                ev.changes.forEach { it.consume() }
                            }
                        } while (ev.changes.any { it.pressed })
                    }
                }
                // Appui = ouvrir / cocher ; appui long + glisser = sélection multiple
                .pointerInput(Unit) {
                    val edge = with(density) { 72.dp.toPx() }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var cancelled = false
                        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation().also { if (it == null) cancelled = true }
                        }
                        if (up != null) {
                            when (val e = currentEntries.getOrNull(indexOfKey(keyAt(up.position)))) {
                                is MediaEntry -> {
                                    up.consume()
                                    if (currentSelection.isEmpty()) onOpenCur(e.media)
                                    else onSel(if (e.media.id in currentSelection) currentSelection - e.media.id else currentSelection + e.media.id)
                                }
                                is HeaderEntry -> if (currentSelection.isNotEmpty()) {
                                    up.consume()
                                    val all = currentSelection.containsAll(e.ids)
                                    onSel(if (all) currentSelection - e.ids.toSet() else currentSelection + e.ids)
                                }
                                null -> Unit
                            }
                            return@awaitEachGesture
                        }
                        if (cancelled) return@awaitEachGesture
                        val startIdx = indexOfKey(keyAt(down.position))
                        val startEntry = currentEntries.getOrNull(startIdx) ?: return@awaitEachGesture
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (startEntry is HeaderEntry) {
                            onSel(currentSelection + startEntry.ids)
                            return@awaitEachGesture
                        }
                        val base = currentSelection
                        val adding = (startEntry as MediaEntry).media.id !in base
                        var lastIdx = -1
                        var pos = down.position
                        fun apply() {
                            val idx = indexOfKey(keyAt(pos))
                            if (idx < 0 || idx == lastIdx) return
                            lastIdx = idx
                            val range = (minOf(startIdx, idx)..maxOf(startIdx, idx))
                                .mapNotNull { (currentEntries.getOrNull(it) as? MediaEntry)?.media?.id }
                            onSel(if (adding) base + range else base - range.toSet())
                        }
                        apply()
                        dragUpdate = ::apply
                        while (true) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            ch.consume()
                            if (!ch.pressed) break
                            pos = ch.position
                            autoScroll = when {
                                pos.y < edge -> -((edge - pos.y) / edge) * 40f
                                pos.y > size.height - edge -> ((pos.y - (size.height - edge)) / edge) * 40f
                                else -> 0f
                            }
                            apply()
                        }
                        autoScroll = 0f
                        dragUpdate = null
                    }
                },
        ) {
            header?.invoke(this)
            items(
                count = entries.size,
                key = { entries[it].key },
                span = { if (entries[it] is HeaderEntry) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                contentType = { if (entries[it] is HeaderEntry) 0 else 1 },
            ) { i ->
                when (val e = entries[i]) {
                    is HeaderEntry -> GroupHeader(e, selectionMode, selectionMode && selection.containsAll(e.ids))
                    is MediaEntry -> MediaCell(e.media, e.media.id in selection, selectionMode, trashMode, corner)
                }
            }
        }
        FastScroller(state) { idx ->
            val offset = state.layoutInfo.totalItemsCount - currentEntries.size
            when (val e = currentEntries.getOrNull(idx - offset)) {
                is HeaderEntry -> e.label
                is MediaEntry -> e.label
                null -> null
            }
        }
    }
}

@Composable
private fun GroupHeader(e: HeaderEntry, selectionMode: Boolean, allSelected: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(start = 2.dp, end = 0.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(16.dp).background(VioletGradient, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(e.title, fontSize = fixedSp(14f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.padding(start = 9.dp, end = if (selectionMode) 6.dp else 9.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${e.ids.size}", fontSize = fixedSp(12f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                if (selectionMode) Icon(
                    if (allSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    null, modifier = Modifier.size(18.dp),
                    tint = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun MediaCell(m: Media, selected: Boolean, selectionMode: Boolean, trashMode: Boolean, corner: androidx.compose.ui.unit.Dp = 14.dp) {
    val ctx = LocalContext.current
    val scale by animateFloatAsState(if (selected) 0.84f else 1f, label = "sel")
    val request = remember(m.id, m.dateModified) {
        ImageRequest.Builder(ctx).data(Thumb(m.uri, m.dateModified)).build()
    }
    Box(Modifier.aspectRatio(1f).border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.22f), RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(corner))) {
        AsyncImage(
            model = request,
            contentDescription = m.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    clip = true
                    shape = RoundedCornerShape(corner)
                },
        )
        if (m.isVideo) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(6.dp)
                    .background(Color(0x88000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Text(Format.duration(m.duration), color = Color.White, fontSize = fixedSp(11f), fontWeight = FontWeight.SemiBold)
            }
        }
        if (m.isGif) Badge("GIF", Modifier.align(Alignment.TopEnd))
        if (m.isFavorite) {
            Icon(
                Icons.Filled.Favorite, null, tint = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(14.dp),
            )
        }
        if (trashMode && m.dateExpires > 0) {
            val days = ((m.dateExpires - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
            Badge("${days} j", Modifier.align(Alignment.TopEnd))
        }
        if (selectionMode) {
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.align(Alignment.TopStart).padding(5.dp).size(22.dp)
                    .background(if (selected) Color.White else Color(0x33000000), CircleShape),
            )
        }
    }
}

@Composable
private fun Badge(text: String, modifier: Modifier) {
    Text(
        text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.padding(4.dp).background(Color(0x99000000), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

// ---------------- Barre de défilement rapide ----------------

@Composable
private fun BoxScope.FastScroller(state: LazyGridState, labelFor: (Int) -> String?) {
    val total by remember { derivedStateOf { state.layoutInfo.totalItemsCount } }
    if (total < 80) return
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(false) }
    val scrolling = state.isScrollInProgress
    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) visible = true else { delay(1500); visible = false }
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "fs")
    val fraction by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val visibleCount = info.visibleItemsInfo.size
            val max = (info.totalItemsCount - visibleCount).coerceAtLeast(1)
            (state.firstVisibleItemIndex.toFloat() / max).coerceIn(0f, 1f)
        }
    }
    val f = if (dragging) dragFraction else fraction
    val density = LocalDensity.current
    val thumbH = 52.dp
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.matchParentSize().padding(vertical = 96.dp).alpha(alpha),
    ) {
        val travel = with(density) { (maxHeight - thumbH).toPx() }
        val y = (travel * f).roundToInt()
        if (dragging) {
            val label = labelFor(state.firstVisibleItemIndex)
            if (label != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 44.dp).offset { IntOffset(0, y) },
                ) {
                    Text(
                        label, maxLines = 1, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).fillMaxHeight().width(48.dp).then(
                if (visible) Modifier.pointerInput(total) {
                    fun go(yPos: Float) {
                        dragFraction = (yPos / size.height).coerceIn(0f, 1f)
                        scope.launch { state.scrollToItem((dragFraction * (total - 1)).roundToInt()) }
                    }
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true; go(it.y) },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ -> change.consume(); go(change.position.y) }
                } else Modifier,
            ),
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier.align(Alignment.TopEnd).offset { IntOffset(0, y) }.size(width = 30.dp, height = thumbH),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.UnfoldMore, null, tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
