package com.galerie.app.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.galerie.app.data.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ---------------- Modèle d'édition ----------------

enum class PhotoFilter(val label: String) {
    NONE("Original"), VIVID("Vif"), CINEMA("Cinéma"), NEON("Néon"), DRAMA("Drama"), GOLD("Golden"),
    BW("N&B"), NOIR("Noir"), SEPIA("Sépia"), WARM("Chaud"), COOL("Froid"), VINTAGE("Vintage"), FADE("Doux"), INVERT("Négatif"),
}

data class Adjust(
    val brightness: Float = 0f, // -1..1
    val contrast: Float = 0f,   // -1..1
    val saturation: Float = 0f, // -1..1
    val warmth: Float = 0f,     // -1..1
    val vignette: Float = 0f,   // 0..1
    val filter: PhotoFilter = PhotoFilter.NONE,
)

private fun concat(a: android.graphics.ColorMatrix, b: android.graphics.ColorMatrix) = a.apply { postConcat(b) }

/** Matrice de couleurs 4×5 (même format pour Android et Compose). */
fun colorMatrix(a: Adjust): FloatArray {
    val m = android.graphics.ColorMatrix()
    when (a.filter) {
        PhotoFilter.NONE -> {}
        PhotoFilter.VIVID -> { m.setSaturation(1.45f); concat(m, contrastM(1.12f)) }
        PhotoFilter.BW -> { m.setSaturation(0f); concat(m, contrastM(1.1f)) }
        PhotoFilter.SEPIA -> { m.setSaturation(0f); concat(m, android.graphics.ColorMatrix().apply { setScale(1.07f, 0.94f, 0.76f, 1f) }) }
        PhotoFilter.WARM -> m.setScale(1.10f, 1.02f, 0.88f, 1f)
        PhotoFilter.COOL -> m.setScale(0.90f, 1.0f, 1.12f, 1f)
        PhotoFilter.VINTAGE -> { m.setSaturation(0.6f); concat(m, android.graphics.ColorMatrix().apply { setScale(1.08f, 1.0f, 0.85f, 1f) }); concat(m, contrastM(0.9f)) ; concat(m, translateM(18f)) }
        PhotoFilter.CINEMA -> { // ombres sarcelle, hautes lumières orangées
            m.set(floatArrayOf(1.12f, 0.05f, -0.08f, 0f, -6f, -0.04f, 1.02f, 0.06f, 0f, 2f, -0.06f, 0.10f, 0.95f, 0f, 14f, 0f, 0f, 0f, 1f, 0f))
            concat(m, contrastM(1.15f))
        }
        PhotoFilter.NEON -> { m.setSaturation(1.8f); concat(m, contrastM(1.25f)); concat(m, android.graphics.ColorMatrix().apply { setScale(1.05f, 0.9f, 1.2f, 1f) }) }
        PhotoFilter.DRAMA -> { m.setSaturation(0.7f); concat(m, contrastM(1.45f)); concat(m, translateM(-10f)) }
        PhotoFilter.GOLD -> { m.setSaturation(1.15f); concat(m, android.graphics.ColorMatrix().apply { setScale(1.15f, 1.04f, 0.78f, 1f) }); concat(m, translateM(8f)) }
        PhotoFilter.NOIR -> { m.setSaturation(0f); concat(m, contrastM(1.55f)); concat(m, translateM(-14f)) }
        PhotoFilter.INVERT -> m.set(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
        PhotoFilter.FADE -> { m.setSaturation(0.8f); concat(m, contrastM(0.82f)); concat(m, translateM(22f)) }
    }
    concat(m, android.graphics.ColorMatrix().apply { setSaturation(1f + a.saturation) })
    concat(m, contrastM(1f + a.contrast * 0.6f))
    concat(m, translateM(a.brightness * 80f))
    concat(m, android.graphics.ColorMatrix().apply { setScale(1f + 0.15f * a.warmth, 1f, 1f - 0.15f * a.warmth, 1f) })
    return m.array
}

private fun contrastM(c: Float) = android.graphics.ColorMatrix(
    floatArrayOf(c, 0f, 0f, 0f, 128f * (1 - c), 0f, c, 0f, 0f, 128f * (1 - c), 0f, 0f, c, 0f, 128f * (1 - c), 0f, 0f, 0f, 1f, 0f),
)

private fun translateM(t: Float) = android.graphics.ColorMatrix(
    floatArrayOf(1f, 0f, 0f, 0f, t, 0f, 1f, 0f, 0f, t, 0f, 0f, 1f, 0f, t, 0f, 0f, 0f, 1f, 0f),
)

private fun decode(ctx: Context, m: Media, maxSide: Int): Bitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, m.uri)) { d, info, _ ->
        d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val s = max(info.size.width, info.size.height)
        if (s > maxSide) {
            val f = maxSide.toFloat() / s
            d.setTargetSize((info.size.width * f).toInt().coerceAtLeast(1), (info.size.height * f).toInt().coerceAtLeast(1))
        }
    }

