package com.galerie.app.ui

import android.app.WallpaperManager
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import coil.compose.AsyncImage
import com.galerie.app.data.Media
import com.galerie.app.data.Thumb
import java.util.concurrent.TimeUnit

fun wallGroupName(path: String): String =
    path.removePrefix(WALLPAPER_PATH).trimEnd('/').ifBlank { "Sans groupe" }

fun wallGroupPath(name: String) = WALLPAPER_PATH + name.trim().replace("/", "") + "/"

// ---------------- Rotation automatique (WorkManager) ----------------

object Rotation {
    private const val WORK = "wallpaper_rotation"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("galerie", Context.MODE_PRIVATE)

    fun activeGroup(ctx: Context): String? = prefs(ctx).getString("rot_group", null)
    fun minutes(ctx: Context): Int = prefs(ctx).getInt("rot_min", 60)

    fun start(ctx: Context, path: String, minutes: Int, flags: Int) {
        prefs(ctx).edit().putString("rot_group", path).putInt("rot_min", minutes).putInt("rot_flags", flags).putInt("rot_index", -1).apply()
        val wm = WorkManager.getInstance(ctx)
        wm.enqueueUniquePeriodicWork(
            WORK, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<WallpaperWorker>(minutes.toLong().coerceAtLeast(15), TimeUnit.MINUTES).build(),
        )
        next(ctx) // applique tout de suite le premier
    }

    fun stop(ctx: Context) {
        prefs(ctx).edit().remove("rot_group").apply()
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
    }

    fun next(ctx: Context) {
        WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<WallpaperWorker>().build())
    }
}

class WallpaperWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("galerie", Context.MODE_PRIVATE)
        val path = prefs.getString("rot_group", null) ?: return Result.success()
        val flags = prefs.getInt("rot_flags", WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        val r = applicationContext.contentResolver
        val coll = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val ids = ArrayList<Long>()
        r.query(coll, arrayOf(MediaStore.MediaColumns._ID), "${MediaStore.MediaColumns.RELATIVE_PATH}=?", arrayOf(path), "${MediaStore.MediaColumns.DATE_ADDED} ASC")?.use {
            while (it.moveToNext()) ids += it.getLong(0)
        }
        if (ids.isEmpty()) return Result.success()
        val idx = (prefs.getInt("rot_index", -1) + 1) % ids.size
        return runCatching {
            r.openInputStream(ContentUris.withAppendedId(coll, ids[idx]))!!.use {
                WallpaperManager.getInstance(applicationContext).setStream(it, null, true, flags)
            }
            prefs.edit().putInt("rot_index", idx).apply()
            Result.success()
        }.getOrDefault(Result.retry())
    }
}

