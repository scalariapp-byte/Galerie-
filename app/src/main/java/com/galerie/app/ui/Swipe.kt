package com.galerie.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.galerie.app.data.Media
import com.galerie.app.data.Thumb
import com.galerie.app.data.thumbKey
import kotlinx.coroutines.launch
import kotlin.math.abs

private val Keep = Color(0xFF3DDC97)
private val Delete = Color(0xFFFF5370)

/**
 * Tri rapide : glisser à gauche = corbeille, à droite = garder.
 * Les suppressions sont envoyées à la corbeille en une seule fois à la fin.
 */
@Composable
fun SwipeScreen(vm: GalleryViewModel, videos: Boolean) {
    val all by vm.all.collectAsState()
    var kept by remember { mutableStateOf(vm.keptIds()) }
    val decided = remember { mutableStateListOf<Pair<Long, Boolean>>() } // (id, garder)
    val decidedIds = decided.map { it.first }.toSet()
    val queue = remember(all, kept) { all.filter { it.isVideo == videos && it.id !in kept }.sortedByDescending { it.date } }
    val remaining = queue.filter { it.id !in decidedIds }
    val current = remaining.firstOrNull()
    val next = remaining.getOrNull(1)
    val toDelete = decided.filter { !it.second }.map { it.first }.toSet()
    var confirmExit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val offset = remember(current?.id) { Animatable(0f) }
    var widthPx by remember { mutableFloatStateOf(1f) }

    fun finish() {
        val items = all.filter { it.id in toDelete }
        if (items.isNotEmpty()) vm.trashItems(items)
        vm.pop()
    }
    fun commit(m: Media, keep: Boolean) {
        decided.add(m.id to keep)
        if (keep) vm.setKept(m.id, true)
    }
    fun decide(keep: Boolean) {
        val m = current ?: return
        scope.launch {
            offset.animateTo(if (keep) widthPx * 1.5f else -widthPx * 1.5f, tween(200))
            commit(m, keep)
        }
    }
    fun undo() {
        val last = decided.removeLastOrNull() ?: return
        if (last.second) vm.setKept(last.first, false)
    }

    BackHandler { if (toDelete.isNotEmpty()) confirmExit = true else vm.pop() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ if (toDelete.isNotEmpty()) confirmExit = true else vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            Column(Modifier.weight(1f)) {
                Text(if (videos) "Trier les vidéos" else "Trier les photos", fontSize = fixedSp(18f), fontWeight = FontWeight.Bold)
                Text("${remaining.size} restant(e)s · ${toDelete.size} à supprimer", fontSize = fixedSp(12f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { undo() }, enabled = decided.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Undo, "Annuler") }
            IconButton({ vm.resetKept(videos); kept = vm.keptIds(); decided.clear() }) { Icon(Icons.Filled.RestartAlt, "Tout revoir") }
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).onSizeChanged { widthPx = it.width.toFloat() }) {
            if (current == null) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tout est trié 🎉", fontSize = fixedSp(22f), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (toDelete.isEmpty()) "Rien à supprimer." else "${toDelete.size} élément(s) iront dans la corbeille.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                if (next != null) Card(next, Modifier.graphicsLayer { scaleX = 0.94f; scaleY = 0.94f; alpha = 0.7f }, 0f)
                val progress = (offset.value / (widthPx * 0.3f)).coerceIn(-1f, 1f)
                Card(
                    current,
                    Modifier
                        .graphicsLayer { translationX = offset.value; rotationZ = offset.value / widthPx * 10f }
                        .pointerInput(current.id) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (abs(offset.value) > widthPx * 0.3f) decide(offset.value > 0)
                                        else offset.animateTo(0f, spring())
                                    }
                                },
                                onDragCancel = { scope.launch { offset.animateTo(0f, spring()) } },
                            ) { ch, dx -> ch.consume(); scope.launch { offset.snapTo(offset.value + dx) } }
                        },
                    progress,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundAction(Icons.Filled.Close, Delete, "Supprimer", enabled = current != null) { decide(false) }
            Button(
                onClick = { finish() },
                colors = ButtonDefaults.buttonColors(containerColor = if (toDelete.isEmpty()) MaterialTheme.colorScheme.surfaceContainerHigh else Delete),
            ) { Text(if (toDelete.isEmpty()) "Terminer" else "Terminer · ${toDelete.size}", color = Color.White) }
            RoundAction(Icons.Filled.Check, Keep, "Garder", enabled = current != null) { decide(true) }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Mettre ${toDelete.size} élément(s) à la corbeille ?") },
            text = { Text("Ils restent récupérables 30 jours dans la corbeille.") },
            confirmButton = { TextButton({ confirmExit = false; finish() }) { Text("Oui") } },
            dismissButton = { TextButton({ confirmExit = false; vm.pop() }) { Text("Non, tout garder") } },
        )
    }
}

@Composable
private fun Card(m: Media, modifier: Modifier, progress: Float) {
    val ctx = LocalContext.current
    val model = remember(m.id) {
        if (m.isVideo) Thumb(m.uri, m.dateModified, 720)
        else ImageRequest.Builder(ctx).data(m.uri).size(1280).placeholderMemoryCacheKey(thumbKey(m.uri, m.dateModified)).build()
    }
    Box(
        modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(ViewerBg)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp)),
    ) {
        AsyncImage(model, m.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        if (m.isVideo) Icon(Icons.Filled.PlayCircle, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.align(Alignment.Center).size(64.dp))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                .padding(16.dp),
        ) {
            Text(Format.fullDate(m.date), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = fixedSp(14f))
            Text(
                "${Format.size(m.size)}${if (m.isVideo) " · " + Format.duration(m.duration) else ""} · ${m.bucketName}",
                color = Color.White.copy(alpha = 0.75f), fontSize = fixedSp(12f),
            )
        }
        if (progress > 0.05f) Stamp("GARDER", Keep, progress, Modifier.align(Alignment.TopStart).rotate(-14f))
        if (progress < -0.05f) Stamp("SUPPRIMER", Delete, -progress, Modifier.align(Alignment.TopEnd).rotate(14f))
    }
}

@Composable
private fun Stamp(text: String, color: Color, alpha: Float, modifier: Modifier) {
    Text(
        text, color = color.copy(alpha = alpha), fontWeight = FontWeight.Black, fontSize = fixedSp(26f),
        modifier = modifier.padding(24.dp).border(3.dp, color.copy(alpha = alpha), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(64.dp).clip(CircleShape).background(color.copy(alpha = if (enabled) 0.18f else 0.06f))
            .border(2.dp, color.copy(alpha = if (enabled) 1f else 0.3f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = color, modifier = Modifier.size(30.dp)) }
}