private fun rotateFlip(src: Bitmap, rot: Int, flip: Boolean): Bitmap {
    if (rot == 0 && !flip) return src
    val mx = Matrix().apply { if (flip) postScale(-1f, 1f); postRotate(rot.toFloat()) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, mx, true)
}

private fun crop(src: Bitmap, r: Rect): Bitmap {
    if (r.left <= 0f && r.top <= 0f && r.right >= 1f && r.bottom >= 1f) return src
    val x = (r.left * src.width).toInt().coerceIn(0, src.width - 1)
    val y = (r.top * src.height).toInt().coerceIn(0, src.height - 1)
    val w = (r.width * src.width).toInt().coerceIn(1, src.width - x)
    val h = (r.height * src.height).toInt().coerceIn(1, src.height - y)
    return Bitmap.createBitmap(src, x, y, w, h)
}

private fun render(ctx: Context, m: Media, rot: Int, flip: Boolean, rect: Rect, adj: Adjust): Bitmap {
    val base = crop(rotateFlip(decode(ctx, m, 4096), rot, flip), rect)
    val out = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawBitmap(base, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(colorMatrix(adj)) })
    if (adj.vignette > 0f) {
        val r = max(out.width, out.height) * 0.75f
        c.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), Paint().apply {
            shader = android.graphics.RadialGradient(
                out.width / 2f, out.height / 2f, r,
                intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.argb((adj.vignette * 230).toInt(), 0, 0, 0)),
                floatArrayOf(0.45f, 1f), android.graphics.Shader.TileMode.CLAMP,
            )
        })
    }
    return out
}

private fun Bitmap.compressTo(m: Media, out: java.io.OutputStream) {
    val png = m.mime == "image/png"
    compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, out)
}

/** Enregistre une nouvelle image à côté de l'original. */
private fun saveCopy(ctx: Context, m: Media, bmp: Bitmap): Boolean = runCatching {
    val png = m.mime == "image/png"
    val path = if (m.relativePath.startsWith("DCIM/") || m.relativePath.startsWith("Pictures/")) m.relativePath else "Pictures/Modifiées/"
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, m.name.substringBeforeLast('.') + "_modif." + if (png) "png" else "jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, if (png) "image/png" else "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, path)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val r = ctx.contentResolver
    val uri = r.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)!!
    r.openOutputStream(uri)!!.use { bmp.compressTo(m, it) }
    r.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
    true
}.getOrDefault(false)

/** Écrase l'original (après autorisation d'écriture accordée par Android). */
fun overwriteOriginal(ctx: Context, m: Media, bmp: Bitmap): Boolean = runCatching {
    val r = ctx.contentResolver
    r.openOutputStream(m.uri, "wt")!!.use { bmp.compressTo(m, it) }
    r.update(m.uri, ContentValues().apply {
        put(MediaStore.MediaColumns.ORIENTATION, 0)
        put(MediaStore.MediaColumns.WIDTH, bmp.width)
        put(MediaStore.MediaColumns.HEIGHT, bmp.height)
    }, null, null)
    true
}.getOrDefault(false)

// ---------------- Écran ----------------

