package com.galerie.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.galerie.app.data.Media
import com.galerie.app.data.Thumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Liste d'une source, calculée en synchrone au premier rendu puis mise à jour en arrière-plan. */
@Composable
fun rememberItems(vm: GalleryViewModel, source: Source): State<List<Media>> {
    val all by vm.all.collectAsState()
    val trash by vm.trash.collectAsState()
    val full by vm.settings.collectAsState()
    val settings = remember(full) { full.queryOnly() }
    val state = remember(source) { mutableStateOf(vm.resolve(source, all, trash, settings)) }
    LaunchedEffect(source, all, trash, settings) {
        state.value = withContext(Dispatchers.Default) { vm.resolve(source, all, trash, settings) }
    }
    return state
}

@Composable
fun rememberEntries(items: List<Media>, settings: Settings, dated: Boolean): List<Entry> {
    val grouping = when {
        !dated || settings.sort == SortMode.NAME || settings.sort == SortMode.SIZE -> Grouping.NONE
        settings.columns >= 6 -> Grouping.MONTH
        else -> Grouping.DAY
    }
    val byAdded = settings.sort == SortMode.ADDED
    return remember(items, grouping, byAdded) { buildEntries(items, grouping, byAdded) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(vm: GalleryViewModel, items: List<Media>) {
    TopAppBar(
        navigationIcon = { IconButton({ vm.selection = emptySet() }) { Icon(Icons.Filled.Close, "Annuler") } },
        title = { Text(Format.plural(vm.selection.size, "sélectionné", "sélectionnés")) },
        actions = {
            IconButton({
                vm.selection = if (vm.selection.size >= items.size) emptySet() else items.map { it.id }.toSet()
            }) { Icon(Icons.Filled.SelectAll, "Tout sélectionner") }
        },
    )
}

@Composable
fun SelectionBottomBar(vm: GalleryViewModel, trashMode: Boolean, floating: Boolean = true) {
    val ctx = LocalContext.current
    var picker by remember { mutableStateOf<Boolean?>(null) }
    val selected = remember(vm.selection) { vm.byIds(vm.selection) }
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = if (floating) 0.dp else 10.dp)
            .then(if (floating) Modifier else Modifier.navigationBarsPadding()),
    ) {
        Row(Modifier.padding(vertical = 4.dp, horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (trashMode) {
                BarAction(Icons.Filled.RestoreFromTrash, "Restaurer") { vm.restore(selected) }
                BarAction(Icons.Filled.DeleteForever, "Supprimer", MaterialTheme.colorScheme.error) { vm.deleteForever(selected) }
            } else {
                BarAction(Icons.Filled.Share, "Partager") { Actions.share(ctx, selected) }
                val allFav = selected.isNotEmpty() && selected.all { it.isFavorite }
                BarAction(if (allFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favori") {
                    vm.setFavorite(selected, !allFav)
                }
                BarAction(Icons.AutoMirrored.Filled.DriveFileMove, "Déplacer") { picker = true }
                BarAction(Icons.Filled.ContentCopy, "Copier") { picker = false }
                BarAction(Icons.Filled.Delete, "Supprimer", MaterialTheme.colorScheme.error) { vm.trashItems(selected) }
            }
        }
    }
    picker?.let { move ->
        AlbumPickerDialog(vm, move, onDismiss = { picker = null }) { path ->
            if (move) vm.move(selected, path) else vm.copy(selected, path)
            picker = null
        }
    }
}

@Composable
fun RowScope.BarAction(icon: ImageVector, label: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint)
        Text(
            label, fontSize = fixedSp(11f), maxLines = 1,
            color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint,
        )
    }
}

@Composable
fun AlbumPickerDialog(vm: GalleryViewModel, move: Boolean, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val albums by vm.albums.collectAsState()
    // MediaStore n'accepte les photos/vidéos que sous DCIM/, Pictures/ et Movies/
    val targets = remember(albums) {
        albums.filter {
            it.relativePath.startsWith("DCIM/") || it.relativePath.startsWith("Pictures/") || it.relativePath.startsWith("Movies/")
        }.distinctBy { it.relativePath }
    }
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (move) "Déplacer vers" else "Copier vers") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.replace("/", "").replace("\\", "") },
                    label = { Text("Nouvel album") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(enabled = newName.isNotBlank(), onClick = { onPick("Pictures/${newName.trim()}/") }) {
                            Icon(Icons.Filled.Check, "Créer")
                        }
                    },
                )
                LazyColumn(Modifier.heightIn(max = 380.dp).padding(top = 8.dp)) {
                    items(targets, key = { it.id }) { a ->
                        ListItem(
                            leadingContent = {
                                AsyncImage(
                                    Thumb(a.cover.uri, a.cover.dateModified), null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                                )
                            },
                            headlineContent = { Text(a.name) },
                            supportingContent = { Text(a.relativePath, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.clickable { onPick(a.relativePath) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } },
    )
}

@Composable
fun RenameDialog(m: Media, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    val ext = m.name.substringAfterLast('.', "")
    val base = if (ext.isEmpty()) m.name else m.name.dropLast(ext.length + 1)
    var text by remember { mutableStateOf(base) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer") },
        text = {
            OutlinedTextField(
                text, { text = it.replace("/", "") }, singleLine = true,
                suffix = { if (ext.isNotEmpty()) Text(".$ext") },
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = {
                onRename(if (ext.isEmpty()) text.trim() else "${text.trim()}.$ext")
            }) { Text("Renommer") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } },
    )
}
