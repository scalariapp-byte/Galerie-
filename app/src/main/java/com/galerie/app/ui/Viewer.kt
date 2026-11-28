package com.galerie.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.galerie.app.R
import com.galerie.app.data.Media
import com.galerie.app.data.MediaRepository
import com.galerie.app.data.Thumb
import com.galerie.app.data.thumbKey
import kotlinx.coroutines.delay
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

@Composable
fun ViewerScreen(vm: GalleryViewModel, source: Source, startId: Long) {
    val items by rememberItems(vm, source)
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { vm.pop() }
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsState()
    val startIndex = remember { items.indexOfFirst { it.id == startId }.coerceAtLeast(0) }
    val pager = rememberPagerState(initialPage = startIndex) { items.size }
    val current = items[pager.currentPage.coerceIn(0, items.lastIndex)]
    LaunchedEffect(current.id) { vm.lastViewedId = current.id }

    var chrome by remember { mutableStateOf(true) }
    var slideshow by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Boolean?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var wallpaper by remember { mutableStateOf(false) }
    var wallGroup by remember { mutableStateOf(false) }
    var compress by remember { mutableStateOf(false) }
    val trashMode = source == Source.Trash
    val player = rememberPlayer(settings.loopVideos && !slideshow)

    ImmersiveMode(chrome && !slideshow)
    val view = LocalView.current
    DisposableEffect(slideshow) {
        view.keepScreenOn = slideshow
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(slideshow, pager.currentPage) {
        if (!slideshow) return@LaunchedEffect
        val m = items.getOrNull(pager.currentPage)
        // Pour une vidéo : attendre la fin de lecture
        if (m != null && m.isVideo) {
            delay(500)
            while (player.playbackState != Player.STATE_ENDED && player.isPlaying) delay(250)
        } else {
            delay(settings.slideshowSeconds * 1000L)
        }
        if (pager.currentPage < items.lastIndex) pager.animateScrollToPage(pager.currentPage + 1) else slideshow = false
    }

    Box(Modifier.fillMaxSize().background(ViewerBg)) {
        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 1,
            pageSpacing = 12.dp,
            key = { items.getOrNull(it)?.id ?: it },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val m = items.getOrNull(page) ?: return@HorizontalPager
            val onTap = { if (slideshow) slideshow = false else chrome = !chrome }
            if (m.isVideo) {
                VideoPage(m, active = page == pager.settledPage, player = player, autoplay = settings.autoplay || slideshow, controls = chrome && !slideshow, onTap = onTap)
            } else {
                ImagePage(m.uri, m.dateModified, m.name, onTap)
            }
        }

        AnimatedVisibility(chrome && !slideshow, Modifier.align(Alignment.TopCenter), enter = fadeIn() + androidx.compose.animation.slideInVertically { -it }, exit = fadeOut() + androidx.compose.animation.slideOutVertically { -it }) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xB3000000), Color.Transparent)))
                    .statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(Format.shortDate(current.date), color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        "${Format.time(current.date)} · ${current.bucketName}", color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("${pager.currentPage + 1} / ${items.size}", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                IconButton({ info = true }) { Icon(Icons.Outlined.Info, "Détails", tint = Color.White) }
            }
        }

        AnimatedVisibility(chrome && !slideshow, Modifier.align(Alignment.BottomCenter), enter = fadeIn() + androidx.compose.animation.slideInVertically { it }, exit = fadeOut() + androidx.compose.animation.slideOutVertically { it }) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .navigationBarsPadding().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                if (trashMode) {
                    ViewerAction(Icons.Filled.RestoreFromTrash, "Restaurer") { vm.restore(listOf(current)) }
                    ViewerAction(Icons.Filled.DeleteForever, "Supprimer") { vm.deleteForever(listOf(current)) }
                } else {
                    ViewerAction(Icons.Filled.Share, "Partager") { Actions.share(ctx, listOf(current)) }
                    ViewerAction(if (current.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favori",
                        tint = if (current.isFavorite) Color(0xFFFF5370) else Color.White) {
                        vm.setFavorite(listOf(current), !current.isFavorite)
                    }
                    ViewerAction(Icons.Filled.Edit, "Modifier") {
                        if (current.isVideo || current.isGif) Actions.edit(ctx, current) else vm.push(Screen.Editor(current.id))
                    }
                    ViewerAction(Icons.Filled.Delete, "Supprimer") { vm.trashItems(listOf(current)) }
                    Box {
                        var menu by remember { mutableStateOf(false) }
                        ViewerAction(Icons.Filled.MoreVert, "Plus") { menu = true }
                        DropdownMenu(menu, { menu = false }) {
                            MenuItem(Icons.Filled.Slideshow, "Diaporama") { menu = false; slideshow = true }
                            if (!current.isVideo) MenuItem(Icons.Filled.Wallpaper, "Fond d'écran") { menu = false; wallpaper = true }
                            if (current.isVideo) MenuItem(Icons.Filled.Compress, "Compresser la vidéo") { menu = false; compress = true }
                            if (!current.isVideo) MenuItem(Icons.AutoMirrored.Filled.OpenInNew, "Définir comme…") { menu = false; Actions.setAs(ctx, current) }
                            MenuItem(Icons.Filled.Edit, "Modifier avec une autre app…") { menu = false; Actions.edit(ctx, current) }
                            MenuItem(Icons.AutoMirrored.Filled.OpenInNew, "Ouvrir avec…") { menu = false; Actions.openWith(ctx, current) }
                            MenuItem(Icons.Filled.ContentCopy, "Copier vers un album") { menu = false; picker = false }
                            MenuItem(Icons.AutoMirrored.Filled.DriveFileMove, "Déplacer vers un album") { menu = false; picker = true }
                            MenuItem(Icons.Filled.DriveFileRenameOutline, "Renommer") { menu = false; renaming = true }
                            MenuItem(Icons.Filled.Info, "Détails") { menu = false; info = true }
                        }
                    }
                }
            }
        }
    }

    if (info) InfoSheet(vm, current) { info = false }
    picker?.let { move ->
        AlbumPickerDialog(vm, move, onDismiss = { picker = null }) { path ->
            if (move) vm.move(listOf(current), path) else vm.copy(listOf(current), path)
            picker = null
        }
    }
    if (wallpaper) WallpaperDialog({ wallpaper = false }, onGroup = { wallpaper = false; wallGroup = true }) { flag -> wallpaper = false; vm.setWallpaper(current, flag) }
    if (wallGroup) WallGroupPicker(vm, { wallGroup = false }) { path -> wallGroup = false; vm.move(listOf(current), path) }
    if (compress) CompressDialog(vm, current) { compress = false }
    if (renaming) RenameDialog(current, { renaming = false }) { vm.rename(current, it); renaming = false }
}