private enum class Tool(val label: String) { CROP("Recadrer"), ADJUST("Ajuster"), FILTER("Filtres") }
private enum class Param(val label: String) { BRIGHT("Luminosité"), CONTRAST("Contraste"), SAT("Saturation"), WARM("Chaleur"), VIGNETTE("Vignette") }
private val Ratios = listOf("Libre" to 0f, "1:1" to 1f, "4:3" to 4f / 3f, "3:4" to 3f / 4f, "16:9" to 16f / 9f)
private val FullRect = Rect(0f, 0f, 1f, 1f)

@Composable
fun EditorScreen(vm: GalleryViewModel, mediaId: Long) {
    val ctx = LocalContext.current
    val m = remember(mediaId) { vm.all.value.firstOrNull { it.id == mediaId } }
    if (m == null) { LaunchedEffect(Unit) { vm.pop() }; return }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(m.id) { preview = withContext(Dispatchers.IO) { runCatching { decode(ctx, m, 1600) }.getOrNull() } }

    var rot by remember { mutableIntStateOf(0) }
    var flip by remember { mutableStateOf(false) }
    var rect by remember { mutableStateOf(FullRect) }
    var ratio by remember { mutableFloatStateOf(0f) }
    var adj by remember { mutableStateOf(Adjust()) }
    var tool by remember { mutableStateOf(Tool.ADJUST) }
    var param by remember { mutableStateOf(Param.BRIGHT) }
    var saving by remember { mutableStateOf(false) }
    var askSave by remember { mutableStateOf(false) }
    var askQuit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val changed = rot != 0 || flip || rect != FullRect || adj != Adjust()

    BackHandler { if (changed) askQuit = true else vm.pop() }

    val rotated = remember(preview, rot, flip) { preview?.let { rotateFlip(it, rot, flip) } }
    val cropped = remember(rotated, rect) { rotated?.let { crop(it, rect) } }
    val filter = remember(adj) { ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(colorMatrix(adj))) }

    fun setRatio(r: Float) {
        ratio = r
        val src = rotated ?: return
        if (r == 0f) { rect = FullRect; return }
        val imgRatio = src.width.toFloat() / src.height
        rect = if (r > imgRatio) { val h = imgRatio / r; Rect(0f, (1 - h) / 2, 1f, (1 + h) / 2) }
        else { val w = r / imgRatio; Rect((1 - w) / 2, 0f, (1 + w) / 2, 1f) }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B0910)).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ if (changed) askQuit = true else vm.pop() }) { Icon(Icons.Filled.Close, "Fermer", tint = Color.White) }
            Text("Modifier", color = Color.White, fontWeight = FontWeight.Bold, fontSize = fixedSp(17f), modifier = Modifier.weight(1f))
            IconButton({ rot = 0; flip = false; rect = FullRect; ratio = 0f; adj = Adjust() }, enabled = changed) {
                Icon(Icons.Filled.RestartAlt, "Réinitialiser", tint = if (changed) Color.White else Color.White.copy(alpha = 0.3f))
            }
            IconButton({ askSave = true }, enabled = changed && !saving) {
                Icon(Icons.Filled.Check, "Enregistrer", tint = if (changed) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f))
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            when {
                preview == null || saving -> CircularProgressIndicator()
                tool == Tool.CROP && rotated != null -> CropView(rotated, rect, ratio, filter) { rect = it }
                cropped != null -> {
                    val img = remember(cropped) { cropped.asImageBitmap() }
                    val vig = adj.vignette
                    Image(
                        img, null, contentScale = ContentScale.Fit, colorFilter = filter,
                        modifier = Modifier.aspectRatio(cropped.width.toFloat() / cropped.height).drawWithContent {
                            drawContent()
                            if (vig > 0f) drawRect(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = vig * 0.9f),
                                    center = center, radius = max(size.width, size.height) * 0.75f,
                                ),
                            )
                        },
                    )
                }
            }
        }

        // Outils de l'onglet courant
        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
            when (tool) {
                Tool.CROP -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        IconButton({ rot = (rot + 270) % 360; rect = FullRect; ratio = 0f }) { Icon(Icons.AutoMirrored.Filled.RotateLeft, "Pivoter à gauche", tint = Color.White) }
                        IconButton({ rot = (rot + 90) % 360; rect = FullRect; ratio = 0f }) { Icon(Icons.AutoMirrored.Filled.RotateRight, "Pivoter à droite", tint = Color.White) }
                        IconButton({ flip = !flip; rect = FullRect; ratio = 0f }) { Icon(Icons.Filled.Flip, "Miroir", tint = Color.White) }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(Ratios) { (label, r) -> FilterChip(ratio == r, { setRatio(r) }, { Text(label, fontSize = fixedSp(12f)) }) }
                    }
                }
                Tool.ADJUST -> Column(Modifier.padding(horizontal = 20.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(Param.entries.toList()) { p -> FilterChip(param == p, { param = p }, { Text(p.label, fontSize = fixedSp(12f)) }) }
                    }
                    val v = when (param) { Param.BRIGHT -> adj.brightness; Param.CONTRAST -> adj.contrast; Param.SAT -> adj.saturation; Param.WARM -> adj.warmth; Param.VIGNETTE -> adj.vignette * 2f - 1f }
                    Slider(v, { n ->
                        adj = when (param) {
                            Param.BRIGHT -> adj.copy(brightness = n); Param.CONTRAST -> adj.copy(contrast = n)
                            Param.SAT -> adj.copy(saturation = n); Param.WARM -> adj.copy(warmth = n)
                            Param.VIGNETTE -> adj.copy(vignette = (n + 1f) / 2f)
                        }
                    }, valueRange = -1f..1f)
                }
                Tool.FILTER -> {
                    val thumb = remember(cropped) { cropped?.let { Bitmap.createScaledBitmap(it, 120, (120f * it.height / it.width).toInt().coerceAtLeast(1), true).asImageBitmap() } }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(PhotoFilter.entries.toList()) { f ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { adj = adj.copy(filter = f) }) {
                                Box(
                                    Modifier.size(62.dp).clip(RoundedCornerShape(12.dp))
                                        .border(2.dp, if (adj.filter == f) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp)),
                                ) {
                                    if (thumb != null) Image(
                                        thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                                        colorFilter = ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(colorMatrix(Adjust(filter = f)))),
                                    )
                                }
                                Text(f.label, color = Color.White, fontSize = fixedSp(11f), modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Tool.entries.forEach { t ->
                Text(
                    t.label, fontSize = fixedSp(14f), fontWeight = FontWeight.SemiBold,
                    color = if (tool == t) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.clip(CircleShape)
                        .background(if (tool == t) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { tool = t }.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    fun save(replace: Boolean) {
        askSave = false
        saving = true
        scope.launch {
            val bmp = withContext(Dispatchers.Default) { runCatching { render(ctx, m, rot, flip, rect, adj) }.getOrNull() }
            if (bmp == null) { saving = false; vm.toast("Échec de l'enregistrement"); return@launch }
            if (replace) {
                vm.overwrite(m, bmp)
                vm.pop()
            } else {
                val ok = withContext(Dispatchers.IO) { saveCopy(ctx, m, bmp) }
                vm.toast(if (ok) "Copie enregistrée" else "Échec de l'enregistrement")
                vm.pop()
            }
        }
    }

    if (askSave) AlertDialog(
        onDismissRequest = { askSave = false },
        title = { Text("Enregistrer") },
        text = { Text("Garder l'original et enregistrer une copie, ou remplacer l'original ?") },
        confirmButton = { TextButton({ save(false) }) { Text("Copie") } },
        dismissButton = { TextButton({ save(true) }) { Text("Remplacer") } },
    )
    if (askQuit) AlertDialog(
        onDismissRequest = { askQuit = false },
        title = { Text("Abandonner les modifications ?") },
        confirmButton = { TextButton({ askQuit = false; vm.pop() }) { Text("Abandonner") } },
        dismissButton = { TextButton({ askQuit = false }) { Text("Continuer") } },
    )
}

/** Image entière avec un cadre de recadrage : coins pour redimensionner, intérieur pour déplacer. */
@Composable
private fun CropView(bmp: Bitmap, rect: Rect, ratio: Float, filter: ColorFilter, onChange: (Rect) -> Unit) {
    val image = remember(bmp) { bmp.asImageBitmap() }
    val touch = with(LocalDensity.current) { 36.dp.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()
        val scale = min(cw / bmp.width, ch / bmp.height)
        val iw = bmp.width * scale
        val ih = bmp.height * scale
        val ox = (cw - iw) / 2
        val oy = (ch - ih) / 2
        var cur by remember(bmp) { mutableStateOf(rect) }
        LaunchedEffect(rect) { cur = rect }
        Image(image, null, colorFilter = filter, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        ComposeCanvas(
            Modifier.fillMaxSize().pointerInput(bmp, ratio) {
                var mode = -1 // 0..3 coins (TL, TR, BL, BR), 4 = déplacer
                detectDragGestures(
                    onDragStart = { p ->
                        val r = cur
                        val corners = listOf(Offset(r.left, r.top), Offset(r.right, r.top), Offset(r.left, r.bottom), Offset(r.right, r.bottom))
                            .map { Offset(ox + it.x * iw, oy + it.y * ih) }
                        val i = corners.indexOfFirst { (it - p).getDistance() < touch * 1.4f }
                        mode = if (i >= 0) i else 4
                    },
                    onDragEnd = { onChange(cur) },
                ) { ch2, d ->
                    ch2.consume()
                    val dx = d.x / iw
                    val dy = d.y / ih
                    val r = cur
                    val minS = 0.08f
                    cur = when (mode) {
                        4 -> {
                            val nx = (r.left + dx).coerceIn(0f, 1f - r.width)
                            val ny = (r.top + dy).coerceIn(0f, 1f - r.height)
                            Rect(nx, ny, nx + r.width, ny + r.height)
                        }
                        else -> {
                            var l = r.left; var t = r.top; var rr = r.right; var b = r.bottom
                            when (mode) { 0 -> { l += dx; t += dy }; 1 -> { rr += dx; t += dy }; 2 -> { l += dx; b += dy }; 3 -> { rr += dx; b += dy } }
                            l = l.coerceIn(0f, rr - minS); rr = rr.coerceIn(l + minS, 1f)
                            t = t.coerceIn(0f, b - minS); b = b.coerceIn(t + minS, 1f)
                            if (ratio > 0f) {
                                // garde le ratio (en pixels) en ajustant la hauteur depuis le coin tiré
                                val wPx = (rr - l) * iw
                                val hN = (wPx / ratio) / ih
                                if (mode == 0 || mode == 1) t = (b - hN) else b = (t + hN)
                                if (t < 0f || b > 1f) return@detectDragGestures
                            }
                            Rect(l, t, rr, b)
                        }
                    }
                }
            },
        ) {
            val r = Rect(ox + cur.left * iw, oy + cur.top * ih, ox + cur.right * iw, oy + cur.bottom * ih)
            val dim = Color.Black.copy(alpha = 0.55f)
            drawRect(dim, Offset(ox, oy), Size(iw, r.top - oy))
            drawRect(dim, Offset(ox, r.bottom), Size(iw, oy + ih - r.bottom))
            drawRect(dim, Offset(ox, r.top), Size(r.left - ox, r.height))
            drawRect(dim, Offset(r.right, r.top), Size(ox + iw - r.right, r.height))
            drawRect(Color.White, r.topLeft, r.size, style = Stroke(2.dp.toPx()))
            for (i in 1..2) {
                drawLine(Color.White.copy(alpha = 0.4f), Offset(r.left + r.width * i / 3, r.top), Offset(r.left + r.width * i / 3, r.bottom))
                drawLine(Color.White.copy(alpha = 0.4f), Offset(r.left, r.top + r.height * i / 3), Offset(r.right, r.top + r.height * i / 3))
            }
            listOf(r.topLeft, Offset(r.right, r.top), Offset(r.left, r.bottom), r.bottomRight).forEach { drawCircle(Color.White, 7.dp.toPx(), it) }
        }
    }
}
