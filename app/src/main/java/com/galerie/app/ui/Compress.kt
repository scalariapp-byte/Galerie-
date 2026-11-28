package com.galerie.app.ui

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.galerie.app.data.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

// ---------------- Fond d'écran ----------------

@Composable
fun WallpaperDialog(onDismiss: () -> Unit, showMove: Boolean = true, onGroup: (() -> Unit)? = null, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Définir comme fond d'écran") },
        text = {
            Column {
                listOf(
                    "Écran d'accueil" to WallpaperManager.FLAG_SYSTEM,
                    "Écran de verrouillage" to WallpaperManager.FLAG_LOCK,
                    "Les deux" to (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
                ).forEach { (label, flag) ->
                    ListItem(headlineContent = { Text(label) }, modifier = Modifier.clickable { onPick(flag) })
                }
                if (onGroup != null) ListItem(
                    headlineContent = { Text("Ajouter à un groupe…", color = MaterialTheme.colorScheme.primary) },
                    supportingContent = { Text("Préparer plusieurs fonds, rotation automatique") },
                    modifier = Modifier.clickable { onGroup() },
                )
                if (showMove) Text(
                    "L'image sera rangée dans « Fonds d'écran » et n'apparaîtra plus dans Galerie.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } },
    )
}

// ---------------- Compresseur vidéo ----------------

private enum class Quality(val label: String, val shortSide: Int, val bitrate: Int) {
    HIGH("Haute (1080p)", 1080, 5_000_000),
    MEDIUM("Moyenne (720p)", 720, 2_500_000),
    LOW("Légère (480p)", 480, 1_000_000),
}

private sealed interface CState {
    data object Choose : CState
    data class Running(val q: Quality) : CState
    data class Done(val newSize: Long) : CState
    data class Failed(val msg: String) : CState
}

@Composable
fun CompressDialog(vm: GalleryViewModel, m: Media, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf<CState>(CState.Choose) }
    var progress by remember { mutableFloatStateOf(0f) }

    (state as? CState.Running)?.let { run ->
        val job = remember(run) { Compressor(ctx, m, run.q) }
        DisposableEffect(job) { onDispose { job.cancel() } }
        LaunchedEffect(job) {
            job.start()
            while (!job.finished) { progress = job.progress(); delay(250) }
            val out = job.output
            state = if (out != null) {
                val size = withContext(Dispatchers.IO) { saveToGallery(ctx, out, m) }
                if (size > 0) CState.Done(size) else CState.Failed("Enregistrement impossible")
            } else CState.Failed(job.error ?: "Échec de la compression")
        }
    }

    AlertDialog(
        onDismissRequest = { if (state !is CState.Running) onDismiss() },
        title = { Text("Compresser la vidéo") },
        text = {
            when (val s = state) {
                CState.Choose -> Column {
                    Text("Taille actuelle : ${Format.size(m.size)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Quality.entries.filter { it.shortSide <= maxOf(minOf(m.width, m.height), 480) }.forEach { q ->
                        val est = (q.bitrate + 128_000L) / 8 * (m.duration / 1000).coerceAtLeast(1)
                        ListItem(
                            headlineContent = { Text(q.label) },
                            supportingContent = { Text("≈ ${Format.size(est)}") },
                            modifier = Modifier.clickable { state = CState.Running(q) },
                        )
                    }
                }
                is CState.Running -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${s.q.label} · ${(progress * 100).toInt()} %")
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("Garde l'application ouverte.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is CState.Done -> Text(
                    "${Format.size(m.size)} → ${Format.size(s.newSize)} (−${(100 - s.newSize * 100 / m.size.coerceAtLeast(1)).coerceAtLeast(0)} %).\n" +
                        "La copie est dans l'album « Compressées ».",
                )
                is CState.Failed -> Text(s.msg)
            }
        },
        confirmButton = {
            when (state) {
                is CState.Done -> TextButton({ vm.trashItems(listOf(m)); onDismiss() }) { Text("Supprimer l'original") }
                else -> {}
            }
        },
        dismissButton = {
            when (state) {
                is CState.Running -> TextButton({ state = CState.Failed("Compression annulée") }) { Text("Annuler") }
                is CState.Done -> TextButton(onDismiss) { Text("Garder les deux") }
                else -> TextButton(onDismiss) { Text("Fermer") }
            }
        },
    )
}

@OptIn(UnstableApi::class)
private class Compressor(private val ctx: Context, private val m: Media, private val q: Quality) {
    private val file = File(ctx.cacheDir, "compress_${m.id}.mp4")
    private val holder = ProgressHolder()
    private var transformer: Transformer? = null
    var finished = false; private set
    var output: File? = null; private set
    var error: String? = null; private set

    fun start() {
        file.delete()
        val t = Transformer.Builder(ctx)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(
                DefaultEncoderFactory.Builder(ctx)
                    .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(q.bitrate).build())
                    .build(),
            )
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    output = file; finished = true
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    error = exportException.message; finished = true
                }
            })
            .build()
        // Réduit le petit côté à la cible, sans jamais agrandir.
        val w = m.width.coerceAtLeast(1)
        val h = m.height.coerceAtLeast(1)
        val short = minOf(w, h)
        val scale = minOf(1f, q.shortSide.toFloat() / short)
        val tw = ((w * scale).toInt() / 2) * 2
        val th = ((h * scale).toInt() / 2) * 2
        val effects = Effects(emptyList(), listOf(Presentation.createForWidthAndHeight(tw, th, Presentation.LAYOUT_SCALE_TO_FIT)))
        val item = EditedMediaItem.Builder(MediaItem.fromUri(m.uri)).setEffects(effects).build()
        transformer = t
        t.start(item, file.absolutePath)
    }

    fun progress(): Float {
        val t = transformer ?: return 0f
        return if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) holder.progress / 100f else 0f
    }

    fun cancel() {
        if (!finished) runCatching { transformer?.cancel() }
        if (output == null) file.delete()
    }
}

/** Copie le fichier compressé dans MediaStore (Movies/Compressées/) et renvoie sa taille. */
private fun saveToGallery(ctx: Context, file: File, src: Media): Long = runCatching {
    val resolver = ctx.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, src.name.substringBeforeLast('.') + "_compressee.mp4")
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Compressées/")
        put(MediaStore.MediaColumns.DATE_TAKEN, src.date)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)!!
    resolver.openOutputStream(uri)!!.use { o -> file.inputStream().use { it.copyTo(o, 1 shl 16) } }
    resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
    val size = file.length()
    file.delete()
    size
}.getOrDefault(0L)