@Composable
private fun MenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text) }, leadingIcon = { Icon(icon, null) }, onClick = onClick)
}

@Composable
private fun ViewerAction(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = tint)
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun ImagePage(uri: Uri, modified: Long, name: String, onTap: () -> Unit) {
    val ctx = LocalContext.current
    val request = remember(uri, modified) {
        ImageRequest.Builder(ctx).data(uri).placeholderMemoryCacheKey(thumbKey(uri, modified)).build()
    }
    val state = rememberZoomableImageState(rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 10f)))
    ZoomableAsyncImage(
        model = request,
        contentDescription = name,
        state = state,
        modifier = Modifier.fillMaxSize(),
        onClick = { onTap() },
    )
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoSheet(vm: GalleryViewModel, m: Media, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val details by produceState<MediaRepository.Details?>(null, m.id) { value = vm.details(m, Perms.hasLocation(ctx)) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Text("Détails", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            val d = details
            if (d == null) {
                CircularProgressIndicator(Modifier.padding(16.dp))
            } else {
                d.rows.forEach { (k, v) ->
                    Column(Modifier.padding(vertical = 7.dp)) {
                        Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (d.lat != null && d.lon != null) {
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton({ Actions.openMap(ctx, d.lat, d.lon) }) {
                        Icon(Icons.Filled.Place, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Voir sur la carte")
                    }
                }
            }
        }
    }
}

// ---------------- Vidéo ----------------

@OptIn(UnstableApi::class)
@Composable
fun rememberPlayer(loop: Boolean): ExoPlayer {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
    LaunchedEffect(loop) { player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    return player
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPage(m: Media, active: Boolean, player: ExoPlayer, autoplay: Boolean, controls: Boolean, onTap: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        var ready by remember(m.id) { mutableStateOf(false) }
        if (active) {
            LaunchedEffect(m.id) {
                player.setMediaItem(MediaItem.fromUri(m.uri))
                player.prepare()
                player.playWhenReady = autoplay
            }
            DisposableEffect(m.id) {
                val l = object : Player.Listener {
                    override fun onRenderedFirstFrame() { ready = true }
                }
                player.addListener(l)
                onDispose { player.removeListener(l); player.pause() }
            }
            AndroidView(
                factory = { c -> (LayoutInflater.from(c).inflate(R.layout.player_view, null) as PlayerView).also { it.player = player } },
                update = { it.player = player },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!active || !ready) {
            AsyncImage(Thumb(m.uri, m.dateModified), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().pointerInput(m.id, active) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { o ->
                        if (active) {
                            val delta = if (o.x < size.width / 2) -10_000L else 10_000L
                            player.seekTo((player.currentPosition + delta).coerceAtLeast(0))
                        }
                    },
                )
            },
        )
        if (active) VideoControls(player, controls)
        else Icon(Icons.Filled.PlayCircle, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.align(Alignment.Center).size(72.dp))
    }
}

@Composable
private fun BoxScope.VideoControls(player: ExoPlayer, visible: Boolean) {
    var playing by remember { mutableStateOf(player.isPlaying) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf<Float?>(null) }
    var speed by remember { mutableFloatStateOf(player.playbackParameters.speed) }
    var muted by remember { mutableStateOf(player.volume == 0f) }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition
            duration = player.duration.takeIf { it > 0 } ?: 0L
            playing = player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING)
            delay(200)
        }
    }
    AnimatedVisibility(visible, Modifier.align(Alignment.Center), enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Color(0x66000000)).clickable {
                if (player.isPlaying) player.pause() else {
                    if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                    player.play()
                }
                playing = !playing
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Lecture", tint = Color.White, modifier = Modifier.size(44.dp))
        }
    }
    AnimatedVisibility(visible, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 72.dp)) {
            Slider(
                value = seeking ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                onValueChange = { seeking = it; player.seekTo((it * duration).toLong()) },
                onValueChangeFinished = { seeking = null },
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${Format.duration(position)} / ${Format.duration(duration)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(
                    "${if (speed % 1f == 0f) speed.toInt() else speed}×", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                        val speeds = listOf(1f, 1.5f, 2f, 0.5f)
                        speed = speeds[(speeds.indexOf(speed) + 1) % speeds.size]
                        player.setPlaybackSpeed(speed)
                    }.padding(horizontal = 10.dp, vertical = 6.dp),
                )
                IconButton({ muted = !muted; player.volume = if (muted) 0f else 1f }) {
                    Icon(if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, "Son", tint = Color.White)
                }
            }
        }
    }
}

