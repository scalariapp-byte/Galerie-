package com.galerie.app.ui

import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DatePicker
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import com.galerie.app.data.Album
import com.galerie.app.data.Media
import com.galerie.app.data.Thumb
import kotlinx.coroutines.delay
import java.util.Calendar

fun smartIcon(k: SmartKind): ImageVector = when (k) {
    SmartKind.FAVORITES -> Icons.Filled.Favorite
    SmartKind.VIDEOS -> Icons.Filled.Videocam
    SmartKind.SCREENSHOTS -> Icons.Filled.Screenshot
    SmartKind.GIF -> Icons.Filled.Gif
    SmartKind.HD -> Icons.Filled.Hd
    SmartKind.PANORAMA -> Icons.Filled.Panorama
    SmartKind.RECENT -> Icons.Filled.NewReleases
    SmartKind.LARGE -> Icons.Filled.SdStorage
    SmartKind.DUPLICATES -> Icons.Filled.ContentCopy
}

@Composable
private fun CenterMessage(icon: ImageVector, text: String) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}

// ======================= BARRE DE RECHERCHE (en haut) =======================

@Composable
fun SearchPill(hint: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().height(42.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(hint, fontSize = fixedSp(15f), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(vm: GalleryViewModel, hint: String, scroll: TopAppBarScrollBehavior? = null, actions: @Composable RowScope.() -> Unit) {
    val bg = MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
    TopAppBar(
        title = { SearchPill(hint, onClick = { vm.push(Screen.Search) }) },
        actions = actions,
        expandedHeight = 58.dp,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = bg, scrolledContainerColor = bg),
        scrollBehavior = scroll,
    )
}

// ======================= PHOTOS =======================

@Composable
fun PhotosTab(vm: GalleryViewModel, outer: PaddingValues) {
    val items by vm.timeline.collectAsState()
    val loaded by vm.loaded.collectAsState()
    MediaTab(vm, outer, Source.Timeline, items, "Galerie", Format.plural(items.size, "élément", "éléments"), loaded, showFilter = true)
}

@Composable
fun SourceTab(vm: GalleryViewModel, outer: PaddingValues, source: Source, title: String, unit: Pair<String, String>) {
    val items by rememberItems(vm, source)
    val loaded by vm.loaded.collectAsState()
    MediaTab(vm, outer, source, items, title, Format.plural(items.size, unit.first, unit.second), loaded, showFilter = false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaTab(
    vm: GalleryViewModel, outer: PaddingValues, source: Source, items: List<Media>,
    title: String, subtitle: String, loaded: Boolean, showFilter: Boolean,
) {
    val settings by vm.settings.collectAsState()
    val entries = rememberEntries(items, settings, dated = true)
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf(false) }
    HeaderScaffold(vm, title, subtitle, items, actions = {
        if (items.isNotEmpty()) RoundIconButton(Icons.Filled.CalendarMonth, "Aller à une date") { picking = true }
        SortMenu(vm, settings, showFilter)
        OverflowMenu(vm)
    }) { top ->
        when {
            !loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            items.isEmpty() -> CenterMessage(Icons.Filled.PhotoLibrary, "Rien ici pour l'instant")
            else -> MediaGrid(
                entries = entries,
                columns = settings.columns,
                onColumnsChange = { c -> vm.update { it.copy(columns = c) } },
                selection = vm.selection,
                onSelectionChange = { vm.selection = it },
                onOpen = { vm.push(Screen.Viewer(source, it.id)) },
                state = gridState,
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = top, bottom = outer.calculateBottomPadding() + 8.dp),
                focusId = vm.lastViewedId,
            )
        }
    }
    if (picking) {
        DateJumpDialog(items, onDismiss = { picking = false }) { day ->
            picking = false
            val byAdded = settings.sort == SortMode.ADDED
            val first = entries.indexOfFirst { e ->
                e is MediaEntry && Format.dayKey(if (byAdded) e.media.dateAdded else e.media.date) == day
            }
            // On remonte sur l'en-tête du jour s'il existe
            val idx = if (first > 0 && entries[first - 1] is HeaderEntry) first - 1 else first
            if (idx >= 0) scope.launch { gridState.scrollToItem(idx) }
        }
    }
}

/**
 * Calendrier : seuls les jours qui contiennent des photos sont sélectionnables,
 * et le nombre de photos du jour choisi s'affiche en titre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateJumpDialog(items: List<Media>, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val perDay = remember(items) { items.groupingBy { Format.dayKey(it.date) }.eachCount() }
    val years = remember(perDay) { perDay.keys.map { java.time.LocalDate.ofEpochDay(it).year }.toSet() }
    val latest = remember(items) { items.maxOfOrNull { it.date } }
    val state = rememberDatePickerState(
        initialDisplayedMonthMillis = latest?.let { Format.dayKey(it) * 86_400_000L },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = (utcTimeMillis / 86_400_000L) in perDay
            override fun isSelectableYear(year: Int) = year in years
        },
    )
    val selectedDay = state.selectedDateMillis?.let { it / 86_400_000L }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = selectedDay != null, onClick = { selectedDay?.let(onPick) }) { Text("Afficher") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } },
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    "Jours en clair = jours avec des photos",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            headline = {
                val n = selectedDay?.let { perDay[it] } ?: 0
                Text(
                    if (selectedDay == null) "Choisissez un jour" else Format.plural(n, "élément ce jour-là", "éléments ce jour-là"),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                )
            },
            showModeToggle = false,
        )
    }
}

@Composable
fun SortMenu(vm: GalleryViewModel, settings: Settings, showFilter: Boolean) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundIconButton(Icons.Filled.Tune, "Trier et filtrer") { open = true }
        DropdownMenu(open, { open = false }) {
            Text("Trier par", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            SortMode.entries.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.label) },
                    leadingIcon = { RadioButton(settings.sort == m, null) },
                    onClick = { vm.update { it.copy(sort = m) } },
                )
            }
            DropdownMenuItem(
                text = { Text("Ordre croissant") },
                leadingIcon = { Checkbox(settings.ascending, null) },
                onClick = { vm.update { it.copy(ascending = !it.ascending) } },
            )
            if (showFilter) {
                HorizontalDivider()
                Text("Afficher", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                Filter.entries.forEach { f ->
                    DropdownMenuItem(
                        text = { Text(f.label) },
                        leadingIcon = { RadioButton(settings.filter == f, null) },
                        onClick = { vm.update { it.copy(filter = f) }; open = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(vm: GalleryViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundIconButton(Icons.Filled.MoreVert, "Plus") { open = true }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text("Libérer de l'espace") }, { open = false; vm.push(Screen.Cleanup) }, leadingIcon = { Icon(Icons.Filled.CleaningServices, null) })
            DropdownMenuItem({ Text("Corbeille") }, { open = false; vm.push(Screen.Collection(Source.Trash, "Corbeille")) }, leadingIcon = { Icon(Icons.Filled.Delete, null) })
            DropdownMenuItem({ Text("Paramètres") }, { open = false; vm.push(Screen.SettingsScreen) }, leadingIcon = { Icon(Icons.Filled.Settings, null) })
        }
    }
}

// ======================= ALBUMS (tout visible sans défiler) =======================

data class SmartItem(val icon: ImageVector, val label: String, val count: Int, val color: Color, val open: () -> Unit)

fun smartColor(k: SmartKind): Color = when (k) {
    SmartKind.FAVORITES -> Color(0xFFFF5C8A)
    SmartKind.VIDEOS -> Color(0xFF5AA9FF)
    SmartKind.SCREENSHOTS -> Color(0xFFB388FF)
    SmartKind.RECENT -> Color(0xFFFFC14D)
    SmartKind.GIF -> Color(0xFF4DE0A0)
    SmartKind.HD -> Color(0xFF4DD8FF)
    SmartKind.PANORAMA -> Color(0xFF8C9EFF)
    SmartKind.LARGE -> Color(0xFFFF9A4D)
    SmartKind.DUPLICATES -> Color(0xFF26D0C0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsTab(vm: GalleryViewModel, outer: PaddingValues) {
    val albums by vm.albums.collectAsState()
    val settings by vm.settings.collectAsState()
    HeaderScaffold(vm, "Albums", Format.plural(albums.size, "album", "albums"), emptyList(), actions = {
        var open by remember { mutableStateOf(false) }
        Box {
            RoundIconButton(Icons.Filled.Sort, "Trier") { open = true }
            DropdownMenu(open, { open = false }) {
                AlbumSort.entries.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.label) },
                        leadingIcon = { RadioButton(settings.albumSort == s, null) },
                        onClick = { vm.update { it.copy(albumSort = s) }; open = false },
                    )
                }
            }
        }
        OverflowMenu(vm)
    }) { top ->
        val hPad = 14.dp
        val gap = 10.dp
        Column(Modifier.fillMaxSize().padding(top = top, bottom = outer.calculateBottomPadding())) {
            // Grille qui remplit exactement l'espace restant : tout est visible sans défiler.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = hPad).padding(top = 6.dp, bottom = 4.dp)) {
                val n = albums.size
                var layout: Triple<Int, Dp, Dp>? = null
                if (n > 0) for (cols in 2..10) {
                    val tw = (maxWidth - gap * (cols - 1)) / cols
                    val rows = (n + cols - 1) / cols
                    if (tw * rows + gap * (rows - 1) <= maxHeight) {
                        val th = minOf(tw * 1.4f, (maxHeight - gap * (rows - 1)) / rows)
                        layout = Triple(cols, tw, th)
                        break
                    }
                }
                val l = layout
                if (l != null) {
                    val (cols, tw, th) = l
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        albums.chunked(cols).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                row.forEach { a ->
                                    AlbumTile(
                                        a, hidden = settings.hiddenAlbums.containsAll(a.bucketIds), width = tw,
                                        modifier = Modifier.size(tw, th),
                                        onClick = { vm.push(Screen.Collection(Source.AlbumSrc(a.bucketIds), a.name)) },
                                        onToggleHidden = { vm.toggleHidden(a.bucketIds) },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        items(albums, key = { it.id }) { a ->
                            AlbumTile(
                                a, hidden = settings.hiddenAlbums.containsAll(a.bucketIds), width = 64.dp,
                                modifier = Modifier.aspectRatio(1f),
                                onClick = { vm.push(Screen.Collection(Source.AlbumSrc(a.bucketIds), a.name)) },
                                onToggleHidden = { vm.toggleHidden(a.bucketIds) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartTile(s: SmartItem, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = s.open).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(30.dp).background(s.color.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(s.icon, null, tint = s.color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(s.label, fontSize = fixedSp(11f), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        Text(if (s.count > 0) "${s.count}" else "–", fontSize = fixedSp(10f), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumTile(a: Album, hidden: Boolean, width: Dp, modifier: Modifier, onClick: () -> Unit, onToggleHidden: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val tiny = width < 72.dp
    Box(
        modifier.clip(RoundedCornerShape(if (tiny) 12.dp else 18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(if (tiny) 12.dp else 18.dp))
            .combinedClickable(onClick = onClick, onLongClick = { menu = true }),
    ) {
        AsyncImage(Thumb(a.cover.uri, a.cover.dateModified), a.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(0.55f).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = if (tiny) 6.dp else 10.dp, vertical = if (tiny) 5.dp else 8.dp)) {
            Text(
                a.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontSize = fixedSp(if (tiny) 10f else 13f), lineHeight = fixedSp(if (tiny) 12f else 16f),
            )
            Text(
                "${a.count}", color = Color.White.copy(alpha = 0.72f),
                fontSize = fixedSp(if (tiny) 9f else 11f), lineHeight = fixedSp(if (tiny) 11f else 14f),
            )
        }
        if (hidden) {
            Icon(
                Icons.Filled.VisibilityOff, "Masqué de Photos", tint = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp)
                    .background(Color(0x66000000), CircleShape).padding(2.dp),
            )
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (hidden) "Afficher dans Photos" else "Masquer de Photos") },
                leadingIcon = { Icon(if (hidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) },
                onClick = { menu = false; onToggleHidden() },
            )
        }
    }
}

// ======================= RECHERCHE (écran ouvert depuis la barre du haut) =======================

@Composable
fun SearchScreen(vm: GalleryViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by remember { mutableStateOf(query.trim()) }
    LaunchedEffect(query) { delay(180); debounced = query.trim() }
    val settings by vm.settings.collectAsState()
    val all by vm.all.collectAsState()
    val albums by vm.albums.collectAsState()
    val years = remember(all) {
        all.asSequence().map { Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.YEAR) }.distinct().sortedDescending().take(8).toList()
    }
    val source = Source.Search(debounced)
    val items by rememberItems(vm, source)
    val focus = remember { FocusRequester() }
    var focusedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!focusedOnce) { focusedOnce = true; runCatching { focus.requestFocus() } } }

    Scaffold(
        topBar = {
            if (vm.selection.isNotEmpty()) SelectionTopBar(vm, items)
            else Column(Modifier.statusBarsPadding().padding(top = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 12.dp)) {
                    IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Nom, album, date, « vidéo »…") },
                        trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Filled.Clear, "Effacer") } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape = RoundedCornerShape(26.dp),
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                    )
                }
                val chips = listOf("Vidéos" to "video", "Favoris" to "favori", "Captures" to "capture", "GIF" to "gif", "Panoramas" to "panorama", "HD" to "hd") +
                    years.map { "$it" to "$it" }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(chips) { (label, q) ->
                        FilterChip(selected = Format.normalize(query).split(' ').contains(q), onClick = {
                            val terms = query.trim().split(' ').filter { it.isNotBlank() }.toMutableList()
                            if (terms.any { Format.normalize(it) == q }) terms.removeAll { Format.normalize(it) == q } else terms += q
                            query = terms.joinToString(" ")
                        }, label = { Text(label) })
                    }
                }
            }
        },
        bottomBar = { if (vm.selection.isNotEmpty()) SelectionBottomBar(vm, trashMode = false, floating = false) },
    ) { inner ->
        Box(Modifier.padding(top = inner.calculateTopPadding()).fillMaxSize()) {
            if (debounced.isBlank()) {
                // Albums correspondants en accès direct
                LazyColumn(contentPadding = PaddingValues(bottom = inner.calculateBottomPadding() + 16.dp)) {
                    item {
                        Text("Albums", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp, 8.dp))
                    }
                    items(albums, key = { it.id }) { a ->
                        ListItem(
                            leadingContent = {
                                AsyncImage(Thumb(a.cover.uri, a.cover.dateModified), null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                            },
                            headlineContent = { Text(a.name) },
                            supportingContent = { Text("${a.count}") },
                            modifier = Modifier.clickable { vm.push(Screen.Collection(Source.AlbumSrc(a.bucketIds), a.name)) },
                        )
                    }
                }
            } else if (items.isEmpty()) {
                CenterMessage(Icons.Filled.Search, "Aucun résultat pour « $debounced »")
            } else {
                Column {
                    Text(
                        Format.plural(items.size, "résultat", "résultats"),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    MediaGrid(
                        entries = rememberEntries(items, settings, dated = true),
                        columns = settings.columns,
                        onColumnsChange = { c -> vm.update { it.copy(columns = c) } },
                        selection = vm.selection,
                        onSelectionChange = { vm.selection = it },
                        onOpen = { vm.push(Screen.Viewer(source, it.id)) },
                        contentPadding = PaddingValues(bottom = inner.calculateBottomPadding() + 8.dp),
                        focusId = vm.lastViewedId,
                    )
                }
            }
        }
    }
}

// ======================= COLLECTION (album, favoris, corbeille…) =======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(vm: GalleryViewModel, source: Source, title: String) {
    val items by rememberItems(vm, source)
    val settings by vm.settings.collectAsState()
    val trashMode = source == Source.Trash
    val dated = !trashMode && !(source is Source.Smart && (source.kind == SmartKind.LARGE || source.kind == SmartKind.DUPLICATES))
    val entries = rememberEntries(items, settings, dated)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val totalSize = remember(items) { items.sumOf { it.size } }
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            if (vm.selection.isNotEmpty()) SelectionTopBar(vm, items)
            else LargeTopAppBar(
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${Format.plural(items.size, "élément", "éléments")} · ${Format.size(totalSize)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
                actions = {
                    if (items.isNotEmpty()) IconButton({ vm.selection = items.map { it.id }.toSet() }) { Icon(Icons.Filled.SelectAll, "Tout sélectionner") }
                    if (trashMode && items.isNotEmpty()) IconButton({ vm.emptyTrash() }) { Icon(Icons.Filled.Delete, "Vider la corbeille") }
                    if (!trashMode) SortMenu(vm, settings, showFilter = false)
                },
                scrollBehavior = scroll,
            )
        },
        bottomBar = { if (vm.selection.isNotEmpty()) SelectionBottomBar(vm, trashMode, floating = false) },
    ) { inner ->
        if (items.isEmpty()) {
            Box(Modifier.padding(inner)) {
                CenterMessage(if (trashMode) Icons.Filled.Delete else Icons.Outlined.Collections, if (trashMode) "La corbeille est vide" else "Aucun élément")
            }
        } else {
            Column(Modifier.padding(top = inner.calculateTopPadding())) {
                if (trashMode) {
                    Text(
                        "Les éléments sont supprimés définitivement après 30 jours.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                if (source == Source.Smart(SmartKind.DUPLICATES)) {
                    Text(
                        "Copies détectées (même taille, mêmes dimensions). L'original le plus ancien n'est pas listé.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                MediaGrid(
                    entries = entries,
                    columns = settings.columns,
                    onColumnsChange = { c -> vm.update { it.copy(columns = c) } },
                    selection = vm.selection,
                    onSelectionChange = { vm.selection = it },
                    onOpen = { vm.push(Screen.Viewer(source, it.id)) },
                    contentPadding = PaddingValues(bottom = inner.calculateBottomPadding() + 8.dp),
                    focusId = vm.lastViewedId,
                    trashMode = trashMode,
                )
            }
        }
    }
}

// ======================= LIBÉRER DE L'ESPACE =======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(vm: GalleryViewModel) {
    val all by vm.all.collectAsState()
    val trash by vm.trash.collectAsState()
    val counts by vm.smartCounts.collectAsState()
    val photos = remember(all) { all.filter { !it.isVideo } }
    val videos = remember(all) { all.filter { it.isVideo } }
    val shots = remember(all) { all.filter { it.isScreenshot } }
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Libérer de l'espace") },
                navigationIcon = { IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
            )
        },
    ) { inner ->
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding() + 24.dp)) {
            item {
                Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Espace utilisé par vos médias", style = MaterialTheme.typography.labelLarge)
                        Text(Format.size(all.sumOf { it.size }), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${Format.plural(photos.size, "photo", "photos")} (${Format.size(photos.sumOf { it.size })}) · " +
                                "${Format.plural(videos.size, "vidéo", "vidéos")} (${Format.size(videos.sumOf { it.size })})",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    val dup = counts[SmartKind.DUPLICATES] ?: (0 to 0L)
                    val large = counts[SmartKind.LARGE] ?: (0 to 0L)
                    CleanRow(Icons.Filled.ContentCopy, "Doublons", dup.first, dup.second) { vm.push(Screen.Collection(Source.Smart(SmartKind.DUPLICATES), "Doublons")) }
                    CleanRow(Icons.Filled.SdStorage, "Gros fichiers (> 20 Mo)", large.first, large.second) { vm.push(Screen.Collection(Source.Smart(SmartKind.LARGE), "Gros fichiers")) }
                    CleanRow(Icons.Filled.Screenshot, "Captures d'écran", shots.size, shots.sumOf { it.size }) { vm.push(Screen.Collection(Source.Smart(SmartKind.SCREENSHOTS), "Captures d'écran")) }
                    CleanRow(Icons.Filled.Videocam, "Vidéos", videos.size, videos.sumOf { it.size }) { vm.push(Screen.Collection(Source.Smart(SmartKind.VIDEOS), "Vidéos")) }
                    CleanRow(Icons.Filled.Delete, "Corbeille", trash.size, trash.sumOf { it.size }) { vm.push(Screen.Collection(Source.Trash, "Corbeille")) }
                }
            }
        }
    }
}

@Composable
private fun CleanRow(icon: ImageVector, label: String, n: Int, size: Long, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(label) },
        supportingContent = { Text("${Format.plural(n, "élément", "éléments")} · ${Format.size(size)}") },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

// ======================= PARAMÈTRES =======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: GalleryViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val albums by vm.albums.collectAsState()
    var canManage by remember { mutableStateOf(Perms.canManageMedia(ctx)) }
    LifecycleResumeEffect(Unit) { canManage = Perms.canManageMedia(ctx); onPauseOrDispose { } }
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = { IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
            )
        },
    ) { inner ->
        LazyColumn(contentPadding = PaddingValues(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding() + 24.dp)) {
            item { Section("Apparence") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ThemeMode.entries) { t ->
                        FilterChip(selected = s.theme == t, onClick = { vm.update { it.copy(theme = t) } }, label = { Text(t.label) })
                    }
                }
            }
            item { SwitchRow("Couleurs du fond d'écran", "Remplace la palette Galerie par Material You", s.dynamicColor) { v -> vm.update { it.copy(dynamicColor = v) } } }
            item {
                ListItem(
                    headlineContent = { Text("Colonnes de la grille : ${s.columns}") },
                    supportingContent = {
                        Column {
                            Text("Astuce : pincez la grille pour zoomer / dézoomer.")
                            Slider(s.columns.toFloat(), { v -> vm.update { it.copy(columns = v.toInt()) } }, valueRange = 2f..8f, steps = 5)
                        }
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Panneau des collections") },
                    supportingContent = { Text("Glisser depuis le bord ${if (s.panelLeft) "gauche" else "droit"} de l'écran") },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(s.panelLeft, { vm.update { it.copy(panelLeft = true) } }, { Text("Gauche") })
                            FilterChip(!s.panelLeft, { vm.update { it.copy(panelLeft = false) } }, { Text("Droite") })
                        }
                    },
                )
            }
            item { Section("Vidéos") }
            item { SwitchRow("Lecture automatique", "Lancer la vidéo à l'ouverture", s.autoplay) { v -> vm.update { it.copy(autoplay = v) } } }
            item { SwitchRow("Lecture en boucle", null, s.loopVideos) { v -> vm.update { it.copy(loopVideos = v) } } }
            item { Section("Diaporama") }
            item {
                ListItem(
                    headlineContent = { Text("Durée par photo : ${s.slideshowSeconds} s") },
                    supportingContent = {
                        Slider(s.slideshowSeconds.toFloat(), { v -> vm.update { it.copy(slideshowSeconds = v.toInt()) } }, valueRange = 1f..10f, steps = 8)
                    },
                )
            }
            item { Section("Autorisations") }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    ListItem(
                        headlineContent = { Text("Actions sans confirmation") },
                        supportingContent = {
                            Text(
                                if (canManage) "Activé : suppression, favoris et déplacements sans boîte de dialogue système."
                                else "Autoriser « Gestion des médias » pour supprimer / mettre en favori en un geste.",
                            )
                        },
                        trailingContent = { if (!canManage) OutlinedButton({ Actions.manageMedia(ctx) }) { Text("Activer") } },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text("Accès aux photos et vidéos") },
                    supportingContent = { Text("Ouvrir les paramètres de l'application") },
                    modifier = Modifier.clickable { Actions.appSettings(ctx) },
                )
            }
            val hidden = albums.filter { s.hiddenAlbums.containsAll(it.bucketIds) }
            if (hidden.isNotEmpty()) {
                item { Section("Albums masqués de Photos") }
                items(hidden, key = { it.id }) { a ->
                    ListItem(
                        leadingContent = {
                            AsyncImage(Thumb(a.cover.uri, a.cover.dateModified), null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                        },
                        headlineContent = { Text(a.name) },
                        supportingContent = { Text("${a.count}") },
                        trailingContent = { OutlinedButton({ vm.toggleHidden(a.bucketIds) }) { Text("Afficher") } },
                    )
                }
            }
            item { Section("À propos") }
            item { ListItem(headlineContent = { Text("Galerie 1.7") }, supportingContent = { Text("Aucune donnée ne quitte le téléphone.") }) }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked, onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}