// ---------------- Écran des groupes ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpapersScreen(vm: GalleryViewModel) {
    val ctx = LocalContext.current
    val all by vm.all.collectAsState()
    val groups = remember(all) {
        all.filter { !it.isVideo && it.relativePath.startsWith(WALLPAPER_PATH, true) }
            .groupBy { it.relativePath }.toList().sortedBy { wallGroupName(it.first).lowercase() }
    }
    var active by remember { mutableStateOf(Rotation.activeGroup(ctx)) }
    var rotating by remember { mutableStateOf<String?>(null) }
    var applyRandom by remember { mutableStateOf<List<Media>?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Fonds d'écran") },
                navigationIcon = { IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
            )
        },
    ) { inner ->
        if (groups.isEmpty()) {
            Column(Modifier.padding(inner).fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Wallpaper, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("Aucun groupe pour l'instant", fontWeight = FontWeight.Bold)
                Text(
                    "Sélectionne des photos (appui long) puis « Fonds », ou ouvre une photo › Plus › Fond d'écran › Ajouter à un groupe.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
                )
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(groups, key = { it.first }) { (path, items) ->
                val isActive = active == path
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { vm.push(Screen.Collection(Source.Folder(path), wallGroupName(path))) }.padding(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items.sortedByDescending { it.dateAdded }.take(4).forEach { m ->
                            AsyncImage(
                                Thumb(m.uri, m.dateModified), null, contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).aspectRatio(0.6f).clip(RoundedCornerShape(12.dp)),
                            )
                        }
                        repeat(4 - items.size.coerceAtMost(4)) { Spacer(Modifier.weight(1f)) }
                    }
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(wallGroupName(path), fontWeight = FontWeight.Bold, fontSize = fixedSp(16f))
                            Text(
                                if (isActive) "${items.size} fonds · rotation ${labelFor(Rotation.minutes(ctx))}" else "${items.size} fonds",
                                fontSize = fixedSp(12f),
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton({ applyRandom = items }) { Icon(Icons.Filled.Shuffle, "Fond au hasard") }
                        if (isActive) {
                            IconButton({ Rotation.next(ctx); vm.toast("Fond suivant") }) { Icon(Icons.Filled.Autorenew, "Suivant") }
                            IconButton({ Rotation.stop(ctx); active = null; vm.toast("Rotation arrêtée") }) { Icon(Icons.Filled.Stop, "Arrêter", tint = Color(0xFFFF5370)) }
                        } else {
                            TextButton({ rotating = path }) { Text("Rotation") }
                        }
                    }
                }
            }
        }
    }

    rotating?.let { path ->
        RotationDialog(wallGroupName(path), onDismiss = { rotating = null }) { minutes, flags ->
            Rotation.start(ctx, path, minutes, flags)
            active = path
            rotating = null
            vm.toast("Rotation lancée")
        }
    }
    applyRandom?.let { list ->
        WallpaperDialog(onDismiss = { applyRandom = null }, showMove = false) { flag ->
            applyRandom = null
            vm.applyWallpaper(list.random(), flag)
        }
    }
}

private val Intervals = listOf(15 to "15 min", 60 to "1 h", 360 to "6 h", 720 to "12 h", 1440 to "24 h")
private fun labelFor(min: Int) = Intervals.firstOrNull { it.first == min }?.second?.let { "toutes les $it" } ?: "active"

@Composable
private fun RotationDialog(name: String, onDismiss: () -> Unit, onStart: (Int, Int) -> Unit) {
    var minutes by remember { mutableIntStateOf(1440) }
    var flags by remember { mutableIntStateOf(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rotation : $name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Changer de fond", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Intervals.forEach { (m, l) -> FilterChip(minutes == m, { minutes = m }, { Text(l, fontSize = fixedSp(12f)) }) }
                }
                Text("Écran", fontWeight = FontWeight.SemiBold)
                listOf(
                    "Accueil" to WallpaperManager.FLAG_SYSTEM,
                    "Verrouillage" to WallpaperManager.FLAG_LOCK,
                    "Les deux" to (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
                ).let { opts ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        opts.forEach { (l, f) -> FilterChip(flags == f, { flags = f }, { Text(l, fontSize = fixedSp(12f)) }) }
                    }
                }
                Text("Fonctionne même app fermée. Android peut décaler légèrement l'heure exacte.", fontSize = fixedSp(11f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton({ onStart(minutes, flags) }) { Text("Démarrer") } },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } },
    )
}

/** Choisir (ou créer) le groupe de fonds où ranger des images. */
@Composable
fun WallGroupPicker(vm: GalleryViewModel, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val all by vm.all.collectAsState()
    val groups = remember(all) {
        all.asSequence().filter { it.relativePath.startsWith(WALLPAPER_PATH, true) && it.relativePath.length > WALLPAPER_PATH.length }
            .map { it.relativePath }.distinct().sorted().toList()
    }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Groupe de fonds d'écran") },
        text = {
            Column {
                OutlinedTextField(
                    name, { name = it.replace("/", "") }, singleLine = true, label = { Text("Nouveau groupe") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton({ onPick(wallGroupPath(name)) }, enabled = name.isNotBlank()) { Icon(Icons.Filled.Check, "Créer") } },
                )
                LazyColumn(Modifier.heightIn(max = 300.dp).padding(top = 8.dp)) {
                    items(groups) { p ->
                        ListItem(
                            leadingContent = { Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) },
                            headlineContent = { Text(wallGroupName(p)) },
                            modifier = Modifier.clickable { onPick(p) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } },
    )
}