// ---------------- Visionneuse pour fichiers ouverts depuis une autre app ----------------

@Composable
fun ExternalViewer(uri: Uri, type: String?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var chrome by remember { mutableStateOf(true) }
    val isVideo = type?.startsWith("video") == true
    val player = rememberPlayer(loop = false)
    ImmersiveMode(chrome)
    Box(Modifier.fillMaxSize().background(ViewerBg)) {
        if (isVideo) {
            LaunchedEffect(uri) { player.setMediaItem(MediaItem.fromUri(uri)); player.prepare(); player.playWhenReady = true }
            AndroidView(
                factory = { c -> (LayoutInflater.from(c).inflate(R.layout.player_view, null) as PlayerView).also { it.player = player } },
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { chrome = !chrome }) })
            VideoControls(player, chrome)
        } else {
            ImagePage(uri, 0L, "", onTap = { chrome = !chrome })
        }
        AnimatedVisibility(chrome, Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xB3000000), Color.Transparent)))
                    .statusBarsPadding().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                IconButton({ Actions.shareUri(ctx, uri, type) }) { Icon(Icons.Filled.Share, "Partager", tint = Color.White) }
            }
        }
    }
}

// ---------------- Plein écran ----------------

fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun ImmersiveMode(showBars: Boolean) {
    val view = LocalView.current
    val window = remember(view) { view.context.findActivity()?.window } ?: return
    val controller = remember(window, view) { WindowCompat.getInsetsController(window, view) }
    DisposableEffect(controller) {
        val light = controller.isAppearanceLightStatusBars
        val lightNav = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = light
            controller.isAppearanceLightNavigationBars = lightNav
        }
    }
    LaunchedEffect(showBars) {
        if (showBars) controller.show(WindowInsetsCompat.Type.systemBars())
        else controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

/** Gris violacé : les images noires ou transparentes se détachent du fond. */
val ViewerBg = Color(0xFF26222F)
